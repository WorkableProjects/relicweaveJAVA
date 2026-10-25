package com.relicweave.client;

import com.relicweave.RelicWeave;
import com.relicweave.network.PurlingNetwork;
import com.relicweave.relic.RelicLocator;
import com.relicweave.relic.RelicType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.material.FogType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Client side of the relic controls: the selected relic (client-only state), the Cycle/Activate keys, and the Tidal
 * Charm's underwater visibility. The server re-validates every activation request.
 */
@Mod.EventBusSubscriber(modid = RelicWeave.MOD_ID, value = Dist.CLIENT)
public final class RelicClient {
    /** Underwater fog far-plane multiplier and minimum (blocks) with a Tidal Charm. */
    public static final float TIDAL_FOG_MULTIPLIER = 2.5F;
    public static final float TIDAL_FOG_MIN_DISTANCE = 96.0F;

    @Nullable
    public static RelicType selected;

    private RelicClient() {}

    /** Activatable relics the local player carries, in {@link RelicType#values()} order. */
    public static List<RelicType> carriedActivatable(LocalPlayer player) {
        List<RelicType> list = new ArrayList<>();
        for (RelicType type : RelicType.values()) {
            if (type.activatable() && RelicLocator.has(player, type)) {
                list.add(type);
            }
        }
        return list;
    }

    /** The current selection, falling back to the first carried activatable relic (or null when there is none). */
    @Nullable
    public static RelicType validSelection(LocalPlayer player) {
        List<RelicType> carried = carriedActivatable(player);
        if (carried.isEmpty()) {
            return null;
        }
        return selected != null && carried.contains(selected) ? selected : carried.get(0);
    }

    /** "Press R to activate when selected (G to switch)" with the current bindings. */
    public static net.minecraft.network.chat.MutableComponent activateHint() {
        return Component.translatable("tooltip.relicweave.relic.activate",
                ModKeyMappings.ACTIVATE_RELIC.getTranslatedKeyMessage(), ModKeyMappings.CYCLE_RELIC.getTranslatedKeyMessage());
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        while (ModKeyMappings.CYCLE_RELIC.consumeClick()) {
            if (player != null) {
                cycle(player);
            }
        }
        while (ModKeyMappings.ACTIVATE_RELIC.consumeClick()) {
            if (player != null) {
                activate(player);
            }
        }
    }

    private static void cycle(LocalPlayer player) {
        List<RelicType> carried = carriedActivatable(player);
        if (carried.isEmpty()) {
            selected = null;
            player.displayClientMessage(Component.translatable("message.relicweave.relic.none"), true);
            return;
        }
        int index = selected == null ? -1 : carried.indexOf(selected);
        selected = carried.get((index + 1) % carried.size());
        player.displayClientMessage(Component.translatable("message.relicweave.relic.selected", selected.displayName()), true);
    }

    private static void activate(LocalPlayer player) {
        RelicType type = validSelection(player);
        if (type == null) {
            player.displayClientMessage(Component.translatable("message.relicweave.relic.none"), true);
            return;
        }
        selected = type;
        PurlingNetwork.CHANNEL.sendToServer(new PurlingNetwork.ActivateRelicPacket(type.itemPath()));
    }

    /** Tidal Charm: see much further underwater. Forge needs the event cancelled for fog changes to apply. */
    @SubscribeEvent
    public static void onRenderFog(ViewportEvent.RenderFog event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || event.getCamera().getFluidInCamera() != FogType.WATER
                || !RelicLocator.has(player, RelicType.TIDAL)) {
            return;
        }
        event.setFarPlaneDistance(Math.max(event.getFarPlaneDistance() * TIDAL_FOG_MULTIPLIER, TIDAL_FOG_MIN_DISTANCE));
        event.setCanceled(true);
    }
}
