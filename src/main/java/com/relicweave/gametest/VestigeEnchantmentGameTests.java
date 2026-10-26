package com.relicweave.gametest;

import com.mojang.authlib.GameProfile;
import com.relicweave.RelicWeave;
import com.relicweave.enchantment.vestige.ArchivistsMarkEvents;
import com.relicweave.enchantment.vestige.CycleboundEvents;
import com.relicweave.enchantment.vestige.GlyphfinderEvents;
import com.relicweave.enchantment.vestige.GravityShearProtection;
import com.relicweave.enchantment.vestige.SealbreakerEvents;
import com.relicweave.enchantment.vestige.SovereignsBaneEvents;
import com.relicweave.enchantment.vestige.VestigeEnchantEffects;
import com.relicweave.infusion.InfusionType;
import com.relicweave.infusion.Infusions;
import com.relicweave.registry.ModEnchantments;
import com.relicweave.registry.ModItems;
import com.relicweave.vestige.boss.a.BossesA;
import com.relicweave.vestige.cycle.CyclePhase;
import com.relicweave.vestige.entity.beast.BeastMobs;
import com.relicweave.vestige.hazard.HazardProtection;
import com.relicweave.vestige.puzzle.PuzzleBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.UUID;

/** Server-side tests for the six 0.0.4 Vestige enchantments. Run with {@code ./gradlew runGameTestServer}. */
@GameTestHolder(RelicWeave.MOD_ID)
@PrefixGameTestTemplate(false)
public final class VestigeEnchantmentGameTests {
    private static final String TEMPLATE = "platform";

    private VestigeEnchantmentGameTests() {}

    // --- applicability -------------------------------------------------------------------------------------------

    @GameTest(template = TEMPLATE)
    public static void enchantmentsApplyToTheRightItemsOnly(GameTestHelper helper) {
        check(helper, ModEnchantments.RIFTWALKER.get(), 2, Items.DIAMOND_BOOTS, Items.DIAMOND_HELMET, Items.DIAMOND_SWORD);
        check(helper, ModEnchantments.GLYPHFINDER.get(), 2, Items.DIAMOND_HELMET, Items.DIAMOND_BOOTS, Items.BOW);
        check(helper, ModEnchantments.SOVEREIGNS_BANE.get(), 3, Items.DIAMOND_SWORD, Items.DIAMOND_PICKAXE, Items.BOW);
        check(helper, ModEnchantments.CYCLEBOUND.get(), 2, Items.DIAMOND_LEGGINGS, Items.DIAMOND_CHESTPLATE, Items.DIAMOND_AXE);
        check(helper, ModEnchantments.SEALBREAKER.get(), 3, Items.DIAMOND_PICKAXE, Items.DIAMOND_SWORD, Items.DIAMOND_SHOVEL);
        check(helper, ModEnchantments.ARCHIVISTS_MARK.get(), 2, Items.CROSSBOW, Items.DIAMOND_SWORD, Items.DIAMOND_LEGGINGS);
        helper.assertTrue(ModEnchantments.SOVEREIGNS_BANE.get().canEnchant(new ItemStack(Items.DIAMOND_AXE)), "Axes eligible for Sovereign's Bane");
        helper.assertTrue(ModEnchantments.ARCHIVISTS_MARK.get().canEnchant(new ItemStack(Items.BOW)), "Bows eligible for Archivist's Mark");
        helper.succeed();
    }

    private static void check(GameTestHelper helper, Enchantment enchantment, int maxLevel, Item fits, Item other1, Item other2) {
        helper.assertTrue(enchantment.getMaxLevel() == maxLevel, enchantment + " max level should be " + maxLevel);
        helper.assertTrue(enchantment.canEnchant(new ItemStack(fits)), enchantment + " should fit " + fits);
        helper.assertFalse(enchantment.canEnchant(new ItemStack(other1)), enchantment + " must not fit " + other1);
        helper.assertFalse(enchantment.canEnchant(new ItemStack(other2)), enchantment + " must not fit " + other2);
    }

    // --- Riftwalker + Gravitic Tread -------------------------------------------------------------------------------

    @GameTest(template = TEMPLATE)
    public static void riftwalkerAloneAndCombinedWithGraviticTread(GameTestHelper helper) {
        ServerPlayer player = makePlayer(helper);
        near(helper, HazardProtection.reduction(player, HazardProtection.Hazard.GRAVITY_SHEAR), 0.0, "No protection bare");

        player.setItemSlot(EquipmentSlot.FEET, enchanted(Items.DIAMOND_BOOTS, ModEnchantments.RIFTWALKER.get(), 1));
        near(helper, HazardProtection.reduction(player, HazardProtection.Hazard.GRAVITY_SHEAR), 0.20, "Riftwalker I");
        player.setItemSlot(EquipmentSlot.FEET, enchanted(Items.DIAMOND_BOOTS, ModEnchantments.RIFTWALKER.get(), 2));
        near(helper, HazardProtection.reduction(player, HazardProtection.Hazard.GRAVITY_SHEAR), 0.35, "Riftwalker II alone");

        ItemStack tread = Infusions.withInfusion(new ItemStack(ModItems.CELESTIAL_BOOTS.get()), InfusionType.GRAVITIC_TREAD);
        player.setItemSlot(EquipmentSlot.FEET, tread);
        near(helper, HazardProtection.reduction(player, HazardProtection.Hazard.GRAVITY_SHEAR), 0.30, "Gravitic Tread alone");

        ItemStack both = tread.copy();
        both.enchant(ModEnchantments.RIFTWALKER.get(), 2);
        player.setItemSlot(EquipmentSlot.FEET, both);
        near(helper, HazardProtection.reduction(player, HazardProtection.Hazard.GRAVITY_SHEAR), GravityShearProtection.COMBINED_CAP, "Both capped at 0.5");
        helper.succeed();
    }

    // --- Glyphfinder -----------------------------------------------------------------------------------------------

    @GameTest(template = TEMPLATE)
    public static void glyphfinderScansOnlyHiddenGlyphsInRange(GameTestHelper helper) {
        ServerPlayer player = makePlayer(helper);
        helper.setBlock(new BlockPos(1, 2, 1), PuzzleBlocks.HIDDEN_GLYPH.get());
        helper.setBlock(new BlockPos(3, 2, 3), PuzzleBlocks.GLYPH_SEAL.get());
        helper.setBlock(new BlockPos(3, 2, 1), Blocks.CHEST);
        helper.assertTrue(GlyphfinderEvents.scan(player, 1).size() == 1, "Only the hidden glyph is found, not seals or chests");
        helper.assertTrue(GlyphfinderEvents.radius(1) == 8 && GlyphfinderEvents.radius(2) == 12, "Radii 8 / 12");
        helper.assertTrue(GlyphfinderEvents.intervalTicks(1) == 200 && GlyphfinderEvents.intervalTicks(2) == 120, "Intervals 200 / 120");
        helper.succeed();
    }

    // --- Sovereign's Bane ------------------------------------------------------------------------------------------

    @GameTest(template = TEMPLATE)
    public static void sovereignsBaneBonusMath(GameTestHelper helper) {
        EntityType<?> elite = BeastMobs.SHARDBACK_BRUTE.get();
        EntityType<?> boss = BossesA.THORNED_REGENT.get();
        near(helper, SovereignsBaneEvents.bonus(elite, 1), 1.5, "Elite I");
        near(helper, SovereignsBaneEvents.bonus(elite, 3), 4.5, "Elite III");
        near(helper, SovereignsBaneEvents.bonus(boss, 1), 1.0, "Boss I");
        near(helper, SovereignsBaneEvents.bonus(boss, 3), 2.0, "Boss III capped at 2.0");
        near(helper, SovereignsBaneEvents.bonus(EntityType.ZOMBIE, 3), 0.0, "Other mobs");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void sovereignsBaneAddsDamageOnlyToElitesInMelee(GameTestHelper helper) {
        ServerPlayer player = makePlayer(helper);
        player.setItemSlot(EquipmentSlot.MAINHAND, enchanted(Items.DIAMOND_SWORD, ModEnchantments.SOVEREIGNS_BANE.get(), 3));
        var brute = helper.spawn(BeastMobs.SHARDBACK_BRUTE.get(), 1, 1, 1);
        brute.setNoAi(true);
        near(helper, hurt(brute, player.damageSources().playerAttack(player), 5.0F), 9.5, "Melee on an elite gets +4.5");
        near(helper, hurt(brute, player.damageSources().arrow(null, player), 5.0F), 5.0, "Ranged damage gets no bonus");
        Zombie zombie = helper.spawn(EntityType.ZOMBIE, 3, 1, 3);
        near(helper, hurt(zombie, player.damageSources().playerAttack(player), 5.0F), 5.0, "Non-elite gets no bonus");
        player.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.DIAMOND_SWORD));
        near(helper, hurt(brute, player.damageSources().playerAttack(player), 5.0F), 5.0, "No enchantment, no bonus");
        helper.succeed();
    }

    // --- Cyclebound ------------------------------------------------------------------------------------------------

    @GameTest(template = TEMPLATE)
    public static void cycleboundOnlyInResonance(GameTestHelper helper) {
        ServerPlayer player = makePlayer(helper);
        player.setItemSlot(EquipmentSlot.LEGS, enchanted(Items.DIAMOND_LEGGINGS, ModEnchantments.CYCLEBOUND.get(), 2));
        var speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        helper.assertFalse(CycleboundEvents.update(player, CyclePhase.QUIET), "Off in Quiet");
        helper.assertTrue(speed.getModifier(CycleboundEvents.MODIFIER_ID) == null, "No modifier in Quiet");
        helper.assertTrue(CycleboundEvents.update(player, CyclePhase.RESONANCE), "On in Resonance");
        var modifier = speed.getModifier(CycleboundEvents.MODIFIER_ID);
        helper.assertTrue(modifier != null && Math.abs(modifier.getAmount() - 0.10) < 1.0E-9, "Level II gives +10%");
        helper.assertFalse(CycleboundEvents.update(player, CyclePhase.CONVERGENCE), "Off in Convergence");
        helper.assertTrue(speed.getModifier(CycleboundEvents.MODIFIER_ID) == null, "Removed in Convergence");

        CycleboundEvents.update(player, CyclePhase.RESONANCE);
        player.setItemSlot(EquipmentSlot.LEGS, enchanted(Items.DIAMOND_LEGGINGS, ModEnchantments.CYCLEBOUND.get(), 1));
        CycleboundEvents.update(player, CyclePhase.RESONANCE);
        helper.assertTrue(Math.abs(speed.getModifier(CycleboundEvents.MODIFIER_ID).getAmount() - 0.05) < 1.0E-9, "Level I replaces: +5%");
        player.setItemSlot(EquipmentSlot.LEGS, ItemStack.EMPTY);
        CycleboundEvents.update(player, CyclePhase.RESONANCE);
        helper.assertTrue(speed.getModifier(CycleboundEvents.MODIFIER_ID) == null, "Removed without leggings");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void cycleboundIsInactiveOutsideTheVestige(GameTestHelper helper) {
        ServerPlayer player = makePlayer(helper);
        player.setItemSlot(EquipmentSlot.LEGS, enchanted(Items.DIAMOND_LEGGINGS, ModEnchantments.CYCLEBOUND.get(), 2));
        CycleboundEvents.update(player);
        helper.assertTrue(player.getAttribute(Attributes.MOVEMENT_SPEED).getModifier(CycleboundEvents.MODIFIER_ID) == null,
                "Overworld never gets the modifier");
        helper.succeed();
    }

    // --- Sealbreaker -----------------------------------------------------------------------------------------------

    @GameTest(template = TEMPLATE)
    public static void sealbreakerSpeedsUpGlyphSealsOnly(GameTestHelper helper) {
        ServerPlayer player = makePlayer(helper);
        player.setItemSlot(EquipmentSlot.MAINHAND, enchanted(Items.DIAMOND_PICKAXE, ModEnchantments.SEALBREAKER.get(), 3));
        near(helper, breakSpeed(player, PuzzleBlocks.GLYPH_SEAL.get().defaultBlockState()), 20.0, "Seal: x(1+3*3)");
        near(helper, breakSpeed(player, Blocks.STONE.defaultBlockState()), 2.0, "Stone unchanged");
        near(helper, breakSpeed(player, com.relicweave.vestige.boss.BossRegistry.SEAL_GATE.get().defaultBlockState()), 2.0, "Seal gate unchanged");
        near(helper, breakSpeed(player, com.relicweave.vestige.boss.BossRegistry.BOSS_ALTAR.get().defaultBlockState()), 2.0, "Boss altar unchanged");
        player.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.DIAMOND_PICKAXE));
        near(helper, breakSpeed(player, PuzzleBlocks.GLYPH_SEAL.get().defaultBlockState()), 2.0, "No enchantment");
        helper.assertTrue(SealbreakerEvents.multiplier(1) == 4.0F && SealbreakerEvents.multiplier(2) == 7.0F, "Multipliers 4 / 7");
        helper.succeed();
    }

    // --- Archivist's Mark ------------------------------------------------------------------------------------------

    @GameTest(template = TEMPLATE)
    public static void archivistsMarkAppliesEffectGlowingAndBonusDamage(GameTestHelper helper) {
        ServerPlayer player = makePlayer(helper);
        Zombie zombie = helper.spawn(EntityType.ZOMBIE, 1, 1, 1);
        zombie.setNoAi(true);
        near(helper, hurt(zombie, player.damageSources().playerAttack(player), 10.0F), 10.0, "Unmarked");

        ArchivistsMarkEvents.mark(zombie, 1);
        var mark = zombie.getEffect(VestigeEnchantEffects.ARCHIVISTS_MARK.get());
        helper.assertTrue(mark != null && mark.getDuration() == 60, "Mark I lasts 60 ticks");
        helper.assertTrue(zombie.hasEffect(MobEffects.GLOWING) && zombie.getEffect(MobEffects.GLOWING).getDuration() == 60, "Glowing for the same time");
        ArchivistsMarkEvents.mark(zombie, 2);
        helper.assertTrue(zombie.getEffect(VestigeEnchantEffects.ARCHIVISTS_MARK.get()).getDuration() == 100, "Mark II lasts 100 ticks");
        near(helper, hurt(zombie, player.damageSources().playerAttack(player), 10.0F), 11.0, "Marked: x1.10, not stacked");
        near(helper, hurt(zombie, zombie.damageSources().generic(), 10.0F), 10.0, "Non-player damage unchanged");

        var boss = helper.spawn(BossesA.THORNED_REGENT.get(), 3, 1, 3);
        boss.setNoAi(true);
        ArchivistsMarkEvents.mark(boss, 1);
        near(helper, hurt(boss, player.damageSources().playerAttack(player), 10.0F), 11.0, "Bosses are marked too");
        helper.succeed();
    }

    // --- helpers ---------------------------------------------------------------------------------------------------

    private static void near(GameTestHelper helper, double actual, double expected, String what) {
        helper.assertTrue(Math.abs(actual - expected) < 1.0E-5, what + ": expected " + expected + " but was " + actual);
    }

    private static ItemStack enchanted(Item item, Enchantment enchantment, int level) {
        ItemStack stack = new ItemStack(item);
        stack.enchant(enchantment, level);
        return stack;
    }

    private static float hurt(net.minecraft.world.entity.LivingEntity victim, net.minecraft.world.damagesource.DamageSource source, float amount) {
        LivingHurtEvent event = new LivingHurtEvent(victim, source, amount);
        MinecraftForge.EVENT_BUS.post(event);
        return event.getAmount();
    }

    private static float breakSpeed(ServerPlayer player, net.minecraft.world.level.block.state.BlockState state) {
        PlayerEvent.BreakSpeed event = new PlayerEvent.BreakSpeed(player, state, 2.0F, BlockPos.ZERO);
        MinecraftForge.EVENT_BUS.post(event);
        return event.getNewSpeed();
    }

    private static ServerPlayer makePlayer(GameTestHelper helper) {
        ServerPlayer player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "relicweave-vestige-ench-test"));
        player.setGameMode(GameType.SURVIVAL);
        Vec3 pos = helper.absoluteVec(new Vec3(2.5, 1.0, 2.5));
        player.moveTo(pos.x, pos.y, pos.z, 0.0F, 0.0F);
        return player;
    }
}
