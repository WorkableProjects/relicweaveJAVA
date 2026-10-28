package com.relicweave.vestige.entity.beast;

import com.relicweave.vestige.entity.Telegraph;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Heavy melee elite. Slams everything within 3 blocks after a 30-tick wind-up and charges in a straight line from
 * range; a finished charge leaves it exposed for 50 ticks. Crystal back plates cut frontal damage to 60%, but while
 * exposed, hits from the sides and back deal 150%.
 */
public class ShardbackBrute extends Monster {
    public static final int POSE_IDLE = 0;
    public static final int POSE_SLAM = 1;
    public static final int POSE_CHARGE = 2;
    public static final int POSE_EXPOSED = 3;

    public static final int SLAM_WINDUP = 30;
    public static final int CHARGE_WINDUP = 25;
    public static final int EXPOSED_TICKS = 50;
    public static final double SLAM_RADIUS = 3.0;
    public static final double CHARGE_DISTANCE = 10.0;

    private static final EntityDataAccessor<Integer> DATA_POSE = SynchedEntityData.defineId(ShardbackBrute.class, EntityDataSerializers.INT);

    private final LineCharge charge = new LineCharge();
    private int slamTimer;
    private int slamCooldown;
    private int chargeCooldown;
    private int attackCooldown;
    private int exposedTicks;

    public ShardbackBrute(EntityType<? extends ShardbackBrute> type, Level level) {
        super(type, level);
        this.xpReward = 30;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 100.0)
                .add(Attributes.ARMOR, 10.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.9)
                .add(Attributes.ATTACK_DAMAGE, 15.0)
                .add(Attributes.MOVEMENT_SPEED, 0.22)
                .add(Attributes.FOLLOW_RANGE, 24.0);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_POSE, POSE_IDLE);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new CombatGoal());
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.6));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    public int getBrutePose() {
        return this.entityData.get(DATA_POSE);
    }

    public boolean isExposed() {
        return exposedTicks > 0;
    }

    public int getExposedTicks() {
        return exposedTicks;
    }

    public void setExposedTicks(int ticks) {
        this.exposedTicks = ticks;
    }

    /** Frontal hits ×0.6 outside the exposed window; hits from the sides or back ×1.5 inside it. */
    public float modifyIncomingDamage(DamageSource source, float amount) {
        Vec3 from = source.getSourcePosition();
        if (from == null) {
            return amount;
        }
        boolean frontal = BeastUtil.facing(this).dot(BeastUtil.flatDirection(position(), from)) > 0.5;
        if (isExposed()) {
            return frontal ? amount : amount * 1.5F;
        }
        return frontal ? amount * 0.6F : amount;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return super.hurt(source, modifyIncomingDamage(source, amount));
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            return;
        }
        if (slamCooldown > 0) {
            slamCooldown--;
        }
        if (chargeCooldown > 0) {
            chargeCooldown--;
        }
        if (attackCooldown > 0) {
            attackCooldown--;
        }
        if (exposedTicks > 0) {
            exposedTicks--;
            getNavigation().stop();
            setDeltaMovement(0.0, getDeltaMovement().y, 0.0);
            if (tickCount % 4 == 0 && level() instanceof ServerLevel server) {
                server.sendParticles(ParticleTypes.SMOKE, getX(), getY() + 1.8, getZ(), 4, 0.5, 0.2, 0.5, 0.01);
            }
        }
        int pose = exposedTicks > 0 ? POSE_EXPOSED : slamTimer > 0 ? POSE_SLAM : charge.windingUp() ? POSE_CHARGE : POSE_IDLE;
        if (pose != getBrutePose()) {
            this.entityData.set(DATA_POSE, pose);
        }
    }

    private void slam() {
        if (level() instanceof ServerLevel server) {
            Telegraph.ring(server, ParticleTypes.CRIT, position(), SLAM_RADIUS, 24);
            server.sendParticles(ParticleTypes.EXPLOSION, getX(), getY() + 0.2, getZ(), 1, 0, 0, 0, 0);
        }
        for (LivingEntity victim : level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(SLAM_RADIUS, 1.0, SLAM_RADIUS),
                e -> e != this && e.isAlive() && e.getType() != getType() && e.distanceToSqr(this) <= SLAM_RADIUS * SLAM_RADIUS + 1.0)) {
            victim.hurt(damageSources().mobAttack(this), (float) getAttributeValue(Attributes.ATTACK_DAMAGE));
            Vec3 away = BeastUtil.flatDirection(position(), victim.position());
            victim.push(away.x * 0.8, 0.5, away.z * 0.8);
            victim.hurtMarked = true;
        }
    }

    private void chargeHit(LivingEntity victim) {
        victim.hurt(damageSources().mobAttack(this), (float) getAttributeValue(Attributes.ATTACK_DAMAGE));
        Vec3 dir = BeastUtil.facing(this);
        victim.push(dir.x * 1.2, 0.5, dir.z * 1.2);
        victim.hurtMarked = true;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("SlamTimer", slamTimer);
        tag.putInt("SlamCooldown", slamCooldown);
        tag.putInt("ChargeCooldown", chargeCooldown);
        tag.putInt("ExposedTicks", exposedTicks);
        charge.save(tag);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        slamTimer = tag.getInt("SlamTimer");
        slamCooldown = tag.getInt("SlamCooldown");
        chargeCooldown = tag.getInt("ChargeCooldown");
        exposedTicks = tag.getInt("ExposedTicks");
        charge.load(tag);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.RAVAGER_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.RAVAGER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.RAVAGER_DEATH;
    }

    private final class CombatGoal extends Goal {
        CombatGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity t = getTarget();
            return t != null && t.isAlive();
        }

        @Override
        public boolean canContinueToUse() {
            return canUse() || slamTimer > 0 || charge.active() || exposedTicks > 0;
        }

        @Override
        public void stop() {
            slamTimer = 0;
            charge.cancel();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            LivingEntity target = getTarget();
            if (exposedTicks > 0) {
                return;
            }
            if (slamTimer > 0) {
                getNavigation().stop();
                if (target != null) {
                    getLookControl().setLookAt(target);
                }
                if (slamTimer % 10 == 0 && level() instanceof ServerLevel server) {
                    Telegraph.ring(server, ParticleTypes.CRIT, position(), SLAM_RADIUS, 24);
                }
                if (--slamTimer == 0) {
                    slam();
                }
                return;
            }
            if (charge.active()) {
                LineCharge.Phase phase = charge.tick(ShardbackBrute.this, ParticleTypes.CRIT, ShardbackBrute.this::chargeHit);
                if (phase == LineCharge.Phase.FINISHED) {
                    exposedTicks = EXPOSED_TICKS;
                    chargeCooldown = 200;
                    setDeltaMovement(0.0, getDeltaMovement().y, 0.0);
                }
                return;
            }
            if (target == null) {
                return;
            }
            getLookControl().setLookAt(target);
            double dist = distanceTo(target);
            if (dist <= SLAM_RADIUS && slamCooldown == 0) {
                slamTimer = SLAM_WINDUP;
                slamCooldown = 100;
                getNavigation().stop();
                Telegraph.windUp(ShardbackBrute.this, ParticleTypes.CRIT, BeastMobs.BRUTE_SLAM.get(), 0.9F);
            } else if (dist >= 6.0 && dist <= 14.0 && chargeCooldown == 0 && hasLineOfSight(target)) {
                charge.start(ShardbackBrute.this, target, CHARGE_WINDUP, CHARGE_DISTANCE, 0.8, ParticleTypes.CRIT,
                        BeastMobs.BRUTE_CHARGE.get());
            } else {
                getNavigation().moveTo(target, 1.0);
                if (dist < 2.6 && attackCooldown == 0) {
                    attackCooldown = 30;
                    swing(net.minecraft.world.InteractionHand.MAIN_HAND);
                    doHurtTarget(target);
                }
            }
        }
    }
}
