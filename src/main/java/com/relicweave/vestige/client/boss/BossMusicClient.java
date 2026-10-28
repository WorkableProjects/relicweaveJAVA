package com.relicweave.vestige.client.boss;

import com.relicweave.RelicWeave;
import com.relicweave.config.RelicWeaveClientConfig;
import com.relicweave.vestige.client.VestigeClientState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.Nullable;

/**
 * Boss fight music. The server tells the client (BossMusicPacket) when it enters or leaves a Vestige boss's arena bar;
 * while set, the boss's track loops on the Music channel (so the Music volume slider applies), other music is held
 * back, and it fades out when the fight ends. Disabled by {@code bossMusic=false} in the client config.
 */
@Mod.EventBusSubscriber(modid = RelicWeave.MOD_ID, value = Dist.CLIENT)
public final class BossMusicClient {
    private static final int FADE_TICKS = 40;
    @Nullable
    private static BossTrack playing;

    private BossMusicClient() {}

    private static boolean enabled() {
        return !RelicWeaveClientConfig.SPEC.isLoaded() || RelicWeaveClientConfig.BOSS_MUSIC.get();
    }

    @Nullable
    private static ResourceLocation wanted() {
        return enabled() ? VestigeClientState.bossMusic : null;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        ResourceLocation wanted = minecraft.level == null ? null : wanted();
        if (playing != null && (playing.isStopped() || !playing.track.equals(wanted))) {
            playing.fadeOut();
            if (playing.isStopped()) {
                playing = null;
            }
        }
        if (wanted != null && playing == null) {
            minecraft.getMusicManager().stopPlaying();
            playing = new BossTrack(wanted);
            minecraft.getSoundManager().play(playing);
        }
    }

    /** While a boss track plays, no other music (biome, Vestige pool, menu) starts on top of it. */
    @SubscribeEvent
    public static void onPlaySound(PlaySoundEvent event) {
        SoundInstance sound = event.getSound();
        if (sound != null && playing != null && !playing.isStopped() && sound != playing
                && sound.getSource() == SoundSource.MUSIC) {
            event.setSound(null);
        }
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        VestigeClientState.bossMusic = null;
        if (playing != null) {
            Minecraft.getInstance().getSoundManager().stop(playing);
            playing = null;
        }
    }

    /** One looping boss track, non-positional, on the Music channel. */
    private static final class BossTrack extends AbstractTickableSoundInstance {
        private final ResourceLocation track;
        private int fade = -1;

        BossTrack(ResourceLocation track) {
            super(SoundEvent.createVariableRangeEvent(track), SoundSource.MUSIC, SoundInstance.createUnseededRandom());
            this.track = track;
            this.looping = true;
            this.delay = 0;
            this.volume = 1.0F;
            this.relative = true;
            this.attenuation = SoundInstance.Attenuation.NONE;
        }

        void fadeOut() {
            if (fade < 0) {
                fade = FADE_TICKS;
            }
        }

        @Override
        public void tick() {
            if (fade >= 0) {
                fade--;
                volume = Math.max(0.0F, fade / (float) FADE_TICKS);
                if (fade <= 0) {
                    stop();
                }
            }
        }
    }
}
