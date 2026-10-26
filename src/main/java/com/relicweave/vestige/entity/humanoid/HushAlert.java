package com.relicweave.vestige.entity.humanoid;

import com.relicweave.vestige.hazard.field.HushNoise;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Mob;

/** Hushfield hearing shared by the Mirebound Cantor and the Echo Weaver. Server side only. */
public final class HushAlert {
    public static final double RANGE = 24.0;
    public static final int COOLDOWN_TICKS = 400;
    public static final int MAX_ALLIES = 4;
    private static final double ALLY_RADIUS = 16.0;

    private HushAlert() {}

    /**
     * If a loud player is within {@link #RANGE}, makes {@code self} (and, if asked, up to {@link #MAX_ALLIES} nearby
     * Vestige mobs) target them. Returns true when an alarm was raised.
     */
    public static boolean raise(Mob self, boolean alertAllies) {
        if (!(self.level() instanceof ServerLevel level)) {
            return false;
        }
        ServerPlayer player = HushNoise.loudestPlayerNear(level, self.position(), RANGE);
        if (player == null || !HushNoise.isLoud(player) || player.isCreative() || player.isSpectator()) {
            return false;
        }
        self.setTarget(player);
        level.sendParticles(ParticleTypes.ANGRY_VILLAGER, self.getX(), self.getEyeY() + 0.4, self.getZ(), 3, 0.2, 0.1, 0.2, 0.0);
        if (alertAllies) {
            for (Mob ally : MobCombat.nearbyPlainVestige(self, ALLY_RADIUS, MAX_ALLIES)) {
                ally.setTarget(player);
            }
        }
        level.playSound(null, self.getX(), self.getY(), self.getZ(), SoundEvents.BELL_BLOCK, SoundSource.HOSTILE, 0.6F, 1.5F);
        return true;
    }
}
