package com.relicweave.vestige.relic;

import com.relicweave.relic.RelicAugment;
import com.relicweave.vestige.VestigeKeys;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

import java.util.List;

/**
 * Sealkeeper's Signet: disrupts nearby non-boss Vestige elites (Slowness III + Weakness I) and opens breakable seals
 * adjacent to the wearer. Bosses are never affected; boss locks are not in {@code #relicweave:breakable_seals}.
 */
public final class SealkeepersSignet {
    public static final double ENTITY_RANGE = 8.0;
    public static final int BASE_DISRUPT_TICKS = 60;
    public static final int BASE_SEAL_RANGE = 2;

    private SealkeepersSignet() {}

    /** True for living entities the signet may disrupt: in {@code #vestige_elites} and never in {@code #vestige_bosses}. */
    public static boolean canDisrupt(LivingEntity entity) {
        return entity.isAlive() && entity.getType().is(VestigeKeys.VESTIGE_ELITES) && !entity.getType().is(VestigeKeys.VESTIGE_BOSSES);
    }

    /** Disruption duration: 60 ticks, Disruption x1.5. */
    public static int disruptTicks(ItemStack stack) {
        return VestigeRelics.has(stack, RelicAugment.SIGNET_DISRUPTION) ? (int) Math.round(BASE_DISRUPT_TICKS * 1.5) : BASE_DISRUPT_TICKS;
    }

    /** Seal opening range in blocks: 2, Opening 4. */
    public static int sealRange(ItemStack stack) {
        return VestigeRelics.has(stack, RelicAugment.SIGNET_OPENING) ? 4 : BASE_SEAL_RANGE;
    }

    /** Applies the pulse. Returns the number of disrupted entities plus opened seals (0 = nothing happened). */
    public static int pulse(ServerPlayer player, ItemStack signet) {
        ServerLevel level = player.serverLevel();
        int affected = 0;
        int ticks = disruptTicks(signet);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(ENTITY_RANGE),
                e -> e != player && canDisrupt(e) && e.distanceToSqr(player) <= ENTITY_RANGE * ENTITY_RANGE);
        for (LivingEntity target : targets) {
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, 2));
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, ticks, 0));
            level.sendParticles(ParticleTypes.ENCHANT, target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ(), 12, 0.3, 0.4, 0.3, 0.1);
            affected++;
        }
        int range = sealRange(signet);
        BlockPos centre = player.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(centre.offset(-range, -range, -range), centre.offset(range, range, range))) {
            if (level.hasChunkAt(pos) && level.getBlockState(pos).is(VestigeKeys.BREAKABLE_SEALS)) {
                BlockPos at = pos.immutable();
                level.setBlock(at, Blocks.AIR.defaultBlockState(), 3);
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, at.getX() + 0.5, at.getY() + 0.5, at.getZ() + 0.5, 16, 0.4, 0.4, 0.4, 0.1);
                level.playSound(null, at, SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.BLOCKS, 1.0F, 0.8F);
                affected++;
            }
        }
        return affected;
    }
}
