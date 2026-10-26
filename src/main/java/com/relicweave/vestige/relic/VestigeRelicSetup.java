package com.relicweave.vestige.relic;

import com.relicweave.RelicWeave;
import com.relicweave.vestige.hazard.HazardProtection;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

/** Wiring for the Vestige relics: hazard-protection contributors, particle/expiry ticking and state cleanup. */
public final class VestigeRelicSetup {
    private VestigeRelicSetup() {}

    @Mod.EventBusSubscriber(modid = RelicWeave.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class ModBus {
        private ModBus() {}

        @SubscribeEvent
        public static void onCommonSetup(FMLCommonSetupEvent event) {
            event.enqueueWork(() -> {
                HazardProtection.register(HazardProtection.Hazard.GRAVITY_SHEAR, GravityNail::protection);
                HazardProtection.register(HazardProtection.Hazard.RIFT_SATURATION, RiftglassLantern::protection);
            });
        }
    }

    @Mod.EventBusSubscriber(modid = RelicWeave.MOD_ID)
    public static final class ForgeBus {
        private ForgeBus() {}

        @SubscribeEvent
        public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
            if (event.phase == TickEvent.Phase.END && event.player instanceof ServerPlayer player) {
                GravityNail.tick(player);
            }
        }

        @SubscribeEvent
        public static void onServerTick(TickEvent.ServerTickEvent event) {
            if (event.phase == TickEvent.Phase.END) {
                RiftglassLantern.tick(event.getServer());
            }
        }

        @SubscribeEvent
        public static void onServerStopped(ServerStoppedEvent event) {
            GravityNail.clearAll();
            RiftglassLantern.clearAll();
        }
    }
}
