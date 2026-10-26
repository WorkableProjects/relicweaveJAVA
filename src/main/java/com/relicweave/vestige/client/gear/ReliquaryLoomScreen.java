package com.relicweave.vestige.client.gear;

import com.relicweave.RelicWeave;
import com.relicweave.relic.RelicAugment;
import com.relicweave.vestige.gear.ReliquaryLoomMenu;
import com.relicweave.vestige.registry.VestigeMaterials;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

/**
 * Reliquary Loom GUI: two path buttons (name, description, exact cost), a Remove augment button, the current augment and
 * the reversibility note. Buttons are greyed out with a tooltip reason using the menu's own validation; the server
 * re-checks everything when the button message arrives.
 */
public class ReliquaryLoomScreen extends AbstractContainerScreen<ReliquaryLoomMenu> {
    private static final ResourceLocation TEXTURE = RelicWeave.id("textures/gui/container/reliquary_loom.png");
    private static final int PANEL_X = 100;
    private static final int PANEL_W = 148;
    private static final int[] BUTTON_Y = {18, 70, 119};
    private static final float SMALL = 0.75F;

    private final Button[] buttons = new Button[3];

    public ReliquaryLoomScreen(ReliquaryLoomMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 256;
        imageHeight = 222;
        inventoryLabelX = ReliquaryLoomMenu.INVENTORY_X;
        inventoryLabelY = ReliquaryLoomMenu.INVENTORY_Y - 11;
        titleLabelX = 8;
        titleLabelY = 6;
    }

    @Override
    protected void init() {
        super.init();
        for (int id = 0; id < buttons.length; id++) {
            final int buttonId = id;
            buttons[id] = addRenderableWidget(Button.builder(Component.empty(), b -> press(buttonId))
                    .bounds(leftPos + PANEL_X, topPos + BUTTON_Y[id], PANEL_W, id == ReliquaryLoomMenu.BUTTON_REMOVE ? 14 : 16)
                    .build());
        }
        refreshButtons();
    }

    private void press(int id) {
        if (menu.unavailableReason(id) == null && minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
        }
    }

    private void refreshButtons() {
        for (int id = 0; id < buttons.length; id++) {
            Component reason = menu.unavailableReason(id);
            buttons[id].active = reason == null;
            buttons[id].setTooltip(reason == null ? null : Tooltip.create(reason));
            buttons[id].setMessage(buttonLabel(id));
        }
    }

    private Component buttonLabel(int id) {
        if (id == ReliquaryLoomMenu.BUTTON_REMOVE) {
            return Component.translatable("gui.relicweave.loom.remove");
        }
        RelicAugment augment = menu.augmentFor(id);
        String letter = id == ReliquaryLoomMenu.BUTTON_PATH_A ? "A" : "B";
        Component path = Component.translatable(id == ReliquaryLoomMenu.BUTTON_PATH_A ? "gui.relicweave.loom.path_a" : "gui.relicweave.loom.path_b");
        return augment == null ? Component.translatable("gui.relicweave.loom.path_empty", letter, path)
                : Component.translatable("gui.relicweave.loom.path", letter, augment.displayName());
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        refreshButtons();
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, 0x404040, false);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0x404040, false);
        graphics.drawString(font, Component.translatable("gui.relicweave.loom.slot_relic"), 32, ReliquaryLoomMenu.SLOT_Y[0] + 5, 0x404040, false);
        graphics.drawString(font, Component.translatable("gui.relicweave.loom.slot_filament"), 32, ReliquaryLoomMenu.SLOT_Y[1] + 5, 0x404040, false);
        graphics.drawString(font, Component.translatable("gui.relicweave.loom.slot_material"), 32, ReliquaryLoomMenu.SLOT_Y[2] + 5, 0x404040, false);

        // Left column: current augment and the reversibility note.
        int y = 94;
        y = small(graphics, Component.translatable("gui.relicweave.loom.current"), 8, y, 86, 0x404040);
        Component current = menu.currentAugment().map(a -> (Component) a.displayName())
                .orElse(Component.translatable("gui.relicweave.loom.current_none"));
        y = small(graphics, current, 8, y, 86, 0x7A3FB0);
        small(graphics, Component.translatable("gui.relicweave.loom.reversible"), 8, y + 4, 86, 0x555555);

        // Right panel: description and cost under each path button.
        for (int id = ReliquaryLoomMenu.BUTTON_PATH_A; id <= ReliquaryLoomMenu.BUTTON_PATH_B; id++) {
            RelicAugment augment = menu.augmentFor(id);
            int top = BUTTON_Y[id] + 18;
            if (augment == null) {
                small(graphics, Component.translatable("gui.relicweave.loom.insert_relic"), PANEL_X, top, PANEL_W, 0x555555);
                continue;
            }
            small(graphics, augment.description(), PANEL_X, top, PANEL_W, 0x404040);
            Component cost = Component.translatable("gui.relicweave.loom.cost", menu.augmentFilamentCost(),
                    VestigeMaterials.MNEMONIC_FILAMENT.get().getDescription(),
                    RelicAugment.MATERIAL_COST, augment.material().getDescription());
            small(graphics, cost, PANEL_X, BUTTON_Y[id] + 41, PANEL_W, 0x2F6B3A);
        }
        small(graphics, Component.translatable("gui.relicweave.loom.remove_cost", menu.resetFilamentCost(),
                VestigeMaterials.MNEMONIC_FILAMENT.get().getDescription()),
                PANEL_X, BUTTON_Y[2] + 15, PANEL_W, 0x2F6B3A);
    }

    /** Draws wrapped text at 75% scale; returns the y below it (unscaled GUI coordinates). */
    private int small(GuiGraphics graphics, Component text, int x, int y, int width, int color) {
        List<FormattedCharSequence> lines = font.split(text, (int) (width / SMALL));
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0);
        graphics.pose().scale(SMALL, SMALL, 1.0F);
        int lineY = 0;
        for (FormattedCharSequence line : lines) {
            graphics.drawString(font, line, 0, lineY, color, false);
            lineY += font.lineHeight;
        }
        graphics.pose().popPose();
        return y + (int) Math.ceil(lineY * SMALL) + 1;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
