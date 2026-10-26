package com.relicweave.vestige.entity.beast;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.List;

/**
 * Pack hunter. Hounds that share a target flank it from opposite sides; a wounded hound that has lost its pack flees
 * for six seconds and then re-engages.
 */
public class RuinHound extends Monster {
    public static final int FLEE_TICKS = 120;
    public static final int FLEE_COOLDOWN = 200;
    public static final double PACK_RADIUS = 16.0;
    public static final int MAX_PACK = 4;

    private int fleeTimer;
    private int fleeCooldown;
    private int flankCooldown;

    public RuinHound(EntityType<? extends RuinHound> type, Level level) {
        super(type, level);
        this.xpReward = 5;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 22.0)
                .add(Attributes.ATTACK_DAMAGE, 5.0)
                .add(Attributes.MOVEMENT_SPEED, 0.35)
                .add(Attributes.FOLLOW_RANGE, 24.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new FleeGoal());
        this.goalSelector.addGoal(2, new FlankGoal());
        this.goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.2, true));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    /** Other living hounds within {@link #PACK_RADIUS}. */
    public List<RuinHound> packmates() {
        return level().getEntitiesOfClass(RuinHound.class, getBoundingBox().inflate(PACK_RADIUS),
                h -> h != this && h.isAlive());
    }

    public boolean isIsolated() {
        return packmates().isEmpty();
    }

    public int getFleeTimer() {
        return fleeTimer;
    }

    public void setFleeTimer(int ticks) {
        this.fleeTimer = ticks;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            return;
        }
        if (fleeTimer > 0) {
            fleeTimer--;
            if (fleeTimer == 0) {
                fleeCooldown = FLEE_COOLDOWN;
            }
        } else if (fleeCooldown > 0) {
            fleeCooldown--;
        }
        if (flankCooldown > 0) {
            flankCooldown--;
        }
        if (fleeTimer == 0 && fleeCooldown == 0 && getTarget() != null && getHealth() < getMaxHealth() * 0.5F
                && tickCount % 10 == 0 && isIsolated()) {
            fleeTimer = FLEE_TICKS;
        }
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason,
                                        SpawnGroupData data, CompoundTag tag) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, reason, data, tag);
        if (reason != MobSpawnType.SPAWN_EGG && reason != MobSpawnType.COMMAND && level.getRandom().nextBoolean()) {
            int nearby = level.getEntitiesOfClass(RuinHound.class, getBoundingBox().inflate(PACK_RADIUS), h -> h != this && h.isAlive()).size();
            if (nearby < MAX_PACK - 1) {
                ServerLevel server = level.getLevel();
                RuinHound extra = BeastMobs.RUIN_HOUND.get().create(server);
                if (extra != null) {
                    BlockPos pos = blockPosition().offset(level.getRandom().nextInt(3) - 1, 0, level.getRandom().nextInt(3) - 1);
                    extra.moveTo(pos.getX() + 0.5, getY(), pos.getZ() + 0.5, level.getRandom().nextFloat() * 360.0F, 0.0F);
                    if (server.noCollision(extra)) {
                        server.addFreshEntity(extra);
                    }
                }
            }
        }
        return result;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("FleeTimer", fleeTimer);
        tag.putInt("FleeCooldown", fleeCooldown);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        fleeTimer = tag.getInt("FleeTimer");
        fleeCooldown = tag.getInt("FleeCooldown");
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.WOLF_GROWL;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.WOLF_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.WOLF_DEATH;
    }

    @Override
    public float getVoicePitch() {
        return super.getVoicePitch() * 0.75F;
    }

    private final class FleeGoal extends Goal {
        FleeGoal() {
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return fleeTimer > 0;
        }

        @Override
        public boolean canContinueToUse() {
            return fleeTimer > 0;
        }

        @Override
        public void tick() {
            LivingEntity target = getTarget();
            if (getNavigation().isDone()) {
                Vec3 away = target != null
                        ? DefaultRandomPos.getPosAway(RuinHound.this, 12, 5, target.position())
                        : DefaultRandomPos.getPos(RuinHound.this, 10, 4);
                if (away != null) {
                    getNavigation().moveTo(away.x, away.y, away.z, 1.3);
                }
            }
        }
    }

    /** Move to the far side of the target from the nearest other hound that shares it, then let melee take over. */
    private final class FlankGoal extends Goal {
        private int ticks;
        private Vec3 point;

        FlankGoal() {
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = getTarget();
            if (target == null || !target.isAlive() || flankCooldown > 0 || fleeTimer > 0 || distanceTo(target) < 4.0F) {
                return false;
            }
            RuinHound mate = nearestMateOnTarget(target);
            if (mate == null) {
                return false;
            }
            Vec3 toMe = BeastUtil.flatDirection(target.position(), position());
            Vec3 toMate = BeastUtil.flatDirection(target.position(), mate.position());
            if (toMe.dot(toMate) < 0.0) {
                return false;   // already on the opposite side
            }
            point = target.position().add(toMate.scale(-3.0));
            return true;
        }

        private RuinHound nearestMateOnTarget(LivingEntity target) {
            RuinHound best = null;
            double bestDist = Double.MAX_VALUE;
            for (RuinHound other : packmates()) {
                if (other.getTarget() == target && distanceToSqr(other) < bestDist) {
                    best = other;
                    bestDist = distanceToSqr(other);
                }
            }
            return best;
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity target = getTarget();
            return target != null && target.isAlive() && ticks < 60 && fleeTimer == 0 && distanceTo(target) >= 3.5F
                    && !getNavigation().isDone();
        }

        @Override
        public void start() {
            ticks = 0;
            getNavigation().moveTo(point.x, point.y, point.z, 1.3);
        }

        @Override
        public void tick() {
            ticks++;
        }

        @Override
        public void stop() {
            flankCooldown = 60;
        }
    }
}
