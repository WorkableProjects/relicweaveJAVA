package com.relicweave.vestige.client.entity.ethereal;

import com.relicweave.RelicWeave;
import com.relicweave.vestige.entity.ethereal.WindupState;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

/** Layer locations, layer definitions (from the generated {@link EtherealLayouts}) and animators for the T08 mobs. */
public final class EtherealModels {
    public static final ModelLayerLocation FRACTURE_WISP = layer("fracture_wisp");
    public static final ModelLayerLocation NULLWING = layer("nullwing");
    public static final ModelLayerLocation ECLIPSE_MOTH = layer("eclipse_moth");
    public static final ModelLayerLocation CONCORDANCE_GUARDIAN = layer("concordance_guardian");
    public static final ModelLayerLocation SEALED_CUSTODIAN = layer("sealed_custodian");
    public static final ModelLayerLocation RUNE_WARDEN = layer("rune_warden");
    public static final ModelLayerLocation CYCLE_HERALD = layer("cycle_herald");

    private EtherealModels() {}

    private static ModelLayerLocation layer(String id) {
        return new ModelLayerLocation(RelicWeave.id(id), "main");
    }

    /** Builds the mesh: every part is a root child with its own pivot. */
    public static LayerDefinition definition(EtherealLayouts.Layout layout) {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        for (EtherealLayouts.Part p : layout.parts()) {
            root.addOrReplaceChild(p.name(), CubeListBuilder.create().texOffs(p.u(), p.v()).addBox(p.x(), p.y(), p.z(), p.w(), p.h(), p.d()),
                    PartPose.offset(p.px(), p.py(), p.pz()));
        }
        return LayerDefinition.create(mesh, layout.texW(), layout.texH());
    }

    private static float windup(Entity e) {
        return e instanceof WindupState w ? w.windupProgress() : 0.0F;
    }

    // ---- animators ----

    public static void wisp(ModelPart root, Entity e, float limb, float limbAmt, float age, float yaw, float pitch) {
        float spin = age * (0.12F + 0.25F * windup(e));
        root.getChild("shard_a").yRot = spin;
        root.getChild("shard_b").yRot = spin * 1.3F + 2.0F;
        root.getChild("shard_c").yRot = spin * 0.8F + 4.0F;
        root.getChild("shard_a").xRot = Mth.sin(age * 0.1F) * 0.3F;
        root.getChild("shard_b").zRot = Mth.cos(age * 0.12F) * 0.3F;
        root.getChild("core").yRot = age * 0.05F;
        root.getChild("core").y += Mth.sin(age * 0.15F) * 0.8F;
    }

    public static void nullwing(ModelPart root, Entity e, float limb, float limbAmt, float age, float yaw, float pitch) {
        float fold = windup(e);
        float flap = Mth.sin(age * 0.55F) * 0.6F * (1.0F - fold) + fold * 1.1F;
        root.getChild("wing_l").zRot = -flap;
        root.getChild("wing_r").zRot = flap;
        root.getChild("head").xRot = pitch * Mth.DEG_TO_RAD * 0.5F;
        root.getChild("eyes").xRot = pitch * Mth.DEG_TO_RAD * 0.5F;
        root.getChild("tail").xRot = Mth.sin(age * 0.3F) * 0.15F + fold * 0.4F;
        root.getChild("body").xRot = fold * 0.35F;
    }

    public static void moth(ModelPart root, Entity e, float limb, float limbAmt, float age, float yaw, float pitch) {
        float flap = Mth.sin(age * 0.9F) * 0.7F;
        root.getChild("wing_l").zRot = -flap;
        root.getChild("glow_l").zRot = -flap;
        root.getChild("wing_r").zRot = flap;
        root.getChild("glow_r").zRot = flap;
        root.getChild("head").xRot = pitch * Mth.DEG_TO_RAD * 0.4F;
    }

    private static void walker(ModelPart root, Entity e, float limb, float limbAmt, float yaw, float pitch,
                               String legL, String legR, String armL, String armR, float raise, String... armExtras) {
        float swing = Mth.cos(limb * 0.6662F) * 1.1F * limbAmt;
        root.getChild(legL).xRot = swing;
        root.getChild(legR).xRot = -swing;
        float lArm = -swing * 0.8F + (-2.4F - -swing * 0.8F) * raise;
        float rArm = swing * 0.8F + (-2.4F - swing * 0.8F) * raise;
        root.getChild(armL).xRot = lArm;
        root.getChild(armR).xRot = rArm;
        for (String extra : armExtras) {
            root.getChild(extra).xRot = extra.endsWith("_l") ? lArm : rArm;
        }
        ModelPart head = root.getChild("head");
        head.yRot = yaw * Mth.DEG_TO_RAD;
        head.xRot = pitch * Mth.DEG_TO_RAD;
        if (root.hasChild("eyes")) {
            root.getChild("eyes").yRot = head.yRot;
            root.getChild("eyes").xRot = head.xRot;
        }
    }

    public static void guardian(ModelPart root, Entity e, float limb, float limbAmt, float age, float yaw, float pitch) {
        walker(root, e, limb, limbAmt, yaw, pitch, "leg_l", "leg_r", "arm_l", "arm_r", windup(e));
    }

    public static void custodian(ModelPart root, Entity e, float limb, float limbAmt, float age, float yaw, float pitch) {
        walker(root, e, limb, limbAmt, yaw, pitch, "leg_l", "leg_r", "arm_l", "arm_r", windup(e));
        root.getChild("hood").yRot = root.getChild("head").yRot;
    }

    public static void warden(ModelPart root, Entity e, float limb, float limbAmt, float age, float yaw, float pitch) {
        walker(root, e, limb, limbAmt, yaw, pitch, "leg_l", "leg_r", "arm_l", "arm_r", windup(e),
                "rune_arm_l", "rune_arm_r");
        root.getChild("crown").yRot = root.getChild("head").yRot;
        root.getChild("rune_eyes").yRot = root.getChild("head").yRot;
        root.getChild("rune_eyes").xRot = root.getChild("head").xRot;
    }

    public static void herald(ModelPart root, Entity e, float limb, float limbAmt, float age, float yaw, float pitch) {
        float raise = windup(e);
        float sway = Mth.sin(age * 0.1F) * 0.08F;
        root.getChild("arm_l").xRot = -0.3F + sway - raise * 1.8F;
        root.getChild("arm_r").xRot = -0.3F - sway - raise * 2.1F;
        root.getChild("lance").xRot = root.getChild("arm_r").xRot + 0.3F;
        root.getChild("ring").yRot = age * 0.08F;
        root.getChild("ring_top").yRot = -age * 0.11F;
        root.getChild("hood").yRot = yaw * Mth.DEG_TO_RAD * 0.6F;
        root.getChild("face").yRot = yaw * Mth.DEG_TO_RAD * 0.6F;
        root.getChild("robe_lower").zRot = sway * 0.5F;
    }
}
