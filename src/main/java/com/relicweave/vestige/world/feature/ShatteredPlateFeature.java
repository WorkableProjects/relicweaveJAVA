package com.relicweave.vestige.world.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Sundered Reach "shattered plate": a slab of the old ground torn up and tilted — a 2-3 thick plate (5-8 wide, 8-13
 * long) rising from ground level to 5-12 blocks at its broken edge, propped on stone where it lifts off. One or two per
 * cluster, so the reach reads as broken plates at varying heights.
 */
public class ShatteredPlateFeature extends Feature<NoneFeatureConfiguration> {
    public ShatteredPlateFeature(Codec<NoneFeatureConfiguration> codec) {
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
        int plates = 1 + random.nextInt(2);
        for (int p = 0; p < plates; p++) {
            int ox = origin.getX() + (p == 0 ? 0 : random.nextInt(9) - 4);
            int oz = origin.getZ() + (p == 0 ? 0 : random.nextInt(9) - 4);
            plate(b, random, ox, oz, origin.getY());
        }
        return true;
    }

    private static void plate(FeatureBuilder b, RandomSource random, int ox, int oz, int originY) {
        int width = 5 + random.nextInt(4);
        int length = 8 + random.nextInt(6);
        int rise = 5 + random.nextInt(8);
        int thickness = 2 + random.nextInt(2);
        boolean alongX = random.nextBoolean();
        int dir = random.nextBoolean() ? 1 : -1;
        int base = b.groundY(ox, oz, originY + 2, 6);
        for (int l = 0; l < length; l++) {
            int lift = Mth.floor(rise * (l / (float) (length - 1)));
            for (int w = -width / 2; w <= width / 2; w++) {
                // Ragged far edge and sides.
                if ((l == length - 1 || Math.abs(w) == width / 2) && random.nextInt(3) == 0) {
                    continue;
                }
                int x = ox + (alongX ? l * dir : w);
                int z = oz + (alongX ? w : l * dir);
                int top = base + lift;
                for (int t = 0; t < thickness; t++) {
                    BlockState state = t == 0 ? (random.nextInt(4) == 0 ? FeatureBuilder.moss() : FeatureBuilder.fractured())
                            : (random.nextInt(3) == 0 ? FeatureBuilder.cracked() : FeatureBuilder.stone());
                    b.set(x, top - t, z, state);
                }
                // Props under the lifted part so it looks torn up, not floating.
                if (lift > thickness && (w + l) % 3 == 0) {
                    b.anchor(x, z, top - thickness, FeatureBuilder.stone());
                }
            }
        }
    }
}
