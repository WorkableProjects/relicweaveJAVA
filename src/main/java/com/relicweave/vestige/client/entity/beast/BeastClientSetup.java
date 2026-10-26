package com.relicweave.vestige.client.entity.beast;

import com.relicweave.RelicWeave;
import com.relicweave.vestige.entity.beast.BeastMobs;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Client-only registration of the beast mob models and renderers. */
@Mod.EventBusSubscriber(modid = RelicWeave.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class BeastClientSetup {
    private BeastClientSetup() {}

    @SubscribeEvent
    public static void onRegisterLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(BeastModels.STALKER, () -> VestigeQuadrupedModel.createLayer(BeastModels.STALKER_SPEC));
        event.registerLayerDefinition(BeastModels.HOUND, () -> VestigeQuadrupedModel.createLayer(BeastModels.HOUND_SPEC));
        event.registerLayerDefinition(BeastModels.GRAZER, () -> VestigeQuadrupedModel.createLayer(BeastModels.GRAZER_SPEC));
        event.registerLayerDefinition(BeastModels.BRUTE, () -> VestigeQuadrupedModel.createLayer(BeastModels.BRUTE_SPEC));
        event.registerLayerDefinition(BeastModels.STRIDER, () -> VestigeQuadrupedModel.createLayer(BeastModels.STRIDER_SPEC));
        event.registerLayerDefinition(BeastModels.LEECH, () -> VestigeQuadrupedModel.createLayer(BeastModels.LEECH_SPEC));
        event.registerLayerDefinition(BeastModels.MIMIC, ArchiveMimicModel::createLayer);
    }

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(BeastMobs.GLASSROOT_STALKER.get(), ctx -> new BeastRenderers.Quadruped<>(ctx,
                new VestigeQuadrupedModel<>(ctx.bakeLayer(BeastModels.STALKER), BeastModels.STALKER_POSE), 0.6F,
                "glassroot_stalker", BeastRenderers.stalkerGhost()));
        event.registerEntityRenderer(BeastMobs.RUIN_HOUND.get(), ctx -> new BeastRenderers.Quadruped<>(ctx,
                new VestigeQuadrupedModel<>(ctx.bakeLayer(BeastModels.HOUND), BeastModels.HOUND_POSE), 0.5F,
                "ruin_hound", h -> false));
        event.registerEntityRenderer(BeastMobs.AETHER_GRAZER.get(), ctx -> new BeastRenderers.Quadruped<>(ctx,
                new VestigeQuadrupedModel<>(ctx.bakeLayer(BeastModels.GRAZER), BeastModels.GRAZER_POSE), 0.7F,
                "aether_grazer", g -> false));
        event.registerEntityRenderer(BeastMobs.SHARDBACK_BRUTE.get(), ctx -> new BeastRenderers.Quadruped<>(ctx,
                new VestigeQuadrupedModel<>(ctx.bakeLayer(BeastModels.BRUTE), BeastModels.BRUTE_POSE), 1.3F,
                "shardback_brute", b -> false));
        event.registerEntityRenderer(BeastMobs.STARFALL_STRIDER.get(), ctx -> new BeastRenderers.Quadruped<>(ctx,
                new VestigeQuadrupedModel<>(ctx.bakeLayer(BeastModels.STRIDER), BeastModels.STRIDER_POSE), 0.6F,
                "starfall_strider", s -> false));
        event.registerEntityRenderer(BeastMobs.GRAVITY_LEECH.get(), ctx -> new BeastRenderers.Quadruped<>(ctx,
                new VestigeQuadrupedModel<>(ctx.bakeLayer(BeastModels.LEECH), BeastModels.LEECH_POSE), 0.7F,
                "gravity_leech", l -> false));
        event.registerEntityRenderer(BeastMobs.ARCHIVE_MIMIC.get(), BeastRenderers.Mimic::new);
    }
}
