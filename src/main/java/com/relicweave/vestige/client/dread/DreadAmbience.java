package com.relicweave.vestige.client.dread;

import com.relicweave.RelicWeave;
import com.relicweave.config.RelicWeaveClientConfig;
import com.relicweave.vestige.VestigeKeys;
import com.relicweave.vestige.client.VestigeClientState;
import com.relicweave.vestige.cycle.CyclePhase;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Dread ambience in The Vestige: ash drifting through the air around the player (scaled by {@code dread} and
 * particle density; souls drift up during Convergence), and every few minutes a distant cave sound from somewhere
 * out in the fog (off with {@code ambience = false}). Client-only and purely cosmetic.
 */
@Mod.EventBusSubscriber(modid = RelicWeave.MOD_ID, value = Dist.CLIENT)
public final class DreadAmbience {
    /** Ticks between distant sounds: 1.5 to 4 minutes. */
    public static final int MIN_STINGER = 1800;
    public static final int MAX_STINGER = 4800;

    private static long nextStinger = -1;

    private DreadAmbience() {}

    /** Ash particles per tick for a density setting (FULL 3, REDUCED 1, MINIMAL 0) and dread. */
    public static int ashPerTick(RelicWeaveClientConfig.ParticleDensity density, double dread) {
        int base = switch (density) {
            case FULL -> 3;
            case REDUCED -> 1;
            case MINIMAL -> 0;
        };
        return (int) Math.round(base * dread);
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        LocalPlayer player = mc.player;
        if (level == null || player == null || mc.isPaused() || level.dimension() != VestigeKeys.THE_VESTIGE) {
            nextStinger = -1;
            return;
        }
        double dread = RelicWeaveClientConfig.DREAD.get();
        if (dread <= 0.0) {
            return;
        }
        RandomSource random = level.random;
        boolean convergence = VestigeClientState.cycleEnabled && VestigeClientState.phase == CyclePhase.CONVERGENCE;
        int ash = ashPerTick(RelicWeaveClientConfig.PARTICLE_DENSITY.get(), dread);
        for (int i = 0; i < ash; i++) {
            double x = player.getX() + (random.nextDouble() - 0.5) * 24.0;
            double y = player.getEyeY() + (random.nextDouble() - 0.3) * 10.0;
            double z = player.getZ() + (random.nextDouble() - 0.5) * 24.0;
            level.addParticle(ParticleTypes.ASH, x, y, z, 0.0, 0.0, 0.0);
            if (convergence && random.nextInt(12) == 0) {
                level.addParticle(ParticleTypes.SOUL, x, y - 2.0, z, 0.0, 0.03, 0.0);
            }
        }

        long now = level.getGameTime();
        if (nextStinger < 0) {
            nextStinger = now + MIN_STINGER / 2 + random.nextInt(MAX_STINGER - MIN_STINGER);
        } else if (now >= nextStinger) {
            nextStinger = now + MIN_STINGER + random.nextInt(MAX_STINGER - MIN_STINGER);
            if (RelicWeaveClientConfig.VESTIGE_AMBIENCE.get()) {
                double angle = random.nextDouble() * Math.PI * 2.0;
                double distance = 10.0 + random.nextDouble() * 8.0;
                level.playLocalSound(player.getX() + Math.cos(angle) * distance, player.getY() - 2.0,
                        player.getZ() + Math.sin(angle) * distance, SoundEvents.AMBIENT_CAVE.value(), SoundSource.AMBIENT,
                        0.9F, 0.75F + random.nextFloat() * 0.2F, false);
            }
        }
    }
}
