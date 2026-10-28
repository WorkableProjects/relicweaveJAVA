package com.relicweave.vestige.gate;

import com.relicweave.RelicWeave;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Concordance Gate blocks, block entities and items (T02). */
public final class GateRegistry {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, RelicWeave.MOD_ID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, RelicWeave.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, RelicWeave.MOD_ID);

    public static final RegistryObject<Block> CONCORDANCE_FRAME = BLOCKS.register("concordance_frame",
            () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE)
                    .strength(3.0F, 9.0F).requiresCorrectToolForDrops().sound(SoundType.AMETHYST)));

    public static final RegistryObject<Block> GATE_KEYSTONE = BLOCKS.register("gate_keystone",
            () -> new GateKeystoneBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE)
                    .strength(5.0F, 1200.0F).requiresCorrectToolForDrops().sound(SoundType.AMETHYST)
                    .lightLevel(state -> state.getValue(GateKeystoneBlock.ACTIVE) ? 10 : 3)));

    public static final RegistryObject<Block> RETURN_WAYSTONE = BLOCKS.register("return_waystone",
            () -> new ReturnWaystoneBlock(BlockBehaviour.Properties.of().mapColor(MapColor.SAND)
                    .strength(-1.0F, 3600000.0F).sound(SoundType.STONE).lightLevel(state -> 7)));

    public static final RegistryObject<Item> CONCORDANCE_FRAME_ITEM = ITEMS.register("concordance_frame",
            () -> new BlockItem(CONCORDANCE_FRAME.get(), new Item.Properties()));
    public static final RegistryObject<Item> GATE_KEYSTONE_ITEM = ITEMS.register("gate_keystone",
            () -> new BlockItem(GATE_KEYSTONE.get(), new Item.Properties().rarity(Rarity.UNCOMMON)));
    public static final RegistryObject<Item> RETURN_WAYSTONE_ITEM = ITEMS.register("return_waystone",
            () -> new BlockItem(RETURN_WAYSTONE.get(), new Item.Properties().rarity(Rarity.RARE)));
    public static final RegistryObject<Item> THRESHOLD_SIGIL = ITEMS.register("threshold_sigil",
            () -> new ThresholdSigilItem(new Item.Properties().stacksTo(16).rarity(Rarity.RARE)));

    public static final RegistryObject<BlockEntityType<GateKeystoneBlockEntity>> GATE_KEYSTONE_BE = BLOCK_ENTITIES.register("gate_keystone",
            () -> BlockEntityType.Builder.of(GateKeystoneBlockEntity::new, GATE_KEYSTONE.get()).build(null));

    private GateRegistry() {}
}
