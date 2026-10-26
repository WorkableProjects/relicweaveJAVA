package com.relicweave.vestige.entity.ethereal;

import com.relicweave.vestige.VestigeConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Shared mini-boss behaviour (Concordance Guardian, Sealed Custodian, Rune Warden): a boss bar shown to players within
 * {@link #RANGE} blocks, a saved {@code home} position and the reset rule (no non-spectator player within range for
 * {@code VestigeConfig.BOSS_RESET_SECONDS} heals to full, clears the target and returns home, silently). Mini-bosses
 * are persistent and are not scaled by the boss config.
 */
public final class MiniBossSupport {
    public static final double RANGE = 32.0;
    private static final int STEP = 10;
    @Nullable
    private static Integer resetOverrideTicks;

    private final Mob mob;
    private final ServerBossEvent bar;
    private final Runnable onReset;
    @Nullable
    private BlockPos home;
    private int idleTicks;
    private int resets;

    public MiniBossSupport(Mob mob, BossEvent.BossBarColor color, Runnable onReset) {
        this.mob = mob;
        this.onReset = onReset;
        this.bar = new ServerBossEvent(mob.getDisplayName(), color, BossEvent.BossBarOverlay.PROGRESS);
        mob.setPersistenceRequired();
    }

    /** Tests only: override the reset delay in ticks ({@code null} restores the config value). */
    public static void overrideResetTicksForTests(@Nullable Integer ticks) {
        resetOverrideTicks = ticks;
    }

    private static int resetTicks() {
        if (resetOverrideTicks != null) {
            return resetOverrideTicks;
        }
        return (VestigeConfig.SPEC.isLoaded() ? VestigeConfig.BOSS_RESET_SECONDS.get() : 30) * 20;
    }

    @Nullable
    public BlockPos home() {
        return home;
    }

    public void setHome(BlockPos pos) {
        this.home = pos.immutable();
    }

    /** Sets the home to the current position if none is saved yet. */
    public BlockPos ensureHome() {
        if (home == null) {
            home = mob.blockPosition();
        }
        return home;
    }

    public int resets() {
        return resets;
    }

    public void setName(Component name) {
        bar.setName(name);
    }

    public void close() {
        bar.removeAllPlayers();
    }

    /** Server tick: bar visibility every {@value #STEP} ticks and the idle reset. */
    public void tick() {
        if (!(mob.level() instanceof ServerLevel level)) {
            return;
        }
        ensureHome();
        if (!mob.isAlive()) {
            bar.removeAllPlayers();
            return;
        }
        if (mob.tickCount % STEP != 0) {
            return;
        }
        boolean near = false;
        for (ServerPlayer player : level.players()) {
            if (player.isSpectator()) {
                bar.removePlayer(player);
                continue;
            }
            if (player.distanceToSqr(mob) <= RANGE * RANGE) {
                bar.addPlayer(player);
                near = true;
            } else {
                bar.removePlayer(player);
            }
        }
        bar.setProgress(Math.max(0.0F, Math.min(1.0F, mob.getHealth() / mob.getMaxHealth())));
        if (near) {
            idleTicks = 0;
            return;
        }
        idleTicks += STEP;
        if (idleTicks >= resetTicks()) {
            idleTicks = 0;
            reset();
        }
    }

    /** Heals fully, drops the target and returns home. */
    public void reset() {
        BlockPos h = ensureHome();
        resets++;
        mob.setHealth(mob.getMaxHealth());
        mob.setTarget(null);
        mob.getNavigation().stop();
        mob.moveTo(h.getX() + 0.5, h.getY(), h.getZ() + 0.5, mob.getYRot(), mob.getXRot());
        mob.setDeltaMovement(Vec3.ZERO);
        onReset.run();
    }

    public void save(CompoundTag tag) {
        if (home != null) {
            tag.putInt("HomeX", home.getX());
            tag.putInt("HomeY", home.getY());
            tag.putInt("HomeZ", home.getZ());
        }
    }

    public void load(CompoundTag tag) {
        if (tag.contains("HomeX")) {
            home = new BlockPos(tag.getInt("HomeX"), tag.getInt("HomeY"), tag.getInt("HomeZ"));
        }
    }
}
