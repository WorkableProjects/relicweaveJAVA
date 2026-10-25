package com.relicweave.item;

/**
 * Charms work from anywhere in the player's main inventory or offhand. Each type counts once, no matter how many are
 * carried; different types stack. Effects are applied by the charm event handler.
 */
public enum CharmType {
    /** +5% movement speed. */
    COPPER,
    /** +2 armor. */
    IRON,
    /** +2 luck. */
    GOLD,
    /** +2 max health. */
    DIAMOND,
    /** Fire Resistance and +10% knockback resistance. */
    NETHERITE
}
