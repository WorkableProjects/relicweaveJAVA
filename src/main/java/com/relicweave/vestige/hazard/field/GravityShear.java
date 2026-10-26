package com.relicweave.vestige.hazard.field;

import com.relicweave.vestige.VestigeConfig;
import com.relicweave.vestige.VestigeKeys;
import com.relicweave.vestige.hazard.FieldRegistry;
import com.relicweave.vestige.hazard.HazardProtection;
import com.relicweave.vestige.registry.VestigeSounds;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Gravity Shear: in {@code #relicweave:gravity_shear} biomes, gravity pulses the player upward (60%) or sideways
 * (40%) every so often. Every pulse is telegraphed 40 ticks ahead (particles, sound, action-bar arrow, HUD flag),
 * crouching halves it, Slow Falling makes the landing harmless, and a {@code gravity_anchor} block within 3 blocks
 * cancels it. Server-side only.
 */
public final class GravityShear {
    /** Ticks of warning before a pulse. */
    public static final int TELEGRAPH_TICKS = 40;
    public static final int ANCHOR_RADIUS = 3;
    public static final double UP_CHANCE = 0.6;

    private static final Map<UUID, State> STATES = new HashMap<>();

    private static final class State {
        long nextPulse;
        boolean telegraphed;
        Direction direction = Direction.UP;
    }

    private GravityShear() {}

    static void clear(UUID id) {
        STATES.remove(id);
    }

    static void clearAll() {
        STATES.clear();
    }

    static boolean isTelegraphing(UUID id) {
        State state = STATES.get(id);
        return state != null && state.telegraphed;
    }

    private static boolean enabled() {
        return !VestigeConfig.SPEC.isLoaded() || VestigeConfig.GRAVITY_SHEAR.get();
    }

    private static double baseStrength() {
        return VestigeConfig.SPEC.isLoaded() ? VestigeConfig.GRAVITY_PULSE_STRENGTH.get() : 0.6;
    }

    private static int intervalTicks() {
        return (VestigeConfig.SPEC.isLoaded() ? VestigeConfig.GRAVITY_PULSE_INTERVAL_SECONDS.get() : 12) * 20;
    }

    /** Ticks until the next pulse: a random delay in [0.6, 1.4] x the configured interval. */
    public static long randomDelay(RandomSource random, int intervalTicks) {
        return Math.round(intervalTicks * (0.6 + random.nextDouble() * 0.8));
    }

    /** Pulse strength after protection and bracing (crouching halves it). */
    public static double effectiveStrength(ServerPlayer player, double raw) {
        double s = raw * (1.0 - HazardProtection.reduction(player, HazardProtection.Hazard.GRAVITY_SHEAR));
        return player.isShiftKeyDown() ? s * 0.5 : s;
    }

    /** True if a {@code gravity_anchor} block is within {@link #ANCHOR_RADIUS} blocks of the player. */
    public static boolean isAnchored(ServerPlayer player) {
        return player.serverLevel().getPoiManager().findClosest(holder -> holder.value() == FieldRegistry.GRAVITY_ANCHOR_POI.get(),
                player.blockPosition(), ANCHOR_RADIUS, PoiManager.Occupancy.ANY).isPresent();
    }

    /**
     * Applies one gravity pulse, ignoring anchors (the caller decides). {@code dir} UP lifts the player, a horizontal
     * direction shoves them sideways with a little lift. {@code strength} is the raw strength: protection and bracing
     * are applied here. Also used by the Gravity Maw boss.
     */
    public static void pulse(ServerPlayer player, Direction dir, double strength) {
        double s = effectiveStrength(player, strength);
        Vec3 motion = player.getDeltaMovement();
        if (dir.getAxis() == Direction.Axis.Y) {
            player.setDeltaMovement(motion.x, Math.max(motion.y, 0.0) + s, motion.z);
            player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 60, 0, false, false, true));
        } else {
            player.setDeltaMovement(motion.x + dir.getStepX() * s * 0.7, motion.y + 0.25 * s, motion.z + dir.getStepZ() * s * 0.7);
            player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 40, 0, false, false, true));
        }
        player.hurtMarked = true;
        player.fallDistance = 0.0F;
        player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
                VestigeSounds.GRAVITY_PULSE.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    static void tick(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        UUID id = player.getUUID();
        if (!enabled() || !level.getBiome(player.blockPosition()).is(VestigeKeys.GRAVITY_SHEAR)) {
            STATES.remove(id);
            return;
        }
        long now = level.getGameTime();
        RandomSource random = level.getRandom();
        State state = STATES.get(id);
        if (state == null) {
            state = new State();
            state.nextPulse = now + randomDelay(random, intervalTicks());
            STATES.put(id, state);
            return;
        }
        if (!state.telegraphed && now >= state.nextPulse - TELEGRAPH_TICKS) {
            state.telegraphed = true;
            state.direction = random.nextDouble() < UP_CHANCE ? Direction.UP
                    : Direction.from2DDataValue(random.nextInt(4));
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    VestigeSounds.GRAVITY_TELEGRAPH.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
            player.displayClientMessage(telegraphMessage(player, state.direction), true);
        }
        if (state.telegraphed && now < state.nextPulse) {
            level.sendParticles(ParticleTypes.REVERSE_PORTAL, player.getX(), player.getY() + 0.2, player.getZ(),
                    10, 0.8, 0.3, 0.8, 0.12);
            return;
        }
        if (state.telegraphed) {
            if (!isAnchored(player)) {
                pulse(player, state.direction, baseStrength());
            }
            state.telegraphed = false;
            state.nextPulse = now + randomDelay(random, intervalTicks());
        }
    }

    /** Action-bar text for a coming pulse; the arrow is relative to where the player is looking. */
    static Component telegraphMessage(ServerPlayer player, Direction dir) {
        if (dir.getAxis() == Direction.Axis.Y) {
            return Component.translatable("message.relicweave.gravity.telegraph_up");
        }
        float relative = Mth.wrapDegrees(dir.toYRot() - player.getYRot());
        String arrow;
        if (Math.abs(relative) <= 45.0F) {
            arrow = "\u2191";
        } else if (Math.abs(relative) >= 135.0F) {
            arrow = "\u2193";
        } else {
            arrow = relative > 0 ? "\u2192" : "\u2190";
        }
        return Component.translatable("message.relicweave.gravity.telegraph_side", arrow);
    }
}
