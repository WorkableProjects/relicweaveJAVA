package com.relicweave.enchantment.attunement;

/** Steady Draw (bow): shared numbers for the client-side handler in {@code attunement.client.SteadyDrawClient}. */
public final class SteadyDrawEvents {
    /** Vanilla multiplier applied to movement input while using an item. */
    public static final float VANILLA_USE_ITEM_MULTIPLIER = 0.2F;
    public static final float MULTIPLIER_BONUS_PER_LEVEL = 0.3F;

    private SteadyDrawEvents() {}

    /** Effective movement multiplier while drawing a bow with Steady Draw {@code level}: I = 0.5, II = 0.8. */
    public static float steadyDrawMultiplier(int level) {
        return Math.min(1.0F, VANILLA_USE_ITEM_MULTIPLIER + MULTIPLIER_BONUS_PER_LEVEL * level);
    }
}
