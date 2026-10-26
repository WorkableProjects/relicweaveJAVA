package com.relicweave.vestige.client.boss.c;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.relicweave.RelicWeave;
import com.relicweave.vestige.boss.c.IndexPage;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/** A small full-bright page billboard that slowly tumbles. Client only. */
public class IndexPageRenderer extends EntityRenderer<IndexPage> {
    private static final ResourceLocation TEXTURE = RelicWeave.id("textures/entity/index_page.png");

    public IndexPageRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(IndexPage entity) {
        return TEXTURE;
    }

    @Override
    protected int getBlockLightLevel(IndexPage entity, net.minecraft.core.BlockPos pos) {
        return 15;
    }

    @Override
    public void render(IndexPage page, float yaw, float partialTick, PoseStack stack, MultiBufferSource buffers, int light) {
        stack.pushPose();
        stack.scale(0.7F, 0.7F, 0.7F);
        stack.mulPose(entityRenderDispatcher.cameraOrientation());
        stack.mulPose(Axis.YP.rotationDegrees(180.0F));
        stack.mulPose(Axis.ZP.rotationDegrees((page.tickCount + partialTick) * 18.0F));
        PoseStack.Pose pose = stack.last();
        Matrix4f matrix = pose.pose();
        Matrix3f normal = pose.normal();
        VertexConsumer consumer = buffers.getBuffer(RenderType.entityTranslucent(TEXTURE));
        vertex(consumer, matrix, normal, 0.0F, 0.0F, 0.0F, 1.0F);
        vertex(consumer, matrix, normal, 1.0F, 0.0F, 1.0F, 1.0F);
        vertex(consumer, matrix, normal, 1.0F, 1.0F, 1.0F, 0.0F);
        vertex(consumer, matrix, normal, 0.0F, 1.0F, 0.0F, 0.0F);
        stack.popPose();
        super.render(page, yaw, partialTick, stack, buffers, light);
    }

    private static void vertex(VertexConsumer consumer, Matrix4f matrix, Matrix3f normal, float x, float y, float u, float v) {
        consumer.vertex(matrix, x - 0.5F, y - 0.5F, 0.0F).color(255, 255, 255, 255).uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(15728880).normal(normal, 0.0F, 1.0F, 0.0F).endVertex();
    }
}
