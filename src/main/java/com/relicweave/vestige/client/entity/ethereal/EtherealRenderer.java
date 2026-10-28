package com.relicweave.vestige.client.entity.ethereal;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;

import java.util.function.Function;
import java.util.function.Predicate;

/** One renderer for all T08 mobs: a {@link PartsModel}, a texture picker, a scale and an optional glow layer. */
public final class EtherealRenderer<T extends Mob> extends MobRenderer<T, PartsModel<T>> {
    private final Function<T, ResourceLocation> texture;
    private final float scale;

    public EtherealRenderer(EntityRendererProvider.Context context, ModelLayerLocation layer, PartsModel.Animator<T> animator,
                            float shadow, float scale, Function<T, ResourceLocation> texture,
                            ResourceLocation glow, Predicate<T> glowVisible) {
        super(context, new PartsModel<>(context.bakeLayer(layer), animator), shadow);
        this.texture = texture;
        this.scale = scale;
        if (glow != null) {
            addLayer(new GlowLayer<>(this, glow, glowVisible));
        }
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return texture.apply(entity);
    }

    @Override
    protected void scale(T entity, PoseStack poseStack, float partialTick) {
        poseStack.scale(scale, scale, scale);
    }
}
