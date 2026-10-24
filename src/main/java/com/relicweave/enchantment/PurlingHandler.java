package com.relicweave.enchantment;

import com.relicweave.RelicWeave;
import com.relicweave.registry.ModEnchantments;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.projectile.ThrownEnderpearl;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.event.entity.EntityTeleportEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Server-side Purling logic: activation validation, the per-player cooldown, and teleport-damage protection for
 * pearls launched by Purling.
 * <p>
 * Cooldowns are keyed by player UUID and measured in server ticks, so they persist across death, respawn, and
 * relogging for as long as the server runs. They reset when the server stops.
 */
@Mod.EventBusSubscriber(modid = RelicWeave.MOD_ID)
public final class PurlingHandler {
    public static final int COOLDOWN_TICKS = 600;
    /** Marks pearls launched by Purling, so only their landing damage is removed. */
    private static final String PURLING_PEARL_TAG = RelicWeave.MOD_ID + ":purling_pearl";

    private static final Map<UUID, Integer> READY_AT_TICK = new HashMap<>();

    private PurlingHandler() {}

    /** Called on the server thread when a client presses the Purling key. */
    public static void tryActivate(ServerPlayer player) {
        if (!player.isAlive() || player.isSpectator()) {
            return;
        }
        ItemStack boots = player.getItemBySlot(EquipmentSlot.FEET);
        if (EnchantmentHelper.getItemEnchantmentLevel(ModEnchantments.PURLING.get(), boots) <= 0) {
            return;
        }

        int now = player.server.getTickCount();
        Integer readyAt = READY_AT_TICK.get(player.getUUID());
        if (readyAt != null && now < readyAt) {
            int seconds = (readyAt - now + 19) / 20;
            player.displayClientMessage(Component.translatable("message.relicweave.purling.cooldown", seconds), true);
            return;
        }

        ItemStack pearls = findEnderPearls(player.getInventory());
        if (pearls.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.relicweave.purling.no_pearl"), true);
            return;
        }

        ServerLevel level = player.serverLevel();
        ThrownEnderpearl pearl = new ThrownEnderpearl(level, player);
        pearl.setItem(new ItemStack(Items.ENDER_PEARL));
        pearl.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 1.5F, 1.0F);
        pearl.getPersistentData().putBoolean(PURLING_PEARL_TAG, true);
        if (!level.addFreshEntity(pearl)) {
            return;
        }

        // Match vanilla: creative players keep their pearl.
        if (!player.getAbilities().instabuild) {
            pearls.shrink(1);
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENDER_PEARL_THROW,
                SoundSource.NEUTRAL, 0.5F, 0.4F / (level.getRandom().nextFloat() * 0.4F + 0.8F));
        READY_AT_TICK.put(player.getUUID(), now + COOLDOWN_TICKS);
    }

    private static ItemStack findEnderPearls(Inventory inventory) {
        ItemStack offhand = inventory.offhand.get(0);
        if (offhand.is(Items.ENDER_PEARL)) {
            return offhand;
        }
        for (ItemStack stack : inventory.items) {
            if (stack.is(Items.ENDER_PEARL)) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    public static boolean isPurlingPearl(Entity entity) {
        return entity instanceof ThrownEnderpearl && entity.getPersistentData().getBoolean(PURLING_PEARL_TAG);
    }

    /** Removes the self-inflicted landing damage, but only for pearls launched by Purling. */
    @SubscribeEvent
    public static void onEnderPearlLand(EntityTeleportEvent.EnderPearl event) {
        if (isPurlingPearl(event.getPearlEntity())) {
            event.setAttackDamage(0.0F);
        }
    }

    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        READY_AT_TICK.clear();
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        READY_AT_TICK.clear();
    }
}
