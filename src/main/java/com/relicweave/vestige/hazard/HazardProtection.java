package com.relicweave.vestige.hazard;

import com.relicweave.vestige.VestigeConfig;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.ToDoubleFunction;

/**
 * Shared hazard-protection registry. Each source (Ward Salve, infusions, relics, Aetherforged armor, ...) registers a
 * contributor once during common setup; a contributor returns a reduction fraction in [0, 1] for a player. Reductions
 * from different sources combine multiplicatively and the total is capped by {@code maxHazardProtection}, so no single
 * item, or pile of items, makes a hazard irrelevant. Server-side only.
 */
public final class HazardProtection {
    public enum Hazard { RIFT_SATURATION, GRAVITY_SHEAR, STARFALL, NULL_POCKET, HUSHFIELD }

    private static final Map<Hazard, List<ToDoubleFunction<Player>>> CONTRIBUTORS = new EnumMap<>(Hazard.class);

    private HazardProtection() {}

    /** Register once (e.g. from FMLCommonSetupEvent#enqueueWork). Thread-safe enough for setup-time registration. */
    public static synchronized void register(Hazard hazard, ToDoubleFunction<Player> contributor) {
        CONTRIBUTORS.computeIfAbsent(hazard, h -> new ArrayList<>()).add(contributor);
    }

    /** Total reduction fraction for {@code hazard}, in [0, maxHazardProtection]. */
    public static double reduction(Player player, Hazard hazard) {
        double remaining = 1.0;
        for (ToDoubleFunction<Player> contributor : CONTRIBUTORS.getOrDefault(hazard, List.of())) {
            double r = contributor.applyAsDouble(player);
            remaining *= 1.0 - Math.max(0.0, Math.min(1.0, r));
        }
        double cap = VestigeConfig.SPEC.isLoaded() ? VestigeConfig.MAX_HAZARD_PROTECTION.get() : 0.8;
        return Math.min(cap, 1.0 - remaining);
    }

    /** {@code amount * (1 - reduction)}. */
    public static double apply(Player player, Hazard hazard, double amount) {
        return amount * (1.0 - reduction(player, hazard));
    }
}
