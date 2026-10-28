package com.relicweave.vestige.world.feature;

import com.mojang.serialization.Codec;
import com.relicweave.vestige.world.VestigeWorldBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.BlockStateConfiguration;

/** A cluster of 2-4 tall, tapering, slightly leaning crystal spikes. The configured state is the crystal block. */
public class CrystalSpireFeature extends Feature<BlockStateConfiguration> {
    public CrystalSpireFeature(Codec<BlockStateConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<BlockStateConfiguration> ctx) {
        if (!FeatureBuilder.validGround(ctx)) {
            return false;
        }
        RandomSource random = ctx.random();
        BlockPos origin = ctx.origin();
        BlockState crystal = ctx.config().state;
        boolean rift = crystal.is(VestigeWorldBlocks.RIFTGLASS_BLOCK.get());
        FeatureBuilder b = new FeatureBuilder(ctx.level(), origin);
        int spikes = 2 + random.nextInt(3);
        for (int s = 0; s < spikes; s++) {
            int bx = origin.getX() + (s == 0 ? 0 : random.nextInt(9) - 4);
            int bz = origin.getZ() + (s == 0 ? 0 : random.nextInt(9) - 4);
            int ground = b.groundY(bx, bz, origin.getY() + 3, 8);
            int h = 5 + random.nextInt(10);
            double lx = (random.nextDouble() - 0.5) * 0.5;
            double lz = (random.nextDouble() - 0.5) * 0.5;
            for (int k = -1; k < h; k++) {
                int cx = bx + (int) Math.round(lx * Math.max(k, 0));
                int cz = bz + (int) Math.round(lz * Math.max(k, 0));
                int y = ground + 1 + k;
                b.set(cx, y, cz, crystal);
                if (h > 8 && k < h * 0.4) {
                    b.set(cx + 1, y, cz, crystal);
                    b.set(cx, y, cz + 1, crystal);
                    if (k < h * 0.2) {
                        b.set(cx + 1, y, cz + 1, crystal);
                    }
                }
            }
            if (rift) {
                for (int i = 0; i < 3; i++) {
                    b.setSoft(bx + random.nextInt(5) - 2, ground + 1, bz + random.nextInt(5) - 2,
                            VestigeWorldBlocks.RIFTGLASS_CLUSTER.get().defaultBlockState().setValue(AmethystClusterBlock.FACING, Direction.UP));
                }
            }
        }
        return true;
    }
}
