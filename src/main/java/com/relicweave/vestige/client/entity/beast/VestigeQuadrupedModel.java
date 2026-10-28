package com.relicweave.vestige.client.entity.beast;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;

import java.util.List;

/**
 * Shared box-built quadruped for the beast mobs. Each mob supplies a {@link Spec} (proportions plus extra parts such
 * as spines, horns or plates) and a {@link PoseHook} for its wind-up poses; legs animate with the vanilla walk formula.
 */
public class VestigeQuadrupedModel<T extends Mob> extends EntityModel<T> {
    /** Extra box attached to the head or body, in that part's local coordinates. */
    public record Extra(String name, boolean onHead, int w, int h, int d, float x, float y, float z) {}

    /** Proportions in pixels. Texture box order: head, leg, body, then extras (see TexLayout). */
    public record Spec(int texW, int texH, int bw, int bh, int bl, int hw, int hh, int hd, int lw, int lh, int ld,
                       int headDrop, List<Extra> extras) {}

    /** Per-mob pose adjustments applied after the walk animation. */
    @FunctionalInterface
    public interface PoseHook<T extends Mob> {
        void apply(VestigeQuadrupedModel<T> model, T entity, float ageInTicks);
    }

    public final ModelPart root;
    public final ModelPart head;
    public final ModelPart body;
    public final ModelPart[] legs = new ModelPart[4];
    private final PoseHook<T> hook;
    /** Model alpha multiplier (used for the translucent lurking Stalker). */
    public float alpha = 1.0F;
    private final float headY;
    private final float bodyY;

    public VestigeQuadrupedModel(ModelPart root, PoseHook<T> hook) {
        this.root = root;
        this.head = root.getChild("head");
        this.body = root.getChild("body");
        for (int i = 0; i < 4; i++) {
            legs[i] = root.getChild("leg" + i);
        }
        this.hook = hook;
        this.headY = head.y;
        this.bodyY = body.y;
    }

    public static LayerDefinition createLayer(Spec s) {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        TexLayout tex = new TexLayout(s.texW());
        float bodyTop = 24 - s.lh() - s.bh() + 1;

        int[] hu = tex.next(s.hw(), s.hh(), s.hd());
        int[] lu = tex.next(s.lw(), s.lh(), s.ld());
        int[] bu = tex.next(s.bw(), s.bh(), s.bl());

        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(bu[0], bu[1])
                .addBox(-s.bw() / 2.0F, 0, -s.bl() / 2.0F, s.bw(), s.bh(), s.bl()), PartPose.offset(0, bodyTop, 0));
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(hu[0], hu[1])
                .addBox(-s.hw() / 2.0F, -s.hh() / 2.0F, -s.hd(), s.hw(), s.hh(), s.hd()),
                PartPose.offset(0, bodyTop + s.headDrop(), -s.bl() / 2.0F + 1));
        float legX = s.bw() / 2.0F - s.lw() / 2.0F;
        float legZ = s.bl() / 2.0F - s.ld() / 2.0F - 0.5F;
        float[][] legPos = {{-legX, -legZ}, {legX, -legZ}, {-legX, legZ}, {legX, legZ}};
        for (int i = 0; i < 4; i++) {
            root.addOrReplaceChild("leg" + i, CubeListBuilder.create().texOffs(lu[0], lu[1])
                    .addBox(-s.lw() / 2.0F, 0, -s.ld() / 2.0F, s.lw(), s.lh(), s.ld()),
                    PartPose.offset(legPos[i][0], 24 - s.lh(), legPos[i][1]));
        }
        for (Extra e : s.extras()) {
            int[] uv = tex.next(e.w(), e.h(), e.d());
            (e.onHead() ? head : body).addOrReplaceChild(e.name(), CubeListBuilder.create().texOffs(uv[0], uv[1])
                    .addBox(e.x(), e.y(), e.z(), e.w(), e.h(), e.d()), PartPose.ZERO);
        }
        return LayerDefinition.create(mesh, s.texW(), s.texH());
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        // Reset anything a pose hook may have moved.
        head.x = 0;
        head.y = headY;
        body.x = 0;
        body.y = bodyY;
        body.xRot = 0;
        alpha = 1.0F;
        head.yRot = netHeadYaw * Mth.DEG_TO_RAD;
        head.xRot = headPitch * Mth.DEG_TO_RAD;
        float a = limbSwing * 0.6662F;
        float amp = 1.4F * limbSwingAmount;
        legs[0].xRot = Mth.cos(a + Mth.PI) * amp;
        legs[1].xRot = Mth.cos(a) * amp;
        legs[2].xRot = Mth.cos(a) * amp;
        legs[3].xRot = Mth.cos(a + Mth.PI) * amp;
        if (hook != null) {
            hook.apply(this, entity, ageInTicks);
        }
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int light, int overlay, float r, float g, float b, float a) {
        root.render(poseStack, buffer, light, overlay, r, g, b, a * alpha);
    }

    /** Finds an extra part (child of head or body) by name, or null. */
    public ModelPart extra(String name) {
        if (head.hasChild(name)) {
            return head.getChild(name);
        }
        return body.hasChild(name) ? body.getChild(name) : null;
    }
}
