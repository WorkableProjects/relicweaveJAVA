package com.relicweave.relic;

import com.relicweave.RelicWeave;
import com.relicweave.registry.ModSounds;
import com.relicweave.vestige.hazard.field.NullPockets;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;

/**
 * Server-side entry point for the Activate Relic key (and right-clicking a Wayfarer's Relic). Re-validates everything:
 * the player must be alive, not a spectator, and carry the relic in a valid location ({@link RelicLocator}); the relic
 * must be activatable and off cooldown ({@link RelicCooldowns}). Duplicates of a relic never activate it twice.
 */
public final class RelicActivation {
    /** Radius (blocks, bounding-box inflate) in which the Echo Charm highlights hostile mobs. */
    public static final double ECHO_RADIUS = 24.0;
    /** Duration (ticks) of the Glowing effect applied by the Echo Charm. Glowing is a vanilla effect visible to everyone. */
    public static final int ECHO_GLOW_TICKS = 200;

    private static final String LODESTONE_KEY = RelicWeave.MOD_ID + ":lodestone_enabled";

    private RelicActivation() {}

    /** Called on the server thread. Returns true if the relic activated (or toggled). */
    public static boolean tryActivate(ServerPlayer player, RelicType type) {
        if (!player.isAlive() || player.isSpectator() || !type.activatable() || !RelicLocator.has(player, type)) {
            return false;
        }
        if (NullPockets.isSuppressed(player)) {
            // Null Pocket (0.0.4): active relics do nothing here; cooldowns and charges are untouched.
            player.displayClientMessage(Component.translatable("message.relicweave.null_pocket.suppressed", type.displayName()), true);
            return false;
        }
        if (type.hasCooldown() && !RelicCooldowns.isReady(player, type)) {
            int seconds = (RelicCooldowns.remainingTicks(player, type) + 19) / 20;
            player.displayClientMessage(Component.translatable("message.relicweave.relic.cooldown", type.displayName(), seconds), true);
            return false;
        }
        return switch (type) {
            case LODESTONE -> toggleLodestone(player);
            case WAYFARER -> WayfarerChannel.start(player);
            case ECHO -> echoPulse(player);
            default -> false;
        };
    }

    // ---- Lodestone ----

    public static boolean isLodestoneEnabled(Player player) {
        return player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG).getBoolean(LODESTONE_KEY);
    }

    public static void setLodestoneEnabled(Player player, boolean enabled) {
        CompoundTag persisted = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        persisted.putBoolean(LODESTONE_KEY, enabled);
        player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persisted);
    }

    private static boolean toggleLodestone(ServerPlayer player) {
        boolean enabled = !isLodestoneEnabled(player);
        setLodestoneEnabled(player, enabled);
        player.displayClientMessage(Component.translatable(enabled ? "message.relicweave.lodestone.on" : "message.relicweave.lodestone.off"), true);
        return true;
    }

    // ---- Echo ----

    private static boolean echoPulse(ServerPlayer player) {
        for (LivingEntity mob : player.level().getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(ECHO_RADIUS), e -> e instanceof Enemy && e.isAlive())) {
            mob.addEffect(new MobEffectInstance(MobEffects.GLOWING, ECHO_GLOW_TICKS, 0, false, false));
        }
        player.level().playSound(null, player.blockPosition(), ModSounds.RELIC_ACTIVATE.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        RelicCooldowns.start(player, RelicType.ECHO);
        return true;
    }
}
