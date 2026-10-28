package com.relicweave.vestige.world;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Predicate;

/** Small cross-shaped plant of The Vestige that may only stand on a configurable set of ground blocks. */
public class VestigePlantBlock extends BushBlock {
    private final Predicate<BlockState> ground;

    public VestigePlantBlock(BlockBehaviour.Properties properties, Predicate<BlockState> ground) {
        super(properties);
        this.ground = ground;
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return ground.test(state);
    }
}
