package com.relicweave.gametest;

import com.relicweave.RelicWeave;
import com.relicweave.attunement.AttunementMapping;
import com.relicweave.attunement.AttunementValidator;
import com.relicweave.block.infuser.InfusionRecipe;
import com.relicweave.recipe.TransmutationRecipe;
import com.relicweave.registry.ModRecipes;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraftforge.common.crafting.conditions.ICondition;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.Map;

/** The 0.0.3 source-item matrix: every row must have both an enchantment and an infusion pathway. */
@GameTestHolder(RelicWeave.MOD_ID)
@PrefixGameTestTemplate(false)
public final class AttunementGameTests {
    private static final String TEMPLATE = "platform";

    /** Default source item per row, as documented in docs/PLANS/0.0.3.md and the changelog. */
    private static final Map<AttunementMapping, ItemStack> DEFAULT_SOURCES = Map.of(
            AttunementMapping.UNDERTOW, new ItemStack(Items.PRISMARINE_SHARD),
            AttunementMapping.RESONANCE, new ItemStack(Items.ECHO_SHARD),
            AttunementMapping.STEADY_DRAW, new ItemStack(Items.PHANTOM_MEMBRANE),
            AttunementMapping.REPRISAL, new ItemStack(Items.COPPER_INGOT),
            AttunementMapping.SOULGUARD, new ItemStack(Items.GHAST_TEAR),
            AttunementMapping.REAPING, new ItemStack(Items.OAK_SAPLING));

    private AttunementGameTests() {}

    @GameTest(template = TEMPLATE)
    public static void everySourceItemHasBothPathways(GameTestHelper helper) {
        List<String> problems = AttunementValidator.validate(helper.getLevel().getRecipeManager());
        helper.assertTrue(problems.isEmpty(), "One-sided attunement mappings: " + problems);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void defaultSourceItemsMatchThePlan(GameTestHelper helper) {
        helper.assertTrue(DEFAULT_SOURCES.size() == AttunementMapping.values().length, "Every row needs a documented default source");
        for (AttunementMapping row : AttunementMapping.values()) {
            ItemStack source = DEFAULT_SOURCES.get(row);
            helper.assertTrue(source.is(row.sourceTag()), row.key() + " source tag should contain " + source.getItem());
        }
        helper.succeed();
    }

    /** The validator must actually catch a missing pathway, not just pass vacuously. */
    @GameTest(template = TEMPLATE)
    public static void validatorDetectsOneSidedMapping(GameTestHelper helper) {
        RecipeManager real = helper.getLevel().getRecipeManager();
        RecipeManager withoutInfusions = new RecipeManager(ICondition.IContext.EMPTY);
        List<TransmutationRecipe> transmutations = real.getAllRecipesFor(ModRecipes.TRANSMUTATION_TYPE.get());
        withoutInfusions.replaceRecipes(List.copyOf(transmutations));
        List<String> problems = AttunementValidator.validate(withoutInfusions);
        helper.assertTrue(problems.size() == AttunementMapping.values().length,
                "Expected one missing-infusion problem per row, got " + problems);
        helper.assertTrue(problems.stream().allMatch(p -> p.contains("infusion")), "Only infusions are missing: " + problems);

        List<InfusionRecipe> infusions = real.getAllRecipesFor(ModRecipes.INFUSION_TYPE.get());
        RecipeManager withoutEnchantments = new RecipeManager(ICondition.IContext.EMPTY);
        withoutEnchantments.replaceRecipes(List.copyOf(infusions));
        problems = AttunementValidator.validate(withoutEnchantments);
        helper.assertTrue(problems.size() == AttunementMapping.values().length && problems.stream().allMatch(p -> p.contains("transmutation")),
                "Expected one missing-transmutation problem per row, got " + problems);
        helper.succeed();
    }
}
