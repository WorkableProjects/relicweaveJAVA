package com.relicweave.vestige.client.entity.humanoid;

import com.relicweave.vestige.entity.humanoid.VisualPose;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.Mob;

/** Vanilla humanoid model whose arms follow the mob's synced {@link VisualPose}. Client only. */
public class VestigeHumanoidModel<T extends Mob> extends HumanoidModel<T> {
    public VestigeHumanoidModel(ModelPart root) {
        super(root);
    }

    @Override
    public void prepareMobModel(T entity, float limbSwing, float limbSwingAmount, float partialTick) {
        super.prepareMobModel(entity, limbSwing, limbSwingAmount, partialTick);
        if (entity instanceof VisualPose pose && pose.visualPose() == VisualPose.POSE_GUARD) {
            leftArmPose = ArmPose.BLOCK;
        }
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        if (!(entity instanceof VisualPose pose)) {
            return;
        }
        float bob = (float) Math.sin(ageInTicks * 0.3F) * 0.05F;
        switch (pose.visualPose()) {
            case VisualPose.POSE_RAISED -> {
                rightArm.xRot = -2.4F + bob;
                leftArm.xRot = -2.4F - bob;
                rightArm.yRot = -0.15F;
                leftArm.yRot = 0.15F;
            }
            case VisualPose.POSE_OVERHEAD -> {
                rightArm.xRot = -2.9F;
                rightArm.yRot = 0.0F;
            }
            case VisualPose.POSE_REACH -> {
                rightArm.xRot = -1.5F + bob;
                leftArm.xRot = -1.5F - bob;
                rightArm.yRot = -0.1F;
                leftArm.yRot = 0.1F;
            }
            default -> { }
        }
    }
}
