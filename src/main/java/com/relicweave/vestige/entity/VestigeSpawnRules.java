package com.relicweave.vestige.entity;

import com.relicweave.RelicWeave;
import com.relicweave.vestige.VestigeConfig;
import com.relicweave.vestige.VestigeKeys;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * One auditable gate for natural Vestige spawns (every entity type in {@code #relicweave:vestige_mobs}): applies
 * {@code naturalSpawnRate}, {@code eliteSpawnRate} (for {@code #relicweave:vestige_elites}) and the local hostile cap
 * ({@code localHostileCap} Vestige hostiles within {@link #CAP_RADIUS} blocks). Structure-placed, spawn-egg, summoned
 * and boss-spawned mobs are never affected. Biome spawn weights and group sizes live in the
 * {@code data/relicweave/forge/biome_modifier/spawns_*.json} files.
 */
@Mod.EventBusSubscriber(modid = RelicWeave.MOD_ID)
public final class VestigeSpawnRules {
    public static final double CAP_RADIUS = 48.0;

    private VestigeSpawnRules() {}

    public static boolean isNatural(MobSpawnType type) {
        return type == MobSpawnType.NATURAL || type == MobSpawnType.CHUNK_GENERATION;
    }

    /** Vestige hostiles (tagged vestige_mobs and {@link Enemy}) within {@link #CAP_RADIUS} of {@code mob}. */
    public static int nearbyHostiles(ServerLevel level, Mob mob) {
        return level.getEntitiesOfClass(Mob.class, mob.getBoundingBox().inflate(CAP_RADIUS),
                m -> m != mob && m.isAlive() && m instanceof Enemy && m.getType().is(VestigeKeys.VESTIGE_MOBS)).size();
    }

    /** True if a natural spawn of {@code mob} should be refused. */
    public static boolean refuse(ServerLevel level, Mob mob) {
        if (!mob.getType().is(VestigeKeys.VESTIGE_MOBS)) {
            return false;
        }
        double rate = VestigeConfig.SPEC.isLoaded() ? VestigeConfig.NATURAL_SPAWN_RATE.get() : 1.0;
        if (mob.getType().is(VestigeKeys.VESTIGE_ELITES)) {
            rate *= Math.min(1.0, VestigeConfig.SPEC.isLoaded() ? VestigeConfig.ELITE_SPAWN_RATE.get() : 1.0);
        }
        if (rate < 1.0 && level.random.nextDouble() >= rate) {
            return true;
        }
        int cap = VestigeConfig.SPEC.isLoaded() ? VestigeConfig.LOCAL_HOSTILE_CAP.get() : 14;
        return mob instanceof Enemy && nearbyHostiles(level, mob) >= cap;
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onFinalizeSpawn(MobSpawnEvent.FinalizeSpawn event) {
        if (isNatural(event.getSpawnType()) && refuse(event.getLevel().getLevel(), event.getEntity())) {
            event.setSpawnCancelled(true);
            event.setCanceled(true);
        }
    }
}
