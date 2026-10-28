package com.relicweave.vestige.boss.a;

import com.relicweave.vestige.boss.VestigeBoss;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.projectile.AbstractHurtingProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/** A thorn fired by the Thorned Regent's volley: no gravity, 6 base damage (scaled by the boss), gone on any hit. */
public class ThornSpike extends AbstractHurtingProjectile {
    public static final float DAMAGE = 6.0F;
    public static final int LIFETIME_TICKS = 60;

    private int age;

    public ThornSpike(EntityType<? extends ThornSpike> type, Level level) {
        super(type, level);
    }

    /** Creates and positions a spike (not yet added to the level). */
    public static ThornSpike create(Level level, @Nullable Entity owner, Vec3 pos, Vec3 velocity) {
        ThornSpike spike = new ThornSpike(BossesA.THORN_SPIKE.get(), level);
        spike.setPos(pos.x, pos.y, pos.z);
        spike.setOwner(owner);
        spike.setDeltaMovement(velocity);
        return spike;
    }

    @Override
    protected boolean shouldBurn() {
        return false;
    }

    @Override
    protected float getInertia() {
        return 1.0F;
    }

    @Override
    protected ParticleOptions getTrailParticle() {
        return ParticleTypes.COMPOSTER;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    protected boolean canHitEntity(Entity entity) {
        return super.canHitEntity(entity) && !(entity instanceof Monster) && !(entity instanceof ThornRoot);
    }

    @Override
    public void tick() {
        if (!level().isClientSide && ++age > LIFETIME_TICKS) {
            discard();
            return;
        }
        super.tick();
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        if (level().isClientSide) {
            return;
        }
        Entity target = result.getEntity();
        if (target instanceof LivingEntity) {
            Entity owner = getOwner();
            float damage = owner instanceof VestigeBoss boss ? boss.scaledDamage(DAMAGE) : DAMAGE;
            target.hurt(damageSources().mobProjectile(this, owner instanceof LivingEntity living ? living : null), damage);
        }
        puff();
        discard();
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        if (!level().isClientSide) {
            puff();
            discard();
        }
    }

    private void puff() {
        if (level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.COMPOSTER, getX(), getY(), getZ(), 6, 0.15, 0.15, 0.15, 0.05);
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Age", age);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        age = tag.getInt("Age");
    }
}
