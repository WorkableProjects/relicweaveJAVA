package com.relicweave.vestige.client.entity.beast;

import com.relicweave.RelicWeave;
import com.relicweave.vestige.client.entity.beast.VestigeQuadrupedModel.Extra;
import com.relicweave.vestige.client.entity.beast.VestigeQuadrupedModel.PoseHook;
import com.relicweave.vestige.client.entity.beast.VestigeQuadrupedModel.Spec;
import com.relicweave.vestige.entity.beast.AetherGrazer;
import com.relicweave.vestige.entity.beast.GlassrootStalker;
import com.relicweave.vestige.entity.beast.GravityLeech;
import com.relicweave.vestige.entity.beast.RuinHound;
import com.relicweave.vestige.entity.beast.ShardbackBrute;
import com.relicweave.vestige.entity.beast.StarfallStrider;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

import java.util.List;

/**
 * Model layer locations, proportions and pose hooks of the beast mobs. Box dimensions here must match
 * {@code SPECS} in {@code tools/textures/gen_beast_mobs.py} (same order: head, leg, body, extras).
 */
final class BeastModels {
    static final ModelLayerLocation STALKER = layer("glassroot_stalker");
    static final ModelLayerLocation HOUND = layer("ruin_hound");
    static final ModelLayerLocation GRAZER = layer("aether_grazer");
    static final ModelLayerLocation BRUTE = layer("shardback_brute");
    static final ModelLayerLocation STRIDER = layer("starfall_strider");
    static final ModelLayerLocation LEECH = layer("gravity_leech");
    static final ModelLayerLocation MIMIC = layer("archive_mimic");

    // Lean ambusher with crystal spines along its back and a crystal horn.
    static final Spec STALKER_SPEC = new Spec(64, 64, 8, 8, 16, 8, 8, 8, 3, 12, 3, 2, List.of(
            new Extra("spine1", false, 2, 6, 2, -1, -6, -5),
            new Extra("spine2", false, 2, 8, 2, -1, -8, -1),
            new Extra("spine3", false, 2, 6, 2, -1, -6, 3),
            new Extra("horn", true, 2, 5, 2, -1, -9, -5)));

    // Low, long pack hunter with a snout, ears and a tail.
    static final Spec HOUND_SPEC = new Spec(64, 64, 6, 7, 11, 6, 6, 6, 3, 8, 3, 1, List.of(
            new Extra("snout", true, 3, 3, 4, -1.5F, 0, -10),
            new Extra("ear1", true, 2, 3, 1, -3, -6, -2),
            new Extra("ear2", true, 2, 3, 1, 1, -6, -2),
            new Extra("tail", false, 2, 2, 7, -1, 1, 5.5F)));

    // Round woolly grazer: the fleece is a slightly larger box that shrinks while regrowing.
    static final Spec GRAZER_SPEC = new Spec(64, 64, 10, 9, 12, 7, 7, 7, 3, 7, 3, 1, List.of(
            new Extra("horn1", true, 2, 3, 2, -4, -5, -3),
            new Extra("horn2", true, 2, 3, 2, 2, -5, -3),
            new Extra("fleece", false, 12, 10, 14, -6, -1, -7)));

    // Massive, wide and low-slung with horns and three crystal back plates.
    static final Spec BRUTE_SPEC = new Spec(128, 64, 14, 14, 20, 10, 10, 10, 5, 12, 5, 2, List.of(
            new Extra("horn1", true, 2, 6, 2, -6, -9, -6),
            new Extra("horn2", true, 2, 6, 2, 4, -9, -6),
            new Extra("plate1", false, 10, 3, 4, -5, -3, -8),
            new Extra("plate2", false, 10, 3, 4, -5, -3, -2),
            new Extra("plate3", false, 10, 3, 4, -5, -3, 4)));

    // Tall digitigrade runner with a flame crest, fins and an ember tail.
    static final Spec STRIDER_SPEC = new Spec(64, 64, 7, 8, 14, 6, 6, 7, 3, 13, 3, 2, List.of(
            new Extra("crest", true, 2, 5, 6, -1, -8, -5),
            new Extra("tail", false, 2, 2, 9, -1, 1, 7),
            new Extra("fin1", false, 1, 4, 3, -3, -4, -3),
            new Extra("fin2", false, 1, 4, 3, 2, -4, 2)));

    // Long slug body on stub legs with a suckered maw and two feeler tendrils.
    static final Spec LEECH_SPEC = new Spec(64, 64, 8, 6, 18, 8, 6, 6, 2, 3, 2, 1, List.of(
            new Extra("sucker", true, 6, 1, 6, -3, 3, -6),
            new Extra("tendril1", true, 1, 1, 8, -3, -1, -14),
            new Extra("tendril2", true, 1, 1, 8, 2, -1, -14)));

    private BeastModels() {}

    private static ModelLayerLocation layer(String id) {
        return new ModelLayerLocation(RelicWeave.id(id), "main");
    }

    // ---- Pose hooks ----

    static final PoseHook<GlassrootStalker> STALKER_POSE = (m, e, age) -> {
        switch (e.getState()) {
            case GlassrootStalker.LURK -> {
                m.alpha = 0.4F;                         // 60% translucent
                m.head.xRot += 0.35F;
            }
            case GlassrootStalker.REVEALING -> {
                m.alpha = 0.7F;
                float shiver = Mth.sin(age * 2.6F);
                m.body.x = shiver * 0.8F;
                m.head.x = -shiver * 0.8F;
                m.head.xRot += 0.2F;
            }
            default -> { }
        }
    };

    static final PoseHook<RuinHound> HOUND_POSE = (m, e, age) -> {
        if (e.getFleeTimer() > 0) {
            m.head.xRot += 0.4F;                        // tail-tucked, head low
        }
    };

    static final PoseHook<AetherGrazer> GRAZER_POSE = (m, e, age) -> {
        ModelPart fleece = m.extra("fleece");
        if (fleece != null) {
            float f = e.fleeceScale();                  // 0.4 (just sheared) .. 1.0 (full)
            float s = 0.8F + 0.2F * ((f - 0.4F) / 0.6F);
            fleece.xScale = s;
            fleece.yScale = s;
            fleece.zScale = s;
        }
    };

    static final PoseHook<ShardbackBrute> BRUTE_POSE = (m, e, age) -> {
        switch (e.getBrutePose()) {
            case ShardbackBrute.POSE_SLAM -> {          // rears up for the overhead slam
                m.body.xRot = -0.45F;
                m.head.xRot = -0.7F;
                m.legs[0].xRot = -2.0F;
                m.legs[1].xRot = -2.0F;
            }
            case ShardbackBrute.POSE_CHARGE -> {        // head down, back plates forward
                m.body.xRot = 0.15F;
                m.head.xRot = 0.6F;
            }
            case ShardbackBrute.POSE_EXPOSED -> {       // stunned: head hanging, back plates dim
                m.head.xRot = 1.0F;
                m.body.y += 1.0F;
                for (ModelPart leg : m.legs) {
                    leg.xRot = 0.0F;
                }
            }
            default -> { }
        }
    };

    static final PoseHook<StarfallStrider> STRIDER_POSE = (m, e, age) -> {
        if (e.isWindingUp()) {
            m.body.xRot = 0.35F;
            m.head.xRot += 0.5F;
        }
    };

    static final PoseHook<GravityLeech> LEECH_POSE = (m, e, age) -> {
        if (e.isPulling()) {
            m.head.xRot -= 0.5F;
            m.head.x = Mth.sin(age * 1.8F) * 0.6F;
        }
    };
}
