package com.relicweave.gametest;

import com.mojang.authlib.GameProfile;
import com.relicweave.RelicWeave;
import com.relicweave.enchantment.attunement.ReapingEvents;
import com.relicweave.enchantment.attunement.ReprisalEvents;
import com.relicweave.enchantment.attunement.ResonanceEvents;
import com.relicweave.enchantment.attunement.SoulguardEvents;
import com.relicweave.enchantment.attunement.SteadyDrawEvents;
import com.relicweave.enchantment.attunement.UndertowEvents;
import com.relicweave.registry.ModEnchantments;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.UUID;

/** Server-side tests for the six 0.0.3 Transmutation Table enchantments (effect cores called directly). */
@GameTestHolder(RelicWeave.MOD_ID)
@PrefixGameTestTemplate(false)
public final class AttunementEnchantmentGameTests {
    private static final String TEMPLATE = "platform";
    private static final BlockPos CENTER = new BlockPos(2, 12, 2);
    private static final int FIELD_RADIUS = 4;
    /** Ticks to let the light engine light the field; crops need light 8+ and would otherwise pop on neighbour updates. */
    private static final int LIGHT_DELAY_TICKS = 3;

    private AttunementEnchantmentGameTests() {}

    // ---- Reaping ----

    @GameTest(template = TEMPLATE)
    public static void reapingOneHarvestsThreeByThreeAndReplants(GameTestHelper helper) {
        ServerPlayer player = makePlayer(helper);
        buildField(helper, null);
        ItemStack hoe = enchanted(Items.DIAMOND_HOE, ModEnchantments.REAPING.get(), 1);
        helper.runAfterDelay(LIGHT_DELAY_TICKS, () -> {
        int harvested = ReapingEvents.harvestArea(helper.getLevel(), helper.absolutePos(CENTER), player, hoe, 1);
        helper.assertTrue(harvested == 9, "Reaping I should harvest 9 crops, got " + harvested);
        helper.assertTrue(hoe.getDamageValue() == 8, "Reaping I should cost 8 durability, cost " + hoe.getDamageValue());
        for (int dx = -FIELD_RADIUS; dx <= FIELD_RADIUS; dx++) {
            for (int dz = -FIELD_RADIUS; dz <= FIELD_RADIUS; dz++) {
                boolean inside = Math.abs(dx) <= 1 && Math.abs(dz) <= 1;
                int age = age(helper, dx, dz);
                helper.assertTrue(inside ? age == 0 : age == 7,
                        "Crop at " + dx + "," + dz + " has wrong age " + age);
            }
        }
        cleanup(helper);
        helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE)
    public static void reapingThreeHarvestsSevenBySevenAndSkipsImmature(GameTestHelper helper) {
        ServerPlayer player = makePlayer(helper);
        buildField(helper, new BlockPos(2, 0, 3)); // one immature crop inside the 7x7 (offset 2,3)
        ItemStack hoe = enchanted(Items.DIAMOND_HOE, ModEnchantments.REAPING.get(), 3);
        helper.runAfterDelay(LIGHT_DELAY_TICKS, () -> {
        int harvested = ReapingEvents.harvestArea(helper.getLevel(), helper.absolutePos(CENTER), player, hoe, 3);
        helper.assertTrue(harvested == 48, "Reaping III should harvest 48 mature crops, got " + harvested);
        helper.assertTrue(hoe.getDamageValue() == 47, "Expected 47 durability, got " + hoe.getDamageValue());
        helper.assertTrue(age(helper, 2, 3) == 3, "Immature crop must be untouched");
        helper.assertTrue(age(helper, 3, 3) == 0 && age(helper, -3, -3) == 0, "Corners of 7x7 must be replanted");
        helper.assertTrue(age(helper, 4, 0) == 7 && age(helper, 0, 4) == 7, "Crops outside 7x7 must be untouched");
        cleanup(helper);
        helper.succeed();
        });
    }

    // ---- Undertow ----

    @GameTest(template = TEMPLATE)
    public static void undertowSlowsOnlyTargetsInWater(GameTestHelper helper) {
        ServerPlayer player = makePlayer(helper);
        Pig wet = helper.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(1, 1, 1));
        UndertowEvents.applyEffects(wet, player, 2, true, true);
        helper.assertTrue(wet.hasEffect(MobEffects.MOVEMENT_SLOWDOWN) && wet.getEffect(MobEffects.MOVEMENT_SLOWDOWN).getAmplifier() == 1,
                "Undertow II should apply Slowness II to a target in water");
        helper.assertTrue(wet.getEffect(MobEffects.MOVEMENT_SLOWDOWN).getDuration() > 60, "Slowness should last about 80 ticks");
        helper.assertTrue(player.hasEffect(MobEffects.DOLPHINS_GRACE), "Wielder in water gets Dolphin's Grace");
        player.removeAllEffects();

        Pig dry = helper.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(1, 1, 1));
        UndertowEvents.applyEffects(dry, player, 2, false, true);
        helper.assertFalse(dry.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "Dry target must not be slowed");
        helper.assertFalse(player.hasEffect(MobEffects.DOLPHINS_GRACE), "No grace without a wet target");

        WitherBoss boss = helper.spawnWithNoFreeWill(EntityType.WITHER, new BlockPos(1, 1, 1));
        UndertowEvents.applyEffects(boss, player, 3, true, true);
        helper.assertFalse(boss.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "Bosses must not be slowed");
        helper.assertTrue(player.hasEffect(MobEffects.DOLPHINS_GRACE), "Wielder still gets Dolphin's Grace against bosses");
        wet.discard();
        dry.discard();
        boss.discard();
        helper.succeed();
    }

    // ---- Resonance ----

    @GameTest(template = TEMPLATE)
    public static void resonanceStacksCapsAndResets(GameTestHelper helper) {
        UUID id = UUID.randomUUID();
        var stone = Blocks.STONE;
        int level = 2;
        helper.assertTrue(ResonanceEvents.speedMultiplier(id, stone, 0, level) == 1.0F, "No streak, no bonus");
        ResonanceEvents.recordBreak(id, stone, 0);
        helper.assertTrue(near(ResonanceEvents.speedMultiplier(id, stone, 1, level), 1.0F), "First break gives no bonus yet");
        ResonanceEvents.recordBreak(id, stone, 10);
        helper.assertTrue(near(ResonanceEvents.speedMultiplier(id, stone, 11, level), 1.02F), "Second same block: +2% at level II");
        for (int i = 0; i < 30; i++) {
            ResonanceEvents.recordBreak(id, stone, 20 + i);
        }
        helper.assertTrue(near(ResonanceEvents.speedMultiplier(id, stone, 60, level), 1.0F + 0.10F * level), "Capped at +20% for level II");
        helper.assertTrue(near(ResonanceEvents.speedMultiplier(id, Blocks.DIRT, 60, level), 1.0F), "Other block type gets no bonus");
        helper.assertTrue(near(ResonanceEvents.speedMultiplier(id, stone, 60 + ResonanceEvents.RESET_TICKS + 5, level), 1.0F),
                "Stale streak gives no bonus");
        ResonanceEvents.recordBreak(id, Blocks.DIRT, 61);
        helper.assertTrue(near(ResonanceEvents.speedMultiplier(id, stone, 62, level), 1.0F), "Different block resets the streak");
        ResonanceEvents.recordBreak(id, stone, 62);
        helper.assertTrue(near(ResonanceEvents.speedMultiplier(id, stone, 63, level), 1.0F), "Streak restarted at 1");
        helper.succeed();
    }

    // ---- Soulguard ----

    @GameTest(template = TEMPLATE)
    public static void soulguardGrantsOnceAndRespectsCap(GameTestHelper helper) {
        ServerPlayer player = makePlayer(helper);
        player.setItemSlot(EquipmentSlot.CHEST, enchanted(Items.IRON_CHESTPLATE, ModEnchantments.SOULGUARD.get(), 2));
        Zombie zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(1, 1, 1));
        Pig pig = helper.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(1, 1, 1));

        helper.assertTrue(SoulguardEvents.onKill(player, pig, 1000) == 0.0F, "Passive mobs grant nothing");
        helper.assertTrue(SoulguardEvents.onKill(player, zombie, 1000) == 4.0F, "Soulguard II grants 4 absorption");
        helper.assertTrue(player.getAbsorptionAmount() == 4.0F, "Absorption should be 4");
        helper.assertTrue(SoulguardEvents.onKill(player, zombie, 1000 + SoulguardEvents.COOLDOWN_TICKS - 1) == 0.0F,
                "Second kill within the cooldown grants nothing");
        helper.assertTrue(player.getAbsorptionAmount() == 4.0F, "Absorption unchanged during cooldown");
        helper.assertTrue(SoulguardEvents.onKill(player, zombie, 1000 + SoulguardEvents.COOLDOWN_TICKS) == 4.0F,
                "Grants again after the cooldown");
        helper.assertTrue(SoulguardEvents.onKill(player, zombie, 5000) == 0.0F && player.getAbsorptionAmount() == 8.0F,
                "Total never exceeds the cap of 8");
        helper.assertTrue(player.getAbsorptionAmount() <= 8.0F, "Cap respected");
        player.setAbsorptionAmount(0.0F);
        player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
        helper.assertTrue(SoulguardEvents.onKill(player, zombie, 9000) == 0.0F, "No enchantment, no shield");
        zombie.discard();
        pig.discard();
        helper.succeed();
    }

    // ---- Steady Draw ----

    @GameTest(template = TEMPLATE)
    public static void steadyDrawMultipliers(GameTestHelper helper) {
        helper.assertTrue(near(SteadyDrawEvents.steadyDrawMultiplier(1), 0.5F), "Steady Draw I should be 0.5");
        helper.assertTrue(near(SteadyDrawEvents.steadyDrawMultiplier(2), 0.8F), "Steady Draw II should be 0.8");
        helper.succeed();
    }

    // ---- Reprisal ----

    @GameTest(template = TEMPLATE)
    public static void reprisalRewardsTimingAndHasCooldown(GameTestHelper helper) {
        ServerPlayer player = makePlayer(helper);
        Zombie late = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(1, 1, 1));
        helper.assertFalse(ReprisalEvents.onBlock(player, late, 2, ReprisalEvents.TIMING_WINDOW_TICKS + 1, 100),
                "Late block gives nothing");
        helper.assertFalse(late.hasEffect(MobEffects.WEAKNESS), "No weakness outside the window");

        Zombie early = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(1, 1, 1));
        helper.assertTrue(ReprisalEvents.onBlock(player, early, 2, ReprisalEvents.TIMING_WINDOW_TICKS, 100), "Timed block works");
        helper.assertTrue(early.hasEffect(MobEffects.WEAKNESS) && early.hasEffect(MobEffects.MOVEMENT_SLOWDOWN)
                && early.getEffect(MobEffects.MOVEMENT_SLOWDOWN).getAmplifier() == 1, "Weakness and Slowness II expected");

        Zombie second = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(1, 1, 1));
        helper.assertFalse(ReprisalEvents.onBlock(player, second, 2, 0, 100 + ReprisalEvents.COOLDOWN_TICKS - 1), "Cooldown blocks the next proc");
        helper.assertTrue(ReprisalEvents.onBlock(player, second, 1, 0, 100 + ReprisalEvents.COOLDOWN_TICKS), "Works after the cooldown");

        EnderDragon boss = helper.spawnWithNoFreeWill(EntityType.ENDER_DRAGON, new BlockPos(1, 1, 1));
        ServerPlayer other = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "relicweave-test2"));
        helper.assertTrue(ReprisalEvents.onBlock(other, boss, 3, 0, 100), "Boss block counts");
        // Vanilla bosses (Wither, Ender Dragon) reject every effect, so Weakness cannot be observed on them here.
        helper.assertFalse(boss.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "Bosses get no Slowness");
        late.discard();
        early.discard();
        second.discard();
        boss.discard();
        helper.succeed();
    }

    // ---- helpers ----

    private static boolean near(float a, float b) {
        return Math.abs(a - b) < 1.0E-5F;
    }

    private static ServerPlayer makePlayer(GameTestHelper helper) {
        ServerPlayer player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "relicweave-test"));
        player.setGameMode(GameType.SURVIVAL);
        Vec3 pos = helper.absoluteVec(new Vec3(2.5, 1.0, 2.5));
        player.moveTo(pos.x, pos.y, pos.z, 0.0F, 0.0F);
        player.removeAllEffects();
        player.setAbsorptionAmount(0.0F);
        return player;
    }

    /** A 9x9 field of mature wheat on farmland, built high above the template so it cannot collide with neighbouring tests (cleaned up after). */
    private static void buildField(GameTestHelper helper, BlockPos immatureOffset) {
        for (int dx = -FIELD_RADIUS; dx <= FIELD_RADIUS; dx++) {
            for (int dz = -FIELD_RADIUS; dz <= FIELD_RADIUS; dz++) {
                BlockPos ground = CENTER.offset(dx, -1, dz);
                helper.getLevel().setBlock(helper.absolutePos(ground), Blocks.FARMLAND.defaultBlockState(), 18);
                boolean immature = immatureOffset != null && immatureOffset.getX() == dx && immatureOffset.getZ() == dz;
                // Crops need light 8+ to survive neighbour updates; the test world can be dark.
                helper.getLevel().setBlock(helper.absolutePos(ground.above(2)), Blocks.LIGHT.defaultBlockState(), 18);
                helper.getLevel().setBlock(helper.absolutePos(ground.above()),
                        Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE, immature ? 3 : 7), 18);
            }
        }
    }

    private static int age(GameTestHelper helper, int dx, int dz) {
        BlockState state = helper.getLevel().getBlockState(helper.absolutePos(CENTER.offset(dx, 0, dz)));
        return state.is(Blocks.WHEAT) ? state.getValue(CropBlock.AGE) : -1;
    }

    private static void cleanup(GameTestHelper helper) {
        for (int dx = -FIELD_RADIUS; dx <= FIELD_RADIUS; dx++) {
            for (int dz = -FIELD_RADIUS; dz <= FIELD_RADIUS; dz++) {
                BlockPos ground = helper.absolutePos(CENTER.offset(dx, -1, dz));
                helper.getLevel().setBlock(ground.above(), Blocks.AIR.defaultBlockState(), 18);
                helper.getLevel().setBlock(ground.above(2), Blocks.AIR.defaultBlockState(), 18);
                helper.getLevel().setBlock(ground, Blocks.AIR.defaultBlockState(), 18);
            }
        }
        helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                new net.minecraft.world.phys.AABB(helper.absolutePos(CENTER)).inflate(8)).forEach(net.minecraft.world.entity.Entity::discard);
    }

    private static ItemStack enchanted(Item item, Enchantment enchantment, int level) {
        ItemStack stack = new ItemStack(item);
        stack.enchant(enchantment, level);
        return stack;
    }
}
