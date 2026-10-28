package com.relicweave.vestige.client.boss.a;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.relicweave.RelicWeave;
import com.relicweave.vestige.boss.a.ChoirUnheard;
import com.relicweave.vestige.boss.a.ThornRoot;
import com.relicweave.vestige.boss.a.ThornSpike;
import com.relicweave.vestige.boss.a.ThornedRegent;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/** Renderers for the T11 bosses and their helper entities. */
final class BossARenderers {
    private BossARenderers() {}

    private static ResourceLocation tex(String id) {
        return RelicWeave.id("textures/entity/" + id + ".png");
    }

    /** Thorned Regent; the core glows (full-bright) while exposed. */
    static final class Regent extends MobRenderer<ThornedRegent, ThornedRegentModel> {
        private static final ResourceLocation TEXTURE = tex("thorned_regent");
        private static final ResourceLocation GLOW = tex("thorned_regent_glow");

        Regent(EntityRendererProvider.Context ctx) {
            super(ctx, new ThornedRegentModel(ctx.bakeLayer(PropModels.REGENT)), 1.4F);
            addLayer(new CoreGlow(this));
        }

        @Override
        public ResourceLocation getTextureLocation(ThornedRegent entity) {
            return TEXTURE;
        }

        private static final class CoreGlow extends RenderLayer<ThornedRegent, ThornedRegentModel> {
            CoreGlow(RenderLayerParent<ThornedRegent, ThornedRegentModel> parent) {
                super(parent);
            }

            @Override
            public void render(PoseStack poseStack, MultiBufferSource buffer, int light, ThornedRegent regent, float limbSwing,
                               float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
                if (regent.isExposed()) {
                    getParentModel().renderToBuffer(poseStack, buffer.getBuffer(RenderType.eyes(GLOW)), LightTexture.FULL_BRIGHT,
                            OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
                }
            }
        }
    }

    /** Choir Unheard. */
    static final class Choir extends MobRenderer<ChoirUnheard, ChoirUnheardModel> {
        private static final ResourceLocation TEXTURE = tex("choir_unheard");

        Choir(EntityRendererProvider.Context ctx) {
            super(ctx, new ChoirUnheardModel(ctx.bakeLayer(PropModels.CHOIR)), 0.6F);
        }

        @Override
        public ResourceLocation getTextureLocation(ChoirUnheard entity) {
            return TEXTURE;
        }
    }

    /** One wall segment. */
    static final class Root extends EntityRenderer<ThornRoot> {
        private static final ResourceLocation TEXTURE = tex("thorn_root");
        private final ModelPart model;

        Root(EntityRendererProvider.Context ctx) {
            super(ctx);
            this.model = ctx.bakeLayer(PropModels.ROOT);
        }

        @Override
        public void render(ThornRoot entity, float yaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int light) {
            poseStack.pushPose();
            poseStack.scale(-1.0F, -1.0F, 1.0F);
            poseStack.translate(0.0F, -1.501F, 0.0F);
            // Rise out of the ground in the first 8 ticks, sink in the last 10.
            float grow = Math.min(1.0F, (entity.tickCount + partialTick) / 8.0F);
            float shrink = Math.min(1.0F, (ThornRoot.LIFETIME_TICKS - entity.tickCount - partialTick) / 10.0F);
            poseStack.translate(0.0F, (1.0F - Math.max(0.0F, Math.min(grow, shrink))) * -1.5F, 0.0F);
            model.render(poseStack, buffer.getBuffer(RenderType.entityCutoutNoCull(TEXTURE)), light, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
            poseStack.popPose();
            super.render(entity, yaw, partialTick, poseStack, buffer, light);
        }

        @Override
        public ResourceLocation getTextureLocation(ThornRoot entity) {
            return TEXTURE;
        }
    }

    /** A flying thorn. */
    static final class Spike extends EntityRenderer<ThornSpike> {
        private static final ResourceLocation TEXTURE = tex("thorn_spike");
        private final ModelPart model;

        Spike(EntityRendererProvider.Context ctx) {
            super(ctx);
            this.model = ctx.bakeLayer(PropModels.SPIKE);
        }

        @Override
        public void render(ThornSpike entity, float yaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int light) {
            poseStack.pushPose();
            poseStack.mulPose(Axis.YP.rotationDegrees(Mth.lerp(partialTick, entity.yRotO, entity.getYRot()) - 90.0F));
            poseStack.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(partialTick, entity.xRotO, entity.getXRot())));
            model.render(poseStack, buffer.getBuffer(RenderType.entityCutoutNoCull(TEXTURE)), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
            poseStack.popPose();
            super.render(entity, yaw, partialTick, poseStack, buffer, light);
        }

        @Override
        public ResourceLocation getTextureLocation(ThornSpike entity) {
            return TEXTURE;
        }
    }
}
