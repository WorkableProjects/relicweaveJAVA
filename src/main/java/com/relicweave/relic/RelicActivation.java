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
import net.minecraft.world.item.ItemStack;
import com.relicweave.vestige.VestigeKeys;
import com.relicweave.vestige.relic.GravityNail;
import com.relicweave.vestige.relic.MnemonicSpindle;
import com.relicweave.vestige.relic.RiftglassLantern;
import com.relicweave.vestige.relic.SealkeepersSignet;
import com.relicweave.vestige.relic.SurveyorScan;
import com.relicweave.vestige.relic.VestigeRelics;

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
            case SURVEYORS_LENS -> surveyorPulse(player);
            case GRAVITY_NAIL -> gravityNail(player);
            case RIFTGLASS_LANTERN -> riftglassLantern(player);
            case MNEMONIC_SPINDLE -> mnemonicSpindle(player);
            case SEALKEEPERS_SIGNET -> sealkeepersSignet(player);
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

    // ---- 0.0.4 Vestige relics (cooldown starts only when the relic actually did something) ----

    private static boolean inVestige(ServerPlayer player) {
        return player.level().dimension() == VestigeKeys.THE_VESTIGE;
    }

    private static boolean vestigeOnly(ServerPlayer player, RelicType type) {
        if (inVestige(player)) {
            return true;
        }
        player.displayClientMessage(Component.translatable("message.relicweave.relic.vestige_only", type.displayName()), true);
        return false;
    }

    private static void finishVestige(ServerPlayer player, RelicType type, ItemStack stack) {
        player.level().playSound(null, player.blockPosition(), ModSounds.RELIC_ACTIVATE.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        RelicCooldowns.start(player, type, VestigeRelics.cooldownTicks(type, stack));
    }

    private static boolean surveyorPulse(ServerPlayer player) {
        ItemStack stack = RelicLocator.first(player, RelicType.SURVEYORS_LENS);
        int found = SurveyorScan.pulse(player, stack);
        player.displayClientMessage(Component.translatable(found > 0 ? "message.relicweave.surveyor.found" : "message.relicweave.surveyor.none", found), true);
        finishVestige(player, RelicType.SURVEYORS_LENS, stack);
        return true;
    }

    private static boolean gravityNail(ServerPlayer player) {
        if (!vestigeOnly(player, RelicType.GRAVITY_NAIL)) {
            return false;
        }
        ItemStack stack = RelicLocator.first(player, RelicType.GRAVITY_NAIL);
        GravityNail.anchor(player, GravityNail.durationTicks(stack));
        player.displayClientMessage(Component.translatable("message.relicweave.gravity_nail.anchored"), true);
        finishVestige(player, RelicType.GRAVITY_NAIL, stack);
        return true;
    }

    private static boolean riftglassLantern(ServerPlayer player) {
        if (!vestigeOnly(player, RelicType.RIFTGLASS_LANTERN)) {
            return false;
        }
        ItemStack stack = RelicLocator.first(player, RelicType.RIFTGLASS_LANTERN);
        RiftglassLantern.create(player, stack);
        player.displayClientMessage(Component.translatable("message.relicweave.lantern.lit"), true);
        finishVestige(player, RelicType.RIFTGLASS_LANTERN, stack);
        return true;
    }

    private static boolean mnemonicSpindle(ServerPlayer player) {
        if (!MnemonicSpindle.terminalNearby(player.serverLevel(), player.blockPosition())) {
            player.displayClientMessage(Component.translatable("message.relicweave.spindle.no_terminal"), true);
            return false;
        }
        ItemStack stack = RelicLocator.first(player, RelicType.MNEMONIC_SPINDLE);
        MnemonicSpindle.decode(player, stack);
        finishVestige(player, RelicType.MNEMONIC_SPINDLE, stack);
        return true;
    }

    private static boolean sealkeepersSignet(ServerPlayer player) {
        if (!vestigeOnly(player, RelicType.SEALKEEPERS_SIGNET)) {
            return false;
        }
        ItemStack stack = RelicLocator.first(player, RelicType.SEALKEEPERS_SIGNET);
        if (SealkeepersSignet.pulse(player, stack) == 0) {
            player.displayClientMessage(Component.translatable("message.relicweave.signet.nothing"), true);
            return false;
        }
        finishVestige(player, RelicType.SEALKEEPERS_SIGNET, stack);
        return true;
    }
}
