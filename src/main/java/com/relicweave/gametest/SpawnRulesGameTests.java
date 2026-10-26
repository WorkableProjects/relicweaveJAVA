package com.relicweave.gametest;

import com.relicweave.RelicWeave;
import com.relicweave.vestige.entity.VestigeSpawnRules;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** The single natural-spawn gate for Vestige mobs ({@link VestigeSpawnRules}). */
@GameTestHolder(RelicWeave.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SpawnRulesGameTests {
    private static final String TEMPLATE = "platform";

    private SpawnRulesGameTests() {}

    @GameTest(template = TEMPLATE)
    public static void defaultsAllowSpawnsBelowTheCap(GameTestHelper helper) {
        helper.assertFalse(VestigeSpawnRules.shouldRefuse(false, true, 0, 1.0, 1.0, 14, 0.99), "Full rate, empty area");
        helper.assertFalse(VestigeSpawnRules.shouldRefuse(false, true, 13, 1.0, 1.0, 14, 0.5), "One below the cap");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void localCapRefusesHostilesOnly(GameTestHelper helper) {
        helper.assertTrue(VestigeSpawnRules.shouldRefuse(false, true, 14, 1.0, 1.0, 14, 0.0), "Hostile at the cap");
        helper.assertTrue(VestigeSpawnRules.shouldRefuse(true, true, 40, 1.0, 1.0, 14, 0.0), "Elite above the cap");
        helper.assertFalse(VestigeSpawnRules.shouldRefuse(false, false, 40, 1.0, 1.0, 14, 0.0), "Creatures ignore the hostile cap");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void spawnRatesThinSpawns(GameTestHelper helper) {
        helper.assertTrue(VestigeSpawnRules.shouldRefuse(false, true, 0, 0.0, 1.0, 14, 0.0), "Rate 0 refuses everything");
        helper.assertTrue(VestigeSpawnRules.shouldRefuse(false, false, 0, 0.5, 1.0, 14, 0.5), "Roll at the rate is refused");
        helper.assertFalse(VestigeSpawnRules.shouldRefuse(false, false, 0, 0.5, 1.0, 14, 0.49), "Roll under the rate passes");
        helper.assertTrue(VestigeSpawnRules.shouldRefuse(true, true, 0, 1.0, 0.25, 14, 0.3), "Elite rate thins elites");
        helper.assertFalse(VestigeSpawnRules.shouldRefuse(false, true, 0, 1.0, 0.25, 14, 0.3), "Elite rate ignores non-elites");
        helper.assertFalse(VestigeSpawnRules.shouldRefuse(true, true, 0, 1.0, 4.0, 14, 0.99), "Elite rate above 1 never refuses");
        helper.succeed();
    }

    /** Non-Vestige mobs (here a vanilla zombie) are never touched, even in a crowd. */
    @GameTest(template = TEMPLATE)
    public static void vanillaMobsAreNeverRefused(GameTestHelper helper) {
        Zombie zombie = null;
        for (int i = 0; i < 20; i++) {
            zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, 2, 2, 2);
        }
        helper.assertFalse(VestigeSpawnRules.refuse(helper.getLevel(), zombie), "Vanilla zombie refused");
        helper.assertTrue(VestigeSpawnRules.isNatural(MobSpawnType.NATURAL), "NATURAL is natural");
        helper.assertTrue(VestigeSpawnRules.isNatural(MobSpawnType.CHUNK_GENERATION), "Chunk generation is natural");
        helper.assertFalse(VestigeSpawnRules.isNatural(MobSpawnType.SPAWN_EGG), "Spawn eggs are not natural");
        helper.assertFalse(VestigeSpawnRules.isNatural(MobSpawnType.STRUCTURE), "Structure mobs are not natural");
        helper.succeed();
    }
}
