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
 * breathing black vignette and dark veins creeping in from the screen edges (bruise violet; crimson in Convergence).
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

        float vignette = Mth.clamp(0.62F * dread * breathe + 0.25F * beat, 0.0F, 0.95F);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, vignette);
        graphics.blit(VIGNETTE, 0, 0, width, height, 0.0F, 0.0F, 256, 256, 256, 256);

        float r = convergence ? 0.62F : 0.30F;
        float g = convergence ? 0.05F : 0.08F;
        float b = convergence ? 0.12F : 0.36F;
        r = Mth.lerp(beat, r, 0.75F);
        g = Mth.lerp(beat, g, 0.02F);
        b = Mth.lerp(beat, b, 0.04F);
        float veins = Mth.clamp((convergence ? 0.62F : 0.42F) * dread * breathe + 0.45F * beat, 0.0F, 0.95F);
        RenderSystem.setShaderColor(r, g, b, veins);
        graphics.blit(VEINS, 0, 0, width, height, 0.0F, 0.0F, 256, 256, 256, 256);

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
    }
}
