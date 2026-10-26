package com.relicweave.vestige.client.gear;

import com.relicweave.RelicWeave;
import com.relicweave.vestige.gear.GearRegistry;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/** Client-only registration of the Reliquary Loom screen. Never loaded on a dedicated server. */
@Mod.EventBusSubscriber(modid = RelicWeave.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class GearClientSetup {
    private GearClientSetup() {}

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> MenuScreens.register(GearRegistry.RELIQUARY_LOOM_MENU.get(), ReliquaryLoomScreen::new));
    }
}
