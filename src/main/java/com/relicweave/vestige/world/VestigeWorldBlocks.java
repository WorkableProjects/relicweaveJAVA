package com.relicweave.vestige.world;

import com.relicweave.RelicWeave;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.GlassBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.material.PushReaction;
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

    // ---- Stone family extras ----
    public static final RegistryObject<Block> POLISHED_VESTIGE_STONE = block("polished_vestige_stone", () -> new Block(stone(MapColor.SAND)));
    public static final RegistryObject<Block> VESTIGE_PILLAR = block("vestige_pillar", () -> new RotatedPillarBlock(stone(MapColor.SAND)));
    public static final RegistryObject<Block> VESTIGE_BRICK_STAIRS = block("vestige_brick_stairs",
            () -> new StairBlock(() -> VESTIGE_BRICKS.get().defaultBlockState(), stone(MapColor.SAND)));
    public static final RegistryObject<Block> VESTIGE_BRICK_SLAB = block("vestige_brick_slab", () -> new SlabBlock(stone(MapColor.SAND)));
    public static final RegistryObject<Block> VESTIGE_BRICK_WALL = block("vestige_brick_wall", () -> new WallBlock(stone(MapColor.SAND)));
    public static final RegistryObject<Block> GLOAM_BRICKS = block("gloam_bricks", () -> new Block(stone(MapColor.COLOR_BLUE)));
    public static final RegistryObject<Block> CONCORDANT_PAVING = block("concordant_paving", () -> new Block(stone(MapColor.GOLD)));

    // ---- Surface blocks ----
    /** Top block of Crownfall Basin and Aurelian Canopy; drops Vestige Stone unless silk touched. */
    public static final RegistryObject<Block> AUREATE_MOSS = block("aureate_moss", () -> new Block(BlockBehaviour.Properties.of()
            .mapColor(MapColor.GOLD).strength(0.8F).sound(SoundType.MOSS)));
    public static final RegistryObject<Block> ECHO_CLAY = block("echo_clay", () -> new Block(BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_LIGHT_BLUE).strength(0.7F).sound(SoundType.PACKED_MUD)));
    public static final RegistryObject<Block> STARFALL_SLAG = block("starfall_slag", () -> new Block(
            stone(MapColor.COLOR_GRAY).strength(1.5F, 4.0F)));
    public static final RegistryObject<Block> FRACTURED_VESTIGE_STONE = block("fractured_vestige_stone", () -> new Block(
            stone(MapColor.SAND).strength(1.5F, 6.0F)));

    // ---- Ores and crystal ----
    public static final RegistryObject<Block> AETHERITE_ORE = block("aetherite_ore", () -> new DropExperienceBlock(
            stone(MapColor.SAND).strength(3.0F, 3.0F), UniformInt.of(2, 5)));
    public static final RegistryObject<Block> RIFTGLASS_CLUSTER = block("riftglass_cluster", () -> new AmethystClusterBlock(7, 3,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).forceSolidOn().noOcclusion().randomTicks()
                    .sound(SoundType.AMETHYST_CLUSTER).strength(1.5F).lightLevel(state -> 5).pushReaction(PushReaction.DESTROY)));
    public static final RegistryObject<Block> RIFTGLASS_BLOCK = block("riftglass_block", () -> new GlassBlock(glass(MapColor.COLOR_PURPLE)));

    // ---- Flora and wood ----
    public static final RegistryObject<Block> GLASSROOT_LOG = block("glassroot_log", () -> new RotatedPillarBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.QUARTZ).strength(2.0F).sound(SoundType.AMETHYST)));
    public static final RegistryObject<Block> GLASSROOT_CRYSTAL = block("glassroot_crystal", () -> new GlassBlock(
            glass(MapColor.COLOR_LIGHT_BLUE).lightLevel(state -> 8)));
    public static final RegistryObject<Block> GLASSROOT_SPROUT = block("glassroot_sprout", () -> new VestigePlantBlock(plant(MapColor.COLOR_CYAN),
            VestigeWorldBlocks::isNaturalGround));
    public static final RegistryObject<Block> CANOPY_LOG = block("canopy_log", () -> new RotatedPillarBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.GOLD).strength(2.0F).sound(SoundType.WOOD)));
    public static final RegistryObject<Block> CANOPY_PLANKS = block("canopy_planks", () -> new Block(BlockBehaviour.Properties.of()
            .mapColor(MapColor.GOLD).strength(2.0F, 3.0F).sound(SoundType.WOOD)));
    public static final RegistryObject<Block> CANOPY_LEAVES = block("canopy_leaves", () -> new LeavesBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_YELLOW).strength(0.2F).randomTicks().sound(SoundType.AZALEA_LEAVES).noOcclusion()
            .lightLevel(state -> 6).isValidSpawn((s, l, p, e) -> false).isSuffocating((s, l, p) -> false)
            .isViewBlocking((s, l, p) -> false).isRedstoneConductor((s, l, p) -> false).pushReaction(PushReaction.DESTROY)));
    public static final RegistryObject<Block> AURELIAN_BLOOM = block("aurelian_bloom", () -> new VestigePlantBlock(plant(MapColor.GOLD),
            VestigeWorldBlocks::isNaturalGround));
    public static final RegistryObject<Block> HUSHFEN_REED = block("hushfen_reed", () -> new VestigePlantBlock(plant(MapColor.COLOR_LIGHT_GREEN),
            state -> state.is(ECHO_CLAY.get()) || state.is(Blocks.MUD) || state.is(AUREATE_MOSS.get()) || state.is(Blocks.MOSS_BLOCK)
                    || state.is(Blocks.CLAY) || state.is(Blocks.DIRT)));

    private VestigeWorldBlocks() {}

    public static BlockBehaviour.Properties stone(MapColor color) {
        return BlockBehaviour.Properties.of().mapColor(color).strength(2.0F, 6.0F).requiresCorrectToolForDrops().sound(SoundType.STONE);
    }

    private static BlockBehaviour.Properties glass(MapColor color) {
        return BlockBehaviour.Properties.of().mapColor(color).strength(0.8F).sound(SoundType.GLASS).noOcclusion()
                .isValidSpawn((s, l, p, e) -> false).isRedstoneConductor((s, l, p) -> false)
                .isSuffocating((s, l, p) -> false).isViewBlocking((s, l, p) -> false);
    }

    private static BlockBehaviour.Properties plant(MapColor color) {
        return BlockBehaviour.Properties.of().mapColor(color).noCollission().instabreak().sound(SoundType.GRASS)
                .offsetType(BlockBehaviour.OffsetType.XZ).pushReaction(PushReaction.DESTROY);
    }

    /** Ground that small Vestige plants can stand on. */
    private static boolean isNaturalGround(net.minecraft.world.level.block.state.BlockState state) {
        return state.is(AUREATE_MOSS.get()) || state.is(VESTIGE_STONE.get()) || state.is(FRACTURED_VESTIGE_STONE.get())
                || state.is(ECHO_CLAY.get()) || state.is(Blocks.MOSS_BLOCK) || state.is(Blocks.MUD);
    }

    private static <T extends Block> RegistryObject<T> block(String name, Supplier<T> block) {
        RegistryObject<T> registered = BLOCKS.register(name, block);
        ITEMS.register(name, () -> new BlockItem(registered.get(), new Item.Properties()));
        return registered;
    }
}
