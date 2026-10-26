package com.relicweave.gametest;

import com.mojang.authlib.GameProfile;
import com.relicweave.RelicWeave;
import com.relicweave.vestige.cycle.CyclePhase;
import com.relicweave.vestige.entity.ethereal.ConcordanceGuardian;
import com.relicweave.vestige.entity.ethereal.CustodianSeals;
import com.relicweave.vestige.entity.ethereal.CycleHerald;
import com.relicweave.vestige.entity.ethereal.EclipseMoth;
import com.relicweave.vestige.entity.ethereal.EtherealMobs;
import com.relicweave.vestige.entity.ethereal.FractureWisp;
import com.relicweave.vestige.entity.ethereal.HeraldEvents;
import com.relicweave.vestige.entity.ethereal.MiniBossSupport;
import com.relicweave.vestige.entity.ethereal.Nullwing;
import com.relicweave.vestige.entity.ethereal.RuneWarden;
import com.relicweave.vestige.entity.ethereal.SealedCustodian;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** T08: flying, ethereal and guardian Vestige mobs. */
@GameTestHolder(RelicWeave.MOD_ID)
@PrefixGameTestTemplate(false)
public final class EtherealMobGameTests {
    private static final String TEMPLATE = "platform";

    private EtherealMobGameTests() {}

    private static boolean near(double a, double b) {
        return Math.abs(a - b) < 1.0E-3;
    }

    /** Spawns {@code type}, checks health/armor/damage, and returns the mob for round-trip checks. */
    private static <T extends Mob> T spawnAndCheck(GameTestHelper helper, EntityType<T> type, double health, double armor, Double damage) {
        T mob = helper.spawnWithNoFreeWill(type, 2.5F, 2.0F, 2.5F);
        helper.assertTrue(mob.isAlive(), type + " spawns");
        helper.assertTrue(near(mob.getAttributeValue(Attributes.MAX_HEALTH), health), "max health " + mob.getAttributeValue(Attributes.MAX_HEALTH));
        helper.assertTrue(near(mob.getAttributeValue(Attributes.ARMOR), armor), "armor " + mob.getAttributeValue(Attributes.ARMOR));
        if (damage != null) {
            helper.assertTrue(near(mob.getAttributeValue(Attributes.ATTACK_DAMAGE), damage), "damage " + mob.getAttributeValue(Attributes.ATTACK_DAMAGE));
        }
        return mob;
    }

    private static <T extends Mob> T reload(GameTestHelper helper, EntityType<T> type, T original) {
        CompoundTag tag = original.saveWithoutId(new CompoundTag());
        T copy = type.create(helper.getLevel());
        helper.assertTrue(copy != null, "creates a copy");
        copy.load(tag);
        return copy;
    }

    private static ServerPlayer makePlayer(GameTestHelper helper) {
        ServerPlayer player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "relicweave-ethereal-test"));
        player.setGameMode(GameType.SURVIVAL);
        return player;
    }

    // ---- per-mob basics ----

    @GameTest(template = TEMPLATE)
    public static void wispStatsAndNbt(GameTestHelper helper) {
        FractureWisp wisp = spawnAndCheck(helper, EtherealMobs.FRACTURE_WISP.get(), 14.0, 0.0, 4.0);
        wisp.registerDash();
        FractureWisp copy = reload(helper, EtherealMobs.FRACTURE_WISP.get(), wisp);
        helper.assertTrue(copy.dashesInRow() == 1, "dash count survives NBT, got " + copy.dashesInRow());
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void nullwingStatsAndNbt(GameTestHelper helper) {
        Nullwing wing = spawnAndCheck(helper, EtherealMobs.NULLWING.get(), 24.0, 0.0, 6.0);
        wing.completeCircle();
        wing.completeCircle();
        Nullwing copy = reload(helper, EtherealMobs.NULLWING.get(), wing);
        helper.assertTrue(copy.circlesWithoutDive() == 2, "circle count survives NBT, got " + copy.circlesWithoutDive());
        wing.land();
        Nullwing landed = reload(helper, EtherealMobs.NULLWING.get(), wing);
        helper.assertTrue(landed.mode() == Nullwing.Mode.LAND && landed.modeTicks() == Nullwing.LAND_TICKS, "landing state survives NBT");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void mothStatsAndNbt(GameTestHelper helper) {
        EclipseMoth moth = spawnAndCheck(helper, EtherealMobs.ECLIPSE_MOTH.get(), 6.0, 0.0, null);
        helper.assertTrue(moth.getType().getCategory() == net.minecraft.world.entity.MobCategory.AMBIENT, "ambient category");
        helper.assertTrue(reload(helper, EtherealMobs.ECLIPSE_MOTH.get(), moth).isAlive(), "round-trips");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void guardianStatsAndNbt(GameTestHelper helper) {
        ConcordanceGuardian guardian = spawnAndCheck(helper, EtherealMobs.CONCORDANCE_GUARDIAN.get(), 220.0, 10.0, 10.0);
        helper.assertTrue(near(guardian.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), 1.0), "full knockback resistance");
        guardian.support().setHome(new BlockPos(7, 8, 9));
        guardian.setHealth(50.0F);
        guardian.tickCount = 1;
        helper.runAfterDelay(2, () -> {
            ConcordanceGuardian copy = reload(helper, EtherealMobs.CONCORDANCE_GUARDIAN.get(), guardian);
            helper.assertTrue(new BlockPos(7, 8, 9).equals(copy.support().home()), "home survives NBT, got " + copy.support().home());
            helper.assertTrue(copy.getHealth() == 50.0F, "health survives NBT");
            helper.assertTrue(copy.isPersistenceRequired(), "persistent");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE)
    public static void custodianStatsAndNbt(GameTestHelper helper) {
        helper.setBlock(new BlockPos(1, 2, 1), Blocks.CHEST);
        SealedCustodian custodian = spawnAndCheck(helper, EtherealMobs.SEALED_CUSTODIAN.get(), 160.0, 8.0, 9.0);
        custodian.recordContainers();
        helper.assertTrue(custodian.sealedCount() == 1, "records the chest, got " + custodian.sealedCount());
        SealedCustodian copy = reload(helper, EtherealMobs.SEALED_CUSTODIAN.get(), custodian);
        helper.assertTrue(copy.sealedCount() == 1, "sealed list survives NBT");
        helper.assertTrue(copy.isSealing(helper.absolutePos(new BlockPos(1, 2, 1))), "sealed position survives NBT");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void wardenStatsAndNbt(GameTestHelper helper) {
        RuneWarden warden = spawnAndCheck(helper, EtherealMobs.RUNE_WARDEN.get(), 180.0, 8.0, 11.0);
        helper.assertTrue(warden.isDormant(), "spawns dormant");
        helper.assertTrue(reload(helper, EtherealMobs.RUNE_WARDEN.get(), warden).isDormant(), "Dormant survives NBT");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void heraldStatsAndNbt(GameTestHelper helper) {
        CycleHerald herald = spawnAndCheck(helper, EtherealMobs.CYCLE_HERALD.get(), 60.0, 0.0, 8.0);
        herald.setEventBound(true);
        CycleHerald copy = reload(helper, EtherealMobs.CYCLE_HERALD.get(), herald);
        helper.assertTrue(copy.isEventBound(), "event-bound flag survives NBT");
        helper.succeed();
    }

    // ---- signature behaviour ----

    @GameTest(template = TEMPLATE)
    public static void wispNeverDashesMoreThanTwice(GameTestHelper helper) {
        FractureWisp wisp = helper.spawnWithNoFreeWill(EtherealMobs.FRACTURE_WISP.get(), 2.5F, 2.0F, 2.5F);
        int dashes = 0;
        for (int i = 0; i < 6; i++) {
            if (wisp.canDash()) {
                wisp.registerDash();
                dashes++;
            }
            for (int t = 0; t < FractureWisp.GAP_TICKS + 1; t++) {
                wisp.tickDashTimers();
            }
            if (wisp.cooldownTicks() > 0) {
                break;
            }
        }
        helper.assertTrue(dashes == 2, "exactly two dashes before the cooldown, got " + dashes);
        helper.assertTrue(wisp.cooldownTicks() > 0, "cooldown running");
        helper.assertFalse(wisp.canDash(), "no third dash during the cooldown");
        for (int t = 0; t < FractureWisp.COOLDOWN_TICKS; t++) {
            wisp.tickDashTimers();
        }
        helper.assertTrue(wisp.canDash(), "dashing is available again after the cooldown");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void nullwingLandsAfterThreeCircles(GameTestHelper helper) {
        Nullwing wing = helper.spawnWithNoFreeWill(EtherealMobs.NULLWING.get(), 2.5F, 2.0F, 2.5F);
        helper.assertTrue(wing.mode() == Nullwing.Mode.CIRCLE, "starts circling");
        wing.completeCircle();
        wing.completeCircle();
        helper.assertTrue(wing.mode() == Nullwing.Mode.CIRCLE, "two dive-less circles keep circling");
        wing.completeCircle();
        helper.assertTrue(wing.mode() == Nullwing.Mode.LAND, "third dive-less circle lands it");
        for (int i = 0; i < Nullwing.LAND_TICKS - 1; i++) {
            wing.tickLanding();
        }
        helper.assertTrue(wing.mode() == Nullwing.Mode.LAND, "still grounded before 60 ticks");
        wing.tickLanding();
        helper.assertTrue(wing.mode() == Nullwing.Mode.CLIMB, "takes off after 60 grounded ticks");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void mothPrefersFissureDuringResonance(GameTestHelper helper) {
        BlockPos me = new BlockPos(0, 64, 0);
        BlockPos fissure = new BlockPos(20, 64, 0);
        BlockPos anchor = new BlockPos(5, 64, 0);
        BlockPos ward = new BlockPos(0, 64, 30);
        Optional<BlockPos> resonance = EclipseMoth.selectTarget(CyclePhase.RESONANCE, me, List.of(fissure, ward), List.of(anchor));
        helper.assertTrue(resonance.isPresent() && resonance.get().equals(fissure), "Resonance prefers the nearest fissure/ward over a nearer anchor");
        Optional<BlockPos> convergence = EclipseMoth.selectTarget(CyclePhase.CONVERGENCE, me, List.of(fissure, ward), List.of(anchor));
        helper.assertTrue(convergence.isPresent() && convergence.get().equals(anchor), "Convergence picks the nearest of all");
        helper.assertTrue(EclipseMoth.selectTarget(CyclePhase.QUIET, me, List.of(fissure), List.of(anchor)).isEmpty(), "Quiet wanders");
        helper.assertTrue(EclipseMoth.selectTarget(CyclePhase.RESONANCE, me, List.of(new BlockPos(60, 64, 0)), List.of()).isEmpty(), "beyond 48 blocks is ignored");
        helper.assertTrue(EclipseMoth.selectTarget(CyclePhase.RESONANCE, me, List.of(), List.of(anchor)).get().equals(anchor), "falls back to an anchor");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void guardianPhasesAndWindupScaling(GameTestHelper helper) {
        helper.assertTrue(ConcordanceGuardian.phaseFor(1.0F) == 1 && ConcordanceGuardian.phaseFor(0.61F) == 1, "above 60% is phase 1");
        helper.assertTrue(ConcordanceGuardian.phaseFor(0.6F) == 2 && ConcordanceGuardian.phaseFor(0.3F) == 2, "60% to 30% is phase 2");
        helper.assertTrue(ConcordanceGuardian.phaseFor(0.29F) == 3, "below 30% is phase 3");
        helper.assertTrue(ConcordanceGuardian.windupFor(20, 1) == 20 && ConcordanceGuardian.windupFor(25, 2) == 25, "no scaling before phase 3");
        helper.assertTrue(ConcordanceGuardian.windupFor(20, 3) == 15 && ConcordanceGuardian.windupFor(25, 3) == 19, "x0.75 in phase 3");
        helper.assertTrue(ConcordanceGuardian.windupFor(14, 3) == 12 && ConcordanceGuardian.windupFor(10, 3) == 12, "never below 12 ticks");
        ConcordanceGuardian guardian = helper.spawnWithNoFreeWill(EtherealMobs.CONCORDANCE_GUARDIAN.get(), 2.5F, 2.0F, 2.5F);
        helper.assertTrue(guardian.phase() == 1, "full health is phase 1");
        guardian.setHealth(110.0F);
        helper.assertTrue(guardian.phase() == 2, "50% is phase 2");
        guardian.setHealth(40.0F);
        helper.assertTrue(guardian.phase() == 3, "18% is phase 3");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void custodianBlocksChestWhileAlive(GameTestHelper helper) {
        BlockPos chest = new BlockPos(1, 2, 1);
        helper.setBlock(chest, Blocks.CHEST);
        SealedCustodian custodian = helper.spawnWithNoFreeWill(EtherealMobs.SEALED_CUSTODIAN.get(), 2.5F, 2.0F, 2.5F);
        custodian.recordContainers();
        ServerPlayer player = makePlayer(helper);
        BlockPos abs = helper.absolutePos(chest);
        player.moveTo(abs.getX() + 0.5, abs.getY() + 1.0, abs.getZ() + 0.5);

        PlayerInteractEvent.RightClickBlock alive = click(player, abs);
        CustodianSeals.onRightClick(alive);
        helper.assertTrue(alive.isCanceled(), "opening the chest is cancelled while the custodian lives");

        custodian.kill();
        helper.assertFalse(custodian.isAlive(), "custodian is dead");
        PlayerInteractEvent.RightClickBlock dead = click(player, abs);
        CustodianSeals.onRightClick(dead);
        helper.assertFalse(dead.isCanceled(), "the chest opens after the custodian dies");
        helper.succeed();
    }

    private static PlayerInteractEvent.RightClickBlock click(ServerPlayer player, BlockPos abs) {
        return new PlayerInteractEvent.RightClickBlock(player, InteractionHand.MAIN_HAND, abs,
                new BlockHitResult(Vec3.atCenterOf(abs), Direction.UP, abs, false));
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void custodianShieldIsNotChained(GameTestHelper helper) {
        SealedCustodian custodian = helper.spawnWithNoFreeWill(EtherealMobs.SEALED_CUSTODIAN.get(), 2.5F, 2.0F, 2.5F);
        helper.assertTrue(custodian.shieldCooldown() == SealedCustodian.SHIELD_INTERVAL, "a fresh custodian waits a full interval before its first shield");
        helper.assertTrue(SealedCustodian.SHIELD_INTERVAL > SealedCustodian.SHIELD_TICKS * 3, "shield gap is much longer than the shield");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void wardenStaysDormantUntilAwakened(GameTestHelper helper) {
        RuneWarden warden = helper.spawn(EtherealMobs.RUNE_WARDEN.get(), 2.5F, 2.0F, 2.5F);
        helper.assertTrue(warden.isDormant(), "dormant at spawn");
        helper.assertTrue(warden.isInvulnerableTo(warden.damageSources().generic()), "invulnerable while dormant");
        float before = warden.getHealth();
        helper.assertFalse(warden.hurt(warden.damageSources().generic(), 20.0F), "damage is refused while dormant");
        helper.assertTrue(warden.getHealth() == before, "health unchanged");
        warden.beginAwakening();
        helper.assertTrue(warden.isAwakening() && warden.isDormant(), "rune sequence started");
        helper.runAfterDelay(RuneWarden.AWAKEN_TICKS - 10, () -> {
            helper.assertTrue(warden.isDormant(), "still awakening before 60 ticks");
            helper.assertFalse(warden.hurt(warden.damageSources().generic(), 20.0F), "invulnerable during the sequence");
        });
        helper.runAfterDelay(RuneWarden.AWAKEN_TICKS + 10, () -> {
            helper.assertFalse(warden.isDormant(), "awake after the 60-tick sequence");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE)
    public static void miniBossResetHealsAndReturnsHome(GameTestHelper helper) {
        ConcordanceGuardian guardian = helper.spawnWithNoFreeWill(EtherealMobs.CONCORDANCE_GUARDIAN.get(), 2.5F, 2.0F, 2.5F);
        BlockPos home = guardian.blockPosition();
        guardian.support().setHome(home);
        guardian.setHealth(30.0F);
        guardian.moveTo(home.getX() + 4.5, home.getY(), home.getZ() + 4.5);
        guardian.support().reset();
        helper.assertTrue(guardian.getHealth() == guardian.getMaxHealth(), "healed to full");
        helper.assertTrue(guardian.getTarget() == null, "target cleared");
        helper.assertTrue(guardian.position().distanceTo(Vec3.atBottomCenterOf(home)) < 0.01, "back at home, at " + guardian.position());
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void miniBossResetsWhenNoPlayerIsNear(GameTestHelper helper) {
        MiniBossSupport.overrideResetTicksForTests(20);
        ConcordanceGuardian guardian = helper.spawnWithNoFreeWill(EtherealMobs.CONCORDANCE_GUARDIAN.get(), 2.5F, 2.0F, 2.5F);
        guardian.setHealth(10.0F);
        helper.runAfterDelay(60, () -> {
            MiniBossSupport.overrideResetTicksForTests(null);
            helper.assertTrue(guardian.support().resets() >= 1, "reset ran after the idle delay");
            helper.assertTrue(guardian.getHealth() == guardian.getMaxHealth(), "healed to full, got " + guardian.getHealth());
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE)
    public static void heraldRefusesSecondWithin64(GameTestHelper helper) {
        Vec3 spot = helper.absoluteVec(new Vec3(2.5, 2.0, 2.5));
        // Heralds left over by neighbouring tests would count; clear them first.
        helper.getLevel().getEntitiesOfClass(CycleHerald.class, new net.minecraft.world.phys.AABB(spot, spot).inflate(200.0)).forEach(h -> h.discard());
        helper.assertTrue(HeraldEvents.canSpawnAt(helper.getLevel(), spot), "no Herald yet");
        helper.spawnWithNoFreeWill(EtherealMobs.CYCLE_HERALD.get(), 2.5F, 2.0F, 2.5F);
        helper.assertFalse(HeraldEvents.canSpawnAt(helper.getLevel(), spot), "refuses at the same spot");
        helper.assertFalse(HeraldEvents.canSpawnAt(helper.getLevel(), spot.add(50.0, 0.0, 0.0)), "refuses 50 blocks away");
        helper.assertTrue(HeraldEvents.canSpawnAt(helper.getLevel(), spot.add(70.0, 0.0, 0.0)), "allows 70 blocks away");
        helper.succeed();
    }
}
