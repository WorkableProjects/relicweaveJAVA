package com.relicweave.vestige.entity.humanoid;

import com.relicweave.vestige.entity.Telegraph;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
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
 * Disruptor. Every 10 seconds it flings two harmless decoy glyph bolts past its target and draws a false particle
 * trail; its real attack is a sonic pulse (15 tick wind-up) hurting everything within 5 blocks. It hears loud players
 * in a Hushfield. Decoys never imitate Starfall markers or HUD warnings.
 */
public class EchoWeaver extends Monster implements VisualPose {
    public static final int DECOY_COOLDOWN_TICKS = 200;
    public static final int PULSE_WINDUP_TICKS = 15;
    public static final int PULSE_COOLDOWN_TICKS = 60;
    public static final double PULSE_RADIUS = 5.0;
    public static final int DECOYS = 2;

    private static final EntityDataAccessor<Boolean> DATA_WINDUP = SynchedEntityData.defineId(EchoWeaver.class, EntityDataSerializers.BOOLEAN);

    private int decoyCooldown = DECOY_COOLDOWN_TICKS;
    private int pulseCooldown = 20;
    private int pulseTicks;
    private int hushCooldown;

    public EchoWeaver(EntityType<? extends EchoWeaver> type, Level level) {
        super(type, level);
        setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.AMETHYST_SHARD));
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            setDropChance(slot, 0.0F);
        }
    }

    public static AttributeSupplier.Builder attributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 26.0)
                .add(Attributes.ATTACK_DAMAGE, 5.0)
                .add(Attributes.MOVEMENT_SPEED, 0.27)
                .add(Attributes.FOLLOW_RANGE, 24.0);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(DATA_WINDUP, false);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new RangeKeepGoal(this, 0.0, 3.5, 1.0));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(6, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    public int decoyCooldown() {
        return decoyCooldown;
    }

    public void setDecoyCooldown(int ticks) {
        decoyCooldown = ticks;
    }

    public boolean isWindingUp() {
        return entityData.get(DATA_WINDUP);
    }

    @Override
    public int visualPose() {
        return isWindingUp() ? POSE_RAISED : POSE_NONE;
    }

    /** Spawns the decoy bolts around {@code target} plus a false particle trail. Returns the decoys. */
    public GlyphBolt[] castDecoys(LivingEntity target) {
        GlyphBolt[] decoys = new GlyphBolt[DECOYS];
        if (!(level() instanceof ServerLevel server)) {
            return decoys;
        }
        for (int i = 0; i < DECOYS; i++) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            Vec3 start = target.position().add(Math.cos(angle) * 3.5, 1.0 + random.nextDouble(), Math.sin(angle) * 3.5);
            Vec3 aimAt = target.position().add((random.nextDouble() - 0.5) * 3.0, 1.2, (random.nextDouble() - 0.5) * 3.0);
            Vec3 velocity = aimAt.subtract(start).normalize().scale(0.45);
            GlyphBolt decoy = GlyphBolt.create(level(), this, start, velocity, true);
            level().addFreshEntity(decoy);
            decoys[i] = decoy;
        }
        Vec3 falseEnd = target.position().add((random.nextDouble() - 0.5) * 8.0, 1.0, (random.nextDouble() - 0.5) * 8.0);
        Telegraph.line(server, ParticleTypes.SCULK_SOUL, getEyePosition(), falseEnd, 14);
        return decoys;
    }

    /** Starts the pulse wind-up now (test/command hook). */
    public void startPulse() {
        pulseTicks = PULSE_WINDUP_TICKS;
        entityData.set(DATA_WINDUP, true);
        Telegraph.windUp(this, ParticleTypes.NOTE, HumanoidMobs.WEAVER_PULSE.get(), 1.0F);
    }

    /** The sonic pulse itself: 5 damage to every non-Vestige living entity within 5 blocks. Returns how many were hit. */
    public int pulse() {
        int hit = 0;
        for (Entity e : level().getEntities(this, getBoundingBox().inflate(PULSE_RADIUS),
                e -> e instanceof LivingEntity && e.isAlive() && !MobCombat.isVestigeMob(e))) {
            if (distanceToSqr(e) <= PULSE_RADIUS * PULSE_RADIUS) {
                e.hurt(damageSources().sonicBoom(this), (float) getAttributeValue(Attributes.ATTACK_DAMAGE));
                hit++;
            }
        }
        if (level() instanceof ServerLevel server) {
            Telegraph.ring(server, ParticleTypes.SCULK_SOUL, position(), PULSE_RADIUS, 24);
            Telegraph.ring(server, ParticleTypes.NOTE, position(), PULSE_RADIUS * 0.6, 12);
        }
        return hit;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!(level() instanceof ServerLevel server) || !isAlive()) {
            return;
        }
        if (hushCooldown > 0) {
            hushCooldown--;
        } else if (tickCount % 20 == 0 && HushAlert.raise(this, false)) {
            hushCooldown = HushAlert.COOLDOWN_TICKS;
        }
        LivingEntity target = getTarget();
        if (isWindingUp()) {
            if (tickCount % 3 == 0) {
                MobCombat.burst(server, ParticleTypes.SCULK_SOUL, this, 3);
                MobCombat.burst(server, ParticleTypes.NOTE, this, 1);
            }
            if (--pulseTicks <= 0) {
                entityData.set(DATA_WINDUP, false);
                pulse();
                pulseCooldown = PULSE_COOLDOWN_TICKS;
            }
            return;
        }
        if (pulseCooldown > 0) {
            pulseCooldown--;
        } else if (target != null && target.isAlive() && distanceToSqr(target) <= 4.5 * 4.5) {
            startPulse();
        }
        if (decoyCooldown > 0) {
            decoyCooldown--;
        } else if (target != null && target.isAlive() && distanceToSqr(target) <= 24.0 * 24.0) {
            castDecoys(target);
            decoyCooldown = DECOY_COOLDOWN_TICKS;
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("DecoyCooldown", decoyCooldown);
        tag.putInt("HushCooldown", hushCooldown);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        decoyCooldown = tag.getInt("DecoyCooldown");
        hushCooldown = tag.getInt("HushCooldown");
    }
}
