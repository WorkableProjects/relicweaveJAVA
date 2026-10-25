package com.relicweave.registry;

import com.relicweave.RelicWeave;
import com.relicweave.enchantment.KeenEdgeEnchantment;
import com.relicweave.enchantment.ModEnchantmentCategories;
import com.relicweave.enchantment.PurlingEnchantment;
import com.relicweave.enchantment.RelicEnchantment;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEnchantments {
    public static final DeferredRegister<Enchantment> ENCHANTMENTS = DeferredRegister.create(ForgeRegistries.ENCHANTMENTS, RelicWeave.MOD_ID);

    // 0.0.1
    public static final RegistryObject<Enchantment> KEEN_EDGE = ENCHANTMENTS.register("keen_edge", KeenEdgeEnchantment::new);
    public static final RegistryObject<Enchantment> PURLING = ENCHANTMENTS.register("purling", PurlingEnchantment::new);

    // 0.0.2 (effects: see the matching event handlers in com.relicweave.enchantment)
    public static final RegistryObject<Enchantment> COBBLED_UP = ENCHANTMENTS.register("cobbled_up",
            () -> new RelicEnchantment(Enchantment.Rarity.UNCOMMON, ModEnchantmentCategories.PICKAXE, 3, EquipmentSlot.MAINHAND));
    public static final RegistryObject<Enchantment> OXIDATION = ENCHANTMENTS.register("oxidation",
            () -> new RelicEnchantment(Enchantment.Rarity.UNCOMMON, ModEnchantmentCategories.SWORD_OR_AXE, 3, EquipmentSlot.MAINHAND));
    public static final RegistryObject<Enchantment> CONDUCTIVE = ENCHANTMENTS.register("conductive",
            () -> new RelicEnchantment(Enchantment.Rarity.UNCOMMON, EnchantmentCategory.DIGGER, 3,
                    other -> other == Enchantments.BLOCK_EFFICIENCY, EquipmentSlot.MAINHAND));
    public static final RegistryObject<Enchantment> GOLEMED = ENCHANTMENTS.register("golemed",
            () -> new RelicEnchantment(Enchantment.Rarity.UNCOMMON, EnchantmentCategory.ARMOR_CHEST, 3, EquipmentSlot.CHEST));
    public static final RegistryObject<Enchantment> PIGLINIFIED = ENCHANTMENTS.register("piglinified",
            () -> new RelicEnchantment(Enchantment.Rarity.RARE, EnchantmentCategory.ARMOR_HEAD, 1, EquipmentSlot.HEAD));
    public static final RegistryObject<Enchantment> VILLAGERS_BEST_FRIEND = ENCHANTMENTS.register("villagers_best_friend",
            () -> new RelicEnchantment(Enchantment.Rarity.RARE, EnchantmentCategory.ARMOR_LEGS, 2, EquipmentSlot.LEGS));
    public static final RegistryObject<Enchantment> THE_NECROMANCER = ENCHANTMENTS.register("the_necromancer",
            () -> new RelicEnchantment(Enchantment.Rarity.VERY_RARE, ModEnchantmentCategories.SWORD_OR_AXE, 3, EquipmentSlot.MAINHAND));
    public static final RegistryObject<Enchantment> NECROMANCED = ENCHANTMENTS.register("necromanced",
            () -> new RelicEnchantment(Enchantment.Rarity.VERY_RARE, EnchantmentCategory.ARMOR_HEAD, 1, EquipmentSlot.HEAD));
    public static final RegistryObject<Enchantment> UPBOUND = ENCHANTMENTS.register("upbound",
            () -> new RelicEnchantment(Enchantment.Rarity.VERY_RARE, EnchantmentCategory.ARMOR_FEET, 2, EquipmentSlot.FEET));

    private ModEnchantments() {}
}
