package com.relicweave.gametest;

import com.mojang.authlib.GameProfile;
import com.relicweave.RelicWeave;
import com.relicweave.vestige.hazard.FieldRegistry;
import com.relicweave.vestige.hazard.field.GravityShear;
import com.relicweave.vestige.hazard.field.HushNoise;
import com.relicweave.vestige.hazard.field.NullPockets;
import com.relicweave.vestige.hazard.field.StarfallImpacts;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.UUID;

/** Starfall markers, Gravity Shear pulses and anchors, Null Pockets and Hushfield noise arithmetic. */
@GameTestHolder(RelicWeave.MOD_ID)
@PrefixGameTestTemplate(false)
public final class FieldHazardGameTests {
    private static final String TEMPLATE = "platform";
    private static final int WARNING = 40;

    private FieldHazardGameTests() {}

    private static boolean near(double a, double b) {
        return Math.abs(a - b) < 1.0E-4;
    }

    private static ServerPlayer makePlayer(GameTestHelper helper, Vec3 relative) {
        ServerPlayer player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "relicweave-field-test"));
        player.setGameMode(GameType.SURVIVAL);
        Vec3 pos = helper.absoluteVec(relative);
        player.moveTo(pos.x, pos.y, pos.z, 0.0F, 0.0F);
        player.setDeltaMovement(Vec3.ZERO);
        player.setShiftKeyDown(false);
        player.removeAllEffects();
        return player;
    }

    private static Zombie helmeted(GameTestHelper helper, double x, double y, double z) {
        Zombie zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, (float) x, (float) y, (float) z);
        // A helmet stops daylight burning, so only the Starfall can hurt it.
        zombie.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
        return zombie;
    }

    // ---- Starfall ----

    /**
     * The test site is underground, so the real sky is never visible. Install a cover check that only looks at the
     * eight blocks above the marker: open air counts as sky, a roof counts as cover.
     */
    private static void useLocalSkyCheck() {
        StarfallImpacts.setSkyCheck((level, pos) -> {
            for (int dy = 1; dy <= 8; dy++) {
                if (level.getBlockState(pos.above(dy)).blocksMotion()) {
                    return false;
                }
            }
            return true;
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void starfallDamagesNearNotFar(GameTestHelper helper) {
        useLocalSkyCheck();
        Zombie near = helmeted(helper, 1.5, 2.0, 1.5);
        Zombie far = helmeted(helper, 4.5, 2.0, 4.5);
        float nearBefore = near.getHealth();
        float farBefore = far.getHealth();
        StarfallImpacts.spawnMarker(helper.getLevel(), helper.absoluteVec(new Vec3(1.5, 2.0, 1.5)), WARNING, 6.0F, null);
        helper.runAfterDelay(WARNING - 5, () -> helper.assertTrue(near.getHealth() == nearBefore, "No damage before the warning ends"));
        helper.runAfterDelay(WARNING + 5, () -> {
            helper.assertTrue(near.getHealth() < nearBefore, "Zombie inside the ring is hurt, health " + near.getHealth());
            // Other parallel tests can nick the far zombie, so check the starfall never hit it rather than raw health.
            helper.assertTrue(far.getLastDamageSource() == null || !far.getLastDamageSource().is(StarfallImpacts.DAMAGE_TYPE),
                    "Zombie outside the ring was hit by the starfall, health " + far.getHealth() + " (was " + farBefore + ")");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void starfallFizzlesUnderRoof(GameTestHelper helper) {
        useLocalSkyCheck();
        for (int x = 0; x < 5; x++) {
            for (int z = 0; z < 5; z++) {
                helper.setBlock(new BlockPos(x, 4, z), Blocks.STONE);
            }
        }
        Zombie zombie = helmeted(helper, 2.5, 2.0, 2.5);
        float before = zombie.getHealth();
        StarfallImpacts.spawnMarker(helper.getLevel(), helper.absoluteVec(new Vec3(2.5, 2.0, 2.5)), WARNING, 6.0F, null);
        helper.runAfterDelay(WARNING + 5, () -> {
            helper.assertTrue(zombie.getHealth() == before, "A covered impact fizzles without damage, health " + zombie.getHealth());
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE)
    public static void starfallDamageFalloff(GameTestHelper helper) {
        helper.assertTrue(near(StarfallImpacts.damageAt(6.0F, 0.0, 2.5), 6.0), "Full damage at the centre");
        helper.assertTrue(near(StarfallImpacts.damageAt(6.0F, 2.5, 2.5), 3.0), "Half damage at the edge");
        helper.assertTrue(near(StarfallImpacts.damageAt(6.0F, 1.25, 2.5), 4.5), "Linear in between");
        helper.succeed();
    }

    // ---- Gravity Shear ----

    @GameTest(template = TEMPLATE)
    public static void gravityPulseLiftsAndCrouchHalves(GameTestHelper helper) {
        ServerPlayer player = makePlayer(helper, new Vec3(2.5, 2.0, 2.5));
        GravityShear.pulse(player, Direction.UP, 0.6);
        helper.assertTrue(near(player.getDeltaMovement().y, 0.6), "Upward velocity equals the strength, got " + player.getDeltaMovement().y);
        helper.assertTrue(player.hasEffect(MobEffects.SLOW_FALLING), "Slow Falling makes the fall harmless");

        player.setDeltaMovement(Vec3.ZERO);
        player.setShiftKeyDown(true);
        GravityShear.pulse(player, Direction.UP, 0.6);
        helper.assertTrue(near(player.getDeltaMovement().y, 0.3), "Bracing halves the pulse, got " + player.getDeltaMovement().y);

        player.setDeltaMovement(Vec3.ZERO);
        player.setShiftKeyDown(false);
        GravityShear.pulse(player, Direction.EAST, 1.0);
        Vec3 motion = player.getDeltaMovement();
        helper.assertTrue(near(motion.x, 0.7) && near(motion.y, 0.25) && near(motion.z, 0.0), "Sideways pulse vector, got " + motion);
        player.setShiftKeyDown(false);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void gravityAnchorMakesPlayerAnchored(GameTestHelper helper) {
        ServerPlayer player = makePlayer(helper, new Vec3(0.5, 2.0, 0.5));
        helper.assertTrue(!GravityShear.isAnchored(player), "No anchor, not anchored");
        helper.setBlock(new BlockPos(4, 2, 4), FieldRegistry.GRAVITY_ANCHOR.get());
        helper.assertTrue(!GravityShear.isAnchored(player), "An anchor 5 blocks away does not count");
        helper.setBlock(new BlockPos(1, 2, 0), FieldRegistry.GRAVITY_ANCHOR.get());
        helper.assertTrue(GravityShear.isAnchored(player), "An anchor within 3 blocks anchors the player");
        helper.succeed();
    }

    // ---- Null Pockets ----

    @GameTest(template = TEMPLATE)
    public static void nullWardSuppressesNearby(GameTestHelper helper) {
        helper.setBlock(new BlockPos(2, 2, 2), FieldRegistry.NULL_WARD.get());
        ServerPlayer player = makePlayer(helper, new Vec3(2.5, 2.0, 3.5));
        helper.assertTrue(NullPockets.isSuppressedIgnoringDimension(player), "Next to a null ward");
        helper.assertTrue(!NullPockets.isSuppressed(player), "Outside The Vestige nothing is suppressed");

        Vec3 far = helper.absoluteVec(new Vec3(2.5, 2.0, 10.5));
        player.moveTo(far.x, far.y, far.z);
        helper.assertTrue(!NullPockets.isSuppressedIgnoringDimension(player), "8 blocks away is outside the pocket");

        Vec3 above = helper.absoluteVec(new Vec3(2.5, 8.0, 2.5));
        player.moveTo(above.x, above.y, above.z);
        helper.assertTrue(!NullPockets.isSuppressedIgnoringDimension(player), "6 blocks above is outside the pocket");
        helper.succeed();
    }

    // ---- Hushfield ----

    @GameTest(template = TEMPLATE)
    public static void hushNoiseArithmetic(GameTestHelper helper) {
        helper.assertTrue(near(HushNoise.step(10, false, false, 0), 7.5), "Decay is 2.5 per step");
        helper.assertTrue(near(HushNoise.step(10, true, false, 0), 9.5), "Sprinting gains 2 then decays");
        helper.assertTrue(near(HushNoise.step(10, true, true, 0), 8.5), "Crouching halves the sprint gain");
        helper.assertTrue(near(HushNoise.step(1, false, false, 0), 0.0), "Decay clamps at 0");
        helper.assertTrue(near(HushNoise.gain(0, HushNoise.ATTACK_GAIN, false, 0), 15.0), "A melee attack adds 15");
        helper.assertTrue(near(HushNoise.gain(0, HushNoise.ATTACK_GAIN, true, 0), 7.5), "Crouching halves event gains");
        helper.assertTrue(near(HushNoise.gain(0, HushNoise.ATTACK_GAIN, false, 0.5), 7.5), "Protection scales gains");
        helper.assertTrue(near(HushNoise.gain(95, HushNoise.BREAK_GAIN, false, 0), 100.0), "Gain clamps at 100");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void hushNoiseApiDefaults(GameTestHelper helper) {
        ServerPlayer player = makePlayer(helper, new Vec3(2.5, 2.0, 2.5));
        helper.assertTrue(near(HushNoise.level(player), 0.0), "No noise outside a Hushfield");
        helper.assertTrue(!HushNoise.isLoud(player), "Not loud");
        helper.assertTrue(HushNoise.loudestPlayerNear(helper.getLevel(), player.position(), 32.0) == null, "Nobody is making noise");
        helper.succeed();
    }
}
