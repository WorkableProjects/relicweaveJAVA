package com.relicweave.vestige.client.entity.humanoid;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;

/** Renders a humanoid Vestige mob with the zombie skin layout, its own texture and a render scale. Client only. */
public class VestigeHumanoidRenderer<T extends Mob> extends HumanoidMobRenderer<T, VestigeHumanoidModel<T>> {
    private final ResourceLocation texture;
    private final float scale;

    public VestigeHumanoidRenderer(EntityRendererProvider.Context context, ResourceLocation texture, float scale) {
        super(context, new VestigeHumanoidModel<>(context.bakeLayer(ModelLayers.ZOMBIE)), 0.5F * scale);
        this.texture = texture;
        this.scale = scale;
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return texture;
    }

    @Override
    protected void scale(T entity, PoseStack stack, float partialTick) {
        stack.scale(scale, scale, scale);
    }
}
