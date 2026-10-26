package com.relicweave.vestige.client.entity.beast;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.relicweave.vestige.entity.beast.ArchiveMimic;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/**
 * A chest-like box: base, hinged lid, a violet keyhole and teeth on both lips. The teeth sit inside the other half
 * while the lid is closed. Texture box order: base, lid, lower teeth, upper teeth, keyhole.
 */
public class ArchiveMimicModel extends EntityModel<ArchiveMimic> {
    private final ModelPart root;
    private final ModelPart lid;

    public ArchiveMimicModel(ModelPart root) {
        this.root = root;
        this.lid = root.getChild("lid");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        TexLayout tex = new TexLayout(64);
        int[] base = tex.next(16, 10, 16);
        int[] lidUv = tex.next(16, 6, 16);
        int[] low = tex.next(14, 2, 2);
        int[] up = tex.next(14, 2, 2);
        int[] key = tex.next(2, 3, 1);
        PartDefinition basePart = root.addOrReplaceChild("base", CubeListBuilder.create().texOffs(base[0], base[1])
                .addBox(-8, -10, -8, 16, 10, 16), PartPose.offset(0, 24, 0));
        basePart.addOrReplaceChild("teeth_low", CubeListBuilder.create().texOffs(low[0], low[1])
                .addBox(-7, -12, -8, 14, 2, 2), PartPose.ZERO);
        basePart.addOrReplaceChild("keyhole", CubeListBuilder.create().texOffs(key[0], key[1])
                .addBox(-1, -6, -9, 2, 3, 1), PartPose.ZERO);
        PartDefinition lidPart = root.addOrReplaceChild("lid", CubeListBuilder.create().texOffs(lidUv[0], lidUv[1])
                .addBox(-8, -6, -16, 16, 6, 16), PartPose.offset(0, 14, 8));
        lidPart.addOrReplaceChild("teeth_up", CubeListBuilder.create().texOffs(up[0], up[1])
                .addBox(-7, 0, -16, 14, 2, 2), PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(ArchiveMimic mimic, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        float open = switch (mimic.getLidPose()) {
            case ArchiveMimic.POSE_CLOSED -> 0.0F;
            case ArchiveMimic.POSE_OPENING -> 0.7F;
            default -> 0.55F + 0.35F * Mth.sin(ageInTicks * 0.7F);   // biting
        };
        lid.xRot = -open * 1.2F;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int light, int overlay, float r, float g, float b, float a) {
        root.render(poseStack, buffer, light, overlay, r, g, b, a);
    }
}
