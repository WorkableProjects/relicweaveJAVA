package com.relicweave.vestige.cycle;

import com.relicweave.vestige.VestigeConfig;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * Pure Concordance Cycle arithmetic. The phase is a function of the world's game time (which keeps counting across
 * restarts and is shared by every dimension), so every player and every system agrees on it and restarting the server
 * can never reset or skip a phase. Durations come from {@link VestigeConfig} (synced to clients), else defaults.
 */
public final class ConcordanceCycle {
    public static final int DEFAULT_QUIET_SECONDS = 540;
    public static final int DEFAULT_RESONANCE_SECONDS = 360;
    public static final int DEFAULT_CONVERGENCE_SECONDS = 240;

    /** GameTest-only override of {@code cycle.enabled}; Forge config values cannot be toggled reliably at runtime. */
    @Nullable
    private static Boolean enabledOverride;

    private ConcordanceCycle() {}

    public static boolean enabled() {
        if (enabledOverride != null) {
            return enabledOverride;
        }
        return !VestigeConfig.SPEC.isLoaded() || VestigeConfig.CYCLE_ENABLED.get();
    }

    /** Tests only: force the enabled state ({@code null} restores the config value). */
    public static void overrideEnabledForTests(@Nullable Boolean enabled) {
        enabledOverride = enabled;
    }

    /** Length of {@code phase} in ticks (always at least 20). */
    public static long durationTicks(CyclePhase phase) {
        int seconds;
        if (VestigeConfig.SPEC.isLoaded()) {
            seconds = switch (phase) {
                case QUIET -> VestigeConfig.QUIET_SECONDS.get();
                case RESONANCE -> VestigeConfig.RESONANCE_SECONDS.get();
                case CONVERGENCE -> VestigeConfig.CONVERGENCE_SECONDS.get();
            };
        } else {
            seconds = switch (phase) {
                case QUIET -> DEFAULT_QUIET_SECONDS;
                case RESONANCE -> DEFAULT_RESONANCE_SECONDS;
                case CONVERGENCE -> DEFAULT_CONVERGENCE_SECONDS;
            };
        }
        return Math.max(1, seconds) * 20L;
    }

    public static long cycleTicks() {
        long total = 0;
        for (CyclePhase phase : CyclePhase.values()) {
            total += durationTicks(phase);
        }
        return total;
    }

    /** Phase at {@code gameTime}. Always {@link CyclePhase#QUIET} when the cycle is disabled. */
    public static CyclePhase phaseAt(long gameTime) {
        if (!enabled()) {
            return CyclePhase.QUIET;
        }
        long t = Math.floorMod(gameTime, cycleTicks());
        for (CyclePhase phase : CyclePhase.values()) {
            long d = durationTicks(phase);
            if (t < d) {
                return phase;
            }
            t -= d;
        }
        return CyclePhase.QUIET;
    }

    /** Ticks until the phase after {@code gameTime} begins; {@link Long#MAX_VALUE} when the cycle is disabled. */
    public static long ticksUntilNextPhase(long gameTime) {
        if (!enabled()) {
            return Long.MAX_VALUE;
        }
        long t = Math.floorMod(gameTime, cycleTicks());
        for (CyclePhase phase : CyclePhase.values()) {
            long d = durationTicks(phase);
            if (t < d) {
                return d - t;
            }
            t -= d;
        }
        return 0;
    }

    public static CyclePhase phase(Level level) {
        return phaseAt(level.getGameTime());
    }

    public static long ticksUntilNextPhase(Level level) {
        return ticksUntilNextPhase(level.getGameTime());
    }
}
