package com.relicweave.vestige.hazard.field;

import com.relicweave.RelicWeave;
import com.relicweave.vestige.VestigeKeys;
import com.relicweave.vestige.hazard.HazardStatus;
import com.relicweave.vestige.network.VestigeNetwork.HazardSyncPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

import java.util.UUID;

/**
 * The single server tick handler for the four field hazards. Every {@link #INTERVAL} ticks it runs each hazard for
 * survival and adventure players in The Vestige (never creative, spectator or other dimensions). Per-player transient
 * state lives in each hazard class and is cleared on logout, dimension change, death and server stop.
 */
@Mod.EventBusSubscriber(modid = RelicWeave.MOD_ID)
public final class FieldHazards {
    /** Ticks between hazard updates. */
    public static final int INTERVAL = 5;

    private FieldHazards() {}

    /** True for a player the field hazards may affect: in The Vestige and in survival or adventure mode. */
    public static boolean isAffected(ServerPlayer player) {
        return player.serverLevel().dimension() == VestigeKeys.THE_VESTIGE && !player.isCreative() && !player.isSpectator();
    }

    private static void clear(UUID id) {
        GravityShear.clear(id);
        StarfallImpacts.clear(id);
        NullPockets.clear(id);
        HushNoise.clear(id);
    }

    /** Display flags contributed to the hazard sync packet. */
    public static int flags(ServerPlayer player) {
        int flags = 0;
        UUID id = player.getUUID();
        if (GravityShear.isTelegraphing(id)) {
            flags |= HazardSyncPacket.GRAVITY_TELEGRAPH;
        }
        if (NullPockets.isInside(id)) {
            flags |= HazardSyncPacket.IN_NULL_POCKET;
        }
        if (HushNoise.isInHushfield(id)) {
            flags |= HazardSyncPacket.IN_HUSHFIELD;
        }
        return flags;
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)
                || player.tickCount % INTERVAL != 0) {
            return;
        }
        if (!isAffected(player)) {
            clear(player.getUUID());
            return;
        }
        GravityShear.tick(player);
        StarfallImpacts.tick(player);
        NullPockets.tick(player);
        HushNoise.tick(player);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        clear(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onDimensionChange(PlayerEvent.PlayerChangedDimensionEvent event) {
        clear(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        clear(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        GravityShear.clearAll();
        StarfallImpacts.clearAll();
        NullPockets.clearAll();
        HushNoise.clearAll();
    }

    /** Registers the display-flag provider. */
    @Mod.EventBusSubscriber(modid = RelicWeave.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class Setup {
        private Setup() {}

        @SubscribeEvent
        public static void onCommonSetup(FMLCommonSetupEvent event) {
            event.enqueueWork(() -> HazardStatus.registerFlags(FieldHazards::flags));
        }
    }
}
