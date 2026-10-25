package com.relicweave.compat.jei;

import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.drawable.IDrawableAnimated;
import mezz.jei.api.gui.drawable.IDrawableStatic;
import mezz.jei.api.helpers.IGuiHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Shared layout for both machines, mirroring their GUIs: top-left input, bottom-left material, arrow, output.
 */
abstract class MachineCategoryBase {
    static final int WIDTH = 140;
    static final int SLOT_X = 1;
    static final int TOP_SLOT_Y = 1;
    static final int BOTTOM_SLOT_Y = 37;
    static final int ARROW_X = 26;
    static final int ARROW_Y = 19;
    static final int OUTPUT_X = 61;
    static final int OUTPUT_Y = 19;
    static final int TEXT_COLOR = 0xFF404040;

    private final IGuiHelper guiHelper;
    private final IDrawableStatic emptyArrow;
    private final IDrawable icon;
    private final Map<Integer, IDrawableAnimated> arrows = new HashMap<>();

    MachineCategoryBase(IGuiHelper guiHelper, IDrawable icon) {
        this.guiHelper = guiHelper;
        this.emptyArrow = guiHelper.getRecipeArrow();
        this.icon = icon;
    }

    IGuiHelper guiHelper() {
        return guiHelper;
    }

    public IDrawable getIcon() {
        return icon;
    }

    IDrawableStatic slot() {
        return guiHelper.getSlotDrawable();
    }

    /** Draws the arrow, animated over the recipe's real processing time. */
    void drawArrow(GuiGraphics graphics, int processTicks) {
        emptyArrow.draw(graphics, ARROW_X, ARROW_Y);
        arrows.computeIfAbsent(Math.max(1, processTicks), guiHelper::createAnimatedRecipeArrow).draw(graphics, ARROW_X, ARROW_Y);
    }

    static void drawText(GuiGraphics graphics, Component text, int x, int y) {
        graphics.drawString(Minecraft.getInstance().font, text, x, y, TEXT_COLOR, false);
    }

    /** Draws {@code text} word-wrapped to the category width; returns the y below the last line. */
    static int drawWrapped(GuiGraphics graphics, Component text, int x, int y) {
        var font = Minecraft.getInstance().font;
        for (var line : font.split(text, WIDTH - x)) {
            graphics.drawString(font, line, x, y, TEXT_COLOR, false);
            y += font.lineHeight + 1;
        }
        return y;
    }

    static Component seconds(int ticks) {
        return Component.translatable("jei.relicweave.process_time", String.format(java.util.Locale.ROOT, "%.1f", ticks / 20.0));
    }
}
