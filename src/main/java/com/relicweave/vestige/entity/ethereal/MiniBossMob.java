package com.relicweave.vestige.entity.ethereal;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Base of the three structure mini-bosses: ground melee {@link Monster} with {@link MiniBossSupport}, plus a tiny
 * wind-up state machine. A subclass picks an attack in {@link #chooseAttack}, calls {@link #beginAttack}, and the base
 * counts the (synced) wind-up down, keeps the mob planted, redraws the telegraph and calls {@link #resolveAttack}.
 */
public abstract class MiniBossMob extends Monster implements WindupState {
    private static final EntityDataAccessor<Integer> DATA_WINDUP = SynchedEntityData.defineId(MiniBossMob.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_WINDUP_TOTAL = SynchedEntityData.defineId(MiniBossMob.class, EntityDataSerializers.INT);

    protected final MiniBossSupport support;
    protected int attackId = -1;
    protected int attackCooldown;
    /** Fixed aim point captured when a wind-up starts (beams and lances are dodgeable). */
    protected Vec3 aim = Vec3.ZERO;

    protected MiniBossMob(net.minecraft.world.entity.EntityType<? extends MiniBossMob> type, Level level, BossEvent.BossBarColor color) {
        super(type, level);
        this.xpReward = 50;
        this.support = new MiniBossSupport(this, color, this::onBossReset);
    }

    public MiniBossSupport support() {
        return support;
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_WINDUP, 0);
        this.entityData.define(DATA_WINDUP_TOTAL, 0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new GatedMelee(this));
        this.goalSelector.addGoal(5, new GatedStroll(this));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    // ---- state machine ----

    public int windup() {
        return this.entityData.get(DATA_WINDUP);
    }

    @Override
    public float windupProgress() {
        int total = this.entityData.get(DATA_WINDUP_TOTAL);
        return total <= 0 ? 0.0F : Math.min(1.0F, 1.0F - windup() / (float) total);
    }

    /** True while winding up or otherwise unable to act (melee and strolling pause). */
    public boolean isBusy() {
        return windup() > 0 || isFrozen();
    }

    /** Subclass hook for "cannot act at all" states (dormant, shielded). */
    protected boolean isFrozen() {
        return false;
    }

    protected void beginAttack(int id, int windupTicks) {
        this.attackId = id;
        int ticks = Math.max(1, windupTicks);
        this.entityData.set(DATA_WINDUP, ticks);
        this.entityData.set(DATA_WINDUP_TOTAL, ticks);
    }

    protected abstract void chooseAttack(LivingEntity target);

    protected abstract void resolveAttack(int id);

    /** Called every wind-up tick with the remaining ticks; redraw the telegraph here. */
    protected void onWindupTick(int id, int remaining) {}

    protected void onBossReset() {
        this.attackId = -1;
        this.attackCooldown = 20;
        this.entityData.set(DATA_WINDUP, 0);
        this.entityData.set(DATA_WINDUP_TOTAL, 0);
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide) {
            support.tick();
        }
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (attackCooldown > 0) {
            attackCooldown--;
        }
        if (isFrozen()) {
            getNavigation().stop();
            return;
        }
        int w = windup();
        if (w > 0) {
            getNavigation().stop();
            LivingEntity target = getTarget();
            if (target != null) {
                getLookControl().setLookAt(target, 30.0F, 30.0F);
            }
            this.entityData.set(DATA_WINDUP, w - 1);
            onWindupTick(attackId, w - 1);
            if (w - 1 <= 0) {
                int id = attackId;
                attackId = -1;
                this.entityData.set(DATA_WINDUP_TOTAL, 0);
                resolveAttack(id);
            }
            return;
        }
        LivingEntity target = getTarget();
        if (target != null && target.isAlive() && attackCooldown <= 0) {
            chooseAttack(target);
        }
    }

    // ---- helpers ----

    /** Survival-damageable players within a flat radius of {@code center}. */
    protected List<Player> playersWithin(Vec3 center, double radius) {
        return level().getEntitiesOfClass(Player.class, new AABB(center, center).inflate(radius, 3.0, radius),
                p -> p.isAlive() && !p.isSpectator() && horizontalDistance(p.position(), center) <= radius);
    }

    protected static double horizontalDistance(Vec3 a, Vec3 b) {
        double dx = a.x - b.x;
        double dz = a.z - b.z;
        return Math.sqrt(dx * dx + dz * dz);
    }

    /** Distance from {@code p} to the segment {@code a}-{@code b}. */
    public static double distanceToSegment(Vec3 p, Vec3 a, Vec3 b) {
        Vec3 ab = b.subtract(a);
        double len2 = ab.lengthSqr();
        if (len2 < 1.0E-8) {
            return p.distanceTo(a);
        }
        double t = Math.max(0.0, Math.min(1.0, p.subtract(a).dot(ab) / len2));
        return p.distanceTo(a.add(ab.scale(t)));
    }

    /** Players within {@code width} of the segment (fixed beam / lance). */
    protected List<Player> playersOnLine(Vec3 from, Vec3 to, double width) {
        return level().getEntitiesOfClass(Player.class, new AABB(from, to).inflate(width + 1.0),
                p -> p.isAlive() && !p.isSpectator()
                        && distanceToSegment(p.position().add(0, p.getBbHeight() / 2.0, 0), from, to) <= width);
    }

    protected ServerLevel serverLevel() {
        return (ServerLevel) level();
    }

    // ---- lifecycle ----

    @Override
    public boolean canChangeDimensions() {
        return false;
    }

    @Override
    public void setCustomName(@Nullable Component name) {
        super.setCustomName(name);
        support.setName(getDisplayName());
    }

    @Override
    public void die(net.minecraft.world.damagesource.DamageSource source) {
        super.die(source);
        support.close();
    }

    @Override
    public void remove(RemovalReason reason) {
        support.close();
        super.remove(reason);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        support.save(tag);
        tag.putInt("AttackId", attackId);
        tag.putInt("Windup", windup());
        tag.putInt("WindupTotal", this.entityData.get(DATA_WINDUP_TOTAL));
        tag.putInt("AttackCd", attackCooldown);
        tag.putDouble("AimX", aim.x);
        tag.putDouble("AimY", aim.y);
        tag.putDouble("AimZ", aim.z);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        support.load(tag);
        attackId = tag.contains("AttackId") ? tag.getInt("AttackId") : -1;
        this.entityData.set(DATA_WINDUP, tag.getInt("Windup"));
        this.entityData.set(DATA_WINDUP_TOTAL, tag.getInt("WindupTotal"));
        attackCooldown = tag.getInt("AttackCd");
        aim = new Vec3(tag.getDouble("AimX"), tag.getDouble("AimY"), tag.getDouble("AimZ"));
        if (hasCustomName()) {
            support.setName(getDisplayName());
        }
    }

    /** Melee that pauses while the boss is winding up or frozen. */
    private static final class GatedMelee extends MeleeAttackGoal {
        private final MiniBossMob boss;

        GatedMelee(MiniBossMob boss) {
            super(boss, 1.0, false);
            this.boss = boss;
        }

        @Override
        public boolean canUse() {
            return !boss.isBusy() && super.canUse();
        }

        @Override
        public boolean canContinueToUse() {
            return !boss.isBusy() && super.canContinueToUse();
        }
    }

    /** Idle stroll that pauses while frozen. */
    private static final class GatedStroll extends WaterAvoidingRandomStrollGoal {
        private final MiniBossMob boss;

        GatedStroll(MiniBossMob boss) {
            super(boss, 0.6);
            this.boss = boss;
        }

        @Override
        public boolean canUse() {
            return !boss.isBusy() && super.canUse();
        }
    }
}
