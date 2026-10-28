package com.relicweave.registry;

import com.relicweave.RelicWeave;
import com.relicweave.item.CelestialArmorItem;
import com.relicweave.item.CelestialMaterials;
import com.relicweave.item.CharmItem;
import com.relicweave.item.CharmType;
import com.relicweave.item.ModFoods;
import com.relicweave.item.MultitoolItem;
import com.relicweave.item.RelicItem;
import com.relicweave.item.RelicTotemItem;
import com.relicweave.item.ScubaArmorItem;
import com.relicweave.item.TotemTier;
import com.relicweave.relic.RelicType;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.Tiers;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Standalone items (block items live in {@link ModBlocks}). */
public final class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, RelicWeave.MOD_ID);

    // ---- Relics: charms (work anywhere in the inventory) ----
    public static final RegistryObject<Item> COPPER_CHARM = charm("copper_charm", CharmType.COPPER, Rarity.COMMON);
    public static final RegistryObject<Item> IRON_CHARM = charm("iron_charm", CharmType.IRON, Rarity.COMMON);
    public static final RegistryObject<Item> GOLD_CHARM = charm("gold_charm", CharmType.GOLD, Rarity.UNCOMMON);
    public static final RegistryObject<Item> DIAMOND_CHARM = charm("diamond_charm", CharmType.DIAMOND, Rarity.RARE);
    public static final RegistryObject<Item> NETHERITE_CHARM = ITEMS.register("netherite_charm",
            () -> new CharmItem(CharmType.NETHERITE, new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()));

    // ---- Relics: 0.0.3 Attunement relics (see RelicType) ----
    public static final RegistryObject<Item> LODESTONE_CHARM = relic(RelicType.LODESTONE, Rarity.UNCOMMON);
    public static final RegistryObject<Item> TIDAL_CHARM = relic(RelicType.TIDAL, Rarity.UNCOMMON);
    public static final RegistryObject<Item> FEATHERWOVEN_CHARM = relic(RelicType.FEATHERWOVEN, Rarity.UNCOMMON);
    public static final RegistryObject<Item> WAYFARERS_RELIC = relic(RelicType.WAYFARER, Rarity.RARE);
    public static final RegistryObject<Item> ECHO_CHARM = relic(RelicType.ECHO, Rarity.RARE);
    public static final RegistryObject<Item> GUARDIAN_RELIC = relic(RelicType.GUARDIAN, Rarity.EPIC);

    // ---- Relics: totems (offhand; Ultra works in either hand) ----
    public static final RegistryObject<Item> DIAMOND_TOTEM = ITEMS.register("diamond_totem",
            () -> new RelicTotemItem(TotemTier.DIAMOND, new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));
    public static final RegistryObject<Item> NETHERITE_TOTEM = ITEMS.register("netherite_totem",
            () -> new RelicTotemItem(TotemTier.NETHERITE, new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()));
    public static final RegistryObject<Item> ULTRA_TOTEM = ITEMS.register("ultra_totem",
            () -> new RelicTotemItem(TotemTier.ULTRA, new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()));

    // ---- Food ----
    public static final RegistryObject<Item> DIAMOND_STEAK = ITEMS.register("diamond_steak",
            () -> new Item(new Item.Properties().food(ModFoods.DIAMOND_STEAK).rarity(Rarity.RARE)));
    public static final RegistryObject<Item> NETHERITE_STEAK = ITEMS.register("netherite_steak",
            () -> new Item(new Item.Properties().food(ModFoods.NETHERITE_STEAK).rarity(Rarity.EPIC).fireResistant()));

    // ---- Celestial materials ----
    public static final RegistryObject<Item> CELESTIAL_SHARD = ITEMS.register("celestial_shard",
            () -> new Item(new Item.Properties().fireResistant().rarity(Rarity.UNCOMMON)));
    public static final RegistryObject<Item> CELESTIAL_INGOT = ITEMS.register("celestial_ingot",
            () -> new Item(new Item.Properties().fireResistant().rarity(Rarity.RARE)));

    // ---- Celestial tools ----
    public static final RegistryObject<Item> CELESTIAL_SWORD = ITEMS.register("celestial_sword",
            () -> new SwordItem(CelestialMaterials.TIER, 3, -2.4F, celestialGear()));
    public static final RegistryObject<Item> CELESTIAL_PICKAXE = ITEMS.register("celestial_pickaxe",
            () -> new PickaxeItem(CelestialMaterials.TIER, 1, -2.8F, celestialGear()));
    public static final RegistryObject<Item> CELESTIAL_AXE = ITEMS.register("celestial_axe",
            () -> new AxeItem(CelestialMaterials.TIER, 5.0F, -3.0F, celestialGear()));
    public static final RegistryObject<Item> CELESTIAL_SHOVEL = ITEMS.register("celestial_shovel",
            () -> new ShovelItem(CelestialMaterials.TIER, 1.5F, -3.0F, celestialGear()));
    public static final RegistryObject<Item> CELESTIAL_HOE = ITEMS.register("celestial_hoe",
            () -> new HoeItem(CelestialMaterials.TIER, -5, 0.0F, celestialGear()));

    // ---- Celestial armor (infusable) ----
    public static final RegistryObject<Item> CELESTIAL_HELMET = armor("celestial_helmet", ArmorItem.Type.HELMET);
    public static final RegistryObject<Item> CELESTIAL_CHESTPLATE = armor("celestial_chestplate", ArmorItem.Type.CHESTPLATE);
    public static final RegistryObject<Item> CELESTIAL_LEGGINGS = armor("celestial_leggings", ArmorItem.Type.LEGGINGS);
    public static final RegistryObject<Item> CELESTIAL_BOOTS = armor("celestial_boots", ArmorItem.Type.BOOTS);

    // ---- 0.0.4.5: Multitools (sword + axe + pickaxe) ----
    public static final RegistryObject<Item> IRON_MULTITOOL = multitool("iron_multitool", Tiers.IRON, new Item.Properties());
    public static final RegistryObject<Item> DIAMOND_MULTITOOL = multitool("diamond_multitool", Tiers.DIAMOND, new Item.Properties());
    public static final RegistryObject<Item> NETHERITE_MULTITOOL = multitool("netherite_multitool", Tiers.NETHERITE,
            new Item.Properties().fireResistant());
    public static final RegistryObject<Item> CELESTIAL_MULTITOOL = multitool("celestial_multitool", CelestialMaterials.TIER,
            celestialGear());

    // ---- 0.0.4.5: Scuba Gear (no protection) ----
    public static final RegistryObject<Item> SCUBA_MASK = scuba("scuba_mask", ArmorItem.Type.HELMET);
    public static final RegistryObject<Item> SCUBA_TANK = scuba("scuba_tank", ArmorItem.Type.CHESTPLATE);
    public static final RegistryObject<Item> SCUBA_WETSUIT = scuba("scuba_wetsuit", ArmorItem.Type.LEGGINGS);
    public static final RegistryObject<Item> SCUBA_FLIPPERS = scuba("scuba_flippers", ArmorItem.Type.BOOTS);


    private ModItems() {}

    private static RegistryObject<Item> charm(String name, CharmType type, Rarity rarity) {
        return ITEMS.register(name, () -> new CharmItem(type, new Item.Properties().stacksTo(1).rarity(rarity)));
    }

    private static RegistryObject<Item> relic(RelicType type, Rarity rarity) {
        return ITEMS.register(type.itemPath(), () -> new RelicItem(type, new Item.Properties().stacksTo(1).rarity(rarity)));
    }

    private static RegistryObject<Item> armor(String name, ArmorItem.Type type) {
        return ITEMS.register(name, () -> new CelestialArmorItem(type, celestialGear()));
    }

    private static RegistryObject<Item> multitool(String name, Tier tier, Item.Properties properties) {
        return ITEMS.register(name, () -> new MultitoolItem(tier, properties));
    }

    private static RegistryObject<Item> scuba(String name, ArmorItem.Type type) {
        return ITEMS.register(name, () -> new ScubaArmorItem(type, new Item.Properties()));
    }

    private static Item.Properties celestialGear() {
        return new Item.Properties().fireResistant().rarity(Rarity.RARE);
    }
}
