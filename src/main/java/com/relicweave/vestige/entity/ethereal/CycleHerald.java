package com.relicweave.vestige.entity.ethereal;

import com.relicweave.vestige.cycle.ConcordanceCycle;
import com.relicweave.vestige.cycle.CyclePhase;
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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Rare Convergence event mob. Floats slowly and fires the Phase Lance: a 20-tick telegraphed line, then 8 damage along
 * the fixed line. Vanishes (no drops) when the phase leaves Convergence, or after {@link #MAX_LIFETIME} ticks.
 * Only Heralds spawned by the Convergence event vanish with the phase; an egg-spawned one only times out.
 */
public class CycleHerald extends Monster implements WindupState {
    private static final EntityDataAccessor<Integer> DATA_WINDUP = SynchedEntityData.defineId(CycleHerald.class, EntityDataSerializers.INT);
    public static final int MAX_LIFETIME = 6000;
    public static final int LANCE_WINDUP = 20;
    public static final float LANCE_DAMAGE = 8.0F;
    public static final double LANCE_LENGTH = 28.0;
    public static final double LANCE_WIDTH = 1.0;

    private int lifetime;
    private int lanceCooldown = 60;
    private boolean eventBound;
    private Vec3 aim = Vec3.ZERO;

    public CycleHerald(EntityType<? extends CycleHerald> type, Level level) {
        super(type, level);
        this.moveControl = new FlyingMoveControl(this, 20, true);
        this.xpReward = 20;
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
        this.entityData.define(DATA_WINDUP, 0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(2, new FlyerApproachGoal(this, 0.6, 8.0, 14.0, 0.0, 3.0, () -> false));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomFlyingGoal(this, 0.5) {
            @Override
            public boolean canUse() {
                return CycleHerald.this.getTarget() == null && super.canUse();
            }
        });
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    public void setEventBound(boolean eventBound) {
        this.eventBound = eventBound;
    }

    public boolean isEventBound() {
        return eventBound;
    }

    public int lifetime() {
        return lifetime;
    }

    @Override
    public float windupProgress() {
        int w = this.entityData.get(DATA_WINDUP);
        return w <= 0 ? 0.0F : 1.0F - w / (float) LANCE_WINDUP;
    }

    private Vec3 origin() {
        return position().add(0.0, 1.4, 0.0);
    }

    private Vec3 end() {
        Vec3 dir = aim.subtract(origin());
        return dir.lengthSqr() < 1.0E-6 ? origin() : origin().add(dir.normalize().scale(LANCE_LENGTH));
    }

    /** Removes the Herald with a portal burst and no drops. */
    public void vanish() {
        if (level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.PORTAL, getX(), getY() + 1.0, getZ(), 48, 0.4, 0.8, 0.4, 0.3);
        }
        discard();
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        lifetime++;
        if (lifetime >= MAX_LIFETIME
                || (eventBound && lifetime % 20 == 0 && ConcordanceCycle.phase(level()) != CyclePhase.CONVERGENCE)) {
            vanish();
            return;
        }
        if (lanceCooldown > 0) {
            lanceCooldown--;
        }
        int w = this.entityData.get(DATA_WINDUP);
        if (w > 0) {
            getNavigation().stop();
            this.entityData.set(DATA_WINDUP, w - 1);
            if ((w - 1) % 5 == 0 && w - 1 > 0) {
                Telegraph.line((ServerLevel) level(), ParticleTypes.END_ROD, origin(), end(), 28);
            }
            if (w - 1 <= 0) {
                fireLance();
            }
            return;
        }
        LivingEntity target = getTarget();
        if (target != null && target.isAlive() && lanceCooldown <= 0 && hasLineOfSight(target) && distanceTo(target) <= LANCE_LENGTH) {
            aim = target.position().add(0.0, 1.0, 0.0);
            this.entityData.set(DATA_WINDUP, LANCE_WINDUP);
            Telegraph.windUp(this, ParticleTypes.END_ROD, EtherealMobs.HERALD_LANCE.get(), 1.0F);
            Telegraph.line((ServerLevel) level(), ParticleTypes.END_ROD, origin(), end(), 28);
        }
    }

    private void fireLance() {
        lanceCooldown = 80;
        ServerLevel server = (ServerLevel) level();
        Vec3 from = origin();
        Vec3 to = end();
        List<Player> hit = level().getEntitiesOfClass(Player.class, new AABB(from, to).inflate(LANCE_WIDTH + 1.0),
                p -> p.isAlive() && !p.isSpectator()
                        && MiniBossMob.distanceToSegment(p.position().add(0, p.getBbHeight() / 2.0, 0), from, to) <= LANCE_WIDTH);
        for (Player player : hit) {
            player.hurt(damageSources().mobAttack(this), LANCE_DAMAGE);
        }
        Telegraph.line(server, ParticleTypes.PORTAL, from, to, 40);
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
        tag.putInt("Lifetime", lifetime);
        tag.putInt("LanceCd", lanceCooldown);
        tag.putInt("LanceWindup", this.entityData.get(DATA_WINDUP));
        tag.putBoolean("EventBound", eventBound);
        tag.putDouble("AimX", aim.x);
        tag.putDouble("AimY", aim.y);
        tag.putDouble("AimZ", aim.z);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        lifetime = tag.getInt("Lifetime");
        lanceCooldown = tag.getInt("LanceCd");
        this.entityData.set(DATA_WINDUP, tag.getInt("LanceWindup"));
        eventBound = tag.getBoolean("EventBound");
        aim = new Vec3(tag.getDouble("AimX"), tag.getDouble("AimY"), tag.getDouble("AimZ"));
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.ALLAY_AMBIENT_WITHOUT_ITEM;
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
        return 0.5F;
    }
}
