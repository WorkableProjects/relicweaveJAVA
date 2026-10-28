package com.relicweave.gametest;

import com.relicweave.RelicWeave;
import com.relicweave.vestige.VestigeKeys;
import com.relicweave.vestige.gate.ArrivalPlaza;
import com.relicweave.vestige.gate.GateRegistry;
import com.relicweave.vestige.gate.SafeSpot;
import com.relicweave.vestige.gate.VestigeWorldData;
import com.relicweave.vestige.world.VestigeWorldBlocks;
import com.relicweave.vestige.world.feature.VestigeFeatures;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.BlockStateConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/**
 * Tests for the Vestige landmark features (they must place and stay within 16 blocks of their origin) and for Arrival
 * Plaza v2 (safe, idempotent, upgraded once from version 1). Features are placed in The Vestige on a flat stone
 * platform far from spawn so they cannot disturb other tests.
 */
@GameTestHolder(RelicWeave.MOD_ID)
@PrefixGameTestTemplate(false)
public final class VestigeFeatureGameTests {
    private static final String TEMPLATE = "platform";
    private static final int SITE_X = 40000;
    private static final int SITE_Z = 40000;
    private static final int PLATFORM_Y = 200;
    private static final int SCAN = 20;
    private static final int SCAN_TOP = 262;

    private VestigeFeatureGameTests() {}

    // ---- Features ----

    @GameTest(template = TEMPLATE, timeoutTicks = 600)
    public static void floatingRuinPlacesWithinLimits(GameTestHelper helper) {
        check(helper, VestigeFeatures.FLOATING_RUIN.get(), NoneFeatureConfiguration.INSTANCE, "floating_ruin");
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 600)
    public static void brokenArchPlacesWithinLimits(GameTestHelper helper) {
        check(helper, VestigeFeatures.BROKEN_ARCH.get(), NoneFeatureConfiguration.INSTANCE, "broken_arch");
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 600)
    public static void crystalSpirePlacesWithinLimits(GameTestHelper helper) {
        check(helper, VestigeFeatures.CRYSTAL_SPIRE.get(),
                new BlockStateConfiguration(VestigeWorldBlocks.GLASSROOT_CRYSTAL.get().defaultBlockState()), "crystal_spire");
        check(helper, VestigeFeatures.CRYSTAL_SPIRE.get(),
                new BlockStateConfiguration(VestigeWorldBlocks.RIFTGLASS_BLOCK.get().defaultBlockState()), "crystal_spire_rift");
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 600)
    public static void fallenStatuePlacesWithinLimits(GameTestHelper helper) {
        check(helper, VestigeFeatures.FALLEN_STATUE.get(), NoneFeatureConfiguration.INSTANCE, "fallen_statue");
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 600)
    public static void ruinedColonnadePlacesWithinLimits(GameTestHelper helper) {
        check(helper, VestigeFeatures.RUINED_COLONNADE.get(),
                new BlockStateConfiguration(VestigeWorldBlocks.VESTIGE_PILLAR.get().defaultBlockState()), "ruined_colonnade");
        check(helper, VestigeFeatures.RUINED_COLONNADE.get(),
                new BlockStateConfiguration(VestigeWorldBlocks.GLOAM_BRICKS.get().defaultBlockState()), "ruined_colonnade_gloam");
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 600)
    public static void rootArchPlacesWithinLimits(GameTestHelper helper) {
        check(helper, VestigeFeatures.ROOT_ARCH.get(), NoneFeatureConfiguration.INSTANCE, "root_arch");
    }

    /** Places the feature with three seeds on a clean platform and verifies success, change and the +-16 write limit. */
    private static <C extends FeatureConfiguration> void check(GameTestHelper helper, Feature<C> feature, C config, String name) {
        ServerLevel vestige = helper.getLevel().getServer().getLevel(VestigeKeys.THE_VESTIGE);
        helper.assertTrue(vestige != null, "The Vestige dimension must exist");
        ChunkGenerator generator = vestige.getChunkSource().getGenerator();
        BlockPos origin = new BlockPos(SITE_X, PLATFORM_Y + 1, SITE_Z);
        for (int seed = 0; seed < 3; seed++) {
            prepareSite(vestige);
            BlockState[] before = snapshot(vestige, origin);
            boolean placed = feature.place(config, vestige, generator, RandomSource.create(1234L + seed * 77L), origin);
            helper.assertTrue(placed, name + " should place on flat solid ground (seed " + seed + ")");
            BlockState[] after = snapshot(vestige, origin);
            int changed = 0;
            int index = 0;
            for (int dx = -SCAN; dx <= SCAN; dx++) {
                for (int dz = -SCAN; dz <= SCAN; dz++) {
                    for (int y = PLATFORM_Y - 5; y <= SCAN_TOP; y++) {
                        if (before[index] != after[index]) {
                            changed++;
                            helper.assertTrue(Math.abs(dx) <= 16 && Math.abs(dz) <= 16,
                                    name + " wrote outside +-16 blocks at offset " + dx + "," + dz + " (seed " + seed + ")");
                        }
                        index++;
                    }
                }
            }
            helper.assertTrue(changed > 10, name + " should write a meaningful number of blocks, wrote " + changed);
        }
        prepareSite(vestige);
        helper.succeed();
    }

    /** Clears the scan box and lays a stone platform whose top is at {@link #PLATFORM_Y}. */
    private static void prepareSite(ServerLevel vestige) {
        BlockState air = Blocks.AIR.defaultBlockState();
        BlockState stone = VestigeWorldBlocks.VESTIGE_STONE.get().defaultBlockState();
        for (int dx = -SCAN; dx <= SCAN; dx++) {
            for (int dz = -SCAN; dz <= SCAN; dz++) {
                for (int y = PLATFORM_Y - 5; y <= SCAN_TOP; y++) {
                    BlockPos pos = new BlockPos(SITE_X + dx, y, SITE_Z + dz);
                    BlockState want = y <= PLATFORM_Y ? stone : air;
                    if (vestige.getBlockState(pos) != want) {
                        vestige.setBlock(pos, want, Block.UPDATE_CLIENTS);
                    }
                }
            }
        }
    }

    private static BlockState[] snapshot(ServerLevel vestige, BlockPos origin) {
        int height = SCAN_TOP - (PLATFORM_Y - 5) + 1;
        BlockState[] states = new BlockState[(2 * SCAN + 1) * (2 * SCAN + 1) * height];
        int index = 0;
        for (int dx = -SCAN; dx <= SCAN; dx++) {
            for (int dz = -SCAN; dz <= SCAN; dz++) {
                for (int y = PLATFORM_Y - 5; y <= SCAN_TOP; y++) {
                    states[index++] = vestige.getBlockState(new BlockPos(origin.getX() + dx, y, origin.getZ() + dz));
                }
            }
        }
        return states;
    }

    // ---- Arrival Plaza v2 ----

    @GameTest(template = TEMPLATE, timeoutTicks = 600)
    public static void plazaV2IsSafeIdempotentAndHasPath(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        ServerLevel vestige = ArrivalPlaza.vestige(server);
        helper.assertTrue(vestige != null, "The Vestige dimension must exist");
        BlockPos first = ArrivalPlaza.arrival(server);
        BlockPos second = ArrivalPlaza.arrival(server);
        helper.assertTrue(first != null && first.equals(second), "Arrival must be idempotent: " + first + " vs " + second);
        helper.assertTrue(SafeSpot.isSafe(vestige, first), "Arrival spot must be safe");
        VestigeWorldData data = VestigeWorldData.get(vestige);
        helper.assertTrue(data.plazaVersion() == ArrivalPlaza.CURRENT_VERSION, "Plaza must be the current version, was " + data.plazaVersion());
        BlockPos centre = data.waystone();
        helper.assertTrue(vestige.getBlockState(centre).is(GateRegistry.RETURN_WAYSTONE.get()), "Waystone at the centre");
        helper.assertTrue(vestige.getBlockState(centre.below()).is(VestigeWorldBlocks.RUNE_VESTIGE_BRICKS.get()), "Rune bricks under the waystone");
        helper.assertTrue(vestige.getBlockState(centre.offset(0, 6, -3)).is(VestigeWorldBlocks.GILDED_VESTIGE_BRICKS.get()), "Obelisk tip behind the dais");
        for (int dz = 2; dz <= 8; dz++) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 3; dy++) {
                    BlockPos p = centre.offset(dx, dy, dz);
                    helper.assertTrue(vestige.getBlockState(p).getCollisionShape(vestige, p).isEmpty(), "Path blocked at " + p);
                }
            }
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 600)
    public static void versionOnePlazaIsUpgradedOnceInPlace(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        ServerLevel vestige = ArrivalPlaza.vestige(server);
        helper.assertTrue(vestige != null, "The Vestige dimension must exist");
        helper.assertTrue(ArrivalPlaza.arrival(server) != null, "Plaza must exist");
        VestigeWorldData data = VestigeWorldData.get(vestige);
        BlockPos centre = data.waystone();
        int builds = data.buildCount();
        data.setPlazaVersion(1);

        // Snapshot the area that must stay untouched: the margin outside the 19x19 footprint and the air above the obelisk.
        int top = ArrivalPlaza.CLEAR_HEIGHT;
        java.util.Map<BlockPos, BlockState> outside = new java.util.HashMap<>();
        for (int dx = -13; dx <= 13; dx++) {
            for (int dz = -13; dz <= 13; dz++) {
                boolean inFootprint = Math.max(Math.abs(dx), Math.abs(dz)) <= ArrivalPlaza.HALF + 1;
                for (int dy = -8; dy <= top + 4; dy++) {
                    if (!inFootprint || dy > top) {
                        BlockPos p = centre.offset(dx, dy, dz);
                        outside.put(p, vestige.getBlockState(p));
                    }
                }
            }
        }
        BlockPos marker = centre.offset(2, 5, 2);
        vestige.setBlock(marker, Blocks.GOLD_BLOCK.defaultBlockState(), Block.UPDATE_CLIENTS);

        BlockPos arrival = ArrivalPlaza.arrival(server);
        helper.assertTrue(arrival != null && SafeSpot.isSafe(vestige, arrival), "Arrival must be safe after the upgrade");
        helper.assertTrue(data.plazaVersion() == ArrivalPlaza.CURRENT_VERSION, "Version must be saved as current, was " + data.plazaVersion());
        helper.assertTrue(data.buildCount() == builds, "An upgrade is not a fresh build: " + builds + " -> " + data.buildCount());
        helper.assertTrue(data.waystone().equals(centre), "The centre must not move");
        helper.assertTrue(vestige.getBlockState(marker).isAir(), "The footprint must be rebuilt in place");
        for (var entry : outside.entrySet()) {
            helper.assertTrue(vestige.getBlockState(entry.getKey()) == entry.getValue(), "Block outside the footprint changed at " + entry.getKey());
        }

        // Second arrival must not rebuild.
        vestige.setBlock(marker, Blocks.GOLD_BLOCK.defaultBlockState(), Block.UPDATE_CLIENTS);
        ArrivalPlaza.arrival(server);
        helper.assertTrue(vestige.getBlockState(marker).is(Blocks.GOLD_BLOCK), "A version 2 plaza must not be rebuilt again");
        vestige.setBlock(marker, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        helper.succeed();
    }
}
