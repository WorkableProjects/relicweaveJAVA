package com.relicweave.relic;

import com.relicweave.RelicWeave;
import com.relicweave.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Wayfarer's Relic: binding (sneak + use stores the block position and dimension in the item's NBT) and the return
 * channel. The channel lives in memory only; it is cancelled, without cooldown, when the player moves, is hurt, dies,
 * logs out, changes dimension, or no longer carries the relic. The cooldown starts only after a successful teleport.
 */
@Mod.EventBusSubscriber(modid = RelicWeave.MOD_ID)
public final class WayfarerChannel {
    /** Channel duration in ticks. */
    public static final int CHANNEL_TICKS = 60;
    /** Maximum horizontal distance (blocks) the player may move from the start before the channel breaks. */
    public static final double MAX_MOVE = 0.5;
    /** Item NBT key of the binding compound. */
    public static final String BINDING_TAG = "WayfarerBinding";

    private record Channel(Vec3 start, int[] ticks) {}

    private static final Map<UUID, Channel> CHANNELS = new HashMap<>();

    private WayfarerChannel() {}

    // ---- Binding ----

    /** Stores the player's block position and dimension in {@code stack}. */
    public static void bind(ServerPlayer player, ItemStack stack) {
        BlockPos pos = player.blockPosition();
        CompoundTag tag = new CompoundTag();
        tag.putInt("x", pos.getX());
        tag.putInt("y", pos.getY());
        tag.putInt("z", pos.getZ());
        tag.putString("dimension", player.level().dimension().location().toString());
        stack.getOrCreateTag().put(BINDING_TAG, tag);
        player.displayClientMessage(Component.translatable("message.relicweave.wayfarer.bound", pos.getX(), pos.getY(), pos.getZ()), true);
    }

    public static boolean hasBinding(ItemStack stack) {
        return stack.hasTag() && stack.getTag().contains(BINDING_TAG, 10);
    }

    public static CompoundTag binding(ItemStack stack) {
        return stack.getTag().getCompound(BINDING_TAG);
    }

    // ---- Channel ----

    public static boolean isChanneling(ServerPlayer player) {
        return CHANNELS.containsKey(player.getUUID());
    }

    /** Starts the channel (the caller has already checked carrying and cooldown). */
    static boolean start(ServerPlayer player) {
        ItemStack bound = ItemStack.EMPTY;
        for (ItemStack stack : RelicLocator.find(player, s -> s.getItem() instanceof com.relicweave.item.RelicItem relic
                && relic.getRelicType() == RelicType.WAYFARER && hasBinding(s))) {
            bound = stack;
            break;
        }
        if (bound.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.relicweave.wayfarer.unbound"), true);
            return false;
        }
        if (!binding(bound).getString("dimension").equals(player.level().dimension().location().toString())) {
            player.displayClientMessage(Component.translatable("message.relicweave.wayfarer.other_dimension"), true);
            return false;
        }
        if (isChanneling(player)) {
            return false;
        }
        CHANNELS.put(player.getUUID(), new Channel(player.position(), new int[] {0}));
        player.displayClientMessage(Component.translatable("message.relicweave.wayfarer.channeling"), true);
        return true;
    }

    private static void cancel(ServerPlayer player, String reasonKey) {
        if (CHANNELS.remove(player.getUUID()) != null && reasonKey != null) {
            player.displayClientMessage(Component.translatable(reasonKey), true);
        }
    }

    /** Advances the player's channel by one tick (no-op when not channelling). Public for GameTests. */
    public static void tickChannel(ServerPlayer player) {
        Channel channel = CHANNELS.get(player.getUUID());
        if (channel == null) {
            return;
        }
        if (!player.isAlive() || !RelicLocator.has(player, RelicType.WAYFARER)) {
            cancel(player, "message.relicweave.wayfarer.cancelled");
            return;
        }
        double dx = player.getX() - channel.start().x;
        double dz = player.getZ() - channel.start().z;
        if (dx * dx + dz * dz > MAX_MOVE * MAX_MOVE) {
            cancel(player, "message.relicweave.wayfarer.cancelled");
            return;
        }
        ServerLevel level = player.serverLevel();
        level.sendParticles(ParticleTypes.PORTAL, player.getX(), player.getY() + 1.0, player.getZ(), 6, 0.4, 0.8, 0.4, 0.1);
        if (++channel.ticks()[0] < CHANNEL_TICKS) {
            return;
        }
        CHANNELS.remove(player.getUUID());
        complete(player);
    }

    private static void complete(ServerPlayer player) {
        ItemStack bound = RelicLocator.first(player, RelicType.WAYFARER);
        for (ItemStack stack : RelicLocator.find(player, s -> s.getItem() instanceof com.relicweave.item.RelicItem relic
                && relic.getRelicType() == RelicType.WAYFARER && hasBinding(s))) {
            bound = stack;
            break;
        }
        ServerLevel level = player.serverLevel();
        if (!hasBinding(bound) || !binding(bound).getString("dimension").equals(level.dimension().location().toString())) {
            player.displayClientMessage(Component.translatable("message.relicweave.wayfarer.cancelled"), true);
            return;
        }
        CompoundTag tag = binding(bound);
        BlockPos pos = new BlockPos(tag.getInt("x"), tag.getInt("y"), tag.getInt("z"));
        if (!level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()
                || !level.getBlockState(pos.above()).getCollisionShape(level, pos.above()).isEmpty()) {
            player.displayClientMessage(Component.translatable("message.relicweave.wayfarer.obstructed"), true);
            return;
        }
        player.teleportTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        player.fallDistance = 0.0F;
        level.playSound(null, pos, ModSounds.RELIC_ACTIVATE.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        RelicCooldowns.start(player, RelicType.WAYFARER);
    }

    // ---- Events ----

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.player instanceof ServerPlayer player) {
            tickChannel(player);
        }
    }

    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && event.getAmount() > 0.0F) {
            cancel(player, "message.relicweave.wayfarer.cancelled");
        }
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            cancel(player, null);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        CHANNELS.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onDimensionChange(PlayerEvent.PlayerChangedDimensionEvent event) {
        CHANNELS.remove(event.getEntity().getUUID());
    }
}
