package com.relicweave.vestige.client.world;

import com.relicweave.RelicWeave;
import com.relicweave.config.RelicWeaveClientConfig;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterDimensionSpecialEffectsEvent;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Client registration of The Vestige's special effects and the music / ambience config toggles. */
public final class VestigeWorldClientEvents {
    private static final ResourceLocation MUSIC = RelicWeave.id("music.vestige");
    private static final String AMBIENT_PREFIX = "ambient.vestige.";

    private VestigeWorldClientEvents() {}

    @Mod.EventBusSubscriber(modid = RelicWeave.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class ModBus {
        private ModBus() {}

        @SubscribeEvent
        public static void onRegisterEffects(RegisterDimensionSpecialEffectsEvent event) {
            event.register(RelicWeave.id("the_vestige"), new VestigeDimensionEffects());
        }
    }

    @Mod.EventBusSubscriber(modid = RelicWeave.MOD_ID, value = Dist.CLIENT)
    public static final class ForgeBus {
        private ForgeBus() {}

        @SubscribeEvent
        public static void onPlaySound(PlaySoundEvent event) {
            SoundInstance sound = event.getSound();
            if (sound == null) {
                return;
            }
            ResourceLocation id = sound.getLocation();
            if (!RelicWeave.MOD_ID.equals(id.getNamespace())) {
                return;
            }
            if (id.equals(MUSIC) && !RelicWeaveClientConfig.VESTIGE_MUSIC.get()) {
                event.setSound(null);
            } else if (id.getPath().startsWith(AMBIENT_PREFIX) && !RelicWeaveClientConfig.VESTIGE_AMBIENCE.get()) {
                event.setSound(null);
            }
        }
    }
}
