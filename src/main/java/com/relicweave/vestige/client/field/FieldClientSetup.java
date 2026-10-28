package com.relicweave.vestige.client.field;

import com.relicweave.RelicWeave;
import com.relicweave.vestige.hazard.FieldRegistry;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Client-only: the Starfall marker has no model, its ring and column are server-sent particles. */
@Mod.EventBusSubscriber(modid = RelicWeave.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class FieldClientSetup {
    private FieldClientSetup() {}

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(FieldRegistry.STARFALL_MARKER.get(), NoopRenderer::new);
    }
}
