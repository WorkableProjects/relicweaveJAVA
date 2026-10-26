package com.relicweave.vestige.gate;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;

/** The Concordance Gate ring: a keystone surrounded by eight frames on the same Y. */
public final class GateShape {
    private GateShape() {}

    public static boolean isComplete(LevelReader level, BlockPos keystone) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) {
                    continue;
                }
                if (!level.getBlockState(keystone.offset(dx, 0, dz)).is(GateRegistry.CONCORDANCE_FRAME.get())) {
                    return false;
                }
            }
        }
        return true;
    }
}
