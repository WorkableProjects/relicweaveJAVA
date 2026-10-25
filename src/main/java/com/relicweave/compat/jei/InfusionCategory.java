package com.relicweave.compat.jei;

import com.relicweave.block.infuser.InfusionRecipe;
import com.relicweave.infusion.InfusionType;
import com.relicweave.infusion.Infusions;
import com.relicweave.registry.ModBlocks;
import com.relicweave.registry.ModItems;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.stream.Stream;

public class InfusionCategory extends MachineCategoryBase implements IRecipeCategory<InfusionRecipe> {
    public InfusionCategory(IGuiHelper guiHelper) {
        super(guiHelper, guiHelper.createDrawableItemLike(ModBlocks.CELESTIAL_INFUSER_ITEM.get()));
    }

    @Override
    public RecipeType<InfusionRecipe> getRecipeType() {
        return RelicWeaveJeiPlugin.INFUSION;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("block.relicweave.celestial_infuser");
    }

    @Override
    public int getWidth() {
        return WIDTH;
    }

    @Override
    public int getHeight() {
        return 124; // text starts at y 58: name, effect description (up to 3 wrapped lines), fits line, limit line
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, InfusionRecipe recipe, IFocusGroup focuses) {
        // Armor and results have equal-length lists, so JEI cycles them together (helmet -> infused helmet, ...).
        List<ItemStack> armor = celestialArmor(recipe.getInfusion());
        builder.addSlot(RecipeIngredientRole.INPUT, SLOT_X, TOP_SLOT_Y)
                .setBackground(slot(), -1, -1)
                .addItemStacks(armor);
        builder.addSlot(RecipeIngredientRole.INPUT, SLOT_X, BOTTOM_SLOT_Y)
                .setBackground(slot(), -1, -1)
                .addItemStacks(TransmutationCategory.withCount(recipe.getIngredient(), recipe.getCount()));
        builder.addSlot(RecipeIngredientRole.OUTPUT, OUTPUT_X, OUTPUT_Y)
                .setBackground(slot(), -1, -1)
                .addItemStacks(armor.stream().map(piece -> Infusions.withInfusion(piece, recipe.getInfusion())).toList());
    }

    @Override
    public void draw(InfusionRecipe recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
        drawArrow(graphics, recipe.getProcessTime());
        drawText(graphics, seconds(recipe.getProcessTime()), 84, 23);
        InfusionType infusion = recipe.getInfusion();
        int y = drawWrapped(graphics, infusion.displayName(), 0, 58);
        if (!infusion.hasAttribute()) {
            y = drawWrapped(graphics, infusion.description(), 0, y);
        }
        if (infusion.slot() != null) {
            List<ItemStack> pieces = celestialArmor(infusion);
            if (!pieces.isEmpty()) {
                y = drawWrapped(graphics, Component.translatable("jei.relicweave.infusion_piece", pieces.get(0).getHoverName()), 0, y);
            }
        }
        drawWrapped(graphics, Component.translatable("jei.relicweave.infusion_limit", Infusions.MAX_INFUSIONS), 0, y);
    }

    /** The Celestial armor pieces {@code infusion} fits: all four for attribute infusions, one for effect infusions. */
    private static List<ItemStack> celestialArmor(InfusionType infusion) {
        return Stream.of(ModItems.CELESTIAL_HELMET, ModItems.CELESTIAL_CHESTPLATE, ModItems.CELESTIAL_LEGGINGS, ModItems.CELESTIAL_BOOTS)
                .map(item -> new ItemStack(item.get()))
                .filter(piece -> infusion.fits(((ArmorItem) piece.getItem()).getEquipmentSlot()))
                .toList();
    }
}
