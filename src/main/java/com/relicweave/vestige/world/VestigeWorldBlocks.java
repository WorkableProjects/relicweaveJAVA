package com.relicweave.vestige.world;

import com.relicweave.RelicWeave;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Supplier;

/**
 * Terrain, ore, flora and decorative blocks of The Vestige. The core stone family is defined here up front because
 * other systems (arrival plaza, structures) build with it.
 */
public final class VestigeWorldBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, RelicWeave.MOD_ID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, RelicWeave.MOD_ID);

    // ---- Core stone family (aged ivory) ----
    public static final RegistryObject<Block> VESTIGE_STONE = block("vestige_stone", () -> new Block(stone(MapColor.SAND)));
    public static final RegistryObject<Block> VESTIGE_BRICKS = block("vestige_bricks", () -> new Block(stone(MapColor.SAND)));
    public static final RegistryObject<Block> CRACKED_VESTIGE_BRICKS = block("cracked_vestige_bricks", () -> new Block(stone(MapColor.SAND)));
    public static final RegistryObject<Block> CARVED_VESTIGE_STONE = block("carved_vestige_stone", () -> new Block(stone(MapColor.SAND)));
    /** Weathered gold inlay. */
    public static final RegistryObject<Block> GILDED_VESTIGE_BRICKS = block("gilded_vestige_bricks", () -> new Block(stone(MapColor.GOLD)));
    /** Oxidized teal rune stone; faintly lit. */
    public static final RegistryObject<Block> RUNE_VESTIGE_BRICKS = block("rune_vestige_bricks",
            () -> new Block(stone(MapColor.WARPED_STEM).lightLevel(state -> 4)));
    /** Ink-blue archive stone (Gloam Archive). */
    public static final RegistryObject<Block> GLOAM_STONE = block("gloam_stone", () -> new Block(stone(MapColor.COLOR_BLUE)));

    // T01 adds the rest of the terrain/flora/ore/decor blocks below this line.

    private VestigeWorldBlocks() {}

    public static BlockBehaviour.Properties stone(MapColor color) {
        return BlockBehaviour.Properties.of().mapColor(color).strength(2.0F, 6.0F).requiresCorrectToolForDrops().sound(SoundType.STONE);
    }

    private static <T extends Block> RegistryObject<T> block(String name, Supplier<T> block) {
        RegistryObject<T> registered = BLOCKS.register(name, block);
        ITEMS.register(name, () -> new BlockItem(registered.get(), new Item.Properties()));
        return registered;
    }
}
