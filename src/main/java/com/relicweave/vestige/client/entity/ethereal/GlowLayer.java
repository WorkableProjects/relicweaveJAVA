package com.relicweave.vestige.client.entity.ethereal;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

import java.util.function.Predicate;

/** Draws the glowing parts of a mob full-bright (the {@code _glow} texture holds only those parts). */
public final class GlowLayer<T extends Entity, M extends EntityModel<T>> extends RenderLayer<T, M> {
    private final ResourceLocation texture;
    private final Predicate<T> visible;

    public GlowLayer(RenderLayerParent<T, M> parent, ResourceLocation texture, Predicate<T> visible) {
        super(parent);
        this.texture = texture;
        this.visible = visible;
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int light, T entity, float limbSwing, float limbSwingAmount,
                       float partialTick, float age, float headYaw, float headPitch) {
        if (!visible.test(entity)) {
            return;
        }
        VertexConsumer consumer = buffer.getBuffer(RenderType.eyes(texture));
        getParentModel().renderToBuffer(poseStack, consumer, 0xF00000, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
    }
}
