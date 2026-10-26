package com.relicweave.vestige.entity.humanoid;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractHurtingProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * A scribe's glyph bolt: no gravity, no fire, 6 magic damage, gone on any block hit. With the {@code decoy} flag it is
 * harmless (translucent violet, vanishes after 2 seconds or on any hit), used by the Echo Weaver to mislead.
 */
public class GlyphBolt extends AbstractHurtingProjectile {
    public static final float DAMAGE = 6.0F;
    public static final int LIFETIME_TICKS = 100;
    public static final int DECOY_LIFETIME_TICKS = 40;

    private static final EntityDataAccessor<Boolean> DATA_DECOY = SynchedEntityData.defineId(GlyphBolt.class, EntityDataSerializers.BOOLEAN);

    private int age;

    public GlyphBolt(EntityType<? extends GlyphBolt> type, Level level) {
        super(type, level);
    }

    /** Creates and positions a bolt (not yet added to the level). */
    public static GlyphBolt create(Level level, @Nullable Entity owner, Vec3 pos, Vec3 velocity, boolean decoy) {
        GlyphBolt bolt = new GlyphBolt(HumanoidMobs.GLYPH_BOLT.get(), level);
        bolt.setPos(pos.x, pos.y, pos.z);
        bolt.setOwner(owner);
        bolt.setDeltaMovement(velocity);
        bolt.setDecoy(decoy);
        return bolt;
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(DATA_DECOY, false);
    }

    public boolean isDecoy() {
        return entityData.get(DATA_DECOY);
    }

    public void setDecoy(boolean decoy) {
        entityData.set(DATA_DECOY, decoy);
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
        return isDecoy() ? ParticleTypes.WITCH : ParticleTypes.SOUL_FIRE_FLAME;
    }

    @Override
    public boolean isPickable() {
        return isDecoy();
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (isDecoy() && !level().isClientSide) {
            discard();
            return true;
        }
        return false;
    }

    @Override
    protected boolean canHitEntity(Entity entity) {
        return super.canHitEntity(entity) && !MobCombat.isVestigeMob(entity);
    }

    @Override
    public void tick() {
        if (!level().isClientSide && ++age > (isDecoy() ? DECOY_LIFETIME_TICKS : LIFETIME_TICKS)) {
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
        if (!isDecoy() && target instanceof LivingEntity) {
            target.hurt(damageSources().indirectMagic(this, getOwner()), DAMAGE);
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
            server.sendParticles(isDecoy() ? ParticleTypes.WITCH : ParticleTypes.ENCHANTED_HIT, getX(), getY(), getZ(), 6, 0.15, 0.15, 0.15, 0.05);
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Decoy", isDecoy());
        tag.putInt("Age", age);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setDecoy(tag.getBoolean("Decoy"));
        age = tag.getInt("Age");
    }
}
