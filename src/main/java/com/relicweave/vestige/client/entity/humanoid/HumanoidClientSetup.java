package com.relicweave.vestige.client.entity.humanoid;

import com.relicweave.RelicWeave;
import com.relicweave.vestige.entity.humanoid.HumanoidMobs;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Client-only renderer registration for the humanoid mobs. They reuse the vanilla zombie / skeleton model layers, so
 * no layer definitions are needed; each mob differs by texture, scale, held items and arm pose.
 */
@Mod.EventBusSubscriber(modid = RelicWeave.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class HumanoidClientSetup {
    private HumanoidClientSetup() {}

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(HumanoidMobs.RELIQUARY_SENTINEL.get(), c -> new VestigeHumanoidRenderer<>(c, tex("reliquary_sentinel"), 1.1F));
        event.registerEntityRenderer(HumanoidMobs.SCRIPTBOUND_SCRIBE.get(), c -> new VestigeHumanoidRenderer<>(c, tex("scriptbound_scribe"), 1.0F));
        event.registerEntityRenderer(HumanoidMobs.GILDED_HUSK.get(), c -> new VestigeHumanoidRenderer<>(c, tex("gilded_husk"), 1.1F));
        event.registerEntityRenderer(HumanoidMobs.MIREBOUND_CANTOR.get(), c -> new VestigeHumanoidRenderer<>(c, tex("mirebound_cantor"), 0.95F));
        event.registerEntityRenderer(HumanoidMobs.OATHBREAKER_KNIGHT.get(), c -> new VestigeHumanoidRenderer<>(c, tex("oathbreaker_knight"), 1.25F));
        event.registerEntityRenderer(HumanoidMobs.VESTIGE_DRIFTER.get(), c -> new VestigeHumanoidRenderer<>(c, tex("vestige_drifter"), 0.95F));
        event.registerEntityRenderer(HumanoidMobs.RIFT_ARCHER.get(), RiftArcherRenderer::new);
        event.registerEntityRenderer(HumanoidMobs.ECHO_WEAVER.get(), c -> new VestigeHumanoidRenderer<>(c, tex("echo_weaver"), 0.9F));
        event.registerEntityRenderer(HumanoidMobs.GLYPH_BOLT.get(), GlyphBoltRenderer::new);
    }

    private static net.minecraft.resources.ResourceLocation tex(String id) {
        return RelicWeave.id("textures/entity/" + id + ".png");
    }
}
