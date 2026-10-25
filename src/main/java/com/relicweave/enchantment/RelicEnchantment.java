package com.relicweave.enchantment;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;

import java.util.function.Predicate;

/**
 * A RelicWeave enchantment: obtainable only through transmutation (not from enchanting tables, loot, or villagers),
 * applied and combined at the anvil. Behaviour lives in event handlers, not in this class.
 */
public class RelicEnchantment extends Enchantment {
    private final int maxLevel;
    private final Predicate<Enchantment> incompatible;

    public RelicEnchantment(Rarity rarity, EnchantmentCategory category, int maxLevel, EquipmentSlot... slots) {
        this(rarity, category, maxLevel, other -> false, slots);
    }

    public RelicEnchantment(Rarity rarity, EnchantmentCategory category, int maxLevel, Predicate<Enchantment> incompatible,
                            EquipmentSlot... slots) {
        super(rarity, category, slots);
        this.maxLevel = maxLevel;
        this.incompatible = incompatible;
    }

    @Override
    public int getMaxLevel() {
        return maxLevel;
    }

    @Override
    public int getMinCost(int level) {
        return 10 + (level - 1) * 10;
    }

    @Override
    public int getMaxCost(int level) {
        return getMinCost(level) + 30;
    }

    @Override
    protected boolean checkCompatibility(Enchantment other) {
        return super.checkCompatibility(other) && !incompatible.test(other);
    }

    @Override
    public boolean isTreasureOnly() {
        return true;
    }

    @Override
    public boolean isDiscoverable() {
        return false;
    }

    @Override
    public boolean isTradeable() {
        return false;
    }
}
