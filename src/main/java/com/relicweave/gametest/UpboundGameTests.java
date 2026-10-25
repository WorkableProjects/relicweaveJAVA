package com.relicweave.gametest;

import com.mojang.authlib.GameProfile;
import com.relicweave.RelicWeave;
import com.relicweave.enchantment.upbound.UpboundHandler;
import com.relicweave.registry.ModEnchantments;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.UUID;

/** Server-side tests for Upbound air-jump validation. */
@GameTestHolder(RelicWeave.MOD_ID)
@PrefixGameTestTemplate(false)
public final class UpboundGameTests {
    private static final String TEMPLATE = "platform";

    private UpboundGameTests() {}

    private static ServerPlayer airbornePlayer(GameTestHelper helper, int upboundLevel) {
        ServerPlayer player = FakePlayerFactory.get(helper.getLevel(),
                new GameProfile(UUID.randomUUID(), "relicweave-upbound"));
        Vec3 pos = helper.absoluteVec(new Vec3(2.5, 4, 2.5));
        player.moveTo(pos.x, pos.y, pos.z);
        player.setOnGround(false);
        if (upboundLevel > 0) {
            ItemStack boots = new ItemStack(Items.IRON_BOOTS);
            boots.enchant(ModEnchantments.UPBOUND.get(), upboundLevel);
            player.setItemSlot(EquipmentSlot.FEET, boots);
        }
        return player;
    }

    @GameTest(template = TEMPLATE)
    public static void upboundRejectedWithoutBoots(GameTestHelper helper) {
        ServerPlayer player = airbornePlayer(helper, 0);
        helper.assertTrue(!UpboundHandler.tryAirJump(player), "Air jump accepted without Upbound boots");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void upboundLevelOneAllowsOneJumpAndResetsFall(GameTestHelper helper) {
        ServerPlayer player = airbornePlayer(helper, 1);
        player.fallDistance = 10;
        helper.assertTrue(UpboundHandler.tryAirJump(player), "First air jump should be accepted");
        helper.assertTrue(player.fallDistance == 0, "Fall distance should be reset, was " + player.fallDistance);
        helper.assertTrue(!UpboundHandler.tryAirJump(player), "Second air jump should be rejected at level 1");
        UpboundHandler.onLanded(player);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void upboundLevelTwoAllowsTwoJumps(GameTestHelper helper) {
        ServerPlayer player = airbornePlayer(helper, 2);
        helper.assertTrue(UpboundHandler.tryAirJump(player), "First air jump should be accepted");
        helper.assertTrue(UpboundHandler.tryAirJump(player), "Second air jump should be accepted");
        helper.assertTrue(!UpboundHandler.tryAirJump(player), "Third air jump should be rejected at level 2");
        UpboundHandler.onLanded(player);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void upboundBudgetResetsOnLanding(GameTestHelper helper) {
        ServerPlayer player = airbornePlayer(helper, 1);
        helper.assertTrue(UpboundHandler.tryAirJump(player), "First air jump should be accepted");
        helper.assertTrue(!UpboundHandler.tryAirJump(player), "Budget should be exhausted");
        UpboundHandler.onLanded(player);
        helper.assertTrue(UpboundHandler.tryAirJump(player), "Budget should reset after landing");
        UpboundHandler.onLanded(player);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void upboundRejectedOnGround(GameTestHelper helper) {
        ServerPlayer player = airbornePlayer(helper, 2);
        player.setOnGround(true);
        helper.assertTrue(!UpboundHandler.tryAirJump(player), "Air jump accepted while on ground");
        helper.succeed();
    }
}
