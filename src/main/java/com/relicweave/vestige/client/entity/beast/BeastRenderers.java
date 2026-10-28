package com.relicweave.vestige.client.entity.beast;

import com.relicweave.RelicWeave;
import com.relicweave.vestige.entity.beast.ArchiveMimic;
import com.relicweave.vestige.entity.beast.GlassrootStalker;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;

import java.util.function.Predicate;

/** Renderer classes for the beast mobs. */
final class BeastRenderers {
    private BeastRenderers() {}

    /** Renderer for any {@link VestigeQuadrupedModel} mob; optionally switches to a translucent render type. */
    static final class Quadruped<T extends Mob> extends MobRenderer<T, VestigeQuadrupedModel<T>> {
        private final ResourceLocation texture;
        private final Predicate<T> translucent;

        Quadruped(EntityRendererProvider.Context ctx, VestigeQuadrupedModel<T> model, float shadow, String id, Predicate<T> translucent) {
            super(ctx, model, shadow);
            this.texture = RelicWeave.id("textures/entity/" + id + ".png");
            this.translucent = translucent;
        }

        @Override
        public ResourceLocation getTextureLocation(T entity) {
            return texture;
        }

        @Override
        protected RenderType getRenderType(T entity, boolean bodyVisible, boolean translucentFlag, boolean glowing) {
            if (translucent.test(entity)) {
                return RenderType.entityTranslucent(texture);
            }
            return super.getRenderType(entity, bodyVisible, translucentFlag, glowing);
        }
    }

    static Predicate<GlassrootStalker> stalkerGhost() {
        return s -> s.getState() != GlassrootStalker.ACTIVE;
    }

    /** Renderer for the Archive Mimic. */
    static final class Mimic extends MobRenderer<ArchiveMimic, ArchiveMimicModel> {
        private static final ResourceLocation TEXTURE = RelicWeave.id("textures/entity/archive_mimic.png");

        Mimic(EntityRendererProvider.Context ctx) {
            super(ctx, new ArchiveMimicModel(ctx.bakeLayer(BeastModels.MIMIC)), 0.7F);
        }

        @Override
        public ResourceLocation getTextureLocation(ArchiveMimic entity) {
            return TEXTURE;
        }
    }
}
