package com.relicweave.gametest;

import com.mojang.authlib.GameProfile;
import com.relicweave.RelicWeave;
import com.relicweave.vestige.boss.BossAltarBlockEntity;
import com.relicweave.vestige.boss.BossRegistry;
import com.relicweave.vestige.boss.BossRewards;
import com.relicweave.vestige.boss.VestigeBoss;
import com.relicweave.vestige.boss.c.ArchivistPrime;
import com.relicweave.vestige.boss.c.BossesC;
import com.relicweave.vestige.boss.c.IndexNode;
import com.relicweave.vestige.boss.c.IndexPage;
import com.relicweave.vestige.boss.c.RuneNode;
import com.relicweave.vestige.boss.c.SealNode;
import com.relicweave.vestige.boss.c.WardenOfSeals;
import com.relicweave.vestige.cycle.CyclePhase;
import com.relicweave.vestige.hazard.FieldRegistry;
import com.relicweave.vestige.hazard.field.StarfallMarker;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.npc.Villager;
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
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.UUID;
import java.util.stream.StreamSupport;

/** T13 bosses: Warden of Seals (Seal Ward, slam, lances, summons) and Archivist-Prime (stages, Cycle Shift, Final Seal, aura). */
@GameTestHolder(RelicWeave.MOD_ID)
@PrefixGameTestTemplate(false)
public final class BossesCGameTests {
    private static final String TEMPLATE = "bosses_c_arena";
    /** Own batch (24 tests, a multiple of the 8-per-row GameTest grid, so other classes' test neighbours do not change). */
    private static final String BATCH = "bossesC";
    /** Arena centre (template-local): the altar is at (13, 0, 10) and the boss spawns 3 blocks south of it. */
    private static final Vec3 CENTER = new Vec3(13.5, 1.0, 13.5);

    private BossesCGameTests() {}

    // ---- helpers ----

    private static boolean near(double a, double b) {
        return Math.abs(a - b) < 1.0E-3;
    }

    private static <T extends VestigeBoss> T spawnBoss(GameTestHelper helper, EntityType<T> type, Class<T> cls) {
        BlockPos altarPos = new BlockPos(13, 0, 10);
        helper.setBlock(altarPos, BossRegistry.BOSS_ALTAR.get());
        BossAltarBlockEntity altar = (BossAltarBlockEntity) helper.getBlockEntity(altarPos);
        helper.assertTrue(altar != null, "altar block entity");
        altar.setBossType(ForgeRegistries.ENTITY_TYPES.getKey(type));
        VestigeBoss boss = altar.spawnBoss(helper.getLevel());
        helper.assertTrue(cls.isInstance(boss), "boss spawned from the altar");
        return cls.cast(boss);
    }

    private static Villager villager(GameTestHelper helper, double x, double z) {
        return helper.spawnWithNoFreeWill(EntityType.VILLAGER, (float) x, 1.0F, (float) z);
    }

    private static ServerPlayer makePlayer(GameTestHelper helper, double x, double z) {
        ServerPlayer player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "relicweave-bossc-test"));
        player.setGameMode(GameType.SURVIVAL);
        Vec3 pos = helper.absoluteVec(new Vec3(x, 1.0, z));
        player.moveTo(pos.x, pos.y, pos.z, 0.0F, 0.0F);
        player.setHealth(player.getMaxHealth());
        return player;
    }

    private static boolean unhurt(Mob mob) {
        return mob.getHealth() >= mob.getMaxHealth();
    }

    @SuppressWarnings("unchecked")
    private static <T extends Mob> T roundTrip(GameTestHelper helper, T mob) {
        CompoundTag tag = mob.saveWithoutId(new CompoundTag());
        T copy = (T) mob.getType().create(helper.getLevel());
        copy.load(tag);
        return copy;
    }

    private static void killNode(GameTestHelper helper, RuneNode node) {
        node.setExposed(true);
        node.hurt(helper.getLevel().damageSources().generic(), 1000.0F);
    }

    private static void killAll(GameTestHelper helper, List<? extends RuneNode> nodes) {
        for (RuneNode node : List.copyOf(nodes)) {
            killNode(helper, node);
        }
    }

    private static List<ItemStack> rollBossLoot(GameTestHelper helper, VestigeBoss boss, long seed) {
        ResourceLocation id = boss.bossId();
        LootTable table = helper.getLevel().getServer().getLootData().getLootTable(new ResourceLocation(id.getNamespace(), "entities/" + id.getPath()));
        helper.assertTrue(table != LootTable.EMPTY, "loot table for " + id);
        LootParams params = new LootParams.Builder(helper.getLevel())
                .withParameter(LootContextParams.THIS_ENTITY, boss)
                .withParameter(LootContextParams.ORIGIN, boss.position())
                .withParameter(LootContextParams.DAMAGE_SOURCE, helper.getLevel().damageSources().generic())
                .create(LootContextParamSets.ENTITY);
        return table.getRandomItems(params, seed);
    }

    /**
     * Kills {@code boss} with a mock player and checks the reward path. {@code PlayerAdvancements#award} reports false for
     * fake players in this environment (see GateGameTests), so "the death path ran" is proven by the altar being notified
     * (the same loop that awards the advancement), and the advancement itself is checked through the player's progress:
     * it exists with the single {@code defeated} criterion, completing it is seen by {@code hasDefeated}, and a further
     * award is a no-op.
     */
    private static void checkKillRewards(GameTestHelper helper, VestigeBoss boss) {
        ServerPlayer player = makePlayer(helper, 10.5, 13.5);
        Advancement advancement = helper.getLevel().getServer().getAdvancements().getAdvancement(BossRewards.advancementId(boss.bossId()));
        helper.assertTrue(advancement != null, "advancement json loaded");
        AdvancementProgress progress = player.getAdvancements().getOrStartProgress(advancement);
        helper.assertTrue(List.of(BossRewards.CRITERION).equals(StreamSupport.stream(progress.getRemainingCriteria().spliterator(), false).toList()),
                "single 'defeated' criterion");
        helper.assertFalse(BossRewards.hasDefeated(player, boss.bossId()), "not defeated yet");
        BossAltarBlockEntity altar = (BossAltarBlockEntity) helper.getLevel().getBlockEntity(boss.altar());
        helper.assertFalse(altar.defeatedOnce(), "altar not yet notified");
        boss.hurt(helper.getLevel().damageSources().playerAttack(player), Float.MAX_VALUE);
        helper.assertFalse(boss.isAlive(), "boss died");
        helper.assertTrue(altar.defeatedOnce(), "altar notified of the defeat");
        progress.grantProgress(BossRewards.CRITERION);
        helper.assertTrue(BossRewards.hasDefeated(player, boss.bossId()), "advancement complete");
        helper.assertFalse(BossRewards.award(player, boss.bossId()), "a second award is a no-op");
    }

    private static int count(List<ItemStack> items, String path) {
        return items.stream().filter(s -> ForgeRegistries.ITEMS.getKey(s.getItem()).getPath().equals(path)).mapToInt(ItemStack::getCount).sum();
    }

    private static int countMarkers(GameTestHelper helper, VestigeBoss boss) {
        return helper.getLevel().getEntitiesOfClass(StarfallMarker.class, new AABB(boss.blockPosition()).inflate(40.0),
                m -> boss.getUUID().equals(m.ownerId())).size();
    }

    // ---- Warden of Seals ----

    @GameTest(batch = BATCH, template = TEMPLATE)
    public static void wardenSpawnsFromAltarWithStats(GameTestHelper helper) {
        WardenOfSeals w = spawnBoss(helper, BossesC.WARDEN_OF_SEALS.get(), WardenOfSeals.class);
        helper.assertTrue(w.home() != null && w.altar() != null, "home and altar bound");
        helper.assertTrue(w.altar().equals(helper.absolutePos(new BlockPos(13, 0, 10))), "altar bound to the altar block");
        helper.assertTrue(w.bossBar() != null && w.bossBar().getColor() == BossEvent.BossBarColor.YELLOW, "yellow boss bar");
        helper.assertTrue(near(w.getAttributeValue(Attributes.MAX_HEALTH), 400), "health");
        helper.assertTrue(near(w.getAttributeValue(Attributes.ARMOR), 14), "armor");
        helper.assertTrue(near(w.getAttributeValue(Attributes.ATTACK_DAMAGE), 13), "damage");
        helper.assertTrue(near(w.getAttributeValue(Attributes.MOVEMENT_SPEED), 0.25), "speed");
        helper.assertTrue(near(w.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), 1.0), "knockback resistance");
        helper.assertTrue(w.getType().fireImmune(), "fire immune");
        helper.succeed();
    }

    @GameTest(batch = BATCH, template = TEMPLATE)
    public static void wardenImmuneWhileNodeLivesVulnerableAfter(GameTestHelper helper) {
        WardenOfSeals w = spawnBoss(helper, BossesC.WARDEN_OF_SEALS.get(), WardenOfSeals.class);
        w.raiseNodes();
        List<SealNode> nodes = w.livingNodes();
        helper.assertTrue(nodes.size() == 3, "three nodes raised, got " + nodes.size());
        for (SealNode node : nodes) {
            double d = Math.hypot(node.getX() - (w.home().getX() + 0.5), node.getZ() - (w.home().getZ() + 0.5));
            helper.assertTrue(Math.abs(d - 9.0) < 0.2, "node 9 blocks from home, was " + d);
        }
        helper.assertTrue(nodes.stream().filter(SealNode::isExposed).count() == 1, "exactly one node exposed");
        helper.assertTrue(w.isSealed(), "sealed");
        ServerLevel level = helper.getLevel();
        helper.assertFalse(w.hurt(level.damageSources().generic(), 20.0F), "hit refused while sealed");
        helper.assertTrue(unhurt(w), "no damage while sealed");
        killAll(helper, nodes);
        helper.assertTrue(w.livingNodes().isEmpty(), "all nodes destroyed");
        w.updateSeal();
        helper.assertTrue(w.cycles() == 1 && w.vulnerableTicks() == WardenOfSeals.VULNERABLE_TICKS, "break cycle recorded");
        helper.assertFalse(w.isSealed(), "no longer sealed");
        w.hurt(level.damageSources().generic(), 20.0F);
        helper.assertTrue(w.getHealth() < w.getMaxHealth(), "vulnerable after all nodes die");
        helper.succeed();
    }

    @GameTest(batch = BATCH, template = TEMPLATE)
    public static void nodesDamageableOnlyWhileExposed(GameTestHelper helper) {
        WardenOfSeals w = spawnBoss(helper, BossesC.WARDEN_OF_SEALS.get(), WardenOfSeals.class);
        w.raiseNodes();
        List<SealNode> nodes = w.livingNodes();
        SealNode exposed = nodes.stream().filter(SealNode::isExposed).findFirst().orElseThrow();
        SealNode covered = nodes.stream().filter(n -> !n.isExposed()).findFirst().orElseThrow();
        helper.assertTrue(near(covered.getMaxHealth(), SealNode.HEALTH), "node has 20 health");
        covered.hurt(helper.getLevel().damageSources().generic(), 5.0F);
        helper.assertTrue(unhurt(covered), "covered node takes no damage");
        exposed.hurt(helper.getLevel().damageSources().generic(), 5.0F);
        helper.assertTrue(near(exposed.getHealth(), SealNode.HEALTH - 5.0), "exposed node takes damage");
        helper.succeed();
    }

    @GameTest(batch = BATCH, template = TEMPLATE)
    public static void wardenExposureRotates(GameTestHelper helper) {
        WardenOfSeals w = spawnBoss(helper, BossesC.WARDEN_OF_SEALS.get(), WardenOfSeals.class);
        w.raiseNodes();
        SealNode first = w.livingNodes().stream().filter(SealNode::isExposed).findFirst().orElseThrow();
        for (int i = 0; i < WardenOfSeals.EXPOSE_TICKS; i++) {
            w.updateSeal();
        }
        List<SealNode> exposed = w.livingNodes().stream().filter(SealNode::isExposed).toList();
        helper.assertTrue(exposed.size() == 1 && exposed.get(0) != first, "exposure moved to another node after 120 ticks");
        helper.succeed();
    }

    @GameTest(batch = BATCH, template = TEMPLATE)
    public static void wardenResealStopsAfterThreeCycles(GameTestHelper helper) {
        WardenOfSeals w = spawnBoss(helper, BossesC.WARDEN_OF_SEALS.get(), WardenOfSeals.class);
        // A fresh cycle re-seals.
        w.raiseNodes();
        killAll(helper, w.livingNodes());
        w.updateSeal();
        helper.assertTrue(w.cycles() == 1, "first cycle broken");
        w.setVulnerableTicks(1);
        w.updateSeal();
        helper.assertTrue(w.livingNodes().size() == 3, "re-sealed with new nodes after the window");
        // The third broken cycle does not re-seal.
        w.setCycles(2);
        killAll(helper, w.livingNodes());
        w.updateSeal();
        helper.assertTrue(w.cycles() == 3 && !w.canReseal(), "three cycles broken");
        w.setVulnerableTicks(1);
        w.updateSeal();
        helper.assertTrue(w.livingNodes().isEmpty() && !w.isSealed(), "does not re-seal after three cycles");
        // Below 30% health it stops re-sealing too, even on the first cycle.
        w.setCycles(0);
        w.raiseNodes();
        killAll(helper, w.livingNodes());
        w.updateSeal();
        w.setHealth(w.getMaxHealth() * 0.29F);
        helper.assertFalse(w.canReseal(), "cannot re-seal below 30% health");
        w.setVulnerableTicks(1);
        w.updateSeal();
        helper.assertTrue(w.livingNodes().isEmpty(), "no re-seal below 30% health");
        helper.succeed();
    }

    @GameTest(batch = BATCH, template = TEMPLATE)
    public static void wardenSlamTelegraphsThenHitsRingOnly(GameTestHelper helper) {
        WardenOfSeals w = spawnBoss(helper, BossesC.WARDEN_OF_SEALS.get(), WardenOfSeals.class);
        Villager inside = villager(helper, 15.5, 13.5);
        Villager outside = villager(helper, 20.5, 13.5);
        w.beginMove(WardenOfSeals.MOVE_SLAM);
        helper.assertTrue(w.move() == WardenOfSeals.MOVE_SLAM && w.moveTicks() == WardenOfSeals.SLAM_WINDUP, "slam wind-up is 25 ticks");
        for (int i = 0; i < WardenOfSeals.SLAM_WINDUP - 1; i++) {
            w.tickMove();
        }
        helper.assertTrue(unhurt(inside) && unhurt(outside), "no damage during the wind-up");
        w.tickMove();
        helper.assertTrue(inside.getHealth() <= inside.getMaxHealth() - w.scaledDamage(WardenOfSeals.SLAM_DAMAGE) + 0.01F, "ring hits inside");
        helper.assertTrue(unhurt(outside), "outside the ring is safe");
        helper.assertTrue(w.recoverTicks() > 0 && w.move() == WardenOfSeals.MOVE_NONE, "recovery window after the slam");
        helper.succeed();
    }

    @GameTest(batch = BATCH, template = TEMPLATE)
    public static void wardenLancesTelegraphThenHitLinesOnly(GameTestHelper helper) {
        WardenOfSeals w = spawnBoss(helper, BossesC.WARDEN_OF_SEALS.get(), WardenOfSeals.class);
        Villager onLine = villager(helper, 21.5, 13.5);
        Villager beside = villager(helper, 21.5, 21.5);
        Villager behind = villager(helper, 8.5, 13.5);
        w.setTarget(onLine);
        w.beginMove(WardenOfSeals.MOVE_LANCES);
        helper.assertTrue(w.move() == WardenOfSeals.MOVE_LANCES && w.moveTicks() == WardenOfSeals.LANCE_WINDUP, "lance wind-up is 20 ticks");
        for (int i = 0; i < WardenOfSeals.LANCE_WINDUP - 1; i++) {
            w.tickMove();
        }
        helper.assertTrue(unhurt(onLine) && unhurt(beside) && unhurt(behind), "no damage during the wind-up");
        w.tickMove();
        helper.assertTrue(onLine.getHealth() <= onLine.getMaxHealth() - w.scaledDamage(WardenOfSeals.LANCE_DAMAGE) + 0.01F, "lance hits its line");
        helper.assertTrue(unhurt(beside) && unhurt(behind), "off-line targets are safe");
        helper.succeed();
    }

    @GameTest(batch = BATCH, template = TEMPLATE)
    public static void wardenSummonsAreCapped(GameTestHelper helper) {
        WardenOfSeals w = spawnBoss(helper, BossesC.WARDEN_OF_SEALS.get(), WardenOfSeals.class);
        helper.assertTrue(w.spawnSummons(2) == 2 && w.spawnSummons(2) == 2, "four sentinels summoned");
        helper.assertTrue(w.spawnSummons(2) == 0, "at most 4 alive");
        for (Mob mob : w.livingSummons()) {
            helper.assertTrue(mob.getType() == com.relicweave.vestige.entity.humanoid.HumanoidMobs.RELIQUARY_SENTINEL.get(), "summons are reliquary sentinels");
            helper.assertTrue(mob.isPersistenceRequired(), "summons persist");
            mob.discard();
        }
        helper.assertTrue(w.spawnSummons(2) == 2 && w.spawnSummons(2) == 2, "four more summoned");
        helper.assertTrue(w.summonedTotal() == 8, "8 summoned so far");
        for (Mob mob : w.livingSummons()) {
            mob.discard();
        }
        helper.assertTrue(w.spawnSummons(2) == 0, "at most 8 per fight");
        helper.succeed();
    }

    @GameTest(batch = BATCH, template = TEMPLATE)
    public static void wardenPhasesSwitchAtHalfHealth(GameTestHelper helper) {
        WardenOfSeals w = spawnBoss(helper, BossesC.WARDEN_OF_SEALS.get(), WardenOfSeals.class);
        w.updatePhase();
        helper.assertTrue(w.phase() == 1, "phase 1 at full health");
        w.setHealth(w.getMaxHealth() * 0.51F);
        w.updatePhase();
        helper.assertTrue(w.phase() == 1, "still phase 1 above 50%");
        w.setHealth(w.getMaxHealth() * 0.50F);
        w.updatePhase();
        helper.assertTrue(w.phase() == 2, "phase 2 at 50%");
        helper.succeed();
    }

    @GameTest(batch = BATCH, template = TEMPLATE, timeoutTicks = 200)
    public static void wardenResetHealsAndClearsSummonsAndNodes(GameTestHelper helper) {
        VestigeBoss.overrideResetTicksForTests(20);
        WardenOfSeals w = spawnBoss(helper, BossesC.WARDEN_OF_SEALS.get(), WardenOfSeals.class);
        w.raiseNodes();
        List<SealNode> nodes = w.livingNodes();
        helper.assertTrue(nodes.size() == 3, "setup");
        w.setHealth(50.0F);
        // Summon a few ticks in so the sentinels only exist briefly (other tests count nearby Vestige mobs).
        helper.runAfterDelay(5, () -> {
            helper.assertTrue(w.spawnSummons(2) == 2, "summoned");
            List<Mob> summons = w.livingSummons();
            helper.runAfterDelay(50, () -> {
                VestigeBoss.overrideResetTicksForTests(null);
                helper.assertTrue(w.getHealth() >= w.getMaxHealth(), "boss healed to full");
                helper.assertTrue(nodes.stream().allMatch(n -> n.isRemoved()) && summons.stream().allMatch(Mob::isRemoved), "nodes and summons removed");
                helper.assertTrue(w.cycles() == 0 && w.livingNodes().isEmpty() && w.summonedTotal() == 0, "ward state reset");
                helper.succeed();
            });
        });
    }

    @GameTest(batch = BATCH, template = TEMPLATE)
    public static void wardenDeathAwardsOnceAndDropsGuaranteedLoot(GameTestHelper helper) {
        WardenOfSeals w = spawnBoss(helper, BossesC.WARDEN_OF_SEALS.get(), WardenOfSeals.class);
        checkKillRewards(helper, w);
        for (long seed = 0; seed < 25; seed++) {
            List<ItemStack> loot = rollBossLoot(helper, w, seed);
            helper.assertTrue(count(loot, "vestige_upgrade_template") == 1, "template always drops");
            helper.assertTrue(count(loot, "concordant_seal") == 2, "two concordant seals always drop");
            int pages = count(loot, "sealed_page");
            int ingots = count(loot, "aetherite_ingot");
            helper.assertTrue(pages >= 3 && ingots >= 2, "pages and ingots drop");
        }
        helper.succeed();
    }

    @GameTest(batch = BATCH, template = TEMPLATE)
    public static void wardenNbtRoundTrip(GameTestHelper helper) {
        WardenOfSeals w = spawnBoss(helper, BossesC.WARDEN_OF_SEALS.get(), WardenOfSeals.class);
        w.raiseNodes();
        w.spawnSummons(2);
        w.setCycles(2);
        w.setVulnerableTicks(77);
        w.beginMove(WardenOfSeals.MOVE_SLAM);
        w.tickMove();
        WardenOfSeals copy = roundTrip(helper, w);
        helper.assertTrue(copy.cycles() == 2 && copy.vulnerableTicks() == 77, "cycle state survives");
        helper.assertTrue(copy.livingNodes().size() == 3 && copy.livingSummons().size() == 2, "node and summon bindings survive");
        helper.assertTrue(copy.summonedTotal() == 2, "summon total survives");
        helper.assertTrue(copy.move() == WardenOfSeals.MOVE_SLAM && copy.moveTicks() == WardenOfSeals.SLAM_WINDUP - 1, "move state survives");
        helper.assertTrue(copy.home() != null && copy.home().equals(w.home()) && copy.altar() != null, "arena binding survives");
        w.livingSummons().forEach(Mob::discard);
        helper.succeed();
    }

    // ---- Archivist-Prime ----

    @GameTest(batch = BATCH, template = TEMPLATE)
    public static void primeSpawnsFromAltarWithStats(GameTestHelper helper) {
        ArchivistPrime p = spawnBoss(helper, BossesC.ARCHIVIST_PRIME.get(), ArchivistPrime.class);
        helper.assertTrue(p.home() != null && p.altar() != null, "home and altar bound");
        helper.assertTrue(p.bossBar() != null && p.bossBar().getColor() == BossEvent.BossBarColor.PINK, "pink boss bar");
        helper.assertTrue(near(p.getAttributeValue(Attributes.MAX_HEALTH), 600), "health");
        helper.assertTrue(near(p.getAttributeValue(Attributes.ARMOR), 16), "armor");
        helper.assertTrue(near(p.getAttributeValue(Attributes.ATTACK_DAMAGE), 14), "damage");
        helper.assertTrue(near(p.getAttributeValue(Attributes.MOVEMENT_SPEED), 0.26), "speed");
        helper.assertTrue(near(p.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), 1.0), "knockback resistance");
        helper.assertTrue(p.getType().fireImmune(), "fire immune");
        helper.assertTrue(near(p.arenaRadius(), 40.0), "arena radius 40");
        helper.succeed();
    }

    @GameTest(batch = BATCH, template = TEMPLATE)
    public static void primeStagesSwitchAtSixtySixAndThirtyThree(GameTestHelper helper) {
        helper.assertTrue(ArchivistPrime.stageFor(0.67F) == 1 && ArchivistPrime.stageFor(0.66F) == 2, "stage 2 from 66%");
        helper.assertTrue(ArchivistPrime.stageFor(0.34F) == 2 && ArchivistPrime.stageFor(0.33F) == 2 && ArchivistPrime.stageFor(0.32F) == 3, "stage 3 below 33%");
        ArchivistPrime p = spawnBoss(helper, BossesC.ARCHIVIST_PRIME.get(), ArchivistPrime.class);
        p.updateStage();
        helper.assertTrue(p.phase() == 1 && !p.finalSealRaised(), "stage 1 at full health");
        p.setHealth(p.getMaxHealth() * 0.65F);
        p.updateStage();
        helper.assertTrue(p.phase() == 2 && p.cycleTicks() > 0 && !p.finalSealRaised(), "stage 2 starts Cycle Shift only");
        p.setHealth(p.getMaxHealth() * 0.30F);
        p.updateStage();
        helper.assertTrue(p.phase() == 3 && p.finalSealRaised() && p.livingNodes().size() == 2, "stage 3 raises the Final Seal");
        helper.assertTrue(p.cooldownTicks() < ArchivistPrime.BASE_COOLDOWN_TICKS
                && p.cooldownTicks() == Math.round(ArchivistPrime.BASE_COOLDOWN_TICKS * 0.85), "stage 3 cooldowns x0.85");
        helper.succeed();
    }

    @GameTest(batch = BATCH, template = TEMPLATE)
    public static void primeImmuneWhileIndexNodeLives(GameTestHelper helper) {
        ArchivistPrime p = spawnBoss(helper, BossesC.ARCHIVIST_PRIME.get(), ArchivistPrime.class);
        p.setHealth(p.getMaxHealth() * 0.30F);
        float before = p.getHealth();
        p.updateStage();
        List<IndexNode> nodes = p.livingNodes();
        helper.assertTrue(nodes.size() == 2 && nodes.stream().allMatch(IndexNode::isExposed), "two always-exposed nodes");
        helper.assertTrue(near(nodes.get(0).getMaxHealth(), IndexNode.HEALTH), "index node has 30 health");
        for (IndexNode n : nodes) {
            double d = Math.hypot(n.getX() - (p.home().getX() + 0.5), n.getZ() - (p.home().getZ() + 0.5));
            helper.assertTrue(Math.abs(d - 12.0) < 0.2, "fallback: 12 blocks from home, was " + d);
        }
        helper.assertFalse(p.hurt(helper.getLevel().damageSources().generic(), 30.0F), "hit refused while a node lives");
        helper.assertTrue(p.getHealth() == before, "no damage while sealed");
        killNode(helper, nodes.get(0));
        helper.assertFalse(p.hurt(helper.getLevel().damageSources().generic(), 30.0F), "still immune with one node left");
        killNode(helper, nodes.get(1));
        p.hurt(helper.getLevel().damageSources().generic(), 30.0F);
        helper.assertTrue(p.getHealth() < before, "vulnerable after both nodes die");
        helper.succeed();
    }

    @GameTest(batch = BATCH, template = TEMPLATE)
    public static void primeIndexNodesStandNearNullWards(GameTestHelper helper) {
        helper.setBlock(new BlockPos(3, 1, 13), FieldRegistry.NULL_WARD.get());
        helper.setBlock(new BlockPos(23, 1, 13), FieldRegistry.NULL_WARD.get());
        ArchivistPrime p = spawnBoss(helper, BossesC.ARCHIVIST_PRIME.get(), ArchivistPrime.class);
        p.setHealth(p.getMaxHealth() * 0.30F);
        p.updateStage();
        List<IndexNode> nodes = p.livingNodes();
        helper.assertTrue(nodes.size() == 2, "two nodes");
        BlockPos west = helper.absolutePos(new BlockPos(3, 1, 13));
        BlockPos east = helper.absolutePos(new BlockPos(23, 1, 13));
        boolean nearWest = nodes.stream().anyMatch(n -> n.position().distanceTo(Vec3.atCenterOf(west)) < 3.0);
        boolean nearEast = nodes.stream().anyMatch(n -> n.position().distanceTo(Vec3.atCenterOf(east)) < 3.0);
        helper.assertTrue(nearWest && nearEast, "one node beside each null ward");
        helper.succeed();
    }

    @GameTest(batch = BATCH, template = TEMPLATE)
    public static void primeAuraHurtsOnlyWithinFourBlocks(GameTestHelper helper) {
        ArchivistPrime p = spawnBoss(helper, BossesC.ARCHIVIST_PRIME.get(), ArchivistPrime.class);
        Villager close = villager(helper, 16.0, 13.5);
        Villager edge = villager(helper, 17.2, 13.5);
        Villager far = villager(helper, 20.5, 13.5);
        int hurt = p.applyAura();
        helper.assertTrue(hurt == 2, "two targets inside the aura, hurt " + hurt);
        helper.assertTrue(near(close.getHealth(), close.getMaxHealth() - 2.0), "aura does 2 damage");
        helper.assertTrue(near(edge.getHealth(), edge.getMaxHealth() - 2.0), "just inside 4 blocks is hit");
        helper.assertTrue(unhurt(far), "beyond 4 blocks is safe");
        helper.succeed();
    }

    @GameTest(batch = BATCH, template = TEMPLATE)
    public static void primeLancesTelegraphThenHitLinesOnly(GameTestHelper helper) {
        ArchivistPrime p = spawnBoss(helper, BossesC.ARCHIVIST_PRIME.get(), ArchivistPrime.class);
        Villager onLine = villager(helper, 22.5, 13.5);
        Villager beside = villager(helper, 13.5, 23.5);
        Villager behind = villager(helper, 6.5, 13.5);
        p.setTarget(onLine);
        p.beginMove(ArchivistPrime.MOVE_LANCES);
        helper.assertTrue(p.move() == ArchivistPrime.MOVE_LANCES && p.moveTicks() == ArchivistPrime.LANCE_WINDUP, "lance wind-up is 20 ticks");
        for (int i = 0; i < ArchivistPrime.LANCE_WINDUP - 1; i++) {
            p.tickMove();
        }
        helper.assertTrue(unhurt(onLine) && unhurt(beside) && unhurt(behind), "no damage during the wind-up");
        p.tickMove();
        helper.assertTrue(onLine.getHealth() <= onLine.getMaxHealth() - p.scaledDamage(ArchivistPrime.LANCE_DAMAGE) + 0.01F, "lance hits its line");
        helper.assertTrue(unhurt(beside) && unhurt(behind), "off-line targets are safe");
        helper.succeed();
    }

    @GameTest(batch = BATCH, template = TEMPLATE)
    public static void primeVolleyFiresFivePagesAfterWindUp(GameTestHelper helper) {
        ArchivistPrime p = spawnBoss(helper, BossesC.ARCHIVIST_PRIME.get(), ArchivistPrime.class);
        Villager target = villager(helper, 20.5, 13.5);
        p.setTarget(target);
        AABB area = new AABB(p.blockPosition()).inflate(10.0);
        p.beginMove(ArchivistPrime.MOVE_VOLLEY);
        helper.assertTrue(p.moveTicks() == ArchivistPrime.VOLLEY_WINDUP, "volley wind-up is 15 ticks");
        for (int i = 0; i < ArchivistPrime.VOLLEY_WINDUP - 1; i++) {
            p.tickMove();
        }
        helper.assertTrue(helper.getLevel().getEntitiesOfClass(IndexPage.class, area).isEmpty(), "no pages during the wind-up");
        p.tickMove();
        List<IndexPage> pages = helper.getLevel().getEntitiesOfClass(IndexPage.class, area);
        helper.assertTrue(pages.size() == ArchivistPrime.VOLLEY_PAGES, "five pages after the wind-up, got " + pages.size());
        helper.assertTrue(pages.stream().allMatch(pg -> near(pg.damage(), p.scaledDamage(IndexPage.BASE_DAMAGE)) && pg.getOwner() == p), "pages carry scaled 6 damage and the owner");
        helper.assertTrue(unhurt(target), "no instant damage");
        helper.succeed();
    }

    @GameTest(batch = BATCH, template = TEMPLATE)
    public static void primeCycleShiftConvergenceMarksEveryPlayer(GameTestHelper helper) {
        ArchivistPrime p = spawnBoss(helper, BossesC.ARCHIVIST_PRIME.get(), ArchivistPrime.class);
        List<ServerPlayer> players = List.of(makePlayer(helper, 6.5, 6.5), makePlayer(helper, 20.5, 20.5));
        p.applyCyclePhase(CyclePhase.QUIET, players);
        helper.assertTrue(countMarkers(helper, p) == 0 && p.rings().isEmpty(), "QUIET adds no hazard");
        p.applyCyclePhase(CyclePhase.CONVERGENCE, players);
        helper.assertTrue(p.cyclePhase() == CyclePhase.CONVERGENCE && p.cycleTicks() == ArchivistPrime.CYCLE_TICKS, "convergence imposed for 20 s");
        helper.assertTrue(countMarkers(helper, p) == 2, "one marker per player, got " + countMarkers(helper, p));
        List<StarfallMarker> markers = helper.getLevel().getEntitiesOfClass(StarfallMarker.class, new AABB(p.blockPosition()).inflate(40.0));
        helper.assertTrue(markers.stream().allMatch(m -> m.remainingTicks() == ArchivistPrime.MARKER_WARNING
                && near(m.damage(), p.scaledDamage(ArchivistPrime.MARKER_DAMAGE))), "markers warn 60 ticks for scaled 10 damage");
        helper.succeed();
    }

    @GameTest(batch = BATCH, template = TEMPLATE)
    public static void primeCycleShiftResonanceRingsTelegraphThenStrike(GameTestHelper helper) {
        ArchivistPrime p = spawnBoss(helper, BossesC.ARCHIVIST_PRIME.get(), ArchivistPrime.class);
        List<ServerPlayer> players = List.of(makePlayer(helper, 6.5, 6.5), makePlayer(helper, 20.5, 20.5));
        Villager inRing = villager(helper, 6.5, 6.5);
        Villager away = villager(helper, 13.5, 13.5);
        p.applyCyclePhase(CyclePhase.RESONANCE, players);
        helper.assertTrue(p.rings().size() == ArchivistPrime.RING_COUNT, "three rune rings");
        helper.assertTrue(p.rings().stream().allMatch(r -> r.ticks() == ArchivistPrime.RING_WARNING), "rings telegraph 40 ticks");
        for (int i = 0; i < ArchivistPrime.RING_WARNING - 1; i++) {
            p.tickRings();
        }
        helper.assertTrue(unhurt(inRing) && unhurt(away), "no damage during the warning");
        p.tickRings();
        helper.assertTrue(p.rings().isEmpty(), "rings resolved");
        helper.assertTrue(inRing.getHealth() <= inRing.getMaxHealth() - p.scaledDamage(ArchivistPrime.RING_DAMAGE) + 0.01F, "ring hits inside");
        helper.assertTrue(unhurt(away), "outside every ring is safe");
        helper.succeed();
    }

    @GameTest(batch = BATCH, template = TEMPLATE, timeoutTicks = 200)
    public static void primeResetHealsAndClearsNodes(GameTestHelper helper) {
        VestigeBoss.overrideResetTicksForTests(20);
        ArchivistPrime p = spawnBoss(helper, BossesC.ARCHIVIST_PRIME.get(), ArchivistPrime.class);
        p.setHealth(p.getMaxHealth() * 0.30F);
        p.updateStage();
        List<IndexNode> nodes = p.livingNodes();
        helper.assertTrue(nodes.size() == 2, "setup");
        helper.runAfterDelay(60, () -> {
            VestigeBoss.overrideResetTicksForTests(null);
            helper.assertTrue(p.getHealth() >= p.getMaxHealth(), "boss healed to full");
            helper.assertTrue(nodes.stream().allMatch(n -> n.isRemoved()) && !p.finalSealRaised(), "index nodes removed");
            helper.succeed();
        });
    }

    @GameTest(batch = BATCH, template = TEMPLATE)
    public static void primeDeathAwardsOnceAndDropsGuaranteedLoot(GameTestHelper helper) {
        ArchivistPrime p = spawnBoss(helper, BossesC.ARCHIVIST_PRIME.get(), ArchivistPrime.class);
        checkKillRewards(helper, p);
        for (long seed = 0; seed < 25; seed++) {
            List<ItemStack> loot = rollBossLoot(helper, p, seed);
            helper.assertTrue(count(loot, "prime_mnemonic_core") == 1 && count(loot, "concordant_seal") == 1, "core and seal always drop");
            helper.assertTrue(count(loot, "mnemonic_filament") >= 3 && count(loot, "aetherite_ingot") >= 4, "filament and ingots drop");
        }
        helper.succeed();
    }

    @GameTest(batch = BATCH, template = TEMPLATE)
    public static void primeNbtRoundTrip(GameTestHelper helper) {
        ArchivistPrime p = spawnBoss(helper, BossesC.ARCHIVIST_PRIME.get(), ArchivistPrime.class);
        p.setHealth(p.getMaxHealth() * 0.30F);
        p.updateStage();
        p.applyCyclePhase(CyclePhase.RESONANCE, List.of(makePlayer(helper, 6.5, 6.5)));
        p.tickRings();
        ArchivistPrime copy = roundTrip(helper, p);
        helper.assertTrue(copy.finalSealRaised() && copy.livingNodes().size() == 2, "Final Seal state survives");
        helper.assertTrue(copy.cycleIndex() == CyclePhase.RESONANCE.ordinal() && copy.cycleTicks() == p.cycleTicks(), "Cycle Shift state survives");
        helper.assertTrue(copy.rings().size() == p.rings().size() && !copy.rings().isEmpty()
                && copy.rings().get(0).ticks() == ArchivistPrime.RING_WARNING - 1, "pending rings survive");
        helper.assertTrue(copy.phase() == 3 && copy.home() != null, "phase and binding survive");
        helper.succeed();
    }
}
