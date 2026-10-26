package com.relicweave.vestige.hazard.field;

import com.relicweave.vestige.VestigeConfig;
import com.relicweave.vestige.VestigeKeys;
import com.relicweave.vestige.hazard.FieldRegistry;
import com.relicweave.vestige.registry.VestigeSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.player.Player;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Null Pockets: around a {@code null_ward} block (6 blocks horizontally, 4 vertically) active relics are suppressed.
 * It never changes cooldowns or charges; relic activation just asks {@link #isSuppressed(Player)}. Server-side only.
 */
public final class NullPockets {
    public static final int HORIZONTAL_RADIUS = 6;
    public static final int VERTICAL_RADIUS = 4;

    private static final Set<UUID> INSIDE = new HashSet<>();

    private NullPockets() {}

    static void clear(UUID id) {
        INSIDE.remove(id);
    }

    static void clearAll() {
        INSIDE.clear();
    }

    static boolean isInside(UUID id) {
        return INSIDE.contains(id);
    }

    private static boolean enabled() {
        return !VestigeConfig.SPEC.isLoaded() || VestigeConfig.NULL_POCKETS.get();
    }

    /** True if {@code player} is in The Vestige, inside a Null Pocket, and Null Pockets are enabled. */
    public static boolean isSuppressed(Player player) {
        return player.level().dimension() == VestigeKeys.THE_VESTIGE && isSuppressedIgnoringDimension(player);
    }

    /** Same as {@link #isSuppressed} without the dimension check (tests, and a future per-dimension override). */
    public static boolean isSuppressedIgnoringDimension(Player player) {
        if (!enabled() || !(player.level() instanceof ServerLevel level)) {
            return false;
        }
        BlockPos origin = player.blockPosition();
        return level.getPoiManager().getInSquare(holder -> holder.value() == FieldRegistry.NULL_WARD_POI.get(),
                origin, HORIZONTAL_RADIUS, PoiManager.Occupancy.ANY).anyMatch(record -> {
            BlockPos p = record.getPos();
            long dx = p.getX() - origin.getX();
            long dz = p.getZ() - origin.getZ();
            return dx * dx + dz * dz <= (long) HORIZONTAL_RADIUS * HORIZONTAL_RADIUS
                    && Math.abs(p.getY() - origin.getY()) <= VERTICAL_RADIUS;
        });
    }

    static void tick(ServerPlayer player) {
        UUID id = player.getUUID();
        boolean inside = isSuppressed(player);
        if (inside && INSIDE.add(id)) {
            player.displayClientMessage(Component.translatable("message.relicweave.null_pocket.enter"), true);
            player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
                    VestigeSounds.NULL_POCKET.get(), SoundSource.PLAYERS, 0.8F, 1.0F);
        } else if (!inside) {
            INSIDE.remove(id);
        }
    }
}
