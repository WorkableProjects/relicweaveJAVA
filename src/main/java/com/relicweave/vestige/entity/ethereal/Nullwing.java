package com.relicweave.vestige.entity.ethereal;

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
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomFlyingGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Flying harasser. It circles 6 to 10 blocks above its target and dives every 8 seconds, or sooner when the target is
 * using an item: a 15-tick screech with an ink burst, the dive, then a climb. After three circles without a dive (for
 * example no line of sight) it lands for 60 ticks, within melee reach, before taking off again.
 */
public class Nullwing extends Monster implements WindupState {
    /** Behaviour states. */
    public enum Mode { CIRCLE, SCREECH, DIVE, CLIMB, LAND }

    private static final EntityDataAccessor<Byte> DATA_MODE = SynchedEntityData.defineId(Nullwing.class, EntityDataSerializers.BYTE);
    public static final int DIVE_INTERVAL = 160;
    public static final int SCREECH_TICKS = 15;
    public static final int LAND_TICKS = 60;
    public static final int MAX_CIRCLES = 3;
    public static final double CIRCLE_RADIUS = 6.0;
    public static final double CIRCLE_HEIGHT = 8.0;
    private static final double CIRCLE_STEP = 0.07;

    private Mode mode = Mode.CIRCLE;
    private int modeTicks;
    private int circlesWithoutDive;
    private double circleAngle;
    private int diveTimer = DIVE_INTERVAL;
    private int sinceDive = 1000;
    private int airborneLanding;
    private int attackCooldown;

    public Nullwing(EntityType<? extends Nullwing> type, Level level) {
        super(type, level);
        this.moveControl = new FlyingMoveControl(this, 20, false);
        this.xpReward = 8;
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation nav = new FlyingPathNavigation(this, level);
        nav.setCanOpenDoors(false);
        nav.setCanFloat(true);
        nav.setCanPassDoors(true);
        return nav;
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_MODE, (byte) 0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(5, new WaterAvoidingRandomFlyingGoal(this, 0.8) {
            @Override
            public boolean canUse() {
                return Nullwing.this.getTarget() == null && super.canUse();
            }
        });
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    // ---- state machine (package-visible so tests can drive it) ----

    public Mode mode() {
        return mode;
    }

    public int circlesWithoutDive() {
        return circlesWithoutDive;
    }

    public int modeTicks() {
        return modeTicks;
    }

    private void setMode(Mode m, int ticks) {
        mode = m;
        modeTicks = ticks;
        this.entityData.set(DATA_MODE, (byte) m.ordinal());
    }

    @Override
    public float windupProgress() {
        int m = this.entityData.get(DATA_MODE);
        return m == Mode.SCREECH.ordinal() || m == Mode.DIVE.ordinal() ? 1.0F : 0.0F;
    }

    /** One full circle was flown. Three in a row without a dive forces a landing. */
    public void completeCircle() {
        if (mode != Mode.CIRCLE) {
            return;
        }
        if (++circlesWithoutDive >= MAX_CIRCLES) {
            land();
        }
    }

    public void land() {
        circlesWithoutDive = 0;
        airborneLanding = 0;
        setMode(Mode.LAND, LAND_TICKS);
    }

    /** One tick spent grounded: after {@link #LAND_TICKS} it climbs back up. */
    public void tickLanding() {
        if (mode == Mode.LAND && --modeTicks <= 0) {
            diveTimer = 60;
            setMode(Mode.CLIMB, 30);
        }
    }

    private void startScreech(LivingEntity target) {
        setMode(Mode.SCREECH, SCREECH_TICKS);
        Telegraph.windUp(this, ParticleTypes.SQUID_INK, EtherealMobs.NULLWING_SCREECH.get(), 1.0F);
        if (level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.SQUID_INK, getX(), getY() + 0.4, getZ(), 24, 0.5, 0.3, 0.5, 0.05);
        }
    }

    private void startDive() {
        circlesWithoutDive = 0;
        sinceDive = 0;
        setMode(Mode.DIVE, 40);
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (attackCooldown > 0) {
            attackCooldown--;
        }
        sinceDive++;
        LivingEntity target = getTarget();
        if (target == null || !target.isAlive()) {
            if (mode != Mode.CIRCLE) {
                setMode(Mode.CIRCLE, 0);
            }
            circleAngle = 0.0;
            return;
        }
        switch (mode) {
            case CIRCLE -> tickCircle(target);
            case SCREECH -> {
                getMoveControl().setWantedPosition(getX(), getY(), getZ(), 0.1);
                getLookControl().setLookAt(target, 60.0F, 60.0F);
                if (--modeTicks <= 0) {
                    startDive();
                }
            }
            case DIVE -> {
                Vec3 eye = target.getEyePosition();
                getMoveControl().setWantedPosition(eye.x, eye.y, eye.z, 2.4);
                getLookControl().setLookAt(target, 60.0F, 60.0F);
                if (distanceToSqr(target.getX(), target.getY(0.5), target.getZ()) < 2.6) {
                    swing(net.minecraft.world.InteractionHand.MAIN_HAND);
                    doHurtTarget(target);
                    setMode(Mode.CLIMB, 30);
                } else if (--modeTicks <= 0) {
                    setMode(Mode.CLIMB, 30);
                }
            }
            case CLIMB -> {
                getMoveControl().setWantedPosition(target.getX(), target.getY() + CIRCLE_HEIGHT, target.getZ(), 1.2);
                if (getY() >= target.getY() + CIRCLE_HEIGHT - 2.0 || --modeTicks <= 0) {
                    diveTimer = DIVE_INTERVAL;
                    setMode(Mode.CIRCLE, 0);
                }
            }
            case LAND -> tickGrounded(target);
        }
    }

    private void tickCircle(LivingEntity target) {
        double dx = target.getX() - getX();
        double dz = target.getZ() - getZ();
        if (dx * dx + dz * dz > 16.0 * 16.0) {
            getMoveControl().setWantedPosition(target.getX(), target.getY() + CIRCLE_HEIGHT, target.getZ(), 1.0);
            return;
        }
        if (diveTimer > 0) {
            diveTimer--;
        }
        circleAngle += CIRCLE_STEP;
        if (circleAngle >= Math.PI * 2.0) {
            circleAngle -= Math.PI * 2.0;
            completeCircle();
            if (mode == Mode.LAND) {
                return;
            }
        }
        getMoveControl().setWantedPosition(target.getX() + Math.cos(circleAngle) * CIRCLE_RADIUS, target.getY() + CIRCLE_HEIGHT,
                target.getZ() + Math.sin(circleAngle) * CIRCLE_RADIUS, 1.0);
        getLookControl().setLookAt(target, 30.0F, 30.0F);
        boolean itemTrigger = target.isUsingItem() && sinceDive >= 60;
        if ((diveTimer <= 0 || itemTrigger) && hasLineOfSight(target)) {
            startScreech(target);
        }
    }

    private void tickGrounded(LivingEntity target) {
        if (onGround()) {
            tickLanding();
            if (attackCooldown <= 0 && distanceToSqr(target) < 3.5) {
                attackCooldown = 20;
                swing(net.minecraft.world.InteractionHand.MAIN_HAND);
                doHurtTarget(target);
            }
        } else if (++airborneLanding > 100) {
            // Never found the ground (water, a ledge): do not hang in the air forever.
            diveTimer = 60;
            setMode(Mode.CLIMB, 30);
        }
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    public boolean canChangeDimensions() {
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putByte("Mode", (byte) mode.ordinal());
        tag.putInt("ModeTicks", modeTicks);
        tag.putInt("Circles", circlesWithoutDive);
        tag.putInt("DiveTimer", diveTimer);
        tag.putInt("SinceDive", sinceDive);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        int m = tag.getByte("Mode");
        Mode loaded = m >= 0 && m < Mode.values().length ? Mode.values()[m] : Mode.CIRCLE;
        setMode(loaded, tag.getInt("ModeTicks"));
        circlesWithoutDive = tag.getInt("Circles");
        diveTimer = tag.contains("DiveTimer") ? tag.getInt("DiveTimer") : DIVE_INTERVAL;
        sinceDive = tag.contains("SinceDive") ? tag.getInt("SinceDive") : 1000;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.PHANTOM_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.PHANTOM_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.PHANTOM_DEATH;
    }

    @Override
    public float getVoicePitch() {
        return 0.7F;
    }
}
