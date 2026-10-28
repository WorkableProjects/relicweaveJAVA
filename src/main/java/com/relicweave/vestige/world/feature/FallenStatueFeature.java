package com.relicweave.vestige.world.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/** A toppled colossal statue: either a huge half-buried head (5x5x6) or a fallen torso with a reaching arm. */
public class FallenStatueFeature extends Feature<NoneFeatureConfiguration> {
    public FallenStatueFeature(Codec<NoneFeatureConfiguration> codec) {
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
        int ground = b.groundY(origin.getX(), origin.getZ(), origin.getY() + 2, 6);
        if (random.nextBoolean()) {
            head(b, random, alongX, ground);
        } else {
            torso(b, random, alongX, ground);
        }
        return true;
    }

    private static BlockState skin(RandomSource random) {
        int roll = random.nextInt(10);
        return roll < 6 ? FeatureBuilder.carved() : roll < 8 ? FeatureBuilder.polished() : FeatureBuilder.cracked();
    }

    /** Head: u in -2..2 (width), y in 0..4 (height, bottom two buried), v in -3..2 (depth, face towards +v). */
    private static void head(FeatureBuilder b, RandomSource random, boolean alongX, int ground) {
        int base = ground - 1;
        for (int u = -2; u <= 2; u++) {
            for (int v = -3; v <= 2; v++) {
                for (int y = 0; y <= 4; y++) {
                    boolean corner = Math.abs(u) == 2 && (v == -3 || v == 2) && (y == 0 || y == 4);
                    if (corner) {
                        continue;
                    }
                    BlockState state = y == 4 && (Math.abs(u) == 2 || v == -3 || v == 2) ? FeatureBuilder.gilded() : skin(random);
                    boolean eye = v == 2 && y == 3 && Math.abs(u) == 1;
                    boolean mouth = v == 2 && y == 1 && Math.abs(u) <= 1;
                    place(b, alongX, u, v, base + y, eye || mouth ? FeatureBuilder.air() : state);
                }
            }
        }
        place(b, alongX, 0, 3, base + 2, FeatureBuilder.carved());
        place(b, alongX, 0, 3, base + 1, FeatureBuilder.carved());
        for (int i = 0; i < 8; i++) {
            int u = random.nextInt(11) - 5;
            int v = random.nextInt(13) - 6;
            int[] p = pos(b, alongX, u, v);
            b.setSoft(p[0], b.groundY(p[0], p[1], ground + 4, 8) + 1, p[1], FeatureBuilder.weathered(random));
        }
    }

    /** Torso lying along v (6 long, 5 wide, 4 tall) with an arm 8 long reaching sideways. */
    private static void torso(FeatureBuilder b, RandomSource random, boolean alongX, int ground) {
        int base = ground;
        for (int v = -3; v <= 2; v++) {
            for (int u = -2; u <= 2; u++) {
                for (int y = 0; y <= 3; y++) {
                    if (Math.abs(u) == 2 && y == 3) {
                        continue;
                    }
                    BlockState state = y == 3 && v == 2 ? FeatureBuilder.gilded() : skin(random);
                    place(b, alongX, u, v, base + y, state);
                }
            }
        }
        // Broken neck stump.
        for (int u = -1; u <= 1; u++) {
            place(b, alongX, u, 3, base + 2, FeatureBuilder.cracked());
        }
        // Arm: 2x2 square section reaching out in +u from the shoulder, hand gilded.
        for (int u = 3; u <= 10; u++) {
            for (int dv = 0; dv < 2; dv++) {
                for (int y = 1; y <= 2; y++) {
                    BlockState state = u >= 9 ? FeatureBuilder.gilded() : skin(random);
                    if (u == 7 && dv == 0 && y == 2) {
                        state = FeatureBuilder.air();
                    }
                    place(b, alongX, u, -1 + dv, base + y, state);
                }
            }
        }
        for (int i = 0; i < 8; i++) {
            int[] p = pos(b, alongX, random.nextInt(15) - 4, random.nextInt(11) - 5);
            b.setSoft(p[0], b.groundY(p[0], p[1], ground + 4, 8) + 1, p[1], FeatureBuilder.weathered(random));
        }
    }

    private static int[] pos(FeatureBuilder b, boolean alongX, int u, int v) {
        return alongX ? new int[] {b.origin.getX() + u, b.origin.getZ() + v} : new int[] {b.origin.getX() + v, b.origin.getZ() + u};
    }

    private static void place(FeatureBuilder b, boolean alongX, int u, int v, int y, BlockState state) {
        int[] p = pos(b, alongX, u, v);
        b.set(p[0], y, p[1], state);
    }
}
