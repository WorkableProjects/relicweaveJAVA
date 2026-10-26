package com.relicweave.vestige.gate;

import com.mojang.datafixers.util.Pair;
import com.relicweave.vestige.VestigeKeys;
import com.relicweave.vestige.world.VestigeWorldBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import org.jetbrains.annotations.Nullable;

/** Builds (once) and resolves the Arrival Plaza in The Vestige. */
public final class ArrivalPlaza {
    public static final int HALF = 5;
    private static final int FLAGS = Block.UPDATE_CLIENTS;

    private ArrivalPlaza() {}

    /** The Vestige level, or null if the dimension is not loaded. */
    @Nullable
    public static ServerLevel vestige(MinecraftServer server) {
        return server.getLevel(VestigeKeys.THE_VESTIGE);
    }

    /** Feet position of the arrival spot, building the plaza on first use. Null when The Vestige does not exist. */
    @Nullable
    public static BlockPos arrival(MinecraftServer server) {
        ServerLevel vestige = vestige(server);
        if (vestige == null) {
            return null;
        }
        VestigeWorldData data = VestigeWorldData.get(vestige);
        if (!data.isBuilt()) {
            return build(vestige, data);
        }
        BlockPos arrival = data.arrival();
        vestige.getChunk(arrival);
        if (!SafeSpot.isSafe(vestige, arrival)) {
            BlockPos center = data.waystone();
            buildFloorAndClearance(vestige, center.getX(), center.getY(), center.getZ());
        }
        return arrival;
    }

    private static BlockPos build(ServerLevel vestige, VestigeWorldData data) {
        int x = 0;
        int z = 0;
        Pair<BlockPos, Holder<Biome>> basin = vestige.findClosestBiome3d(h -> h.is(VestigeKeys.CROWNFALL_BASIN),
                new BlockPos(0, 100, 0), 6400, 32, 64);
        if (basin != null) {
            x = basin.getFirst().getX();
            z = basin.getFirst().getZ();
        }
        vestige.getChunk(new BlockPos(x, 0, z));
        int y = vestige.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        if (y <= vestige.getMinBuildHeight() + 5) {
            y = 100;
        }
        buildFloorAndClearance(vestige, x, y, z);
        BlockState stone = VestigeWorldBlocks.VESTIGE_STONE.get().defaultBlockState();
        BlockState rune = VestigeWorldBlocks.RUNE_VESTIGE_BRICKS.get().defaultBlockState();
        for (int dx = -HALF; dx <= HALF; dx++) {
            for (int dz = -HALF; dz <= HALF; dz++) {
                if (isBorder(dx, dz)) {
                    vestige.setBlock(new BlockPos(x + dx, y - 2, z + dz), stone, FLAGS);
                    vestige.setBlock(new BlockPos(x + dx, y - 3, z + dz), stone, FLAGS);
                }
            }
        }
        for (int sx : new int[] {-HALF, HALF}) {
            for (int sz : new int[] {-HALF, HALF}) {
                for (int dy = 0; dy < 3; dy++) {
                    vestige.setBlock(new BlockPos(x + sx, y + dy, z + sz), rune, FLAGS);
                }
            }
        }
        BlockPos waystone = new BlockPos(x, y, z);
        vestige.setBlock(waystone, GateRegistry.RETURN_WAYSTONE.get().defaultBlockState(), FLAGS);
        BlockPos arrival = new BlockPos(x, y, z + 3);
        data.setBuilt(waystone, arrival);
        return arrival;
    }

    private static boolean isBorder(int dx, int dz) {
        return Math.abs(dx) == HALF || Math.abs(dz) == HALF;
    }

    /** Floor at {@code y - 1} (gilded border) and 11x11x6 of air above, centred on x/z. */
    private static void buildFloorAndClearance(ServerLevel vestige, int x, int y, int z) {
        BlockState bricks = VestigeWorldBlocks.VESTIGE_BRICKS.get().defaultBlockState();
        BlockState gilded = VestigeWorldBlocks.GILDED_VESTIGE_BRICKS.get().defaultBlockState();
        BlockState air = Blocks.AIR.defaultBlockState();
        for (int dx = -HALF; dx <= HALF; dx++) {
            for (int dz = -HALF; dz <= HALF; dz++) {
                vestige.setBlock(new BlockPos(x + dx, y - 1, z + dz), isBorder(dx, dz) ? gilded : bricks, FLAGS);
                for (int dy = 0; dy < 6; dy++) {
                    BlockPos pos = new BlockPos(x + dx, y + dy, z + dz);
                    BlockState existing = vestige.getBlockState(pos);
                    if (!existing.isAir() && !existing.is(GateRegistry.RETURN_WAYSTONE.get())) {
                        vestige.setBlock(pos, air, FLAGS);
                    }
                }
            }
        }
    }
}
