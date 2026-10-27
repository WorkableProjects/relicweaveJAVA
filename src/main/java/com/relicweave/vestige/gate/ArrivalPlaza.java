package com.relicweave.vestige.gate;

import com.mojang.datafixers.util.Pair;
import com.relicweave.vestige.VestigeKeys;
import com.relicweave.vestige.world.VestigeWorldBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import org.jetbrains.annotations.Nullable;

/**
 * Builds (once) and resolves the Arrival Plaza in The Vestige.
 *
 * <p>Version 2 is a 17x17 landmark: patterned floor with a gilded inlay ring, a raised dais carrying the return
 * waystone with a rune obelisk behind it, four lantern pillars, two broken arches framing the north and south
 * approaches, benches and planters, and steps plus a stone fill that join it to the terrain. Layout (x/z relative to the
 * centre, {@code y} = waystone level): floor top at {@code y - 2}, dais top at {@code y - 1}, the player stands at
 * {@code y - 1}. A 3-wide path stays open from the south edge to the dais. The footprint never exceeds {@link #HALF}+1
 * blocks from the centre horizontally and {@link #CLEAR_HEIGHT} blocks above the waystone.
 */
public final class ArrivalPlaza {
    /** Half width of the plaza floor (17 wide). */
    public static final int HALF = 8;
    /** Current design version stored in {@link VestigeWorldData}. */
    public static final int CURRENT_VERSION = 2;
    /** Blocks above the waystone level that are cleared. */
    public static final int CLEAR_HEIGHT = 8;
    private static final int FLAGS = Block.UPDATE_CLIENTS;
    private static final int MAX_FILL_DEPTH = 48;

    private ArrivalPlaza() {}

    /** The Vestige level, or null if the dimension is not loaded. */
    @Nullable
    public static ServerLevel vestige(MinecraftServer server) {
        return server.getLevel(VestigeKeys.THE_VESTIGE);
    }

    /** Feet position of the arrival spot, building (or upgrading) the plaza on first use. Null when The Vestige does not exist. */
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
        BlockPos centre = data.waystone();
        if (data.plazaVersion() < CURRENT_VERSION) {
            vestige.getChunk(centre);
            BlockPos arrival = buildPlaza(vestige, centre.getX(), centre.getY(), centre.getZ());
            data.setUpgraded(centre, arrival);
            return arrival;
        }
        BlockPos arrival = data.arrival();
        vestige.getChunk(arrival);
        if (!SafeSpot.isSafe(vestige, arrival)) {
            buildPlaza(vestige, centre.getX(), centre.getY(), centre.getZ());
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
        BlockPos arrival = buildPlaza(vestige, x, y, z);
        data.setBuilt(new BlockPos(x, y, z), arrival);
        return arrival;
    }

    /** Chebyshev distance from the centre. */
    private static int ring(int dx, int dz) {
        return Math.max(Math.abs(dx), Math.abs(dz));
    }

    /** Builds the v2 plaza around {@code x/z} with the waystone at {@code y}; returns the arrival (feet) position. */
    private static BlockPos buildPlaza(ServerLevel level, int x, int y, int z) {
        BlockState air = Blocks.AIR.defaultBlockState();
        BlockState stone = VestigeWorldBlocks.VESTIGE_STONE.get().defaultBlockState();
        BlockState bricks = VestigeWorldBlocks.VESTIGE_BRICKS.get().defaultBlockState();
        BlockState gilded = VestigeWorldBlocks.GILDED_VESTIGE_BRICKS.get().defaultBlockState();
        BlockState rune = VestigeWorldBlocks.RUNE_VESTIGE_BRICKS.get().defaultBlockState();
        BlockState paving = VestigeWorldBlocks.CONCORDANT_PAVING.get().defaultBlockState();
        BlockState polished = VestigeWorldBlocks.POLISHED_VESTIGE_STONE.get().defaultBlockState();
        BlockState carved = VestigeWorldBlocks.CARVED_VESTIGE_STONE.get().defaultBlockState();
        BlockState cracked = VestigeWorldBlocks.CRACKED_VESTIGE_BRICKS.get().defaultBlockState();
        BlockState pillar = VestigeWorldBlocks.VESTIGE_PILLAR.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);
        Put put = new Put(level, x, z);

        // 1. Clear the footprint (floor ring plus the step ring) and the space above it.
        for (int dx = -HALF - 1; dx <= HALF + 1; dx++) {
            for (int dz = -HALF - 1; dz <= HALF + 1; dz++) {
                int from = ring(dx, dz) > HALF ? y - 2 : y - 1;
                for (int yy = from; yy <= y + CLEAR_HEIGHT; yy++) {
                    BlockPos pos = new BlockPos(x + dx, yy, z + dz);
                    if (!level.getBlockState(pos).isAir()) {
                        level.setBlock(pos, air, FLAGS);
                    }
                }
            }
        }

        // 2. Floor pattern, steps and the stone fill down to the terrain.
        for (int dx = -HALF - 1; dx <= HALF + 1; dx++) {
            for (int dz = -HALF - 1; dz <= HALF + 1; dz++) {
                int m = ring(dx, dz);
                if (m > HALF) {
                    put.set(dx, y - 3, dz, bricks);
                    fill(level, x + dx, y - 4, z + dz, stone);
                    continue;
                }
                BlockState floor;
                if (m == 8) {
                    floor = bricks;
                } else if (m == 7) {
                    floor = paving;
                } else if (m == 6) {
                    floor = gilded;
                } else if (m >= 3) {
                    floor = ((dx + dz) & 1) == 0 ? bricks : paving;
                } else if (m == 2) {
                    floor = rune;
                } else {
                    floor = stone;
                }
                if (dz >= 2 && Math.abs(dx) <= 1 && m != 6) {
                    floor = dx == 0 ? gilded : paving;
                }
                put.set(dx, y - 2, dz, floor);
                fill(level, x + dx, y - 3, z + dz, stone);
            }
        }

        // 3. Raised dais with the waystone, and the obelisk behind it.
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                put.set(dx, y - 1, dz, dx == 0 && dz == 0 ? rune : polished);
            }
        }
        for (int dx = -1; dx <= 1; dx++) {
            put.set(dx, y - 1, -3, carved);
        }
        int obeliskHeight = 7;
        for (int i = 0; i < obeliskHeight; i++) {
            put.set(0, y + i, -3, i == obeliskHeight - 1 ? gilded : (i & 1) == 0 ? rune : carved);
        }

        // 4. Corner pillars with lanterns.
        for (int sx : new int[] {-7, 7}) {
            for (int sz : new int[] {-7, 7}) {
                for (int dy = -1; dy <= 2; dy++) {
                    put.set(sx, y + dy, sz, dy == -1 ? bricks : pillar);
                }
                put.set(sx, y + 3, sz, carved);
                put.set(sx, y + 4, sz, Blocks.LANTERN.defaultBlockState());
            }
        }

        // 5. Broken arches framing the south (+z) and north (-z) approaches.
        for (int side : new int[] {1, -1}) {
            int dz = 8 * side;
            boolean south = side > 0;
            for (int dx : new int[] {-5, -4, 4, 5}) {
                boolean brokenPillar = !south && dx > 0;
                int top = brokenPillar ? 4 : 5;
                for (int dy = -1; dy <= top; dy++) {
                    put.set(dx, y + dy, dz, dy == -1 ? bricks : dy == top ? (brokenPillar ? cracked : gilded) : pillar);
                }
            }
            for (int dx = -5; dx <= 5; dx++) {
                boolean missing = south ? (dx == 1 || dx == 2) : dx >= 0;
                if (!missing) {
                    put.set(dx, y + 6, dz, (dx & 1) == 0 ? gilded : bricks);
                    if (Math.abs(dx) >= 4) {
                        put.set(dx, y + 7, dz, cracked);
                    }
                }
            }
        }

        // 6. Benches (stair seats) and planters.
        BlockState seatSouth = VestigeWorldBlocks.VESTIGE_BRICK_STAIRS.get().defaultBlockState().setValue(StairBlock.FACING, Direction.NORTH);
        BlockState seatNorth = VestigeWorldBlocks.VESTIGE_BRICK_STAIRS.get().defaultBlockState().setValue(StairBlock.FACING, Direction.SOUTH);
        for (int sx : new int[] {-4, -3, 3, 4}) {
            put.set(sx, y - 1, 5, seatSouth);
            put.set(sx, y - 1, -5, seatNorth);
        }
        BlockState moss = VestigeWorldBlocks.AUREATE_MOSS.get().defaultBlockState();
        BlockState bloom = VestigeWorldBlocks.AURELIAN_BLOOM.get().defaultBlockState();
        for (int sx : new int[] {-5, 5}) {
            for (int dz = -1; dz <= 1; dz++) {
                put.set(sx, y - 1, dz, moss);
                put.set(sx, y, dz, bloom);
            }
        }

        // 7. The waystone last, so nothing overwrites it.
        level.setBlock(new BlockPos(x, y, z), GateRegistry.RETURN_WAYSTONE.get().defaultBlockState(), FLAGS);
        return new BlockPos(x, y - 1, z + 4);
    }

    /** Fills downward from {@code (x, topY, z)} with {@code state} until solid terrain is reached. */
    private static void fill(ServerLevel level, int x, int topY, int z, BlockState state) {
        for (int i = 0; i < MAX_FILL_DEPTH && topY - i > level.getMinBuildHeight(); i++) {
            BlockPos pos = new BlockPos(x, topY - i, z);
            BlockState existing = level.getBlockState(pos);
            boolean open = existing.isAir() || existing.canBeReplaced() || !existing.getFluidState().isEmpty();
            if (!open) {
                return;
            }
            level.setBlock(pos, state, FLAGS);
        }
    }

    /** Relative-coordinate writer. */
    private record Put(ServerLevel level, int x, int z) {
        void set(int dx, int y, int dz, BlockState state) {
            level.setBlock(new BlockPos(x + dx, y, z + dz), state, FLAGS);
        }
    }
}
