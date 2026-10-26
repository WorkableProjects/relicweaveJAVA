package com.relicweave.compat.jei.vestige;

import com.relicweave.relic.RelicAugment;
import com.relicweave.vestige.gear.GearRegistry;
import com.relicweave.vestige.registry.VestigeMaterials;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** "Reliquary Loom" JEI category: one entry per {@link RelicAugment} (relic + filament + material -> augmented relic). */
public class ReliquaryLoomCategory implements IRecipeCategory<RelicAugment> {
    private static final int WIDTH = 140;
    private static final int TEXT_COLOR = 0xFF404040;

    private final IGuiHelper guiHelper;
    private final IDrawable icon;

    public ReliquaryLoomCategory(IGuiHelper guiHelper) {
        this.guiHelper = guiHelper;
        this.icon = guiHelper.createDrawableItemLike(GearRegistry.RELIQUARY_LOOM_ITEM.get());
    }

    @Override
    public RecipeType<RelicAugment> getRecipeType() {
        return VestigeJeiPlugin.LOOM;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("block.relicweave.reliquary_loom");
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public int getWidth() {
        return WIDTH;
    }

    @Override
    public int getHeight() {
        return 92;
    }

    /** The relic item, or air when that relic is not registered (the category then shows nothing for it). */
    static Item relicItem(RelicAugment augment) {
        return BuiltInRegistries.ITEM.get(augment.relic().itemId());
    }

    static ItemStack augmented(RelicAugment augment) {
        ItemStack stack = new ItemStack(relicItem(augment));
        augment.apply(stack);
        return stack;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RelicAugment augment, IFocusGroup focuses) {
        Item relic = relicItem(augment);
        builder.addSlot(RecipeIngredientRole.INPUT, 1, 1).setBackground(guiHelper.getSlotDrawable(), -1, -1)
                .addItemStack(new ItemStack(relic == Items.AIR ? Items.BARRIER : relic));
        builder.addSlot(RecipeIngredientRole.INPUT, 1, 21).setBackground(guiHelper.getSlotDrawable(), -1, -1)
                .addItemStack(new ItemStack(VestigeMaterials.MNEMONIC_FILAMENT.get(), com.relicweave.vestige.VestigeConfig.LOOM_AUGMENT_FILAMENT_COST.get()));
        builder.addSlot(RecipeIngredientRole.INPUT, 1, 41).setBackground(guiHelper.getSlotDrawable(), -1, -1)
                .addItemStack(new ItemStack(augment.material(), RelicAugment.MATERIAL_COST));
        builder.addSlot(RecipeIngredientRole.CATALYST, 61, 21).setBackground(guiHelper.getSlotDrawable(), -1, -1)
                .addItemStack(new ItemStack(GearRegistry.RELIQUARY_LOOM_ITEM.get()));
        builder.addSlot(RecipeIngredientRole.OUTPUT, 101, 21).setBackground(guiHelper.getSlotDrawable(), -1, -1)
                .addItemStack(relic == Items.AIR ? new ItemStack(Items.BARRIER) : augmented(augment));
    }

    @Override
    public void draw(RelicAugment augment, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
        guiHelper.getRecipeArrow().draw(graphics, 31, 21);
        var font = Minecraft.getInstance().font;
        int y = 62;
        graphics.drawString(font, augment.displayName(), 0, y, TEXT_COLOR, false);
        y += font.lineHeight + 1;
        for (var line : font.split(augment.description(), WIDTH)) {
            graphics.drawString(font, line, 0, y, TEXT_COLOR, false);
            y += font.lineHeight;
            if (y > getHeight() - font.lineHeight) {
                break;
            }
        }
    }
}
