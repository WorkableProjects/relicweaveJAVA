package com.relicweave.vestige.client.hazard;

import com.relicweave.RelicWeave;
import com.relicweave.vestige.client.VestigeClientState;
import com.relicweave.vestige.client.cycle.CycleHud;
import com.relicweave.vestige.client.dread.DreadOverlay;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Client-only registration of the dread vignette, the cycle and hazard HUD, and cleanup of synced state on disconnect. */
public final class VestigeHudSetup {
    private VestigeHudSetup() {}

    @Mod.EventBusSubscriber(modid = RelicWeave.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class ModBusEvents {
        private ModBusEvents() {}

        @SubscribeEvent
        public static void onRegisterGuiOverlays(RegisterGuiOverlaysEvent event) {
            event.registerBelowAll("vestige_dread", DreadOverlay.OVERLAY);
            event.registerAbove(VanillaGuiOverlay.HOTBAR.id(), "vestige_cycle", CycleHud.OVERLAY);
            event.registerAbove(VanillaGuiOverlay.HOTBAR.id(), "vestige_hazard", HazardHud.OVERLAY);
        }
    }

    @Mod.EventBusSubscriber(modid = RelicWeave.MOD_ID, value = Dist.CLIENT)
    public static final class ForgeBusEvents {
        private ForgeBusEvents() {}

        @SubscribeEvent
        public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
            VestigeClientState.reset();
        }
    }
}
