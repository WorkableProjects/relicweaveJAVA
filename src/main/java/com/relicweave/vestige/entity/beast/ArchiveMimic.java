package com.relicweave.vestige.entity.beast;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Structure-only trap. Dormant it is an inert container that creaks now and then; it wakes when a survival player
 * comes within 3 blocks, right-clicks it or hits it, opens its lid with teeth for 10 ticks, then hops and bites.
 * Structures place it with NBT {@code {Dormant:1b}}; the flag survives save/load. Never despawns.
 */
public class ArchiveMimic extends Monster {
    public static final double WAKE_RADIUS = 3.0;
    public static final int REVEAL_TICKS = 10;
    public static final int CREAK_INTERVAL = 200;

    /** Lid poses: closed (dormant), opening (reveal), open and biting. */
    public static final int POSE_CLOSED = 0;
    public static final int POSE_OPENING = 1;
    public static final int POSE_OPEN = 2;

    private static final EntityDataAccessor<Boolean> DATA_DORMANT = SynchedEntityData.defineId(ArchiveMimic.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_LID = SynchedEntityData.defineId(ArchiveMimic.class, EntityDataSerializers.INT);

    private int revealTimer;
    private int creakTimer;
    private int hopCooldown;
    private int biteCooldown;

    public ArchiveMimic(EntityType<? extends ArchiveMimic> type, Level level) {
        super(type, level);
        this.xpReward = 15;
        this.setPersistenceRequired();
        this.creakTimer = 100 + random.nextInt(CREAK_INTERVAL / 2);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 50.0)
                .add(Attributes.ARMOR, 4.0)
                .add(Attributes.ATTACK_DAMAGE, 9.0)
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.FOLLOW_RANGE, 16.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.5);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_DORMANT, true);
        this.entityData.define(DATA_LID, POSE_CLOSED);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new HopBiteGoal());
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 0, false, false,
                e -> awake()));
    }

    public boolean isDormant() {
        return this.entityData.get(DATA_DORMANT);
    }

    public void setDormant(boolean dormant) {
        this.entityData.set(DATA_DORMANT, dormant);
        this.entityData.set(DATA_LID, dormant ? POSE_CLOSED : POSE_OPEN);
        if (dormant) {
            revealTimer = 0;
        }
    }

    public int getLidPose() {
        return this.entityData.get(DATA_LID);
    }

    /** Awake and past the lid-opening reveal. */
    public boolean awake() {
        return !isDormant() && revealTimer <= 0;
    }

    /** Wake up, opening the lid for {@link #REVEAL_TICKS} ticks before it may move or bite. */
    public void wake(LivingEntity cause) {
        if (!isDormant()) {
            return;
        }
        this.entityData.set(DATA_DORMANT, false);
        this.entityData.set(DATA_LID, POSE_OPENING);
        revealTimer = REVEAL_TICKS;
        hopCooldown = 0;
        biteCooldown = 0;
        if (cause != null) {
            setTarget(cause);
        }
        level().playSound(null, getX(), getY(), getZ(), BeastMobs.MIMIC_CREAK.get(), SoundSource.HOSTILE, 1.2F, 0.6F);
        if (level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.SMOKE, getX(), getY() + 0.9, getZ(), 8, 0.3, 0.1, 0.3, 0.02);
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            return;
        }
        if (isDormant()) {
            getNavigation().stop();
            setDeltaMovement(0.0, Math.min(0.0, getDeltaMovement().y), 0.0);
            if (getTarget() != null) {
                setTarget(null);
            }
            if (--creakTimer <= 0) {
                creakTimer = CREAK_INTERVAL;
                level().playSound(null, getX(), getY(), getZ(), BeastMobs.MIMIC_CREAK.get(), SoundSource.HOSTILE, 0.8F, 1.0F);
                if (level() instanceof ServerLevel server) {
                    server.sendParticles(ParticleTypes.SMOKE, getX(), getY() + 1.0, getZ(), 1, 0.1, 0.0, 0.1, 0.01);
                }
            }
            Player near = BeastUtil.nearestPlayer(level(), position(), WAKE_RADIUS);
            if (near != null) {
                wake(near);
            }
            return;
        }
        if (hopCooldown > 0) {
            hopCooldown--;
        }
        if (biteCooldown > 0) {
            biteCooldown--;
        }
        if (revealTimer > 0) {
            getNavigation().stop();
            setDeltaMovement(0.0, getDeltaMovement().y, 0.0);
            if (--revealTimer == 0) {
                this.entityData.set(DATA_LID, POSE_OPEN);
            }
        }
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (isDormant()) {
            if (!level().isClientSide) {
                wake(player);
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        return super.mobInteract(player, hand);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean result = super.hurt(source, amount);
        if (result && isDormant() && !level().isClientSide) {
            wake(source.getEntity() instanceof LivingEntity attacker ? attacker : null);
        }
        return result;
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        return awake() && super.doHurtTarget(target);
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    public boolean isPushable() {
        return !isDormant() && super.isPushable();
    }

    @Override
    public void knockback(double strength, double x, double z) {
        if (!isDormant()) {
            super.knockback(strength, x, z);
        }
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public void checkDespawn() {
        // Persistent: never despawns (also not in peaceful, which would otherwise remove monsters).
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Dormant", isDormant());
        tag.putInt("RevealTimer", revealTimer);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("Dormant")) {
            boolean dormant = tag.getBoolean("Dormant");
            revealTimer = tag.getInt("RevealTimer");
            this.entityData.set(DATA_DORMANT, dormant);
            this.entityData.set(DATA_LID, dormant ? POSE_CLOSED : revealTimer > 0 ? POSE_OPENING : POSE_OPEN);
        }
        setPersistenceRequired();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.WOOD_HIT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.CHEST_CLOSE;
    }

    private final class HopBiteGoal extends Goal {
        HopBiteGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            LivingEntity t = getTarget();
            return awake() && t != null && t.isAlive();
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            LivingEntity target = getTarget();
            getLookControl().setLookAt(target);
            double dist = distanceTo(target);
            if (dist < 1.8 && biteCooldown == 0) {
                biteCooldown = 20;
                swing(InteractionHand.MAIN_HAND);
                doHurtTarget(target);
            } else if (onGround() && hopCooldown == 0 && dist >= 1.2) {
                Vec3 d = BeastUtil.flatDirection(position(), target.position());
                setDeltaMovement(d.x * 0.35, 0.42, d.z * 0.35);
                hasImpulse = true;
                hopCooldown = 14;
            }
        }
    }
}
