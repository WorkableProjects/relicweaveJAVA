package com.relicweave.client;

import com.relicweave.block.infuser.CelestialInfuserMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

// STUB (wave 0): replaced by task T05.
public class CelestialInfuserScreen extends AbstractContainerScreen<CelestialInfuserMenu> {
    public CelestialInfuserScreen(CelestialInfuserMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {}
}
