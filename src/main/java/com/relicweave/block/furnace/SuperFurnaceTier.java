package com.relicweave.block.furnace;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Rarity;

/**
 * The five Super Furnace tiers (0.0.4.5). Each one is an upgrade of the last:
 * <ul>
 *   <li>{@code speed}: smelts this many times faster than a Furnace;</li>
 *   <li>{@code fuelEfficiency}: items smelted per fuel compared with a Furnace (a Celestial furnace needs no fuel);</li>
 *   <li>{@code rawOutput}: output multiplier for ores and raw materials (tag {@code relicweave:super_furnace_multiplied});</li>
 *   <li>{@code experience}: smelting experience multiplier.</li>
 * </ul>
 */
public enum SuperFurnaceTier {
    IRON("iron", 1, 2.0F, 1.0F, 1, 1, Rarity.COMMON),
    GOLD("gold", 2, 3.0F, 1.25F, 1, 2, Rarity.COMMON),
    DIAMOND("diamond", 3, 4.0F, 1.5F, 2, 2, Rarity.UNCOMMON),
    NETHERITE("netherite", 4, 6.0F, 2.0F, 2, 3, Rarity.RARE),
    CELESTIAL("celestial", 5, 10.0F, 0.0F, 3, 3, Rarity.EPIC);

    private final String name;
    private final int level;
    private final float speed;
    private final float fuelEfficiency;
    private final int rawOutput;
    private final int experience;
    private final Rarity rarity;

    SuperFurnaceTier(String name, int level, float speed, float fuelEfficiency, int rawOutput, int experience, Rarity rarity) {
        this.name = name;
        this.level = level;
        this.speed = speed;
        this.fuelEfficiency = fuelEfficiency;
        this.rawOutput = rawOutput;
        this.experience = experience;
        this.rarity = rarity;
    }

    /** Registry path of the block, e.g. {@code diamond_super_furnace}. */
    public String blockName() {
        return name + "_super_furnace";
    }

    public int level() { return level; }
    public float speed() { return speed; }
    public float fuelEfficiency() { return fuelEfficiency; }
    public boolean needsFuel() { return fuelEfficiency > 0.0F; }
    public int rawOutput() { return rawOutput; }
    public int experience() { return experience; }
    public Rarity rarity() { return rarity; }

    /** Cook time in ticks for a recipe that takes {@code recipeTicks} in a Furnace (never below 1). */
    public int cookTime(int recipeTicks) {
        return Math.max(1, Math.round(recipeTicks / speed));
    }

    /**
     * Burn time of one fuel item, scaled so it smelts {@code fuelEfficiency} times as many items as in a Furnace at
     * this tier's speed.
     */
    public int burnTime(int furnaceBurnTicks) {
        if (furnaceBurnTicks <= 0) {
            return 0;
        }
        return Math.max(1, Math.round(furnaceBurnTicks * fuelEfficiency / speed));
    }

    /** Tooltip / JEI lines describing this tier's perks. */
    public Component[] perkLines() {
        String key = "tooltip.relicweave.super_furnace.";
        java.util.List<Component> lines = new java.util.ArrayList<>();
        lines.add(Component.translatable(key + "tier", level));
        lines.add(Component.translatable(key + "speed", fmt(speed)));
        lines.add(needsFuel() ? Component.translatable(key + "fuel", fmt(fuelEfficiency))
                : Component.translatable(key + "no_fuel"));
        if (rawOutput > 1) {
            lines.add(Component.translatable(key + "raw_output", rawOutput));
        }
        if (experience > 1) {
            lines.add(Component.translatable(key + "experience", experience));
        }
        return lines.toArray(Component[]::new);
    }

    private static String fmt(float value) {
        return value == Math.floor(value) ? Integer.toString((int) value) : Float.toString(value);
    }
}
