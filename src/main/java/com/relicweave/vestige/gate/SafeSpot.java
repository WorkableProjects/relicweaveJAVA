package com.relicweave.vestige.gate;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Optional;

/** Safe-landing checks and search for teleports. */
public final class SafeSpot {
    /** Search radius used by {@link #find(ServerLevel, BlockPos)}. */
    public static final int RADIUS = 8;

    private SafeSpot() {}

    /** True when a player standing with feet at {@code feet} would be safe. */
    public static boolean isSafe(ServerLevel level, BlockPos feet) {
        if (!level.isInWorldBounds(feet) || !level.isInWorldBounds(feet.above()) || !level.isInWorldBounds(feet.below())) {
            return false;
        }
        if (!free(level, feet) || !free(level, feet.above())) {
            return false;
        }
        BlockPos below = feet.below();
        BlockState ground = level.getBlockState(below);
        if (!ground.getFluidState().isEmpty() || !Block.isFaceFull(ground.getCollisionShape(level, below), Direction.UP)) {
            return false;
        }
        for (BlockPos p : BlockPos.betweenClosed(feet.offset(-1, -1, -1), feet.offset(1, 1, 1))) {
            BlockState s = level.getBlockState(p);
            if (s.is(Blocks.MAGMA_BLOCK) || s.is(Blocks.FIRE) || s.is(Blocks.SOUL_FIRE) || s.is(Blocks.CAMPFIRE)
                    || s.is(Blocks.SOUL_CAMPFIRE) || s.getFluidState().is(FluidTags.LAVA)) {
                return false;
            }
        }
        return true;
    }

    private static boolean free(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.getCollisionShape(level, pos).isEmpty() && state.getFluidState().isEmpty();
    }

    /** The spot itself if safe, else the nearest safe spot within {@link #RADIUS} blocks. */
    public static Optional<BlockPos> find(ServerLevel level, BlockPos around) {
        if (isSafe(level, around)) {
            return Optional.of(around);
        }
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;
        for (int dx = -RADIUS; dx <= RADIUS; dx++) {
            for (int dz = -RADIUS; dz <= RADIUS; dz++) {
                for (int dy = -RADIUS; dy <= RADIUS; dy++) {
                    double d = dx * dx + dy * dy + dz * dz;
                    if (d >= bestDist || d > RADIUS * RADIUS) {
                        continue;
                    }
                    BlockPos candidate = around.offset(dx, dy, dz);
                    if (isSafe(level, candidate)) {
                        best = candidate;
                        bestDist = d;
                    }
                }
            }
        }
        return Optional.ofNullable(best);
    }
}
