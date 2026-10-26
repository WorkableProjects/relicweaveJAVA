package com.relicweave.vestige.client.boss.b;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.relicweave.vestige.boss.b.StarforgedColossus;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/**
 * A hulking forge golem (about 3 x 4.75 blocks): broad torso with a furnace core, vent stacks on its back, heavy
 * shoulder plates and long arms. Texture 256x128; box order, mirrored by {@code tools/textures/gen_bosses_b.py}: legR,
 * legL, torso, head, armR, armL, shoulderR, shoulderL, furnace, ventR, ventL, visor.
 */
public class StarforgedColossusModel extends EntityModel<StarforgedColossus> {
    private final ModelPart root;
    private final ModelPart head;
    private final ModelPart armR;
    private final ModelPart armL;
    private final ModelPart legR;
    private final ModelPart legL;

    public StarforgedColossusModel(ModelPart root) {
        this.root = root;
        this.head = root.getChild("head");
        this.armR = root.getChild("arm_r");
        this.armL = root.getChild("arm_l");
        this.legR = root.getChild("leg_r");
        this.legL = root.getChild("leg_l");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        TexLayout tex = new TexLayout(256);
        int[] legRUv = tex.next(12, 28, 12);
        int[] legLUv = tex.next(12, 28, 12);
        int[] torsoUv = tex.next(30, 32, 18);
        int[] headUv = tex.next(16, 16, 16);
        int[] armRUv = tex.next(10, 30, 10);
        int[] armLUv = tex.next(10, 30, 10);
        int[] shRUv = tex.next(14, 6, 14);
        int[] shLUv = tex.next(14, 6, 14);
        int[] furnaceUv = tex.next(12, 10, 2);
        int[] ventRUv = tex.next(4, 10, 4);
        int[] ventLUv = tex.next(4, 10, 4);
        int[] visorUv = tex.next(12, 3, 1);
        root.addOrReplaceChild("leg_r", CubeListBuilder.create().texOffs(legRUv[0], legRUv[1])
                .addBox(-6, 0, -6, 12, 28, 12), PartPose.offset(-8, -4, 0));
        root.addOrReplaceChild("leg_l", CubeListBuilder.create().texOffs(legLUv[0], legLUv[1])
                .addBox(-6, 0, -6, 12, 28, 12), PartPose.offset(8, -4, 0));
        PartDefinition torso = root.addOrReplaceChild("torso", CubeListBuilder.create().texOffs(torsoUv[0], torsoUv[1])
                .addBox(-15, -32, -9, 30, 32, 18), PartPose.offset(0, -4, 0));
        torso.addOrReplaceChild("furnace", CubeListBuilder.create().texOffs(furnaceUv[0], furnaceUv[1])
                .addBox(-6, -24, -10, 12, 10, 2), PartPose.ZERO);
        torso.addOrReplaceChild("vent_r", CubeListBuilder.create().texOffs(ventRUv[0], ventRUv[1])
                .addBox(-11, -42, 4, 4, 10, 4), PartPose.ZERO);
        torso.addOrReplaceChild("vent_l", CubeListBuilder.create().texOffs(ventLUv[0], ventLUv[1])
                .addBox(7, -42, 4, 4, 10, 4), PartPose.ZERO);
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(headUv[0], headUv[1])
                .addBox(-8, -16, -8, 16, 16, 16), PartPose.offset(0, -36, 0));
        head.addOrReplaceChild("visor", CubeListBuilder.create().texOffs(visorUv[0], visorUv[1])
                .addBox(-6, -10, -9, 12, 3, 1), PartPose.ZERO);
        PartDefinition armR = root.addOrReplaceChild("arm_r", CubeListBuilder.create().texOffs(armRUv[0], armRUv[1])
                .addBox(-5, 0, -5, 10, 30, 10), PartPose.offset(-20, -30, 0));
        armR.addOrReplaceChild("shoulder_r", CubeListBuilder.create().texOffs(shRUv[0], shRUv[1])
                .addBox(-7, -5, -7, 14, 6, 14), PartPose.ZERO);
        PartDefinition armL = root.addOrReplaceChild("arm_l", CubeListBuilder.create().texOffs(armLUv[0], armLUv[1])
                .addBox(-5, 0, -5, 10, 30, 10), PartPose.offset(20, -30, 0));
        armL.addOrReplaceChild("shoulder_l", CubeListBuilder.create().texOffs(shLUv[0], shLUv[1])
                .addBox(-7, -5, -7, 14, 6, 14), PartPose.ZERO);
        return LayerDefinition.create(mesh, 256, 128);
    }

    @Override
    public void setupAnim(StarforgedColossus boss, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        head.yRot = netHeadYaw * Mth.DEG_TO_RAD;
        head.xRot = headPitch * Mth.DEG_TO_RAD;
        float swing = Mth.cos(limbSwing * 0.5F) * limbSwingAmount;
        legR.xRot = swing * 0.9F;
        legL.xRot = -swing * 0.9F;
        armR.xRot = -swing * 0.5F;
        armL.xRot = swing * 0.5F;
        armR.zRot = 0.05F;
        armL.zRot = -0.05F;
        switch (boss.getColossusPose()) {
            case StarforgedColossus.POSE_SLAM -> {
                armR.xRot = -2.6F + Mth.sin(ageInTicks * 0.6F) * 0.05F;
                armL.xRot = -2.6F + Mth.sin(ageInTicks * 0.6F) * 0.05F;
            }
            case StarforgedColossus.POSE_PULSE -> {
                armR.zRot = 1.3F;
                armL.zRot = -1.3F;
                head.xRot = -0.35F;
            }
            case StarforgedColossus.POSE_BARRAGE -> {
                armR.xRot = -3.0F;
                armL.xRot = -3.0F;
                head.xRot = -0.5F;
            }
            default -> { }
        }
        if (boss.getHeat() == StarforgedColossus.HEAT_VULNERABLE) {
            head.xRot += 0.45F;
            armR.xRot += 0.2F;
            armL.xRot += 0.2F;
        } else if (boss.getHeat() == StarforgedColossus.HEAT_OVERHEAT) {
            armR.zRot += 0.35F;
            armL.zRot -= 0.35F;
        }
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int light, int overlay, float r, float g, float b, float a) {
        root.render(poseStack, buffer, light, overlay, r, g, b, a);
    }
}
