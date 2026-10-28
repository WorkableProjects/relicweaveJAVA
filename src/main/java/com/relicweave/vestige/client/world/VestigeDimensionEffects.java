package com.relicweave.vestige.client.world;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.relicweave.config.RelicWeaveClientConfig;
import com.relicweave.vestige.client.VestigeClientState;
import com.relicweave.vestige.client.dread.DreadPalette;
import com.relicweave.vestige.cycle.CyclePhase;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Sky, light and fog presentation of The Vestige. With the client {@code dread} setting (0.0.4.5.b) the sky is a
 * near-black dome fading into the fog at the horizon, with faint cold stars (red in Convergence), no clouds, and the
 * lightmap loses up to 55 % of daylight and ambient light while block light stays as bright as ever. At
 * {@code dread = 0} it is the brighter 0.0.4 overworld-like sky.
 */
public final class VestigeDimensionEffects extends DimensionSpecialEffects {
    private static final Vec3 TEAL = new Vec3(0.10, 0.27, 0.29);
    private static final float[] ZENITH = {0.012F, 0.010F, 0.022F};
    private static final int STARS = 360;
    private static final float[][] STAR_POSITIONS = stars();

    public VestigeDimensionEffects() {
        super(230.0F, true, SkyType.NORMAL, false, false);
    }

    private static double dread() {
        return RelicWeaveClientConfig.DREAD.get();
    }

    @Override
    public Vec3 getBrightnessDependentFogColor(Vec3 fogColor, float brightness) {
        float bright = Mth.clamp(brightness * 0.94F + 0.06F, 0.0F, 1.0F);
        Vec3 base = fogColor.multiply(bright, bright, bright);
        double teal = (1.0 - Mth.clamp(brightness, 0.0F, 1.0F)) * 0.45;
        return base.scale(1.0 - teal).add(TEAL.scale(teal));
    }

    @Override
    public boolean isFoggyAt(int x, int y) {
        return false;
    }

    @Override
    public void adjustLightmapColors(ClientLevel level, float partialTicks, float skyDarken, float blockLightRedFlicker,
                                     float skyLight, int pixelX, int pixelY, Vector3f colors) {
        double dread = dread();
        if (dread <= 0.0) {
            return;
        }
        float scale = DreadPalette.lightScale(pixelX, dread);
        // A cold cast on whatever is lit only by the sky.
        float cold = (float) dread * 0.08F * (1.0F - pixelX / 15.0F);
        colors.set(colors.x() * scale * (1.0F - cold), colors.y() * scale * (1.0F - cold * 0.6F), colors.z() * scale);
    }

    @Override
    public boolean renderClouds(ClientLevel level, int ticks, float partialTick, PoseStack poseStack, double camX, double camY,
                                double camZ, Matrix4f projectionMatrix) {
        return dread() >= 0.5;
    }

    @Override
    public boolean renderSky(ClientLevel level, int ticks, float partialTick, PoseStack poseStack, Camera camera,
                             Matrix4f projectionMatrix, boolean isFoggy, Runnable setupFog) {
        float dread = (float) dread();
        if (dread <= 0.0F) {
            return false;
        }
        Vec3 sky = level.getSkyColor(camera.getPosition(), partialTick);
        float[] fog = RenderSystem.getShaderFogColor();
        float[] zenith = {
                Mth.lerp(dread, (float) sky.x, ZENITH[0]),
                Mth.lerp(dread, (float) sky.y, ZENITH[1]),
                Mth.lerp(dread, (float) sky.z, ZENITH[2])};

        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        Matrix4f m = poseStack.last().pose();
        BufferBuilder buffer = Tesselator.getInstance().getBuilder();

        // Dome: latitude bands from straight down (-90) to straight up (+90). Below the horizon it is all fog colour.
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        int bands = 12;
        int segments = 32;
        float radius = 100.0F;
        for (int b = 0; b < bands; b++) {
            float lat0 = -Mth.HALF_PI + Mth.PI * b / bands;
            float lat1 = -Mth.HALF_PI + Mth.PI * (b + 1) / bands;
            float[] c0 = domeColor(lat0, fog, zenith);
            float[] c1 = domeColor(lat1, fog, zenith);
            for (int s = 0; s < segments; s++) {
                float lon0 = Mth.TWO_PI * s / segments;
                float lon1 = Mth.TWO_PI * (s + 1) / segments;
                vertex(buffer, m, radius, lat0, lon0, c0);
                vertex(buffer, m, radius, lat0, lon1, c0);
                vertex(buffer, m, radius, lat1, lon1, c1);
                vertex(buffer, m, radius, lat1, lon0, c1);
            }
        }
        BufferUploader.drawWithShader(buffer.end());

        // Faint stars, red while the Concordance converges.
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        boolean convergence = VestigeClientState.cycleEnabled && VestigeClientState.phase == CyclePhase.CONVERGENCE;
        float twinkle = 0.75F + 0.25F * Mth.sin((ticks + partialTick) * 0.05F);
        float alpha = 0.45F * dread * twinkle;
        float r = convergence ? 0.85F : 0.62F;
        float g = convergence ? 0.22F : 0.64F;
        float bl = convergence ? 0.25F : 0.78F;
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        for (float[] star : STAR_POSITIONS) {
            star(buffer, m, star, r, g, bl, alpha);
        }
        BufferUploader.drawWithShader(buffer.end());
        RenderSystem.disableBlend();
        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        return true;
    }

    /** Fog colour at and below the horizon, deepening to the zenith colour above it. */
    private static float[] domeColor(float latitude, float[] fog, float[] zenith) {
        float t = Mth.clamp(latitude / (Mth.HALF_PI * 0.55F), 0.0F, 1.0F);
        t = (float) Math.sqrt(t);
        return new float[]{Mth.lerp(t, fog[0], zenith[0]), Mth.lerp(t, fog[1], zenith[1]), Mth.lerp(t, fog[2], zenith[2])};
    }

    private static void vertex(BufferBuilder buffer, Matrix4f m, float radius, float lat, float lon, float[] c) {
        float cos = Mth.cos(lat);
        buffer.vertex(m, radius * cos * Mth.cos(lon), radius * Mth.sin(lat), radius * cos * Mth.sin(lon))
                .color(c[0], c[1], c[2], 1.0F).endVertex();
    }

    /** Star: {x, y, z, size} on the upper sky; drawn as a small camera-independent quad. */
    private static void star(BufferBuilder buffer, Matrix4f m, float[] s, float r, float g, float b, float alpha) {
        Vector3f dir = new Vector3f(s[0], s[1], s[2]).normalize(90.0F);
        Vector3f side = new Vector3f(dir).cross(0.0F, 1.0F, 0.0F);
        if (side.lengthSquared() < 1.0E-4F) {
            side.set(1.0F, 0.0F, 0.0F);
        }
        side.normalize(s[3]);
        Vector3f up = new Vector3f(side).cross(dir).normalize(s[3]);
        float a = alpha * s[4];
        buffer.vertex(m, dir.x - side.x - up.x, dir.y - side.y - up.y, dir.z - side.z - up.z).color(r, g, b, a).endVertex();
        buffer.vertex(m, dir.x + side.x - up.x, dir.y + side.y - up.y, dir.z + side.z - up.z).color(r, g, b, a).endVertex();
        buffer.vertex(m, dir.x + side.x + up.x, dir.y + side.y + up.y, dir.z + side.z + up.z).color(r, g, b, a).endVertex();
        buffer.vertex(m, dir.x - side.x + up.x, dir.y - side.y + up.y, dir.z - side.z + up.z).color(r, g, b, a).endVertex();
    }

    private static float[][] stars() {
        RandomSource random = RandomSource.create(4152L);
        float[][] stars = new float[STARS][];
        for (int i = 0; i < STARS; i++) {
            float x = random.nextFloat() * 2.0F - 1.0F;
            float y = random.nextFloat() * 0.9F + 0.1F;
            float z = random.nextFloat() * 2.0F - 1.0F;
            stars[i] = new float[]{x, y, z, 0.12F + random.nextFloat() * 0.14F, 0.4F + random.nextFloat() * 0.6F};
        }
        return stars;
    }
}
