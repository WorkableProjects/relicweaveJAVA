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
 * The Vestige's sky and atmosphere: the "sky-spanning mechanism" of the Concordance — four vast concentric rune rings
 * hanging in the northern sky, each turning tinted by the current cycle phase (teal Quiet, gold Resonance, violet Convergence) — plus a soft distance haze
 * and a phase tint on the fog. Purely cosmetic; shader packs may replace sky and fog with their own.
 */
@Mod.EventBusSubscriber(modid = RelicWeave.MOD_ID, value = Dist.CLIENT)
public final class VestigeSky {
    /**
     * The mechanism hangs far to the north, high in the sky: concentric rings around one centre (sky-space, so it never
     * gets closer). Per ring: radius, band half-width, extra tilt (degrees), spin speed (degrees per tick).
     */
    private static final float[][] RINGS = {
            {62.0F, 2.6F, 0.0F, 0.020F},
            {48.0F, 1.8F, 9.0F, -0.032F},
            {35.0F, 1.4F, -7.0F, 0.045F},
            {22.0F, 1.0F, 4.0F, -0.060F},
    };
    /** Centre of the rings in sky space (x east, y up, -z north) and how far the plane leans back from facing you. */
    private static final float CENTER_Y = 48.0F;
    private static final float CENTER_Z = -95.0F;
    private static final float LEAN = 62.0F;
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
            pose.translate(0.0F, CENTER_Y, CENTER_Z);
            pose.mulPose(Axis.XP.rotationDegrees(LEAN + ring[2]));
            pose.mulPose(Axis.YP.rotationDegrees(time * ring[3]));
            Matrix4f matrix = pose.last().pose();
            buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
            for (int i = 0; i < SEGMENTS; i++) {
                float a0 = (float) (i * Math.PI * 2.0 / SEGMENTS);
                float a1 = (float) ((i + 1) * Math.PI * 2.0 / SEGMENTS);
                // Rune pattern: brighter dashes every few segments, with a gap so the ring visibly turns.
                int phase = i % 12;
                float alpha = phase == 11 ? 0.0F : alphaScale * (phase < 4 ? 1.0F : 0.5F);
                band(buffer, matrix, ring[0], ring[1], a0, a1, rgb, alpha);
            }
            BufferUploader.drawWithShader(buffer.end());
            pose.popPose();
        }
        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    /** One segment of a flat ring band in the XZ plane: bright along the radius, fading to the inner/outer edges. */
    private static void band(BufferBuilder buffer, Matrix4f m, float r, float half, float a0, float a1, float[] rgb, float alpha) {
        float c0 = Mth.cos(a0);
        float s0 = Mth.sin(a0);
        float c1 = Mth.cos(a1);
        float s1 = Mth.sin(a1);
        float in = r - half;
        float out = r + half;
        buffer.vertex(m, c0 * in, 0.0F, s0 * in).color(rgb[0], rgb[1], rgb[2], 0.0F).endVertex();
        buffer.vertex(m, c1 * in, 0.0F, s1 * in).color(rgb[0], rgb[1], rgb[2], 0.0F).endVertex();
        buffer.vertex(m, c1 * r, 0.0F, s1 * r).color(rgb[0], rgb[1], rgb[2], alpha).endVertex();
        buffer.vertex(m, c0 * r, 0.0F, s0 * r).color(rgb[0], rgb[1], rgb[2], alpha).endVertex();
        buffer.vertex(m, c0 * r, 0.0F, s0 * r).color(rgb[0], rgb[1], rgb[2], alpha).endVertex();
        buffer.vertex(m, c1 * r, 0.0F, s1 * r).color(rgb[0], rgb[1], rgb[2], alpha).endVertex();
        buffer.vertex(m, c1 * out, 0.0F, s1 * out).color(rgb[0], rgb[1], rgb[2], 0.0F).endVertex();
        buffer.vertex(m, c0 * out, 0.0F, s0 * out).color(rgb[0], rgb[1], rgb[2], 0.0F).endVertex();
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
