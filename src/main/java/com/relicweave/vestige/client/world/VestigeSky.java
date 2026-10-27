package com.relicweave.vestige.client.world;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import com.relicweave.RelicWeave;
import com.relicweave.vestige.VestigeKeys;
import com.relicweave.vestige.client.VestigeClientState;
import com.relicweave.vestige.cycle.CyclePhase;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.material.FogType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

/**
 * The Vestige's sky and atmosphere: the "sky-spanning mechanism" of the Concordance — three vast, slowly turning rune
 * rings tinted by the current cycle phase (teal Quiet, gold Resonance, violet Convergence) — plus a soft distance haze
 * and a phase tint on the fog. Purely cosmetic; shader packs may replace sky and fog with their own.
 */
@Mod.EventBusSubscriber(modid = RelicWeave.MOD_ID, value = Dist.CLIENT)
public final class VestigeSky {
    /** Ring radius, height above the horizon, tilt (degrees), turn speed (degrees per tick), band width. */
    private static final float[][] RINGS = {
            {120.0F, 30.0F, 18.0F, 0.010F, 3.2F},
            {96.0F, 18.0F, -27.0F, -0.016F, 2.2F},
            {74.0F, 8.0F, 41.0F, 0.024F, 1.6F},
    };
    private static final int SEGMENTS = 128;

    private VestigeSky() {}

    private static boolean inVestige() {
        ClientLevel level = Minecraft.getInstance().level;
        return level != null && level.dimension() == VestigeKeys.THE_VESTIGE;
    }

    /** RGB of the current phase. */
    static float[] phaseColor() {
        CyclePhase phase = VestigeClientState.cycleEnabled ? VestigeClientState.phase : CyclePhase.QUIET;
        return switch (phase) {
            case QUIET -> new float[]{0.42F, 0.90F, 0.84F};
            case RESONANCE -> new float[]{1.00F, 0.80F, 0.38F};
            case CONVERGENCE -> new float[]{0.80F, 0.50F, 1.00F};
        };
    }

    @SubscribeEvent
    public static void onRenderStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SKY || !inVestige()) {
            return;
        }
        ClientLevel level = Minecraft.getInstance().level;
        float time = level.getGameTime() + event.getPartialTick();
        // Rings fade a little in full daylight so they read as something huge and distant, never as UI.
        float daylight = level.getSkyDarken(event.getPartialTick());
        float alphaScale = Mth.lerp(daylight, 0.55F, 0.32F);
        if (VestigeClientState.convergenceWarning) {
            alphaScale *= 0.85F + 0.15F * Mth.sin(time * 0.25F);
        }
        float[] rgb = phaseColor();

        PoseStack pose = event.getPoseStack();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(com.mojang.blaze3d.platform.GlStateManager.SourceFactor.SRC_ALPHA,
                com.mojang.blaze3d.platform.GlStateManager.DestFactor.ONE);
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        BufferBuilder buffer = Tesselator.getInstance().getBuilder();
        for (float[] ring : RINGS) {
            pose.pushPose();
            pose.mulPose(Axis.XP.rotationDegrees(ring[2]));
            pose.mulPose(Axis.YP.rotationDegrees(time * ring[3]));
            Matrix4f matrix = pose.last().pose();
            buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
            float radius = ring[0];
            float y = ring[1];
            float half = ring[4];
            for (int i = 0; i < SEGMENTS; i++) {
                float a0 = (float) (i * Math.PI * 2.0 / SEGMENTS);
                float a1 = (float) ((i + 1) * Math.PI * 2.0 / SEGMENTS);
                // Rune pattern: brighter dashes every few segments.
                float alpha = alphaScale * ((i % 8 < 3) ? 0.95F : 0.45F);
                quad(buffer, matrix, radius, y, half, a0, a1, rgb, alpha);
            }
            BufferUploader.drawWithShader(buffer.end());
            pose.popPose();
        }
        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    private static void quad(BufferBuilder buffer, Matrix4f m, float r, float y, float half, float a0, float a1,
                             float[] rgb, float alpha) {
        float c0 = Mth.cos(a0);
        float s0 = Mth.sin(a0);
        float c1 = Mth.cos(a1);
        float s1 = Mth.sin(a1);
        buffer.vertex(m, c0 * r, y - half, s0 * r).color(rgb[0], rgb[1], rgb[2], 0.0F).endVertex();
        buffer.vertex(m, c1 * r, y - half, s1 * r).color(rgb[0], rgb[1], rgb[2], 0.0F).endVertex();
        buffer.vertex(m, c1 * r, y, s1 * r).color(rgb[0], rgb[1], rgb[2], alpha).endVertex();
        buffer.vertex(m, c0 * r, y, s0 * r).color(rgb[0], rgb[1], rgb[2], alpha).endVertex();
        buffer.vertex(m, c0 * r, y, s0 * r).color(rgb[0], rgb[1], rgb[2], alpha).endVertex();
        buffer.vertex(m, c1 * r, y, s1 * r).color(rgb[0], rgb[1], rgb[2], alpha).endVertex();
        buffer.vertex(m, c1 * r, y + half, s1 * r).color(rgb[0], rgb[1], rgb[2], 0.0F).endVertex();
        buffer.vertex(m, c0 * r, y + half, s0 * r).color(rgb[0], rgb[1], rgb[2], 0.0F).endVertex();
    }

    /** Distance haze: terrain fog starts closer so far ruins fade into the biome's fog colour. */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onRenderFog(ViewportEvent.RenderFog event) {
        if (!inVestige() || event.getMode() != FogRenderer.FogMode.FOG_TERRAIN
                || event.getCamera().getFluidInCamera() != FogType.NONE) {
            return;
        }
        float far = event.getFarPlaneDistance();
        event.setNearPlaneDistance(far * 0.35F);
        event.setFarPlaneDistance(far * 0.92F);
        event.setCanceled(true);
    }

    /** Tints the fog slightly toward the cycle phase, strongest in Convergence. */
    @SubscribeEvent
    public static void onFogColor(ViewportEvent.ComputeFogColor event) {
        if (!inVestige() || event.getCamera().getFluidInCamera() != FogType.NONE) {
            return;
        }
        float[] rgb = phaseColor();
        float mix = VestigeClientState.phase == CyclePhase.CONVERGENCE ? 0.22F
                : VestigeClientState.phase == CyclePhase.RESONANCE ? 0.12F : 0.06F;
        event.setRed(Mth.lerp(mix, event.getRed(), rgb[0]));
        event.setGreen(Mth.lerp(mix, event.getGreen(), rgb[1]));
        event.setBlue(Mth.lerp(mix, event.getBlue(), rgb[2]));
    }
}
