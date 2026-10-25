package com.relicweave.relic;

import net.minecraft.world.entity.player.Player;

/** Small helpers shared by the charm and totem handlers. */
final class RelicAttributes {
    private RelicAttributes() {}

    /** Minecraft 1.20.1 does not lower current health when max health drops, so do it explicitly. */
    static void clampHealth(Player player) {
        if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
    }
}
