package com.relicweave.vestige.world.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/** A monumental ruined civic arch: two tall 2x2 pillars joined by a partly collapsed span, with rubble at the base. */
public class BrokenArchFeature extends Feature<NoneFeatureConfiguration> {
    public BrokenArchFeature(Codec<NoneFeatureConfiguration> codec) {
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
        boolean alongX = random.nextBoolean();
        int gap = 7 + random.nextInt(7);
        int left = -(gap / 2) - 1;
        int right = left + gap + 1;
        int[] heights = {8 + random.nextInt(9), 8 + random.nextInt(9)};
        int[] bases = new int[2];
        int[] us = {left, right};
        for (int p = 0; p < 2; p++) {
            int ground = Integer.MIN_VALUE;
            for (int w = 0; w < 2; w++) {
                int[] pos = at(alongX, origin, us[p], w);
                ground = Math.max(ground, b.groundY(pos[0], pos[1], origin.getY() + 2, 12));
            }
            bases[p] = ground;
            for (int w = 0; w < 2; w++) {
                int[] pos = at(alongX, origin, us[p], w);
                for (int y = ground - 2; y <= ground + heights[p]; y++) {
                    int layer = y - ground;
                    BlockState state;
                    if (layer < 0) {
                        state = FeatureBuilder.stone();
                    } else if (layer == 0) {
                        state = FeatureBuilder.bricks();
                    } else if (layer % 5 == 4) {
                        state = FeatureBuilder.gilded();
                    } else if (layer >= heights[p] - 1) {
                        state = FeatureBuilder.cracked();
                    } else {
                        state = FeatureBuilder.pillar();
                    }
                    b.set(pos[0], y, pos[1], state);
                }
            }
        }
        // Collapsed span: solid near the pillars, increasingly broken towards the middle.
        int spanY = Math.min(bases[0] + heights[0], bases[1] + heights[1]) + 1;
        for (int u = left; u <= right; u++) {
            int edge = Math.min(u - left, right - u);
            for (int w = 0; w < 2; w++) {
                int[] pos = at(alongX, origin, u, w);
                boolean keep = edge <= 1 || (edge <= 3 && random.nextInt(4) != 0) || random.nextInt(100) < 35;
                if (keep) {
                    b.set(pos[0], spanY, pos[1], edge % 4 == 0 ? FeatureBuilder.gilded() : FeatureBuilder.weathered(random));
                    if (edge <= 2) {
                        b.set(pos[0], spanY - 1, pos[1], FeatureBuilder.weathered(random));
                    }
                }
            }
        }
        // Rubble.
        int rubble = 10 + random.nextInt(8);
        for (int i = 0; i < rubble; i++) {
            int u = left - 3 + random.nextInt(right - left + 7);
            int w = random.nextInt(8) - 3;
            int[] pos = at(alongX, origin, u, w);
            int g = b.groundY(pos[0], pos[1], origin.getY() + 3, 8);
            BlockState state = random.nextInt(3) == 0 ? FeatureBuilder.slab() : FeatureBuilder.weathered(random);
            b.setSoft(pos[0], g + 1, pos[1], state);
            if (random.nextInt(4) == 0) {
                b.setSoft(pos[0], g + 2, pos[1], FeatureBuilder.cracked());
            }
        }
        return true;
    }

    private static int[] at(boolean alongX, BlockPos origin, int u, int v) {
        return alongX ? new int[] {origin.getX() + u, origin.getZ() + v} : new int[] {origin.getX() + v, origin.getZ() + u};
    }
}
