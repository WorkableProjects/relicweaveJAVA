package com.relicweave.vestige.world.feature;

import com.mojang.serialization.Codec;
import com.relicweave.vestige.world.VestigeWorldBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/** A floating island fragment 20-45 blocks above the surface: inverted stone cone, moss cap, ruins and hanging crystals. */
public class FloatingRuinFeature extends Feature<NoneFeatureConfiguration> {
    public FloatingRuinFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        if (!FeatureBuilder.validGround(ctx)) {
            return false;
        }
        RandomSource random = ctx.random();
        BlockPos origin = ctx.origin();
        FeatureBuilder b = new FeatureBuilder(ctx.level(), origin);
        int r = 3 + random.nextInt(5);
        int top = origin.getY() + 20 + random.nextInt(26);
        if (top + 8 >= ctx.level().getMaxBuildHeight()) {
            return false;
        }
        int depth = r + 2 + random.nextInt(r);
        int size = 2 * r + 3;
        int[] lowest = new int[size * size];
        java.util.Arrays.fill(lowest, Integer.MAX_VALUE);
        for (int k = 0; k <= depth; k++) {
            double rk = r * (1.0 - k / (double) (depth + 1)) + (k == 0 ? 0.5 : 0.0);
            for (int dx = -r - 1; dx <= r + 1; dx++) {
                for (int dz = -r - 1; dz <= r + 1; dz++) {
                    double d = Math.sqrt(dx * dx + dz * dz);
                    if (d > rk + random.nextFloat() * 0.9 - 0.3) {
                        continue;
                    }
                    int y = top - k;
                    int roll = random.nextInt(20);
                    var state = k == 0 ? FeatureBuilder.moss()
                            : roll < 12 ? FeatureBuilder.stone() : roll < 17 ? FeatureBuilder.fractured() : FeatureBuilder.cracked();
                    if (b.set(origin.getX() + dx, y, origin.getZ() + dz, state)) {
                        lowest[(dx + r + 1) * size + dz + r + 1] = Math.min(lowest[(dx + r + 1) * size + dz + r + 1], y);
                    }
                }
            }
        }
        // Moss flowers.
        for (int i = 0; i < 4; i++) {
            b.setSoft(origin.getX() + random.nextInt(2 * r - 1) - r + 1, top + 1, origin.getZ() + random.nextInt(2 * r - 1) - r + 1,
                    VestigeWorldBlocks.AURELIAN_BLOOM.get().defaultBlockState());
        }
        int reach = Math.max(0, r - 3);
        if (random.nextInt(100) < 55) {
            int px = origin.getX() + random.nextInt(2 * reach + 1) - reach;
            int pz = origin.getZ() + random.nextInt(2 * reach + 1) - reach;
            int h = 2 + random.nextInt(4);
            for (int y = 1; y <= h; y++) {
                b.set(px, top + y, pz, y == h ? FeatureBuilder.cracked() : FeatureBuilder.pillar());
            }
        }
        if (r >= 5 && random.nextInt(100) < 40) {
            int ax = origin.getX() - 2;
            int az = origin.getZ() + random.nextInt(3) - 1;
            for (int y = 1; y <= 4; y++) {
                b.set(ax, top + y, az, y == 4 ? FeatureBuilder.gilded() : FeatureBuilder.pillar());
                b.set(ax + 4, top + y, az, y <= 2 ? FeatureBuilder.pillar() : FeatureBuilder.air());
            }
            b.set(ax + 1, top + 4, az, FeatureBuilder.bricks());
            b.set(ax + 2, top + 4, az, FeatureBuilder.slab());
        }
        // Hanging crystals under the cone.
        int crystals = 2 + random.nextInt(3);
        for (int i = 0; i < crystals; i++) {
            int dx = random.nextInt(2 * r + 3) - r - 1;
            int dz = random.nextInt(2 * r + 3) - r - 1;
            int low = lowest[(dx + r + 1) * size + dz + r + 1];
            if (low == Integer.MAX_VALUE) {
                continue;
            }
            int len = 1 + random.nextInt(3);
            for (int j = 1; j <= len; j++) {
                b.set(origin.getX() + dx, low - j, origin.getZ() + dz, VestigeWorldBlocks.GLASSROOT_CRYSTAL.get().defaultBlockState());
            }
        }
        return true;
    }
}
