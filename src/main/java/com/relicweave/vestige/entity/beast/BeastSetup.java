package com.relicweave.vestige.entity.beast;

import com.relicweave.RelicWeave;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.SpawnPlacementRegisterEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Mod-bus wiring for the beast mobs: attributes and natural-spawn placements. */
@Mod.EventBusSubscriber(modid = RelicWeave.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class BeastSetup {
    private BeastSetup() {}

    @SubscribeEvent
    public static void onAttributes(EntityAttributeCreationEvent event) {
        event.put(BeastMobs.GLASSROOT_STALKER.get(), GlassrootStalker.createAttributes().build());
        event.put(BeastMobs.RUIN_HOUND.get(), RuinHound.createAttributes().build());
        event.put(BeastMobs.AETHER_GRAZER.get(), AetherGrazer.createAttributes().build());
        event.put(BeastMobs.SHARDBACK_BRUTE.get(), ShardbackBrute.createAttributes().build());
        event.put(BeastMobs.STARFALL_STRIDER.get(), StarfallStrider.createAttributes().build());
        event.put(BeastMobs.GRAVITY_LEECH.get(), GravityLeech.createAttributes().build());
        event.put(BeastMobs.ARCHIVE_MIMIC.get(), ArchiveMimic.createAttributes().build());
    }

    @SubscribeEvent
    public static void onSpawnPlacements(SpawnPlacementRegisterEvent event) {
        // Vestige natives ignore light level. The Archive Mimic is structure-only and has no natural placement.
        hostile(event, BeastMobs.GLASSROOT_STALKER.get());
        hostile(event, BeastMobs.RUIN_HOUND.get());
        hostile(event, BeastMobs.SHARDBACK_BRUTE.get());
        hostile(event, BeastMobs.STARFALL_STRIDER.get());
        hostile(event, BeastMobs.GRAVITY_LEECH.get());
        event.register(BeastMobs.AETHER_GRAZER.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Mob::checkMobSpawnRules, SpawnPlacementRegisterEvent.Operation.REPLACE);
    }

    private static <T extends Monster> void hostile(SpawnPlacementRegisterEvent event, net.minecraft.world.entity.EntityType<T> type) {
        event.register(type, SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Monster::checkAnyLightMonsterSpawnRules, SpawnPlacementRegisterEvent.Operation.REPLACE);
    }
}
