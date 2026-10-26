package com.relicweave.vestige.client.boss.a;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.relicweave.vestige.boss.a.ChoirUnheard;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/**
 * A tall veiled figure in a long robe, haloed, with six voice masks orbiting it. Texture boxes (see
 * {@code gen_bosses_a.py}): robe, head, veil, arm, mask, halo. Arms rise during any move and the masks spin faster.
 */
public class ChoirUnheardModel extends EntityModel<ChoirUnheard> {
    static final int TEX_W = 128;
    static final int TEX_H = 64;
    static final int[][] BOXES = {{12, 30, 8}, {8, 8, 8}, {9, 10, 1}, {3, 16, 3}, {6, 8, 2}, {10, 1, 10}};
    static final int MASKS = 6;

    private final ModelPart root;
    private final ModelPart head;
    private final ModelPart leftArm;
    private final ModelPart rightArm;
    private final ModelPart[] masks = new ModelPart[MASKS];

    public ChoirUnheardModel(ModelPart root) {
        this.root = root;
        this.head = root.getChild("head");
        this.leftArm = root.getChild("left_arm");
        this.rightArm = root.getChild("right_arm");
        for (int i = 0; i < MASKS; i++) {
            masks[i] = root.getChild("mask" + i);
        }
    }

    public static LayerDefinition createLayer() {
        int[][] uv = BossTex.layout(TEX_W, BOXES);
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("robe", CubeListBuilder.create().texOffs(uv[0][0], uv[0][1]).addBox(-6, -30, -4, 12, 30, 8), PartPose.offset(0, 24, 0));
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(uv[1][0], uv[1][1]).addBox(-4, -8, -4, 8, 8, 8), PartPose.offset(0, -6, 0));
        head.addOrReplaceChild("veil", CubeListBuilder.create().texOffs(uv[2][0], uv[2][1]).addBox(-4.5F, -7, -5, 9, 10, 1), PartPose.ZERO);
        head.addOrReplaceChild("halo", CubeListBuilder.create().texOffs(uv[5][0], uv[5][1]).addBox(-5, -12, -5, 10, 1, 10), PartPose.ZERO);
        root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(uv[3][0], uv[3][1]).addBox(-1.5F, 0, -1.5F, 3, 16, 3), PartPose.offset(7.5F, -3, 0));
        root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(uv[3][0], uv[3][1]).addBox(-1.5F, 0, -1.5F, 3, 16, 3), PartPose.offset(-7.5F, -3, 0));
        for (int i = 0; i < MASKS; i++) {
            root.addOrReplaceChild("mask" + i, CubeListBuilder.create().texOffs(uv[4][0], uv[4][1]).addBox(-3, -4, -1, 6, 8, 2), PartPose.offset(0, 6, 18));
        }
        return LayerDefinition.create(mesh, TEX_W, TEX_H);
    }

    @Override
    public void setupAnim(ChoirUnheard choir, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        head.yRot = netHeadYaw * Mth.DEG_TO_RAD;
        head.xRot = headPitch * Mth.DEG_TO_RAD;
        boolean casting = choir.move() != ChoirUnheard.IDLE;
        float sway = Mth.sin(ageInTicks * 0.08F) * 0.05F;
        leftArm.xRot = casting ? -2.3F : sway;
        rightArm.xRot = casting ? -2.3F : -sway;
        leftArm.zRot = casting ? -0.3F : -0.12F;
        rightArm.zRot = casting ? 0.3F : 0.12F;
        float speed = casting ? 0.16F : 0.05F;
        for (int i = 0; i < MASKS; i++) {
            float angle = ageInTicks * speed + i * Mth.TWO_PI / MASKS;
            masks[i].x = Mth.cos(angle) * 18.0F;
            masks[i].z = Mth.sin(angle) * 18.0F;
            masks[i].y = 6.0F + Mth.sin(ageInTicks * 0.1F + i) * 2.0F;
            masks[i].yRot = -angle + Mth.HALF_PI;
        }
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int light, int overlay, float r, float g, float b, float a) {
        root.render(poseStack, buffer, light, overlay, r, g, b, a);
    }
}
