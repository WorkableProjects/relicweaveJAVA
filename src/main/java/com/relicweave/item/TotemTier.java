package com.relicweave.item;

/**
 * RelicWeave totems are not consumed: they save the holder from death, then go on cooldown. Diamond and Netherite
 * totems work from the offhand only; the Ultra Totem works from either hand. Behaviour lives in the totem event handler.
 */
public enum TotemTier {
    /** Passive: +4 max health. Save: 4 HP, Resistance II + Regeneration II (5 s). Cooldown 5 min. */
    DIAMOND(6000, false),
    /** Passive: +4 max health, Fire Resistance. Save: 50% HP, Resistance II + Regeneration II (10 s), Fire Resistance (20 s), knocks back nearby mobs. Cooldown 3 min. */
    NETHERITE(3600, false),
    /** Passive: +8 max health, Fire Resistance, Regeneration I. Save: full HP, clears harmful effects, Absorption IV (30 s), Resistance II + Regeneration III + Strength I (15 s), Fire Resistance (60 s). Cooldown 1 min. */
    ULTRA(1200, true);

    private final int cooldownTicks;
    private final boolean worksInMainHand;

    TotemTier(int cooldownTicks, boolean worksInMainHand) {
        this.cooldownTicks = cooldownTicks;
        this.worksInMainHand = worksInMainHand;
    }

    public int cooldownTicks() {
        return cooldownTicks;
    }

    public boolean worksInMainHand() {
        return worksInMainHand;
    }
}
