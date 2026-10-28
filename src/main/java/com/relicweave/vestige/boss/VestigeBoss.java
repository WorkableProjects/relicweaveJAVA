package com.relicweave.vestige.boss;

import com.relicweave.RelicWeave;
import com.relicweave.vestige.VestigeConfig;
import com.relicweave.vestige.network.VestigeNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Base class for the six Vestige bosses. Handles everything that must be identical and server-authoritative for all of
 * them, so each boss class only implements its moves:
 * <ul>
 *   <li>Arena binding: {@code home} (spawn point) and the summoning altar, saved in NBT. The boss is leashed to its
 *   arena ({@link #arenaRadius()}) and teleported home if it ever leaves it.</li>
 *   <li>Boss bar for players inside the arena, and {@link #announce} for phase announcements (action bar).</li>
 *   <li>Reset: no non-spectator player in the arena for {@code bosses.resetSeconds} heals fully, returns home, calls
 *   {@link #onArenaReset()} and tells the altar, so a death or disconnect never locks the structure.</li>
 *   <li>Scaling: health = base × {@code healthMultiplier} × min({@code maxScaling}, 1 + {@code playerScaling} × (players − 1)),
 *   fixed when the fight starts; melee damage × {@code damageMultiplier}; other attacks use {@link #scaledDamage}.</li>
 *   <li>Rewards: loot comes from the boss's normal loot table on every kill; the one-time unlock is the advancement
 *   {@code relicweave:vestige/<boss path>}, awarded through {@link BossRewards} (idempotent by nature).</li>
 * </ul>
 * Subclasses must call {@code super.aiStep()}/{@code customServerAiStep()} chains as usual and implement
 * {@link #bossColor()}.
 */
public abstract class VestigeBoss extends Monster {
    private static final UUID HEALTH_SCALING_ID = UUID.fromString("6f3d7c2a-0d4e-4b8a-9a54-3c1e1b2f0a01");
    private static final UUID DAMAGE_SCALING_ID = UUID.fromString("6f3d7c2a-0d4e-4b8a-9a54-3c1e1b2f0a02");
    @Nullable
    private static Integer resetOverrideTicks;

    protected final ServerBossEvent bossEvent;
    @Nullable
    private BlockPos home;
    @Nullable
    private BlockPos altar;
    private int idleTicks;
    private boolean scaled;
    private int phase;

    protected VestigeBoss(EntityType<? extends VestigeBoss> type, Level level) {
        super(type, level);
        this.bossEvent = new ServerBossEvent(getDisplayName(), BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.NOTCHED_10);
        this.bossEvent.setColor(bossColor());
        this.xpReward = 200;
        setPersistenceRequired();
    }

    /** Boss bar color. */
    protected abstract BossEvent.BossBarColor bossColor();

    /** Radius (blocks) of the arena around {@link #home()}; players inside see the bar and count for scaling. */
    public double arenaRadius() {
        return 32.0;
    }

    /** Called after a reset (players left). Clear summons, timers and arena hazards here. */
    protected void onArenaReset() {}

    /** Called once when the first player engages (health scaling is fixed at this moment). */
    protected void onFightStart(int players) {}

    // ---- Arena ----

    public void bindArena(BlockPos home, @Nullable BlockPos altar) {
        this.home = home.immutable();
        this.altar = altar == null ? null : altar.immutable();
    }

    @Nullable
    public BlockPos home() {
        return home;
    }

    @Nullable
    public BlockPos altar() {
        return altar;
    }

    /** Players (non-spectator) inside the arena. */
    public List<ServerPlayer> arenaPlayers() {
        List<ServerPlayer> players = new ArrayList<>();
        if (!(level() instanceof ServerLevel server)) {
            return players;
        }
        double r2 = arenaRadius() * arenaRadius();
        BlockPos center = home != null ? home : blockPosition();
        for (ServerPlayer player : server.players()) {
            if (!player.isSpectator() && player.isAlive() && player.distanceToSqr(center.getX() + 0.5, center.getY(), center.getZ() + 0.5) <= r2) {
                players.add(player);
            }
        }
        return players;
    }

    /** Action-bar message to every player in the arena. */
    public void announce(Component message) {
        for (ServerPlayer player : arenaPlayers()) {
            player.displayClientMessage(message, true);
        }
    }

    // ---- Phases ----

    public int phase() {
        return phase;
    }

    /** Sets the phase; when it changes, announces {@code boss.relicweave.<path>.phase<N>} to the arena. */
    protected void setPhase(int newPhase) {
        if (newPhase != phase) {
            phase = newPhase;
            announce(Component.translatable("boss." + RelicWeave.MOD_ID + "." + bossId().getPath() + ".phase" + newPhase));
        }
    }

    // ---- Scaling ----

    public static double healthScale(int players) {
        double perPlayer = VestigeConfig.SPEC.isLoaded() ? VestigeConfig.BOSS_PLAYER_SCALING.get() : 0.35;
        double cap = VestigeConfig.SPEC.isLoaded() ? VestigeConfig.BOSS_MAX_SCALING.get() : 2.5;
        double mult = VestigeConfig.SPEC.isLoaded() ? VestigeConfig.BOSS_HEALTH_MULTIPLIER.get() : 1.0;
        return mult * Math.min(cap, 1.0 + perPlayer * Math.max(0, players - 1));
    }

    public static double damageMultiplier() {
        return VestigeConfig.SPEC.isLoaded() ? VestigeConfig.BOSS_DAMAGE_MULTIPLIER.get() : 1.0;
    }

    public static double arenaHazardStrength() {
        return VestigeConfig.SPEC.isLoaded() ? VestigeConfig.ARENA_HAZARD_STRENGTH.get() : 1.0;
    }

    /** Damage for a non-melee attack: {@code base × damageMultiplier}. */
    public float scaledDamage(float base) {
        return (float) (base * damageMultiplier());
    }

    /** Applies health/damage scaling for {@code players} and heals to the new maximum. Called once per fight. */
    public void applyScaling(int players) {
        AttributeInstance health = getAttribute(Attributes.MAX_HEALTH);
        if (health != null) {
            health.removeModifier(HEALTH_SCALING_ID);
            health.addPermanentModifier(new AttributeModifier(HEALTH_SCALING_ID, "Vestige boss scaling",
                    healthScale(players) - 1.0, AttributeModifier.Operation.MULTIPLY_TOTAL));
        }
        AttributeInstance damage = getAttribute(Attributes.ATTACK_DAMAGE);
        if (damage != null) {
            damage.removeModifier(DAMAGE_SCALING_ID);
            damage.addPermanentModifier(new AttributeModifier(DAMAGE_SCALING_ID, "Vestige boss damage",
                    damageMultiplier() - 1.0, AttributeModifier.Operation.MULTIPLY_TOTAL));
        }
        setHealth(getMaxHealth());
        scaled = true;
    }

    // ---- Ticking ----

    /** Tests only: override the reset delay in ticks. */
    public static void overrideResetTicksForTests(@Nullable Integer ticks) {
        resetOverrideTicks = ticks;
    }

    private static int resetTicks() {
        if (resetOverrideTicks != null) {
            return resetOverrideTicks;
        }
        return (VestigeConfig.SPEC.isLoaded() ? VestigeConfig.BOSS_RESET_SECONDS.get() : 30) * 20;
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (home == null) {
            home = blockPosition();
        }
        bossEvent.setProgress(getHealth() / getMaxHealth());
        if (tickCount % 10 != 0) {
            return;
        }
        List<ServerPlayer> players = arenaPlayers();
        for (ServerPlayer player : List.copyOf(bossEvent.getPlayers())) {
            if (!players.contains(player)) {
                untrack(player);
            }
        }
        for (ServerPlayer player : players) {
            if (!bossEvent.getPlayers().contains(player)) {
                bossEvent.addPlayer(player);
                VestigeNetwork.send(player, new VestigeNetwork.BossMusicPacket(musicEvent().toString()));
            }
        }
        if (players.isEmpty()) {
            idleTicks += 10;
            if (idleTicks >= resetTicks()) {
                resetArena();
            }
        } else {
            idleTicks = 0;
            if (!scaled) {
                applyScaling(players.size());
                onFightStart(players.size());
            }
        }
        double leash = arenaRadius() + 8.0;
        if (distanceToSqr(home.getX() + 0.5, home.getY(), home.getZ() + 0.5) > leash * leash) {
            teleportTo(home.getX() + 0.5, home.getY(), home.getZ() + 0.5);
            getNavigation().stop();
        }
    }

    /** Heal, return home, forget the fight. Public for tests and the altar. */
    public void resetArena() {
        idleTicks = 0;
        scaled = false;
        setTarget(null);
        setPhase(0);
        setHealth(getMaxHealth());
        if (home != null) {
            teleportTo(home.getX() + 0.5, home.getY(), home.getZ() + 0.5);
        }
        getNavigation().stop();
        onArenaReset();
    }

    public boolean isScaled() {
        return scaled;
    }

    // ---- Death and rewards ----

    /** {@code relicweave:<entity path>}, e.g. {@code relicweave:thorned_regent}. */
    public ResourceLocation bossId() {
        ResourceLocation key = ForgeRegistries.ENTITY_TYPES.getKey(getType());
        return key == null ? RelicWeave.id("unknown_boss") : key;
    }

    @Override
    public void die(DamageSource source) {
        if (!level().isClientSide && !isRemoved() && !dead) {
            BossRewards.onDefeated(this, source);
        }
        super.die(source);
    }

    @Override
    public void remove(RemovalReason reason) {
        for (ServerPlayer player : List.copyOf(bossEvent.getPlayers())) {
            untrack(player);
        }
        super.remove(reason);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        untrack(player);
    }

    /** Removes {@code player} from the boss bar and stops their boss music. */
    private void untrack(ServerPlayer player) {
        if (bossEvent.getPlayers().contains(player)) {
            bossEvent.removePlayer(player);
            VestigeNetwork.send(player, new VestigeNetwork.BossMusicPacket(""));
        }
    }

    /** Sound event of this boss's fight music: {@code relicweave:music.boss.<boss path>}. */
    public ResourceLocation musicEvent() {
        return RelicWeave.id("music.boss." + bossId().getPath());
    }

    @Override
    public void setCustomName(@Nullable Component name) {
        super.setCustomName(name);
        bossEvent.setName(getDisplayName());
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public boolean canChangeDimensions() {
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (home != null) {
            tag.put("Home", NbtUtils.writeBlockPos(home));
        }
        if (altar != null) {
            tag.put("Altar", NbtUtils.writeBlockPos(altar));
        }
        tag.putBoolean("Scaled", scaled);
        tag.putInt("BossPhase", phase);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        home = tag.contains("Home") ? NbtUtils.readBlockPos(tag.getCompound("Home")) : null;
        altar = tag.contains("Altar") ? NbtUtils.readBlockPos(tag.getCompound("Altar")) : null;
        scaled = tag.getBoolean("Scaled");
        phase = tag.getInt("BossPhase");
        if (hasCustomName()) {
            bossEvent.setName(getDisplayName());
        }
    }
}
