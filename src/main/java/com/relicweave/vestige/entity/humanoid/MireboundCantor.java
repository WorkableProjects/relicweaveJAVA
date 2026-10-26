package com.relicweave.vestige.entity.humanoid;

import com.relicweave.vestige.entity.Telegraph;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Support mob. Avoids players within 6 blocks, never attacks and never summons. While other Vestige mobs are near it
 * chants for 40 ticks (taking 4+ damage interrupts), then grants up to 4 of them Resistance I and Strength I for 8
 * seconds (never elites or bosses). In a Hushfield it raises an alarm against loud players.
 */
public class MireboundCantor extends Monster implements VisualPose {
    public static final int CHANT_TICKS = 40;
    public static final int CHANT_COOLDOWN_TICKS = 160;
    public static final int INTERRUPT_COOLDOWN_TICKS = 100;
    public static final float INTERRUPT_DAMAGE = 4.0F;
    public static final int BUFF_TICKS = 160;
    public static final int MAX_BUFFED = 4;
    public static final double BUFF_RADIUS = 10.0;

    private static final EntityDataAccessor<Boolean> DATA_CHANTING = SynchedEntityData.defineId(MireboundCantor.class, EntityDataSerializers.BOOLEAN);

    private int chantTicks;
    private int chantCooldown = CHANT_COOLDOWN_TICKS;
    private int hushCooldown;

    public MireboundCantor(EntityType<? extends MireboundCantor> type, Level level) {
        super(type, level);
        setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BELL));
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            setDropChance(slot, 0.0F);
        }
    }

    public static AttributeSupplier.Builder attributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 24.0)
                .add(Attributes.ARMOR, 2.0)
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.FOLLOW_RANGE, 24.0);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(DATA_CHANTING, false);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new AvoidEntityGoal<>(this, Player.class, 6.0F, 1.0, 1.2));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.7));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(6, new RandomLookAroundGoal(this));
    }

    public boolean isChanting() {
        return entityData.get(DATA_CHANTING);
    }

    public int chantCooldown() {
        return chantCooldown;
    }

    public void setChantCooldown(int ticks) {
        chantCooldown = ticks;
    }

    public int hushCooldown() {
        return hushCooldown;
    }

    public void setHushCooldown(int ticks) {
        hushCooldown = ticks;
    }

    @Override
    public int visualPose() {
        return isChanting() ? POSE_RAISED : POSE_NONE;
    }

    /** Test/command hook: start chanting with {@code ticks} left. */
    public void startChant(int ticks) {
        chantTicks = ticks;
        entityData.set(DATA_CHANTING, true);
        Telegraph.windUp(this, ParticleTypes.NOTE, HumanoidMobs.CANTOR_CHANT.get(), 1.0F);
    }

    /** Finishes the chant now: buffs up to 4 nearby non-elite Vestige mobs. Returns how many were buffed. */
    public int completeChant() {
        chantTicks = 0;
        entityData.set(DATA_CHANTING, false);
        chantCooldown = CHANT_COOLDOWN_TICKS;
        List<Mob> allies = MobCombat.nearbyPlainVestige(this, BUFF_RADIUS, MAX_BUFFED);
        for (Mob ally : allies) {
            ally.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, BUFF_TICKS, 0));
            ally.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, BUFF_TICKS, 0));
            if (level() instanceof ServerLevel server) {
                MobCombat.burst(server, ParticleTypes.NOTE, ally, 4);
            }
        }
        return allies.size();
    }

    private void interrupt() {
        chantTicks = 0;
        entityData.set(DATA_CHANTING, false);
        chantCooldown = INTERRUPT_COOLDOWN_TICKS;
        if (level() instanceof ServerLevel server) {
            MobCombat.burst(server, ParticleTypes.SMOKE, this, 8);
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (hurt && !level().isClientSide && isChanting() && amount >= INTERRUPT_DAMAGE) {
            interrupt();
        }
        return hurt;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!(level() instanceof ServerLevel server) || !isAlive()) {
            return;
        }
        if (hushCooldown > 0) {
            hushCooldown--;
        } else if (tickCount % 20 == 0 && HushAlert.raise(this, true)) {
            hushCooldown = HushAlert.COOLDOWN_TICKS;
        }
        if (isChanting()) {
            if (tickCount % 4 == 0) {
                MobCombat.burst(server, ParticleTypes.NOTE, this, 2);
            }
            if (--chantTicks <= 0) {
                completeChant();
            }
        } else if (chantCooldown > 0) {
            chantCooldown--;
        } else if (!MobCombat.nearbyPlainVestige(this, BUFF_RADIUS, 1).isEmpty()) {
            startChant(CHANT_TICKS);
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("ChantCooldown", chantCooldown);
        tag.putInt("HushCooldown", hushCooldown);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        chantCooldown = tag.getInt("ChantCooldown");
        hushCooldown = tag.getInt("HushCooldown");
    }
}
