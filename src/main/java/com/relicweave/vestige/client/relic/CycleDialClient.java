package com.relicweave.vestige.client.relic;

import com.relicweave.RelicWeave;
import com.relicweave.relic.RelicAugment;
import com.relicweave.relic.RelicLocator;
import com.relicweave.relic.RelicType;
import com.relicweave.vestige.VestigeKeys;
import com.relicweave.vestige.client.VestigeClientState;
import com.relicweave.vestige.client.cycle.CycleHud;
import com.relicweave.vestige.cycle.CyclePhase;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * Cycle Dial (passive): while carried it enables the HUD countdown, warns once 30 s (Foresight: 60 s) before each phase
 * change and, with the Chime augment, chimes and names the next phase at each change. Several dials behave as one.
 * Everything is computed from {@link VestigeClientState}; nothing here is authoritative.
 */
public final class CycleDialClient {
    public static final int BASE_WARNING_SECONDS = 30;
    public static final int FORESIGHT_WARNING_SECONDS = 60;

    private static CyclePhase warnedPhase;
    private static CyclePhase lastPhase;

    private CycleDialClient() {}

    /** Warning lead time in seconds for a dial carrying {@code dial}. */
    public static int warningSeconds(ItemStack dial) {
        return RelicAugment.CYCLE_DIAL_FORESIGHT.isOn(dial) ? FORESIGHT_WARNING_SECONDS : BASE_WARNING_SECONDS;
    }

    private static boolean carries(Player player) {
        return RelicLocator.has(player, RelicType.CYCLE_DIAL);
    }

    private static void reset() {
        warnedPhase = null;
        lastPhase = null;
    }

    @Mod.EventBusSubscriber(modid = RelicWeave.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class ModBus {
        private ModBus() {}

        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            event.enqueueWork(() -> CycleHud.countdownSource = () -> {
                Player player = Minecraft.getInstance().player;
                return player != null && carries(player);
            });
        }
    }

    @Mod.EventBusSubscriber(modid = RelicWeave.MOD_ID, value = Dist.CLIENT)
    public static final class ForgeBus {
        private ForgeBus() {}

        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) {
                return;
            }
            Minecraft minecraft = Minecraft.getInstance();
            Player player = minecraft.player;
            if (player == null || minecraft.level == null || minecraft.level.dimension() != VestigeKeys.THE_VESTIGE
                    || !VestigeClientState.cycleEnabled || !carries(player)) {
                reset();
                return;
            }
            ItemStack dial = RelicLocator.first(player, RelicType.CYCLE_DIAL);
            long now = minecraft.level.getGameTime();
            long remaining = VestigeClientState.ticksRemaining - (now - VestigeClientState.syncedAtGameTime);
            CyclePhase phase = VestigeClientState.phase;
            CyclePhase next = phase.next();

            if (lastPhase != null && lastPhase != phase && RelicAugment.CYCLE_DIAL_CHIME.isOn(dial)) {
                minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.AMETHYST_BLOCK_CHIME, 1.2F, 1.0F));
                player.displayClientMessage(Component.translatable("message.relicweave.dial.changed", phase.displayName(), next.displayName()), true);
            }
            lastPhase = phase;

            int lead = warningSeconds(dial) * 20;
            if (remaining > 0 && remaining <= lead && phase != warnedPhase) {
                warnedPhase = phase;
                player.displayClientMessage(Component.translatable("message.relicweave.dial.warning", next.displayName(), (remaining + 19) / 20), true);
            }
        }

        @SubscribeEvent
        public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
            reset();
        }
    }
}
