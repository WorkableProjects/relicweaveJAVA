package com.relicweave.relic;

import net.minecraft.server.level.ServerPlayer;

/**
 * Server-side entry point for the Activate Relic key. TODO(T04): validate (alive, not spectator, relic carried in a
 * valid location via {@link RelicLocator}, activatable, off cooldown via {@link RelicCooldowns}) and dispatch.
 */
public final class RelicActivation {
    private RelicActivation() {}

    /** Called on the server thread. Returns true if the relic activated (or toggled). */
    public static boolean tryActivate(ServerPlayer player, RelicType type) {
        return false;
    }
}
