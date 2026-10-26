package com.relicweave.vestige.client.boss.b;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.relicweave.vestige.boss.b.GravityMaw;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/**
 * A hinged maw (upper and lower jaw with teeth and one eye) ringed by six orbiting rocks. Texture 128x64; box order,
 * mirrored by {@code tools/textures/gen_bosses_b.py}: upper, lower, tooth_u, tooth_l, back, eye, rock0..rock5.
 */
public class GravityMawModel extends EntityModel<GravityMaw> {
    /** Rock boxes (w, h, d) and their offsets from the orbit centre, in texture order. */
    static final int[][] ROCKS = {
            {5, 5, 5, 22, -6, 0}, {4, 6, 4, -22, 4, 3}, {6, 4, 5, 3, -12, 22},
            {5, 5, 5, -4, 8, -22}, {4, 4, 6, 16, 10, 16}, {6, 6, 4, -17, -10, -16}};

    private final ModelPart root;
    private final ModelPart upper;
    private final ModelPart lower;
    private final ModelPart orbit;

    public GravityMawModel(ModelPart root) {
        this.root = root;
        this.upper = root.getChild("hinge").getChild("upper");
        this.lower = root.getChild("hinge").getChild("lower");
        this.orbit = root.getChild("orbit");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        TexLayout tex = new TexLayout(128);
        int[] upperUv = tex.next(16, 8, 16);
        int[] lowerUv = tex.next(16, 6, 16);
        int[] toothU = tex.next(14, 2, 2);
        int[] toothL = tex.next(14, 2, 2);
        int[] backUv = tex.next(12, 12, 8);
        int[] eyeUv = tex.next(4, 4, 2);
        // Hinge at the back of the head, 22 px above the feet (the entity is 48 px tall).
        PartDefinition hinge = root.addOrReplaceChild("hinge", CubeListBuilder.create(), PartPose.offset(0, 2, 8));
        PartDefinition up = hinge.addOrReplaceChild("upper", CubeListBuilder.create().texOffs(upperUv[0], upperUv[1])
                .addBox(-8, -8, -16, 16, 8, 16), PartPose.ZERO);
        up.addOrReplaceChild("tooth_u", CubeListBuilder.create().texOffs(toothU[0], toothU[1])
                .addBox(-7, 0, -16, 14, 2, 2), PartPose.ZERO);
        up.addOrReplaceChild("eye", CubeListBuilder.create().texOffs(eyeUv[0], eyeUv[1])
                .addBox(-2, -10, -10, 4, 4, 2), PartPose.ZERO);
        PartDefinition low = hinge.addOrReplaceChild("lower", CubeListBuilder.create().texOffs(lowerUv[0], lowerUv[1])
                .addBox(-8, 0, -16, 16, 6, 16), PartPose.ZERO);
        low.addOrReplaceChild("tooth_l", CubeListBuilder.create().texOffs(toothL[0], toothL[1])
                .addBox(-7, -2, -16, 14, 2, 2), PartPose.ZERO);
        hinge.addOrReplaceChild("back", CubeListBuilder.create().texOffs(backUv[0], backUv[1])
                .addBox(-6, -6, 0, 12, 12, 8), PartPose.ZERO);
        PartDefinition orbit = root.addOrReplaceChild("orbit", CubeListBuilder.create(), PartPose.offset(0, 2, 0));
        for (int i = 0; i < ROCKS.length; i++) {
            int[] r = ROCKS[i];
            int[] uv = tex.next(r[0], r[1], r[2]);
            orbit.addOrReplaceChild("rock" + i, CubeListBuilder.create().texOffs(uv[0], uv[1])
                    .addBox(r[3] - r[0] / 2.0F, r[4] - r[1] / 2.0F, r[5] - r[2] / 2.0F, r[0], r[1], r[2]), PartPose.ZERO);
        }
        return LayerDefinition.create(mesh, 128, 64);
    }

    @Override
    public void setupAnim(GravityMaw maw, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        float open;
        float spin = 0.04F;
        float shake = 0.0F;
        switch (maw.getMawPose()) {
            case GravityMaw.POSE_INHALE -> {
                open = 0.95F;
                spin = 0.16F;
            }
            case GravityMaw.POSE_REPULSE -> {
                open = 0.1F;
                shake = Mth.sin(ageInTicks * 2.5F) * 0.04F;
                spin = 0.02F;
            }
            case GravityMaw.POSE_JAW -> open = 0.75F;
            case GravityMaw.POSE_GORGED -> {
                open = 0.35F;
                spin = 0.01F;
            }
            case GravityMaw.POSE_DEBRIS -> {
                open = 0.25F;
                shake = Mth.sin(ageInTicks * 1.7F) * 0.03F;
                spin = 0.08F;
            }
            default -> open = 0.15F + 0.1F * Mth.sin(ageInTicks * 0.1F);
        }
        upper.xRot = -open * 0.7F + shake;
        lower.xRot = open * 0.6F - shake;
        orbit.yRot = ageInTicks * spin;
        orbit.xRot = Mth.sin(ageInTicks * 0.03F) * 0.12F;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int light, int overlay, float r, float g, float b, float a) {
        root.render(poseStack, buffer, light, overlay, r, g, b, a);
    }
}
