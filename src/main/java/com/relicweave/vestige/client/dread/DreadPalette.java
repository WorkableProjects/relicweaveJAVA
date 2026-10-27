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
}
