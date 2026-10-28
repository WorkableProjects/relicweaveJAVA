package com.relicweave.vestige.client.boss.a;

import com.relicweave.RelicWeave;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/** Layer locations and layer definitions for the T11 boss models, plus the two prop models (thorn root, thorn spike). */
final class PropModels {
    static final ModelLayerLocation REGENT = layer("thorned_regent");
    static final ModelLayerLocation CHOIR = layer("choir_unheard");
    static final ModelLayerLocation ROOT = layer("thorn_root");
    static final ModelLayerLocation SPIKE = layer("thorn_spike");

    /** Thorn root boxes: base, trunk, thorn. Texture 128x64. */
    static final int[][] ROOT_BOXES = {{14, 4, 14}, {10, 26, 10}, {2, 10, 2}};
    /** Thorn spike box: shaft along x. Texture 32x16. */
    static final int[][] SPIKE_BOXES = {{12, 2, 2}};

    private PropModels() {}

    private static ModelLayerLocation layer(String id) {
        return new ModelLayerLocation(RelicWeave.id(id), "main");
    }

    static LayerDefinition rootLayer() {
        int[][] uv = BossTex.layout(128, ROOT_BOXES);
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition base = root.addOrReplaceChild("base", CubeListBuilder.create().texOffs(uv[0][0], uv[0][1]).addBox(-7, -4, -7, 14, 4, 14), PartPose.offset(0, 24, 0));
        PartDefinition trunk = base.addOrReplaceChild("trunk", CubeListBuilder.create().texOffs(uv[1][0], uv[1][1]).addBox(-5, -26, -5, 10, 26, 10), PartPose.offset(0, -4, 0));
        // x, z, xRot, zRot of the four thorns that splay out from the top of the trunk.
        float[][] thorns = {{-4, 0, 0, -0.5F}, {4, 0, 0, 0.5F}, {0, -4, -0.5F, 0}, {0, 4, 0.5F, 0}};
        for (int i = 0; i < thorns.length; i++) {
            trunk.addOrReplaceChild("thorn" + i, CubeListBuilder.create().texOffs(uv[2][0], uv[2][1]).addBox(-1, -10, -1, 2, 10, 2),
                    PartPose.offsetAndRotation(thorns[i][0], -24, thorns[i][1], thorns[i][2], 0, thorns[i][3]));
        }
        return LayerDefinition.create(mesh, 128, 64);
    }

    static LayerDefinition spikeLayer() {
        int[][] uv = BossTex.layout(32, SPIKE_BOXES);
        MeshDefinition mesh = new MeshDefinition();
        mesh.getRoot().addOrReplaceChild("shaft", CubeListBuilder.create().texOffs(uv[0][0], uv[0][1]).addBox(-6, -1, -1, 12, 2, 2), PartPose.ZERO);
        return LayerDefinition.create(mesh, 32, 16);
    }
}
