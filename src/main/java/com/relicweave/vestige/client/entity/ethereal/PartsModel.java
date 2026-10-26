package com.relicweave.vestige.client.entity.ethereal;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.Entity;

/** A flat bag of named cuboids (see {@link EtherealLayouts}) animated by a per-mob {@link Animator}. */
public final class PartsModel<T extends Entity> extends EntityModel<T> {
    /** Poses the parts of {@code root} for one frame. */
    @FunctionalInterface
    public interface Animator<T extends Entity> {
        void animate(ModelPart root, T entity, float limbSwing, float limbSwingAmount, float age, float headYaw, float headPitch);
    }

    private final ModelPart root;
    private final Animator<T> animator;

    public PartsModel(ModelPart root, Animator<T> animator) {
        this.root = root;
        this.animator = animator;
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float age, float headYaw, float headPitch) {
        root.getAllParts().forEach(ModelPart::resetPose);
        animator.animate(root, entity, limbSwing, limbSwingAmount, age, headYaw, headPitch);
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int light, int overlay, float r, float g, float b, float a) {
        root.render(poseStack, buffer, light, overlay, r, g, b, a);
    }
}
