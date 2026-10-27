package com.relicweave.vestige.world.feature;

import com.relicweave.RelicWeave;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.BlockStateConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Custom worldgen features that give The Vestige its ruined, monumental silhouette (floating ruins, arches, spires...). */
public final class VestigeFeatures {
    public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(ForgeRegistries.FEATURES, RelicWeave.MOD_ID);

    public static final RegistryObject<FloatingRuinFeature> FLOATING_RUIN =
            FEATURES.register("floating_ruin", () -> new FloatingRuinFeature(NoneFeatureConfiguration.CODEC));
    public static final RegistryObject<BrokenArchFeature> BROKEN_ARCH =
            FEATURES.register("broken_arch", () -> new BrokenArchFeature(NoneFeatureConfiguration.CODEC));
    public static final RegistryObject<CrystalSpireFeature> CRYSTAL_SPIRE =
            FEATURES.register("crystal_spire", () -> new CrystalSpireFeature(BlockStateConfiguration.CODEC));
    public static final RegistryObject<FallenStatueFeature> FALLEN_STATUE =
            FEATURES.register("fallen_statue", () -> new FallenStatueFeature(NoneFeatureConfiguration.CODEC));
    public static final RegistryObject<RuinedColonnadeFeature> RUINED_COLONNADE =
            FEATURES.register("ruined_colonnade", () -> new RuinedColonnadeFeature(BlockStateConfiguration.CODEC));
    public static final RegistryObject<RootArchFeature> ROOT_ARCH =
            FEATURES.register("root_arch", () -> new RootArchFeature(NoneFeatureConfiguration.CODEC));

    public static final RegistryObject<MirrorPoolFeature> MIRROR_POOL =
            FEATURES.register("mirror_pool", () -> new MirrorPoolFeature(NoneFeatureConfiguration.CODEC));
    public static final RegistryObject<ShatteredPlateFeature> SHATTERED_PLATE =
            FEATURES.register("shattered_plate", () -> new ShatteredPlateFeature(NoneFeatureConfiguration.CODEC));

    private VestigeFeatures() {}
}
