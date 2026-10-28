package com.relicweave.gametest;

import com.relicweave.RelicWeave;
import com.relicweave.vestige.VestigeKeys;
import com.relicweave.vestige.registry.VestigeMaterials;
import com.relicweave.vestige.world.VestigeWorldBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** Server-side tests for The Vestige dimension, its eight biomes and the terrain block family. */
@GameTestHolder(RelicWeave.MOD_ID)
@PrefixGameTestTemplate(false)
public final class VestigeWorldGameTests {
    private static final org.slf4j.Logger LOGGER = com.mojang.logging.LogUtils.getLogger();
    private static final String TEMPLATE = "platform";

    private static final List<ResourceKey<Biome>> BIOMES = List.of(
            VestigeKeys.CROWNFALL_BASIN, VestigeKeys.GLASSROOT_WILDS, VestigeKeys.AURELIAN_CANOPY, VestigeKeys.HUSHFEN,
            VestigeKeys.SUNDERED_REACH, VestigeKeys.STARFALL_CALDERA, VestigeKeys.GLOAM_ARCHIVE, VestigeKeys.CROWN_OF_CONCORDANCE);

    private VestigeWorldGameTests() {}

    private static ServerLevel vestige(GameTestHelper helper) {
        ServerLevel level = helper.getLevel().getServer().getLevel(VestigeKeys.THE_VESTIGE);
        helper.assertTrue(level != null, "The Vestige dimension should be loaded");
        return level;
    }

    private static boolean isVanillaSettings(ServerLevel level, ResourceKey<NoiseGeneratorSettings> expected) {
        ChunkGenerator generator = level.getChunkSource().getGenerator();
        return generator instanceof NoiseBasedChunkGenerator noise && noise.generatorSettings().is(expected);
    }

    @GameTest(template = TEMPLATE)
    public static void vestigeDimensionLoads(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        ServerLevel level = vestige(helper);
        helper.assertTrue(level.dimensionTypeId().equals(VestigeKeys.THE_VESTIGE_TYPE),
                "Wrong dimension type: " + level.dimensionTypeId());
        helper.assertTrue(level.getMinBuildHeight() == -64 && level.getHeight() == 384, "Height range should be -64..320");
        helper.assertTrue(isVanillaSettings(server.getLevel(Level.OVERWORLD), NoiseGeneratorSettings.OVERWORLD), "Overworld settings changed");
        helper.assertTrue(isVanillaSettings(server.getLevel(Level.NETHER), NoiseGeneratorSettings.NETHER), "Nether settings changed");
        helper.assertTrue(isVanillaSettings(server.getLevel(Level.END), NoiseGeneratorSettings.END), "End settings changed");
        helper.assertFalse(isVanillaSettings(level, NoiseGeneratorSettings.OVERWORLD), "Vestige must not reuse overworld settings");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void allEightBiomesRegisteredAndTagged(GameTestHelper helper) {
        var registry = helper.getLevel().registryAccess().registryOrThrow(Registries.BIOME);
        for (ResourceKey<Biome> key : BIOMES) {
            helper.assertTrue(registry.containsKey(key), "Biome missing: " + key.location());
            helper.assertTrue(registry.getHolderOrThrow(key).is(VestigeKeys.IS_VESTIGE), "Biome not in is_vestige: " + key.location());
        }
        helper.assertTrue(registry.getHolderOrThrow(VestigeKeys.STARFALL_CALDERA).is(VestigeKeys.RIFT_SATURATED), "Caldera is rift saturated");
        helper.assertTrue(registry.getHolderOrThrow(VestigeKeys.CROWN_OF_CONCORDANCE).is(VestigeKeys.RIFT_SATURATED), "Crown is rift saturated");
        helper.assertTrue(registry.getHolderOrThrow(VestigeKeys.SUNDERED_REACH).is(VestigeKeys.GRAVITY_SHEAR), "Reach has gravity shear");
        helper.assertTrue(registry.getHolderOrThrow(VestigeKeys.STARFALL_CALDERA).is(VestigeKeys.STARFALL), "Caldera has starfall");
        helper.assertTrue(registry.getHolderOrThrow(VestigeKeys.HUSHFEN).is(VestigeKeys.HUSHFIELD), "Hushfen has hushfield");
        helper.assertTrue(registry.getHolderOrThrow(VestigeKeys.GLOAM_ARCHIVE).is(VestigeKeys.HUSHFIELD), "Gloam Archive has hushfield");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void vestigeBiomeSourceOnlyVestigeBiomes(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        Set<ResourceKey<Biome>> vestige = vestige(helper).getChunkSource().getGenerator().getBiomeSource().possibleBiomes().stream()
                .map(holder -> holder.unwrapKey().orElseThrow()).collect(Collectors.toSet());
        helper.assertTrue(vestige.equals(Set.copyOf(BIOMES)), "Vestige biome source should hold exactly the 8 biomes, got " + vestige);
        for (Holder<Biome> holder : server.getLevel(Level.OVERWORLD).getChunkSource().getGenerator().getBiomeSource().possibleBiomes()) {
            helper.assertFalse(holder.is(VestigeKeys.IS_VESTIGE), "Overworld must not contain Vestige biome " + holder.unwrapKey());
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 400)
    public static void allBiomesAppearWithinReach(GameTestHelper helper) {
        ServerLevel level = vestige(helper);
        BiomeSource source = level.getChunkSource().getGenerator().getBiomeSource();
        Climate.Sampler sampler = level.getChunkSource().randomState().sampler();
        Map<ResourceKey<Biome>, Integer> counts = new HashMap<>();
        for (int x = -6144; x <= 6144; x += 128) {
            for (int z = -6144; z <= 6144; z += 128) {
                Holder<Biome> biome = source.getNoiseBiome(x >> 2, 64 >> 2, z >> 2, sampler);
                counts.merge(biome.unwrapKey().orElseThrow(), 1, Integer::sum);
            }
        }
        LOGGER.info("Vestige biome sample counts: {}", counts);
        for (ResourceKey<Biome> key : BIOMES) {
            helper.assertTrue(counts.getOrDefault(key, 0) > 0, "Biome never sampled within +-6144 blocks: " + key.location());
        }
        // The world seed differs per run, so exact ordering between biomes varies; Crown of Concordance must still be
        // rare: under half of an even share (1/16 of all samples).
        int crown = counts.get(VestigeKeys.CROWN_OF_CONCORDANCE);
        int total = counts.values().stream().mapToInt(Integer::intValue).sum();
        helper.assertTrue(crown * 16 < total, "Crown of Concordance should be rare: " + crown + " of " + total + " samples");
        helper.succeed();
    }

    /** Generates real chunks in every biome and checks that the data-driven surface rules and features run without errors. */
    @GameTest(template = TEMPLATE, timeoutTicks = 1200)
    public static void biomesGenerateWithTheirSurfaces(GameTestHelper helper) {
        ServerLevel level = vestige(helper);
        BiomeSource source = level.getChunkSource().getGenerator().getBiomeSource();
        Climate.Sampler sampler = level.getChunkSource().randomState().sampler();
        Map<ResourceKey<Biome>, List<BlockPos>> candidates = new HashMap<>();
        for (int x = -4096; x <= 4096; x += 128) {
            for (int z = -4096; z <= 4096; z += 128) {
                ResourceKey<Biome> key = source.getNoiseBiome(x >> 2, 64 >> 2, z >> 2, sampler).unwrapKey().orElseThrow();
                candidates.computeIfAbsent(key, k -> new ArrayList<>()).add(new BlockPos(x, 64, z));
            }
        }
        Map<ResourceKey<Biome>, Set<Block>> expected = Map.of(
                VestigeKeys.CROWNFALL_BASIN, Set.of(VestigeWorldBlocks.AUREATE_MOSS.get(), VestigeWorldBlocks.CONCORDANT_PAVING.get()),
                VestigeKeys.GLASSROOT_WILDS, Set.of(VestigeWorldBlocks.AUREATE_MOSS.get()),
                VestigeKeys.AURELIAN_CANOPY, Set.of(VestigeWorldBlocks.AUREATE_MOSS.get()),
                VestigeKeys.HUSHFEN, Set.of(VestigeWorldBlocks.ECHO_CLAY.get(), Blocks.MUD),
                VestigeKeys.SUNDERED_REACH, Set.of(VestigeWorldBlocks.FRACTURED_VESTIGE_STONE.get()),
                VestigeKeys.STARFALL_CALDERA, Set.of(VestigeWorldBlocks.STARFALL_SLAG.get(), Blocks.BASALT),
                VestigeKeys.GLOAM_ARCHIVE, Set.of(VestigeWorldBlocks.GLOAM_STONE.get()),
                VestigeKeys.CROWN_OF_CONCORDANCE, Set.of(VestigeWorldBlocks.CONCORDANT_PAVING.get(), VestigeWorldBlocks.VESTIGE_BRICKS.get()));
        for (ResourceKey<Biome> biome : BIOMES) {
            List<BlockPos> all = candidates.getOrDefault(biome, List.of());
            boolean found = false;
            for (int i = 0; i < 4 && !found && !all.isEmpty(); i++) {
                BlockPos pos = all.get(i * all.size() / 4);
                LevelChunk chunk = level.getChunk(pos.getX() >> 4, pos.getZ() >> 4);
                for (int dx = 0; dx < 16 && !found; dx++) {
                    for (int dz = 0; dz < 16 && !found; dz++) {
                        int y = chunk.getHeight(Heightmap.Types.WORLD_SURFACE_WG, dx, dz);
                        found = expected.get(biome).contains(chunk.getBlockState(new BlockPos(dx, y, dz)).getBlock());
                    }
                }
            }
            helper.assertTrue(found, "No expected surface block generated for " + biome.location());
        }
        helper.succeed();
    }

    private static List<ItemStack> drops(GameTestHelper helper, BlockState state, ItemStack tool) {
        LootParams.Builder builder = new LootParams.Builder(helper.getLevel())
                .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(helper.absolutePos(new BlockPos(2, 2, 2))))
                .withParameter(LootContextParams.TOOL, tool);
        return state.getDrops(builder);
    }

    @GameTest(template = TEMPLATE)
    public static void aetheriteOreDropsShards(GameTestHelper helper) {
        BlockState ore = VestigeWorldBlocks.AETHERITE_ORE.get().defaultBlockState();
        List<ItemStack> shards = drops(helper, ore, new ItemStack(Items.IRON_PICKAXE));
        helper.assertTrue(!shards.isEmpty() && shards.stream().allMatch(s -> s.is(VestigeMaterials.AETHERITE_SHARD.get())),
                "Iron pickaxe should drop aetherite shards, got " + shards);
        int total = shards.stream().mapToInt(ItemStack::getCount).sum();
        helper.assertTrue(total >= 1 && total <= 2, "Unenchanted ore drops 1-2 shards, got " + total);

        ItemStack silk = new ItemStack(Items.IRON_PICKAXE);
        silk.enchant(net.minecraft.world.item.enchantment.Enchantments.SILK_TOUCH, 1);
        List<ItemStack> silked = drops(helper, ore, silk);
        helper.assertTrue(silked.size() == 1 && silked.get(0).is(VestigeWorldBlocks.AETHERITE_ORE.get().asItem()), "Silk touch drops the ore");
        helper.assertTrue(ore.requiresCorrectToolForDrops(), "Aetherite ore needs a correct tool");
        helper.assertTrue(ore.is(net.minecraft.tags.BlockTags.NEEDS_IRON_TOOL), "Aetherite ore needs an iron tool");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void everyWorldBlockDropsSomething(GameTestHelper helper) {
        ItemStack pick = new ItemStack(Items.NETHERITE_PICKAXE);
        ItemStack axe = new ItemStack(Items.NETHERITE_AXE);
        ItemStack shovel = new ItemStack(Items.NETHERITE_SHOVEL);
        int checked = 0;
        for (RegistryObject<Block> entry : VestigeWorldBlocks.BLOCKS.getEntries()) {
            Block block = entry.get();
            if (block instanceof LeavesBlock || block instanceof BushBlock) {
                continue;
            }
            String path = entry.getId().getPath();
            ItemStack tool = path.contains("log") || path.contains("planks") ? axe : path.equals("echo_clay") || path.equals("aureate_moss") ? shovel : pick;
            List<ItemStack> result = drops(helper, block.defaultBlockState(), tool);
            helper.assertTrue(!result.isEmpty(), path + " dropped nothing");
            checked++;
        }
        helper.assertTrue(checked >= 25, "Expected at least 25 world blocks, checked " + checked);
        helper.assertTrue(drops(helper, VestigeWorldBlocks.AUREATE_MOSS.get().defaultBlockState(), shovel).get(0)
                .is(VestigeWorldBlocks.VESTIGE_STONE.get().asItem()), "Aureate moss drops Vestige Stone without silk touch");
        helper.succeed();
    }
}
