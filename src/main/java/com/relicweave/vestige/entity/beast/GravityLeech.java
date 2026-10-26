package com.relicweave.vestige.entity.beast;

import com.relicweave.vestige.entity.Telegraph;
import com.relicweave.vestige.hazard.HazardProtection;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Hazard predator. With line of sight and a target within 12 blocks it telegraphs for 30 ticks, then drags the target
 * toward itself (reduced by Gravity Shear protection for players). A target behind a wall at release is not pulled.
 */
public class GravityLeech extends Monster {
    public static final double PULL_RANGE = 12.0;
    public static final int PULL_TELEGRAPH = 30;
    public static final int PULL_COOLDOWN = 160;
    public static final double PULL_STRENGTH = 0.8;

    private static final EntityDataAccessor<Boolean> DATA_PULLING = SynchedEntityData.defineId(GravityLeech.class, EntityDataSerializers.BOOLEAN);

    private int pullTimer;
    private int pullCooldown;

    public GravityLeech(EntityType<? extends GravityLeech> type, Level level) {
        super(type, level);
        this.xpReward = 8;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 30.0)
                .add(Attributes.ATTACK_DAMAGE, 4.0)
                .add(Attributes.MOVEMENT_SPEED, 0.22)
                .add(Attributes.FOLLOW_RANGE, 20.0);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_PULLING, false);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new PullGoal());
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, true));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.7));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    public boolean isPulling() {
        return this.entityData.get(DATA_PULLING);
    }

    public int getPullCooldown() {
        return pullCooldown;
    }

    public void setPullCooldown(int ticks) {
        this.pullCooldown = ticks;
    }

    /**
     * Drag {@code target} toward this leech if it is within range and visible. Returns whether a pull happened.
     * Players' strength is reduced by Gravity Shear protection.
     */
    public boolean pull(LivingEntity target) {
        if (target == null || !target.isAlive() || distanceTo(target) > PULL_RANGE || !hasLineOfSight(target)) {
            return false;
        }
        double strength = PULL_STRENGTH;
        if (target instanceof Player player) {
            strength *= 1.0 - HazardProtection.reduction(player, HazardProtection.Hazard.GRAVITY_SHEAR);
        }
        Vec3 toward = target.position().vectorTo(position().add(0, 0.5, 0)).normalize();
        target.setDeltaMovement(toward.x * strength, Math.max(toward.y * strength, 0.1), toward.z * strength);
        target.hurtMarked = true;
        return true;
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit) {
            heal(2.0F);
        }
        return hit;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            return;
        }
        if (pullCooldown > 0) {
            pullCooldown--;
        }
        boolean pulling = pullTimer > 0;
        if (pulling != isPulling()) {
            this.entityData.set(DATA_PULLING, pulling);
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("PullTimer", pullTimer);
        tag.putInt("PullCooldown", pullCooldown);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        pullTimer = tag.getInt("PullTimer");
        pullCooldown = tag.getInt("PullCooldown");
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.SLIME_SQUISH_SMALL;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.SLIME_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.SLIME_DEATH;
    }

    private final class PullGoal extends Goal {
        PullGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity t = getTarget();
            return t != null && t.isAlive() && pullCooldown == 0 && distanceTo(t) <= PULL_RANGE && hasLineOfSight(t);
        }

        @Override
        public boolean canContinueToUse() {
            return pullTimer > 0;
        }

        @Override
        public void start() {
            pullTimer = PULL_TELEGRAPH;
            getNavigation().stop();
            Telegraph.windUp(GravityLeech.this, ParticleTypes.REVERSE_PORTAL, BeastMobs.LEECH_PULL.get(), 1.0F);
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            LivingEntity target = getTarget();
            getNavigation().stop();
            if (target == null || !target.isAlive()) {
                pullTimer = 0;
                return;
            }
            getLookControl().setLookAt(target);
            if (pullTimer % 5 == 0 && level() instanceof ServerLevel server) {
                Telegraph.line(server, ParticleTypes.REVERSE_PORTAL, getEyePosition(), target.getEyePosition(), 12);
            }
            if (--pullTimer <= 0) {
                pull(target);
                pullCooldown = PULL_COOLDOWN;
            }
        }

        @Override
        public void stop() {
            pullTimer = 0;
        }
    }
}
