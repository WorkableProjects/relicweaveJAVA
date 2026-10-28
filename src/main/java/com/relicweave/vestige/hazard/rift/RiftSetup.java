package com.relicweave.vestige.hazard.rift;

import com.relicweave.RelicWeave;
import com.relicweave.vestige.hazard.HazardProtection;
import com.relicweave.vestige.hazard.RiftRegistry;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

/** Common-setup wiring for Rift Saturation: the {@code warded} protection contributor. */
@Mod.EventBusSubscriber(modid = RelicWeave.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class RiftSetup {
    private RiftSetup() {}

    @SubscribeEvent
    public static void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> HazardProtection.register(HazardProtection.Hazard.RIFT_SATURATION,
                player -> player.hasEffect(RiftRegistry.WARDED.get()) ? WardedEffect.RIFT_PROTECTION : 0.0));
    }
}
