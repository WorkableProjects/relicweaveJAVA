package com.relicweave.gametest;

import com.mojang.authlib.GameProfile;
import com.relicweave.RelicWeave;
import com.relicweave.vestige.boss.BossAltarBlockEntity;
import com.relicweave.vestige.boss.BossRewards;
import com.relicweave.vestige.boss.VestigeBoss;
import com.relicweave.vestige.boss.b.BossesB;
import com.relicweave.vestige.boss.b.GravityMaw;
import com.relicweave.vestige.boss.b.StarforgedColossus;
import com.relicweave.vestige.hazard.FieldRegistry;
import com.relicweave.vestige.hazard.field.StarfallMarker;
import com.relicweave.vestige.registry.VestigeMaterials;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Server-side tests for the Gravity Maw and the Starforged Colossus. The bosses are spawned from a Boss Altar in a
 * 21 x 8 x 21 arena template (template y 0 is relative y 1 in GameTest coordinates) and driven synchronously: {@code setScripted(true)} stops their own AI, then the tests call
 * {@code start*} and {@code tickMoves()}/{@code tickVents()} directly, so "no damage during the wind-up" is exact.
 * Mock players are added to the level's player list so the bosses see them as arena players, and removed again before
 * the test returns. Nothing waits for later ticks (the bars and resets call {@code boss.tick()} in a loop), so tests that
 * share the batch run back to back and never see each other's players, bosses or levers.
 */
@GameTestHolder(RelicWeave.MOD_ID)
@PrefixGameTestTemplate(false)
public final class BossesBGameTests {
    /**
     * Own batch of 24 tests (a multiple of the 8-per-row GameTest grid, so other classes' test neighbours do not change).
     * Every test is synchronous, so tests in the batch cannot observe each other's players, bosses or levers.
     */
    private static final String BATCH = "bosses_b";
    private static final String MAW_TEMPLATE = "bosses_b_maw";
    private static final String COLOSSUS_TEMPLATE = "bosses_b_colossus";
    private static final BlockPos ALTAR = new BlockPos(10, 2, 3);
    
    private BossesBGameTests() {}

    // ---- helpers ----

    private static boolean near(double a, double b) {
        return Math.abs(a - b) < 1.0E-3;
    }

    private static <T extends VestigeBoss> T spawn(GameTestHelper helper, Class<T> type) {
        BossAltarBlockEntity altar = (BossAltarBlockEntity) helper.getBlockEntity(ALTAR);
        helper.assertTrue(altar != null, "Altar block entity missing from the template");
        VestigeBoss boss = altar.spawnBoss(helper.getLevel());
        helper.assertTrue(type.isInstance(boss), "Altar should spawn a " + type.getSimpleName());
        return type.cast(boss);
    }

    private static ServerPlayer player(GameTestHelper helper, Vec3 relative, List<ServerPlayer> registry) {
        ServerPlayer player = new TestPlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "relicweave-bossb-test"));
        player.setGameMode(GameType.SURVIVAL);
        Vec3 pos = helper.absoluteVec(relative);
        player.moveTo(pos.x, pos.y, pos.z, 0.0F, 0.0F);
        player.setDeltaMovement(Vec3.ZERO);
        player.removeAllEffects();
        player.setHealth(player.getMaxHealth());
        player.invulnerableTime = 0;
        // A fresh ServerPlayer is invulnerable for 60 ticks and mock players are never ticked.
        ObfuscationReflectionHelper.setPrivateValue(ServerPlayer.class, player, 0, "spawnInvulnerableTime");
        player.setRemainingFireTicks(0);
        helper.getLevel().players().add(player);
        registry.add(player);
        return player;
    }

    /**
     * A real {@link ServerPlayer} (not Forge's FakePlayer, which is invulnerable and never receives advancements) whose
     * connection drops every packet, so damage, effects and advancements behave as for a connected survival player.
     */
    private static final class TestPlayer extends ServerPlayer {
        TestPlayer(net.minecraft.server.level.ServerLevel level, GameProfile profile) {
            super(level.getServer(), level, profile);
            new ServerGamePacketListenerImpl(level.getServer(), new Connection(PacketFlow.SERVERBOUND), this) {
                @Override
                public void send(Packet<?> packet) {}

                @Override
                public void send(Packet<?> packet, PacketSendListener listener) {}

                @Override
                public void disconnect(Component reason) {}
            };
        }
    }

    private static void cleanup(GameTestHelper helper, List<ServerPlayer> players) {
        helper.getLevel().players().removeAll(players);
    }

    /** Runs the boss's whole entity tick (AI step included) {@code n} times, synchronously, advancing tickCount as the level would. */
    private static void fullTicks(VestigeBoss boss, int n) {
        for (int i = 0; i < n; i++) {
            boss.tickCount++;
            boss.tick();
        }
    }

    private static void tick(GravityMaw boss, int n) {
        for (int i = 0; i < n; i++) {
            boss.tickMoves();
        }
    }

    private static void tick(StarforgedColossus boss, int n) {
        for (int i = 0; i < n; i++) {
            boss.tickMoves();
        }
    }

    private static void tickVents(StarforgedColossus boss, int n) {
        for (int i = 0; i < n; i++) {
            boss.tickVents();
        }
    }

    private static List<StarfallMarker> markers(GameTestHelper helper, VestigeBoss owner) {
        List<StarfallMarker> out = new ArrayList<>();
        for (StarfallMarker m : helper.getLevel().getEntitiesOfClass(StarfallMarker.class, new AABB(helper.absolutePos(BlockPos.ZERO)).inflate(40))) {
            if (owner.getUUID().equals(m.ownerId())) {
                out.add(m);
            }
        }
        return out;
    }

    @SuppressWarnings("unchecked")
    private static <T extends VestigeBoss> T roundTrip(GameTestHelper helper, T boss) {
        CompoundTag tag = boss.saveWithoutId(new CompoundTag());
        T copy = (T) boss.getType().create(helper.getLevel());
        copy.load(tag);
        return copy;
    }

    private static void checkStats(GameTestHelper helper, VestigeBoss boss, double health, double damage, double armor, double speed) {
        helper.assertTrue(near(boss.getMaxHealth(), health), "Max health should be " + health + " but is " + boss.getMaxHealth());
        helper.assertTrue(near(boss.getAttributeValue(Attributes.ATTACK_DAMAGE), damage), "Attack damage should be " + damage);
        helper.assertTrue(near(boss.getAttributeValue(Attributes.ARMOR), armor), "Armor should be " + armor);
        helper.assertTrue(near(boss.getAttributeValue(Attributes.MOVEMENT_SPEED), speed), "Speed should be " + speed);
        helper.assertTrue(near(boss.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), 1.0), "Knockback resistance should be 1.0");
    }

    private static void checkLoot(GameTestHelper helper, VestigeBoss boss, String id, Item guaranteed, int guaranteedCount,
                                  Item glass, int glassMin, int glassMax, int ingotMin, int ingotMax) {
        LootTable table = helper.getLevel().getServer().getLootData().getLootTable(RelicWeave.id("entities/" + id));
        helper.assertTrue(table != LootTable.EMPTY, "Missing loot table for " + id);
        for (long seed = 1; seed <= 5; seed++) {
            LootParams params = new LootParams.Builder(helper.getLevel())
                    .withParameter(LootContextParams.THIS_ENTITY, boss)
                    .withParameter(LootContextParams.ORIGIN, boss.position())
                    .withParameter(LootContextParams.DAMAGE_SOURCE, boss.damageSources().generic())
                    .create(LootContextParamSets.ENTITY);
            int core = 0;
            int riftglass = 0;
            int ingots = 0;
            for (ItemStack stack : table.getRandomItems(params, seed)) {
                if (stack.is(guaranteed)) {
                    core += stack.getCount();
                } else if (stack.is(glass)) {
                    riftglass += stack.getCount();
                } else if (stack.is(VestigeMaterials.AETHERITE_INGOT.get())) {
                    ingots += stack.getCount();
                }
            }
            helper.assertTrue(core == guaranteedCount, id + " must always drop " + guaranteedCount + " of its signature item, got " + core);
            helper.assertTrue(riftglass >= glassMin && riftglass <= glassMax, id + " riftglass out of range: " + riftglass);
            helper.assertTrue(ingots >= ingotMin && ingots <= ingotMax, id + " aetherite out of range: " + ingots);
        }
    }

    // ================= Gravity Maw =================

    @GameTest(template = MAW_TEMPLATE, batch = BATCH)
    public static void mawSpawnsFromAltar(GameTestHelper helper) {
        GravityMaw maw = spawn(helper, GravityMaw.class);
        maw.setScripted(true);
        helper.assertTrue(maw.home() != null && maw.home().equals(helper.absolutePos(new BlockPos(10, 2, 9))), "Home should be the spawn point, was " + maw.home());
        helper.assertTrue(helper.absolutePos(ALTAR).equals(maw.altar()), "Altar should be bound");
        checkStats(helper, maw, 340.0, 14.0, 12.0, 0.2);
        helper.assertTrue(maw.bossBar().getColor() == net.minecraft.world.BossEvent.BossBarColor.PURPLE, "Bar should be purple");
        List<ServerPlayer> players = new ArrayList<>();
        try {
            ServerPlayer p = player(helper, new Vec3(10.5, 2, 14.5), players);
            helper.assertTrue(maw.bossBar().getPlayers().isEmpty(), "No viewers before the boss ticks");
            fullTicks(maw, 12);
            helper.assertTrue(maw.bossBar().getPlayers().contains(p), "Arena player should see the boss bar");
            helper.assertTrue(maw.isScaled(), "Health scaling is fixed when the fight starts");
        } finally {
            cleanup(helper, players);
        }
        maw.discard();
        helper.succeed();
    }

    @GameTest(template = MAW_TEMPLATE, batch = BATCH)
    public static void mawPullMovesOnlyUnanchoredPlayers(GameTestHelper helper) {
        List<ServerPlayer> players = new ArrayList<>();
        try {
            GravityMaw maw = spawn(helper, GravityMaw.class);
            maw.setScripted(true);
            ServerPlayer free = player(helper, new Vec3(16.5, 2, 9.5), players);
            ServerPlayer anchored = player(helper, new Vec3(4.5, 2, 9.5), players);
            helper.setBlock(new BlockPos(4, 2, 11), FieldRegistry.GRAVITY_ANCHOR.get());
            helper.assertTrue(GravityMaw.isAnchoredNear(anchored), "Player beside an anchor counts as anchored");
            helper.assertTrue(!GravityMaw.isAnchoredNear(free), "Player far from any anchor is not anchored");
            maw.startPull();
            tick(maw, GravityMaw.PULL_WINDUP - 1);
            helper.assertTrue(free.getDeltaMovement().lengthSqr() < 1.0E-6, "No pull during the telegraph");
            tick(maw, 1);
            helper.assertTrue(maw.moveState() == GravityMaw.STATE_PULLING, "Pull starts after the 30-tick telegraph");
            helper.assertTrue(free.getDeltaMovement().lengthSqr() < 1.0E-6, "First pulling tick not yet applied");
            tick(maw, 3);
            Vec3 toMaw = maw.position().subtract(free.position()).normalize();
            helper.assertTrue(free.getDeltaMovement().dot(toMaw) > 0.2, "Unanchored player is pulled toward the Maw: " + free.getDeltaMovement());
            helper.assertTrue(anchored.getDeltaMovement().lengthSqr() < 1.0E-6, "Anchored player is not pulled: " + anchored.getDeltaMovement());
            tick(maw, GravityMaw.PULL_TICKS);
            helper.assertTrue(maw.isGorged(), "The Maw is gorged after the pull");
            maw.discard();
            helper.succeed();
        } finally {
            cleanup(helper, players);
        }
    }

    @GameTest(template = MAW_TEMPLATE, batch = BATCH)
    public static void mawGorgedTakesOneAndAHalfDamage(GameTestHelper helper) {
        GravityMaw maw = spawn(helper, GravityMaw.class);
        maw.setScripted(true);
        net.minecraft.world.damagesource.DamageSource src = maw.damageSources().generic();
        helper.assertTrue(near(maw.modifyIncomingDamage(src, 10.0F), 10.0), "Not gorged: unchanged");
        maw.setHealth(maw.getMaxHealth());
        maw.invulnerableTime = 0;
        maw.hurt(src, 20.0F);
        float normalLoss = maw.getMaxHealth() - maw.getHealth();
        maw.setHealth(maw.getMaxHealth());
        maw.invulnerableTime = 0;
        maw.setMoveState(GravityMaw.STATE_GORGED, 0);
        helper.assertTrue(near(maw.modifyIncomingDamage(src, 10.0F), 15.0), "Gorged: x1.5");
        maw.hurt(src, 20.0F);
        float gorgedLoss = maw.getMaxHealth() - maw.getHealth();
        helper.assertTrue(gorgedLoss > normalLoss * 1.3F, "Gorged loss " + gorgedLoss + " should exceed normal loss " + normalLoss);
        maw.discard();
        helper.succeed();
    }

    @GameTest(template = MAW_TEMPLATE, batch = BATCH)
    public static void mawRepulseTelegraphsThenHitsInRadius(GameTestHelper helper) {
        List<ServerPlayer> players = new ArrayList<>();
        try {
            GravityMaw maw = spawn(helper, GravityMaw.class);
            maw.setScripted(true);
            ServerPlayer close = player(helper, new Vec3(13.5, 2, 9.5), players);
            ServerPlayer far = player(helper, new Vec3(10.5, 2, 18.5), players);
            maw.startRepulse();
            tick(maw, GravityMaw.REPULSE_WINDUP - 1);
            helper.assertTrue(close.getHealth() == close.getMaxHealth(), "No damage during the wind-up");
            helper.assertTrue(close.getDeltaMovement().lengthSqr() < 1.0E-6, "No push during the wind-up");
            tick(maw, 1);
            helper.assertTrue(close.getHealth() < close.getMaxHealth(), "Player inside the ring is hurt");
            helper.assertTrue(close.getDeltaMovement().x > 0.5, "Pushed outward: " + close.getDeltaMovement());
            helper.assertTrue(close.hasEffect(MobEffects.SLOW_FALLING), "Slow Falling follows the push");
            helper.assertTrue(far.getHealth() == far.getMaxHealth(), "Player outside the ring is untouched");
            helper.assertTrue(!far.hasEffect(MobEffects.SLOW_FALLING), "Player outside gets no effect");
            maw.discard();
            helper.succeed();
        } finally {
            cleanup(helper, players);
        }
    }

    @GameTest(template = MAW_TEMPLATE, batch = BATCH)
    public static void mawBiteTelegraphsThenHits(GameTestHelper helper) {
        List<ServerPlayer> players = new ArrayList<>();
        try {
            GravityMaw maw = spawn(helper, GravityMaw.class);
            maw.setScripted(true);
            ServerPlayer close = player(helper, new Vec3(10.5, 2, 12.0), players);
            ServerPlayer far = player(helper, new Vec3(10.5, 2, 19.5), players);
            maw.startBite();
            tick(maw, GravityMaw.BITE_WINDUP - 1);
            helper.assertTrue(maw.getMawPose() == GravityMaw.POSE_JAW || maw.moveState() == GravityMaw.STATE_BITE_WIND, "Jaw is open during the wind-up");
            helper.assertTrue(close.getHealth() == close.getMaxHealth(), "No bite during the 12-tick wind-up");
            tick(maw, 1);
            helper.assertTrue(close.getHealth() < close.getMaxHealth(), "Bite lands after the wind-up");
            helper.assertTrue(far.getHealth() == far.getMaxHealth(), "Bite only reaches nearby players");
            maw.discard();
            helper.succeed();
        } finally {
            cleanup(helper, players);
        }
    }

    @GameTest(template = MAW_TEMPLATE, batch = BATCH)
    public static void mawDebrisMarksZonesNearPlayers(GameTestHelper helper) {
        List<ServerPlayer> players = new ArrayList<>();
        try {
            GravityMaw maw = spawn(helper, GravityMaw.class);
            maw.setScripted(true);
            ServerPlayer p = player(helper, new Vec3(14.5, 2, 14.5), players);
            maw.setHealth(maw.getMaxHealth() * 0.4F);
            maw.updatePhase();
            maw.startDebris();
            tick(maw, GravityMaw.DEBRIS_WINDUP - 1);
            helper.assertTrue(markers(helper, maw).isEmpty(), "No impact zones during the rumble");
            tick(maw, 1);
            List<StarfallMarker> zones = markers(helper, maw);
            helper.assertTrue(zones.size() >= 4 && zones.size() <= 6, "4-6 impact zones expected, got " + zones.size());
            for (StarfallMarker zone : zones) {
                helper.assertTrue(zone.position().distanceTo(p.position()) <= 6.0, "Zones are marked near the player");
                helper.assertTrue(zone.remainingTicks() >= 49, "Zones warn for 50 ticks");
                zone.discard();
            }
            maw.discard();
            helper.succeed();
        } finally {
            cleanup(helper, players);
        }
    }

    @GameTest(template = MAW_TEMPLATE, batch = BATCH)
    public static void mawPhasesSwitchAtHalfHealth(GameTestHelper helper) {
        GravityMaw maw = spawn(helper, GravityMaw.class);
        maw.setScripted(true);
        maw.setHealth(maw.getMaxHealth() * 0.51F);
        maw.updatePhase();
        helper.assertTrue(maw.phase() == 1, "Above 50%: phase 1");
        maw.setHealth(maw.getMaxHealth() * 0.5F);
        maw.updatePhase();
        helper.assertTrue(maw.phase() == 2, "At 50%: phase 2");
        maw.discard();
        helper.succeed();
    }

    @GameTest(template = MAW_TEMPLATE, batch = BATCH)
    public static void mawResetsWithoutPlayers(GameTestHelper helper) {
        VestigeBoss.overrideResetTicksForTests(20);
        GravityMaw maw = spawn(helper, GravityMaw.class);
        maw.setHealth(100.0F);
        maw.setMoveState(GravityMaw.STATE_GORGED, 10);
        com.relicweave.vestige.hazard.field.StarfallImpacts.spawnMarker(helper.getLevel(), maw.position(), 500, 5.0F, maw);
        helper.assertTrue(markers(helper, maw).size() == 1, "Marker spawned");
        try {
            fullTicks(maw, 5);
            helper.assertTrue(maw.getHealth() < maw.getMaxHealth(), "Not reset after only 5 ticks without players");
            fullTicks(maw, 35);
            helper.assertTrue(maw.getHealth() == maw.getMaxHealth(), "Reset heals fully, health " + maw.getHealth());
            helper.assertTrue(maw.moveState() == GravityMaw.STATE_IDLE, "Reset clears the move state");
            helper.assertTrue(markers(helper, maw).isEmpty(), "Reset removes its impact markers");
        } finally {
            VestigeBoss.overrideResetTicksForTests(null);
        }
        maw.discard();
        helper.succeed();
    }

    @GameTest(template = MAW_TEMPLATE, batch = BATCH)
    public static void mawDeathAwardsAdvancementOnce(GameTestHelper helper) {
        List<ServerPlayer> players = new ArrayList<>();
        try {
            GravityMaw maw = spawn(helper, GravityMaw.class);
            maw.setScripted(true);
            ServerPlayer p = player(helper, new Vec3(10.5, 2, 14.5), players);
            checkLoot(helper, maw, "gravity_maw", VestigeMaterials.GRAVITY_HEART.get(), 2, VestigeMaterials.RIFTGLASS.get(), 3, 6, 2, 3);
            helper.assertTrue(helper.getLevel().getServer().getAdvancements().getAdvancement(BossRewards.advancementId(maw.bossId())) != null, "Advancement json must load");
            helper.assertTrue(!BossRewards.hasDefeated(p, maw.bossId()), "Not defeated yet");
            maw.hurt(maw.damageSources().playerAttack(p), Float.MAX_VALUE);
            helper.assertTrue(!maw.isAlive(), "Maw dies");
            helper.assertTrue(BossRewards.hasDefeated(p, maw.bossId()), "Advancement awarded to the arena player");
            helper.succeed();
        } finally {
            cleanup(helper, players);
        }
    }

    @GameTest(template = MAW_TEMPLATE, batch = BATCH)
    public static void mawNbtRoundTrip(GameTestHelper helper) {
        GravityMaw maw = spawn(helper, GravityMaw.class);
        maw.setScripted(true);
        maw.setMoveState(GravityMaw.STATE_PULLING, 17);
        maw.setCooldowns(11, 22, 33, 44);
        GravityMaw copy = roundTrip(helper, maw);
        helper.assertTrue(copy.moveState() == GravityMaw.STATE_PULLING && copy.stateTicks() == 17, "Move state and ticks saved");
        helper.assertTrue(copy.pullCooldown() == 11, "Cooldowns saved");
        helper.assertTrue(copy.home() != null && copy.home().equals(maw.home()) && copy.altar() != null, "Arena binding saved");
        maw.discard();
        helper.succeed();
    }

    // ================= Starforged Colossus =================

    @GameTest(template = COLOSSUS_TEMPLATE, batch = BATCH)
    public static void colossusSpawnsFromAltar(GameTestHelper helper) {
        StarforgedColossus boss = spawn(helper, StarforgedColossus.class);
        boss.setScripted(true);
        helper.assertTrue(boss.home() != null && boss.home().equals(helper.absolutePos(new BlockPos(10, 2, 9))), "Home bound to the spawn point");
        helper.assertTrue(helper.absolutePos(ALTAR).equals(boss.altar()), "Altar bound");
        checkStats(helper, boss, 420.0, 16.0, 16.0, 0.18);
        helper.assertTrue(boss.getType().fireImmune(), "Colossus is fire immune");
        helper.assertTrue(boss.bossBar().getColor() == net.minecraft.world.BossEvent.BossBarColor.RED, "Bar should be red");
        List<ServerPlayer> players = new ArrayList<>();
        try {
            ServerPlayer p = player(helper, new Vec3(10.5, 2, 15.5), players);
            helper.assertTrue(boss.bossBar().getPlayers().isEmpty(), "No viewers before the boss ticks");
            fullTicks(boss, 12);
            helper.assertTrue(boss.bossBar().getPlayers().contains(p), "Arena player should see the boss bar");
            helper.assertTrue(boss.isScaled(), "Health scaling is fixed when the fight starts");
        } finally {
            cleanup(helper, players);
        }
        boss.discard();
        helper.succeed();
    }

    @GameTest(template = COLOSSUS_TEMPLATE, batch = BATCH)
    public static void colossusArmoredTakesQuarterDamage(GameTestHelper helper) {
        StarforgedColossus boss = spawn(helper, StarforgedColossus.class);
        boss.setScripted(true);
        net.minecraft.world.damagesource.DamageSource src = boss.damageSources().generic();
        helper.assertTrue(near(boss.modifyIncomingDamage(src, 20.0F), 5.0), "Armored: 25% damage");
        boss.setVulnerableTicks(100);
        helper.assertTrue(near(boss.modifyIncomingDamage(src, 20.0F), 30.0), "Vulnerable: x1.5 damage");
        boss.setVulnerableTicks(0);
        boss.setHealth(boss.getMaxHealth());
        boss.invulnerableTime = 0;
        helper.assertTrue(!boss.armorHintShown(), "Hint not shown before the first hit");
        boss.hurt(src, 40.0F);
        float armoredLoss = boss.getMaxHealth() - boss.getHealth();
        helper.assertTrue(boss.armorHintShown(), "Armor hint is shown on the first armored hit");
        boss.setHealth(boss.getMaxHealth());
        boss.invulnerableTime = 0;
        boss.setVulnerableTicks(100);
        boss.hurt(src, 40.0F);
        float vulnerableLoss = boss.getMaxHealth() - boss.getHealth();
        helper.assertTrue(vulnerableLoss > armoredLoss * 4.0F, "Vulnerable loss " + vulnerableLoss + " must far exceed armored loss " + armoredLoss);
        boss.discard();
        helper.succeed();
    }

    @GameTest(template = COLOSSUS_TEMPLATE, batch = BATCH)
    public static void colossusLeverDuringOverheatCausesVentBurst(GameTestHelper helper) {
        StarforgedColossus boss = spawn(helper, StarforgedColossus.class);
        boss.setScripted(true);
        BlockPos a = new BlockPos(3, 2, 3);
        BlockPos b = new BlockPos(17, 2, 3);
        BlockState lever = Blocks.LEVER.defaultBlockState().setValue(LeverBlock.FACE, AttachFace.FLOOR);
        helper.setBlock(a, lever);
        helper.setBlock(b, lever);
        boss.recordLevers();
        helper.assertTrue(boss.levers().size() == 2, "Both levers recorded, got " + boss.levers().size());
        // A lever left ON before the overheat is switched off when the overheat begins and does not count.
        helper.setBlock(a, lever.setValue(LeverBlock.POWERED, true));
        boss.startOverheat();
        tickVents(boss, StarforgedColossus.OVERHEAT_WARNING - 1);
        helper.assertTrue(!boss.isOverheating() && boss.overheatWarn() > 0, "Still telegraphing");
        tickVents(boss, 1);
        helper.assertTrue(boss.isOverheating(), "Overheating after the 40-tick telegraph");
        helper.assertTrue(!boss.isVulnerable(), "Overheating alone does not make it vulnerable when levers exist");
        helper.assertTrue(!helper.getBlockState(a).getValue(LeverBlock.POWERED), "Pre-flipped lever was reset");
        tickVents(boss, 5);
        helper.assertTrue(!boss.isVulnerable(), "Pre-flipped lever does not trigger a burst");
        helper.setBlock(b, lever.setValue(LeverBlock.POWERED, true));
        helper.setBlock(a, lever.setValue(LeverBlock.POWERED, true));
        tickVents(boss, 1);
        helper.assertTrue(boss.isVulnerable() && boss.vulnerableTicks() >= 199, "Vent Burst makes it vulnerable for 200 ticks");
        helper.assertTrue(!boss.isOverheating(), "Overheat ends with the burst");
        helper.assertTrue(!helper.getBlockState(a).getValue(LeverBlock.POWERED) && !helper.getBlockState(b).getValue(LeverBlock.POWERED),
                "Every recorded lever is switched back off");
        helper.setBlock(a, Blocks.AIR);   // levers left behind would be recorded by other tests' arenas
        helper.setBlock(b, Blocks.AIR);
        boss.discard();
        helper.succeed();
    }

    @GameTest(template = COLOSSUS_TEMPLATE, batch = BATCH)
    public static void colossusWithoutLeversBecomesVulnerableOnOverheat(GameTestHelper helper) {
        StarforgedColossus boss = spawn(helper, StarforgedColossus.class);
        boss.setScripted(true);
        boss.recordLevers();
        helper.assertTrue(boss.levers().isEmpty(), "No levers in this arena");
        boss.startOverheat();
        tickVents(boss, StarforgedColossus.OVERHEAT_WARNING - 1);
        helper.assertTrue(!boss.isVulnerable(), "Not vulnerable during the telegraph");
        tickVents(boss, 1);
        helper.assertTrue(boss.isVulnerable(), "Overheat makes it vulnerable automatically when no levers exist");
        boss.discard();
        helper.succeed();
    }

    @GameTest(template = COLOSSUS_TEMPLATE, batch = BATCH)
    public static void colossusSlamTelegraphsThenHitsGroundedPlayers(GameTestHelper helper) {
        List<ServerPlayer> players = new ArrayList<>();
        try {
            StarforgedColossus boss = spawn(helper, StarforgedColossus.class);
            boss.setScripted(true);
            ServerPlayer close = player(helper, new Vec3(13.5, 2, 9.5), players);
            ServerPlayer jumper = player(helper, new Vec3(7.5, 4.0, 9.5), players);
            ServerPlayer far = player(helper, new Vec3(10.5, 2, 18.5), players);
            boss.startSlam();
            tick(boss, StarforgedColossus.SLAM_WINDUP - 1);
            helper.assertTrue(close.getHealth() == close.getMaxHealth(), "No damage during the 30-tick wind-up");
            tick(boss, 1);
            helper.assertTrue(close.getHealth() <= close.getMaxHealth() - 8.0F, "Slam hits hard (16 before difficulty scaling), health " + close.getHealth());
            helper.assertTrue(far.getHealth() == far.getMaxHealth(), "Players outside the ring are safe");
            helper.assertTrue(jumper.getHealth() == jumper.getMaxHealth(), "Jumping over the shockwave avoids it");
            boss.discard();
            helper.succeed();
        } finally {
            cleanup(helper, players);
        }
    }

    @GameTest(template = COLOSSUS_TEMPLATE, batch = BATCH)
    public static void colossusHeatPulseSparesPlayersBehindCover(GameTestHelper helper) {
        List<ServerPlayer> players = new ArrayList<>();
        try {
            StarforgedColossus boss = spawn(helper, StarforgedColossus.class);
            boss.setScripted(true);
            ServerPlayer open = player(helper, new Vec3(15.5, 2, 17.5), players);
            ServerPlayer covered = player(helper, new Vec3(5.5, 2, 17.5), players);
            for (int x = 2; x <= 9; x++) {
                for (int y = 2; y <= 8; y++) {
                    helper.setBlock(new BlockPos(x, y, 13), Blocks.STONE);
                }
            }
            helper.assertTrue(!boss.isCovered(open), "Open player has line of sight");
            helper.assertTrue(boss.isCovered(covered), "Wall blocks line of sight");
            boss.startPulse();
            tick(boss, StarforgedColossus.PULSE_WINDUP - 1);
            helper.assertTrue(open.getHealth() == open.getMaxHealth(), "No heat damage during the 40-tick telegraph");
            tick(boss, 1);
            helper.assertTrue(open.getHealth() < open.getMaxHealth(), "Exposed player is burned");
            helper.assertTrue(open.getRemainingFireTicks() > 0, "Exposed player is set on fire");
            helper.assertTrue(covered.getHealth() == covered.getMaxHealth(), "Player behind a wall is spared");
            boss.discard();
            helper.succeed();
        } finally {
            cleanup(helper, players);
        }
    }

    @GameTest(template = COLOSSUS_TEMPLATE, batch = BATCH)
    public static void colossusBarrageMarksEveryPlayer(GameTestHelper helper) {
        List<ServerPlayer> players = new ArrayList<>();
        try {
            StarforgedColossus boss = spawn(helper, StarforgedColossus.class);
            boss.setScripted(true);
            ServerPlayer a = player(helper, new Vec3(5.5, 2, 15.5), players);
            ServerPlayer b = player(helper, new Vec3(15.5, 2, 15.5), players);
            boss.setHealth(boss.getMaxHealth() * 0.4F);
            boss.updatePhase();
            helper.assertTrue(boss.phase() == 2, "Phase 2 below half health");
            boss.startBarrage();
            tick(boss, StarforgedColossus.BARRAGE_WINDUP - 1);
            helper.assertTrue(markers(helper, boss).isEmpty(), "No markers during the telegraph");
            tick(boss, 1);
            List<StarfallMarker> zones = markers(helper, boss);
            helper.assertTrue(zones.size() == 2, "One marker per player, got " + zones.size());
            boolean atA = false;
            boolean atB = false;
            for (StarfallMarker zone : zones) {
                atA |= zone.position().distanceTo(a.position()) < 0.5;
                atB |= zone.position().distanceTo(b.position()) < 0.5;
                helper.assertTrue(zone.remainingTicks() >= 59, "Barrage warns for 60 ticks");
                zone.discard();
            }
            helper.assertTrue(atA && atB, "Markers sit at the players' positions");
            boss.discard();
            helper.succeed();
        } finally {
            cleanup(helper, players);
        }
    }

    @GameTest(template = COLOSSUS_TEMPLATE, batch = BATCH)
    public static void colossusPhasesSwitchAtHalfHealth(GameTestHelper helper) {
        StarforgedColossus boss = spawn(helper, StarforgedColossus.class);
        boss.setScripted(true);
        boss.setHealth(boss.getMaxHealth() * 0.51F);
        boss.updatePhase();
        helper.assertTrue(boss.phase() == 1, "Above 50%: phase 1");
        boss.setHealth(boss.getMaxHealth() * 0.5F);
        boss.updatePhase();
        helper.assertTrue(boss.phase() == 2, "At 50%: phase 2");
        boss.discard();
        helper.succeed();
    }

    @GameTest(template = COLOSSUS_TEMPLATE, batch = BATCH)
    public static void colossusResetsWithoutPlayers(GameTestHelper helper) {
        VestigeBoss.overrideResetTicksForTests(20);
        StarforgedColossus boss = spawn(helper, StarforgedColossus.class);
        boss.setHealth(100.0F);
        boss.setVulnerableTicks(150);
        com.relicweave.vestige.hazard.field.StarfallImpacts.spawnMarker(helper.getLevel(), boss.position(), 500, 5.0F, boss);
        try {
            fullTicks(boss, 5);
            helper.assertTrue(boss.getHealth() < boss.getMaxHealth(), "Not reset after only 5 ticks without players");
            fullTicks(boss, 35);
            helper.assertTrue(boss.getHealth() == boss.getMaxHealth(), "Reset heals fully, health " + boss.getHealth());
            helper.assertTrue(!boss.isVulnerable() && !boss.isOverheating(), "Reset clears vent state");
            helper.assertTrue(markers(helper, boss).isEmpty(), "Reset removes its impact markers");
        } finally {
            VestigeBoss.overrideResetTicksForTests(null);
        }
        boss.discard();
        helper.succeed();
    }

    @GameTest(template = COLOSSUS_TEMPLATE, batch = BATCH)
    public static void colossusDeathAwardsAdvancementOnce(GameTestHelper helper) {
        List<ServerPlayer> players = new ArrayList<>();
        try {
            StarforgedColossus boss = spawn(helper, StarforgedColossus.class);
            boss.setScripted(true);
            ServerPlayer p = player(helper, new Vec3(10.5, 2, 15.5), players);
            checkLoot(helper, boss, "starforged_colossus", VestigeMaterials.STARFORGED_CORE.get(), 2, VestigeMaterials.RIFTGLASS.get(), 4, 8, 3, 5);
            helper.assertTrue(helper.getLevel().getServer().getAdvancements().getAdvancement(BossRewards.advancementId(boss.bossId())) != null, "Advancement json must load");
            helper.assertTrue(!BossRewards.hasDefeated(p, boss.bossId()), "Not defeated yet");
            boss.hurt(boss.damageSources().playerAttack(p), Float.MAX_VALUE);
            helper.assertTrue(!boss.isAlive(), "Colossus dies");
            helper.assertTrue(BossRewards.hasDefeated(p, boss.bossId()), "Advancement awarded to the arena player");
            helper.assertTrue(!BossRewards.award(p, boss.bossId()), "Second award returns false");
            helper.succeed();
        } finally {
            cleanup(helper, players);
        }
    }

    @GameTest(template = COLOSSUS_TEMPLATE, batch = BATCH)
    public static void colossusNbtRoundTrip(GameTestHelper helper) {
        StarforgedColossus boss = spawn(helper, StarforgedColossus.class);
        boss.setScripted(true);
        BlockPos lever = new BlockPos(3, 2, 3);
        helper.setBlock(lever, Blocks.LEVER.defaultBlockState().setValue(LeverBlock.FACE, AttachFace.FLOOR));
        boss.recordLevers();
        boss.hurt(boss.damageSources().generic(), 1.0F);   // armored hit: shows the hint once
        boss.setVulnerableTicks(123);
        boss.startOverheat();
        boss.setMoveState(StarforgedColossus.STATE_PULSE_WIND, 9);
        StarforgedColossus copy = roundTrip(helper, boss);
        helper.assertTrue(copy.vulnerableTicks() == 123, "Vulnerability timer saved");
        helper.assertTrue(copy.overheatWarn() == StarforgedColossus.OVERHEAT_WARNING, "Overheat telegraph saved");
        helper.assertTrue(copy.moveState() == StarforgedColossus.STATE_PULSE_WIND, "Move state saved");
        helper.assertTrue(copy.armorHintShown(), "Hint flag saved");
        helper.assertTrue(copy.levers().size() == boss.levers().size() && copy.levers().contains(helper.absolutePos(lever)), "Recorded levers saved");
        helper.assertTrue(copy.home() != null && copy.home().equals(boss.home()), "Arena binding saved");
        helper.setBlock(lever, Blocks.AIR);   // levers outside the vent tests would be picked up by neighbouring arenas
        boss.discard();
        helper.succeed();
    }

    @GameTest(template = MAW_TEMPLATE, batch = BATCH)
    public static void mawPhaseTwoShortensCooldowns(GameTestHelper helper) {
        GravityMaw maw = spawn(helper, GravityMaw.class);
        maw.setScripted(true);
        maw.startPull();
        int phaseOne = maw.pullCooldown();
        maw.setMoveState(GravityMaw.STATE_IDLE, 0);
        maw.setHealth(maw.getMaxHealth() * 0.3F);
        maw.updatePhase();
        maw.startPull();
        int phaseTwo = maw.pullCooldown();
        helper.assertTrue(phaseOne == 300, "Phase 1 pull cooldown is 300 ticks, was " + phaseOne);
        helper.assertTrue(phaseTwo == 240, "Phase 2 cooldowns are x0.8, was " + phaseTwo);
        maw.discard();
        helper.succeed();
    }

    @GameTest(template = COLOSSUS_TEMPLATE, batch = BATCH)
    public static void colossusVulnerableWindowEnds(GameTestHelper helper) {
        StarforgedColossus boss = spawn(helper, StarforgedColossus.class);
        boss.setScripted(true);
        helper.assertTrue(boss.levers().isEmpty(), "No recorded levers, so nothing but time ends the window");
        boss.setVulnerableTicks(StarforgedColossus.VULNERABLE_TICKS);
        tickVents(boss, StarforgedColossus.VULNERABLE_TICKS - 1);
        helper.assertTrue(boss.isVulnerable(), "Still vulnerable one tick before the window ends");
        tickVents(boss, 1);
        helper.assertTrue(!boss.isVulnerable(), "Armored again after 200 ticks");
        helper.assertTrue(near(boss.modifyIncomingDamage(boss.damageSources().generic(), 20.0F), 5.0), "Armor is back");
        boss.discard();
        helper.succeed();
    }

    @GameTest(template = COLOSSUS_TEMPLATE, batch = BATCH)
    public static void colossusOverheatEndsWithoutBurstIfNoLeverIsFlipped(GameTestHelper helper) {
        StarforgedColossus boss = spawn(helper, StarforgedColossus.class);
        boss.setScripted(true);
        BlockPos pos = new BlockPos(3, 2, 3);
        helper.setBlock(pos, Blocks.LEVER.defaultBlockState().setValue(LeverBlock.FACE, AttachFace.FLOOR));
        boss.recordLevers();
        helper.assertTrue(boss.levers().size() == 1, "One lever recorded, got " + boss.levers().size());
        boss.startOverheat();
        tickVents(boss, StarforgedColossus.OVERHEAT_WARNING + StarforgedColossus.OVERHEAT_TICKS - 1);
        helper.assertTrue(boss.isOverheating(), "Vents are open for 160 ticks");
        tickVents(boss, 1);
        helper.assertTrue(!boss.isOverheating() && !boss.isVulnerable(), "Overheat ends and the Colossus stays armored");
        helper.setBlock(pos, Blocks.AIR);
        boss.discard();
        helper.succeed();
    }
}
