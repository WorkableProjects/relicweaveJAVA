package com.relicweave.gametest;

import com.mojang.authlib.GameProfile;
import com.relicweave.RelicWeave;
import com.relicweave.vestige.hazard.HazardProtection;
import com.relicweave.vestige.hazard.RiftRegistry;
import com.relicweave.vestige.hazard.rift.RiftSaturation;
import com.relicweave.vestige.hazard.rift.WardSalveItem;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.UUID;

/** Rift Saturation maths, protection, debuff, Ward Salve and the fissure point of interest. */
@GameTestHolder(RelicWeave.MOD_ID)
@PrefixGameTestTemplate(false)
public final class RiftGameTests {
    private static final String TEMPLATE = "platform";
    private static final String PROT_A = "relicweave_test_protection_a";
    private static final String PROT_B = "relicweave_test_protection_b";
    private static boolean testContributorsRegistered;

    private RiftGameTests() {}

    private static boolean near(float a, double b) {
        return Math.abs(a - b) < 1.0E-4;
    }

    @GameTest(template = TEMPLATE)
    public static void riftStepGainsDecaysAndClamps(GameTestHelper helper) {
        helper.assertTrue(near(RiftSaturation.step(10, true, false, 0), 11.0), "Exposed gains 1 per update (2/s)");
        helper.assertTrue(near(RiftSaturation.step(10, false, false, 0), 8.5), "Unexposed decays 1.5 per update (3/s)");
        helper.assertTrue(near(RiftSaturation.step(0.5F, false, false, 0), 0.0), "Decay clamps at 0");
        helper.assertTrue(near(RiftSaturation.step(99.5F, true, false, 0), 100.0), "Gain clamps at 100");
        helper.assertTrue(near(RiftSaturation.step(10, true, true, 0), 12.0), "Convergence doubles the gain");
        helper.assertTrue(near(RiftSaturation.step(10, false, true, 0), 8.5), "Convergence does not change decay");
        helper.assertTrue(near(RiftSaturation.step(10, true, false, 0.5), 10.5), "50% reduction halves the gain");
        helper.assertTrue(near(RiftSaturation.step(10, true, true, 0.5), 11.0), "Reduction applies on top of Convergence");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void wardedHalvesRiftGain(GameTestHelper helper) {
        ServerPlayer player = makePlayer(helper);
        helper.assertTrue(near((float) HazardProtection.reduction(player, HazardProtection.Hazard.RIFT_SATURATION), 0.0),
                "No protection without the effect");
        player.addEffect(new MobEffectInstance(RiftRegistry.WARDED.get(), 200));
        double reduction = HazardProtection.reduction(player, HazardProtection.Hazard.RIFT_SATURATION);
        helper.assertTrue(near((float) reduction, 0.5), "Warded gives 50%, got " + reduction);
        helper.assertTrue(near(RiftSaturation.step(0, true, false, reduction), 0.5), "Gain is halved");
        helper.assertTrue(near((float) HazardProtection.reduction(player, HazardProtection.Hazard.GRAVITY_SHEAR), 0.0),
                "Warded does not protect against other hazards");
        player.removeAllEffects();
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void hazardProtectionIsCapped(GameTestHelper helper) {
        if (!testContributorsRegistered) {
            // Keyed on player data, so these never affect anyone else (HazardProtection has no unregister).
            HazardProtection.register(HazardProtection.Hazard.HUSHFIELD, p -> p.getPersistentData().getDouble(PROT_A));
            HazardProtection.register(HazardProtection.Hazard.HUSHFIELD, p -> p.getPersistentData().getDouble(PROT_B));
            testContributorsRegistered = true;
        }
        ServerPlayer player = makePlayer(helper);
        HazardProtection.Hazard hazard = HazardProtection.Hazard.HUSHFIELD;
        helper.assertTrue(near((float) HazardProtection.reduction(player, hazard), 0.0), "Nothing without tags");
        player.getPersistentData().putDouble(PROT_A, 0.6);
        helper.assertTrue(near((float) HazardProtection.reduction(player, hazard), 0.6), "One source");
        player.getPersistentData().putDouble(PROT_B, 0.6);
        double both = HazardProtection.reduction(player, hazard);
        helper.assertTrue(near((float) both, 0.8), "0.6 and 0.6 combine to 0.84 and are capped at 0.8, got " + both);
        helper.assertTrue(near((float) HazardProtection.apply(player, hazard, 10.0), 2.0), "apply uses the capped value");
        player.getPersistentData().remove(PROT_A);
        player.getPersistentData().remove(PROT_B);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void fullMeterAppliesSicknessAndDropsTo60(GameTestHelper helper) {
        ServerPlayer player = makePlayer(helper);
        RiftSaturation.setExposure(player, 0.0F);
        helper.assertTrue(near(RiftSaturation.getExposure(player), 0.0), "Starts empty");
        RiftSaturation.setExposure(player, 70.0F);
        helper.assertTrue(near(RiftSaturation.getExposure(player), 70.0), "Stored in persisted data");
        helper.assertTrue(near(RiftSaturation.update(player, true, false), 71.0), "One exposed update");
        helper.assertFalse(player.hasEffect(RiftRegistry.RIFT_SICKNESS.get()), "No sickness yet");
        RiftSaturation.setExposure(player, 99.5F);
        float after = RiftSaturation.update(player, true, false);
        helper.assertTrue(near(after, 60.0), "Meter drops to 60, got " + after);
        helper.assertTrue(near(RiftSaturation.getExposure(player), 60.0), "Stored value is 60");
        MobEffectInstance sickness = player.getEffect(RiftRegistry.RIFT_SICKNESS.get());
        helper.assertTrue(sickness != null && sickness.getDuration() > 0 && sickness.getDuration() <= 8 * 20,
                "Rift Sickness for the configured 8 s");
        helper.assertFalse(sickness.getEffect().isBeneficial(), "Sickness is harmful");
        player.removeAllEffects();
        RiftSaturation.setExposure(player, 0.0F);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void unexposedMeterDecays(GameTestHelper helper) {
        ServerPlayer player = makePlayer(helper);
        RiftSaturation.setExposure(player, 30.0F);
        helper.assertTrue(near(RiftSaturation.update(player, false, false), 28.5), "Decay of 1.5");
        RiftSaturation.setExposure(player, 0.0F);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void wardSalveGivesWardedAndReturnsBottle(GameTestHelper helper) {
        ServerPlayer player = makePlayer(helper);
        ItemStack single = new ItemStack(RiftRegistry.WARD_SALVE.get());
        helper.assertTrue(single.getMaxStackSize() == 16, "Stacks to 16");
        helper.assertTrue(single.getUseDuration() == 32, "32 tick drink");
        ItemStack result = single.getItem().finishUsingItem(single, helper.getLevel(), player);
        helper.assertTrue(result.is(Items.GLASS_BOTTLE), "A glass bottle is returned for the last salve");
        MobEffectInstance warded = player.getEffect(RiftRegistry.WARDED.get());
        helper.assertTrue(warded != null && warded.getDuration() == WardSalveItem.DURATION_TICKS, "Warded for 3:00");

        player.removeAllEffects();
        ItemStack stack = new ItemStack(RiftRegistry.WARD_SALVE.get(), 3);
        ItemStack rest = stack.getItem().finishUsingItem(stack, helper.getLevel(), player);
        helper.assertTrue(rest.getCount() == 2 && rest.is(RiftRegistry.WARD_SALVE.get()), "Two salves remain");
        helper.assertTrue(player.getInventory().contains(new ItemStack(Items.GLASS_BOTTLE)), "Bottle goes to the inventory");
        helper.assertTrue(player.hasEffect(RiftRegistry.WARDED.get()), "Warded again");
        player.removeAllEffects();
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void riftFissureIsAPointOfInterest(GameTestHelper helper) {
        BlockPos fissure = new BlockPos(2, 2, 2);
        helper.setBlock(fissure, RiftRegistry.RIFT_FISSURE.get());
        // The POI is added on the next server tick.
        helper.runAfterDelay(5, () -> {
            BlockPos near = helper.absolutePos(new BlockPos(2, 2, 4));
            helper.assertTrue(RiftSaturation.nearFissure(helper.getLevel(), near), "Fissure within 5 blocks is found");
            helper.assertFalse(RiftSaturation.nearFissure(helper.getLevel(), helper.absolutePos(new BlockPos(2, 2, 2)).offset(0, 0, 40)),
                    "Fissure 40 blocks away is not");
            helper.setBlock(fissure, net.minecraft.world.level.block.Blocks.AIR);
            helper.succeed();
        });
    }

    private static ServerPlayer makePlayer(GameTestHelper helper) {
        ServerPlayer player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "relicweave-rift-test"));
        player.setGameMode(GameType.SURVIVAL);
        Vec3 pos = helper.absoluteVec(new Vec3(2.5, 1.0, 2.5));
        player.moveTo(pos.x, pos.y, pos.z, 0.0F, 0.0F);
        return player;
    }
}
