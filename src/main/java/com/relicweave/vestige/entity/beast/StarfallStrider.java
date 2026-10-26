package com.relicweave.vestige.entity.beast;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
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
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

/**
 * Caldera predator. Leaves a short-lived heat trail (never placing blocks) that sets players standing on it alight,
 * and dashes at its target after a 15-tick telegraphed wind-up.
 */
public class StarfallStrider extends Monster {
    public static final int TRAIL_MAX = 12;
    public static final int TRAIL_LIFETIME = 40;
    public static final int TRAIL_INTERVAL = 10;
    public static final double TRAIL_RADIUS = 0.8;
    public static final int CHARGE_WINDUP = 15;
    public static final int CHARGE_COOLDOWN = 160;

    private static final EntityDataAccessor<Boolean> DATA_WINDUP = SynchedEntityData.defineId(StarfallStrider.class, EntityDataSerializers.BOOLEAN);

    /** A recorded trail point: x, y, z and remaining ticks. */
    public static final class TrailPoint {
        public final Vec3 pos;
        public int ticksLeft;

        TrailPoint(Vec3 pos, int ticksLeft) {
            this.pos = pos;
            this.ticksLeft = ticksLeft;
        }
    }

    private final List<TrailPoint> trail = new ArrayList<>();
    private final LineCharge charge = new LineCharge();
    private int chargeCooldown;

    public StarfallStrider(EntityType<? extends StarfallStrider> type, Level level) {
        super(type, level);
        this.xpReward = 12;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 45.0)
                .add(Attributes.ATTACK_DAMAGE, 9.0)
                .add(Attributes.MOVEMENT_SPEED, 0.32)
                .add(Attributes.FOLLOW_RANGE, 24.0);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_WINDUP, false);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new ChargeGoal());
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.1, true));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    public boolean isWindingUp() {
        return this.entityData.get(DATA_WINDUP);
    }

    public int getChargeCooldown() {
        return chargeCooldown;
    }

    public void setChargeCooldown(int ticks) {
        this.chargeCooldown = ticks;
    }

    public List<TrailPoint> getTrail() {
        return trail;
    }

    /** Record a trail point at {@code pos} (oldest dropped beyond {@link #TRAIL_MAX}). */
    public void recordTrail(Vec3 pos) {
        trail.add(0, new TrailPoint(pos, TRAIL_LIFETIME));
        while (trail.size() > TRAIL_MAX) {
            trail.remove(trail.size() - 1);
        }
    }

    /** Age the trail by one tick: particles, expiry, and igniting players standing within reach of a point. */
    public void tickTrail() {
        trail.removeIf(p -> --p.ticksLeft <= 0);
        if (!(level() instanceof ServerLevel server)) {
            return;
        }
        for (TrailPoint p : trail) {
            if (p.ticksLeft % 4 == 0) {
                server.sendParticles(p.ticksLeft % 8 == 0 ? ParticleTypes.FLAME : ParticleTypes.SMALL_FLAME,
                        p.pos.x, p.pos.y + 0.1, p.pos.z, 1, 0.15, 0.05, 0.15, 0.0);
            }
        }
        if (trail.isEmpty()) {
            return;
        }
        for (Player player : BeastUtil.playersNear(level(), position(), 40.0)) {
            for (TrailPoint p : trail) {
                double dx = player.getX() - p.pos.x;
                double dz = player.getZ() - p.pos.z;
                double dy = player.getY() - p.pos.y;
                if (dx * dx + dz * dz <= TRAIL_RADIUS * TRAIL_RADIUS && dy > -1.0 && dy < 1.5) {
                    player.setSecondsOnFire(2);
                    break;
                }
            }
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            return;
        }
        if (chargeCooldown > 0) {
            chargeCooldown--;
        }
        if (tickCount % TRAIL_INTERVAL == 0 && getDeltaMovement().horizontalDistanceSqr() > 1.0E-4) {
            recordTrail(position());
        }
        tickTrail();
        boolean winding = charge.windingUp();
        if (winding != isWindingUp()) {
            this.entityData.set(DATA_WINDUP, winding);
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("ChargeCooldown", chargeCooldown);
        charge.save(tag);
        ListTag list = new ListTag();
        for (TrailPoint p : trail) {
            CompoundTag t = new CompoundTag();
            t.putDouble("X", p.pos.x);
            t.putDouble("Y", p.pos.y);
            t.putDouble("Z", p.pos.z);
            t.putInt("T", p.ticksLeft);
            list.add(t);
        }
        tag.put("Trail", list);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        chargeCooldown = tag.getInt("ChargeCooldown");
        charge.load(tag);
        trail.clear();
        for (Tag raw : tag.getList("Trail", Tag.TAG_COMPOUND)) {
            CompoundTag t = (CompoundTag) raw;
            trail.add(new TrailPoint(new Vec3(t.getDouble("X"), t.getDouble("Y"), t.getDouble("Z")), t.getInt("T")));
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.STRIDER_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.STRIDER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.STRIDER_DEATH;
    }

    private void chargeHit(LivingEntity victim) {
        victim.hurt(damageSources().mobAttack(this), (float) getAttributeValue(Attributes.ATTACK_DAMAGE));
        Vec3 dir = BeastUtil.facing(this);
        victim.push(dir.x * 0.8, 0.35, dir.z * 0.8);
        victim.hurtMarked = true;
    }

    private final class ChargeGoal extends Goal {
        ChargeGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity t = getTarget();
            if (t == null || !t.isAlive() || chargeCooldown > 0) {
                return false;
            }
            double d = distanceTo(t);
            return d >= 4.0 && d <= 12.0 && hasLineOfSight(t);
        }

        @Override
        public boolean canContinueToUse() {
            return charge.active();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void start() {
            charge.start(StarfallStrider.this, getTarget(), CHARGE_WINDUP, 6.0, 0.9, ParticleTypes.FLAME,
                    BeastMobs.STRIDER_CHARGE.get());
        }

        @Override
        public void tick() {
            if (charge.tick(StarfallStrider.this, ParticleTypes.FLAME, StarfallStrider.this::chargeHit) == LineCharge.Phase.FINISHED) {
                chargeCooldown = CHARGE_COOLDOWN;
            }
        }

        @Override
        public void stop() {
            charge.cancel();
        }
    }
}
