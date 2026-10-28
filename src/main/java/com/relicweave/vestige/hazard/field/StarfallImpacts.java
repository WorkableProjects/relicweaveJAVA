package com.relicweave.vestige.hazard.field;

import com.relicweave.RelicWeave;
import com.relicweave.vestige.VestigeConfig;
import com.relicweave.vestige.VestigeKeys;
import com.relicweave.vestige.cycle.ConcordanceCycle;
import com.relicweave.vestige.cycle.CyclePhase;
import com.relicweave.vestige.hazard.FieldRegistry;
import com.relicweave.vestige.hazard.HazardProtection;
import com.relicweave.vestige.registry.VestigeSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiPredicate;

/**
 * Starfall Impact: in {@code #relicweave:starfall} biomes during Resonance and Convergence, a ring marker appears near
 * the player and, after the warning time, strikes for area damage if the sky above it is open. Cover makes the impact
 * fizzle. No block damage, no fire blocks. Server-side only.
 */
public final class StarfallImpacts {
    public static final ResourceKey<DamageType> DAMAGE_TYPE = ResourceKey.create(Registries.DAMAGE_TYPE, RelicWeave.id("starfall"));
    public static final TagKey<EntityType<?>> IMMUNE = TagKey.create(Registries.ENTITY_TYPE, RelicWeave.id("starfall_immune"));
    public static final int MAX_MARKERS_PER_PLAYER = 3;

    private static final Map<UUID, Long> NEXT_SPAWN = new HashMap<>();
    private static volatile BiPredicate<ServerLevel, BlockPos> skyCheck = StarfallImpacts::defaultSkyCheck;

    private StarfallImpacts() {}

    static void clear(UUID id) {
        NEXT_SPAWN.remove(id);
    }

    static void clearAll() {
        NEXT_SPAWN.clear();
    }

    private static boolean enabled() {
        return !VestigeConfig.SPEC.isLoaded() || VestigeConfig.STARFALL.get();
    }

    private static float baseDamage() {
        return VestigeConfig.SPEC.isLoaded() ? VestigeConfig.STARFALL_DAMAGE.get().floatValue() : 6.0F;
    }

    private static int warningTicks() {
        return VestigeConfig.SPEC.isLoaded() ? VestigeConfig.STARFALL_WARNING_TICKS.get() : 60;
    }

    private static int intervalTicks(boolean convergence) {
        int seconds = VestigeConfig.SPEC.isLoaded() ? VestigeConfig.STARFALL_INTERVAL_SECONDS.get() : 12;
        return convergence ? seconds * 10 : seconds * 20;
    }

    /** Ticks until the next impact: a random delay in [0.5, 1.5] x the interval. */
    public static long randomDelay(RandomSource random, int intervalTicks) {
        return Math.round(intervalTicks * (0.5 + random.nextDouble()));
    }

    /** Damage at {@code distance} from the centre: full at the centre, half at the edge. */
    public static float damageAt(float base, double distance, double radius) {
        return (float) (base * (1.0 - 0.5 * Math.min(1.0, distance / radius)));
    }

    /**
     * Spawns a telegraphed impact at {@code pos}: after {@code warningTicks} it strikes for up to {@code damage}
     * within 2.5 blocks if the sky is open. Also used by the Starforged Colossus.
     */
    public static StarfallMarker spawnMarker(ServerLevel level, Vec3 pos, int warningTicks, float damage, @Nullable Entity owner) {
        StarfallMarker marker = new StarfallMarker(level);
        marker.setup(warningTicks, damage, owner);
        marker.setPos(pos.x, pos.y, pos.z);
        level.addFreshEntity(marker);
        level.playSound(null, pos.x, pos.y, pos.z, VestigeSounds.STARFALL_WARNING.get(), SoundSource.HOSTILE, 1.5F, 1.0F);
        return marker;
    }

    static void tick(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        UUID id = player.getUUID();
        CyclePhase phase = ConcordanceCycle.phase(level);
        boolean active = enabled() && phase != CyclePhase.QUIET && level.getBiome(player.blockPosition()).is(VestigeKeys.STARFALL);
        if (!active) {
            NEXT_SPAWN.remove(id);
            return;
        }
        long now = level.getGameTime();
        RandomSource random = level.getRandom();
        int interval = intervalTicks(phase == CyclePhase.CONVERGENCE);
        Long next = NEXT_SPAWN.get(id);
        if (next == null) {
            NEXT_SPAWN.put(id, now + randomDelay(random, interval));
            return;
        }
        if (now < next) {
            return;
        }
        NEXT_SPAWN.put(id, now + randomDelay(random, interval));
        AABB nearby = player.getBoundingBox().inflate(24.0);
        if (level.getEntitiesOfClass(StarfallMarker.class, nearby).size() >= MAX_MARKERS_PER_PLAYER) {
            return;
        }
        spawnMarker(level, pickTarget(player, random), warningTicks(), baseDamage(), null);
    }

    private static Vec3 pickTarget(ServerPlayer player, RandomSource random) {
        if (random.nextFloat() < 0.25F) {
            return player.position();
        }
        double angle = random.nextDouble() * Math.PI * 2.0;
        double distance = 3.0 + random.nextDouble() * 7.0;
        int x = Mth.floor(player.getX() + Math.cos(angle) * distance);
        int z = Mth.floor(player.getZ() + Math.sin(angle) * distance);
        int y = player.serverLevel().getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
        return new Vec3(x + 0.5, y, z + 0.5);
    }

    /** Default cover check: the sky is visible from the block above the marker. */
    private static boolean defaultSkyCheck(ServerLevel level, BlockPos pos) {
        return level.canSeeSky(pos.above());
    }

    /**
     * Replaces the cover check (GameTests run underground, where the real sky is never visible). Pass {@code null}
     * to restore the default {@code canSeeSky(pos.above())}.
     */
    public static void setSkyCheck(@Nullable BiPredicate<ServerLevel, BlockPos> check) {
        skyCheck = check == null ? StarfallImpacts::defaultSkyCheck : check;
    }

    /** Resolves the marker: strike if the sky is open, otherwise fizzle. */
    static void strike(ServerLevel level, StarfallMarker marker) {
        Vec3 c = marker.position();
        BlockPos pos = marker.blockPosition();
        if (!skyCheck.test(level, pos)) {
            level.sendParticles(ParticleTypes.SMOKE, c.x, c.y + 0.3, c.z, 20, 0.8, 0.3, 0.8, 0.02);
            return;
        }
        level.playSound(null, c.x, c.y, c.z, VestigeSounds.STARFALL_IMPACT.get(), SoundSource.HOSTILE, 2.0F, 1.0F);
        level.sendParticles(ParticleTypes.EXPLOSION, c.x, c.y + 0.3, c.z, 3, 0.8, 0.2, 0.8, 0.0);
        level.sendParticles(ParticleTypes.LAVA, c.x, c.y + 0.3, c.z, 14, 1.0, 0.3, 1.0, 0.0);
        Entity owner = marker.ownerId() == null ? null : level.getEntity(marker.ownerId());
        DamageSource source = new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolderOrThrow(DAMAGE_TYPE), marker, owner);
        double radius = marker.radius();
        List<LivingEntity> victims = level.getEntitiesOfClass(LivingEntity.class, new AABB(pos).inflate(radius + 1.0));
        for (LivingEntity victim : victims) {
            if (victim == owner || victim.isSpectator() || victim.getType().is(IMMUNE)) {
                continue;
            }
            double distance = victim.position().distanceTo(c);
            if (distance > radius) {
                continue;
            }
            float amount = damageAt(marker.damage(), distance, radius);
            if (victim instanceof ServerPlayer player) {
                amount *= 1.0F - (float) HazardProtection.reduction(player, HazardProtection.Hazard.STARFALL);
            }
            if (amount > 0.0F && victim.hurt(source, amount)) {
                victim.setSecondsOnFire(2);
            }
        }
    }
}
