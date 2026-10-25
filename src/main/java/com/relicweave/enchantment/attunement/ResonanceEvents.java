package com.relicweave.enchantment.attunement;

import com.relicweave.RelicWeave;
import com.relicweave.network.PurlingNetwork;
import com.relicweave.registry.ModEnchantments;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Resonance (pickaxe): consecutive breaks of the same block type build a streak. While the streak is fresh, mining
 * that block type is faster by {@code 1 + min(streak - 1, MAX_STACKS) * 1% * level} (at most +10% per level). A
 * different block or a pause of {@link #RESET_TICKS} resets it. Streaks live in memory (cleared on server
 * start/stop). The server is authoritative and sends each new streak to the miner
 * ({@link PurlingNetwork.ResonanceStreakPacket}), because break speed is also computed client-side and a client that
 * did not know the streak would not mine faster on a dedicated server.
 */
@Mod.EventBusSubscriber(modid = RelicWeave.MOD_ID)
public final class ResonanceEvents {
    public static final int RESET_TICKS = 100;
    public static final int MAX_STACKS = 10;
    public static final float BONUS_PER_STACK_PER_LEVEL = 0.01F;

    private record Streak(Block block, int count, long lastTick) {}

    private static final Map<UUID, Streak> STREAKS = new ConcurrentHashMap<>();

    private ResonanceEvents() {}

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        Player player = event.getPlayer();
        if (player instanceof ServerPlayer serverPlayer && level(player.getMainHandItem()) > 0) {
            recordBreak(player.getUUID(), event.getState().getBlock(), player.level().getGameTime());
            Streak streak = STREAKS.get(player.getUUID());
            PurlingNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> serverPlayer),
                    new PurlingNetwork.ResonanceStreakPacket(ForgeRegistries.BLOCKS.getKey(streak.block()), streak.count()));
        }
    }

    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        Player player = event.getEntity();
        int level = level(player.getMainHandItem());
        if (level > 0) {
            event.setNewSpeed(event.getNewSpeed()
                    * speedMultiplier(player.getUUID(), event.getState().getBlock(), player.level().getGameTime(), level));
        }
    }

    private static int level(ItemStack tool) {
        return EnchantmentHelper.getItemEnchantmentLevel(ModEnchantments.RESONANCE.get(), tool);
    }

    /** Registers a break: extends the streak when same block and fresh, otherwise restarts it at 1. */
    public static void recordBreak(UUID player, Block block, long gameTime) {
        STREAKS.compute(player, (id, old) -> old != null && old.block == block && gameTime - old.lastTick <= RESET_TICKS
                ? new Streak(block, old.count + 1, gameTime) : new Streak(block, 1, gameTime));
    }

    /**
     * Client side: replaces the local player's streak with the server's (set, not increment, so an integrated server
     * sharing this map is unaffected). Stamped with the client's own game time, which the freshness check uses.
     */
    public static void applyStreak(UUID player, Block block, int count, long gameTime) {
        STREAKS.put(player, new Streak(block, count, gameTime));
    }

    /** Break speed multiplier for mining {@code block} now (1.0 when there is no fresh streak of that block). */
    public static float speedMultiplier(UUID player, Block block, long gameTime, int level) {
        Streak streak = STREAKS.get(player);
        if (streak == null || streak.block != block || gameTime - streak.lastTick > RESET_TICKS) {
            return 1.0F;
        }
        return 1.0F + Math.min(streak.count - 1, MAX_STACKS) * BONUS_PER_STACK_PER_LEVEL * level;
    }

    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        STREAKS.clear();
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        STREAKS.clear();
    }
}
