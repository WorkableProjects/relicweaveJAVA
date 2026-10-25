package com.relicweave.enchantment;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.enchantment.Enchantment;

/**
 * Swords and Axes. The damage bonus itself is applied by {@link KeenEdgeEvents}. Obtainable only through
 * transmutation (not from enchanting tables, loot, or villagers); books apply and combine at the anvil.
 */
public class KeenEdgeEnchantment extends Enchantment {
    public static final int MAX_LEVEL = 3;

    public KeenEdgeEnchantment() {
        super(Rarity.UNCOMMON, ModEnchantmentCategories.SWORD_OR_AXE, new EquipmentSlot[]{EquipmentSlot.MAINHAND});
    }

    @Override
    public int getMaxLevel() {
        return MAX_LEVEL;
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
