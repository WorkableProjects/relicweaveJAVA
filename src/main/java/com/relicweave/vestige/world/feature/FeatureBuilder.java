package com.relicweave.vestige.world.feature;

import com.relicweave.vestige.world.VestigeWorldBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;

/**
 * Small write helper shared by the Vestige landmark features. It refuses every write more than {@link #LIMIT} blocks
 * horizontally from the feature origin (worldgen only guarantees a 3x3 chunk write area), never writes into fluids or
 * outside the build height, and exposes the common block palette.
 */
final class FeatureBuilder {
    /** Maximum horizontal distance from the origin that may be written. */
    static final int LIMIT = 16;

    final WorldGenLevel level;
    final BlockPos origin;
    private final BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

    FeatureBuilder(WorldGenLevel level, BlockPos origin) {
        this.level = level;
        this.origin = origin;
    }

    /** True when the origin stands in free space on solid dry ground at least two blocks above sea level. */
    static boolean validGround(FeaturePlaceContext<?> ctx) {
        WorldGenLevel level = ctx.level();
        BlockPos origin = ctx.origin();
        if (origin.getY() < ctx.chunkGenerator().getSeaLevel() + 2 || origin.getY() < level.getMinBuildHeight() + 2) {
            return false;
        }
        BlockState here = level.getBlockState(origin);
        if (!here.getFluidState().isEmpty()) {
            return false;
        }
        BlockPos below = origin.below();
        BlockState ground = level.getBlockState(below);
        return ground.getFluidState().isEmpty() && ground.blocksMotion();
    }

    /** Writes {@code state} at an absolute position, honouring the limits. Returns true when written. */
    boolean set(int x, int y, int z, BlockState state) {
        if (Math.abs(x - origin.getX()) > LIMIT || Math.abs(z - origin.getZ()) > LIMIT
                || y < level.getMinBuildHeight() || y >= level.getMaxBuildHeight()) {
            return false;
        }
        cursor.set(x, y, z);
        if (!level.getBlockState(cursor).getFluidState().isEmpty()) {
            return false;
        }
        return level.setBlock(cursor, state, Block.UPDATE_CLIENTS);
    }

    /** Writes only into air or replaceable blocks (plants, snow). */
    boolean setSoft(int x, int y, int z, BlockState state) {
        if (Math.abs(x - origin.getX()) > LIMIT || Math.abs(z - origin.getZ()) > LIMIT) {
            return false;
        }
        cursor.set(x, y, z);
        BlockState existing = level.getBlockState(cursor);
        if (!existing.isAir() && !existing.canBeReplaced()) {
            return false;
        }
        return set(x, y, z, state);
    }

    /** Y of the first solid dry block at or below {@code fromY} (within {@code maxDown}), else {@code fromY - maxDown}. */
    int groundY(int x, int z, int fromY, int maxDown) {
        for (int i = 0; i <= maxDown; i++) {
            cursor.set(x, fromY - i, z);
            BlockState state = level.getBlockState(cursor);
            if (state.blocksMotion() && state.getFluidState().isEmpty()) {
                return fromY - i;
            }
        }
        return fromY - maxDown;
    }

    /** Fills a column from just below {@code topY} down to the ground with the given state (at most 12 blocks). */
    void anchor(int x, int z, int topY, BlockState state) {
        for (int y = topY; y > topY - 12; y--) {
            cursor.set(x, y, z);
            BlockState existing = level.getBlockState(cursor);
            if (existing.blocksMotion() && existing.getFluidState().isEmpty()) {
                return;
            }
            set(x, y, z, state);
        }
    }

    static BlockState stone() { return VestigeWorldBlocks.VESTIGE_STONE.get().defaultBlockState(); }
    static BlockState fractured() { return VestigeWorldBlocks.FRACTURED_VESTIGE_STONE.get().defaultBlockState(); }
    static BlockState bricks() { return VestigeWorldBlocks.VESTIGE_BRICKS.get().defaultBlockState(); }
    static BlockState cracked() { return VestigeWorldBlocks.CRACKED_VESTIGE_BRICKS.get().defaultBlockState(); }
    static BlockState carved() { return VestigeWorldBlocks.CARVED_VESTIGE_STONE.get().defaultBlockState(); }
    static BlockState gilded() { return VestigeWorldBlocks.GILDED_VESTIGE_BRICKS.get().defaultBlockState(); }
    static BlockState polished() { return VestigeWorldBlocks.POLISHED_VESTIGE_STONE.get().defaultBlockState(); }
    static BlockState paving() { return VestigeWorldBlocks.CONCORDANT_PAVING.get().defaultBlockState(); }
    static BlockState moss() { return VestigeWorldBlocks.AUREATE_MOSS.get().defaultBlockState(); }
    static BlockState slab() { return VestigeWorldBlocks.VESTIGE_BRICK_SLAB.get().defaultBlockState(); }
    static BlockState air() { return Blocks.AIR.defaultBlockState(); }

    static BlockState pillar() {
        return VestigeWorldBlocks.VESTIGE_PILLAR.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);
    }

    static BlockState log(Direction.Axis axis) {
        return VestigeWorldBlocks.GLASSROOT_LOG.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, axis);
    }

    /** Random weathered masonry: mostly bricks, some cracked, a few stone. */
    static BlockState weathered(net.minecraft.util.RandomSource random) {
        int roll = random.nextInt(10);
        return roll < 5 ? bricks() : roll < 8 ? cracked() : stone();
    }
}
