package com.relicweave.vestige.client.hazard;

import com.mojang.blaze3d.systems.RenderSystem;
import com.relicweave.RelicWeave;
import com.relicweave.config.RelicWeaveClientConfig;
import com.relicweave.vestige.VestigeKeys;
import com.relicweave.vestige.client.VestigeClientState;
import com.relicweave.vestige.network.VestigeNetwork.HazardSyncPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

/**
 * Compact vertical Rift Saturation meter right of the hotbar, flag icons above it, and a faint violet vignette when
 * the meter is past 50. Purely cosmetic and driven by {@link VestigeClientState}.
 */
public final class HazardHud {
    public static final IGuiOverlay OVERLAY = HazardHud::render;
    private static final ResourceLocation ICONS = RelicWeave.id("textures/gui/vestige/hazard_icons.png");

    public static final int METER_WIDTH = 5;
    public static final int METER_HEIGHT = 40;

    /** Icon column order in {@code hazard_icons.png}; flags are {@link HazardSyncPacket} bits. */
    private static final int[] FLAGS = {
            HazardSyncPacket.IN_NULL_POCKET, HazardSyncPacket.GRAVITY_TELEGRAPH,
            HazardSyncPacket.IN_HUSHFIELD, HazardSyncPacket.RIFT_SICKNESS};
    private static final int[] FLAG_ICON = {1, 2, 3, 4};
    private static final String[] FLAG_KEY = {"in_null_pocket", "gravity_telegraph", "in_hushfield", "rift_sickness"};

    private static int previousExposure;
    private static long risingUntil;

    private HazardHud() {}

    /** Meter colour for {@code exposure} 0..100: teal, then gold at 50, then violet. */
    public static int meterColor(float exposure) {
        int teal = 0x3A8C86;
        int gold = 0xE2BE5C;
        int violet = 0xA268CC;
        return exposure < 50.0F
                ? lerp(teal, gold, exposure / 50.0F)
                : lerp(gold, violet, (exposure - 50.0F) / 50.0F);
    }

    /** Vignette alpha for {@code exposure}: 0 up to 50, then up to 0.25 * intensity at 100. */
    public static float vignetteAlpha(float exposure, double intensity) {
        return exposure <= 50.0F ? 0.0F : (float) ((exposure - 50.0F) / 50.0F * 0.25 * intensity);
    }

    private static int lerp(int a, int b, float t) {
        t = Mth.clamp(t, 0.0F, 1.0F);
        int r = (int) Mth.lerp(t, (a >> 16) & 0xFF, (b >> 16) & 0xFF);
        int g = (int) Mth.lerp(t, (a >> 8) & 0xFF, (b >> 8) & 0xFF);
        int bl = (int) Mth.lerp(t, a & 0xFF, b & 0xFF);
        return (r << 16) | (g << 8) | bl;
    }

    private static void render(ForgeGui gui, GuiGraphics graphics, float partialTick, int width, int height) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null || minecraft.options.hideGui
                || minecraft.level.dimension() != VestigeKeys.THE_VESTIGE) {
            previousExposure = 0;
            return;
        }
        int exposure = VestigeClientState.riftExposure;
        int flags = VestigeClientState.hazardFlags;
        long now = minecraft.level.getGameTime();
        if (exposure > previousExposure) {
            risingUntil = now + 40;
        }
        previousExposure = exposure;
        boolean minimal = RelicWeaveClientConfig.HAZARD_HUD.get() == RelicWeaveClientConfig.HazardHud.MINIMAL;

        float alpha = vignetteAlpha(exposure, RelicWeaveClientConfig.DISTORTION_INTENSITY.get());
        if (alpha > 0.0F) {
            vignette(graphics, width, height, alpha);
        }

        Font font = minecraft.font;
        int x = width / 2 + 96;
        int top = height - 44;
        boolean showMeter = exposure > 0 && (!minimal || now < risingUntil || exposure >= 50);
        if (showMeter) {
            if (minimal) {
                graphics.blit(ICONS, x - 2, top + METER_HEIGHT - 9, 0, 0, 9, 9, 45, 9);
            } else {
                meter(graphics, x, top, exposure);
                if (VestigeClientState.riftProtectionPercent > 0) {
                    graphics.pose().pushPose();
                    graphics.pose().translate(x + METER_WIDTH + 3, top + METER_HEIGHT - 8, 0);
                    graphics.pose().scale(0.7F, 0.7F, 1.0F);
                    graphics.drawString(font, "-" + VestigeClientState.riftProtectionPercent + "%", 0, 0, 0x96D6C8, true);
                    graphics.pose().popPose();
                }
            }
        }

        // Flag icons stack upward from just above the meter so they never run off the bottom of the screen.
        int row = 0;
        int baseY = (showMeter && !minimal ? top : top + METER_HEIGHT) - 11;
        for (int i = 0; i < FLAGS.length; i++) {
            if ((flags & FLAGS[i]) == 0) {
                continue;
            }
            int y = baseY - row * 10;
            graphics.blit(ICONS, x - 2, y, FLAG_ICON[i] * 9, 0, 9, 9, 45, 9);
            if (!minimal) {
                graphics.drawString(font, Component.translatable("hud.relicweave.hazard." + FLAG_KEY[i]), x + 9, y + 1, 0xE0E0E0, true);
            }
            row++;
        }
    }

    private static void meter(GuiGraphics graphics, int x, int top, int exposure) {
        graphics.fill(x - 1, top - 1, x + METER_WIDTH + 1, top + METER_HEIGHT + 1, 0xA0000000);
        graphics.fill(x, top, x + METER_WIDTH, top + METER_HEIGHT, 0xC0182030);
        int filled = Math.round(METER_HEIGHT * Mth.clamp(exposure, 0, 100) / 100.0F);
        graphics.fill(x, top + METER_HEIGHT - filled, x + METER_WIDTH, top + METER_HEIGHT, 0xFF000000 | meterColor(exposure));
        for (int mark : new int[] {50, 80}) {
            int y = top + METER_HEIGHT - Math.round(METER_HEIGHT * mark / 100.0F);
            graphics.fill(x - 2, y, x + METER_WIDTH + 2, y + 1, 0xFFE8E8E8);
        }
    }

    private static void vignette(GuiGraphics graphics, int width, int height, float alpha) {
        RenderSystem.enableBlend();
        int layers = 12;
        int step = 4;
        for (int i = 0; i < layers; i++) {
            int a = (int) (255 * alpha * (1.0F - i / (float) layers));
            if (a <= 0) {
                continue;
            }
            int color = (a << 24) | 0x5A2896;
            int o = i * step;
            graphics.fill(o, o, width - o, o + step, color);
            graphics.fill(o, height - o - step, width - o, height - o, color);
            graphics.fill(o, o + step, o + step, height - o - step, color);
            graphics.fill(width - o - step, o + step, width - o, height - o - step, color);
        }
    }
}
