package com.relicweave.vestige.client.entity.humanoid;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.relicweave.RelicWeave;
import com.relicweave.vestige.entity.humanoid.GlyphBolt;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/** A small glowing billboard: teal for real glyph bolts, translucent violet for decoys. Client only. */
public class GlyphBoltRenderer extends EntityRenderer<GlyphBolt> {
    private static final ResourceLocation TEXTURE = RelicWeave.id("textures/entity/glyph_bolt.png");

    public GlyphBoltRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(GlyphBolt entity) {
        return TEXTURE;
    }

    @Override
    protected int getBlockLightLevel(GlyphBolt entity, net.minecraft.core.BlockPos pos) {
        return 15;
    }

    @Override
    public void render(GlyphBolt bolt, float yaw, float partialTick, PoseStack stack, MultiBufferSource buffers, int light) {
        boolean decoy = bolt.isDecoy();
        stack.pushPose();
        stack.scale(0.6F, 0.6F, 0.6F);
        stack.mulPose(entityRenderDispatcher.cameraOrientation());
        stack.mulPose(Axis.YP.rotationDegrees(180.0F));
        PoseStack.Pose pose = stack.last();
        Matrix4f matrix = pose.pose();
        Matrix3f normal = pose.normal();
        VertexConsumer consumer = buffers.getBuffer(RenderType.entityTranslucent(TEXTURE));
        int r = decoy ? 190 : 255;
        int g = decoy ? 120 : 255;
        int b = 255;
        int a = decoy ? 110 : 255;
        vertex(consumer, matrix, normal, 0.0F, 0.0F, 0.0F, 1.0F, r, g, b, a);
        vertex(consumer, matrix, normal, 1.0F, 0.0F, 1.0F, 1.0F, r, g, b, a);
        vertex(consumer, matrix, normal, 1.0F, 1.0F, 1.0F, 0.0F, r, g, b, a);
        vertex(consumer, matrix, normal, 0.0F, 1.0F, 0.0F, 0.0F, r, g, b, a);
        stack.popPose();
        super.render(bolt, yaw, partialTick, stack, buffers, light);
    }

    private static void vertex(VertexConsumer consumer, Matrix4f matrix, Matrix3f normal, float x, float y, float u, float v,
                               int r, int g, int b, int a) {
        consumer.vertex(matrix, x - 0.5F, y - 0.5F, 0.0F).color(r, g, b, a).uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(15728880).normal(normal, 0.0F, 1.0F, 0.0F).endVertex();
    }
}
