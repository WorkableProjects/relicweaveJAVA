package com.relicweave.vestige.client.cycle;

import com.mojang.blaze3d.systems.RenderSystem;
import com.relicweave.RelicWeave;
import com.relicweave.config.RelicWeaveClientConfig;
import com.relicweave.vestige.VestigeKeys;
import com.relicweave.vestige.client.VestigeClientState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

import java.util.function.BooleanSupplier;

/**
 * Concordance Cycle readout in the top-right corner while in The Vestige: a 9x9 phase glyph and the phase name. The
 * countdown to the next phase is only shown when {@link #countdownSource} says so (the Cycle Dial relic, Phase 4).
 */
public final class CycleHud {
    public static final IGuiOverlay OVERLAY = CycleHud::render;
    private static final ResourceLocation GLYPHS = RelicWeave.id("textures/gui/vestige/cycle_glyphs.png");

    /** Set by the Cycle Dial relic: when it returns true the HUD also shows mm:ss to the next phase. */
    public static BooleanSupplier countdownSource = () -> false;

    private CycleHud() {}

    /** Whole seconds left until the next phase, from the last sync and the client clock. */
    public static long secondsRemaining(long gameTime) {
        long ticks = VestigeClientState.ticksRemaining - (gameTime - VestigeClientState.syncedAtGameTime);
        return Math.max(0, ticks) / 20;
    }

    private static void render(ForgeGui gui, GuiGraphics graphics, float partialTick, int width, int height) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null || minecraft.options.hideGui
                || minecraft.level.dimension() != VestigeKeys.THE_VESTIGE || !VestigeClientState.cycleEnabled) {
            return;
        }
        Font font = minecraft.font;
        long gameTime = minecraft.level.getGameTime();
        boolean warning = VestigeClientState.convergenceWarning;

        Component name = VestigeClientState.phase.displayName();
        if (countdownSource.getAsBoolean()) {
            long s = secondsRemaining(gameTime);
            name = Component.empty().append(name).append(Component.literal(String.format(" %d:%02d", s / 60, s % 60)));
        }
        int textWidth = font.width(name);
        int x = width - 4 - textWidth - 12;
        int y = 4;

        boolean dark = RelicWeaveClientConfig.DARK_HUD.get();
        if (dark) {
            // Dark plate with a thin bruise-violet rule, in keeping with the darkened HUD.
            int left = x - 4;
            int bottom = y + (warning ? 22 : 11);
            graphics.fill(left, y - 2, width - 1, bottom, 0xA8050208);
            graphics.fill(left, bottom, width - 1, bottom + 1, 0xC0401838);
        }
        float alpha = warning ? 0.55F + 0.45F * (float) Math.sin((gameTime + partialTick) * 0.4) : 1.0F;
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);
        graphics.blit(GLYPHS, x, y, VestigeClientState.phase.ordinal() * 9, 0, 9, 9, 27, 9);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        graphics.drawString(font, name, x + 12, y + 1, dark ? 0xC9B8B0 : 0xFFFFFF, true);

        if (warning) {
            Component line = Component.translatable("hud.relicweave.cycle.warning");
            graphics.drawString(font, line, width - 4 - font.width(line), y + 12, dark ? 0xD84050 : 0xE0A0FF, true);
        }
    }
}
