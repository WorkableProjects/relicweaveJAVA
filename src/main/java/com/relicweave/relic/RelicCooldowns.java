package com.relicweave.relic;

import com.relicweave.RelicWeave;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Authoritative per-player relic cooldowns, keyed by {@link RelicType} rather than by item stack, so moving, dropping,
 * duplicating or re-equipping a relic can never reset one.
 * <p>
 * Stored as an absolute "ready at" overworld game time in Forge's persisted player data
 * ({@link Player#PERSISTED_NBT_TAG}), which survives death, respawn, dimension changes, relogging and server restarts
 * (overworld game time is saved with the world and never goes backwards).
 * <p>
 * The vanilla item cooldown overlay is set as well, purely as a visual hint; it is never consulted.
 */
public final class RelicCooldowns {
    private static final String TAG = RelicWeave.MOD_ID + ":relic_cooldowns";

    private RelicCooldowns() {}

    private static long now(ServerPlayer player) {
        return player.server.overworld().getGameTime();
    }

    private static CompoundTag data(ServerPlayer player) {
        return player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG).getCompound(TAG);
    }

    /** Remaining cooldown in ticks (0 when ready). */
    public static int remainingTicks(ServerPlayer player, RelicType type) {
        long readyAt = data(player).getLong(type.itemPath());
        return (int) Math.max(0L, Math.min(Integer.MAX_VALUE, readyAt - now(player)));
    }

    public static boolean isReady(ServerPlayer player, RelicType type) {
        return remainingTicks(player, type) == 0;
    }

    /** Starts {@code type}'s configured cooldown. */
    public static void start(ServerPlayer player, RelicType type) {
        start(player, type, type.cooldownTicks());
    }

    public static void start(ServerPlayer player, RelicType type, int ticks) {
        CompoundTag persisted = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        CompoundTag cooldowns = persisted.getCompound(TAG);
        cooldowns.putLong(type.itemPath(), now(player) + ticks);
        persisted.put(TAG, cooldowns);
        player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persisted);
        showOverlay(player, type, ticks);
    }

    /** Re-applies the cosmetic item cooldown overlay (e.g. after login), from the authoritative value. */
    public static void showOverlay(ServerPlayer player, RelicType type, int ticks) {
        Item item = ForgeRegistries.ITEMS.getValue(type.itemId());
        if (item != null && ticks > 0) {
            player.getCooldowns().addCooldown(item, ticks);
        }
    }

    /** Clears every relic cooldown (GameTests and admin use). */
    public static void clearAll(ServerPlayer player) {
        CompoundTag persisted = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        persisted.remove(TAG);
        player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persisted);
    }
}
