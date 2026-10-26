package com.relicweave.vestige.boss.c;

import com.relicweave.vestige.boss.VestigeBoss;
import com.relicweave.vestige.entity.Telegraph;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

/** Shared geometry and target filtering for the T13 bosses. Server-side only. */
final class BossCombat {
    private BossCombat() {}

    /** Things the bosses' area attacks may hurt: living, non-hostile (players, pets, villagers), never their own nodes. */
    static boolean isFoe(Entity self, Entity e) {
        return e instanceof LivingEntity living && e != self && living.isAlive() && !living.isSpectator()
                && !(e instanceof Enemy) && !(e instanceof RuneNode) && !(e instanceof ArmorStand) && !(e instanceof VestigeBoss);
    }

    /** Foes whose position is within {@code radius} (horizontally) of {@code center} and within 3 blocks vertically. */
    static List<LivingEntity> inRing(Mob boss, Vec3 center, double radius) {
        AABB box = new AABB(center.x - radius - 1, center.y - 4, center.z - radius - 1, center.x + radius + 1, center.y + 4, center.z + radius + 1);
        List<LivingEntity> out = new ArrayList<>();
        for (LivingEntity e : boss.level().getEntitiesOfClass(LivingEntity.class, box, e -> isFoe(boss, e))) {
            double dx = e.getX() - center.x;
            double dz = e.getZ() - center.z;
            if (dx * dx + dz * dz <= radius * radius && Math.abs(e.getY() - center.y) <= 3.0) {
                out.add(e);
            }
        }
        return out;
    }

    /** Foes within {@code halfWidth} of the horizontal segment from {@code from} along angle {@code angle} (radians). */
    static List<LivingEntity> alongLine(Mob boss, Vec3 from, double angle, double length, double halfWidth) {
        Vec3 dir = dir(angle);
        Vec3 to = from.add(dir.scale(length));
        AABB box = new AABB(Math.min(from.x, to.x) - halfWidth - 1, from.y - 4, Math.min(from.z, to.z) - halfWidth - 1,
                Math.max(from.x, to.x) + halfWidth + 1, from.y + 4, Math.max(from.z, to.z) + halfWidth + 1);
        List<LivingEntity> out = new ArrayList<>();
        for (LivingEntity e : boss.level().getEntitiesOfClass(LivingEntity.class, box, e -> isFoe(boss, e))) {
            double rx = e.getX() - from.x;
            double rz = e.getZ() - from.z;
            double along = rx * dir.x + rz * dir.z;
            double across = Math.abs(rx * dir.z - rz * dir.x);
            if (along >= 0.0 && along <= length && across <= halfWidth + e.getBbWidth() / 2.0 && Math.abs(e.getY() - from.y) <= 3.0) {
                out.add(e);
            }
        }
        return out;
    }

    static Vec3 dir(double angle) {
        return new Vec3(Math.cos(angle), 0.0, Math.sin(angle));
    }

    /** Horizontal angle (radians) from {@code from} to {@code to}. */
    static double angleTo(Vec3 from, Vec3 to) {
        return Math.atan2(to.z - from.z, to.x - from.x);
    }

    static void drawLine(ServerLevel level, ParticleOptions particle, Vec3 from, double angle, double length) {
        Telegraph.line(level, particle, from, from.add(dir(angle).scale(length)), (int) (length * 2));
    }

    static void clink(Entity entity) {
        if (entity.level() instanceof ServerLevel level) {
            level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.CHAIN_HIT, SoundSource.HOSTILE, 1.0F, 1.6F);
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.CRIT, entity.getX(), entity.getY() + entity.getBbHeight() * 0.6,
                    entity.getZ(), 4, 0.3, 0.3, 0.3, 0.1);
        }
    }

    /** Melee goal that stands down while the boss is busy with a telegraphed move. */
    static final class BossMelee extends MeleeAttackGoal {
        private final BooleanSupplier busy;

        BossMelee(PathfinderMob mob, BooleanSupplier busy) {
            super(mob, 1.0, false);
            this.busy = busy;
        }

        @Override
        public boolean canUse() {
            return !busy.getAsBoolean() && super.canUse();
        }

        @Override
        public boolean canContinueToUse() {
            return !busy.getAsBoolean() && super.canContinueToUse();
        }
    }
}
