package com.relicweave.vestige.client.boss.b;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.relicweave.RelicWeave;
import com.relicweave.vestige.boss.b.GravityMaw;
import com.relicweave.vestige.boss.b.StarforgedColossus;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;

/** Renderer classes for the T12 bosses. */
final class BossesBRenderers {
    private BossesBRenderers() {}

    static final class Maw extends MobRenderer<GravityMaw, GravityMawModel> {
        private static final ResourceLocation TEXTURE = RelicWeave.id("textures/entity/gravity_maw.png");

        Maw(EntityRendererProvider.Context ctx) {
            super(ctx, new GravityMawModel(ctx.bakeLayer(BossesBClientSetup.MAW)), 0.0F);
        }

        @Override
        public ResourceLocation getTextureLocation(GravityMaw entity) {
            return TEXTURE;
        }
    }

    static final class Colossus extends MobRenderer<StarforgedColossus, StarforgedColossusModel> {
        private static final ResourceLocation TEXTURE = RelicWeave.id("textures/entity/starforged_colossus.png");
        private static final ResourceLocation GLOW = RelicWeave.id("textures/entity/starforged_colossus_glow.png");

        Colossus(EntityRendererProvider.Context ctx) {
            super(ctx, new StarforgedColossusModel(ctx.bakeLayer(BossesBClientSetup.COLOSSUS)), 1.8F);
            addLayer(new HeatLayer(this));
        }

        @Override
        public ResourceLocation getTextureLocation(StarforgedColossus entity) {
            return TEXTURE;
        }

        /** Emissive vein overlay, drawn while the Colossus telegraphs a heat pulse, overheats or is vulnerable. */
        private static final class HeatLayer extends RenderLayer<StarforgedColossus, StarforgedColossusModel> {
            HeatLayer(RenderLayerParent<StarforgedColossus, StarforgedColossusModel> parent) {
                super(parent);
            }

            @Override
            public void render(PoseStack poseStack, MultiBufferSource buffers, int light, StarforgedColossus boss, float limbSwing,
                               float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
                boolean glowing = boss.getHeat() != StarforgedColossus.HEAT_NONE
                        || boss.getColossusPose() == StarforgedColossus.POSE_PULSE;
                if (!glowing || boss.isInvisible()) {
                    return;
                }
                VertexConsumer consumer = buffers.getBuffer(RenderType.eyes(GLOW));
                getParentModel().renderToBuffer(poseStack, consumer, 15728640, LivingEntityRenderer.getOverlayCoords(boss, 0.0F),
                        1.0F, 1.0F, 1.0F, 1.0F);
            }
        }
    }
}
