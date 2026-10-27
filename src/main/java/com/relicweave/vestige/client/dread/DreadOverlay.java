package com.relicweave.vestige.client.dread;

import com.mojang.blaze3d.systems.RenderSystem;
import com.relicweave.RelicWeave;
import com.relicweave.config.RelicWeaveClientConfig;
import com.relicweave.vestige.VestigeKeys;
import com.relicweave.vestige.client.VestigeClientState;
import com.relicweave.vestige.cycle.CyclePhase;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

/**
 * The dread vignette, drawn under the rest of the HUD while in The Vestige with {@code darkHud} on: a slowly
 * breathing black vignette and two layers of purple veins that slowly creep in and out at the screen edges (crimson
 * in Convergence).
 * Below 40 % health the veins pulse blood red like a heartbeat.
 */
public final class DreadOverlay {
    public static final IGuiOverlay OVERLAY = DreadOverlay::render;
    private static final ResourceLocation VIGNETTE = RelicWeave.id("textures/gui/vestige/dread_vignette.png");
    private static final ResourceLocation VEINS = RelicWeave.id("textures/gui/vestige/dread_veins.png");

    private DreadOverlay() {}

    /** 0 above 40 % health, rising to 1 at 0 health. */
    public static float danger(float health, float maxHealth) {
        if (maxHealth <= 0.0F) {
            return 0.0F;
        }
        return Mth.clamp((0.4F - health / maxHealth) / 0.4F, 0.0F, 1.0F);
    }

    /** Heartbeat pulse 0..1: a sharp beat about once a second. */
    public static float heartbeat(float time) {
        float s = Math.max(0.0F, Mth.sin(time * 0.33F));
        return s * s * s * s * s * s;
    }

    private static void render(ForgeGui gui, GuiGraphics graphics, float partialTick, int width, int height) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.options.hideGui || mc.level.dimension() != VestigeKeys.THE_VESTIGE
                || !RelicWeaveClientConfig.DARK_HUD.get()) {
            return;
        }
        float time = mc.level.getGameTime() + partialTick;
        float dread = 0.5F + 0.5F * RelicWeaveClientConfig.DREAD.get().floatValue();
        float breathe = 0.85F + 0.15F * Mth.sin(time * 0.03F);
        float danger = danger(mc.player.getHealth(), mc.player.getMaxHealth());
        float beat = heartbeat(time) * danger;
        boolean convergence = VestigeClientState.cycleEnabled && VestigeClientState.phase == CyclePhase.CONVERGENCE;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask(false);

        float vignette = Mth.clamp(0.72F * dread * breathe + 0.25F * beat, 0.0F, 0.95F);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, vignette);
        graphics.blit(VIGNETTE, 0, 0, width, height, 0.0F, 0.0F, 256, 256, 256, 256);

        // Veins: bright bruise purple (crimson in Convergence, blood red on a heartbeat), two layers that swell and
        // shrink out of step so the tendrils seem to creep along the edges.
        float r = convergence ? 0.78F : 0.58F;
        float g = convergence ? 0.08F : 0.18F;
        float b = convergence ? 0.20F : 0.92F;
        r = Mth.lerp(beat, r, 0.85F);
        g = Mth.lerp(beat, g, 0.03F);
        b = Mth.lerp(beat, b, 0.06F);
        float base = (convergence ? 0.75F : 0.6F) * dread;
        float waveA = 0.5F + 0.5F * Mth.sin(time * 0.045F);
        float waveB = 0.5F + 0.5F * Mth.sin(time * 0.045F + Mth.PI);
        veins(graphics, width, height, 1.0F + 0.05F * waveA, false, r, g, b,
                Mth.clamp(base * (0.55F + 0.45F * waveA) + 0.45F * beat, 0.0F, 0.95F));
        veins(graphics, width, height, 1.0F + 0.05F * waveB, true, r, g, b,
                Mth.clamp(base * (0.35F + 0.45F * waveB) + 0.3F * beat, 0.0F, 0.95F));

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
    }

    /**
     * One veins layer, scaled about the screen centre by {@code zoom} (above 1 pulls the tendrils outward), optionally
     * turned 180 degrees so the second layer's tendrils sit in different places.
     */
    private static void veins(GuiGraphics graphics, int width, int height, float zoom, boolean turned,
                              float r, float g, float b, float alpha) {
        if (alpha <= 0.0F) {
            return;
        }
        var pose = graphics.pose();
        pose.pushPose();
        pose.translate(width / 2.0F, height / 2.0F, 0.0F);
        pose.scale(turned ? -zoom : zoom, turned ? -zoom : zoom, 1.0F);
        pose.translate(-width / 2.0F, -height / 2.0F, 0.0F);
        RenderSystem.setShaderColor(r, g, b, alpha);
        graphics.blit(VEINS, 0, 0, width, height, 0.0F, 0.0F, 256, 256, 256, 256);
        pose.popPose();
    }
}
