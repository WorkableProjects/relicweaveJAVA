package com.relicweave.enchantment;

import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.enchantment.EnchantmentCategory;

public final class ModEnchantmentCategories {
    public static final EnchantmentCategory SWORD_OR_AXE =
            EnchantmentCategory.create("RELICWEAVE_SWORD_OR_AXE", item -> item instanceof SwordItem || item instanceof AxeItem);
    public static final EnchantmentCategory PICKAXE =
            EnchantmentCategory.create("RELICWEAVE_PICKAXE", item -> item instanceof PickaxeItem);

    private ModEnchantmentCategories() {}
}
