package com.relicweave.vestige.client.boss.a;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.relicweave.vestige.boss.a.ThornedRegent;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/**
 * A crowned plant-knight, 56 px (3.5 blocks) tall. Texture boxes (see {@code gen_bosses_a.py}): leg, body, head, crown
 * ring, crown spike, arm, core, shoulder pad. Arms rise for volleys and calls, spread for walls, hang slack while the
 * core is exposed.
 */
public class ThornedRegentModel extends EntityModel<ThornedRegent> {
    static final int TEX_W = 128;
    static final int TEX_H = 64;
    static final int[][] BOXES = {{6, 16, 6}, {16, 20, 10}, {10, 10, 10}, {12, 2, 12}, {2, 7, 2}, {5, 20, 5}, {6, 6, 2}, {7, 5, 7}};

    private final ModelPart root;
    private final ModelPart head;
    private final ModelPart leftArm;
    private final ModelPart rightArm;
    private final ModelPart leftLeg;
    private final ModelPart rightLeg;

    public ThornedRegentModel(ModelPart root) {
        this.root = root;
        this.head = root.getChild("head");
        this.leftArm = root.getChild("left_arm");
        this.rightArm = root.getChild("right_arm");
        this.leftLeg = root.getChild("left_leg");
        this.rightLeg = root.getChild("right_leg");
    }

    public static LayerDefinition createLayer() {
        int[][] uv = BossTex.layout(TEX_W, BOXES);
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(uv[0][0], uv[0][1]).addBox(-3, 0, -3, 6, 16, 6), PartPose.offset(4, 8, 0));
        root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(uv[0][0], uv[0][1]).addBox(-3, 0, -3, 6, 16, 6), PartPose.offset(-4, 8, 0));
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(uv[1][0], uv[1][1]).addBox(-8, -20, -5, 16, 20, 10), PartPose.offset(0, 8, 0));
        body.addOrReplaceChild("core", CubeListBuilder.create().texOffs(uv[6][0], uv[6][1]).addBox(-3, -16, -6, 6, 6, 2), PartPose.ZERO);
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(uv[2][0], uv[2][1]).addBox(-5, -10, -5, 10, 10, 10), PartPose.offset(0, -12, 0));
        head.addOrReplaceChild("crown_ring", CubeListBuilder.create().texOffs(uv[3][0], uv[3][1]).addBox(-6, -12, -6, 12, 2, 12), PartPose.ZERO);
        float[][] spikes = {{-6, -6}, {4, -6}, {-6, 4}, {4, 4}, {-1, -1}};
        for (int i = 0; i < spikes.length; i++) {
            head.addOrReplaceChild("crown_spike" + i, CubeListBuilder.create().texOffs(uv[4][0], uv[4][1])
                    .addBox(spikes[i][0], i == 4 ? -21 : -19, spikes[i][1], 2, i == 4 ? 9 : 7, 2), PartPose.ZERO);
        }
        PartDefinition left = root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(uv[5][0], uv[5][1]).addBox(-2.5F, 0, -2.5F, 5, 20, 5), PartPose.offset(10.5F, -10, 0));
        left.addOrReplaceChild("pad", CubeListBuilder.create().texOffs(uv[7][0], uv[7][1]).addBox(-3.5F, -3, -3.5F, 7, 5, 7), PartPose.ZERO);
        PartDefinition right = root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(uv[5][0], uv[5][1]).addBox(-2.5F, 0, -2.5F, 5, 20, 5), PartPose.offset(-10.5F, -10, 0));
        right.addOrReplaceChild("pad", CubeListBuilder.create().texOffs(uv[7][0], uv[7][1]).addBox(-3.5F, -3, -3.5F, 7, 5, 7), PartPose.ZERO);
        return LayerDefinition.create(mesh, TEX_W, TEX_H);
    }

    @Override
    public void setupAnim(ThornedRegent regent, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        head.yRot = netHeadYaw * Mth.DEG_TO_RAD;
        head.xRot = headPitch * Mth.DEG_TO_RAD;
        float swing = Mth.cos(limbSwing * 0.5F) * 0.9F * limbSwingAmount;
        rightLeg.xRot = swing;
        leftLeg.xRot = -swing;
        leftArm.zRot = 0.0F;
        rightArm.zRot = 0.0F;
        if (regent.isExposed()) {
            head.xRot += 0.45F;
            leftArm.xRot = 0.15F;
            rightArm.xRot = 0.15F;
            leftArm.zRot = 0.1F;
            rightArm.zRot = -0.1F;
            return;
        }
        switch (regent.move()) {
            case ThornedRegent.VOLLEY, ThornedRegent.CALL -> {
                float shake = Mth.sin(ageInTicks * 1.3F) * 0.06F;
                leftArm.xRot = -2.6F + shake;
                rightArm.xRot = -2.6F - shake;
            }
            case ThornedRegent.STRIKE -> {
                leftArm.xRot = -3.0F;
                rightArm.xRot = -3.0F;
                leftArm.zRot = 0.15F;
                rightArm.zRot = -0.15F;
            }
            case ThornedRegent.WALL -> {
                leftArm.xRot = 0.0F;
                rightArm.xRot = 0.0F;
                leftArm.zRot = -1.3F;
                rightArm.zRot = 1.3F;
            }
            default -> {
                leftArm.xRot = -swing;
                rightArm.xRot = swing;
            }
        }
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int light, int overlay, float r, float g, float b, float a) {
        root.render(poseStack, buffer, light, overlay, r, g, b, a);
    }
}
