package com.relicweave.vestige.hazard;

import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToIntFunction;

/**
 * Lets each hazard system contribute display flags ({@code VestigeNetwork.HazardSyncPacket} bits) to the single hazard
 * sync packet the Rift Saturation ticker sends. Providers register once during common setup; server-side only.
 */
public final class HazardStatus {
    private static final List<ToIntFunction<ServerPlayer>> PROVIDERS = new ArrayList<>();

    private HazardStatus() {}

    public static synchronized void registerFlags(ToIntFunction<ServerPlayer> provider) {
        PROVIDERS.add(provider);
    }

    /** OR of every provider's flags for {@code player}. */
    public static int extraFlags(ServerPlayer player) {
        int flags = 0;
        for (ToIntFunction<ServerPlayer> provider : PROVIDERS) {
            flags |= provider.applyAsInt(player);
        }
        return flags;
    }
}
