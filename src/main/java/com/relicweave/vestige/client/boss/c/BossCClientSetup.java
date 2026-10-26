package com.relicweave.vestige.client.boss.c;

import com.relicweave.RelicWeave;
import com.relicweave.vestige.boss.c.BossesC;
import com.relicweave.vestige.client.entity.ethereal.EtherealModels;
import com.relicweave.vestige.client.entity.ethereal.EtherealRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Client-only: layer definitions and renderers for the T13 bosses, nodes and the index page projectile. */
@Mod.EventBusSubscriber(modid = RelicWeave.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class BossCClientSetup {
    private BossCClientSetup() {}

    private static ResourceLocation tex(String name) {
        return RelicWeave.id("textures/entity/" + name + ".png");
    }

    @SubscribeEvent
    public static void onLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(BossCModels.WARDEN_OF_SEALS, () -> EtherealModels.definition(BossCLayouts.WARDEN_OF_SEALS));
        event.registerLayerDefinition(BossCModels.ARCHIVIST_PRIME, () -> EtherealModels.definition(BossCLayouts.ARCHIVIST_PRIME));
        event.registerLayerDefinition(BossCModels.SEAL_NODE, () -> EtherealModels.definition(BossCLayouts.SEAL_NODE));
        event.registerLayerDefinition(BossCModels.INDEX_NODE, () -> EtherealModels.definition(BossCLayouts.INDEX_NODE));
    }

    @SubscribeEvent
    public static void onRenderers(EntityRenderersEvent.RegisterRenderers event) {
        ResourceLocation warden = tex("warden_of_seals");
        event.registerEntityRenderer(BossesC.WARDEN_OF_SEALS.get(), ctx -> new EtherealRenderer<>(ctx, BossCModels.WARDEN_OF_SEALS,
                BossCModels::warden, 1.2F, 1.6F, e -> warden, tex("warden_of_seals_glow"), e -> true));
        ResourceLocation prime = tex("archivist_prime");
        event.registerEntityRenderer(BossesC.ARCHIVIST_PRIME.get(), ctx -> new EtherealRenderer<>(ctx, BossCModels.ARCHIVIST_PRIME,
                BossCModels::prime, 1.1F, 1.7F, e -> prime, tex("archivist_prime_glow"), e -> true));
        ResourceLocation seal = tex("seal_node");
        event.registerEntityRenderer(BossesC.SEAL_NODE.get(), ctx -> new EtherealRenderer<>(ctx, BossCModels.SEAL_NODE,
                BossCModels::sealNode, 0.6F, 1.3F, e -> seal, tex("seal_node_glow"), e -> e.isExposed()));
        ResourceLocation index = tex("index_node");
        event.registerEntityRenderer(BossesC.INDEX_NODE.get(), ctx -> new EtherealRenderer<>(ctx, BossCModels.INDEX_NODE,
                BossCModels::indexNode, 0.6F, 1.4F, e -> index, tex("index_node_glow"), e -> true));
        event.registerEntityRenderer(BossesC.INDEX_PAGE.get(), IndexPageRenderer::new);
    }
}
