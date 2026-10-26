package com.relicweave.vestige.entity.humanoid;

import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
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

import java.util.UUID;

/**
 * Heavily armoured undead. Hits from the front 90 degrees deal 40% less; a single hit of 8 or more cracks its armor
 * (-8 armor for 10 seconds). It does not burn in daylight (only zombies and skeletons do).
 */
public class GildedHusk extends Monster implements VisualPose {
    public static final float CRACK_THRESHOLD = 8.0F;
    public static final double CRACK_ARMOR = 8.0;
    public static final int CRACK_TICKS = 200;
    public static final float FRONT_REDUCTION = 0.4F;
    private static final UUID CRACK_ID = UUID.fromString("5f0c3a1e-7d2b-4c6a-9b21-6a1d8e4f0c11");

    private int crackTicks;

    public GildedHusk(EntityType<? extends GildedHusk> type, Level level) {
        super(type, level);
        setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.GOLDEN_AXE));
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            setDropChance(slot, 0.0F);
        }
    }

    public static AttributeSupplier.Builder attributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 36.0)
                .add(Attributes.ARMOR, 14.0)
                .add(Attributes.ATTACK_DAMAGE, 6.0)
                .add(Attributes.MOVEMENT_SPEED, 0.20)
                .add(Attributes.FOLLOW_RANGE, 24.0);
    }

    @Override
    public MobType getMobType() {
        return MobType.UNDEAD;
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

    @Override
    public int visualPose() {
        return isAggressive() ? POSE_REACH : POSE_NONE;
    }

    public int crackTicks() {
        return crackTicks;
    }

    public boolean isCracked() {
        return crackTicks > 0;
    }

    /** Applies (or refreshes) the cracked-armor modifier for {@code ticks} ticks. */
    public void setCrackTicks(int ticks) {
        crackTicks = Math.max(0, ticks);
        AttributeInstance armor = getAttribute(Attributes.ARMOR);
        if (armor == null) {
            return;
        }
        armor.removeModifier(CRACK_ID);
        if (crackTicks > 0) {
            armor.addTransientModifier(new AttributeModifier(CRACK_ID, "Cracked gilding", -CRACK_ARMOR, AttributeModifier.Operation.ADDITION));
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide) {
            return super.hurt(source, amount);
        }
        float raw = amount;
        if (!source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) && MobCombat.isFront(this, source, 45.0)) {
            amount *= 1.0F - FRONT_REDUCTION;
        }
        boolean hurt = super.hurt(source, amount);
        if (hurt && raw >= CRACK_THRESHOLD && !isCracked() && isAlive()) {
            crack();
        }
        return hurt;
    }

    private void crack() {
        setCrackTicks(CRACK_TICKS);
        if (level() instanceof ServerLevel server) {
            server.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.GOLD_NUGGET)),
                    getX(), getY() + getBbHeight() * 0.6, getZ(), 14, 0.35, 0.5, 0.35, 0.1);
            server.playSound(null, getX(), getY(), getZ(), HumanoidMobs.HUSK_CRACK.get(), SoundSource.HOSTILE, 1.0F, 1.0F);
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!level().isClientSide && crackTicks > 0 && --crackTicks == 0) {
            setCrackTicks(0);
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("CrackTicks", crackTicks);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setCrackTicks(tag.getInt("CrackTicks"));
    }
}
