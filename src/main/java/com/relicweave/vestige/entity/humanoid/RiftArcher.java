package com.relicweave.vestige.entity.humanoid;

import com.relicweave.vestige.entity.Telegraph;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Ranged elite on the skeleton model. It only shoots with a clear line of sight: it aims for 25 ticks while drawing an
 * electric aim line to the target, then fires an arrow led by the target's velocity (lead capped at 4 blocks) for
 * about 8 damage, and strafes during the 40 tick cooldown.
 */
public class RiftArcher extends Monster implements RangedAttackMob {
    public static final int AIM_TICKS = 25;
    public static final int COOLDOWN_TICKS = 40;
    public static final double ARROW_SPEED = 2.0;
    public static final double MAX_LEAD = 4.0;
    public static final double TARGET_DAMAGE = 8.0;
    public static final double SHOOT_RANGE = 24.0;

    private int cooldown = COOLDOWN_TICKS;
    private int aimTicks;

    public RiftArcher(EntityType<? extends RiftArcher> type, Level level) {
        super(type, level);
        setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            setDropChance(slot, 0.0F);
        }
    }

    public static AttributeSupplier.Builder attributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 40.0)
                .add(Attributes.ARMOR, 6.0)
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.FOLLOW_RANGE, 32.0);
    }

    /** How far ahead of a moving target to aim: velocity times flight time, capped at {@link #MAX_LEAD} blocks. */
    public static Vec3 lead(Vec3 targetVelocity, double distance) {
        Vec3 lead = targetVelocity.scale(distance / ARROW_SPEED);
        double length = lead.length();
        return length > MAX_LEAD ? lead.scale(MAX_LEAD / length) : lead;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new AimGoal(this));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(6, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    public int cooldown() {
        return cooldown;
    }

    public void setCooldown(int ticks) {
        cooldown = ticks;
    }

    public boolean isAiming() {
        return aimTicks > 0;
    }

    /** Fires one arrow at {@code target} with predictive lead, using {@code targetVelocity} (blocks per tick). */
    public void shoot(LivingEntity target, Vec3 targetVelocity) {
        AbstractArrow arrow = ProjectileUtil.getMobArrow(this, new ItemStack(Items.ARROW), 1.0F);
        arrow.pickup = AbstractArrow.Pickup.DISALLOWED;
        arrow.setBaseDamage(TARGET_DAMAGE / ARROW_SPEED);
        Vec3 origin = arrow.position();
        Vec3 center = target.getBoundingBox().getCenter();
        Vec3 aim = center.add(lead(targetVelocity, origin.distanceTo(center))).subtract(origin);
        double horizontal = Math.sqrt(aim.x * aim.x + aim.z * aim.z);
        arrow.shoot(aim.x, aim.y + horizontal * 0.2, aim.z, (float) ARROW_SPEED, 1.0F);
        level().addFreshEntity(arrow);
        level().playSound(null, getX(), getY(), getZ(), net.minecraft.sounds.SoundEvents.SKELETON_SHOOT, SoundSource.HOSTILE, 1.0F, 1.2F);
    }

    @Override
    public void performRangedAttack(LivingEntity target, float power) {
        shoot(target, target.getDeltaMovement());
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("ShotCooldown", cooldown);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        cooldown = tag.getInt("ShotCooldown");
    }

    /** Close in until there is line of sight, then aim, fire and strafe between shots. */
    private static final class AimGoal extends Goal {
        private final RiftArcher archer;
        private Vec3 lastTargetPos;
        private Vec3 targetVelocity = Vec3.ZERO;
        private int strafeTime;
        private boolean strafeClockwise;
        private boolean strafeBack;

        AimGoal(RiftArcher archer) {
            this.archer = archer;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = archer.getTarget();
            return target != null && target.isAlive();
        }

        @Override
        public void start() {
            archer.setAggressive(true);
            lastTargetPos = null;
        }

        @Override
        public void stop() {
            archer.setAggressive(false);
            archer.aimTicks = 0;
            archer.getMoveControl().strafe(0.0F, 0.0F);
            archer.getNavigation().stop();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            LivingEntity target = archer.getTarget();
            if (target == null || !(archer.level() instanceof ServerLevel server)) {
                return;
            }
            Vec3 pos = target.position();
            if (lastTargetPos != null) {
                targetVelocity = pos.subtract(lastTargetPos);
            }
            lastTargetPos = pos;

            double dist = Math.sqrt(archer.distanceToSqr(target));
            boolean sight = archer.hasLineOfSight(target);
            archer.getLookControl().setLookAt(target, 30.0F, 30.0F);
            if (!sight || dist > SHOOT_RANGE) {
                archer.aimTicks = 0;
                archer.getMoveControl().strafe(0.0F, 0.0F);
                archer.getNavigation().moveTo(target, 1.0);
                return;
            }
            archer.getNavigation().stop();
            if (archer.aimTicks > 0) {
                archer.getMoveControl().strafe(0.0F, 0.0F);
                if (archer.aimTicks % 5 == 0) {
                    Vec3 from = archer.getEyePosition().add(0, -0.3, 0);
                    Telegraph.line(server, ParticleTypes.ELECTRIC_SPARK, from, target.getBoundingBox().getCenter(),
                            Math.max(4, Math.min(24, (int) (dist * 1.5))));
                }
                if (--archer.aimTicks == 0) {
                    archer.shoot(target, targetVelocity);
                    archer.cooldown = COOLDOWN_TICKS;
                }
                return;
            }
            // Cooldown: strafe like a skeleton.
            if (archer.cooldown > 0) {
                archer.cooldown--;
                if (--strafeTime <= 0) {
                    strafeTime = 20 + archer.random.nextInt(20);
                    strafeClockwise = archer.random.nextBoolean();
                    strafeBack = dist < 9.0 || (dist < 14.0 && archer.random.nextFloat() < 0.3F);
                }
                archer.getMoveControl().strafe(strafeBack ? -0.5F : 0.0F, strafeClockwise ? 0.5F : -0.5F);
                return;
            }
            archer.getMoveControl().strafe(0.0F, 0.0F);
            archer.aimTicks = AIM_TICKS;
            archer.level().playSound(null, archer.getX(), archer.getY(), archer.getZ(), HumanoidMobs.ARCHER_AIM.get(),
                    SoundSource.HOSTILE, 1.0F, 1.0F);
        }
    }
}
