package com.relicweave.vestige.world.feature;

import com.mojang.serialization.Codec;
import com.relicweave.vestige.world.VestigeWorldBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Hushfen "mirror pool": a still, black-rimmed pond (radius 3-6, 1-2 deep) sunk into flat ground, ringed with mud and
 * reeds, sometimes with a drowned pillar standing in it. Placed only where every water block is enclosed by solid
 * ground, so it never spills.
 */
public class MirrorPoolFeature extends Feature<NoneFeatureConfiguration> {
    public MirrorPoolFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        if (!FeatureBuilder.validGround(ctx)) {
            return false;
        }
        RandomSource random = ctx.random();
        WorldGenLevel level = ctx.level();
        BlockPos origin = ctx.origin();
        FeatureBuilder b = new FeatureBuilder(level, origin);
        int rx = 3 + random.nextInt(4);
        int rz = 3 + random.nextInt(4);
        int surface = origin.getY() - 1;
        // The pool needs flat, solid ground: every rim column must be solid at the surface level and open above it.
        for (int dx = -rx - 1; dx <= rx + 1; dx++) {
            for (int dz = -rz - 1; dz <= rz + 1; dz++) {
                if (!inside(dx, dz, rx + 1, rz + 1)) {
                    continue;
                }
                BlockPos ground = origin.offset(dx, -1, dz);
                BlockState state = level.getBlockState(ground);
                if (!state.blocksMotion() || !state.getFluidState().isEmpty() || level.getBlockState(ground.above()).blocksMotion()
                        || !level.getBlockState(ground.below()).blocksMotion()) {
                    return false;
                }
            }
        }
        BlockState water = Blocks.WATER.defaultBlockState();
        BlockState mud = Blocks.MUD.defaultBlockState();
        BlockState rim = VestigeWorldBlocks.ECHO_CLAY.get().defaultBlockState();
        BlockState reed = VestigeWorldBlocks.HUSHFEN_REED.get().defaultBlockState();
        for (int dx = -rx - 1; dx <= rx + 1; dx++) {
            for (int dz = -rz - 1; dz <= rz + 1; dz++) {
                int x = origin.getX() + dx;
                int z = origin.getZ() + dz;
                if (inside(dx, dz, rx, rz)) {
                    boolean deep = inside(dx, dz, rx - 1, rz - 1);
                    b.set(x, surface, z, water);
                    if (deep) {
                        b.set(x, surface - 1, z, water);
                        b.set(x, surface - 2, z, mud);
                    } else {
                        b.set(x, surface - 1, z, mud);
                    }
                } else if (inside(dx, dz, rx + 1, rz + 1)) {
                    b.set(x, surface, z, random.nextInt(3) == 0 ? mud : rim);
                    if (random.nextInt(3) == 0) {
                        b.setSoft(x, surface + 1, z, reed);
                    }
                }
            }
        }
        if (random.nextInt(3) == 0) {
            int h = 2 + random.nextInt(4);
            for (int y = surface - 1; y <= surface + h; y++) {
                b.set(origin.getX(), y, origin.getZ(), y == surface + h ? FeatureBuilder.cracked() : FeatureBuilder.pillar());
            }
        }
        return true;
    }

    private static boolean inside(int dx, int dz, int rx, int rz) {
        double nx = dx / (rx + 0.5);
        double nz = dz / (rz + 0.5);
        return nx * nx + nz * nz <= 1.0;
    }
}
