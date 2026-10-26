package com.relicweave.enchantment;

import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.enchantment.EnchantmentCategory;

public final class ModEnchantmentCategories {
    public static final EnchantmentCategory SWORD_OR_AXE =
            EnchantmentCategory.create("RELICWEAVE_SWORD_OR_AXE", item -> item instanceof SwordItem || item instanceof AxeItem);
    public static final EnchantmentCategory PICKAXE =
            EnchantmentCategory.create("RELICWEAVE_PICKAXE", item -> item instanceof PickaxeItem);
    public static final EnchantmentCategory HOE =
            EnchantmentCategory.create("RELICWEAVE_HOE", item -> item instanceof HoeItem);
    public static final EnchantmentCategory SHIELD =
            EnchantmentCategory.create("RELICWEAVE_SHIELD", item -> item instanceof ShieldItem);

    /** 0.0.4 Archivist's Mark. */
    public static final EnchantmentCategory BOW_OR_CROSSBOW =
            EnchantmentCategory.create("RELICWEAVE_BOW_OR_CROSSBOW", item -> item instanceof BowItem || item instanceof CrossbowItem);

    private ModEnchantmentCategories() {}
}
