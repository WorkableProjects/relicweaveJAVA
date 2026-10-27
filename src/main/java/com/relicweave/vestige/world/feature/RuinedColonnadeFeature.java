package com.relicweave.vestige.world.feature;

import com.mojang.serialization.Codec;
import com.relicweave.vestige.world.VestigeWorldBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.BlockStateConfiguration;

/** A row of 4-8 columns of varying broken heights on a paving strip. The configured state is the shaft block. */
public class RuinedColonnadeFeature extends Feature<BlockStateConfiguration> {
    public RuinedColonnadeFeature(Codec<BlockStateConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<BlockStateConfiguration> ctx) {
        if (!FeatureBuilder.validGround(ctx)) {
            return false;
        }
        RandomSource random = ctx.random();
        BlockPos origin = ctx.origin();
        FeatureBuilder b = new FeatureBuilder(ctx.level(), origin);
        BlockState shaft = ctx.config().state;
        boolean gloam = shaft.is(VestigeWorldBlocks.GLOAM_BRICKS.get());
        BlockState strip = gloam ? VestigeWorldBlocks.GLOAM_STONE.get().defaultBlockState() : FeatureBuilder.paving();
        boolean alongX = random.nextBoolean();
        int count = 4 + random.nextInt(5);
        int length = 3 * (count - 1);
        int start = -length / 2;
        for (int u = start - 2; u <= start + length + 2; u++) {
            for (int v = -2; v <= 2; v++) {
                int[] p = at(alongX, origin, u, v);
                int g = b.groundY(p[0], p[1], origin.getY() + 2, 6);
                if (Math.abs(v) <= 1 || random.nextInt(3) == 0) {
                    b.set(p[0], g, p[1], random.nextInt(8) == 0 ? FeatureBuilder.cracked() : strip);
                }
            }
        }
        for (int i = 0; i < count; i++) {
            int u = start + 3 * i;
            int[] p = at(alongX, origin, u, 0);
            int g = b.groundY(p[0], p[1], origin.getY() + 2, 6);
            int h = 3 + random.nextInt(7);
            boolean broken = random.nextInt(3) == 0;
            for (int y = 1; y <= h; y++) {
                BlockState state = y == 1 ? FeatureBuilder.carved()
                        : (y == h && !broken) ? FeatureBuilder.gilded()
                        : gloam ? shaft : (y == h ? FeatureBuilder.cracked() : shaft);
                b.set(p[0], g + y, p[1], state);
            }
            if (broken) {
                b.set(p[0], g + h + 1, p[1], FeatureBuilder.slab());
            }
            if (random.nextInt(3) == 0) {
                int[] q = at(alongX, origin, u + random.nextInt(3) - 1, random.nextBoolean() ? 2 : -2);
                b.setSoft(q[0], g + 1, q[1], FeatureBuilder.weathered(random));
            }
        }
        return true;
    }

    private static int[] at(boolean alongX, BlockPos origin, int u, int v) {
        return alongX ? new int[] {origin.getX() + u, origin.getZ() + v} : new int[] {origin.getX() + v, origin.getZ() + u};
    }
}
