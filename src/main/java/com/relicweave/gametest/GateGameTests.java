package com.relicweave.gametest;

import com.mojang.authlib.GameProfile;
import com.relicweave.RelicWeave;
import com.relicweave.vestige.VestigeKeys;
import com.relicweave.vestige.gate.ArrivalPlaza;
import com.relicweave.vestige.gate.GateKeystoneBlock;
import com.relicweave.vestige.gate.GateKeystoneBlockEntity;
import com.relicweave.vestige.gate.GateRegistry;
import com.relicweave.vestige.gate.GateShape;
import com.relicweave.vestige.gate.GateTravel;
import com.relicweave.vestige.gate.SafeSpot;
import com.relicweave.vestige.gate.VestigeWorldData;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Tests for the Concordance Gate: ring detection, the activation ritual, the Arrival Plaza and travel resolution.
 * Cross-dimension teleports of mock players are not exercised; destinations and persisted data are tested through
 * {@link GateTravel#destinationFor}, {@link GateTravel#prepare} and {@link GateTravel#returnDestination}. These tests
 * need the real {@code relicweave:the_vestige} dimension to exist.
 */
@GameTestHolder(RelicWeave.MOD_ID)
@PrefixGameTestTemplate(false)
public final class GateGameTests {
    private static final String TEMPLATE = "platform";
    private static final BlockPos CENTER = new BlockPos(2, 1, 2);

    private GateGameTests() {}

    private static void buildRing(GameTestHelper helper) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                BlockPos rel = CENTER.offset(dx, 0, dz);
                helper.setBlock(rel, dx == 0 && dz == 0 ? GateRegistry.GATE_KEYSTONE.get() : GateRegistry.CONCORDANCE_FRAME.get());
            }
        }
    }

    /** The template floor is not reliably solid under the player spot, so lay our own. */
    private static void layFloor(GameTestHelper helper) {
        for (int x = 0; x < 5; x++) {
            for (int z = 0; z < 5; z++) {
                helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
            }
        }
    }

    private static ServerPlayer makePlayer(GameTestHelper helper) {
        layFloor(helper);
        ServerPlayer player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "relicweave-gate-test"));
        player.setGameMode(GameType.SURVIVAL);
        Vec3 pos = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(2, 1, 4)));
        player.moveTo(pos.x, pos.y, pos.z, 0.0F, 0.0F);
        return player;
    }

    private static GateKeystoneBlockEntity keystone(GameTestHelper helper) {
        helper.assertTrue(helper.getBlockEntity(CENTER) instanceof GateKeystoneBlockEntity, "Keystone block entity expected");
        return (GateKeystoneBlockEntity) helper.getBlockEntity(CENTER);
    }

    /**
     * Completes the dragon advancement on the mock player's progress. {@code PlayerAdvancements#award} reports false for
     * fake players in this environment, so the criterion is granted on the progress object directly.
     */
    private static void awardDragonKill(ServerPlayer player) {
        Advancement advancement = player.server.getAdvancements().getAdvancement(new ResourceLocation("minecraft", "end/kill_dragon"));
        AdvancementProgress progress = player.getAdvancements().getOrStartProgress(advancement);
        List<String> remaining = new ArrayList<>();
        progress.getRemainingCriteria().forEach(remaining::add);
        remaining.forEach(progress::grantProgress);
    }

    private static int sigils(ServerPlayer player) {
        int count = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(GateRegistry.THRESHOLD_SIGIL.get())) {
                count += stack.getCount();
            }
        }
        return count;
    }

    // ---- Ring ----

    @GameTest(template = TEMPLATE)
    public static void ringDetection(GameTestHelper helper) {
        buildRing(helper);
        BlockPos keystone = helper.absolutePos(CENTER);
        helper.assertTrue(GateShape.isComplete(helper.getLevel(), keystone), "Complete ring must be detected");
        helper.setBlock(CENTER.offset(1, 0, 1), Blocks.AIR);
        helper.assertFalse(GateShape.isComplete(helper.getLevel(), keystone), "A missing frame must break the ring");
        helper.succeed();
    }

    // ---- Activation ----

    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void activationNeedsDragonKillThenConsumesSigil(GameTestHelper helper) {
        buildRing(helper);
        ServerPlayer player = makePlayer(helper);
        player.getInventory().setItem(0, new ItemStack(GateRegistry.THRESHOLD_SIGIL.get(), 2));
        GateKeystoneBlockEntity keystone = keystone(helper);

        helper.assertFalse(keystone.tryStart(player), "Without the dragon advancement activation must be refused");
        helper.assertTrue(sigils(player) == 2, "A refused activation must not consume a sigil");

        awardDragonKill(player);
        helper.assertTrue(keystone.tryStart(player), "After killing the dragon the ritual should start");
        helper.assertFalse(keystone.tryStart(player), "Only one ritual at a time");
        helper.runAfterDelay(GateKeystoneBlockEntity.RITUAL_TICKS + 5, () -> {
            helper.assertTrue(sigils(player) == 1, "Exactly one sigil should be consumed, left " + sigils(player));
            helper.assertBlockProperty(CENTER, GateKeystoneBlock.ACTIVE, true);
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void ritualCancelsWhenFrameBreaks(GameTestHelper helper) {
        buildRing(helper);
        ServerPlayer player = makePlayer(helper);
        player.getInventory().setItem(0, new ItemStack(GateRegistry.THRESHOLD_SIGIL.get()));
        awardDragonKill(player);
        helper.assertTrue(keystone(helper).tryStart(player), "Ritual should start");
        helper.runAfterDelay(20, () -> helper.setBlock(CENTER.offset(-1, 0, 0), Blocks.AIR));
        helper.runAfterDelay(GateKeystoneBlockEntity.RITUAL_TICKS + 10, () -> {
            helper.assertTrue(sigils(player) == 1, "The sigil must be kept when the ritual is interrupted");
            helper.assertBlockProperty(CENTER, GateKeystoneBlock.ACTIVE, false);
            helper.succeed();
        });
    }

    // ---- Arrival Plaza ----

    @GameTest(template = TEMPLATE, timeoutTicks = 400)
    public static void arrivalPlazaIsSafeAndIdempotent(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        ServerLevel vestige = server.getLevel(VestigeKeys.THE_VESTIGE);
        helper.assertTrue(vestige != null, "The Vestige dimension must exist");
        BlockPos first = ArrivalPlaza.arrival(server);
        BlockPos second = ArrivalPlaza.arrival(server);
        helper.assertTrue(first != null && first.equals(second), "Arrival must be idempotent: " + first + " vs " + second);
        helper.assertTrue(SafeSpot.isSafe(vestige, first), "Arrival spot must be safe");
        VestigeWorldData data = VestigeWorldData.get(vestige);
        helper.assertTrue(data.isBuilt() && data.buildCount() == 1, "Plaza must be built exactly once, count " + data.buildCount());
        helper.assertTrue(vestige.getBlockState(data.waystone()).is(GateRegistry.RETURN_WAYSTONE.get()), "Waystone expected at the plaza centre");
        helper.succeed();
    }

    // ---- Travel ----

    @GameTest(template = TEMPLATE, timeoutTicks = 400)
    public static void travelStoresReturnPointAndCooldownBlocksSecondTrip(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        ServerPlayer player = makePlayer(helper);
        GateTravel.Destination dest = GateTravel.destinationFor(player);
        helper.assertTrue(dest != null, "Destination expected");
        helper.assertTrue(dest.level().dimension().equals(VestigeKeys.THE_VESTIGE), "Gate from the overworld leads to The Vestige");
        BlockPos arrival = ArrivalPlaza.arrival(server);
        helper.assertTrue(BlockPos.containing(dest.x(), dest.y(), dest.z()).equals(arrival), "Destination must be the arrival spot");
        helper.assertTrue(SafeSpot.isSafe(dest.level(), BlockPos.containing(dest.x(), dest.y(), dest.z())), "Arrival must be safe");

        helper.assertTrue(GateTravel.prepare(player) != null, "First trip should be allowed");
        CompoundTag point = GateTravel.returnPoint(player);
        helper.assertTrue(point.getString("dimension").equals("minecraft:overworld"), "Return dimension saved");
        helper.assertTrue(Math.abs(point.getDouble("x") - player.getX()) < 1.0E-6, "Return x saved");
        helper.assertTrue(GateTravel.prepare(player) == null, "Cooldown must block an immediate second trip");
        helper.assertTrue(GateTravel.cooldownRemaining(player) > 0, "Cooldown should be running");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void returnPointResolvesToSavedSpot(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        ServerPlayer player = makePlayer(helper);
        helper.setBlock(new BlockPos(2, 1, 4), Blocks.AIR);
        helper.setBlock(new BlockPos(2, 2, 4), Blocks.AIR);
        GateTravel.saveReturnPoint(player);
        GateTravel.Destination dest = GateTravel.returnDestination(server, GateTravel.returnPoint(player));
        helper.assertTrue(dest.level() == helper.getLevel(), "Return must target the saved level");
        helper.assertTrue(Math.abs(dest.x() - player.getX()) < 1.0E-6 && Math.abs(dest.z() - player.getZ()) < 1.0E-6
                && Math.abs(dest.y() - player.getY()) < 1.0E-6, "Return must land on the saved spot: player " + player.position() + " dest " + dest.x() + "," + dest.y() + "," + dest.z());
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void missingOrUnsafeReturnPointFallsBack(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        ServerLevel overworld = server.overworld();

        GateTravel.Destination none = GateTravel.returnDestination(server, new CompoundTag());
        helper.assertTrue(none.level() == overworld, "No return point: overworld spawn");

        CompoundTag missing = new CompoundTag();
        missing.putString("dimension", "relicweave:does_not_exist");
        missing.putDouble("x", 0);
        missing.putDouble("y", 80);
        missing.putDouble("z", 0);
        GateTravel.Destination gone = GateTravel.returnDestination(server, missing);
        helper.assertTrue(gone.level() == overworld, "Missing dimension: overworld spawn");

        CompoundTag outOfBounds = new CompoundTag();
        outOfBounds.putString("dimension", "minecraft:overworld");
        outOfBounds.putDouble("x", 0);
        outOfBounds.putDouble("y", 100000);
        outOfBounds.putDouble("z", 0);
        helper.assertTrue(GateTravel.returnDestination(server, outOfBounds).level() == overworld, "Out of bounds: overworld spawn");

        // A spot buried in stone resolves to the nearest safe spot within 8 blocks.
        layFloor(helper);
        BlockPos feetRel = new BlockPos(2, 1, 2);
        helper.setBlock(feetRel, Blocks.STONE);
        helper.setBlock(feetRel.above(), Blocks.STONE);
        BlockPos feet = helper.absolutePos(feetRel);
        CompoundTag buried = new CompoundTag();
        buried.putString("dimension", "minecraft:overworld");
        buried.putDouble("x", feet.getX() + 0.5);
        buried.putDouble("y", feet.getY());
        buried.putDouble("z", feet.getZ() + 0.5);
        GateTravel.Destination near = GateTravel.returnDestination(server, buried);
        BlockPos landed = BlockPos.containing(near.x(), near.y(), near.z());
        helper.assertTrue(near.level() == helper.getLevel() && landed.distSqr(feet) <= 64 && SafeSpot.isSafe(helper.getLevel(), landed),
                "Buried return point should resolve to a nearby safe spot, got " + landed);
        helper.succeed();
    }
}
