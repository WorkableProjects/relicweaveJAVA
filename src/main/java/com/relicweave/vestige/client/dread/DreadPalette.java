package com.relicweave.vestige.client.dread;

import net.minecraft.util.Mth;

/**
 * The numbers behind The Vestige's "dread" look (0.0.4.5.b): fog distances, light and fog darkening, and the colour
 * transform for the darkened HUD textures. Pure functions with no client classes, so GameTests can check them.
 * {@code dread} is the client config value, 0 (0.0.4 look) to 1 (full dread).
 */
public final class DreadPalette {
    /** Fog far plane at full dread; Convergence pulls it closer still. */
    public static final float FAR = 72.0F;
    public static final float FAR_CONVERGENCE = 52.0F;
    /** Fraction of daylight that is lost at full dread when no block light is present. */
    public static final float MAX_DIM = 0.55F;
    /** Fraction of the fog colour that is lost at full dread. */
    public static final float FOG_DARKEN = 0.8F;

    private DreadPalette() {}

    /** Fog far plane: between the old haze (92 % of render distance) and a close wall of fog. */
    public static float fogFar(float renderFar, double dread, boolean convergence) {
        float target = Math.min(renderFar, convergence ? FAR_CONVERGENCE : FAR);
        return Mth.lerp((float) dread, renderFar * 0.92F, target);
    }

    /** Fog near plane for a given far plane: fog starts almost at your feet at full dread. */
    public static float fogNear(float far, double dread) {
        return far * Mth.lerp((float) dread, 0.35F, 0.06F);
    }

    /** Multiplier for a fog colour channel. */
    public static float fogScale(double dread) {
        return 1.0F - FOG_DARKEN * (float) dread;
    }

    /**
     * Multiplier for one lightmap entry: daylight and ambient light are dimmed, block light (torches, lava, glowing
     * blocks) is not, so carrying a light matters.
     */
    public static float lightScale(int blockLight, double dread) {
        float block = Mth.clamp(blockLight, 0, 15) / 15.0F;
        return 1.0F - MAX_DIM * (float) dread * (1.0F - block);
    }

    /**
     * Darkened HUD pixel (ARGB): every channel dimmed, green most, so hearts turn blood red, the XP bar a murky
     * olive and grey frames a bruised violet-black. Alpha is kept.
     */
    public static int darkenArgb(int argb) {
        int a = argb >>> 24;
        int r = (argb >> 16) & 0xFF;
        int g = (argb >> 8) & 0xFF;
        int b = argb & 0xFF;
        r = Math.round(r * 0.66F);
        g = Math.round(g * 0.48F);
        b = Math.round(b * 0.58F);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    /** Black-to-purple gradient stops for hearts and the XP bar: {position, r, g, b}. */
    private static final float[][] VOID_PURPLE = {
            {0.00F, 6, 2, 10}, {0.40F, 52, 12, 88}, {0.75F, 140, 60, 210}, {1.00F, 214, 168, 255}};

    /**
     * Hearts and XP bar recolour (ARGB): the pixel's brightness picks a colour on a black-to-purple gradient, so
     * outlines stay black and the fill turns violet. {@code glow} 0..1 brightens it a little (the shimmer). Alpha kept.
     */
    public static int voidPurple(int argb, float glow) {
        int a = argb >>> 24;
        float r = ((argb >> 16) & 0xFF) / 255.0F;
        float g = ((argb >> 8) & 0xFF) / 255.0F;
        float b = (argb & 0xFF) / 255.0F;
        // Max channel rather than luminance, so saturated reds and greens still read as bright.
        float t = Math.max(r, Math.max(g, b));
        t = Mth.clamp(t * (0.85F + 0.3F * glow), 0.0F, 1.0F);
        for (int i = 1; i < VOID_PURPLE.length; i++) {
            float[] lo = VOID_PURPLE[i - 1];
            float[] hi = VOID_PURPLE[i];
            if (t <= hi[0]) {
                float f = (t - lo[0]) / (hi[0] - lo[0]);
                int rr = Math.round(Mth.lerp(f, lo[1], hi[1]));
                int gg = Math.round(Mth.lerp(f, lo[2], hi[2]));
                int bb = Math.round(Mth.lerp(f, lo[3], hi[3]));
                return (a << 24) | (rr << 16) | (gg << 8) | bb;
            }
        }
        return (a << 24) | 0xD6A8FF;
    }
}
