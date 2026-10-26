package com.relicweave.gametest;

import com.mojang.authlib.GameProfile;
import com.relicweave.RelicWeave;
import com.relicweave.item.RelicItem;
import com.relicweave.relic.RelicActivation;
import com.relicweave.relic.RelicAugment;
import com.relicweave.relic.RelicCooldowns;
import com.relicweave.relic.RelicLocator;
import com.relicweave.relic.RelicType;
import com.relicweave.vestige.VestigeKeys;
import com.relicweave.vestige.entity.humanoid.HumanoidMobs;
import com.relicweave.vestige.hazard.HazardProtection;
import com.relicweave.vestige.puzzle.PuzzleBlocks;
import com.relicweave.vestige.relic.GravityNail;
import com.relicweave.vestige.relic.MnemonicSpindle;
import com.relicweave.vestige.relic.RiftglassLantern;
import com.relicweave.vestige.relic.SealkeepersSignet;
import com.relicweave.vestige.relic.SurveyorScan;
import com.relicweave.vestige.relic.VestigeRelics;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.UUID;

/** Server-side tests for the six 0.0.4 Vestige relics (T15). Each test spawns only what it needs near its centre. */
@GameTestHolder(RelicWeave.MOD_ID)
@PrefixGameTestTemplate(false)
public final class VestigeRelicGameTests {
    private static final String TEMPLATE = "platform";
    /** Own batch: batches run one after another, so these tests never share the grid with other mob tests. */
    private static final String BATCH = "vestigeRelics";

    private static final List<RelicType> TYPES = List.of(RelicType.SURVEYORS_LENS, RelicType.GRAVITY_NAIL, RelicType.CYCLE_DIAL,
            RelicType.RIFTGLASS_LANTERN, RelicType.MNEMONIC_SPINDLE, RelicType.SEALKEEPERS_SIGNET);

    private VestigeRelicGameTests() {}

    private static ServerPlayer makePlayer(GameTestHelper helper, double x, double z) {
        ServerPlayer player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "relicweave-vrelic-test"));
        player.setGameMode(GameType.SURVIVAL);
        Vec3 pos = helper.absoluteVec(new Vec3(x, 1.0, z));
        player.moveTo(pos.x, pos.y, pos.z, 0.0F, 0.0F);
        return player;
    }

    private static ItemStack stack(RelicType type) {
        return new ItemStack(ForgeRegistries.ITEMS.getValue(type.itemId()));
    }

    private static ItemStack augmented(RelicAugment augment) {
        ItemStack stack = stack(augment.relic());
        augment.apply(stack);
        return stack;
    }

    // ---- Items and cooldowns ----

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void relicItemsExistAndMatchTheirType(GameTestHelper helper) {
        for (RelicType type : TYPES) {
            Item item = ForgeRegistries.ITEMS.getValue(type.itemId());
            helper.assertTrue(item instanceof RelicItem, type.itemPath() + " should be a registered RelicItem");
            helper.assertTrue(((RelicItem) item).getRelicType() == type, type.itemPath() + " should map to " + type);
            helper.assertTrue(new ItemStack(item).getMaxStackSize() == 1, type.itemPath() + " should not stack");
        }
        helper.assertTrue(VestigeRelics.SURVEYORS_LENS.get() == ForgeRegistries.ITEMS.getValue(RelicWeave.id("surveyors_lens")), "Registry object mismatch");
        helper.assertFalse(RelicType.CYCLE_DIAL.activatable(), "The Cycle Dial is passive");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void cooldownStartsOnlyOnSuccessAndSurvivesMovingTheItem(GameTestHelper helper) {
        ServerPlayer player = makePlayer(helper, 2.5, 2.5);
        player.getInventory().setItem(0, stack(RelicType.SEALKEEPERS_SIGNET));
        player.getInventory().setItem(1, stack(RelicType.GRAVITY_NAIL));
        player.getInventory().setItem(2, stack(RelicType.MNEMONIC_SPINDLE));
        player.getInventory().setItem(3, stack(RelicType.SURVEYORS_LENS));
        // The Vestige only relics refuse elsewhere, and the spindle needs a terminal: none of these may start a cooldown.
        helper.assertFalse(RelicActivation.tryActivate(player, RelicType.SEALKEEPERS_SIGNET), "Signet must refuse outside The Vestige");
        helper.assertFalse(RelicActivation.tryActivate(player, RelicType.GRAVITY_NAIL), "Gravity Nail must refuse outside The Vestige");
        helper.assertFalse(RelicActivation.tryActivate(player, RelicType.MNEMONIC_SPINDLE), "Spindle needs a terminal");
        for (RelicType type : List.of(RelicType.SEALKEEPERS_SIGNET, RelicType.GRAVITY_NAIL, RelicType.MNEMONIC_SPINDLE)) {
            helper.assertTrue(RelicCooldowns.isReady(player, type), type + " must not be on cooldown after a refused activation");
        }
        helper.assertFalse(GravityNail.isAnchored(player), "A refused Gravity Nail must not anchor");

        // The Surveyor's Lens works anywhere.
        helper.assertTrue(RelicActivation.tryActivate(player, RelicType.SURVEYORS_LENS), "Lens should activate anywhere");
        int remaining = RelicCooldowns.remainingTicks(player, RelicType.SURVEYORS_LENS);
        helper.assertTrue(remaining == RelicType.SURVEYORS_LENS.cooldownTicks(), "Lens cooldown should be the configured one, got " + remaining);
        player.getInventory().setItem(3, ItemStack.EMPTY);
        player.setItemSlot(EquipmentSlot.OFFHAND, stack(RelicType.SURVEYORS_LENS));
        helper.assertTrue(RelicCooldowns.remainingTicks(player, RelicType.SURVEYORS_LENS) == remaining, "Moving the lens must not change the cooldown");
        helper.assertFalse(RelicActivation.tryActivate(player, RelicType.SURVEYORS_LENS), "A second activation must wait for the cooldown");
        helper.succeed();
    }

    // ---- Surveyor's Lens ----

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void surveyorFindsFalseWallAndSealButNotChest(GameTestHelper helper) {
        ServerPlayer player = makePlayer(helper, 2.5, 2.5);
        BlockPos wall = helper.absolutePos(new BlockPos(1, 2, 3));
        BlockPos seal = helper.absolutePos(new BlockPos(3, 2, 3));
        BlockPos chest = helper.absolutePos(new BlockPos(3, 2, 1));
        helper.setBlock(new BlockPos(1, 2, 3), PuzzleBlocks.FALSE_WALL.get());
        helper.setBlock(new BlockPos(3, 2, 3), PuzzleBlocks.GLYPH_SEAL.get());
        helper.setBlock(new BlockPos(3, 2, 1), Blocks.CHEST);
        List<BlockPos> found = SurveyorScan.scan(helper.getLevel(), player.blockPosition(), SurveyorScan.radius(stack(RelicType.SURVEYORS_LENS)));
        helper.assertTrue(found.contains(wall), "The false wall should be revealed");
        helper.assertTrue(found.contains(seal), "The glyph seal should be revealed");
        helper.assertFalse(found.contains(chest), "Containers must never be revealed");
        helper.assertTrue(found.size() <= 256, "At most 256 positions");
        for (int i = 1; i < found.size(); i++) {
            helper.assertTrue(found.get(i - 1).distSqr(player.blockPosition()) <= found.get(i).distSqr(player.blockPosition()), "Closest first");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void augmentsChangeTheNumbers(GameTestHelper helper) {
        helper.assertTrue(SurveyorScan.radius(stack(RelicType.SURVEYORS_LENS)) == 16, "Base radius 16");
        helper.assertTrue(SurveyorScan.radius(augmented(RelicAugment.SURVEYOR_WIDE)) == 24, "Wide Survey radius 24");
        helper.assertTrue(SurveyorScan.radius(augmented(RelicAugment.SURVEYOR_FOCUSED)) == 12, "Focused Survey radius 12");
        helper.assertTrue(GravityNail.durationTicks(stack(RelicType.GRAVITY_NAIL)) == 100, "Base anchor 100 ticks");
        helper.assertTrue(GravityNail.durationTicks(augmented(RelicAugment.GRAVITY_NAIL_LONG)) == 150, "Long Anchor 150 ticks");
        helper.assertTrue(GravityNail.durationTicks(augmented(RelicAugment.GRAVITY_NAIL_QUICK)) == 60, "Quick Anchor 60 ticks");
        helper.assertTrue(SealkeepersSignet.disruptTicks(augmented(RelicAugment.SIGNET_DISRUPTION)) == 90, "Disruption 90 ticks");
        helper.assertTrue(SealkeepersSignet.sealRange(augmented(RelicAugment.SIGNET_OPENING)) == 4, "Opening range 4");
        int base = RelicType.MNEMONIC_SPINDLE.cooldownTicks();
        helper.assertTrue(VestigeRelics.cooldownTicks(RelicType.MNEMONIC_SPINDLE, augmented(RelicAugment.SPINDLE_SWIFT)) == Math.round(base * 0.5F),
                "Swift Reading halves the cooldown");
        helper.assertTrue(VestigeRelics.cooldownTicks(RelicType.MNEMONIC_SPINDLE, stack(RelicType.MNEMONIC_SPINDLE)) == base, "No augment, no change");
        helper.succeed();
    }

    // ---- Gravity Nail ----

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void gravityNailAnchorHitsTheCapAndExpires(GameTestHelper helper) {
        ServerPlayer player = makePlayer(helper, 2.5, 2.5);
        helper.assertTrue(HazardProtection.reduction(player, HazardProtection.Hazard.GRAVITY_SHEAR) == 0.0, "No protection before anchoring");
        GravityNail.anchor(player, 5);
        double reduction = HazardProtection.reduction(player, HazardProtection.Hazard.GRAVITY_SHEAR);
        helper.assertTrue(Math.abs(reduction - 0.8) < 1.0E-9, "Anchored reduction should be the 0.8 cap, got " + reduction);
        helper.runAfterDelay(8, () -> {
            helper.assertFalse(GravityNail.isAnchored(player), "The anchor should have expired");
            helper.assertTrue(HazardProtection.reduction(player, HazardProtection.Hazard.GRAVITY_SHEAR) == 0.0, "No protection after expiry");
            helper.succeed();
        });
    }

    // ---- Riftglass Lantern ----

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void lanternZoneProtectsInsideNotOutside(GameTestHelper helper) {
        RiftglassLantern.clearAll();
        ServerPlayer player = makePlayer(helper, 2.5, 2.5);
        helper.assertTrue(HazardProtection.reduction(player, HazardProtection.Hazard.RIFT_SATURATION) == 0.0, "No protection before the lantern");
        RiftglassLantern.create(player, stack(RelicType.RIFTGLASS_LANTERN));
        double inside = HazardProtection.reduction(player, HazardProtection.Hazard.RIFT_SATURATION);
        helper.assertTrue(Math.abs(inside - 0.5) < 1.0E-9, "Inside the zone: 0.5, got " + inside);
        Vec3 near = player.position();
        player.moveTo(near.x + 3.5, near.y, near.z);
        helper.assertTrue(HazardProtection.reduction(player, HazardProtection.Hazard.RIFT_SATURATION) == 0.5, "3.5 blocks away is inside radius 4");
        player.moveTo(near.x + 5.0, near.y, near.z);
        helper.assertTrue(HazardProtection.reduction(player, HazardProtection.Hazard.RIFT_SATURATION) == 0.0, "5 blocks away is outside radius 4");
        RiftglassLantern.clearAll();
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void lanternBrightAndBroadNumbers(GameTestHelper helper) {
        ItemStack broad = augmented(RelicAugment.LANTERN_BROAD);
        ItemStack bright = augmented(RelicAugment.LANTERN_BRIGHT);
        ItemStack base = stack(RelicType.RIFTGLASS_LANTERN);
        helper.assertTrue(RiftglassLantern.radius(base) == 4.0 && RiftglassLantern.strength(base) == 0.5, "Base 4 / 0.5");
        helper.assertTrue(RiftglassLantern.radius(broad) == 6.0 && RiftglassLantern.strength(broad) == 0.35, "Broad 6 / 0.35");
        helper.assertTrue(RiftglassLantern.radius(bright) == 3.0 && RiftglassLantern.strength(bright) == 0.65, "Bright 3 / 0.65");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void lanternZonesDoNotStackAndOnePerPlayer(GameTestHelper helper) {
        RiftglassLantern.clearAll();
        ServerPlayer a = makePlayer(helper, 2.5, 2.5);
        ServerPlayer b = makePlayer(helper, 2.5, 2.5);
        RiftglassLantern.create(a, augmented(RelicAugment.LANTERN_BROAD));
        RiftglassLantern.create(b, augmented(RelicAugment.LANTERN_BRIGHT));
        helper.assertTrue(RiftglassLantern.activeZones() == 2, "Two owners, two zones");
        double inside = HazardProtection.reduction(a, HazardProtection.Hazard.RIFT_SATURATION);
        helper.assertTrue(Math.abs(inside - 0.65) < 1.0E-9, "Only the strongest zone (0.65) applies, got " + inside);
        RiftglassLantern.create(a, stack(RelicType.RIFTGLASS_LANTERN));
        helper.assertTrue(RiftglassLantern.activeZones() == 2, "A player owns at most one zone");
        RiftglassLantern.clearAll();
        helper.assertTrue(RiftglassLantern.activeZones() == 0, "clearAll empties the map");
        helper.succeed();
    }

    // ---- Mnemonic Spindle ----

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void spindleWithoutTerminalDoesNothing(GameTestHelper helper) {
        ServerPlayer player = makePlayer(helper, 2.5, 2.5);
        player.getInventory().setItem(0, stack(RelicType.MNEMONIC_SPINDLE));
        helper.assertFalse(MnemonicSpindle.terminalNearby(helper.getLevel(), player.blockPosition()), "No terminal placed");
        helper.assertFalse(RelicActivation.tryActivate(player, RelicType.MNEMONIC_SPINDLE), "Without a terminal nothing happens");
        helper.assertTrue(RelicCooldowns.isReady(player, RelicType.MNEMONIC_SPINDLE), "No cooldown without a terminal");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void spindleFindsTerminalWithinFiveBlocksAndPointsEightWays(GameTestHelper helper) {
        ServerPlayer player = makePlayer(helper, 0.5, 2.5);
        helper.setBlock(new BlockPos(4, 2, 2), PuzzleBlocks.ANCIENT_TERMINAL.get());
        helper.assertTrue(MnemonicSpindle.terminalNearby(helper.getLevel(), player.blockPosition()), "A terminal 4 blocks away counts");
        BlockPos o = BlockPos.ZERO;
        helper.assertTrue(MnemonicSpindle.direction(o, new BlockPos(100, 0, 0)).equals("east"), "+x is east");
        helper.assertTrue(MnemonicSpindle.direction(o, new BlockPos(0, 0, 100)).equals("south"), "+z is south");
        helper.assertTrue(MnemonicSpindle.direction(o, new BlockPos(-100, 0, -100)).equals("north_west"), "-x -z is north-west");
        helper.assertTrue(MnemonicSpindle.direction(o, new BlockPos(100, 0, -100)).equals("north_east"), "+x -z is north-east");
        helper.assertTrue(MnemonicSpindle.roughDistance(o, new BlockPos(120, 0, 0)) == 100, "120 rounds to 100");
        helper.assertTrue(MnemonicSpindle.roughDistance(o, new BlockPos(140, 0, 0)) == 150, "140 rounds to 150");
        helper.assertTrue(MnemonicSpindle.roughDistance(o, new BlockPos(10, 0, 0)) == 50, "Never below 50");
        helper.assertFalse(MnemonicSpindle.CLUE_STRUCTURES.contains(VestigeKeys.CROWN_OF_CONCORDANCE_KEEP), "The Crown keep is never a clue");
        helper.assertTrue(MnemonicSpindle.CLUE_STRUCTURES.size() == 13, "13 clue structures");
        helper.succeed();
    }

    // ---- Sealkeeper's Signet ----

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void signetDisruptsEliteNotBossAndOpensSealsInRange(GameTestHelper helper) {
        ServerPlayer player = makePlayer(helper, 0.5, 2.5);
        Mob knight = helper.spawnWithNoFreeWill(HumanoidMobs.OATHBREAKER_KNIGHT.get(), 2.5F, 2.0F, 2.5F);
        // Spawned and discarded within this single tick so neighbouring mob tests never see them.
        Mob sentinel = HumanoidMobs.RELIQUARY_SENTINEL.get().create(helper.getLevel());
        try {
            helper.setBlock(new BlockPos(2, 1, 2), PuzzleBlocks.GLYPH_SEAL.get());
            helper.setBlock(new BlockPos(3, 1, 2), PuzzleBlocks.GLYPH_SEAL.get());

            helper.assertTrue(SealkeepersSignet.canDisrupt(knight), "An Oathbreaker Knight (elite) is disruptable");
            helper.assertFalse(SealkeepersSignet.canDisrupt(sentinel), "A plain Reliquary Sentinel is not in #vestige_elites");
            for (EntityType<?> type : ForgeRegistries.ENTITY_TYPES.tags().getTag(VestigeKeys.VESTIGE_BOSSES)) {
                var entity = type.create(helper.getLevel());
                helper.assertTrue(entity instanceof LivingEntity, type + " should be a living entity");
                helper.assertFalse(SealkeepersSignet.canDisrupt((LivingEntity) entity), "Bosses must never be disruptable: " + type);
            }

            int affected = SealkeepersSignet.pulse(player, stack(RelicType.SEALKEEPERS_SIGNET));
            helper.assertTrue(affected == 2, "Knight plus the seal within 2 blocks, got " + affected);
            helper.assertTrue(knight.getEffect(MobEffects.MOVEMENT_SLOWDOWN) != null && knight.getEffect(MobEffects.MOVEMENT_SLOWDOWN).getAmplifier() == 2,
                    "Slowness III expected");
            helper.assertTrue(knight.getEffect(MobEffects.WEAKNESS) != null, "Weakness expected");
            helper.assertTrue(knight.getEffect(MobEffects.MOVEMENT_SLOWDOWN).getDuration() <= 60, "60 tick duration");
            helper.assertTrue(sentinel.getEffect(MobEffects.MOVEMENT_SLOWDOWN) == null, "The sentinel is not affected");
            helper.assertBlock(new BlockPos(2, 1, 2), b -> b == Blocks.AIR, "The seal 2 blocks away should open");
            helper.assertBlock(new BlockPos(3, 1, 2), b -> b == PuzzleBlocks.GLYPH_SEAL.get(), "The seal 3 blocks away must stay shut");

            // Opening augment reaches 4 blocks.
            SealkeepersSignet.pulse(player, augmented(RelicAugment.SIGNET_OPENING));
            helper.assertBlock(new BlockPos(3, 1, 2), b -> b == Blocks.AIR, "Opening opens the seal at 3 blocks");
        } finally {
            knight.discard();
        }
        helper.succeed();
    }
}
