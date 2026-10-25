package com.relicweave.gametest;

import com.relicweave.RelicWeave;
import com.relicweave.enchantment.effects.NecromancerMinions;
import com.relicweave.registry.ModEnchantments;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.monster.piglin.PiglinBrute;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.Collection;
import java.util.List;

/** Server-side tests for the 0.0.2 enchantments' effects and their transmutation recipes. */
@GameTestHolder(RelicWeave.MOD_ID)
@PrefixGameTestTemplate(false)
public final class EnchantmentGameTests {
    private static final String TEMPLATE = "platform";
    private static final List<String> RECIPES = List.of("cobbled_up_1", "oxidation_1", "conductive_1", "golemed_1",
            "piglinified_1", "villagers_best_friend_1", "the_necromancer_1", "necromanced_1", "upbound_1");

    private EnchantmentGameTests() {}

    @GameTest(template = TEMPLATE)
    public static void allNewTransmutationRecipesLoad(GameTestHelper helper) {
        for (String name : RECIPES) {
            helper.assertTrue(helper.getLevel().getRecipeManager().byKey(RelicWeave.id("transmutation/" + name)).isPresent(),
                    "Missing recipe " + name);
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void oxidationSlowsOnPlayerMeleeOnly(GameTestHelper helper) {
        Player player = helper.makeMockSurvivalPlayer();

        player.setItemSlot(EquipmentSlot.MAINHAND, enchanted(Items.IRON_SWORD, ModEnchantments.OXIDATION.get(), 2));
        Pig melee = hit(helper, player, true);
        helper.assertTrue(melee.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "Oxidation II should slow on melee");
        helper.assertTrue(melee.getEffect(MobEffects.MOVEMENT_SLOWDOWN).getDuration() > 60, "Slowness should last about 80 ticks");
        helper.assertFalse(melee.hasEffect(MobEffects.WEAKNESS), "Weakness is only for level III");
        melee.discard();

        Pig mob = hit(helper, player, false);
        helper.assertFalse(mob.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "Mob attack must not trigger Oxidation");
        mob.discard();

        player.setItemSlot(EquipmentSlot.MAINHAND, enchanted(Items.IRON_AXE, ModEnchantments.OXIDATION.get(), 3));
        Pig strong = hit(helper, player, true);
        helper.assertTrue(strong.hasEffect(MobEffects.MOVEMENT_SLOWDOWN) && strong.hasEffect(MobEffects.WEAKNESS),
                "Oxidation III should apply Slowness and Weakness");
        strong.discard();

        player.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
        Pig plain = hit(helper, player, true);
        helper.assertFalse(plain.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "Unenchanted sword must not slow");
        plain.discard();
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void golemedAddsChestAttributes(GameTestHelper helper) {
        ItemStack chest = enchanted(Items.IRON_CHESTPLATE, ModEnchantments.GOLEMED.get(), 2);
        helper.assertTrue(modifierTotal(chest, EquipmentSlot.CHEST, Attributes.ARMOR) == 2.0 + 6.0,
                "Chest armor should be vanilla 6 + 2");
        helper.assertTrue(Math.abs(modifierTotal(chest, EquipmentSlot.CHEST, Attributes.KNOCKBACK_RESISTANCE) - 0.2) < 1.0E-6,
                "Chest knockback resistance should be +0.2");
        helper.assertTrue(modifierTotal(chest, EquipmentSlot.LEGS, Attributes.ARMOR) == 0.0, "No bonus for legs");
        helper.assertTrue(modifierTotal(new ItemStack(Items.IRON_CHESTPLATE), EquipmentSlot.CHEST, Attributes.KNOCKBACK_RESISTANCE) == 0.0,
                "Unenchanted chestplate has no knockback resistance");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void conductiveScalesBreakSpeed(GameTestHelper helper) {
        Player player = helper.makeMockSurvivalPlayer();
        var stone = Blocks.STONE.defaultBlockState();

        player.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_PICKAXE));
        float base = breakSpeed(player, stone);
        player.setItemSlot(EquipmentSlot.MAINHAND, enchanted(Items.IRON_PICKAXE, ModEnchantments.CONDUCTIVE.get(), 2));
        float boosted = breakSpeed(player, stone);
        helper.assertTrue(Math.abs(boosted - base * 1.5F) < 1.0E-4F, "Conductive II should be 1.5x, got " + boosted + " vs " + base);

        // Wrong tool for the block: no bonus.
        player.setItemSlot(EquipmentSlot.MAINHAND, enchanted(Items.IRON_SHOVEL, ModEnchantments.CONDUCTIVE.get(), 2));
        float shovelOnStone = breakSpeed(player, stone);
        player.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SHOVEL));
        helper.assertTrue(Math.abs(shovelOnStone - breakSpeed(player, stone)) < 1.0E-4F, "No bonus for the wrong tool");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void necromancedStopsUndeadTargetingAndWither(GameTestHelper helper) {
        Player player = helper.makeMockSurvivalPlayer();

        Zombie control = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(1, 1, 1));
        control.setTarget(player);
        helper.assertTrue(control.getTarget() == player, "Without the helmet a zombie may target the player");
        control.discard();

        player.setItemSlot(EquipmentSlot.HEAD, enchanted(Items.IRON_HELMET, ModEnchantments.NECROMANCED.get(), 1));
        Zombie zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(1, 1, 1));
        zombie.setTarget(player);
        helper.assertTrue(zombie.getTarget() == null, "Zombie must not target a Necromanced wearer");
        zombie.discard();

        Pig pig = helper.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(1, 1, 1));
        pig.setTarget(player);
        helper.assertTrue(pig.getTarget() == player, "Non-undead mobs are unaffected");
        pig.discard();

        helper.assertFalse(player.addEffect(new MobEffectInstance(MobEffects.WITHER, 100)), "Wither must be denied");
        helper.assertFalse(player.hasEffect(MobEffects.WITHER), "Player must not have Wither");
        helper.assertTrue(player.addEffect(new MobEffectInstance(MobEffects.POISON, 100)), "Other effects still apply");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void piglinifiedStopsPiglinTargeting(GameTestHelper helper) {
        Player player = helper.makeMockSurvivalPlayer();
        // Piglin#getTarget reads its brain, so exercise the event vanilla's brain behaviours fire instead of setTarget.
        Piglin piglin = helper.spawnWithNoFreeWill(EntityType.PIGLIN, new BlockPos(1, 1, 1));
        helper.assertFalse(changeTarget(piglin, player).isCanceled(), "Without the helmet a piglin may target the player");

        player.setItemSlot(EquipmentSlot.HEAD, enchanted(Items.GOLDEN_HELMET, ModEnchantments.PIGLINIFIED.get(), 1));
        helper.assertTrue(changeTarget(piglin, player).isCanceled(), "Piglin must not target a Piglinified wearer");

        piglin.setLastHurtByMob(player);
        helper.assertFalse(changeTarget(piglin, player).isCanceled(), "A provoked piglin may target the wearer");
        piglin.discard();

        PiglinBrute brute = helper.spawnWithNoFreeWill(EntityType.PIGLIN_BRUTE, new BlockPos(1, 1, 1));
        helper.assertFalse(changeTarget(brute, player).isCanceled(), "Brutes are unaffected");
        brute.discard();
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 700)
    public static void necromancerMinionIsTaggedHarmlessAndExpires(GameTestHelper helper) {
        Player player = helper.makeMockSurvivalPlayer();
        Vec3 pos = helper.absoluteVec(new Vec3(2.5, 1.0, 2.5));
        Husk minion = NecromancerMinions.spawnMinion(helper.getLevel(), pos, player);
        helper.assertTrue(minion != null, "Minion should spawn");
        helper.assertTrue(NecromancerMinions.isMinion(minion), "Minion must carry the tag");
        helper.assertTrue(minion.getPersistentData().getUUID("relicweave:minion_owner").equals(player.getUUID()), "Owner not recorded");

        minion.setTarget(player);
        helper.assertTrue(minion.getTarget() == null, "Minion must never target a player");

        Husk other = NecromancerMinions.spawnMinion(helper.getLevel(), pos, player);
        minion.setTarget(other);
        helper.assertTrue(minion.getTarget() == null, "Minion must never target another minion");
        other.discard();

        helper.runAfterDelay(NecromancerMinions.MINION_LIFETIME_TICKS + 1, () -> {
            helper.assertTrue(minion.isRemoved(), "Minion should expire after its lifetime");
            helper.succeed();
        });
    }

    // ---- helpers ----

    private static LivingChangeTargetEvent changeTarget(LivingEntity entity, LivingEntity target) {
        return ForgeHooks.onLivingChangeTarget(entity, target, LivingChangeTargetEvent.LivingTargetType.BEHAVIOR_TARGET);
    }

    private static Pig hit(GameTestHelper helper, Player player, boolean melee) {
        Pig pig = helper.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(1, 1, 1));
        pig.hurt(melee ? player.damageSources().playerAttack(player) : player.damageSources().mobAttack(player), 1.0F);
        return pig;
    }

    private static double modifierTotal(ItemStack stack, EquipmentSlot slot, net.minecraft.world.entity.ai.attributes.Attribute attribute) {
        Collection<AttributeModifier> modifiers = stack.getAttributeModifiers(slot).get(attribute);
        return modifiers.stream().mapToDouble(AttributeModifier::getAmount).sum();
    }

    private static float breakSpeed(Player player, net.minecraft.world.level.block.state.BlockState state) {
        PlayerEvent.BreakSpeed event = new PlayerEvent.BreakSpeed(player, state, 4.0F, BlockPos.ZERO);
        MinecraftForge.EVENT_BUS.post(event);
        return event.getNewSpeed();
    }

    private static ItemStack enchanted(Item item, Enchantment enchantment, int level) {
        ItemStack stack = new ItemStack(item);
        stack.enchant(enchantment, level);
        return stack;
    }
}
