package com.relicweave.vestige.client.world;

import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Sky and fog presentation of The Vestige: overworld-like sky with a high cloud layer, and a fog colour that drifts
 * toward a faint teal when light is low, so silhouettes stay readable at dusk.
 */
public final class VestigeDimensionEffects extends DimensionSpecialEffects {
    private static final Vec3 TEAL = new Vec3(0.10, 0.27, 0.29);

    public VestigeDimensionEffects() {
        super(230.0F, true, SkyType.NORMAL, false, false);
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
}
