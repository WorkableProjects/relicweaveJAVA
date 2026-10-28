package com.relicweave.vestige.boss.c;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * A crystal pedestal raised by a T13 boss. It never moves, never drops anything and takes damage only while
 * {@link #isExposed() exposed}; otherwise hits make a clink and do nothing. Its owner (a boss, by UUID) controls which
 * node is exposed; a node whose owner has vanished removes itself after ten seconds so none can be left behind.
 */
public abstract class RuneNode extends Mob {
    private static final EntityDataAccessor<Boolean> DATA_EXPOSED = SynchedEntityData.defineId(RuneNode.class, EntityDataSerializers.BOOLEAN);
    private static final int ORPHAN_TICKS = 200;

    @Nullable
    private UUID owner;
    private int orphanTicks;

    protected RuneNode(EntityType<? extends RuneNode> type, Level level) {
        super(type, level);
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder attributes(double health) {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, health).add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(DATA_EXPOSED, false);
    }

    /** True if hits currently hurt it. Index nodes are always exposed. */
    public boolean isExposed() {
        return entityData.get(DATA_EXPOSED);
    }

    /** Server: exposes or covers the node; becoming exposed plays its cue. */
    public void setExposed(boolean exposed) {
        boolean was = entityData.get(DATA_EXPOSED);
        entityData.set(DATA_EXPOSED, exposed);
        if (exposed && !was && level() instanceof ServerLevel server) {
            server.playSound(null, getX(), getY(), getZ(), BossesC.SEAL_NODE_EXPOSED.get(), SoundSource.HOSTILE, 1.2F, 1.0F);
        }
    }

    public void setOwner(@Nullable UUID owner) {
        this.owner = owner;
    }

    @Nullable
    public UUID owner() {
        return owner;
    }

    @Override
    public boolean isNoAi() {
        return true;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public void push(Entity entity) {}

    @Override
    public void knockback(double strength, double x, double z) {}

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    protected boolean shouldDropLoot() {
        return false;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!level().isClientSide && !isExposed() && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            BossCombat.clink(this);
            return false;
        }
        return super.hurt(source, amount);
    }

    @Override
    public void die(DamageSource source) {
        if (level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 1.0, getZ(), 24, 0.4, 0.6, 0.4, 0.08);
            server.playSound(null, getX(), getY(), getZ(), net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.HOSTILE, 1.5F, 0.7F);
        }
        super.die(source);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!(level() instanceof ServerLevel server) || !isAlive()) {
            return;
        }
        if (isExposed() && tickCount % 6 == 0) {
            server.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 1.4, getZ(), 2, 0.25, 0.5, 0.25, 0.01);
        }
        if (owner != null && tickCount % 10 == 0) {
            Entity boss = server.getEntity(owner);
            if (boss == null || !boss.isAlive()) {
                orphanTicks += 10;
                if (orphanTicks >= ORPHAN_TICKS) {
                    discard();
                }
            } else {
                orphanTicks = 0;
            }
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Exposed", entityData.get(DATA_EXPOSED));
        if (owner != null) {
            tag.putUUID("NodeOwner", owner);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(DATA_EXPOSED, tag.getBoolean("Exposed"));
        owner = tag.hasUUID("NodeOwner") ? tag.getUUID("NodeOwner") : null;
    }
}
