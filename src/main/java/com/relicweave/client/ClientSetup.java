package com.relicweave.client;

import com.relicweave.RelicWeave;
import com.relicweave.network.PurlingNetwork;
import com.relicweave.registry.ModEnchantments;
import com.relicweave.registry.ModMenus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/** Client-only registration. These classes are never loaded on a dedicated server. */
public final class ClientSetup {
    private ClientSetup() {}

    @Mod.EventBusSubscriber(modid = RelicWeave.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class ModBusEvents {
        private ModBusEvents() {}

        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            event.enqueueWork(() -> {
                MenuScreens.register(ModMenus.TRANSMUTATION_TABLE.get(), TransmutationTableScreen::new);
                MenuScreens.register(ModMenus.CELESTIAL_INFUSER.get(), CelestialInfuserScreen::new);
            });
        }

        @SubscribeEvent
        public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
            event.register(ModKeyMappings.PURLING);
            event.register(ModKeyMappings.ACTIVATE_RELIC);
            event.register(ModKeyMappings.CYCLE_RELIC);
        }
    }

    @Mod.EventBusSubscriber(modid = RelicWeave.MOD_ID, value = Dist.CLIENT)
    public static final class ForgeBusEvents {
        private ForgeBusEvents() {}

        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) {
                return;
            }
            Minecraft minecraft = Minecraft.getInstance();
            UpboundClient.onClientTick(minecraft);
            while (ModKeyMappings.PURLING.consumeClick()) {
                // Skip obviously useless requests; the server re-validates everything.
                if (minecraft.player != null && EnchantmentHelper.getItemEnchantmentLevel(
                        ModEnchantments.PURLING.get(), minecraft.player.getItemBySlot(EquipmentSlot.FEET)) > 0) {
                    PurlingNetwork.CHANNEL.sendToServer(new PurlingNetwork.ActivatePurlingPacket());
                }
            }
        }
    }
}
