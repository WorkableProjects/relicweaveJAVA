package com.relicweave.infusion.effects.vestige;

import com.relicweave.RelicWeave;
import com.relicweave.infusion.InfusionType;
import com.relicweave.infusion.Infusions;
import com.relicweave.vestige.hazard.HazardProtection;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

/** Riftward (helmet): a {@value #RIFT_PROTECTION} Rift Saturation {@code HazardProtection} contributor while worn. */
@Mod.EventBusSubscriber(modid = RelicWeave.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class RiftwardEffect {
    public static final double RIFT_PROTECTION = 0.30;

    private RiftwardEffect() {}

    public static double protection(Player player) {
        return Infusions.isWearing(player, InfusionType.RIFTWARD) ? RIFT_PROTECTION : 0.0;
    }

    @SubscribeEvent
    public static void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> HazardProtection.register(HazardProtection.Hazard.RIFT_SATURATION, RiftwardEffect::protection));
    }
}
