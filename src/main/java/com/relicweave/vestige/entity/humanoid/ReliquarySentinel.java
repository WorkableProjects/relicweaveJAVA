package com.relicweave.vestige.entity.humanoid;

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
import com.relicweave.vestige.entity.Telegraph;

/**
 * Common melee guard. Every few seconds, while it has a target, it winds up, raises its shield (80% less damage from
 * the front 120 degrees) and afterwards is left Exposed: it cannot attack and takes 30% more damage.
 */
public class ReliquarySentinel extends Monster implements VisualPose {
    public static final int IDLE = 0;
    public static final int WINDUP = 1;
    public static final int GUARDING = 2;
    public static final int EXPOSED = 3;
    public static final int WINDUP_TICKS = 10;
    public static final int GUARD_TICKS = 40;
    public static final int EXPOSED_TICKS = 30;
    public static final int COOLDOWN_TICKS = 120;
    public static final float FRONT_REDUCTION = 0.8F;
    public static final float EXPOSED_BONUS = 0.3F;

    private static final EntityDataAccessor<Integer> DATA_STATE = SynchedEntityData.defineId(ReliquarySentinel.class, EntityDataSerializers.INT);

    private int stateTicks;
    private int cooldown = COOLDOWN_TICKS;

    public ReliquarySentinel(EntityType<? extends ReliquarySentinel> type, Level level) {
        super(type, level);
        setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
        setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            setDropChance(slot, 0.0F);
        }
    }

    public static AttributeSupplier.Builder attributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 40.0)
                .add(Attributes.ARMOR, 8.0)
                .add(Attributes.ATTACK_DAMAGE, 7.0)
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.FOLLOW_RANGE, 24.0);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(DATA_STATE, IDLE);
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

    public int state() {
        return entityData.get(DATA_STATE);
    }

    /** Test/command hook: jump straight to a state with a full timer for it. */
    public void setState(int state, int ticks) {
        entityData.set(DATA_STATE, state);
        stateTicks = ticks;
    }

    public int stateTicks() {
        return stateTicks;
    }

    public int cooldown() {
        return cooldown;
    }

    public void setCooldown(int ticks) {
        cooldown = ticks;
    }

    @Override
    public int visualPose() {
        return switch (state()) {
            case GUARDING -> POSE_GUARD;
            case WINDUP -> POSE_RAISED;
            default -> POSE_NONE;
        };
    }

    /** Damage after the guard / exposed rules. Pure apart from reading state and facing. */
    public float adjustIncoming(DamageSource source, float amount) {
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return amount;
        }
        int state = state();
        if (state == GUARDING && MobCombat.isFront(this, source, 60.0)) {
            return amount * (1.0F - FRONT_REDUCTION);
        }
        if (state == EXPOSED) {
            return amount * (1.0F + EXPOSED_BONUS);
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
        return state() != EXPOSED && super.doHurtTarget(target);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!(level() instanceof ServerLevel server) || !isAlive()) {
            return;
        }
        switch (state()) {
            case IDLE -> {
                if (cooldown > 0) {
                    cooldown--;
                } else if (getTarget() != null && distanceToSqr(getTarget()) < 100.0) {
                    entityData.set(DATA_STATE, WINDUP);
                    stateTicks = WINDUP_TICKS;
                    Telegraph.windUp(this, ParticleTypes.ENCHANTED_HIT, HumanoidMobs.SENTINEL_GUARD.get(), 1.0F);
                }
            }
            case WINDUP -> {
                if (--stateTicks <= 0) {
                    setState(GUARDING, GUARD_TICKS);
                }
            }
            case GUARDING -> {
                if (--stateTicks <= 0) {
                    setState(EXPOSED, EXPOSED_TICKS);
                }
            }
            default -> {
                if (tickCount % 4 == 0) {
                    MobCombat.burst(server, ParticleTypes.SMOKE, this, 3);
                }
                if (--stateTicks <= 0) {
                    setState(IDLE, 0);
                    cooldown = COOLDOWN_TICKS;
                }
            }
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("GuardState", state());
        tag.putInt("GuardTicks", stateTicks);
        tag.putInt("GuardCooldown", cooldown);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(DATA_STATE, tag.getInt("GuardState"));
        stateTicks = tag.getInt("GuardTicks");
        cooldown = tag.getInt("GuardCooldown");
    }
}
