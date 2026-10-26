package com.relicweave.vestige.entity.ethereal;

import com.relicweave.RelicWeave;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.SpawnPlacementRegisterEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** MOD-bus wiring for the T08 mobs: attributes and natural spawn placements. */
@Mod.EventBusSubscriber(modid = RelicWeave.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class EtherealSetup {
    private EtherealSetup() {}

    @SubscribeEvent
    public static void onAttributes(EntityAttributeCreationEvent event) {
        event.put(EtherealMobs.FRACTURE_WISP.get(), Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 14.0).add(Attributes.ATTACK_DAMAGE, 4.0)
                .add(Attributes.FLYING_SPEED, 0.6).add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.FOLLOW_RANGE, 20.0).build());
        event.put(EtherealMobs.NULLWING.get(), Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 24.0).add(Attributes.ATTACK_DAMAGE, 6.0)
                .add(Attributes.FLYING_SPEED, 0.5).add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.FOLLOW_RANGE, 16.0).build());
        event.put(EtherealMobs.ECLIPSE_MOTH.get(), Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 6.0).add(Attributes.FLYING_SPEED, 0.4)
                .add(Attributes.MOVEMENT_SPEED, 0.2).build());
        event.put(EtherealMobs.CONCORDANCE_GUARDIAN.get(), miniBoss(220.0, 10.0, 10.0, 0.25).build());
        event.put(EtherealMobs.SEALED_CUSTODIAN.get(), miniBoss(160.0, 8.0, 9.0, 0.24).build());
        event.put(EtherealMobs.RUNE_WARDEN.get(), miniBoss(180.0, 8.0, 11.0, 0.26).build());
        event.put(EtherealMobs.CYCLE_HERALD.get(), Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 60.0).add(Attributes.ATTACK_DAMAGE, 8.0)
                .add(Attributes.FLYING_SPEED, 0.3).add(Attributes.MOVEMENT_SPEED, 0.2)
                .add(Attributes.FOLLOW_RANGE, 32.0).build());
    }

    private static AttributeSupplier.Builder miniBoss(double health, double armor, double damage, double speed) {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, health).add(Attributes.ARMOR, armor)
                .add(Attributes.ATTACK_DAMAGE, damage).add(Attributes.MOVEMENT_SPEED, speed)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0).add(Attributes.FOLLOW_RANGE, 32.0);
    }

    @SubscribeEvent
    public static void onSpawnPlacements(SpawnPlacementRegisterEvent event) {
        event.register(EtherealMobs.FRACTURE_WISP.get(), SpawnPlacements.Type.NO_RESTRICTIONS, Heightmap.Types.MOTION_BLOCKING,
                Monster::checkAnyLightMonsterSpawnRules, SpawnPlacementRegisterEvent.Operation.OR);
        event.register(EtherealMobs.NULLWING.get(), SpawnPlacements.Type.NO_RESTRICTIONS, Heightmap.Types.MOTION_BLOCKING,
                Monster::checkAnyLightMonsterSpawnRules, SpawnPlacementRegisterEvent.Operation.OR);
        event.register(EtherealMobs.ECLIPSE_MOTH.get(), SpawnPlacements.Type.NO_RESTRICTIONS, Heightmap.Types.MOTION_BLOCKING,
                Mob::checkMobSpawnRules, SpawnPlacementRegisterEvent.Operation.OR);
    }
}
