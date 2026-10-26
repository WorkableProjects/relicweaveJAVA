package com.relicweave.vestige.hazard.rift;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;

/**
 * A crack in reality. Players standing within 5 blocks gain Rift Saturation (see {@link RiftSaturation}); the block
 * needs a diamond pickaxe and drops nothing, so removing one is a deliberate act.
 */
public class RiftFissureBlock extends Block {
    public RiftFissureBlock() {
        super(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(50.0F, 1200.0F)
                .requiresCorrectToolForDrops().lightLevel(state -> 6).sound(SoundType.AMETHYST));
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(3) == 0) {
            level.addParticle(ParticleTypes.REVERSE_PORTAL,
                    pos.getX() + random.nextDouble(), pos.getY() + 1.0 + random.nextDouble() * 0.3, pos.getZ() + random.nextDouble(),
                    0.0, 0.04, 0.0);
        }
    }
}
