package com.relicweave.vestige.boss.c;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.projectile.AbstractHurtingProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/** A glowing page fired by Archivist-Prime's Glyph Volley: no gravity, magic damage, gone on any hit or after 4 seconds. */
public class IndexPage extends AbstractHurtingProjectile {
    public static final float BASE_DAMAGE = 6.0F;
    public static final int LIFETIME_TICKS = 80;

    private float damage = BASE_DAMAGE;
    private int age;

    public IndexPage(EntityType<? extends IndexPage> type, Level level) {
        super(type, level);
    }

    /** Creates and positions a page (not yet added to the level). {@code damage} is already scaled by the owner. */
    public static IndexPage create(Level level, @Nullable Entity owner, Vec3 pos, Vec3 velocity, float damage) {
        IndexPage page = new IndexPage(BossesC.INDEX_PAGE.get(), level);
        page.setPos(pos.x, pos.y, pos.z);
        page.setOwner(owner);
        page.setDeltaMovement(velocity);
        page.damage = damage;
        return page;
    }

    public float damage() {
        return damage;
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
        return ParticleTypes.ENCHANT;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    protected boolean canHitEntity(Entity entity) {
        return super.canHitEntity(entity) && !(entity instanceof Enemy) && !(entity instanceof RuneNode);
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
        if (result.getEntity() instanceof LivingEntity target) {
            target.hurt(damageSources().indirectMagic(this, getOwner()), damage);
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
            server.sendParticles(ParticleTypes.ENCHANTED_HIT, getX(), getY(), getZ(), 6, 0.15, 0.15, 0.15, 0.05);
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("PageDamage", damage);
        tag.putInt("Age", age);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        damage = tag.contains("PageDamage") ? tag.getFloat("PageDamage") : BASE_DAMAGE;
        age = tag.getInt("Age");
    }
}
