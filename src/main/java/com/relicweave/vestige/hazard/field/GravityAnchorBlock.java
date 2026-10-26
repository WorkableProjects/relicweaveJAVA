package com.relicweave.vestige.hazard.field;

import com.relicweave.config.RelicWeaveClientConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;

/** A heavy obsidian-gold block: Gravity Shear pulses are skipped within 3 blocks of one ({@link GravityShear}). */
public class GravityAnchorBlock extends Block {
    public GravityAnchorBlock() {
        super(BlockBehaviour.Properties.of().mapColor(MapColor.GOLD).strength(25.0F, 1200.0F)
                .requiresCorrectToolForDrops().lightLevel(state -> 4).sound(SoundType.NETHERITE_BLOCK));
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        int chance = FieldParticles.chance(RelicWeaveClientConfig.SPEC.isLoaded()
                ? RelicWeaveClientConfig.PARTICLE_DENSITY.get() : RelicWeaveClientConfig.ParticleDensity.FULL, 6);
        if (random.nextInt(chance) == 0) {
            level.addParticle(ParticleTypes.ENCHANT, pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5,
                    (random.nextDouble() - 0.5) * 0.4, -0.3, (random.nextDouble() - 0.5) * 0.4);
        }
    }
}
