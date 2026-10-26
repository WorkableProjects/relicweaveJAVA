package com.relicweave.gametest;

import com.relicweave.RelicWeave;
import com.relicweave.vestige.VestigeKeys;
import com.relicweave.vestige.boss.BossRegistry;
import com.relicweave.vestige.hazard.FieldRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Tests for the second batch of Vestige structures (T10): templates, registration, altars, loot and locating. */
@GameTestHolder(RelicWeave.MOD_ID)
@PrefixGameTestTemplate(false)
public final class StructurePart2GameTests {
    private static final String TEMPLATE = "platform";
    private static final List<String> NAMES = List.of("gravity_vault", "starfall_forge", "cycle_observatory",
            "hall_of_nine_seals", "crown_bastion", "crown_of_concordance", "rift_scar");
    private static final Set<String> BOSSES = Set.of("thorned_regent", "choir_unheard", "gravity_maw",
            "starforged_colossus", "warden_of_seals", "archivist_prime");
    private static final Map<String, String> ARENAS = Map.of("gravity_vault", "gravity_maw",
            "starfall_forge", "starforged_colossus", "hall_of_nine_seals", "warden_of_seals",
            "crown_of_concordance", "archivist_prime");

    private StructurePart2GameTests() {}

    private static StructureTemplate template(GameTestHelper helper, String name) {
        StructureTemplate t = helper.getLevel().getServer().getStructureManager().get(RelicWeave.id("vestige/" + name)).orElse(null);
        helper.assertTrue(t != null, "Template missing: " + name);
        return t;
    }

    private static List<StructureTemplate.StructureBlockInfo> blocks(StructureTemplate t, Block block) {
        return t.filterBlocks(BlockPos.ZERO, new StructurePlaceSettings(), block);
    }

    private static List<String> lootTables(StructureTemplate t) {
        List<String> out = new ArrayList<>();
        for (Block b : new Block[]{Blocks.CHEST, Blocks.BARREL}) {
            for (StructureTemplate.StructureBlockInfo info : blocks(t, b)) {
                CompoundTag nbt = info.nbt();
                if (nbt != null && nbt.contains("LootTable")) {
                    out.add(nbt.getString("LootTable"));
                }
            }
        }
        return out;
    }

    private static LootTable table(GameTestHelper helper, String path) {
        LootTable table = helper.getLevel().getServer().getLootData().getLootTable(RelicWeave.id(path));
        helper.assertTrue(table != LootTable.EMPTY, "Missing loot table " + path);
        return table;
    }

    private static List<ItemStack> roll(GameTestHelper helper, LootTable table, long seed) {
        LootParams params = new LootParams.Builder(helper.getLevel())
                .withParameter(LootContextParams.ORIGIN, Vec3.ZERO).create(LootContextParamSets.CHEST);
        return table.getRandomItems(params, seed);
    }

    @GameTest(template = TEMPLATE)
    public static void part2TemplatesLoad(GameTestHelper helper) {
        for (String name : NAMES) {
            StructureTemplate t = template(helper, name);
            helper.assertTrue(t.getSize().getX() > 0 && t.getSize().getY() > 0 && t.getSize().getZ() > 0, name + " is empty");
            helper.assertTrue(t.getSize().getX() <= 48 && t.getSize().getZ() <= 48 && t.getSize().getY() <= 48,
                    name + " exceeds the 48 block limit: " + t.getSize());
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void part2StructuresRegistered(GameTestHelper helper) {
        var access = helper.getLevel().registryAccess();
        for (String name : NAMES) {
            helper.assertTrue(access.registryOrThrow(Registries.STRUCTURE).containsKey(RelicWeave.id(name)), "Structure not registered: " + name);
            helper.assertTrue(access.registryOrThrow(Registries.STRUCTURE_SET).containsKey(RelicWeave.id(name)), "Structure set not registered: " + name);
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void part2AltarsNameKnownBosses(GameTestHelper helper) {
        for (String name : NAMES) {
            for (StructureTemplate.StructureBlockInfo info : blocks(template(helper, name), BossRegistry.BOSS_ALTAR.get())) {
                ResourceLocation boss = new ResourceLocation(info.nbt().getString("Boss"));
                helper.assertTrue(boss.getNamespace().equals(RelicWeave.MOD_ID) && BOSSES.contains(boss.getPath()),
                        name + " altar names unknown boss " + boss);
            }
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void part2ArenasHaveOneAltar(GameTestHelper helper) {
        for (Map.Entry<String, String> e : ARENAS.entrySet()) {
            List<StructureTemplate.StructureBlockInfo> altars = blocks(template(helper, e.getKey()), BossRegistry.BOSS_ALTAR.get());
            helper.assertTrue(altars.size() == 1, e.getKey() + " should have exactly one altar, found " + altars.size());
            helper.assertTrue(altars.get(0).nbt().getString("Boss").equals(RelicWeave.MOD_ID + ":" + e.getValue()),
                    e.getKey() + " altar boss is " + altars.get(0).nbt().getString("Boss"));
        }
        for (String name : List.of("cycle_observatory", "crown_bastion", "rift_scar")) {
            helper.assertTrue(blocks(template(helper, name), BossRegistry.BOSS_ALTAR.get()).isEmpty(), name + " must have no altar");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void part2LootTablesExist(GameTestHelper helper) {
        int found = 0;
        for (String name : NAMES) {
            for (String id : lootTables(template(helper, name))) {
                ResourceLocation loc = new ResourceLocation(id);
                helper.assertTrue(helper.getLevel().getServer().getLootData().getLootTable(loc) != LootTable.EMPTY, "Missing loot table " + id);
                found++;
            }
        }
        helper.assertTrue(found >= 9, "Expected chests/barrels in the templates, found " + found);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void foundryHasFourLevers(GameTestHelper helper) {
        List<StructureTemplate.StructureBlockInfo> levers = blocks(template(helper, "starfall_forge"), Blocks.LEVER);
        helper.assertTrue(levers.size() >= 4, "Foundry needs at least 4 vent levers, found " + levers.size());
        for (StructureTemplate.StructureBlockInfo info : levers) {
            helper.assertFalse(info.state().getValue(net.minecraft.world.level.block.LeverBlock.POWERED), "Lever should start unpowered");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void grandIndexHasSealGatesAndNullWards(GameTestHelper helper) {
        StructureTemplate t = template(helper, "crown_of_concordance");
        helper.assertTrue(blocks(t, BossRegistry.SEAL_GATE.get()).size() >= 3, "The Grand Index needs a line of seal gates");
        helper.assertTrue(blocks(t, FieldRegistry.NULL_WARD.get()).size() == 2, "The Grand Index needs exactly two null wards");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void sealsVaultAlwaysYieldsUpgradeTemplate(GameTestHelper helper) {
        LootTable table = table(helper, "chests/vestige/hall_of_nine_seals_vault");
        for (long seed = 1; seed <= 20; seed++) {
            int count = roll(helper, table, seed * 7919L).stream()
                    .filter(s -> s.getItem() == BossRegistry.VESTIGE_UPGRADE_TEMPLATE.get()).mapToInt(ItemStack::getCount).sum();
            helper.assertTrue(count == 1, "Roll " + seed + " yielded " + count + " upgrade templates");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void observatoryAlwaysYieldsRiftglassLens(GameTestHelper helper) {
        LootTable table = table(helper, "chests/vestige/cycle_observatory");
        var lens = net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(RelicWeave.id("riftglass_lens"));
        for (long seed = 1; seed <= 20; seed++) {
            boolean has = roll(helper, table, seed * 104729L).stream().anyMatch(s -> s.getItem() == lens);
            helper.assertTrue(has, "Roll " + seed + " had no riftglass lens");
        }
        helper.succeed();
    }

    private static void locate(GameTestHelper helper, ResourceKey<Structure> key, int radiusChunks) {
        MinecraftServer server = helper.getLevel().getServer();
        ServerLevel vestige = server.getLevel(VestigeKeys.THE_VESTIGE);
        helper.assertTrue(vestige != null, "The Vestige should be loaded");
        var holder = vestige.registryAccess().registryOrThrow(Registries.STRUCTURE).getHolderOrThrow(key);
        var found = vestige.getChunkSource().getGenerator()
                .findNearestMapStructure(vestige, HolderSet.direct(holder), BlockPos.ZERO, radiusChunks, false);
        helper.assertTrue(found != null, key.location() + " could not be located within " + radiusChunks + " chunks");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 1200)
    public static void locateGravityVault(GameTestHelper helper) {
        locate(helper, VestigeKeys.GRAVITY_VAULT, 100);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 1200)
    public static void locateStarfallForge(GameTestHelper helper) {
        locate(helper, VestigeKeys.STARFALL_FORGE, 100);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 1200)
    public static void locateCycleObservatory(GameTestHelper helper) {
        locate(helper, VestigeKeys.CYCLE_OBSERVATORY, 100);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 1200)
    public static void locateHallOfNineSeals(GameTestHelper helper) {
        locate(helper, VestigeKeys.HALL_OF_NINE_SEALS, 100);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 2400)
    public static void locateCrownBastion(GameTestHelper helper) {
        locate(helper, VestigeKeys.CROWN_BASTION, 300);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 2400)
    public static void locateCrownOfConcordance(GameTestHelper helper) {
        locate(helper, VestigeKeys.CROWN_OF_CONCORDANCE_KEEP, 300);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 1200)
    public static void locateRiftScar(GameTestHelper helper) {
        locate(helper, VestigeKeys.RIFT_SCAR, 100);
    }
}
