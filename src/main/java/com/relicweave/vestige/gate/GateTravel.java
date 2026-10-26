package com.relicweave.vestige.gate;

import com.relicweave.RelicWeave;
import com.relicweave.vestige.VestigeConfig;
import com.relicweave.vestige.VestigeKeys;
import com.relicweave.vestige.registry.VestigeSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * Server-side travel between the world and The Vestige: destination resolution, the persisted return point and the
 * per-player teleport cooldown. Both live in Forge's persisted player data so they survive death, relog and restart.
 */
public final class GateTravel {
    /** Persisted key: absolute game time at which the player may use a gate again. */
    public static final String COOLDOWN_TAG = RelicWeave.MOD_ID + ":gate_cooldown";
    /** Persisted key: compound with dimension, x, y, z, yaw of where the player entered The Vestige. */
    public static final String RETURN_TAG = RelicWeave.MOD_ID + ":vestige_return";

    /** A resolved teleport target. */
    public record Destination(ServerLevel level, double x, double y, double z, float yRot) {}

    private GateTravel() {}

    // ---- Persisted data ----

    private static CompoundTag persisted(ServerPlayer player) {
        CompoundTag root = player.getPersistentData();
        if (!root.contains(Player.PERSISTED_NBT_TAG, 10)) {
            root.put(Player.PERSISTED_NBT_TAG, new CompoundTag());
        }
        return root.getCompound(Player.PERSISTED_NBT_TAG);
    }

    private static long now(ServerPlayer player) {
        return player.server.overworld().getGameTime();
    }

    public static int cooldownRemaining(ServerPlayer player) {
        long readyAt = persisted(player).getLong(COOLDOWN_TAG);
        return (int) Math.max(0L, Math.min(Integer.MAX_VALUE, readyAt - now(player)));
    }

    public static void startCooldown(ServerPlayer player) {
        persisted(player).putLong(COOLDOWN_TAG, now(player) + VestigeConfig.TELEPORT_COOLDOWN_SECONDS.get() * 20L);
    }

    /** Saves the player's current dimension, position and yaw as the return point. */
    public static void saveReturnPoint(ServerPlayer player) {
        CompoundTag tag = new CompoundTag();
        tag.putString("dimension", player.level().dimension().location().toString());
        tag.putDouble("x", player.getX());
        tag.putDouble("y", player.getY());
        tag.putDouble("z", player.getZ());
        tag.putFloat("yaw", player.getYRot());
        persisted(player).put(RETURN_TAG, tag);
    }

    public static boolean hasReturnPoint(ServerPlayer player) {
        return persisted(player).contains(RETURN_TAG, 10);
    }

    /** The stored return point compound (empty if none). */
    public static CompoundTag returnPoint(ServerPlayer player) {
        return persisted(player).getCompound(RETURN_TAG).copy();
    }

    // ---- Destination resolution ----

    public static boolean inVestige(ServerPlayer player) {
        return player.level().dimension().equals(VestigeKeys.THE_VESTIGE);
    }

    /** Where this player would go if they used a gate now. Builds the Arrival Plaza on first need; no other side effects. */
    @Nullable
    public static Destination destinationFor(ServerPlayer player) {
        MinecraftServer server = player.server;
        if (!inVestige(player)) {
            return arrivalDestination(server);
        }
        return returnDestination(server, persisted(player).getCompound(RETURN_TAG));
    }

    @Nullable
    public static Destination arrivalDestination(MinecraftServer server) {
        BlockPos arrival = ArrivalPlaza.arrival(server);
        ServerLevel vestige = ArrivalPlaza.vestige(server);
        if (arrival == null || vestige == null) {
            return null;
        }
        return new Destination(vestige, arrival.getX() + 0.5, arrival.getY(), arrival.getZ() + 0.5, 180.0F);
    }

    /** Return point if usable, else the nearest safe spot within 8 blocks, else the overworld spawn. */
    public static Destination returnDestination(MinecraftServer server, CompoundTag point) {
        if (point.contains("dimension", 8)) {
            ResourceLocation id = ResourceLocation.tryParse(point.getString("dimension"));
            ResourceKey<Level> key = id == null ? null : ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, id);
            ServerLevel level = key == null || key.equals(VestigeKeys.THE_VESTIGE) ? null : server.getLevel(key);
            if (level != null) {
                double x = point.getDouble("x");
                double y = point.getDouble("y");
                double z = point.getDouble("z");
                float yaw = point.getFloat("yaw");
                BlockPos feet = BlockPos.containing(x, y, z);
                if (level.isInWorldBounds(feet)) {
                    level.getChunk(feet);
                    if (SafeSpot.isSafe(level, feet)) {
                        return new Destination(level, x, y, z, yaw);
                    }
                    Optional<BlockPos> near = SafeSpot.find(level, feet);
                    if (near.isPresent()) {
                        BlockPos p = near.get();
                        return new Destination(level, p.getX() + 0.5, p.getY(), p.getZ() + 0.5, yaw);
                    }
                }
            }
        }
        return overworldSpawn(server);
    }

    public static Destination overworldSpawn(MinecraftServer server) {
        ServerLevel overworld = server.overworld();
        BlockPos spawn = overworld.getSharedSpawnPos();
        overworld.getChunk(spawn);
        int y = overworld.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, spawn.getX(), spawn.getZ());
        return new Destination(overworld, spawn.getX() + 0.5, y, spawn.getZ() + 0.5, 0.0F);
    }

    // ---- Travel ----

    /**
     * Checks the cooldown, resolves the destination, saves the return point when leaving the overworld side and starts
     * the cooldown. Returns null (after telling the player why) when travel is not possible. Does not teleport.
     */
    @Nullable
    public static Destination prepare(ServerPlayer player) {
        Destination destination = destinationFor(player);
        if (destination == null) {
            player.displayClientMessage(Component.translatable("message.relicweave.gate.unavailable"), true);
            return null;
        }
        int remaining = cooldownRemaining(player);
        if (remaining > 0) {
            player.displayClientMessage(Component.translatable("message.relicweave.gate.cooldown", (remaining + 19) / 20), true);
            return null;
        }
        if (!inVestige(player)) {
            saveReturnPoint(player);
        }
        startCooldown(player);
        return destination;
    }

    /** Full gate use: {@link #prepare} then teleport. */
    public static boolean travel(ServerPlayer player) {
        Destination destination = prepare(player);
        if (destination == null) {
            return false;
        }
        teleport(player, destination);
        return true;
    }

    /** Return Waystone use: only meaningful inside The Vestige. */
    public static boolean travelBack(ServerPlayer player) {
        if (!inVestige(player)) {
            player.displayClientMessage(Component.translatable("message.relicweave.gate.waystone_dormant"), true);
            return false;
        }
        return travel(player);
    }

    /** Debug: go to the Arrival Plaza without cooldown (saves a return point unless already inside). */
    public static boolean debugToArrival(ServerPlayer player) {
        Destination destination = arrivalDestination(player.server);
        if (destination == null) {
            return false;
        }
        if (!inVestige(player)) {
            saveReturnPoint(player);
        }
        teleport(player, destination);
        return true;
    }

    /** Debug: go to the return point without cooldown. */
    public static boolean debugReturn(ServerPlayer player) {
        teleport(player, returnDestination(player.server, persisted(player).getCompound(RETURN_TAG)));
        return true;
    }

    public static void teleport(ServerPlayer player, Destination destination) {
        player.serverLevel().playSound(null, player.blockPosition(), VestigeSounds.GATE_TELEPORT.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        player.stopRiding();
        player.teleportTo(destination.level(), destination.x(), destination.y(), destination.z(), destination.yRot(), player.getXRot());
        player.fallDistance = 0.0F;
        destination.level().playSound(null, BlockPos.containing(destination.x(), destination.y(), destination.z()),
                VestigeSounds.GATE_TELEPORT.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
    }
}
