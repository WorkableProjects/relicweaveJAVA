package com.relicweave.vestige.gear;

import com.relicweave.RelicWeave;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Aetherforged gear and the Reliquary Loom (T14). */
public final class GearRegistry {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, RelicWeave.MOD_ID);
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, RelicWeave.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, RelicWeave.MOD_ID);
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, RelicWeave.MOD_ID);

    // ---- Aetherforged tools ----
    public static final RegistryObject<Item> AETHERFORGED_SWORD = ITEMS.register("aetherforged_sword",
            () -> new AetherforgedItems.Sword(3, -2.4F, gear()));
    public static final RegistryObject<Item> AETHERFORGED_PICKAXE = ITEMS.register("aetherforged_pickaxe",
            () -> new AetherforgedItems.Pickaxe(1, -2.8F, gear()));
    public static final RegistryObject<Item> AETHERFORGED_AXE = ITEMS.register("aetherforged_axe",
            () -> new AetherforgedItems.Axe(5.0F, -3.0F, gear()));
    public static final RegistryObject<Item> AETHERFORGED_SHOVEL = ITEMS.register("aetherforged_shovel",
            () -> new AetherforgedItems.Shovel(1.5F, -3.0F, gear()));
    public static final RegistryObject<Item> AETHERFORGED_HOE = ITEMS.register("aetherforged_hoe",
            () -> new AetherforgedItems.Hoe(-4, 0.0F, gear()));

    // ---- Aetherforged armor ----
    public static final RegistryObject<Item> AETHERFORGED_HELMET = ITEMS.register("aetherforged_helmet",
            () -> new AetherforgedItems.Armor(ArmorItem.Type.HELMET, gear()));
    public static final RegistryObject<Item> AETHERFORGED_CHESTPLATE = ITEMS.register("aetherforged_chestplate",
            () -> new AetherforgedItems.Armor(ArmorItem.Type.CHESTPLATE, gear()));
    public static final RegistryObject<Item> AETHERFORGED_LEGGINGS = ITEMS.register("aetherforged_leggings",
            () -> new AetherforgedItems.Armor(ArmorItem.Type.LEGGINGS, gear()));
    public static final RegistryObject<Item> AETHERFORGED_BOOTS = ITEMS.register("aetherforged_boots",
            () -> new AetherforgedItems.Armor(ArmorItem.Type.BOOTS, gear()));

    // ---- Reliquary Loom ----
    public static final RegistryObject<Block> RELIQUARY_LOOM = BLOCKS.register("reliquary_loom", () -> new ReliquaryLoomBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.GOLD).strength(3.5F).sound(SoundType.WOOD)));
    public static final RegistryObject<Item> RELIQUARY_LOOM_ITEM = ITEMS.register("reliquary_loom",
            () -> new BlockItem(RELIQUARY_LOOM.get(), new Item.Properties().rarity(Rarity.UNCOMMON)));
    public static final RegistryObject<BlockEntityType<ReliquaryLoomBlockEntity>> RELIQUARY_LOOM_BE = BLOCK_ENTITIES.register("reliquary_loom",
            () -> BlockEntityType.Builder.of(ReliquaryLoomBlockEntity::new, RELIQUARY_LOOM.get()).build(null));
    public static final RegistryObject<MenuType<ReliquaryLoomMenu>> RELIQUARY_LOOM_MENU = MENUS.register("reliquary_loom",
            () -> IForgeMenuType.create(ReliquaryLoomMenu::new));

    private GearRegistry() {}

    private static Item.Properties gear() {
        return new Item.Properties().fireResistant().rarity(Rarity.RARE);
    }
}
