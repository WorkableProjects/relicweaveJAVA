package com.relicweave.gametest;

import com.mojang.authlib.GameProfile;
import com.relicweave.RelicWeave;
import com.relicweave.block.infuser.CelestialInfuserBlockEntity;
import com.relicweave.infusion.InfusionType;
import com.relicweave.infusion.Infusions;
import com.relicweave.infusion.effects.AnchoredEffect;
import com.relicweave.infusion.effects.PursuitEffect;
import com.relicweave.infusion.effects.TidalEffect;
import com.relicweave.infusion.effects.WardingEffect;
import com.relicweave.registry.ModBlocks;
import com.relicweave.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingKnockBackEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.UUID;

/** Server-side tests for the four 0.0.3 effect infusions. Run with {@code ./gradlew runGameTestServer}. */
@GameTestHolder(RelicWeave.MOD_ID)
@PrefixGameTestTemplate(false)
public final class AttunementInfusionGameTests {
    private static final String TEMPLATE = "platform";
    private static final BlockPos INFUSER_POS = new BlockPos(2, 1, 2);
    private static final int PROCESS_TICKS = 200;

    private AttunementInfusionGameTests() {}

    // --- recipes end-to-end -------------------------------------------------------------------------------------

    @GameTest(template = TEMPLATE, timeoutTicks = 320)
    public static void tidalInfusesHelmet(GameTestHelper helper) {
        infuses(helper, ModItems.CELESTIAL_HELMET.get(), new ItemStack(Items.PRISMARINE_SHARD, 16), InfusionType.TIDAL);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 320)
    public static void wardingInfusesChestplate(GameTestHelper helper) {
        infuses(helper, ModItems.CELESTIAL_CHESTPLATE.get(), new ItemStack(Items.ECHO_SHARD, 4), InfusionType.WARDING);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 320)
    public static void pursuitInfusesLeggings(GameTestHelper helper) {
        infuses(helper, ModItems.CELESTIAL_LEGGINGS.get(), new ItemStack(Items.PHANTOM_MEMBRANE, 8), InfusionType.PURSUIT);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 320)
    public static void anchoredInfusesBootsFromCopper(GameTestHelper helper) {
        infuses(helper, ModItems.CELESTIAL_BOOTS.get(), new ItemStack(Items.COPPER_INGOT, 32), InfusionType.ANCHORED);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 320)
    public static void anchoredInfusesBootsFromSaplings(GameTestHelper helper) {
        infuses(helper, ModItems.CELESTIAL_BOOTS.get(), new ItemStack(Items.OAK_SAPLING, 32), InfusionType.ANCHORED);
    }

    private static void infuses(GameTestHelper helper, Item armor, ItemStack material, InfusionType expected) {
        CelestialInfuserBlockEntity infuser = place(helper, new ItemStack(armor), material);
        helper.runAfterDelay(PROCESS_TICKS + 10, () -> {
            ItemStack output = infuser.getItems().getStackInSlot(2);
            helper.assertTrue(output.is(armor) && output.getCount() == 1, "Expected one infused piece, found " + output);
            helper.assertTrue(Infusions.get(output).equals(java.util.List.of(expected)), "Wrong infusions: " + Infusions.get(output));
            helper.assertTrue(infuser.getItems().getStackInSlot(0).isEmpty(), "Armor should be consumed");
            helper.assertTrue(infuser.getItems().getStackInSlot(1).isEmpty(), "Material should be consumed");
            helper.succeed();
        });
    }

    // --- slot restriction and limit -----------------------------------------------------------------------------

    @GameTest(template = TEMPLATE)
    public static void effectInfusionsOnlyFitTheirPiece(GameTestHelper helper) {
        for (InfusionType type : InfusionType.values()) {
            if (type.hasAttribute()) {
                continue;
            }
            for (Item piece : new Item[]{ModItems.CELESTIAL_HELMET.get(), ModItems.CELESTIAL_CHESTPLATE.get(),
                    ModItems.CELESTIAL_LEGGINGS.get(), ModItems.CELESTIAL_BOOTS.get()}) {
                ItemStack stack = new ItemStack(piece);
                boolean fits = ((net.minecraft.world.item.ArmorItem) piece).getEquipmentSlot() == type.slot();
                helper.assertTrue(Infusions.canInfuse(stack, type) == fits, type + " on " + piece + " should " + (fits ? "" : "not ") + "fit");
            }
        }
        helper.assertFalse(Infusions.canInfuse(new ItemStack(ModItems.CELESTIAL_BOOTS.get()), InfusionType.TIDAL), "Tidal must not fit boots");
        helper.assertFalse(Infusions.canInfuse(new ItemStack(ModItems.CELESTIAL_CHESTPLATE.get()), InfusionType.TIDAL), "Tidal must not fit chestplate");
        helper.assertFalse(Infusions.canInfuse(new ItemStack(ModItems.CELESTIAL_LEGGINGS.get()), InfusionType.TIDAL), "Tidal must not fit leggings");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 320)
    public static void bootsWithPrismarineMakeNoProgress(GameTestHelper helper) {
        CelestialInfuserBlockEntity infuser = place(helper, new ItemStack(ModItems.CELESTIAL_BOOTS.get()), new ItemStack(Items.PRISMARINE_SHARD, 16));
        helper.runAfterDelay(PROCESS_TICKS + 20, () -> {
            helper.assertTrue(infuser.getProgress() == 0, "Must not make progress for the wrong piece");
            helper.assertTrue(infuser.getItems().getStackInSlot(0).is(ModItems.CELESTIAL_BOOTS.get()), "Boots must stay in place");
            helper.assertTrue(infuser.getItems().getStackInSlot(1).getCount() == 16, "Material must stay in place");
            helper.assertTrue(infuser.getItems().getStackInSlot(2).isEmpty(), "Nothing should be produced");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE)
    public static void limitStillAppliesWithEffectInfusion(GameTestHelper helper) {
        ItemStack helmet = new ItemStack(ModItems.CELESTIAL_HELMET.get());
        helmet = Infusions.withInfusion(helmet, InfusionType.COBBLED);
        helmet = Infusions.withInfusion(helmet, InfusionType.OXIDIZED);
        helper.assertTrue(Infusions.canInfuse(helmet, InfusionType.TIDAL), "Third infusion should be accepted");
        helmet = Infusions.withInfusion(helmet, InfusionType.TIDAL);
        helper.assertTrue(Infusions.get(helmet).size() == 3, "Expected three infusions");
        helper.assertFalse(Infusions.canInfuse(helmet, InfusionType.CONDUCTIVE), "A fourth infusion must be refused");
        helper.assertTrue(Infusions.check(helmet, InfusionType.CONDUCTIVE) == Infusions.Result.LIMIT_REACHED, "Expected LIMIT_REACHED");
        helper.succeed();
    }

    // --- Tidal ---------------------------------------------------------------------------------------------------

    @GameTest(template = TEMPLATE)
    public static void tidalGrantsWaterBreathingOnlyUnderwater(GameTestHelper helper) {
        ServerPlayer player = makePlayer(helper);
        wear(player, EquipmentSlot.HEAD, ModItems.CELESTIAL_HELMET.get(), InfusionType.TIDAL);
        player.baseTick();
        TidalEffect.apply(player);
        helper.assertFalse(player.hasEffect(MobEffects.WATER_BREATHING), "No Water Breathing on land");

        for (int y = 1; y <= 3; y++) {
            helper.setBlock(new BlockPos(2, y, 2), Blocks.WATER);
        }
        player.baseTick();
        TidalEffect.apply(player);
        helper.assertTrue(player.hasEffect(MobEffects.WATER_BREATHING), "Water Breathing expected with eyes in water");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void tidalNeverShortensLongerWaterBreathing(GameTestHelper helper) {
        ServerPlayer player = makePlayer(helper);
        wear(player, EquipmentSlot.HEAD, ModItems.CELESTIAL_HELMET.get(), InfusionType.TIDAL);
        for (int y = 1; y <= 3; y++) {
            helper.setBlock(new BlockPos(2, y, 2), Blocks.WATER);
        }
        player.addEffect(new net.minecraft.world.effect.MobEffectInstance(MobEffects.WATER_BREATHING, 1000));
        player.baseTick();
        TidalEffect.apply(player);
        helper.assertTrue(player.getEffect(MobEffects.WATER_BREATHING).getDuration() > 900, "Longer effect must be kept");
        helper.succeed();
    }

    // --- Warding -------------------------------------------------------------------------------------------------

    @GameTest(template = TEMPLATE, timeoutTicks = 500)
    public static void wardingReducesOneHitAndRecharges(GameTestHelper helper) {
        ServerPlayer player = makePlayer(helper);
        wear(player, EquipmentSlot.CHEST, ModItems.CELESTIAL_CHESTPLATE.get(), InfusionType.WARDING);

        // A fresh wearer starts the timer: the first hit is not yet reduced.
        helper.assertTrue(hurt(player, 6.0F) == 6.0F, "Charge must not be ready right after putting the chestplate on");
        WardingEffect.charge(player);
        helper.assertTrue(hurt(player, 6.0F) == 6.0F - WardingEffect.WARD_REDUCTION, "First hit should be reduced by 4");
        helper.assertTrue(hurt(player, 6.0F) == 6.0F, "Second hit must not be reduced");
        helper.assertTrue(hurt(player, 3.0F) == 3.0F, "Third hit must not be reduced");

        helper.runAfterDelay(WardingEffect.WARD_RECHARGE_TICKS - 20, () -> {
            WardingEffect.refresh(player);
            helper.assertFalse(WardingEffect.isCharged(player), "Charge must not return early");
        });
        helper.runAfterDelay(WardingEffect.WARD_RECHARGE_TICKS + 5, () -> {
            WardingEffect.refresh(player);
            helper.assertTrue(WardingEffect.isCharged(player), "Charge should be back after 400 ticks");
            helper.assertTrue(hurt(player, 1.0F) == 0.0F, "Reduction must not go below zero");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE)
    public static void wardingIgnoresBypassDamageAndOtherWearers(GameTestHelper helper) {
        ServerPlayer player = makePlayer(helper);
        wear(player, EquipmentSlot.CHEST, ModItems.CELESTIAL_CHESTPLATE.get(), InfusionType.WARDING);
        WardingEffect.charge(player);
        LivingHurtEvent event = new LivingHurtEvent(player, player.damageSources().fellOutOfWorld(), 6.0F);
        MinecraftForge.EVENT_BUS.post(event);
        helper.assertTrue(event.getAmount() == 6.0F, "Void damage must not be reduced");
        helper.assertTrue(WardingEffect.isCharged(player), "Charge must be kept");

        player.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
        helper.assertTrue(hurt(player, 6.0F) == 6.0F, "No reduction without the infused chestplate");
        helper.succeed();
    }

    // --- Pursuit -------------------------------------------------------------------------------------------------

    @GameTest(template = TEMPLATE)
    public static void pursuitGivesSpeedAfterKillWithCooldown(GameTestHelper helper) {
        ServerPlayer player = makePlayer(helper);
        wear(player, EquipmentSlot.LEGS, ModItems.CELESTIAL_LEGGINGS.get(), InfusionType.PURSUIT);
        Zombie zombie = helper.spawn(EntityType.ZOMBIE, 1, 1, 1);

        MinecraftForge.EVENT_BUS.post(new LivingDeathEvent(zombie, player.damageSources().playerAttack(player)));
        helper.assertTrue(player.hasEffect(MobEffects.MOVEMENT_SPEED), "Speed expected after killing a zombie");
        helper.assertTrue(player.getEffect(MobEffects.MOVEMENT_SPEED).getAmplifier() == 1, "Expected Speed II");
        helper.assertTrue(player.getEffect(MobEffects.MOVEMENT_SPEED).getDuration() <= PursuitEffect.PURSUIT_DURATION_TICKS, "Burst too long");

        player.removeEffect(MobEffects.MOVEMENT_SPEED);
        MinecraftForge.EVENT_BUS.post(new LivingDeathEvent(zombie, player.damageSources().playerAttack(player)));
        helper.assertFalse(player.hasEffect(MobEffects.MOVEMENT_SPEED), "No new burst within the cooldown");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void pursuitNeedsInfusionAndHostileKill(GameTestHelper helper) {
        ServerPlayer player = makePlayer(helper);
        Zombie zombie = helper.spawn(EntityType.ZOMBIE, 1, 1, 1);
        MinecraftForge.EVENT_BUS.post(new LivingDeathEvent(zombie, player.damageSources().playerAttack(player)));
        helper.assertFalse(player.hasEffect(MobEffects.MOVEMENT_SPEED), "No burst without Pursuit");

        wear(player, EquipmentSlot.LEGS, ModItems.CELESTIAL_LEGGINGS.get(), InfusionType.PURSUIT);
        net.minecraft.world.entity.animal.Pig pig = helper.spawn(EntityType.PIG, 1, 1, 1);
        MinecraftForge.EVENT_BUS.post(new LivingDeathEvent(pig, player.damageSources().playerAttack(player)));
        helper.assertFalse(player.hasEffect(MobEffects.MOVEMENT_SPEED), "Passive mobs must not trigger a burst");
        helper.succeed();
    }

    // --- Anchored ------------------------------------------------------------------------------------------------

    @GameTest(template = TEMPLATE)
    public static void anchoredReducesKnockbackOnlyWhileCrouching(GameTestHelper helper) {
        ServerPlayer player = makePlayer(helper);
        wear(player, EquipmentSlot.FEET, ModItems.CELESTIAL_BOOTS.get(), InfusionType.ANCHORED);

        player.setPose(Pose.STANDING);
        helper.assertTrue(knockback(player, 0.8F) == 0.8F, "Standing wearer keeps full knockback");

        player.setPose(Pose.CROUCHING);
        float reduced = knockback(player, 0.8F);
        helper.assertTrue(Math.abs(reduced - 0.8F * AnchoredEffect.ANCHORED_KNOCKBACK_FACTOR) < 1.0E-6F && reduced > 0.0F,
                "Crouching wearer should take 25% knockback, got " + reduced);

        player.setItemSlot(EquipmentSlot.FEET, ItemStack.EMPTY);
        helper.assertTrue(knockback(player, 0.8F) == 0.8F, "No reduction without the infused boots");
        helper.succeed();
    }

    // --- helpers -------------------------------------------------------------------------------------------------

    /** Posts a hurt event and returns the resulting damage amount. */
    private static float hurt(ServerPlayer player, float amount) {
        LivingHurtEvent event = new LivingHurtEvent(player, player.damageSources().generic(), amount);
        MinecraftForge.EVENT_BUS.post(event);
        return event.getAmount();
    }

    private static float knockback(ServerPlayer player, float strength) {
        LivingKnockBackEvent event = new LivingKnockBackEvent(player, strength, 1.0, 0.0);
        MinecraftForge.EVENT_BUS.post(event);
        return event.getStrength();
    }

    private static void wear(ServerPlayer player, EquipmentSlot slot, Item armor, InfusionType infusion) {
        player.setItemSlot(slot, Infusions.withInfusion(new ItemStack(armor), infusion));
    }

    private static ServerPlayer makePlayer(GameTestHelper helper) {
        ServerPlayer player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "relicweave-infusion-test"));
        player.setGameMode(GameType.SURVIVAL);
        Vec3 pos = helper.absoluteVec(new Vec3(2.5, 1.0, 2.5));
        player.moveTo(pos.x, pos.y, pos.z, 0.0F, 0.0F);
        return player;
    }

    private static CelestialInfuserBlockEntity place(GameTestHelper helper, ItemStack armor, ItemStack material) {
        helper.setBlock(INFUSER_POS, ModBlocks.CELESTIAL_INFUSER.get());
        CelestialInfuserBlockEntity infuser = (CelestialInfuserBlockEntity) helper.getBlockEntity(INFUSER_POS);
        infuser.getItems().setStackInSlot(0, armor);
        infuser.getItems().setStackInSlot(1, material);
        return infuser;
    }
}
