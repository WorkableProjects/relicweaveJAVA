package com.relicweave.vestige.relic;

import com.relicweave.relic.RelicAugment;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Riftglass Lantern: a temporary zone (level, centre, expiry, strength) that gives players inside Rift protection.
 * Zones never stack (the strongest applies) and each player owns at most one. Server-side; cleared on server stop.
 */
public final class RiftglassLantern {
    public static final int DURATION_TICKS = 600;
    public static final double BASE_RADIUS = 4.0;
    public static final double BASE_STRENGTH = 0.5;

    /** One active zone. */
    public record Zone(ResourceKey<Level> level, Vec3 centre, long expiresAt, double strength, double radius) {}

    private static final Map<UUID, Zone> ZONES = new HashMap<>();

    private RiftglassLantern() {}

    /** Radius: 4, Broad Lantern 6, Bright Lantern 3. */
    public static double radius(ItemStack stack) {
        if (VestigeRelics.has(stack, RelicAugment.LANTERN_BROAD)) {
            return 6.0;
        }
        return VestigeRelics.has(stack, RelicAugment.LANTERN_BRIGHT) ? 3.0 : BASE_RADIUS;
    }

    /** Rift protection fraction: 0.5, Broad Lantern 0.35, Bright Lantern 0.65. */
    public static double strength(ItemStack stack) {
        if (VestigeRelics.has(stack, RelicAugment.LANTERN_BROAD)) {
            return 0.35;
        }
        return VestigeRelics.has(stack, RelicAugment.LANTERN_BRIGHT) ? 0.65 : BASE_STRENGTH;
    }

    /** Creates (or replaces) {@code owner}'s zone at their position. */
    public static Zone create(ServerPlayer owner, ItemStack lantern) {
        Zone zone = new Zone(owner.level().dimension(), owner.position(), owner.level().getGameTime() + DURATION_TICKS,
                strength(lantern), radius(lantern));
        ZONES.put(owner.getUUID(), zone);
        return zone;
    }

    /** Strongest live zone protecting {@code player} right now (0 outside every zone). */
    public static double protection(Player player) {
        double best = 0.0;
        long now = player.level().getGameTime();
        for (Zone zone : ZONES.values()) {
            if (zone.level() == player.level().dimension() && now < zone.expiresAt()
                    && player.position().distanceToSqr(zone.centre()) <= zone.radius() * zone.radius()) {
                best = Math.max(best, zone.strength());
            }
        }
        return best;
    }

    public static int activeZones() {
        return ZONES.size();
    }

    /** Server tick: expiry and the END_ROD ring every 10 ticks. */
    static void tick(MinecraftServer server) {
        if (ZONES.isEmpty()) {
            return;
        }
        List<UUID> expired = new ArrayList<>();
        for (Map.Entry<UUID, Zone> entry : ZONES.entrySet()) {
            Zone zone = entry.getValue();
            ServerLevel level = server.getLevel(zone.level());
            if (level == null || level.getGameTime() >= zone.expiresAt()) {
                expired.add(entry.getKey());
            } else if (level.getGameTime() % 10 == 0) {
                int points = (int) Math.max(12, Math.round(zone.radius() * 5));
                for (int i = 0; i < points; i++) {
                    double angle = i * 2 * Math.PI / points;
                    level.sendParticles(ParticleTypes.END_ROD, zone.centre().x + Math.cos(angle) * zone.radius(), zone.centre().y + 0.1,
                            zone.centre().z + Math.sin(angle) * zone.radius(), 1, 0.0, 0.02, 0.0, 0.0);
                }
            }
        }
        expired.forEach(ZONES::remove);
    }

    public static void clearAll() {
        ZONES.clear();
    }
}
