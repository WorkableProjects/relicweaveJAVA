package com.relicweave.compat.jei;

import com.relicweave.recipe.TransmutationRecipe;
import com.relicweave.registry.ModBlocks;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.Arrays;
import java.util.List;

public class TransmutationCategory extends MachineCategoryBase implements IRecipeCategory<TransmutationRecipe> {
    public TransmutationCategory(IGuiHelper guiHelper) {
        super(guiHelper, guiHelper.createDrawableItemLike(ModBlocks.TRANSMUTATION_TABLE_ITEM.get()));
    }

    @Override
    public RecipeType<TransmutationRecipe> getRecipeType() {
        return RelicWeaveJeiPlugin.TRANSMUTATION;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("block.relicweave.transmutation_table");
    }

    @Override
    public int getWidth() {
        return WIDTH;
    }

    @Override
    public int getHeight() {
        return 54;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, TransmutationRecipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, SLOT_X, TOP_SLOT_Y)
                .setBackground(slot(), -1, -1)
                .addItemStacks(withCount(recipe.getInput(), recipe.getInputCount()));
        builder.addSlot(RecipeIngredientRole.CATALYST, SLOT_X, BOTTOM_SLOT_Y)
                .setBackground(slot(), -1, -1)
                .addItemStacks(withCount(recipe.getCatalyst(), recipe.getCatalystCount()));
        builder.addSlot(RecipeIngredientRole.OUTPUT, OUTPUT_X, OUTPUT_Y)
                .setBackground(slot(), -1, -1)
                .addItemStack(recipe.createResult());
    }

    @Override
    public void draw(TransmutationRecipe recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
        drawArrow(graphics, recipe.getProcessTime());
        drawText(graphics, seconds(recipe.getProcessTime()), 84, 23);
    }

    static List<ItemStack> withCount(Ingredient ingredient, int count) {
        return Arrays.stream(ingredient.getItems()).map(stack -> stack.copyWithCount(count)).toList();
    }
}
