package com.relicweave.client;

import com.relicweave.relic.RelicType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import net.minecraftforge.registries.ForgeRegistries;

/** Selected-relic icon left of the hotbar, with the activate key and the cosmetic cooldown overlay. */
public final class RelicHud {
    public static final IGuiOverlay OVERLAY = RelicHud::render;

    private RelicHud() {}

    private static void render(ForgeGui gui, GuiGraphics graphics, float partialTick, int width, int height) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.options.hideGui || player.isSpectator()) {
            return;
        }
        RelicType type = RelicClient.validSelection(player);
        if (type == null) {
            return;
        }
        Item item = ForgeRegistries.ITEMS.getValue(type.itemId());
        if (item == null) {
            return;
        }
        int x = width / 2 - 91 - 26;
        int y = height - 20;
        graphics.renderItem(new ItemStack(item), x, y);

        float cooldown = player.getCooldowns().getCooldownPercent(item, partialTick);
        if (cooldown > 0.0F) {
            int top = y + Mth.floor(16.0F * (1.0F - cooldown));
            graphics.fill(RenderType.guiOverlay(), x, top, x + 16, y + 16, 0xA0000000);
        }

        graphics.pose().pushPose();
        graphics.pose().translate(x + 8.0F, y - 7.0F, 200.0F);
        graphics.pose().scale(0.6F, 0.6F, 1.0F);
        String key = ModKeyMappings.ACTIVATE_RELIC.getTranslatedKeyMessage().getString();
        graphics.drawString(minecraft.font, key, -minecraft.font.width(key) / 2, 0, 0xFFFFFF, true);
        graphics.pose().popPose();
    }
}
