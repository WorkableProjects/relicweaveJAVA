package com.relicweave.vestige.world.feature;

import com.mojang.serialization.Codec;
import com.relicweave.vestige.world.VestigeWorldBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/** Giant glassroot roots arching out of the ground and back down around a broken pillar. */
public class RootArchFeature extends Feature<NoneFeatureConfiguration> {
    public RootArchFeature(Codec<NoneFeatureConfiguration> codec) {
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
        int ground = b.groundY(origin.getX(), origin.getZ(), origin.getY() + 2, 6);
        int ph = 5 + random.nextInt(5);
        for (int y = 0; y <= ph; y++) {
            b.set(origin.getX(), ground + y, origin.getZ(), y == ph ? FeatureBuilder.cracked() : y % 4 == 3 ? FeatureBuilder.gilded() : FeatureBuilder.pillar());
        }
        int roots = 3 + random.nextInt(3);
        for (int i = 0; i < roots; i++) {
            double phi = Math.PI * i / roots + (random.nextDouble() - 0.5) * 0.3;
            double cos = Math.cos(phi);
            double sin = Math.sin(phi);
            double radius = 4 + random.nextInt(4);
            double height = ph + 3 + random.nextInt(5);
            boolean thick = random.nextInt(3) == 0;
            int steps = 90;
            int lastX = Integer.MIN_VALUE;
            int lastY = Integer.MIN_VALUE;
            int lastZ = Integer.MIN_VALUE;
            for (int s = 0; s <= steps; s++) {
                double t = Math.PI * s / steps;
                double u = radius * Math.cos(t);
                int x = origin.getX() + (int) Math.round(u * cos);
                int z = origin.getZ() + (int) Math.round(u * sin);
                int y = ground + (int) Math.round(height * Math.sin(t)) - 1;
                if (x == lastX && y == lastY && z == lastZ) {
                    continue;
                }
                int dx = Math.abs(x - lastX);
                int dy = Math.abs(y - lastY);
                int dz = Math.abs(z - lastZ);
                Direction.Axis axis = lastX == Integer.MIN_VALUE || dy >= Math.max(dx, dz) ? Direction.Axis.Y
                        : dx >= dz ? Direction.Axis.X : Direction.Axis.Z;
                if (s < 6 || s > steps - 6) {
                    axis = Direction.Axis.Y;
                }
                b.set(x, y, z, FeatureBuilder.log(axis));
                if (thick) {
                    b.set(x + 1, y, z, FeatureBuilder.log(axis));
                    b.set(x, y, z + 1, FeatureBuilder.log(axis));
                }
                if (s == 0 || s == steps) {
                    b.anchor(x, z, y - 1, FeatureBuilder.log(Direction.Axis.Y));
                }
                lastX = x;
                lastY = y;
                lastZ = z;
            }
            // A crystal bud near the crown of the arch.
            if (random.nextInt(2) == 0) {
                b.setSoft(origin.getX(), ground + (int) height, origin.getZ(), VestigeWorldBlocks.GLASSROOT_CRYSTAL.get().defaultBlockState());
            }
        }
        return true;
    }
}
