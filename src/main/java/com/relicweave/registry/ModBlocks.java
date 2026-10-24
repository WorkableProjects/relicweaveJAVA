package com.relicweave.registry;

import com.relicweave.RelicWeave;
import com.relicweave.block.TransmutationTableBlock;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, RelicWeave.MOD_ID);
    // Block items live here; RelicWeave 0.0.1 has no standalone custom items.
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, RelicWeave.MOD_ID);

    public static final RegistryObject<Block> TRANSMUTATION_TABLE = BLOCKS.register("transmutation_table",
            () -> new TransmutationTableBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .strength(3.5F, 6.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.AMETHYST)));

    public static final RegistryObject<Item> TRANSMUTATION_TABLE_ITEM = ITEMS.register("transmutation_table",
            () -> new BlockItem(TRANSMUTATION_TABLE.get(), new Item.Properties()));

    private ModBlocks() {}
}
