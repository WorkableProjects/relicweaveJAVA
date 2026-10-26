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
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomFlyingGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

/**
 * Mobile harasser with a phase dash: a 10-tick telegraph (END_ROD trail ahead plus a cue), then a 5-block dash to a
 * free spot near or behind the target, never into blocks. At most {@link #MAX_DASHES} dashes in a row, then a 4 second
 * cooldown.
 */
public class FractureWisp extends Monster implements WindupState {
    private static final EntityDataAccessor<Boolean> DATA_TELEGRAPH = SynchedEntityData.defineId(FractureWisp.class, EntityDataSerializers.BOOLEAN);
    public static final int MAX_DASHES = 2;
    public static final int TELEGRAPH_TICKS = 10;
    public static final int COOLDOWN_TICKS = 80;
    public static final int GAP_TICKS = 20;
    public static final double DASH_DISTANCE = 5.0;

    private int dashesInRow;
    private int cooldown;
    private int gap;
    private int sinceDash;
    private int telegraphTicks;
    @Nullable
    private Vec3 dashEnd;

    public FractureWisp(EntityType<? extends FractureWisp> type, Level level) {
        super(type, level);
        this.moveControl = new FlyingMoveControl(this, 20, true);
        this.xpReward = 5;
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
        this.entityData.define(DATA_TELEGRAPH, false);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new DashGoal(this));
        this.goalSelector.addGoal(2, new FlyerApproachGoal(this, 1.0, 0.0, 1.2, 2.0, 0.0, () -> telegraphTicks > 0));
        this.goalSelector.addGoal(5, new WanderGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    // ---- dash state (pure counters, directly testable) ----

    public boolean canDash() {
        return cooldown == 0 && gap == 0 && telegraphTicks == 0 && dashesInRow < MAX_DASHES;
    }

    /** Counts a dash as performed: short gap between dashes, long cooldown after the second. */
    public void registerDash() {
        dashesInRow++;
        sinceDash = 0;
        gap = GAP_TICKS;
        if (dashesInRow >= MAX_DASHES) {
            cooldown = COOLDOWN_TICKS;
        }
    }

    public void tickDashTimers() {
        if (gap > 0) {
            gap--;
        }
        if (cooldown > 0 && --cooldown == 0) {
            dashesInRow = 0;
        }
        if (dashesInRow > 0 && cooldown == 0 && ++sinceDash > 100) {
            dashesInRow = 0;
        }
    }

    public int dashesInRow() {
        return dashesInRow;
    }

    public int cooldownTicks() {
        return cooldown;
    }

    public boolean isTelegraphing() {
        return telegraphTicks > 0;
    }

    @Override
    public float windupProgress() {
        return this.entityData.get(DATA_TELEGRAPH) ? 1.0F : 0.0F;
    }

    /** Picks the dash destination and starts the telegraph; false (and a short gap) if nowhere safe to go. */
    boolean beginDash(LivingEntity target) {
        Vec3 end = findDashEnd(target);
        if (end == null) {
            gap = 40;
            return false;
        }
        dashEnd = end;
        telegraphTicks = TELEGRAPH_TICKS;
        this.entityData.set(DATA_TELEGRAPH, true);
        Telegraph.windUp(this, ParticleTypes.END_ROD, EtherealMobs.WISP_DASH.get(), 1.0F);
        if (level() instanceof ServerLevel server) {
            Telegraph.line(server, ParticleTypes.END_ROD, position().add(0, getBbHeight() / 2, 0), end.add(0, getBbHeight() / 2, 0), 8);
        }
        return true;
    }

    /** Free spot up to 5 blocks along the way to a point just behind the target, or null. */
    @Nullable
    Vec3 findDashEnd(LivingEntity target) {
        Vec3 flat = new Vec3(target.getX() - getX(), 0.0, target.getZ() - getZ());
        Vec3 behind = flat.lengthSqr() < 1.0E-4 ? Vec3.ZERO : flat.normalize().scale(1.5);
        Vec3 aim = new Vec3(target.getX() + behind.x, target.getY(0.5), target.getZ() + behind.z);
        Vec3 dir = aim.subtract(position());
        if (dir.lengthSqr() < 0.25) {
            return null;
        }
        double reach = Math.min(DASH_DISTANCE, dir.length());
        dir = dir.normalize();
        for (double len = reach; len >= 2.0; len -= 1.0) {
            Vec3 end = position().add(dir.scale(len));
            AABB box = getBoundingBox().move(end.subtract(position()));
            if (!level().noCollision(this, box)) {
                continue;
            }
            HitResult hit = level().clip(new ClipContext(position(), end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
            if (hit.getType() == HitResult.Type.MISS) {
                return end;
            }
        }
        return null;
    }

    private void finishDash() {
        Vec3 end = dashEnd;
        telegraphTicks = 0;
        this.entityData.set(DATA_TELEGRAPH, false);
        dashEnd = null;
        if (end != null && level() instanceof ServerLevel server) {
            Vec3 from = position().add(0, getBbHeight() / 2, 0);
            Telegraph.line(server, ParticleTypes.END_ROD, from, end.add(0, getBbHeight() / 2, 0), 10);
            moveTo(end.x, end.y, end.z, getYRot(), getXRot());
            setDeltaMovement(Vec3.ZERO);
        }
        registerDash();
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        tickDashTimers();
        if (telegraphTicks > 0 && --telegraphTicks == 0) {
            finishDash();
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide && random.nextInt(3) == 0) {
            level().addParticle(ParticleTypes.END_ROD, getRandomX(0.4), getY(0.5), getRandomZ(0.4), 0, 0.01, 0);
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
        tag.putInt("Dashes", dashesInRow);
        tag.putInt("DashCd", cooldown);
        tag.putInt("DashGap", gap);
        tag.putInt("Telegraph", telegraphTicks);
        if (dashEnd != null) {
            tag.putDouble("DashX", dashEnd.x);
            tag.putDouble("DashY", dashEnd.y);
            tag.putDouble("DashZ", dashEnd.z);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        dashesInRow = tag.getInt("Dashes");
        cooldown = tag.getInt("DashCd");
        gap = tag.getInt("DashGap");
        telegraphTicks = tag.getInt("Telegraph");
        dashEnd = tag.contains("DashX") ? new Vec3(tag.getDouble("DashX"), tag.getDouble("DashY"), tag.getDouble("DashZ")) : null;
        this.entityData.set(DATA_TELEGRAPH, telegraphTicks > 0);
        if (telegraphTicks > 0 && dashEnd == null) {
            telegraphTicks = 0;
            this.entityData.set(DATA_TELEGRAPH, false);
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.AMETHYST_BLOCK_CHIME;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.AMETHYST_BLOCK_BREAK;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.AMETHYST_CLUSTER_BREAK;
    }

    @Override
    public float getVoicePitch() {
        return 1.4F;
    }

    /** Starts a telegraphed dash when the target is 3 to 10 blocks away and a dash is allowed. */
    private static final class DashGoal extends Goal {
        private final FractureWisp wisp;

        DashGoal(FractureWisp wisp) {
            this.wisp = wisp;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = wisp.getTarget();
            if (target == null || !target.isAlive() || !wisp.canDash() || wisp.random.nextInt(15) != 0) {
                return false;
            }
            double d = wisp.distanceTo(target);
            return d >= 3.0 && d <= 10.0 && wisp.hasLineOfSight(target) && wisp.beginDash(target);
        }

        @Override
        public boolean canContinueToUse() {
            return wisp.telegraphTicks > 0;
        }

        @Override
        public void tick() {
            wisp.getNavigation().stop();
            wisp.getMoveControl().setWantedPosition(wisp.getX(), wisp.getY(), wisp.getZ(), 0.1);
        }
    }

    /** Idle wandering. */
    private static final class WanderGoal extends WaterAvoidingRandomFlyingGoal {
        WanderGoal(FractureWisp wisp) {
            super(wisp, 0.8);
        }

        @Override
        public boolean canUse() {
            return mob.getTarget() == null && super.canUse();
        }
    }
}
