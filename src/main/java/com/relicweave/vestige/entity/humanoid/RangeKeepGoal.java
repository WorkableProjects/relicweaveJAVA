package com.relicweave.vestige.entity.humanoid;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/** Keeps a mob between {@code min} and {@code max} blocks from its target: backs off when close, closes in when far. */
public class RangeKeepGoal extends Goal {
    private final PathfinderMob mob;
    private final double min;
    private final double max;
    private final double speed;

    public RangeKeepGoal(PathfinderMob mob, double min, double max, double speed) {
        this.mob = mob;
        this.min = min;
        this.max = max;
        this.speed = speed;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        LivingEntity target = mob.getTarget();
        return target != null && target.isAlive();
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
        mob.getLookControl().setLookAt(target, 30.0F, 30.0F);
        double dist = Math.sqrt(mob.distanceToSqr(target));
        if (dist < min) {
            if (mob.getNavigation().isDone()) {
                Vec3 away = DefaultRandomPos.getPosAway(mob, 8, 4, target.position());
                if (away != null) {
                    mob.getNavigation().moveTo(away.x, away.y, away.z, speed);
                }
            }
        } else if (dist > max) {
            mob.getNavigation().moveTo(target, speed);
        } else {
            mob.getNavigation().stop();
        }
    }
}
