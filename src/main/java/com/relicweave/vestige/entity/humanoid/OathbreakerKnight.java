package com.relicweave.vestige.entity.humanoid;

import com.relicweave.vestige.entity.Telegraph;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Elite guardian that cycles through three stances about every 5 seconds: Guard (70% less frontal damage), Sweep (a
 * telegraphed 180 degree arc) and Lunge (a telegraphed dash of up to 6 blocks). The stance is synced for the pose and
 * saved with the entity.
 */
public class OathbreakerKnight extends Monster implements VisualPose {
    public static final int STANCE_GUARD = 0;
    public static final int STANCE_SWEEP = 1;
    public static final int STANCE_LUNGE = 2;
    public static final int STANCE_TICKS = 100;
    public static final float GUARD_REDUCTION = 0.7F;
    public static final int SWEEP_WINDUP = 15;
    public static final double SWEEP_RADIUS = 3.0;
    public static final int LUNGE_WINDUP = 12;
    public static final double LUNGE_DISTANCE = 6.0;
    public static final double LUNGE_SPEED = 1.0;
    public static final int ATTACK_COOLDOWN = 40;

    private static final EntityDataAccessor<Integer> DATA_STANCE = SynchedEntityData.defineId(OathbreakerKnight.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_POSE = SynchedEntityData.defineId(OathbreakerKnight.class, EntityDataSerializers.INT);

    private int stanceTicks = STANCE_TICKS;
    private int attackCooldown = 20;
    private int phase; // 0 none, 1 wind-up, 2 dashing
    private int phaseTicks;
    private Vec3 lungeDir = Vec3.ZERO;
    private double dashed;

    public OathbreakerKnight(EntityType<? extends OathbreakerKnight> type, Level level) {
        super(type, level);
        setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.NETHERITE_SWORD));
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            setDropChance(slot, 0.0F);
        }
    }

    public static AttributeSupplier.Builder attributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 80.0)
                .add(Attributes.ARMOR, 12.0)
                .add(Attributes.ARMOR_TOUGHNESS, 4.0)
                .add(Attributes.ATTACK_DAMAGE, 12.0)
                .add(Attributes.MOVEMENT_SPEED, 0.27)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.6)
                .add(Attributes.FOLLOW_RANGE, 28.0);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(DATA_STANCE, STANCE_GUARD);
        entityData.define(DATA_POSE, POSE_NONE);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, false));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(6, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    public int stance() {
        return entityData.get(DATA_STANCE);
    }

    public int stanceTicks() {
        return stanceTicks;
    }

    public void setStance(int stance, int ticks) {
        entityData.set(DATA_STANCE, Math.floorMod(stance, 3));
        stanceTicks = ticks;
    }

    @Override
    public int visualPose() {
        return entityData.get(DATA_POSE);
    }

    private void updatePose() {
        int pose = POSE_NONE;
        if (phase == 1 && stance() == STANCE_SWEEP) {
            pose = POSE_OVERHEAD;
        } else if (phase != 0) {
            pose = POSE_REACH;
        } else if (stance() == STANCE_GUARD) {
            pose = POSE_GUARD;
        }
        if (pose != entityData.get(DATA_POSE)) {
            entityData.set(DATA_POSE, pose);
        }
    }

    /** Damage after the Guard stance reduction (70% from the front 120 degrees). */
    public float adjustIncoming(DamageSource source, float amount) {
        if (stance() == STANCE_GUARD && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) && MobCombat.isFront(this, source, 60.0)) {
            return amount * (1.0F - GUARD_REDUCTION);
        }
        return amount;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!level().isClientSide) {
            amount = adjustIncoming(source, amount);
        }
        return super.hurt(source, amount);
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        return phase == 0 && super.doHurtTarget(target);
    }

    private boolean canDamage(Entity e) {
        return e != this && e instanceof LivingEntity && e.isAlive() && !MobCombat.isVestigeMob(e);
    }

    private float damage() {
        return (float) getAttributeValue(Attributes.ATTACK_DAMAGE);
    }

    private void sweep(ServerLevel server) {
        for (Entity e : level().getEntities(this, getBoundingBox().inflate(SWEEP_RADIUS, 1.0, SWEEP_RADIUS), this::canDamage)) {
            double reach = SWEEP_RADIUS + e.getBbWidth() * 0.5;
            if (distanceToSqr(e) <= reach * reach && MobCombat.inFrontArc(this, e.position())) {
                e.hurt(damageSources().mobAttack(this), damage());
                if (e instanceof LivingEntity living) {
                    living.knockback(0.5, getX() - e.getX(), getZ() - e.getZ());
                }
            }
        }
        double yaw = Math.toRadians(yBodyRot);
        server.sendParticles(ParticleTypes.SWEEP_ATTACK, getX() - Math.sin(yaw) * 1.8, getY() + 1.0, getZ() + Math.cos(yaw) * 1.8, 3, 0.8, 0.1, 0.8, 0.0);
    }

    private void tickAttacks(ServerLevel server) {
        LivingEntity target = getTarget();
        if (phase == 0) {
            if (attackCooldown > 0) {
                attackCooldown--;
            } else if (target != null && target.isAlive()) {
                double dist = Math.sqrt(distanceToSqr(target));
                if (stance() == STANCE_SWEEP && dist <= SWEEP_RADIUS + 1.0) {
                    phase = 1;
                    phaseTicks = SWEEP_WINDUP;
                    Telegraph.windUp(this, ParticleTypes.SWEEP_ATTACK, HumanoidMobs.KNIGHT_SWEEP.get(), 0.9F);
                    Telegraph.ring(server, ParticleTypes.SWEEP_ATTACK, position(), SWEEP_RADIUS, 16);
                } else if (stance() == STANCE_LUNGE && dist >= 2.5 && dist <= 9.0 && hasLineOfSight(target)) {
                    Vec3 d = new Vec3(target.getX() - getX(), 0.0, target.getZ() - getZ());
                    lungeDir = d.lengthSqr() < 1.0E-4 ? Vec3.ZERO : d.normalize();
                    phase = 1;
                    phaseTicks = LUNGE_WINDUP;
                    Telegraph.windUp(this, ParticleTypes.ELECTRIC_SPARK, HumanoidMobs.KNIGHT_LUNGE.get(), 1.0F);
                    Telegraph.line(server, ParticleTypes.ELECTRIC_SPARK, position().add(0, 0.2, 0),
                            position().add(lungeDir.scale(LUNGE_DISTANCE)).add(0, 0.2, 0), 12);
                }
            }
        } else if (phase == 1) {
            if (target != null && stance() == STANCE_LUNGE) {
                getLookControl().setLookAt(position().x + lungeDir.x, getEyeY(), position().z + lungeDir.z);
            }
            if (--phaseTicks <= 0) {
                if (stance() == STANCE_SWEEP) {
                    sweep(server);
                    endAttack();
                } else if (lungeDir.lengthSqr() > 0.0) {
                    phase = 2;
                    dashed = 0.0;
                } else {
                    endAttack();
                }
            }
        } else {
            setDeltaMovement(lungeDir.x * LUNGE_SPEED, getDeltaMovement().y, lungeDir.z * LUNGE_SPEED);
            hurtMarked = true;
            dashed += LUNGE_SPEED;
            for (Entity e : level().getEntities(this, getBoundingBox().inflate(0.5, 0.0, 0.5), this::canDamage)) {
                e.hurt(damageSources().mobAttack(this), damage());
                endAttack();
                return;
            }
            if (dashed >= LUNGE_DISTANCE || horizontalCollision) {
                endAttack();
            }
        }
    }

    private void endAttack() {
        phase = 0;
        attackCooldown = ATTACK_COOLDOWN;
        setDeltaMovement(getDeltaMovement().multiply(0.2, 1.0, 0.2));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!(level() instanceof ServerLevel server) || !isAlive()) {
            return;
        }
        if (--stanceTicks <= 0) {
            setStance(stance() + 1, STANCE_TICKS);
        }
        tickAttacks(server);
        updatePose();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Stance", stance());
        tag.putInt("StanceTicks", stanceTicks);
        tag.putInt("AttackCooldown", attackCooldown);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(DATA_STANCE, Math.floorMod(tag.getInt("Stance"), 3));
        stanceTicks = tag.contains("StanceTicks") ? tag.getInt("StanceTicks") : STANCE_TICKS;
        attackCooldown = tag.getInt("AttackCooldown");
    }
}
