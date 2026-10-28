package com.relicweave.gametest;

import com.relicweave.RelicWeave;
import com.relicweave.vestige.VestigeKeys;
import com.relicweave.vestige.boss.BossRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.Set;

/** Server-side tests for the first seven Vestige structures (templates, registration, loot, altars, locating). */
@GameTestHolder(RelicWeave.MOD_ID)
@PrefixGameTestTemplate(false)
public final class StructurePart1GameTests {
    private static final String TEMPLATE = "platform";
    private static final List<String> NAMES = List.of("waystone_monolith", "archivists_outpost", "ruined_aether_aqueduct",
            "gilded_civic_temple", "glassroot_warren", "hush_archive", "sundered_observatory");
    private static final Set<String> BOSSES = Set.of("relicweave:thorned_regent", "relicweave:choir_unheard",
            "relicweave:gravity_maw", "relicweave:starforged_colossus", "relicweave:warden_of_seals", "relicweave:archivist_prime");

    private StructurePart1GameTests() {}

    private static StructureTemplate template(GameTestHelper helper, String name) {
        StructureTemplate template = helper.getLevel().getStructureManager().get(RelicWeave.id("vestige/" + name)).orElse(null);
        helper.assertTrue(template != null, "Template missing: vestige/" + name);
        return template;
    }

    private static ServerLevel vestige(GameTestHelper helper) {
        ServerLevel level = helper.getLevel().getServer().getLevel(VestigeKeys.THE_VESTIGE);
        helper.assertTrue(level != null, "The Vestige dimension should be loaded");
        return level;
    }

    private static List<StructureTemplate.StructureBlockInfo> blocks(StructureTemplate template, net.minecraft.world.level.block.Block block) {
        return template.filterBlocks(BlockPos.ZERO, new StructurePlaceSettings(), block);
    }

    @GameTest(template = TEMPLATE)
    public static void part1TemplatesLoad(GameTestHelper helper) {
        for (String name : NAMES) {
            StructureTemplate template = template(helper, name);
            var size = template.getSize();
            helper.assertTrue(size.getX() > 0 && size.getY() > 0 && size.getZ() > 0, name + " has an empty size");
            helper.assertTrue(size.getX() <= 48 && size.getZ() <= 48 && size.getY() <= 48, name + " exceeds 48x48x48: " + size);
            CompoundTag tag = template.save(new CompoundTag());
            helper.assertTrue(tag.getList("blocks", Tag.TAG_COMPOUND).size() > 20, name + " has almost no blocks");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void part1StructuresRegistered(GameTestHelper helper) {
        var structures = helper.getLevel().registryAccess().registryOrThrow(Registries.STRUCTURE);
        var sets = helper.getLevel().registryAccess().registryOrThrow(Registries.STRUCTURE_SET);
        for (String name : NAMES) {
            helper.assertTrue(structures.containsKey(RelicWeave.id(name)), "Structure not registered: " + name);
            helper.assertTrue(sets.containsKey(RelicWeave.id(name)), "Structure set not registered: " + name);
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void part1LootTablesExist(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        for (String name : NAMES) {
            StructureTemplate template = template(helper, name);
            int containers = 0;
            for (var block : List.of(Blocks.CHEST, Blocks.BARREL)) {
                for (var info : blocks(template, block)) {
                    containers++;
                    String table = info.nbt() == null ? "" : info.nbt().getString("LootTable");
                    helper.assertTrue(!table.isEmpty(), name + ": container without loot table");
                    ResourceLocation id = new ResourceLocation(table);
                    helper.assertTrue(id.getPath().startsWith("chests/vestige/" + name), name + ": unexpected loot table " + id);
                    helper.assertTrue(server.getLootData().getLootTable(id) != LootTable.EMPTY, "Missing loot table " + id);
                }
            }
            helper.assertTrue(containers > 0, name + " has no loot container");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void part1AltarsNameBosses(GameTestHelper helper) {
        for (String name : NAMES) {
            for (var info : blocks(template(helper, name), BossRegistry.BOSS_ALTAR.get())) {
                String boss = info.nbt() == null ? "" : info.nbt().getString("Boss");
                helper.assertTrue(BOSSES.contains(boss), name + ": altar names unknown boss '" + boss + "'");
            }
        }
        helper.succeed();
    }

    private static void assertSingleAltar(GameTestHelper helper, String name, String boss) {
        var altars = blocks(template(helper, name), BossRegistry.BOSS_ALTAR.get());
        helper.assertTrue(altars.size() == 1, name + " should contain exactly one altar, found " + altars.size());
        helper.assertTrue(boss.equals(altars.get(0).nbt().getString("Boss")),
                name + " altar should summon " + boss + ", got " + altars.get(0).nbt().getString("Boss"));
        int[] offset = altars.get(0).nbt().getIntArray("SpawnOffset");
        helper.assertTrue(offset.length == 3 && (offset[0] != 0 || offset[2] != 0), name + " altar spawn offset should point into the arena");
    }

    @GameTest(template = TEMPLATE)
    public static void rootCrownCourtAltar(GameTestHelper helper) {
        assertSingleAltar(helper, "glassroot_warren", "relicweave:thorned_regent");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void drownedConservatoryAltar(GameTestHelper helper) {
        assertSingleAltar(helper, "hush_archive", "relicweave:choir_unheard");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void part1EntitiesAreRegisteredMobs(GameTestHelper helper) {
        for (String name : NAMES) {
            CompoundTag tag = template(helper, name).save(new CompoundTag());
            ListTag entities = tag.getList("entities", Tag.TAG_COMPOUND);
            for (int i = 0; i < entities.size(); i++) {
                CompoundTag nbt = entities.getCompound(i).getCompound("nbt");
                ResourceLocation id = new ResourceLocation(nbt.getString("id"));
                helper.assertTrue(ForgeRegistries.ENTITY_TYPES.containsKey(id), name + ": unknown entity " + id);
                helper.assertTrue(nbt.getBoolean("PersistenceRequired"), name + ": " + id + " must be persistent");
            }
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void sunderedObservatoryAlwaysYieldsLens(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        LootTable table = level.getServer().getLootData().getLootTable(RelicWeave.id("chests/vestige/sundered_observatory"));
        helper.assertTrue(table != LootTable.EMPTY, "sundered_observatory loot table missing");
        for (int i = 0; i < 20; i++) {
            LootParams params = new LootParams.Builder(level)
                    .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(helper.absolutePos(new BlockPos(2, 2, 2))))
                    .create(LootContextParamSets.CHEST);
            List<ItemStack> items = table.getRandomItems(params, 1000L + i);
            boolean lens = items.stream().anyMatch(s -> s.is(com.relicweave.vestige.registry.VestigeMaterials.RIFTGLASS_LENS.get()));
            helper.assertTrue(lens, "Roll " + i + " produced no riftglass lens: " + items);
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 1200)
    public static void part1StructuresCanBeLocated(GameTestHelper helper) {
        ServerLevel vestige = vestige(helper);
        var registry = vestige.registryAccess().registryOrThrow(Registries.STRUCTURE);
        for (String name : NAMES) {
            ResourceKey<Structure> key = ResourceKey.create(Registries.STRUCTURE, RelicWeave.id(name));
            Holder<Structure> holder = registry.getHolderOrThrow(key);
            var found = vestige.getChunkSource().getGenerator()
                    .findNearestMapStructure(vestige, HolderSet.direct(holder), BlockPos.ZERO, 100, false);
            helper.assertTrue(found != null, name + " could not be located in The Vestige within 100 chunks");
        }
        helper.succeed();
    }
}
