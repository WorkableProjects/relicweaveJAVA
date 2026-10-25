package com.relicweave.client;

import com.relicweave.RelicWeave;
import com.relicweave.block.infuser.CelestialInfuserMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class CelestialInfuserScreen extends AbstractContainerScreen<CelestialInfuserMenu> {
    private static final ResourceLocation TEXTURE = RelicWeave.id("textures/gui/container/celestial_infuser.png");
    private static final int ARROW_X = 79;
    private static final int ARROW_Y = 34;
    private static final int ARROW_U = 176;
    private static final int ARROW_V = 0;
    private static final int ARROW_WIDTH = 24;
    private static final int ARROW_HEIGHT = 17;

    private static final int STATUS_Y = 72;
    private static final int STATUS_MAX_WIDTH = 160;
    private static final int OUTPUT_X = 115;
    private static final int OUTPUT_Y = 34;
    private static final int FLASH_TICKS = 10;

    private int lastCompletions = -1;
    private int flashTicks;

    public CelestialInfuserScreen(CelestialInfuserMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected void init() {
        super.init();
        titleLabelX = (imageWidth - font.width(title)) / 2;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        int filled = menu.getScaledProgress(ARROW_WIDTH);
        if (filled > 0) {
            graphics.blit(TEXTURE, leftPos + ARROW_X, topPos + ARROW_Y, ARROW_U, ARROW_V, filled, ARROW_HEIGHT);
        }
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        int completions = menu.getCompletions();
        if (lastCompletions >= 0 && completions != lastCompletions) {
            flashTicks = FLASH_TICKS;
        } else if (flashTicks > 0) {
            flashTicks--;
        }
        lastCompletions = completions;
    }

    /**
     * The status line shares y = 72 with the vanilla "Inventory" label (the gap between the machine slots and the
     * player inventory is only that one row), so the inventory label is skipped while the two would overlap.
     */
    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, 0x404040, false);
        Component status = menu.getStatus().message();
        int width = status == null ? 0 : font.width(status);
        float scale = width > STATUS_MAX_WIDTH ? (float) STATUS_MAX_WIDTH / width : 1.0F;
        int scaledWidth = (int) Math.ceil(width * scale);
        int statusLeft = (imageWidth - scaledWidth) / 2;
        boolean overlaps = status != null
                && statusLeft < inventoryLabelX + font.width(playerInventoryTitle) + 4;
        if (!overlaps) {
            graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0x404040, false);
        }
        if (status == null) {
            return;
        }
        graphics.pose().pushPose();
        graphics.pose().translate(statusLeft, STATUS_Y, 0);
        graphics.pose().scale(scale, scale, 1.0F);
        graphics.drawString(font, status, 0, 0, 0xFFFFFF, false);
        graphics.pose().popPose();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        if (flashTicks > 0) {
            float fade = Math.max(0.0F, (flashTicks - partialTick) / FLASH_TICKS);
            int alpha = (int) (0x60 * Math.min(1.0F, fade));
            graphics.fill(leftPos + OUTPUT_X, topPos + OUTPUT_Y, leftPos + OUTPUT_X + 18, topPos + OUTPUT_Y + 18,
                    (alpha << 24) | 0xFFFFFF);
        }
        renderTooltip(graphics, mouseX, mouseY);
    }
}
