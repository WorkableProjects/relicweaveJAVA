package com.relicweave.vestige.entity;

import com.relicweave.config.RelicWeaveClientConfig;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Shared server-side telegraph cues for Vestige mobs and bosses: every wind-up gets a visible particle cue and a sound
 * (whose sound event has a subtitle), so no attack relies on audio alone. Call from server code only.
 */
public final class Telegraph {
    private Telegraph() {}

    /** Particles in a ring around {@code entity}'s feet plus a sound: "something is coming from this mob". */
    public static void windUp(LivingEntity entity, ParticleOptions particle, SoundEvent sound, float pitch) {
        if (!(entity.level() instanceof ServerLevel level)) {
            return;
        }
        double r = Math.max(0.6, entity.getBbWidth());
        for (int i = 0; i < 12; i++) {
            double a = i * Math.PI / 6.0;
            level.sendParticles(particle, entity.getX() + Math.cos(a) * r, entity.getY() + 0.1,
                    entity.getZ() + Math.sin(a) * r, 1, 0, 0.05, 0, 0.0);
        }
        level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), sound, SoundSource.HOSTILE, 1.0F, pitch);
    }

    /** A line of particles from {@code from} to {@code to} (aim lines, charge paths, pull lines). */
    public static void line(ServerLevel level, ParticleOptions particle, Vec3 from, Vec3 to, int points) {
        for (int i = 0; i <= points; i++) {
            Vec3 p = from.lerp(to, i / (double) points);
            level.sendParticles(particle, p.x, p.y, p.z, 1, 0, 0, 0, 0.0);
        }
    }

    /** A flat ring of particles at {@code center} (area-of-effect markers). */
    public static void ring(ServerLevel level, ParticleOptions particle, Vec3 center, double radius, int points) {
        for (int i = 0; i < points; i++) {
            double a = i * 2 * Math.PI / points;
            level.sendParticles(particle, center.x + Math.cos(a) * radius, center.y + 0.1, center.z + Math.sin(a) * radius,
                    1, 0, 0, 0, 0.0);
        }
    }

    /** Client helper: how many ambient particles to draw out of {@code full}, per the client particle setting. */
    public static int clientParticleCount(int full) {
        if (!RelicWeaveClientConfig.SPEC.isLoaded()) {
            return full;
        }
        return switch (RelicWeaveClientConfig.PARTICLE_DENSITY.get()) {
            case FULL -> full;
            case REDUCED -> Math.max(1, full / 2);
            case MINIMAL -> 1;
        };
    }
}
