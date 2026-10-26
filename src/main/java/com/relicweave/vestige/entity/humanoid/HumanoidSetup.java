package com.relicweave.vestige.entity.humanoid;

import com.relicweave.RelicWeave;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.SpawnPlacementRegisterEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Mod-bus setup for the humanoid mobs: attributes and natural spawn placements. */
@Mod.EventBusSubscriber(modid = RelicWeave.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class HumanoidSetup {
    private HumanoidSetup() {}

    @SubscribeEvent
    public static void onAttributes(EntityAttributeCreationEvent event) {
        event.put(HumanoidMobs.RELIQUARY_SENTINEL.get(), ReliquarySentinel.attributes().build());
        event.put(HumanoidMobs.SCRIPTBOUND_SCRIBE.get(), ScriptboundScribe.attributes().build());
        event.put(HumanoidMobs.GILDED_HUSK.get(), GildedHusk.attributes().build());
        event.put(HumanoidMobs.MIREBOUND_CANTOR.get(), MireboundCantor.attributes().build());
        event.put(HumanoidMobs.OATHBREAKER_KNIGHT.get(), OathbreakerKnight.attributes().build());
        event.put(HumanoidMobs.VESTIGE_DRIFTER.get(), VestigeDrifter.attributes().build());
        event.put(HumanoidMobs.RIFT_ARCHER.get(), RiftArcher.attributes().build());
        event.put(HumanoidMobs.ECHO_WEAVER.get(), EchoWeaver.attributes().build());
    }

    @SubscribeEvent
    public static void onPlacements(SpawnPlacementRegisterEvent event) {
        monster(event, HumanoidMobs.RELIQUARY_SENTINEL.get());
        monster(event, HumanoidMobs.SCRIPTBOUND_SCRIBE.get());
        monster(event, HumanoidMobs.GILDED_HUSK.get());
        monster(event, HumanoidMobs.MIREBOUND_CANTOR.get());
        monster(event, HumanoidMobs.OATHBREAKER_KNIGHT.get());
        monster(event, HumanoidMobs.RIFT_ARCHER.get());
        monster(event, HumanoidMobs.ECHO_WEAVER.get());
        event.register(HumanoidMobs.VESTIGE_DRIFTER.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Mob::checkMobSpawnRules, SpawnPlacementRegisterEvent.Operation.OR);
    }

    private static <T extends Monster> void monster(SpawnPlacementRegisterEvent event, net.minecraft.world.entity.EntityType<T> type) {
        event.register(type, SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Monster::checkAnyLightMonsterSpawnRules, SpawnPlacementRegisterEvent.Operation.OR);
    }
}
