package com.relicweave.vestige.world.feature;

import com.relicweave.RelicWeave;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

/** Custom worldgen features that give The Vestige its ruined, monumental silhouette (floating ruins, arches, spires...). */
public final class VestigeFeatures {
    public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(ForgeRegistries.FEATURES, RelicWeave.MOD_ID);

    private VestigeFeatures() {}
}
