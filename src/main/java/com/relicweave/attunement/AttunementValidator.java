package com.relicweave.attunement;

import com.mojang.logging.LogUtils;
import com.relicweave.RelicWeave;
import com.relicweave.block.infuser.InfusionRecipe;
import com.relicweave.recipe.TransmutationRecipe;
import com.relicweave.registry.ModRecipes;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Checks that every {@link AttunementMapping} row has both of its pathways loaded: a transmutation recipe producing the
 * row's enchantment and an infusion recipe producing the row's infusion, each accepting every item in the row's source
 * tag. Problems are logged on server start and after {@code /reload}; GameTests assert there are none.
 */
@Mod.EventBusSubscriber(modid = RelicWeave.MOD_ID)
public final class AttunementValidator {
    private static final Logger LOGGER = LogUtils.getLogger();

    private AttunementValidator() {}

    /** Returns one human-readable message per problem; empty when every row is complete. */
    public static List<String> validate(RecipeManager recipes) {
        List<String> problems = new ArrayList<>();
        List<TransmutationRecipe> transmutations = recipes.getAllRecipesFor(ModRecipes.TRANSMUTATION_TYPE.get());
        List<InfusionRecipe> infusions = recipes.getAllRecipesFor(ModRecipes.INFUSION_TYPE.get());

        for (AttunementMapping row : AttunementMapping.values()) {
            List<ItemStack> sources = new ArrayList<>();
            for (Holder<Item> holder : BuiltInRegistries.ITEM.getTagOrEmpty(row.sourceTag())) {
                sources.add(new ItemStack(holder.value()));
            }
            if (sources.isEmpty()) {
                problems.add(row.key() + ": source tag " + row.sourceTag().location() + " is empty");
                continue;
            }
            for (ItemStack source : sources) {
                boolean hasEnchantment = transmutations.stream().anyMatch(r ->
                        r.getEnchantmentId().equals(row.enchantmentId()) && r.getInput().test(source));
                boolean hasInfusion = infusions.stream().anyMatch(r ->
                        r.getInfusion() == row.infusion() && r.getIngredient().test(source));
                if (!hasEnchantment) {
                    problems.add(row.key() + ": no transmutation recipe turns " + source.getItem() + " into " + row.enchantmentId());
                }
                if (!hasInfusion) {
                    problems.add(row.key() + ": no infusion recipe turns " + source.getItem() + " into " + row.infusion().id());
                }
            }
        }
        return problems;
    }

    private static void logProblems(RecipeManager recipes) {
        for (String problem : validate(recipes)) {
            LOGGER.error("RelicWeave attunement mapping is one-sided: {}", problem);
        }
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        logProblems(event.getServer().getRecipeManager());
    }

    /** Fires after {@code /reload} (player == null) as well as on join; only validate once per reload. */
    @SubscribeEvent
    public static void onDatapackSync(OnDatapackSyncEvent event) {
        if (event.getPlayer() == null) {
            logProblems(event.getPlayerList().getServer().getRecipeManager());
        }
    }
}
