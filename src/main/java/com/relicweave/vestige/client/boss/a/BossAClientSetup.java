package com.relicweave.vestige.client.boss.a;

import com.relicweave.RelicWeave;
import com.relicweave.vestige.boss.a.BossesA;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Client-only registration of the Thorned Regent / Choir Unheard models and renderers. */
@Mod.EventBusSubscriber(modid = RelicWeave.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class BossAClientSetup {
    private BossAClientSetup() {}

    @SubscribeEvent
    public static void onRegisterLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(PropModels.REGENT, ThornedRegentModel::createLayer);
        event.registerLayerDefinition(PropModels.CHOIR, ChoirUnheardModel::createLayer);
        event.registerLayerDefinition(PropModels.ROOT, PropModels::rootLayer);
        event.registerLayerDefinition(PropModels.SPIKE, PropModels::spikeLayer);
    }

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(BossesA.THORNED_REGENT.get(), BossARenderers.Regent::new);
        event.registerEntityRenderer(BossesA.CHOIR_UNHEARD.get(), BossARenderers.Choir::new);
        event.registerEntityRenderer(BossesA.THORN_ROOT.get(), BossARenderers.Root::new);
        event.registerEntityRenderer(BossesA.THORN_SPIKE.get(), BossARenderers.Spike::new);
    }
}
