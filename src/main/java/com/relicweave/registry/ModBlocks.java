package com.relicweave.registry;

import com.relicweave.RelicWeave;
import com.relicweave.block.TransmutationTableBlock;
import com.relicweave.block.furnace.SuperFurnaceBlock;
import com.relicweave.block.furnace.SuperFurnaceTier;
import com.relicweave.block.infuser.CelestialInfuserBlock;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.List;
import java.util.function.Supplier;

public final class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, RelicWeave.MOD_ID);
    /** Block items. Standalone items live in {@link ModItems}. */
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, RelicWeave.MOD_ID);

    public static final RegistryObject<Block> TRANSMUTATION_TABLE = BLOCKS.register("transmutation_table",
            () -> new TransmutationTableBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .strength(3.5F, 6.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.AMETHYST)));
    public static final RegistryObject<Item> TRANSMUTATION_TABLE_ITEM = blockItem("transmutation_table", TRANSMUTATION_TABLE, Rarity.COMMON);

    /** Celestial ore in End Stone (common in the End). */
    public static final RegistryObject<Block> CELESTIAL_ORE = BLOCKS.register("celestial_ore",
            () -> new DropExperienceBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.SAND)
                    .strength(30.0F, 1200.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.ANCIENT_DEBRIS), UniformInt.of(4, 8)));
    public static final RegistryObject<Item> CELESTIAL_ORE_ITEM = blockItem("celestial_ore", CELESTIAL_ORE, Rarity.UNCOMMON);

    /** Celestial ore in Netherrack (rarer, in the Nether). */
    public static final RegistryObject<Block> NETHER_CELESTIAL_ORE = BLOCKS.register("nether_celestial_ore",
            () -> new DropExperienceBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.NETHER)
                    .strength(30.0F, 1200.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.ANCIENT_DEBRIS), UniformInt.of(4, 8)));
    public static final RegistryObject<Item> NETHER_CELESTIAL_ORE_ITEM = blockItem("nether_celestial_ore", NETHER_CELESTIAL_ORE, Rarity.UNCOMMON);

    public static final RegistryObject<Block> CELESTIAL_BLOCK = BLOCKS.register("celestial_block",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_LIGHT_BLUE)
                    .strength(50.0F, 1200.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.NETHERITE_BLOCK)));
    public static final RegistryObject<Item> CELESTIAL_BLOCK_ITEM = blockItem("celestial_block", CELESTIAL_BLOCK, Rarity.RARE);

    public static final RegistryObject<Block> CELESTIAL_INFUSER = BLOCKS.register("celestial_infuser",
            () -> new CelestialInfuserBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_LIGHT_BLUE)
                    .strength(5.0F, 1200.0F)
                    .requiresCorrectToolForDrops()
                    .lightLevel(state -> 7)
                    .sound(SoundType.NETHERITE_BLOCK)));
    public static final RegistryObject<Item> CELESTIAL_INFUSER_ITEM = blockItem("celestial_infuser", CELESTIAL_INFUSER, Rarity.RARE);

    // ---- 0.0.4.5: Super Furnaces, one block per tier ----
    public static final RegistryObject<Block> IRON_SUPER_FURNACE = superFurnace(SuperFurnaceTier.IRON, MapColor.METAL, 5.0F, 6.0F, SoundType.METAL);
    public static final RegistryObject<Block> GOLD_SUPER_FURNACE = superFurnace(SuperFurnaceTier.GOLD, MapColor.GOLD, 5.0F, 6.0F, SoundType.METAL);
    public static final RegistryObject<Block> DIAMOND_SUPER_FURNACE = superFurnace(SuperFurnaceTier.DIAMOND, MapColor.DIAMOND, 5.0F, 6.0F, SoundType.METAL);
    public static final RegistryObject<Block> NETHERITE_SUPER_FURNACE = superFurnace(SuperFurnaceTier.NETHERITE, MapColor.COLOR_BLACK, 50.0F, 1200.0F, SoundType.NETHERITE_BLOCK);
    public static final RegistryObject<Block> CELESTIAL_SUPER_FURNACE = superFurnace(SuperFurnaceTier.CELESTIAL, MapColor.COLOR_LIGHT_BLUE, 50.0F, 1200.0F, SoundType.NETHERITE_BLOCK);
    public static final List<RegistryObject<Block>> SUPER_FURNACES = List.of(IRON_SUPER_FURNACE, GOLD_SUPER_FURNACE,
            DIAMOND_SUPER_FURNACE, NETHERITE_SUPER_FURNACE, CELESTIAL_SUPER_FURNACE);


    private ModBlocks() {}

    private static RegistryObject<Block> superFurnace(SuperFurnaceTier tier, MapColor color, float hardness, float resistance, SoundType sound) {
        RegistryObject<Block> block = BLOCKS.register(tier.blockName(), () -> new SuperFurnaceBlock(tier, BlockBehaviour.Properties.of()
                .mapColor(color)
                .strength(hardness, resistance)
                .requiresCorrectToolForDrops()
                .lightLevel(state -> state.getValue(AbstractFurnaceBlock.LIT) ? 13 : 0)
                .sound(sound)));
        boolean fireResistant = tier.level() >= SuperFurnaceTier.NETHERITE.level();
        ITEMS.register(tier.blockName(), () -> {
            Item.Properties properties = new Item.Properties().rarity(tier.rarity());
            return new BlockItem(block.get(), fireResistant ? properties.fireResistant() : properties);
        });
        return block;
    }

    private static RegistryObject<Item> blockItem(String name, Supplier<Block> block, Rarity rarity) {
        boolean celestial = name.contains("celestial");
        return ITEMS.register(name, () -> {
            Item.Properties properties = new Item.Properties().rarity(rarity);
            return new BlockItem(block.get(), celestial ? properties.fireResistant() : properties);
        });
    }
}
