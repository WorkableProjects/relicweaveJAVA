package com.relicweave.gametest;

import com.mojang.authlib.GameProfile;
import com.relicweave.RelicWeave;
import com.relicweave.vestige.VestigeKeys;
import com.relicweave.vestige.entity.beast.AetherGrazer;
import com.relicweave.vestige.entity.beast.ArchiveMimic;
import com.relicweave.vestige.entity.beast.BeastMobs;
import com.relicweave.vestige.entity.beast.BeastUtil;
import com.relicweave.vestige.entity.beast.GlassrootStalker;
import com.relicweave.vestige.entity.beast.GravityLeech;
import com.relicweave.vestige.entity.beast.RuinHound;
import com.relicweave.vestige.entity.beast.ShardbackBrute;
import com.relicweave.vestige.entity.beast.StarfallStrider;
import com.relicweave.vestige.registry.VestigeMaterials;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.UUID;

/** Server-side tests for the seven beast and creature Vestige mobs. */
@GameTestHolder(RelicWeave.MOD_ID)
@PrefixGameTestTemplate(false)
public final class BeastMobGameTests {
    private static final String TEMPLATE = "platform";

    private BeastMobGameTests() {}

    private static boolean near(double a, double b) {
        return Math.abs(a - b) < 1.0E-3;
    }

    private static ServerPlayer makePlayer(GameTestHelper helper, Vec3 relative) {
        ServerPlayer player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "relicweave-beast-test"));
        player.setGameMode(GameType.SURVIVAL);
        Vec3 pos = helper.absoluteVec(relative);
        player.moveTo(pos.x, pos.y, pos.z, 0.0F, 0.0F);
        player.setDeltaMovement(Vec3.ZERO);
        player.removeAllEffects();
        return player;
    }


    private static <T extends Mob> T roundTrip(GameTestHelper helper, T mob) {
        CompoundTag tag = mob.saveWithoutId(new CompoundTag());
        @SuppressWarnings("unchecked")
        T copy = (T) mob.getType().create(helper.getLevel());
        copy.load(tag);
        return copy;
    }

    private static void checkStats(GameTestHelper helper, Mob mob, double health, double damage, double armor) {
        helper.assertTrue(near(mob.getMaxHealth(), health), "Max health should be " + health + " but is " + mob.getMaxHealth());
        if (damage > 0) {
            helper.assertTrue(near(mob.getAttributeValue(Attributes.ATTACK_DAMAGE), damage), "Attack damage should be " + damage);
        }
        helper.assertTrue(near(mob.getAttributeValue(Attributes.ARMOR), armor), "Armor should be " + armor);
    }

    // ---- Glassroot Stalker ----

    @GameTest(template = TEMPLATE)
    public static void stalkerStatsAndNbt(GameTestHelper helper) {
        GlassrootStalker stalker = helper.spawnWithNoFreeWill(BeastMobs.GLASSROOT_STALKER.get(), 2.5F, 2.0F, 2.5F);
        checkStats(helper, stalker, 34, 8, 0);
        helper.assertTrue(near(stalker.getAttributeValue(Attributes.MOVEMENT_SPEED), 0.30), "Speed 0.30");
        helper.assertTrue(stalker.isLurking(), "Starts lurking");
        stalker.setState(GlassrootStalker.ACTIVE);
        stalker.setLungeCooldown(77);
        GlassrootStalker copy = roundTrip(helper, stalker);
        helper.assertTrue(copy.getState() == GlassrootStalker.ACTIVE, "State persists");
        helper.assertTrue(copy.getLungeCooldown() == 77, "Lunge cooldown persists");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void stalkerNeverHurtsBeforeRevealFinishes(GameTestHelper helper) {
        GlassrootStalker stalker = helper.spawn(BeastMobs.GLASSROOT_STALKER.get(), new BlockPos(1, 2, 1));
        Zombie victim = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, 2.5F, 2.0F, 2.5F);
        victim.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
        float before = victim.getHealth();
        helper.assertTrue(!stalker.doHurtTarget(victim), "A lurking stalker must not attack");
        helper.assertTrue(victim.getHealth() == before, "No damage while lurking");

        ServerPlayer player = makePlayer(helper, new Vec3(4.5, 2.0, 4.5));
        BeastUtil.addTestPlayer(player);
        helper.runAfterDelay(3, () -> {
            helper.assertTrue(stalker.getState() == GlassrootStalker.REVEALING, "A player within 10 blocks starts the reveal");
            helper.assertTrue(!stalker.doHurtTarget(victim), "No attack during the reveal shiver");
            helper.assertTrue(victim.getHealth() == before, "No damage during the reveal");
        });
        helper.runAfterDelay(12, () -> {
            helper.assertTrue(stalker.getState() == GlassrootStalker.REVEALING, "Still shivering");
            helper.assertTrue(!stalker.doHurtTarget(victim), "No attack before the reveal finishes");
        });
        helper.runAfterDelay(30, () -> {
            helper.assertTrue(stalker.getState() == GlassrootStalker.ACTIVE, "Reveal finished");
            helper.assertTrue(stalker.doHurtTarget(victim), "Attacks after the reveal");
            helper.assertTrue(victim.getHealth() < before, "Damage after the reveal");
            BeastUtil.removeTestPlayer(player);
                helper.succeed();
        });
    }

    // ---- Ruin Hound ----

    @GameTest(template = TEMPLATE)
    public static void houndStatsNbtAndPack(GameTestHelper helper) {
        RuinHound hound = helper.spawnWithNoFreeWill(BeastMobs.RUIN_HOUND.get(), 1.5F, 2.0F, 1.5F);
        checkStats(helper, hound, 22, 5, 0);
        helper.assertTrue(near(hound.getAttributeValue(Attributes.MOVEMENT_SPEED), 0.35), "Speed 0.35");
        helper.assertTrue(hound.isIsolated(), "A lone hound is isolated");
        RuinHound mate = helper.spawnWithNoFreeWill(BeastMobs.RUIN_HOUND.get(), 3.5F, 2.0F, 3.5F);
        helper.assertTrue(!hound.isIsolated() && !mate.isIsolated(), "Hounds within 16 blocks form a pack");
        hound.setFleeTimer(55);
        helper.assertTrue(roundTrip(helper, hound).getFleeTimer() == 55, "Flee timer persists");
        helper.succeed();
    }

    // ---- Aether Grazer ----

    @GameTest(template = TEMPLATE)
    public static void grazerStatsAndShearOnce(GameTestHelper helper) {
        AetherGrazer grazer = helper.spawnWithNoFreeWill(BeastMobs.AETHER_GRAZER.get(), 2.5F, 2.0F, 2.5F);
        checkStats(helper, grazer, 20, 0, 0);
        helper.assertTrue(grazer.isShearable(), "A fresh grazer has a fleece");
        List<ItemStack> first = grazer.shearFleece();
        helper.assertTrue(first.size() == 1 && first.get(0).is(VestigeMaterials.AETHER_DOWN.get()), "Shearing yields aether down");
        int count = first.get(0).getCount();
        helper.assertTrue(count >= 1 && count <= 2, "1-2 aether down, got " + count);
        helper.assertTrue(grazer.shearFleece().isEmpty(), "Cannot be sheared again until it regrows");
        helper.assertTrue(!grazer.isShearable(), "Bare");
        AetherGrazer copy = roundTrip(helper, grazer);
        helper.assertTrue(copy.getRegrowTicks() == AetherGrazer.REGROW_TICKS, "Regrowth timer persists through NBT");
        helper.assertTrue(!copy.isShearable() && copy.shearFleece().isEmpty(), "Still bare after reload");
        copy.setRegrowTicks(0);
        helper.assertTrue(!copy.shearFleece().isEmpty(), "Shearable again once regrown");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void grazerShearsInteraction(GameTestHelper helper) {
        AetherGrazer grazer = helper.spawnWithNoFreeWill(BeastMobs.AETHER_GRAZER.get(), 2.5F, 2.0F, 2.5F);
        ServerPlayer player = makePlayer(helper, new Vec3(1.5, 2.0, 1.5));
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.SHEARS));
        InteractionResult result = grazer.mobInteract(player, InteractionHand.MAIN_HAND);
        helper.assertTrue(result.consumesAction(), "Shears interact with the grazer");
        helper.assertTrue(!grazer.isShearable(), "Sheared by the interaction");
        helper.assertTrue(!grazer.mobInteract(player, InteractionHand.MAIN_HAND).consumesAction(), "A bare grazer ignores shears");
        helper.succeed();
    }

    // ---- Shardback Brute ----

    @GameTest(template = TEMPLATE)
    public static void bruteStatsAndNbt(GameTestHelper helper) {
        ShardbackBrute brute = helper.spawnWithNoFreeWill(BeastMobs.SHARDBACK_BRUTE.get(), 2.5F, 2.0F, 2.5F);
        checkStats(helper, brute, 100, 15, 10);
        helper.assertTrue(near(brute.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), 0.9), "Knockback resistance 0.9");
        helper.assertTrue(near(brute.getAttributeValue(Attributes.MOVEMENT_SPEED), 0.22), "Speed 0.22");
        brute.setExposedTicks(33);
        helper.assertTrue(roundTrip(helper, brute).getExposedTicks() == 33, "Exposed window persists");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void bruteFrontalAndExposedDamage(GameTestHelper helper) {
        ShardbackBrute brute = helper.spawnWithNoFreeWill(BeastMobs.SHARDBACK_BRUTE.get(), 2.5F, 2.0F, 2.5F);
        brute.setYRot(0.0F);          // faces +Z (south)
        brute.yBodyRot = 0.0F;
        Zombie front = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, 2.5F, 2.0F, 4.5F);
        Zombie back = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, 2.5F, 2.0F, 0.5F);
        Zombie side = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, 0.5F, 2.0F, 2.5F);
        DamageSource fromFront = helper.getLevel().damageSources().mobAttack(front);
        DamageSource fromBack = helper.getLevel().damageSources().mobAttack(back);
        DamageSource fromSide = helper.getLevel().damageSources().mobAttack(side);

        helper.assertTrue(near(brute.modifyIncomingDamage(fromFront, 10.0F), 6.0), "Frontal hits deal x0.6");
        helper.assertTrue(near(brute.modifyIncomingDamage(fromBack, 10.0F), 10.0), "Rear hits are normal when not exposed");
        helper.assertTrue(near(brute.modifyIncomingDamage(fromSide, 10.0F), 10.0), "Side hits are normal when not exposed");

        brute.setExposedTicks(50);
        helper.assertTrue(near(brute.modifyIncomingDamage(fromBack, 10.0F), 15.0), "Rear hits deal x1.5 while exposed");
        helper.assertTrue(near(brute.modifyIncomingDamage(fromSide, 10.0F), 15.0), "Side hits deal x1.5 while exposed");
        helper.assertTrue(near(brute.modifyIncomingDamage(fromFront, 10.0F), 10.0), "Frontal hits are unmodified while exposed");
        helper.succeed();
    }

    // ---- Starfall Strider ----

    @GameTest(template = TEMPLATE)
    public static void striderStatsAndImmunity(GameTestHelper helper) {
        StarfallStrider strider = helper.spawnWithNoFreeWill(BeastMobs.STARFALL_STRIDER.get(), 2.5F, 2.0F, 2.5F);
        checkStats(helper, strider, 45, 9, 0);
        helper.assertTrue(near(strider.getAttributeValue(Attributes.MOVEMENT_SPEED), 0.32), "Speed 0.32");
        helper.assertTrue(strider.fireImmune(), "Fire immune");
        helper.assertTrue(BeastMobs.STARFALL_STRIDER.get().is(TagKey.create(Registries.ENTITY_TYPE, RelicWeave.id("starfall_immune"))),
                "Strider is in #relicweave:starfall_immune");
        helper.assertTrue(BeastMobs.STARFALL_STRIDER.get().is(VestigeKeys.VESTIGE_MOBS), "Strider is a Vestige mob");
        strider.setChargeCooldown(88);
        strider.recordTrail(new Vec3(1, 2, 3));
        StarfallStrider copy = roundTrip(helper, strider);
        helper.assertTrue(copy.getChargeCooldown() == 88, "Charge cooldown persists");
        helper.assertTrue(copy.getTrail().size() == 1, "Trail persists");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void striderTrailIgnitesPlayer(GameTestHelper helper) {
        StarfallStrider strider = helper.spawnWithNoFreeWill(BeastMobs.STARFALL_STRIDER.get(), 2.5F, 2.0F, 2.5F);
        ServerPlayer player = makePlayer(helper, new Vec3(0.5, 2.0, 0.5));
        player.clearFire();
        BeastUtil.addTestPlayer(player);
        Vec3 away = helper.absoluteVec(new Vec3(4.5, 2.0, 4.5));
        strider.recordTrail(away);
        strider.tickTrail();
        helper.assertTrue(!player.isOnFire(), "A player away from the trail is not ignited");
        strider.recordTrail(player.position().add(0.4, 0.0, 0.0));
        strider.tickTrail();
        helper.assertTrue(player.isOnFire(), "Standing within 0.8 blocks of a trail point sets a player alight");
        for (int i = 0; i < 20; i++) {
            strider.recordTrail(away);
        }
        helper.assertTrue(strider.getTrail().size() == StarfallStrider.TRAIL_MAX, "At most 12 trail points");
        for (int i = 0; i < StarfallStrider.TRAIL_LIFETIME; i++) {
            strider.tickTrail();
        }
        helper.assertTrue(strider.getTrail().isEmpty(), "Trail points expire after 40 ticks");
        BeastUtil.removeTestPlayer(player);
        helper.succeed();
    }

    // ---- Gravity Leech ----

    @GameTest(template = TEMPLATE)
    public static void leechStatsAndNbt(GameTestHelper helper) {
        GravityLeech leech = helper.spawnWithNoFreeWill(BeastMobs.GRAVITY_LEECH.get(), 2.5F, 2.0F, 2.5F);
        checkStats(helper, leech, 30, 4, 0);
        helper.assertTrue(near(leech.getAttributeValue(Attributes.MOVEMENT_SPEED), 0.22), "Speed 0.22");
        leech.setPullCooldown(66);
        helper.assertTrue(roundTrip(helper, leech).getPullCooldown() == 66, "Pull cooldown persists");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void leechPullsWithLineOfSightNotThroughWall(GameTestHelper helper) {
        GravityLeech leech = helper.spawnWithNoFreeWill(BeastMobs.GRAVITY_LEECH.get(), 0.5F, 2.0F, 2.5F);
        ServerPlayer player = makePlayer(helper, new Vec3(4.5, 2.0, 2.5));
        helper.assertTrue(leech.pull(player), "Clear line of sight: the pull happens");
        helper.assertTrue(player.getDeltaMovement().x < -0.3, "The player is pulled toward the leech (-x), got " + player.getDeltaMovement());

        player.setDeltaMovement(Vec3.ZERO);
        for (int z = 0; z <= 4; z++) {
            for (int y = 1; y <= 3; y++) {
                helper.setBlock(new BlockPos(2, y, z), Blocks.STONE);
            }
        }
        helper.assertTrue(!leech.pull(player), "A wall blocks the pull");
        helper.assertTrue(player.getDeltaMovement().lengthSqr() < 1.0E-6, "The player is not moved through a wall");
        helper.succeed();
    }

    // ---- Archive Mimic ----

    @GameTest(template = TEMPLATE)
    public static void mimicStatsAndDormantNbt(GameTestHelper helper) {
        ArchiveMimic mimic = helper.spawnWithNoFreeWill(BeastMobs.ARCHIVE_MIMIC.get(), 2.5F, 2.0F, 2.5F);
        checkStats(helper, mimic, 50, 9, 4);
        helper.assertTrue(mimic.isDormant(), "Dormant by default");
        helper.assertTrue(roundTrip(helper, mimic).isDormant(), "Dormant persists through NBT");
        mimic.setDormant(false);
        helper.assertTrue(!roundTrip(helper, mimic).isDormant(), "Awake persists through NBT");
        CompoundTag structureTag = mimic.saveWithoutId(new CompoundTag());
        structureTag.putBoolean("Dormant", true);       // what structures place
        ArchiveMimic placed = BeastMobs.ARCHIVE_MIMIC.get().create(helper.getLevel());
        placed.setDormant(false);
        placed.load(structureTag);
        helper.assertTrue(placed.isDormant(), "{Dormant:1b} makes the mimic dormant");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void mimicStaysDormantUntilPlayerWithinThree(GameTestHelper helper) {
        ArchiveMimic mimic = helper.spawn(BeastMobs.ARCHIVE_MIMIC.get(), new BlockPos(0, 2, 0));
        ServerPlayer player = makePlayer(helper, new Vec3(4.5, 2.0, 4.5));
        BeastUtil.addTestPlayer(player);
        Vec3 start = mimic.position();
        helper.runAfterDelay(20, () -> {
            helper.assertTrue(mimic.isDormant(), "A player 5+ blocks away does not wake it");
            helper.assertTrue(mimic.position().subtract(start).horizontalDistanceSqr() < 0.01, "A dormant mimic does not move");
            Vec3 close = mimic.position().add(1.5, 0.0, 1.0);
            player.moveTo(close.x, close.y, close.z);
        });
        helper.runAfterDelay(26, () -> {
            BeastUtil.removeTestPlayer(player);
            helper.assertTrue(!mimic.isDormant(), "A player within 3 blocks wakes it");
                helper.succeed();
        });
    }
}
