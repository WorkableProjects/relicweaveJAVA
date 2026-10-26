package com.relicweave.vestige.entity.ethereal;

import com.relicweave.vestige.registry.VestigeMaterials;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/** The Rune Warden's volley projectile: flies straight, 6 magic damage, vanishes on impact or after 3 seconds. */
public class RuneShard extends ThrowableItemProjectile {
    public static final float DAMAGE = 6.0F;
    private static final int MAX_AGE = 60;

    public RuneShard(EntityType<? extends RuneShard> type, Level level) {
        super(type, level);
    }

    public RuneShard(Level level, LivingEntity owner) {
        super(EtherealMobs.RUNE_SHARD.get(), owner, level);
    }

    @Override
    protected Item getDefaultItem() {
        return VestigeMaterials.AETHERITE_SHARD.get();
    }

    @Override
    protected float getGravity() {
        return 0.0F;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            level().addParticle(ParticleTypes.END_ROD, getX(), getY(), getZ(), 0, 0, 0);
        } else if (tickCount > MAX_AGE) {
            discard();
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        Entity target = result.getEntity();
        if (!level().isClientSide && target instanceof LivingEntity) {
            target.hurt(damageSources().indirectMagic(this, getOwner()), DAMAGE);
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!level().isClientSide) {
            discard();
        }
    }
}
