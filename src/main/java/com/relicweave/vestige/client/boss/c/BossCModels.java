package com.relicweave.vestige.client.boss.c;

import com.relicweave.RelicWeave;
import com.relicweave.vestige.boss.c.ArchivistPrime;
import com.relicweave.vestige.boss.c.WardenOfSeals;
import com.relicweave.vestige.entity.ethereal.WindupState;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

/** Layer locations and animators for the T13 bosses and nodes (meshes come from the generated {@link BossCLayouts}). */
public final class BossCModels {
    public static final ModelLayerLocation WARDEN_OF_SEALS = layer("warden_of_seals");
    public static final ModelLayerLocation ARCHIVIST_PRIME = layer("archivist_prime");
    public static final ModelLayerLocation SEAL_NODE = layer("seal_node");
    public static final ModelLayerLocation INDEX_NODE = layer("index_node");

    private BossCModels() {}

    private static ModelLayerLocation layer(String id) {
        return new ModelLayerLocation(RelicWeave.id(id), "main");
    }

    private static float windup(Entity e) {
        return e instanceof WindupState w ? w.windupProgress() : 0.0F;
    }

    /** Places {@code part} on a horizontal orbit of {@code radius} around its pivot, facing outward. */
    private static void orbit(ModelPart part, float angle, float radius, float bob) {
        part.x += Mth.cos(angle) * radius;
        part.z += Mth.sin(angle) * radius;
        part.y += bob;
        part.yRot = -angle + Mth.HALF_PI;
    }

    private static void walker(ModelPart root, float limb, float limbAmt, float yaw, float pitch, float raise) {
        ModelPart head = root.getChild("head");
        head.yRot = yaw * Mth.DEG_TO_RAD;
        head.xRot = pitch * Mth.DEG_TO_RAD;
        root.getChild("leg_l").xRot = Mth.cos(limb * 0.6662F) * 1.2F * limbAmt;
        root.getChild("leg_r").xRot = Mth.cos(limb * 0.6662F + Mth.PI) * 1.2F * limbAmt;
        root.getChild("arm_l").xRot = Mth.cos(limb * 0.6662F + Mth.PI) * 0.8F * limbAmt * (1.0F - raise) - raise * 2.6F;
        root.getChild("arm_r").xRot = Mth.cos(limb * 0.6662F) * 0.8F * limbAmt * (1.0F - raise) - raise * 2.6F;
    }

    public static void warden(ModelPart root, Entity e, float limb, float limbAmt, float age, float yaw, float pitch) {
        float raise = windup(e);
        walker(root, limb, limbAmt, yaw, pitch, raise);
        root.getChild("crest").yRot = root.getChild("head").yRot;
        root.getChild("eyes").yRot = root.getChild("head").yRot;
        root.getChild("eyes").xRot = root.getChild("head").xRot;
        root.getChild("pauldron_l").xRot = root.getChild("arm_l").xRot * 0.5F;
        root.getChild("pauldron_r").xRot = root.getChild("arm_r").xRot * 0.5F;
        boolean sealed = e instanceof WardenOfSeals w && w.isSealed();
        float speed = sealed ? 0.14F : 0.06F;
        String[] plates = {"halo_a", "halo_b", "halo_c", "halo_d"};
        for (int i = 0; i < plates.length; i++) {
            float a = age * speed + i * Mth.HALF_PI;
            orbit(root.getChild(plates[i]), a, sealed ? 13.0F : 11.0F, Mth.sin(age * 0.08F + i) * 1.0F - raise * 2.0F);
        }
    }

    public static void prime(ModelPart root, Entity e, float limb, float limbAmt, float age, float yaw, float pitch) {
        float raise = windup(e);
        ModelPart head = root.getChild("head");
        head.yRot = yaw * Mth.DEG_TO_RAD;
        head.xRot = pitch * Mth.DEG_TO_RAD;
        for (String n : new String[] {"hood", "crown", "face"}) {
            root.getChild(n).yRot = head.yRot;
            root.getChild(n).xRot = head.xRot;
        }
        root.getChild("arm_l").xRot = -0.25F + Mth.sin(age * 0.07F) * 0.05F - raise * 2.4F;
        root.getChild("arm_r").xRot = -0.25F - Mth.sin(age * 0.07F) * 0.05F - raise * 2.4F;
        root.getChild("robe_lower").zRot = Mth.sin(age * 0.05F) * 0.03F;
        boolean sealed = e instanceof ArchivistPrime p && p.isSealed();
        float speed = (sealed ? 0.12F : 0.05F) + raise * 0.1F;
        String[] pages = {"page_a", "page_b", "page_c", "page_d", "page_e", "page_f"};
        for (int i = 0; i < pages.length; i++) {
            float a = age * speed + i * Mth.TWO_PI / pages.length;
            orbit(root.getChild(pages[i]), a, 14.0F - raise * 4.0F, Mth.sin(age * 0.09F + i * 1.3F) * 3.0F - 4.0F);
        }
    }

    public static void sealNode(ModelPart root, Entity e, float limb, float limbAmt, float age, float yaw, float pitch) {
        ModelPart crystal = root.getChild("crystal");
        crystal.yRot = age * 0.06F;
        crystal.y += Mth.sin(age * 0.1F) * 1.2F;
        ModelPart ring = root.getChild("ring");
        ring.yRot = -age * 0.09F;
        ring.y += Mth.sin(age * 0.1F) * 1.2F;
    }

    public static void indexNode(ModelPart root, Entity e, float limb, float limbAmt, float age, float yaw, float pitch) {
        root.getChild("core").yRot = age * 0.08F;
        root.getChild("core").y += Mth.sin(age * 0.1F) * 1.0F;
        String[] pages = {"page_a", "page_b", "page_c"};
        for (int i = 0; i < pages.length; i++) {
            orbit(root.getChild(pages[i]), age * 0.07F + i * Mth.TWO_PI / 3.0F, 8.0F, Mth.sin(age * 0.1F + i) * 1.5F);
        }
    }
}
