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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Ambush predator. Without a target it lurks (motionless, 60% translucent, silent). A survival player within 10 blocks
 * makes it reveal: a 20-tick shiver with a sound and particles during which it cannot hurt anyone. Only then does it
 * hunt, pouncing with a lunge on a cooldown.
 */
public class GlassrootStalker extends Monster {
    public static final int LURK = 0;
    public static final int REVEALING = 1;
    public static final int ACTIVE = 2;
    public static final int REVEAL_TICKS = 20;
    public static final int LUNGE_COOLDOWN = 120;
    public static final double LURK_RADIUS = 10.0;

    private static final EntityDataAccessor<Integer> DATA_STATE = SynchedEntityData.defineId(GlassrootStalker.class, EntityDataSerializers.INT);

    private int revealTimer;
    private int lungeCooldown;
    private int idleTicks;

    public GlassrootStalker(EntityType<? extends GlassrootStalker> type, Level level) {
        super(type, level);
        this.xpReward = 10;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 34.0)
                .add(Attributes.ATTACK_DAMAGE, 8.0)
                .add(Attributes.MOVEMENT_SPEED, 0.30)
                .add(Attributes.FOLLOW_RANGE, 20.0);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_STATE, LURK);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new LungeGoal());
        this.goalSelector.addGoal(2, new StalkerMeleeGoal());
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false,
                e -> getState() == ACTIVE));
    }

    public int getState() {
        return this.entityData.get(DATA_STATE);
    }

    public void setState(int state) {
        this.entityData.set(DATA_STATE, state);
    }

    public boolean isLurking() {
        return getState() == LURK;
    }

    public int getRevealTimer() {
        return revealTimer;
    }

    public int getLungeCooldown() {
        return lungeCooldown;
    }

    public void setLungeCooldown(int ticks) {
        this.lungeCooldown = ticks;
    }

    /** Start the shiver. Harmless until {@link #REVEAL_TICKS} ticks have passed. */
    public void beginReveal(LivingEntity cause) {
        if (getState() != LURK) {
            return;
        }
        setState(REVEALING);
        revealTimer = REVEAL_TICKS;
        if (cause != null) {
            setTarget(cause);
        }
        Telegraph.windUp(this, ParticleTypes.END_ROD, BeastMobs.STALKER_REVEAL.get(), 1.0F);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            return;
        }
        if (lungeCooldown > 0) {
            lungeCooldown--;
        }
        switch (getState()) {
            case LURK -> {
                getNavigation().stop();
                setDeltaMovement(0.0, getDeltaMovement().y, 0.0);
                if (getTarget() != null) {
                    setTarget(null);
                }
                Player nearest = BeastUtil.nearestPlayer(level(), position(), LURK_RADIUS);
                if (nearest != null) {
                    beginReveal(nearest);
                }
            }
            case REVEALING -> {
                getNavigation().stop();
                setDeltaMovement(0.0, getDeltaMovement().y, 0.0);
                if (level() instanceof ServerLevel server && revealTimer % 2 == 0) {
                    server.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 0.9, getZ(), 2, 0.4, 0.6, 0.4, 0.01);
                }
                if (--revealTimer <= 0) {
                    setState(ACTIVE);
                    idleTicks = 0;
                    if (getTarget() == null) {
                        Player nearest = BeastUtil.nearestPlayer(level(), position(), 16.0);
                        if (nearest != null) {
                            setTarget(nearest);
                        }
                    }
                }
            }
            default -> {
                if (getTarget() == null || !getTarget().isAlive()) {
                    if (++idleTicks > 100) {
                        setState(LURK);
                    }
                } else {
                    idleTicks = 0;
                }
            }
        }
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        // It never attacks from lurk, and never before the reveal finished.
        return getState() == ACTIVE && super.doHurtTarget(target);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean result = super.hurt(source, amount);
        if (result && getState() == LURK && source.getEntity() instanceof LivingEntity attacker) {
            beginReveal(attacker);
        }
        return result;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("StalkerState", getState());
        tag.putInt("RevealTimer", revealTimer);
        tag.putInt("LungeCooldown", lungeCooldown);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setState(tag.getInt("StalkerState"));
        revealTimer = tag.getInt("RevealTimer");
        lungeCooldown = tag.getInt("LungeCooldown");
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return getState() == ACTIVE ? SoundEvents.OCELOT_AMBIENT : null;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.AMETHYST_CLUSTER_HIT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.AMETHYST_CLUSTER_BREAK;
    }

    @Override
    public float getVoicePitch() {
        return super.getVoicePitch() * 0.7F;
    }

    private final class StalkerMeleeGoal extends MeleeAttackGoal {
        StalkerMeleeGoal() {
            super(GlassrootStalker.this, 1.2, true);
        }

        @Override
        public boolean canUse() {
            return getState() == ACTIVE && super.canUse();
        }

        @Override
        public boolean canContinueToUse() {
            return getState() == ACTIVE && super.canContinueToUse();
        }
    }

    private final class LungeGoal extends Goal {
        LungeGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = getTarget();
            if (getState() != ACTIVE || target == null || !target.isAlive() || lungeCooldown > 0 || !onGround()) {
                return false;
            }
            double d = distanceTo(target);
            return d >= 3.0 && d <= 8.0 && hasLineOfSight(target);
        }

        @Override
        public boolean canContinueToUse() {
            return false;
        }

        @Override
        public void start() {
            LivingEntity target = getTarget();
            Vec3 d = BeastUtil.flatDirection(position(), target.position());
            setDeltaMovement(d.x * 0.9, 0.45, d.z * 0.9);
            hasImpulse = true;
            lungeCooldown = LUNGE_COOLDOWN;
            if (level() instanceof ServerLevel server) {
                server.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 0.5, getZ(), 6, 0.3, 0.3, 0.3, 0.02);
            }
        }
    }
}
