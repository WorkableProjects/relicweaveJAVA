package com.relicweave.compat.jei;

import com.relicweave.RelicWeave;
import com.relicweave.attunement.AttunementMapping;
import com.relicweave.block.infuser.InfusionRecipe;
import com.relicweave.infusion.InfusionType;
import com.relicweave.client.CelestialInfuserScreen;
import com.relicweave.client.TransmutationTableScreen;
import com.relicweave.recipe.TransmutationRecipe;
import com.relicweave.registry.ModBlocks;
import com.relicweave.registry.ModItems;
import com.relicweave.registry.ModRecipes;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Optional Just Enough Items integration: shows every Transmutation Table and Celestial Infuser recipe, plus info
 * pages for the machines and Celestial materials. Loaded by JEI only when it is installed.
 */
@JeiPlugin
public class RelicWeaveJeiPlugin implements IModPlugin {
    public static final RecipeType<TransmutationRecipe> TRANSMUTATION =
            RecipeType.create(RelicWeave.MOD_ID, "transmutation", TransmutationRecipe.class);
    public static final RecipeType<InfusionRecipe> INFUSION =
            RecipeType.create(RelicWeave.MOD_ID, "infusion", InfusionRecipe.class);

    @Override
    public ResourceLocation getPluginUid() {
        return RelicWeave.id("jei_plugin");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        IGuiHelper guiHelper = registration.getJeiHelpers().getGuiHelper();
        registration.addRecipeCategories(new TransmutationCategory(guiHelper), new InfusionCategory(guiHelper));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        if (Minecraft.getInstance().level == null) {
            return;
        }
        RecipeManager recipes = Minecraft.getInstance().level.getRecipeManager();
        registration.addRecipes(TRANSMUTATION, recipes.getAllRecipesFor(ModRecipes.TRANSMUTATION_TYPE.get()));
        registration.addRecipes(INFUSION, recipes.getAllRecipesFor(ModRecipes.INFUSION_TYPE.get()));

        registration.addIngredientInfo(ModBlocks.TRANSMUTATION_TABLE_ITEM.get(), Component.translatable("jei.relicweave.info.transmutation_table"));
        registration.addIngredientInfo(ModBlocks.CELESTIAL_INFUSER_ITEM.get(), Component.translatable("jei.relicweave.info.celestial_infuser"));
        registration.addIngredientInfo(ModBlocks.CELESTIAL_ORE_ITEM.get(), Component.translatable("jei.relicweave.info.celestial_ore"));
        registration.addIngredientInfo(ModBlocks.NETHER_CELESTIAL_ORE_ITEM.get(), Component.translatable("jei.relicweave.info.celestial_ore"));
        registration.addIngredientInfo(ModItems.CELESTIAL_SHARD.get(), Component.translatable("jei.relicweave.info.celestial_ore"));
        for (AttunementMapping row : AttunementMapping.values()) {
            List<ItemStack> sources = new ArrayList<>();
            for (Holder<Item> holder : BuiltInRegistries.ITEM.getTagOrEmpty(row.sourceTag())) {
                sources.add(new ItemStack(holder.value()));
            }
            if (sources.isEmpty()) {
                continue;
            }
            Component enchantment = Component.translatable(row.enchantment().getDescriptionId());
            Component piece = new ItemStack(pieceFor(row.infusion())).getHoverName();
            registration.addItemStackInfo(sources, Component.translatable("jei.relicweave.info.attunement_source",
                    enchantment, row.infusion().displayName(), piece));
        }
        for (var armor : new net.minecraft.world.item.Item[]{ModItems.CELESTIAL_HELMET.get(), ModItems.CELESTIAL_CHESTPLATE.get(),
                ModItems.CELESTIAL_LEGGINGS.get(), ModItems.CELESTIAL_BOOTS.get()}) {
            registration.addIngredientInfo(armor, Component.translatable("jei.relicweave.info.celestial_armor"));
        }

        // 0.0.4.5 toolkit: their crafting/smithing recipes show in the vanilla categories; these pages explain them.
        registration.addItemStackInfo(stacks(ModItems.IRON_MULTITOOL.get(), ModItems.DIAMOND_MULTITOOL.get(),
                ModItems.NETHERITE_MULTITOOL.get(), ModItems.CELESTIAL_MULTITOOL.get()),
                Component.translatable("jei.relicweave.info.multitool"));
        registration.addItemStackInfo(ModBlocks.SUPER_FURNACES.stream().map(b -> new ItemStack(b.get())).toList(),
                Component.translatable("jei.relicweave.info.super_furnace"));
        registration.addItemStackInfo(stacks(ModItems.SCUBA_MASK.get(), ModItems.SCUBA_TANK.get(),
                ModItems.SCUBA_WETSUIT.get(), ModItems.SCUBA_FLIPPERS.get()),
                Component.translatable("jei.relicweave.info.scuba"));
    }

    private static List<ItemStack> stacks(Item... items) {
        List<ItemStack> list = new ArrayList<>();
        for (Item item : items) {
            list.add(new ItemStack(item));
        }
        return list;
    }

    private static Item pieceFor(InfusionType infusion) {
        return switch (infusion.slot()) {
            case HEAD -> ModItems.CELESTIAL_HELMET.get();
            case CHEST -> ModItems.CELESTIAL_CHESTPLATE.get();
            case LEGS -> ModItems.CELESTIAL_LEGGINGS.get();
            default -> ModItems.CELESTIAL_BOOTS.get();
        };
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(ModBlocks.TRANSMUTATION_TABLE_ITEM.get(), TRANSMUTATION);
        registration.addRecipeCatalyst(ModBlocks.CELESTIAL_INFUSER_ITEM.get(), INFUSION);
        // Super Furnaces smelt every Furnace recipe and burn every Furnace fuel.
        for (var furnace : ModBlocks.SUPER_FURNACES) {
            registration.addRecipeCatalyst(new ItemStack(furnace.get()), RecipeTypes.SMELTING, RecipeTypes.FUELING);
        }
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        // Clicking the progress arrow in either machine's GUI opens its JEI recipes.
        registration.addRecipeClickArea(TransmutationTableScreen.class, 79, 34, 24, 17, TRANSMUTATION);
        registration.addRecipeClickArea(CelestialInfuserScreen.class, 79, 34, 24, 17, INFUSION);
    }
}
