package com.relicweave.vestige.client.boss.b;

import com.relicweave.RelicWeave;
import com.relicweave.vestige.boss.b.BossesB;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Client-only registration of the T12 boss models and renderers. */
@Mod.EventBusSubscriber(modid = RelicWeave.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class BossesBClientSetup {
    static final ModelLayerLocation MAW = new ModelLayerLocation(RelicWeave.id("gravity_maw"), "main");
    static final ModelLayerLocation COLOSSUS = new ModelLayerLocation(RelicWeave.id("starforged_colossus"), "main");

    private BossesBClientSetup() {}

    @SubscribeEvent
    public static void onRegisterLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(MAW, GravityMawModel::createLayer);
        event.registerLayerDefinition(COLOSSUS, StarforgedColossusModel::createLayer);
    }

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(BossesB.GRAVITY_MAW.get(), BossesBRenderers.Maw::new);
        event.registerEntityRenderer(BossesB.STARFORGED_COLOSSUS.get(), BossesBRenderers.Colossus::new);
    }
}
