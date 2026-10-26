package com.relicweave.vestige.entity.humanoid;

import com.relicweave.vestige.VestigeKeys;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;

/** Small shared helpers for the humanoid mobs: facing checks and Vestige-ally filters. */
public final class MobCombat {
    private MobCombat() {}

    /** The position a hit came from, or null if it has none (fall, fire, magic without an entity). */
    public static Vec3 sourcePos(DamageSource source) {
        Vec3 pos = source.getSourcePosition();
        if (pos == null && source.getDirectEntity() != null) {
            pos = source.getDirectEntity().position();
        }
        return pos;
    }

    /** True if {@code source} comes from within {@code halfAngleDeg} of the direction {@code self}'s body faces. */
    public static boolean isFront(LivingEntity self, DamageSource source, double halfAngleDeg) {
        Vec3 from = sourcePos(source);
        if (from == null) {
            return false;
        }
        double dx = from.x - self.getX();
        double dz = from.z - self.getZ();
        double len = Math.sqrt(dx * dx + dz * dz);
        if (len < 1.0E-4) {
            return true;
        }
        double yaw = Math.toRadians(self.yBodyRot);
        double fx = -Math.sin(yaw);
        double fz = Math.cos(yaw);
        return (dx * fx + dz * fz) / len >= Math.cos(Math.toRadians(halfAngleDeg));
    }

    /** True if {@code point} lies in the half-plane in front of {@code self} (a 180 degree arc). */
    public static boolean inFrontArc(LivingEntity self, Vec3 point) {
        double yaw = Math.toRadians(self.yBodyRot);
        return (point.x - self.getX()) * -Math.sin(yaw) + (point.z - self.getZ()) * Math.cos(yaw) >= 0.0;
    }

    public static boolean isVestigeMob(Entity entity) {
        return entity.getType().is(VestigeKeys.VESTIGE_MOBS);
    }

    /** A Vestige mob that is neither an elite nor a boss. */
    public static boolean isPlainVestigeMob(Entity entity) {
        return isVestigeMob(entity) && !entity.getType().is(VestigeKeys.VESTIGE_ELITES)
                && !entity.getType().is(VestigeKeys.VESTIGE_BOSSES);
    }

    /** Up to {@code limit} living non-elite Vestige mobs near {@code self}, nearest first, excluding {@code self}. */
    public static List<Mob> nearbyPlainVestige(Mob self, double radius, int limit) {
        return self.level().getEntitiesOfClass(Mob.class, self.getBoundingBox().inflate(radius),
                        m -> m != self && m.isAlive() && isPlainVestigeMob(m) && m.distanceToSqr(self) <= radius * radius)
                .stream().sorted(Comparator.comparingDouble(m -> m.distanceToSqr(self))).limit(limit).toList();
    }

    /** Sends a burst of particles at {@code entity}'s chest height. */
    public static void burst(ServerLevel level, net.minecraft.core.particles.ParticleOptions particle, Entity entity, int count) {
        level.sendParticles(particle, entity.getX(), entity.getY() + entity.getBbHeight() * 0.6, entity.getZ(), count,
                entity.getBbWidth() * 0.4, entity.getBbHeight() * 0.25, entity.getBbWidth() * 0.4, 0.02);
    }
}
