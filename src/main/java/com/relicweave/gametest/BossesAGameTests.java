package com.relicweave.gametest;

import com.mojang.authlib.GameProfile;
import com.relicweave.RelicWeave;
import com.relicweave.vestige.boss.BossAltarBlockEntity;
import com.relicweave.vestige.boss.BossRegistry;
import com.relicweave.vestige.boss.BossRewards;
import com.relicweave.vestige.boss.VestigeBoss;
import com.relicweave.vestige.boss.a.BossesA;
import com.relicweave.vestige.boss.a.ChoirUnheard;
import com.relicweave.vestige.boss.a.ThornRoot;
import com.relicweave.vestige.boss.a.ThornSpike;
import com.relicweave.vestige.boss.a.ThornedRegent;
import com.relicweave.vestige.entity.beast.GlassrootStalker;
import com.relicweave.vestige.registry.VestigeMaterials;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Server-side tests for the Thorned Regent and the Choir Unheard (T11). Arena template: 21x6x21 stone floor. */
@GameTestHolder(RelicWeave.MOD_ID)
@PrefixGameTestTemplate(false)
public final class BossesAGameTests {
    private static final String TEMPLATE = "bosses_a_arena";
    private static final List<Player> MOCKS = new ArrayList<>();

    private BossesAGameTests() {}

    // ---- helpers ----

    private static boolean near(double a, double b) {
        return Math.abs(a - b) < 1.0E-3;
    }

    /** A survival mock player standing still at the given template-relative position, present in the level. */
    private static Player mock(GameTestHelper helper, double x, double y, double z) {
        Player player = helper.makeMockSurvivalPlayer();
        Vec3 pos = helper.absoluteVec(new Vec3(x, y, z));
        player.moveTo(pos.x, pos.y, pos.z, 0.0F, 0.0F);
        player.setNoGravity(true);
        player.setDeltaMovement(Vec3.ZERO);
        helper.getLevel().addFreshEntity(player);
        MOCKS.add(player);
        return player;
    }

    private static void discardMocks() {
        for (Player p : MOCKS) {
            p.discard();
        }
        MOCKS.clear();
    }

    private static ServerPlayer fakePlayer(GameTestHelper helper) {
        ServerPlayer player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "relicweave-bossa-test"));
        player.setGameMode(GameType.SURVIVAL);
        Vec3 pos = helper.absoluteVec(new Vec3(10.5, 2.0, 10.5));
        player.moveTo(pos.x, pos.y, pos.z, 0.0F, 0.0F);
        return player;
    }

    /** Removes leftovers of earlier tests (bosses, stalkers, roots, thorns) from the arena. */
    private static void sweep(GameTestHelper helper) {
        AABB box = new AABB(helper.absolutePos(new BlockPos(10, 3, 10))).inflate(12.0, 8.0, 12.0);
        for (Class<? extends Entity> type : List.of(ThornedRegent.class, ChoirUnheard.class, GlassrootStalker.class, ThornRoot.class, ThornSpike.class)) {
            helper.getLevel().getEntitiesOfClass(type, box).forEach(Entity::discard);
        }
    }

    private static ThornedRegent regent(GameTestHelper helper, double x, double y, double z) {
        sweep(helper);
        ThornedRegent boss = helper.spawnWithNoFreeWill(BossesA.THORNED_REGENT.get(), (float) x, (float) y, (float) z);
        boss.bindArena(helper.absolutePos(new BlockPos(10, 2, 10)), null);
        return boss;
    }

    private static ChoirUnheard choir(GameTestHelper helper, double x, double y, double z) {
        sweep(helper);
        ChoirUnheard boss = helper.spawnWithNoFreeWill(BossesA.CHOIR_UNHEARD.get(), (float) x, (float) y, (float) z);
        boss.bindArena(helper.absolutePos(new BlockPos(10, 2, 10)), null);
        return boss;
    }

    private static <T extends VestigeBoss> T roundTrip(GameTestHelper helper, T boss) {
        CompoundTag tag = boss.saveWithoutId(new CompoundTag());
        @SuppressWarnings("unchecked")
        T copy = (T) boss.getType().create(helper.getLevel());
        copy.load(tag);
        return copy;
    }

    private static VestigeBoss altarSpawn(GameTestHelper helper, String boss) {
        sweep(helper);
        BlockPos altarPos = new BlockPos(10, 1, 6);
        helper.setBlock(altarPos, BossRegistry.BOSS_ALTAR.get());
        BossAltarBlockEntity altar = (BossAltarBlockEntity) helper.getBlockEntity(altarPos);
        altar.setBossType(RelicWeave.id(boss));
        VestigeBoss spawned = altar.spawnBoss(helper.getLevel());
        helper.assertTrue(spawned != null, "Altar should spawn " + boss);
        return spawned;
    }

    /**
     * Kills the boss with a player attack. {@code PlayerAdvancements#award} reports false for fake players in this
     * environment (see GateGameTests), so the reward hook is verified through the altar notification, and the
     * idempotence through a progress completed directly: a completed advancement is never awarded again.
     */
    private static void killAndCheckRewards(GameTestHelper helper, VestigeBoss boss) {
        BossAltarBlockEntity altar = (BossAltarBlockEntity) helper.getBlockEntity(new BlockPos(10, 1, 6));
        ServerPlayer killer = fakePlayer(helper);
        helper.assertFalse(BossRewards.hasDefeated(killer, boss.bossId()), "Not yet defeated");
        helper.assertFalse(altar.defeatedOnce(), "Altar not yet defeated");
        boss.hurt(helper.getLevel().damageSources().playerAttack(killer), Float.MAX_VALUE);
        helper.assertTrue(boss.isDeadOrDying(), "The boss should die");
        helper.assertTrue(altar.defeatedOnce(), "The death reward hook should notify the altar");
        Advancement advancement = killer.server.getAdvancements().getAdvancement(BossRewards.advancementId(boss.bossId()));
        helper.assertTrue(advancement != null, "The boss advancement should exist");
        AdvancementProgress progress = killer.getAdvancements().getOrStartProgress(advancement);
        helper.assertTrue(progress.getRemainingCriteria().iterator().hasNext(), "Advancement has the defeated criterion");
        List<String> remaining = new ArrayList<>();
        progress.getRemainingCriteria().forEach(remaining::add);
        helper.assertTrue(remaining.contains(BossRewards.CRITERION), "Criterion is named 'defeated'");
        remaining.forEach(progress::grantProgress);
        helper.assertTrue(BossRewards.hasDefeated(killer, boss.bossId()), "Completed advancement counts as defeated");
        helper.assertFalse(BossRewards.award(killer, boss.bossId()), "A second award returns false");
    }

    private static int countNear(GameTestHelper helper, Class<? extends Entity> type) {
        AABB box = new AABB(helper.absolutePos(new BlockPos(10, 3, 10))).inflate(12.0, 8.0, 12.0);
        return helper.getLevel().getEntitiesOfClass(type, box).size();
    }

    private static void assertDamaged(GameTestHelper helper, Player player, boolean expected, String label) {
        boolean damaged = player.getHealth() < player.getMaxHealth();
        helper.assertTrue(damaged == expected, label + (expected ? " should be damaged" : " should be unharmed") + " (health " + player.getHealth() + ")");
    }

    private static void rollLoot(GameTestHelper helper, VestigeBoss boss, String table, int[][] expected, Item[] items) {
        ServerLevel level = helper.getLevel();
        LootTable loot = level.getServer().getLootData().getLootTable(RelicWeave.id("entities/" + table));
        for (long seed = 1; seed <= 20; seed++) {
            LootParams params = new LootParams.Builder(level)
                    .withParameter(LootContextParams.THIS_ENTITY, boss)
                    .withParameter(LootContextParams.ORIGIN, boss.position())
                    .withParameter(LootContextParams.DAMAGE_SOURCE, level.damageSources().generic())
                    .create(LootContextParamSets.ENTITY);
            List<ItemStack> drops = loot.getRandomItems(params, seed);
            for (int i = 0; i < items.length; i++) {
                int count = 0;
                for (ItemStack stack : drops) {
                    if (stack.is(items[i])) {
                        count += stack.getCount();
                    }
                }
                helper.assertTrue(count >= expected[i][0] && count <= expected[i][1],
                        table + " seed " + seed + ": " + items[i] + " x" + count + " outside " + expected[i][0] + ".." + expected[i][1]);
            }
        }
    }

    // ---- Thorned Regent ----

    @GameTest(template = TEMPLATE)
    public static void regentSpawnsFromAltarWithStats(GameTestHelper helper) {
        VestigeBoss boss = altarSpawn(helper, "thorned_regent");
        helper.assertTrue(boss instanceof ThornedRegent, "Altar should spawn the Thorned Regent");
        ThornedRegent regent = (ThornedRegent) boss;
        helper.assertTrue(regent.home() != null && regent.altar() != null, "Arena should be bound to the altar");
        helper.assertTrue(regent.altar().equals(helper.absolutePos(new BlockPos(10, 1, 6))), "Altar position should be remembered");
        helper.assertTrue(regent.bar() != null && regent.bar().getColor() == BossEvent.BossBarColor.GREEN, "Boss bar should be green");
        helper.assertTrue(near(regent.getMaxHealth(), 300), "Max health 300");
        helper.assertTrue(near(regent.getAttributeValue(Attributes.ARMOR), 10), "Armor 10");
        helper.assertTrue(near(regent.getAttributeValue(Attributes.ATTACK_DAMAGE), 12), "Damage 12");
        helper.assertTrue(near(regent.getAttributeValue(Attributes.MOVEMENT_SPEED), 0.24), "Speed 0.24");
        helper.assertTrue(near(regent.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), 0.8), "Knockback resistance 0.8");
        helper.assertTrue(regent.bossId().equals(RelicWeave.id("thorned_regent")), "Boss id");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void regentVolleyTelegraphsThenFiresFan(GameTestHelper helper) {
        try {
            ThornedRegent regent = regent(helper, 10.5, 2.0, 3.5);
            Player target = mock(helper, 10.5, 2.0, 14.5);
            regent.setTarget(target);
            helper.assertTrue(regent.beginMove(ThornedRegent.VOLLEY), "Volley should start");
            helper.assertTrue(regent.move() == ThornedRegent.VOLLEY && regent.moveTimer() == ThornedRegent.VOLLEY_WINDUP, "Wind-up is 20 ticks");
            for (int i = 0; i < ThornedRegent.VOLLEY_WINDUP - 1; i++) {
                regent.tickBrain();
            }
            helper.assertTrue(countNear(helper, ThornSpike.class) == 0, "No thorns during the wind-up");
            assertDamaged(helper, target, false, "Target during wind-up");
            regent.tickBrain();
            helper.assertTrue(countNear(helper, ThornSpike.class) == 5, "Five thorns after the wind-up, found " + countNear(helper, ThornSpike.class));
            helper.assertTrue(regent.move() == ThornedRegent.IDLE, "Back to idle after the volley");
            helper.assertTrue(regent.cooldown(ThornedRegent.VOLLEY) > 0, "Volley goes on cooldown");
            helper.succeed();
        } finally {
            discardMocks();
        }
    }

    @GameTest(template = TEMPLATE)
    public static void regentStrikeHitsOnlyInsideRingThenExposesCore(GameTestHelper helper) {
        try {
            ThornedRegent regent = regent(helper, 10.5, 2.0, 3.5);
            Player inside = mock(helper, 10.5, 2.0, 14.5);
            Player leaver = mock(helper, 11.5, 2.0, 14.5);
            Player far = mock(helper, 10.5, 2.0, 19.5);
            regent.setTarget(inside);
            helper.assertTrue(regent.beginMove(ThornedRegent.STRIKE), "Strike should start");
            helper.assertTrue(near(regent.mark().z, inside.getZ()), "Ring is marked under the target at wind-up start");
            for (int i = 0; i < ThornedRegent.STRIKE_WINDUP - 1; i++) {
                if (i == 10) {
                    Vec3 out = helper.absoluteVec(new Vec3(16.5, 2.0, 14.5));
                    leaver.moveTo(out.x, out.y, out.z);
                }
                regent.tickBrain();
            }
            assertDamaged(helper, inside, false, "Player during the wind-up");
            helper.assertFalse(regent.isExposed(), "Core is hidden during the wind-up");
            regent.tickBrain();
            assertDamaged(helper, inside, true, "Player inside the ring");
            assertDamaged(helper, leaver, false, "Player who left the ring");
            assertDamaged(helper, far, false, "Player outside the ring");
            helper.assertTrue(regent.isExposed() && regent.exposedTicks() == ThornedRegent.CORE_TICKS, "Core exposed for 60 ticks");
            helper.succeed();
        } finally {
            discardMocks();
        }
    }

    @GameTest(template = TEMPLATE)
    public static void regentExposedCoreDoublesDamageAndStopsAttacks(GameTestHelper helper) {
        try {
            ThornedRegent regent = regent(helper, 10.5, 2.0, 10.5);
            Player victim = mock(helper, 10.5, 2.0, 11.5);
            float before = regent.getHealth();
            regent.hurt(regent.damageSources().magic(), 4.0F);
            float normal = before - regent.getHealth();
            regent.setHealth(before);
            regent.invulnerableTime = 0;
            regent.setExposed(ThornedRegent.CORE_TICKS);
            regent.hurt(regent.damageSources().magic(), 4.0F);
            float exposed = before - regent.getHealth();
            helper.assertTrue(near(normal, 4.0), "Normal damage should be 4, was " + normal);
            helper.assertTrue(near(exposed, 8.0), "Exposed damage should be 8, was " + exposed);
            helper.assertFalse(regent.doHurtTarget(victim), "No melee while the core is exposed");
            assertDamaged(helper, victim, false, "Victim of an exposed Regent");
            helper.succeed();
        } finally {
            discardMocks();
        }
    }

    @GameTest(template = TEMPLATE)
    public static void regentWallTelegraphsThenRootsBlockPath(GameTestHelper helper) {
        try {
            ThornedRegent regent = regent(helper, 10.5, 2.0, 3.5);
            Player target = mock(helper, 10.5, 2.0, 15.5);
            Player walker = mock(helper, 10.5, 2.0, 7.0);
            regent.setTarget(target);
            helper.assertTrue(regent.beginMove(ThornedRegent.WALL), "Wall should start");
            helper.assertTrue(regent.moveTimer() == ThornedRegent.WALL_WINDUP, "Wall wind-up is 30 ticks");
            for (int i = 0; i < ThornedRegent.WALL_WINDUP - 1; i++) {
                regent.tickBrain();
            }
            helper.assertTrue(countNear(helper, ThornRoot.class) == 0, "No roots during the wind-up");
            regent.tickBrain();
            int roots = countNear(helper, ThornRoot.class);
            helper.assertTrue(roots == ThornedRegent.WALL_SEGMENTS, "A full wall of 15 roots expected, found " + roots);
            double before = walker.getZ();
            walker.move(MoverType.SELF, new Vec3(0.0, 0.0, 6.0));
            helper.assertTrue(walker.getZ() < helper.absoluteVec(new Vec3(10.5, 2.0, 9.6)).z,
                    "The wall should stop the walker (moved from " + before + " to " + walker.getZ() + ")");
            helper.runAfterDelay(8, () -> {
                try {
                    assertDamaged(helper, walker, true, "Walker pressed against the roots");
                    helper.succeed();
                } finally {
                    discardMocks();
                }
            });
        } catch (RuntimeException e) {
            discardMocks();
            throw e;
        }
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 320)
    public static void regentRootsDespawnAndNeverTrapAPlayer(GameTestHelper helper) {
        try {
            ThornedRegent regent = regent(helper, 10.5, 2.0, 3.5);
            Player standing = mock(helper, 10.5, 2.0, 9.5);
            int placed = regent.buildWall(helper.absoluteVec(new Vec3(10.5, 2.0, 9.5)), new Vec3(1.0, 0.0, 0.0));
            helper.assertTrue(placed < ThornedRegent.WALL_SEGMENTS && placed >= 10, "Occupied spots are skipped, placed " + placed);
            for (ThornRoot root : regent.ownedRoots()) {
                helper.assertFalse(root.getBoundingBox().intersects(standing.getBoundingBox()), "No root may overlap a player");
            }
            helper.runAfterDelay(ThornRoot.LIFETIME_TICKS + 10, () -> {
                try {
                    helper.assertTrue(countNear(helper, ThornRoot.class) == 0, "Roots despawn after 200 ticks");
                    helper.succeed();
                } finally {
                    discardMocks();
                }
            });
        } catch (RuntimeException e) {
            discardMocks();
            throw e;
        }
    }

    @GameTest(template = TEMPLATE)
    public static void regentAllowsAtMostTwoWalls(GameTestHelper helper) {
        ThornedRegent regent = regent(helper, 10.5, 2.0, 3.5);
        regent.buildWall(helper.absoluteVec(new Vec3(10.5, 2.0, 8.5)), new Vec3(1.0, 0.0, 0.0));
        helper.assertTrue(regent.canWall(), "One wall standing: a second is allowed");
        regent.buildWall(helper.absoluteVec(new Vec3(10.5, 2.0, 12.5)), new Vec3(1.0, 0.0, 0.0));
        helper.assertTrue(regent.activeWallCount() == 2, "Two walls standing, counted " + regent.activeWallCount());
        helper.assertFalse(regent.canWall(), "A third wall is refused");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void regentStalkerCallRespectsCaps(GameTestHelper helper) {
        ThornedRegent regent = regent(helper, 10.5, 2.0, 10.5);
        helper.assertFalse(regent.canCall(), "No Stalker Call in phase 1");
        regent.setHealth(regent.getMaxHealth() * 0.4F);
        regent.updatePhase();
        helper.assertTrue(regent.phase() == 2, "Phase 2 at 40% health");
        helper.assertTrue(regent.canCall(), "Stalker Call available in phase 2");
        helper.assertTrue(regent.summonStalkers() == 2, "First call summons 2");
        helper.assertTrue(regent.summonStalkers() == 0, "Never more than 2 alive");
        helper.assertFalse(regent.canCall(), "No call while 2 are alive");
        helper.assertTrue(countNear(helper, GlassrootStalker.class) == 2, "Two stalkers in the arena");
        for (GlassrootStalker s : helper.getLevel().getEntitiesOfClass(GlassrootStalker.class,
                new AABB(helper.absolutePos(new BlockPos(10, 3, 10))).inflate(12.0, 8.0, 12.0))) {
            helper.assertTrue(s.isPersistenceRequired(), "Summons are persistent");
            s.discard();
        }
        helper.assertTrue(regent.summonStalkers() == 2, "After they die, two more");
        helper.assertTrue(regent.stalkersSummoned() == 4, "Four summoned so far");
        for (GlassrootStalker s : helper.getLevel().getEntitiesOfClass(GlassrootStalker.class,
                new AABB(helper.absolutePos(new BlockPos(10, 3, 10))).inflate(12.0, 8.0, 12.0))) {
            s.discard();
        }
        helper.assertTrue(regent.summonStalkers() == 0 && !regent.canCall(), "No more than 4 per fight");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void regentCallTelegraphsFortyTicks(GameTestHelper helper) {
        ThornedRegent regent = regent(helper, 10.5, 2.0, 10.5);
        regent.setHealth(regent.getMaxHealth() * 0.4F);
        regent.updatePhase();
        helper.assertTrue(regent.beginMove(ThornedRegent.CALL), "Call should start in phase 2");
        for (int i = 0; i < ThornedRegent.CALL_WINDUP - 1; i++) {
            regent.tickBrain();
        }
        helper.assertTrue(countNear(helper, GlassrootStalker.class) == 0, "No stalkers during the wind-up");
        regent.tickBrain();
        helper.assertTrue(countNear(helper, GlassrootStalker.class) == 2, "Two stalkers after the wind-up");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void regentPhasesSwitchAtHalfHealth(GameTestHelper helper) {
        ThornedRegent regent = regent(helper, 10.5, 2.0, 10.5);
        regent.setHealth(regent.getMaxHealth() * 0.7F);
        regent.updatePhase();
        helper.assertTrue(regent.phase() == 1, "Phase 1 above 50%");
        regent.setHealth(regent.getMaxHealth() * 0.45F);
        regent.updatePhase();
        helper.assertTrue(regent.phase() == 2, "Phase 2 at or below 50%");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void regentNbtRoundTripKeepsState(GameTestHelper helper) {
        ThornedRegent regent = regent(helper, 10.5, 2.0, 10.5);
        regent.setHealth(regent.getMaxHealth() * 0.4F);
        regent.updatePhase();
        regent.summonStalkers();
        regent.setCooldown(ThornedRegent.WALL, 77);
        regent.setExposed(33);
        ThornedRegent copy = roundTrip(helper, regent);
        helper.assertTrue(copy.phase() == 2, "Phase kept");
        helper.assertTrue(copy.exposedTicks() == 33 && copy.isExposed(), "Exposure kept");
        helper.assertTrue(copy.cooldown(ThornedRegent.WALL) == 77, "Cooldowns kept");
        helper.assertTrue(copy.stalkersSummoned() == 2, "Summon total kept");
        helper.assertTrue(copy.home() != null && copy.home().equals(regent.home()), "Home kept");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 160)
    public static void regentResetsWithoutPlayers(GameTestHelper helper) {
        VestigeBoss.overrideResetTicksForTests(20);
        try {
            ThornedRegent regent = (ThornedRegent) altarSpawn(helper, "thorned_regent");
            regent.setHealth(40.0F);
            regent.summonStalkers();
            regent.buildWall(regent.position().add(0.0, 0.0, 3.0), new Vec3(1.0, 0.0, 0.0));
            helper.assertTrue(countNear(helper, GlassrootStalker.class) == 2, "Stalkers present before the reset");
            helper.runAfterDelay(70, () -> {
                try {
                    helper.assertTrue(regent.getHealth() >= regent.getMaxHealth() - 0.01F, "Heals to full, was " + regent.getHealth());
                    helper.assertTrue(countNear(helper, GlassrootStalker.class) == 0, "Summons removed");
                    helper.assertTrue(countNear(helper, ThornRoot.class) == 0, "Roots removed");
                    helper.succeed();
                } finally {
                    VestigeBoss.overrideResetTicksForTests(null);
                }
            });
        } catch (RuntimeException e) {
            VestigeBoss.overrideResetTicksForTests(null);
            throw e;
        }
    }

    @GameTest(template = TEMPLATE)
    public static void regentDeathAwardsAdvancementOnceAndDropsGuaranteedLoot(GameTestHelper helper) {
        ThornedRegent regent = (ThornedRegent) altarSpawn(helper, "thorned_regent");
        rollLoot(helper, regent, "thorned_regent", new int[][]{{2, 3}, {2, 4}, {1, 1}},
                new Item[]{VestigeMaterials.SOVEREIGN_THORN.get(), VestigeMaterials.AETHERITE_INGOT.get(), VestigeMaterials.MNEMONIC_FILAMENT.get()});
        killAndCheckRewards(helper, regent);
        helper.succeed();
    }

    // ---- Choir Unheard ----

    @GameTest(template = TEMPLATE)
    public static void choirSpawnsFromAltarWithStats(GameTestHelper helper) {
        VestigeBoss boss = altarSpawn(helper, "choir_unheard");
        helper.assertTrue(boss instanceof ChoirUnheard, "Altar should spawn the Choir Unheard");
        ChoirUnheard choir = (ChoirUnheard) boss;
        helper.assertTrue(choir.home() != null && choir.altar() != null, "Arena should be bound to the altar");
        helper.assertTrue(choir.bar() != null && choir.bar().getColor() == BossEvent.BossBarColor.BLUE, "Boss bar should be blue");
        helper.assertTrue(near(choir.getMaxHealth(), 280), "Max health 280");
        helper.assertTrue(near(choir.getAttributeValue(Attributes.ARMOR), 6), "Armor 6");
        helper.assertTrue(near(choir.getAttributeValue(Attributes.ATTACK_DAMAGE), 8), "Damage 8");
        helper.assertTrue(choir.isNoGravity(), "It floats");
        helper.assertTrue(choir.bossId().equals(RelicWeave.id("choir_unheard")), "Boss id");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void choirLanceTelegraphsThenHitsOnlyTheLine(GameTestHelper helper) {
        try {
            ChoirUnheard choir = choir(helper, 10.5, 4.5, 3.5);
            Player online = mock(helper, 10.5, 2.0, 12.5);
            Player offline = mock(helper, 16.5, 2.0, 12.5);
            choir.setTarget(online);
            helper.assertTrue(choir.beginMove(ChoirUnheard.LANCE), "Lance should start");
            helper.assertTrue(choir.moveTimer() == ChoirUnheard.LANCE_WINDUP, "Lance wind-up is 20 ticks");
            for (int i = 0; i < ChoirUnheard.LANCE_WINDUP - 1; i++) {
                choir.tickBrain();
            }
            assertDamaged(helper, online, false, "Player during the wind-up");
            choir.tickBrain();
            assertDamaged(helper, online, true, "Player on the lance line");
            assertDamaged(helper, offline, false, "Player off the line");
            helper.succeed();
        } finally {
            discardMocks();
        }
    }

    @GameTest(template = TEMPLATE)
    public static void choirSilenceZonesDamageOnlyInsideCircles(GameTestHelper helper) {
        try {
            ChoirUnheard choir = choir(helper, 10.5, 4.5, 10.5);
            Player inside = mock(helper, 5.5, 2.0, 15.5);
            Player outside = mock(helper, 10.5, 2.0, 10.5);
            choir.setTarget(inside);
            helper.assertTrue(choir.beginMove(ChoirUnheard.ZONES), "Zones should start");
            helper.assertTrue(choir.zones().size() == 3, "Three circles marked");
            helper.assertTrue(near(choir.zones().get(0).x, inside.getX()) && near(choir.zones().get(0).z, inside.getZ()), "One circle is centred on the player");
            helper.assertTrue(choir.moveTimer() == ChoirUnheard.ZONES_WINDUP, "Zones wind-up is 40 ticks");
            // Pin the other two circles far from the bystander so the test is deterministic.
            choir.zones().set(1, helper.absoluteVec(new Vec3(3.5, 2.0, 3.5)));
            choir.zones().set(2, helper.absoluteVec(new Vec3(17.5, 2.0, 3.5)));
            for (int i = 0; i < ChoirUnheard.ZONES_WINDUP - 1; i++) {
                choir.tickBrain();
            }
            assertDamaged(helper, inside, false, "Player during the wind-up");
            choir.tickBrain();
            assertDamaged(helper, inside, true, "Player inside a circle");
            helper.assertTrue(inside.hasEffect(MobEffects.DARKNESS), "Darkness applied inside");
            assertDamaged(helper, outside, false, "Player outside every circle");
            helper.assertFalse(outside.hasEffect(MobEffects.DARKNESS), "No Darkness outside");
            helper.succeed();
        } finally {
            discardMocks();
        }
    }

    @GameTest(template = TEMPLATE)
    public static void choirToneShiftDamagesOnlyTheWrongHalf(GameTestHelper helper) {
        try {
            ChoirUnheard choir = choir(helper, 10.5, 4.5, 10.5);
            helper.assertFalse(choir.beginMove(ChoirUnheard.TONE), "No tone shift in phase 1");
            choir.setHealth(choir.getMaxHealth() * 0.5F);
            choir.updatePhase();
            helper.assertTrue(choir.phase() == 2, "Phase 2 at 50%");
            helper.assertTrue(choir.beginMove(ChoirUnheard.TONE), "Tone shift should start in phase 2");
            helper.assertTrue(choir.moveTimer() == ChoirUnheard.TONE_WINDUP, "Tone wind-up is 40 ticks");
            Vec3 origin = choir.toneOrigin();
            Vec3 normal = choir.toneNormal();
            Vec3 highPos = origin.add(normal.scale(6.0));
            Vec3 lowPos = origin.subtract(normal.scale(6.0));
            Player high = mock(helper, 0.0, 2.0, 0.0);
            high.moveTo(highPos.x, helper.absoluteVec(new Vec3(0, 2, 0)).y, highPos.z);
            Player low = mock(helper, 0.0, 2.0, 0.0);
            low.moveTo(lowPos.x, helper.absoluteVec(new Vec3(0, 2, 0)).y, lowPos.z);
            boolean highIsSafe = choir.toneHigh();
            for (int i = 0; i < ChoirUnheard.TONE_WINDUP - 1; i++) {
                choir.tickBrain();
            }
            assertDamaged(helper, high, false, "High-half player during the wind-up");
            assertDamaged(helper, low, false, "Low-half player during the wind-up");
            choir.tickBrain();
            assertDamaged(helper, high, !highIsSafe, "High-half player");
            assertDamaged(helper, low, highIsSafe, "Low-half player");
            helper.succeed();
        } finally {
            discardMocks();
        }
    }

    @GameTest(template = TEMPLATE)
    public static void choirPhasesAndWindupFloor(GameTestHelper helper) {
        ChoirUnheard choir = choir(helper, 10.5, 4.5, 10.5);
        choir.setHealth(choir.getMaxHealth() * 0.7F);
        choir.updatePhase();
        helper.assertTrue(choir.phase() == 1, "Phase 1 above 60%");
        choir.setHealth(choir.getMaxHealth() * 0.5F);
        choir.updatePhase();
        helper.assertTrue(choir.phase() == 2, "Phase 2 between 60% and 25%");
        choir.setHealth(choir.getMaxHealth() * 0.2F);
        choir.updatePhase();
        helper.assertTrue(choir.phase() == 3, "Phase 3 below 25%");
        helper.assertTrue(ChoirUnheard.LANCE_WINDUP >= ChoirUnheard.MIN_WINDUP && ChoirUnheard.ZONES_WINDUP >= ChoirUnheard.MIN_WINDUP
                && ChoirUnheard.TONE_WINDUP >= ChoirUnheard.MIN_WINDUP, "Wind-ups never below 15 ticks");
        Player p = mock(helper, 10.5, 2.0, 16.5);
        try {
            choir.setTarget(p);
            choir.beginMove(ChoirUnheard.LANCE);
            helper.assertTrue(choir.moveTimer() >= ChoirUnheard.MIN_WINDUP, "Phase 3 wind-up stays at least 15 ticks");
            for (int i = 0; i < choir.moveTimer() + 2; i++) {
                choir.tickBrain();
            }
            helper.assertTrue(choir.cooldown(ChoirUnheard.LANCE) <= 45, "Phase 3 cooldowns are x0.75, got " + choir.cooldown(ChoirUnheard.LANCE));
            helper.succeed();
        } finally {
            discardMocks();
        }
    }

    @GameTest(template = TEMPLATE)
    public static void choirNbtRoundTripKeepsState(GameTestHelper helper) {
        ChoirUnheard choir = choir(helper, 10.5, 4.5, 10.5);
        choir.setHealth(choir.getMaxHealth() * 0.5F);
        choir.updatePhase();
        Player p = mock(helper, 10.5, 2.0, 16.5);
        try {
            choir.setTarget(p);
            choir.beginMove(ChoirUnheard.ZONES);
            choir.setCooldown(ChoirUnheard.TONE, 55);
            ChoirUnheard copy = roundTrip(helper, choir);
            helper.assertTrue(copy.phase() == 2, "Phase kept");
            helper.assertTrue(copy.move() == ChoirUnheard.ZONES && copy.moveTimer() == choir.moveTimer(), "Move and timer kept");
            helper.assertTrue(copy.zones().size() == 3 && copy.zones().get(0).equals(choir.zones().get(0)), "Zones kept");
            helper.assertTrue(copy.cooldown(ChoirUnheard.TONE) == 55, "Cooldowns kept");
            helper.assertTrue(copy.home() != null && copy.home().equals(choir.home()), "Home kept");
            helper.succeed();
        } finally {
            discardMocks();
        }
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 160)
    public static void choirResetsWithoutPlayers(GameTestHelper helper) {
        VestigeBoss.overrideResetTicksForTests(20);
        try {
            ChoirUnheard choir = (ChoirUnheard) altarSpawn(helper, "choir_unheard");
            choir.setHealth(40.0F);
            helper.runAfterDelay(70, () -> {
                try {
                    helper.assertTrue(choir.getHealth() >= choir.getMaxHealth() - 0.01F, "Heals to full, was " + choir.getHealth());
                    helper.succeed();
                } finally {
                    VestigeBoss.overrideResetTicksForTests(null);
                }
            });
        } catch (RuntimeException e) {
            VestigeBoss.overrideResetTicksForTests(null);
            throw e;
        }
    }

    @GameTest(template = TEMPLATE)
    public static void choirDeathAwardsAdvancementOnceAndDropsGuaranteedLoot(GameTestHelper helper) {
        ChoirUnheard choir = (ChoirUnheard) altarSpawn(helper, "choir_unheard");
        rollLoot(helper, choir, "choir_unheard", new int[][]{{2, 2}, {4, 8}, {1, 2}},
                new Item[]{VestigeMaterials.LIMINAL_CHORD.get(), VestigeMaterials.INSCRIBED_PAGE.get(), VestigeMaterials.SEALED_PAGE.get()});
        killAndCheckRewards(helper, choir);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void bossEntityTypesAreRegistered(GameTestHelper helper) {
        EntityType<?>[] types = {BossesA.THORNED_REGENT.get(), BossesA.CHOIR_UNHEARD.get(), BossesA.THORN_SPIKE.get(), BossesA.THORN_ROOT.get()};
        for (EntityType<?> type : types) {
            helper.assertTrue(type.create(helper.getLevel()) != null, "Entity type should create: " + type);
        }
        helper.assertTrue(BossesA.THORNED_REGENT.get().fireImmune() && BossesA.CHOIR_UNHEARD.get().fireImmune(), "Bosses are fire immune");
        helper.succeed();
    }
}
