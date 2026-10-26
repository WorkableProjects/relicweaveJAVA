package com.relicweave.vestige.entity.ethereal;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.function.BooleanSupplier;

/**
 * Hover-style chase for flying mobs: close to {@code maxDist} of the target, back off inside {@code minDist}, hold
 * position (hover) in between, and melee when within {@code reach} (0 = never melee).
 */
final class FlyerApproachGoal extends Goal {
    private final Mob mob;
    private final double speed;
    private final double minDist;
    private final double maxDist;
    private final double reach;
    private final double hoverHeight;
    private final BooleanSupplier paused;
    private int cooldown;

    FlyerApproachGoal(Mob mob, double speed, double minDist, double maxDist, double reach, double hoverHeight, BooleanSupplier paused) {
        this.mob = mob;
        this.speed = speed;
        this.minDist = minDist;
        this.maxDist = maxDist;
        this.reach = reach;
        this.hoverHeight = hoverHeight;
        this.paused = paused;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        LivingEntity target = mob.getTarget();
        return target != null && target.isAlive() && !paused.getAsBoolean();
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public void tick() {
        LivingEntity target = mob.getTarget();
        if (target == null) {
            return;
        }
        if (cooldown > 0) {
            cooldown--;
        }
        mob.getLookControl().setLookAt(target, 30.0F, 30.0F);
        double dist = mob.distanceTo(target);
        if (dist > maxDist) {
            mob.getMoveControl().setWantedPosition(target.getX(), target.getY(0.5) + hoverHeight, target.getZ(), speed);
        } else if (dist < minDist) {
            Vec3 away = mob.position().subtract(target.position());
            away = away.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : away.normalize();
            mob.getMoveControl().setWantedPosition(mob.getX() + away.x * 2.0, mob.getY() + 0.5, mob.getZ() + away.z * 2.0, speed);
        } else {
            mob.getMoveControl().setWantedPosition(mob.getX(), mob.getY(), mob.getZ(), speed);
        }
        if (reach > 0.0 && cooldown <= 0 && mob.distanceToSqr(target) <= reach * reach) {
            cooldown = 20;
            mob.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
            mob.doHurtTarget(target);
        }
    }
}
