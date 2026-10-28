package com.relicweave.vestige.client.entity.ethereal;

import com.relicweave.RelicWeave;
import com.relicweave.vestige.entity.ethereal.EtherealMobs;
import com.relicweave.vestige.entity.ethereal.RuneWarden;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Client-only: layer definitions and renderers for the T08 mobs. */
@Mod.EventBusSubscriber(modid = RelicWeave.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class EtherealClientSetup {
    private EtherealClientSetup() {}

    private static ResourceLocation tex(String name) {
        return RelicWeave.id("textures/entity/" + name + ".png");
    }

    @SubscribeEvent
    public static void onLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(EtherealModels.FRACTURE_WISP, () -> EtherealModels.definition(EtherealLayouts.FRACTURE_WISP));
        event.registerLayerDefinition(EtherealModels.NULLWING, () -> EtherealModels.definition(EtherealLayouts.NULLWING));
        event.registerLayerDefinition(EtherealModels.ECLIPSE_MOTH, () -> EtherealModels.definition(EtherealLayouts.ECLIPSE_MOTH));
        event.registerLayerDefinition(EtherealModels.CONCORDANCE_GUARDIAN, () -> EtherealModels.definition(EtherealLayouts.CONCORDANCE_GUARDIAN));
        event.registerLayerDefinition(EtherealModels.SEALED_CUSTODIAN, () -> EtherealModels.definition(EtherealLayouts.SEALED_CUSTODIAN));
        event.registerLayerDefinition(EtherealModels.RUNE_WARDEN, () -> EtherealModels.definition(EtherealLayouts.RUNE_WARDEN));
        event.registerLayerDefinition(EtherealModels.CYCLE_HERALD, () -> EtherealModels.definition(EtherealLayouts.CYCLE_HERALD));
    }

    @SubscribeEvent
    public static void onRenderers(EntityRenderersEvent.RegisterRenderers event) {
        ResourceLocation wisp = tex("fracture_wisp");
        event.registerEntityRenderer(EtherealMobs.FRACTURE_WISP.get(), ctx -> new EtherealRenderer<>(ctx, EtherealModels.FRACTURE_WISP,
                EtherealModels::wisp, 0.2F, 1.0F, e -> wisp, tex("fracture_wisp_glow"), e -> true));
        ResourceLocation nullwing = tex("nullwing");
        event.registerEntityRenderer(EtherealMobs.NULLWING.get(), ctx -> new EtherealRenderer<>(ctx, EtherealModels.NULLWING,
                EtherealModels::nullwing, 0.4F, 1.1F, e -> nullwing, tex("nullwing_glow"), e -> true));
        ResourceLocation moth = tex("eclipse_moth");
        event.registerEntityRenderer(EtherealMobs.ECLIPSE_MOTH.get(), ctx -> new EtherealRenderer<>(ctx, EtherealModels.ECLIPSE_MOTH,
                EtherealModels::moth, 0.15F, 1.0F, e -> moth, tex("eclipse_moth_glow"), e -> true));
        ResourceLocation guardian = tex("concordance_guardian");
        event.registerEntityRenderer(EtherealMobs.CONCORDANCE_GUARDIAN.get(), ctx -> new EtherealRenderer<>(ctx, EtherealModels.CONCORDANCE_GUARDIAN,
                EtherealModels::guardian, 1.0F, 1.5F, e -> guardian, tex("concordance_guardian_glow"), e -> true));
        ResourceLocation custodian = tex("sealed_custodian");
        event.registerEntityRenderer(EtherealMobs.SEALED_CUSTODIAN.get(), ctx -> new EtherealRenderer<>(ctx, EtherealModels.SEALED_CUSTODIAN,
                EtherealModels::custodian, 0.8F, 1.3F, e -> custodian, tex("sealed_custodian_glow"), e -> true));
        ResourceLocation wardenAwake = tex("rune_warden");
        ResourceLocation wardenStone = tex("rune_warden_dormant");
        event.registerEntityRenderer(EtherealMobs.RUNE_WARDEN.get(), ctx -> new EtherealRenderer<RuneWarden>(ctx, EtherealModels.RUNE_WARDEN,
                EtherealModels::warden, 0.7F, 1.25F, e -> e.isDormant() && !e.isAwakening() ? wardenStone : wardenAwake,
                tex("rune_warden_glow"), e -> !e.isDormant() || e.isAwakening()));
        ResourceLocation herald = tex("cycle_herald");
        event.registerEntityRenderer(EtherealMobs.CYCLE_HERALD.get(), ctx -> new EtherealRenderer<>(ctx, EtherealModels.CYCLE_HERALD,
                EtherealModels::herald, 0.5F, 1.2F, e -> herald, tex("cycle_herald_glow"), e -> true));
        event.registerEntityRenderer(EtherealMobs.RUNE_SHARD.get(), ctx -> new ThrownItemRenderer<>(ctx, 1.2F, true));
    }
}
