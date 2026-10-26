package com.relicweave.vestige.relic;

import com.relicweave.relic.RelicAugment;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Gravity Nail: while anchored the player is fully protected against Gravity Shear (still bounded by the global
 * protection cap). Grants no flight and no knockback immunity. State is a server-side map of player to expiry game
 * time, cleared on server stop.
 */
public final class GravityNail {
    public static final int BASE_TICKS = 100;

    private static final Map<UUID, Long> ANCHORED_UNTIL = new HashMap<>();

    private GravityNail() {}

    /** Anchor duration (ticks): 100, Long Anchor x1.5, Quick Anchor x0.6. */
    public static int durationTicks(ItemStack stack) {
        if (VestigeRelics.has(stack, RelicAugment.GRAVITY_NAIL_LONG)) {
            return (int) Math.round(BASE_TICKS * 1.5);
        }
        if (VestigeRelics.has(stack, RelicAugment.GRAVITY_NAIL_QUICK)) {
            return (int) Math.round(BASE_TICKS * 0.6);
        }
        return BASE_TICKS;
    }

    /** Anchors {@code player} for {@code ticks}. A second anchor replaces the first; it never extends it. */
    public static void anchor(Player player, int ticks) {
        ANCHORED_UNTIL.put(player.getUUID(), player.level().getGameTime() + ticks);
    }

    public static boolean isAnchored(Player player) {
        Long until = ANCHORED_UNTIL.get(player.getUUID());
        return until != null && player.level().getGameTime() < until;
    }

    /** {@link com.relicweave.vestige.hazard.HazardProtection} contributor for GRAVITY_SHEAR. */
    public static double protection(Player player) {
        return isAnchored(player) ? 1.0 : 0.0;
    }

    /** Server tick: ground particles that show the anchor, and expiry cleanup. */
    static void tick(ServerPlayer player) {
        Long until = ANCHORED_UNTIL.get(player.getUUID());
        if (until == null) {
            return;
        }
        if (player.level().getGameTime() >= until) {
            ANCHORED_UNTIL.remove(player.getUUID());
            return;
        }
        if (player.tickCount % 4 == 0 && player.level() instanceof ServerLevel level) {
            for (int i = 0; i < 8; i++) {
                double angle = i * Math.PI / 4.0 + player.tickCount * 0.1;
                level.sendParticles(ParticleTypes.ENCHANT, player.getX() + Math.cos(angle) * 0.9, player.getY() + 0.05,
                        player.getZ() + Math.sin(angle) * 0.9, 1, 0.0, 0.1, 0.0, 0.02);
            }
        }
    }

    static void clearAll() {
        ANCHORED_UNTIL.clear();
    }
}
