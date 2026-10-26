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

/**
 * Anchor of a Null Pocket: active relics are suppressed nearby (see {@link NullPockets}). Unbreakable in survival and
 * drops nothing.
 */
public class NullWardBlock extends Block {
    public NullWardBlock() {
        super(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(-1.0F, 3600000.0F)
                .lightLevel(state -> 3).sound(SoundType.AMETHYST).noLootTable());
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        int chance = FieldParticles.chance(RelicWeaveClientConfig.SPEC.isLoaded()
                ? RelicWeaveClientConfig.PARTICLE_DENSITY.get() : RelicWeaveClientConfig.ParticleDensity.FULL, 4);
        if (random.nextInt(chance) == 0) {
            level.addParticle(ParticleTypes.PORTAL, pos.getX() + random.nextDouble(), pos.getY() + 1.0 + random.nextDouble() * 0.5,
                    pos.getZ() + random.nextDouble(), 0.0, 0.02, 0.0);
        }
    }
}
