package com.relicweave.vestige.client.entity.humanoid;

import com.mojang.blaze3d.vertex.PoseStack;
import com.relicweave.RelicWeave;
import com.relicweave.vestige.entity.humanoid.RiftArcher;
import net.minecraft.client.model.SkeletonModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

/** The Rift Archer: vanilla skeleton model (64x32), bow pose, slightly taller. Client only. */
public class RiftArcherRenderer extends HumanoidMobRenderer<RiftArcher, SkeletonModel<RiftArcher>> {
    private static final ResourceLocation TEXTURE = RelicWeave.id("textures/entity/rift_archer.png");
    private static final float SCALE = 1.1F;

    public RiftArcherRenderer(EntityRendererProvider.Context context) {
        super(context, new SkeletonModel<>(context.bakeLayer(ModelLayers.SKELETON)), 0.5F * SCALE);
    }

    @Override
    public ResourceLocation getTextureLocation(RiftArcher entity) {
        return TEXTURE;
    }

    @Override
    protected void scale(RiftArcher entity, PoseStack stack, float partialTick) {
        stack.scale(SCALE, SCALE, SCALE);
    }
}
