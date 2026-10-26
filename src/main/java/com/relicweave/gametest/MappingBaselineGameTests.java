package com.relicweave.gametest;

import com.relicweave.RelicWeave;
import com.relicweave.attunement.AttunementMapping;
import com.relicweave.block.infuser.InfusionRecipe;
import com.relicweave.recipe.TransmutationRecipe;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

/**
 * Frozen 0.0.2/0.0.3 baseline. Every Transmutation Table and Celestial Infuser recipe in these tables, plus the six
 * 0.0.3 attunement rows, must keep its ID, inputs, counts and outputs. Later versions may add rows, so these tests
 * never assert totals. If a test fails, the recipe is not the problem to fix: never edit a recipe JSON to pass.
 */
@GameTestHolder(RelicWeave.MOD_ID)
@PrefixGameTestTemplate(false)
public final class MappingBaselineGameTests {
    private static final String TEMPLATE = "platform";

    /**
     * Transmutation rows. The input column is the item tested against the recipe. For {@code #tag} inputs the
     * 0.0.3 default source item is listed instead (the tag itself is not tested here).
     */
    private static final Transmutation[] TRANSMUTATIONS = {
            new Transmutation("relicweave:transmutation/cobbled_up_1", "minecraft:cobblestone", 32, "minecraft:amethyst_shard", 1, "relicweave:cobbled_up", 1),
            new Transmutation("relicweave:transmutation/conductive_1", "minecraft:redstone", 16, "minecraft:amethyst_shard", 1, "relicweave:conductive", 1),
            new Transmutation("relicweave:transmutation/golemed_1", "minecraft:iron_ingot", 8, "minecraft:amethyst_shard", 2, "relicweave:golemed", 1),
            new Transmutation("relicweave:transmutation/keen_edge_1", "minecraft:diamond", 1, "minecraft:amethyst_shard", 1, "relicweave:keen_edge", 1),
            new Transmutation("relicweave:transmutation/necromanced_1", "minecraft:wither_skeleton_skull", 1, "minecraft:soul_sand", 8, "relicweave:necromanced", 1),
            new Transmutation("relicweave:transmutation/oxidation_1", "minecraft:copper_ingot", 8, "minecraft:amethyst_shard", 1, "relicweave:oxidation", 1),
            new Transmutation("relicweave:transmutation/piglinified_1", "minecraft:gold_ingot", 8, "minecraft:amethyst_shard", 2, "relicweave:piglinified", 1),
            new Transmutation("relicweave:transmutation/purling_1", "minecraft:ender_pearl", 1, "minecraft:amethyst_shard", 1, "relicweave:purling", 1),
            // #relicweave:attunement_sources/reaping, default source
            new Transmutation("relicweave:transmutation/reaping_1", "minecraft:oak_sapling", 16, "minecraft:amethyst_shard", 1, "relicweave:reaping", 1),
            // #relicweave:attunement_sources/reprisal, default source
            new Transmutation("relicweave:transmutation/reprisal_1", "minecraft:copper_ingot", 8, "minecraft:lapis_lazuli", 4, "relicweave:reprisal", 1),
            // #relicweave:attunement_sources/resonance, default source
            new Transmutation("relicweave:transmutation/resonance_1", "minecraft:echo_shard", 2, "minecraft:amethyst_shard", 2, "relicweave:resonance", 1),
            // #relicweave:attunement_sources/soulguard, default source
            new Transmutation("relicweave:transmutation/soulguard_1", "minecraft:ghast_tear", 2, "minecraft:amethyst_shard", 2, "relicweave:soulguard", 1),
            // #relicweave:attunement_sources/steady_draw, default source
            new Transmutation("relicweave:transmutation/steady_draw_1", "minecraft:phantom_membrane", 4, "minecraft:amethyst_shard", 1, "relicweave:steady_draw", 1),
            new Transmutation("relicweave:transmutation/the_necromancer_1", "minecraft:netherite_ingot", 1, "minecraft:echo_shard", 1, "relicweave:the_necromancer", 1),
            // #relicweave:attunement_sources/undertow, default source
            new Transmutation("relicweave:transmutation/undertow_1", "minecraft:prismarine_shard", 8, "minecraft:amethyst_shard", 1, "relicweave:undertow", 1),
            new Transmutation("relicweave:transmutation/upbound_1", "minecraft:nether_star", 1, "minecraft:echo_shard", 2, "relicweave:upbound", 1),
            new Transmutation("relicweave:transmutation/villagers_best_friend_1", "minecraft:emerald", 8, "minecraft:amethyst_shard", 2, "relicweave:villagers_best_friend", 1),
    };

    /**
     * Celestial Infuser rows. The input column is the item tested against the recipe. For {@code #tag} inputs the
     * 0.0.3 default source item is listed instead.
     */
    private static final Infusion[] INFUSIONS = {
            // #relicweave:attunement_sources/reaping, default source
            new Infusion("relicweave:infusion/anchored_from_reaping_source", "minecraft:oak_sapling", 32, "relicweave:anchored"),
            // #relicweave:attunement_sources/reprisal, default source
            new Infusion("relicweave:infusion/anchored_from_reprisal_source", "minecraft:copper_ingot", 32, "relicweave:anchored"),
            new Infusion("relicweave:infusion/cobbled", "minecraft:cobblestone", 32, "relicweave:cobbled"),
            new Infusion("relicweave:infusion/conductive", "minecraft:redstone_block", 1, "relicweave:conductive"),
            new Infusion("relicweave:infusion/golemed", "minecraft:iron_block", 1, "relicweave:golemed"),
            new Infusion("relicweave:infusion/necromanced", "minecraft:wither_skeleton_skull", 1, "relicweave:necromanced"),
            new Infusion("relicweave:infusion/necromanced_soul_sand", "minecraft:soul_sand", 32, "relicweave:necromanced"),
            new Infusion("relicweave:infusion/necromancer", "minecraft:netherite_ingot", 1, "relicweave:necromancer"),
            new Infusion("relicweave:infusion/oxidized", "minecraft:copper_block", 1, "relicweave:oxidized"),
            new Infusion("relicweave:infusion/piglinified", "minecraft:gold_block", 1, "relicweave:piglinified"),
            // #relicweave:attunement_sources/steady_draw, default source
            new Infusion("relicweave:infusion/pursuit_from_steady_draw_source", "minecraft:phantom_membrane", 8, "relicweave:pursuit"),
            // #relicweave:attunement_sources/undertow, default source
            new Infusion("relicweave:infusion/tidal_from_undertow_source", "minecraft:prismarine_shard", 16, "relicweave:tidal"),
            new Infusion("relicweave:infusion/upbound", "minecraft:nether_star", 1, "relicweave:upbound"),
            new Infusion("relicweave:infusion/villagers_best_friend", "minecraft:emerald_block", 1, "relicweave:villagers_best_friend"),
            // #relicweave:attunement_sources/resonance, default source
            new Infusion("relicweave:infusion/warding_from_resonance_source", "minecraft:echo_shard", 4, "relicweave:warding"),
            // #relicweave:attunement_sources/soulguard, default source
            new Infusion("relicweave:infusion/warding_from_soulguard_source", "minecraft:ghast_tear", 4, "relicweave:warding"),
    };

    /** The six 0.0.3 attunement rows: row key, enchantment ID, and the infusion type path (TIDAL = "tidal", etc.). */
    private static final AttunementRow[] ATTUNEMENT_ROWS = {
            new AttunementRow("undertow", "relicweave:undertow", "tidal"),
            new AttunementRow("resonance", "relicweave:resonance", "warding"),
            new AttunementRow("steady_draw", "relicweave:steady_draw", "pursuit"),
            new AttunementRow("reprisal", "relicweave:reprisal", "anchored"),
            new AttunementRow("soulguard", "relicweave:soulguard", "warding"),
            new AttunementRow("reaping", "relicweave:reaping", "anchored"),
    };

    private MappingBaselineGameTests() {}

    @GameTest(template = TEMPLATE)
    public static void legacyTransmutationRecipesUnchanged(GameTestHelper helper) {
        RecipeManager recipes = helper.getLevel().getRecipeManager();
        List<String> problems = new ArrayList<>();
        for (Transmutation row : TRANSMUTATIONS) {
            problems.addAll(transmutationProblems(recipes, row));
        }
        helper.assertTrue(problems.isEmpty(), "Legacy transmutation recipes changed: " + problems);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void legacyInfusionRecipesUnchanged(GameTestHelper helper) {
        RecipeManager recipes = helper.getLevel().getRecipeManager();
        List<String> problems = new ArrayList<>();
        for (Infusion row : INFUSIONS) {
            problems.addAll(infusionProblems(recipes, row));
        }
        helper.assertTrue(problems.isEmpty(), "Legacy infusion recipes changed: " + problems);
        helper.succeed();
    }

    /** Protected legacy contract, deliberately repeated from the tables above. */
    @GameTest(template = TEMPLATE)
    public static void ironIngotAndNetherStarMappingsUnchanged(GameTestHelper helper) {
        RecipeManager recipes = helper.getLevel().getRecipeManager();
        List<String> problems = new ArrayList<>();
        problems.addAll(transmutationProblems(recipes, new Transmutation(
                "relicweave:transmutation/golemed_1", "minecraft:iron_ingot", 8, "minecraft:amethyst_shard", 2, "relicweave:golemed", 1)));
        problems.addAll(infusionProblems(recipes, new Infusion(
                "relicweave:infusion/golemed", "minecraft:iron_block", 1, "relicweave:golemed")));
        problems.addAll(transmutationProblems(recipes, new Transmutation(
                "relicweave:transmutation/upbound_1", "minecraft:nether_star", 1, "minecraft:echo_shard", 2, "relicweave:upbound", 1)));
        problems.addAll(infusionProblems(recipes, new Infusion(
                "relicweave:infusion/upbound", "minecraft:nether_star", 1, "relicweave:upbound")));
        helper.assertTrue(problems.isEmpty(), "Protected Iron Ingot / Nether Star mappings changed: " + problems);
        helper.succeed();
    }

    /** Each named row must still exist with its documented enchantment and infusion. Totals are not asserted. */
    @GameTest(template = TEMPLATE)
    public static void attunementRowsUnchanged(GameTestHelper helper) {
        List<String> problems = new ArrayList<>();
        for (AttunementRow row : ATTUNEMENT_ROWS) {
            AttunementMapping mapping = null;
            for (AttunementMapping candidate : AttunementMapping.values()) {
                if (candidate.key().equals(row.key())) {
                    mapping = candidate;
                    break;
                }
            }
            if (mapping == null) {
                problems.add(row.key() + ": no AttunementMapping row");
                continue;
            }
            if (!mapping.enchantmentId().toString().equals(row.enchantmentId())) {
                problems.add(row.key() + ": enchantment is " + mapping.enchantmentId() + ", expected " + row.enchantmentId());
            }
            if (!mapping.infusion().id().getPath().equals(row.infusionPath())) {
                problems.add(row.key() + ": infusion is " + mapping.infusion().id() + ", expected path " + row.infusionPath());
            }
        }
        helper.assertTrue(problems.isEmpty(), "Attunement rows changed: " + problems);
        helper.succeed();
    }

    private static List<String> transmutationProblems(RecipeManager recipes, Transmutation row) {
        List<String> problems = new ArrayList<>();
        Recipe<?> recipe = recipes.byKey(new ResourceLocation(row.id())).orElse(null);
        if (!(recipe instanceof TransmutationRecipe transmutation)) {
            problems.add(row.id() + ": missing or not a TransmutationRecipe");
            return problems;
        }
        if (!transmutation.getEnchantmentId().toString().equals(row.output())) {
            problems.add(row.id() + ": output is " + transmutation.getEnchantmentId() + ", expected " + row.output());
        }
        if (transmutation.getLevel() != row.level()) {
            problems.add(row.id() + ": level is " + transmutation.getLevel() + ", expected " + row.level());
        }
        if (transmutation.getInputCount() != row.inputCount()) {
            problems.add(row.id() + ": input count is " + transmutation.getInputCount() + ", expected " + row.inputCount());
        }
        if (transmutation.getCatalystCount() != row.catalystCount()) {
            problems.add(row.id() + ": catalyst count is " + transmutation.getCatalystCount() + ", expected " + row.catalystCount());
        }
        if (!transmutation.getInput().test(stackOf(row.input()))) {
            problems.add(row.id() + ": input does not accept " + row.input());
        }
        if (!transmutation.getCatalyst().test(stackOf(row.catalyst()))) {
            problems.add(row.id() + ": catalyst does not accept " + row.catalyst());
        }
        return problems;
    }

    private static List<String> infusionProblems(RecipeManager recipes, Infusion row) {
        List<String> problems = new ArrayList<>();
        Recipe<?> recipe = recipes.byKey(new ResourceLocation(row.id())).orElse(null);
        if (!(recipe instanceof InfusionRecipe infusion)) {
            problems.add(row.id() + ": missing or not an InfusionRecipe");
            return problems;
        }
        if (!infusion.getInfusion().id().toString().equals(row.output())) {
            problems.add(row.id() + ": output is " + infusion.getInfusion().id() + ", expected " + row.output());
        }
        if (infusion.getCount() != row.count()) {
            problems.add(row.id() + ": count is " + infusion.getCount() + ", expected " + row.count());
        }
        if (!infusion.getIngredient().test(stackOf(row.input()))) {
            problems.add(row.id() + ": ingredient does not accept " + row.input());
        }
        return problems;
    }

    private static ItemStack stackOf(String itemId) {
        return new ItemStack(ForgeRegistries.ITEMS.getValue(new ResourceLocation(itemId)));
    }

    /** One Transmutation Table row. {@code input} is the tested item (default source for {@code #tag} inputs). */
    private record Transmutation(String id, String input, int inputCount, String catalyst, int catalystCount, String output, int level) {}

    /** One Celestial Infuser row. {@code input} is the tested item (default source for {@code #tag} inputs). */
    private record Infusion(String id, String input, int count, String output) {}

    /** One 0.0.3 attunement row: {@code key} is {@link AttunementMapping#key()}. */
    private record AttunementRow(String key, String enchantmentId, String infusionPath) {}
}
