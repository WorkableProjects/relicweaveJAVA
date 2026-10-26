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
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
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
 * Ranged caster that keeps 8-14 blocks away. A cast winds up for 20 ticks (arms raised, enchant glyphs), then fires
 * glyph bolts, alternating a single aimed bolt and a 25 degree three-bolt fan. Afterwards it is in Recovery for 30
 * ticks: it cannot move and takes 50% more damage.
 */
public class ScriptboundScribe extends Monster implements VisualPose {
    public static final int WINDUP_TICKS = 20;
    public static final int RECOVERY_TICKS = 30;
    public static final int COOLDOWN_TICKS = 50;
    public static final float RECOVERY_BONUS = 0.5F;
    public static final double BOLT_SPEED = 1.1;
    public static final double FAN_DEGREES = 25.0;

    private static final EntityDataAccessor<Integer> DATA_PHASE = SynchedEntityData.defineId(ScriptboundScribe.class, EntityDataSerializers.INT);
    private static final int IDLE = 0;
    private static final int CASTING = 1;
    private static final int RECOVERING = 2;

    private int phaseTicks;
    private int cooldown = COOLDOWN_TICKS;
    private boolean fanNext;

    public ScriptboundScribe(EntityType<? extends ScriptboundScribe> type, Level level) {
        super(type, level);
        setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BOOK));
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            setDropChance(slot, 0.0F);
        }
    }

    public static AttributeSupplier.Builder attributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 28.0)
                .add(Attributes.ARMOR, 2.0)
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.FOLLOW_RANGE, 24.0);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(DATA_PHASE, IDLE);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new RangeKeepGoal(this, 8.0, 14.0, 1.0));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.7));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 10.0F));
        goalSelector.addGoal(6, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    public boolean isRecovering() {
        return entityData.get(DATA_PHASE) == RECOVERING;
    }

    public boolean isCasting() {
        return entityData.get(DATA_PHASE) == CASTING;
    }

    public boolean fanNext() {
        return fanNext;
    }

    public void setFanNext(boolean fan) {
        fanNext = fan;
    }

    public int recoveryTicks() {
        return isRecovering() ? phaseTicks : 0;
    }

    /** Test/command hook: enter Recovery with {@code ticks} left. */
    public void setRecoveryTicks(int ticks) {
        entityData.set(DATA_PHASE, ticks > 0 ? RECOVERING : IDLE);
        phaseTicks = ticks;
    }

    @Override
    public int visualPose() {
        return isCasting() ? POSE_RAISED : POSE_NONE;
    }

    @Override
    protected boolean isImmobile() {
        return super.isImmobile() || isRecovering();
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!level().isClientSide && isRecovering() && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            amount *= 1.0F + RECOVERY_BONUS;
        }
        return super.hurt(source, amount);
    }

    /** Fires a single bolt or a 25 degree fan of three at {@code target}. Server side. */
    public void fireBolts(LivingEntity target, boolean fan) {
        Vec3 origin = new Vec3(getX(), getEyeY() - 0.2, getZ());
        Vec3 aim = target.getBoundingBox().getCenter().subtract(origin).normalize();
        double[] angles = fan ? new double[] {-FAN_DEGREES, 0.0, FAN_DEGREES} : new double[] {0.0};
        for (double angle : angles) {
            Vec3 dir = aim.yRot((float) Math.toRadians(angle));
            GlyphBolt bolt = GlyphBolt.create(level(), this, origin.add(dir.scale(0.6)), dir.scale(BOLT_SPEED), false);
            level().addFreshEntity(bolt);
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!(level() instanceof ServerLevel server) || !isAlive()) {
            return;
        }
        LivingEntity target = getTarget();
        switch (entityData.get(DATA_PHASE)) {
            case IDLE -> {
                if (cooldown > 0) {
                    cooldown--;
                } else if (target != null && target.isAlive() && hasLineOfSight(target) && distanceToSqr(target) < 24.0 * 24.0) {
                    entityData.set(DATA_PHASE, CASTING);
                    phaseTicks = WINDUP_TICKS;
                    Telegraph.windUp(this, ParticleTypes.ENCHANT, HumanoidMobs.SCRIBE_CAST.get(), 1.0F);
                }
            }
            case CASTING -> {
                if (tickCount % 3 == 0) {
                    MobCombat.burst(server, ParticleTypes.ENCHANT, this, 4);
                }
                if (target != null) {
                    getLookControl().setLookAt(target, 60.0F, 60.0F);
                }
                if (--phaseTicks <= 0) {
                    if (target != null && target.isAlive()) {
                        fireBolts(target, fanNext);
                        fanNext = !fanNext;
                    }
                    setRecoveryTicks(RECOVERY_TICKS);
                }
            }
            default -> {
                if (--phaseTicks <= 0) {
                    setRecoveryTicks(0);
                    cooldown = COOLDOWN_TICKS;
                }
            }
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("ScribePhase", entityData.get(DATA_PHASE));
        tag.putInt("ScribeTicks", phaseTicks);
        tag.putInt("ScribeCooldown", cooldown);
        tag.putBoolean("FanNext", fanNext);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        // An interrupted cast is simply abandoned; recovery is kept.
        int phase = tag.getInt("ScribePhase");
        entityData.set(DATA_PHASE, phase == RECOVERING ? RECOVERING : IDLE);
        phaseTicks = tag.getInt("ScribeTicks");
        cooldown = tag.getInt("ScribeCooldown");
        fanNext = tag.getBoolean("FanNext");
    }
}
