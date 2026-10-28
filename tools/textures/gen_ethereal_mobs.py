#!/usr/bin/env python3
"""Generates the entity textures and the UV layout class for the T08 ethereal Vestige mobs.

Single source of truth: the PART tables below. Running this script
  * packs every part's box-UV region into the mob's texture (shelf packing, deterministic),
  * paints the regions from the palette ramps (seeded randomness only),
  * writes <id>.png (+ <id>_glow.png holding only the glowing parts, rendered full-bright),
  * writes rune_warden_dormant.png (statue: glow parts painted as plain stone),
  * writes src/main/java/com/relicweave/vestige/client/entity/ethereal/EtherealLayouts.java so the Java models use
    exactly the same offsets and sizes.

Part tuple: (name, (w, h, d), (ox, oy, oz), (px, py, pz), style) with style = (ramp, lo, hi) or (ramp, lo, hi, 'glow').
Model space is Minecraft's: y = 24 is the ground, y grows downwards.
"""
import random
import sys
import pathlib

sys.path.insert(0, str(pathlib.Path(__file__).parent))
from vestige_palette import *  # noqa: E402,F401
from PIL import Image  # noqa: E402

JAVA_OUT = ROOT / "src/main/java/com/relicweave/vestige/client/entity/ethereal/EtherealLayouts.java"

MOBS = {
    "fracture_wisp": ((32, 32), [
        ("core", (4, 4, 4), (-2, -2, -2), (0, 16, 0), (VIOLET, 3, 4, "glow")),
        ("shard_a", (2, 4, 2), (3, -2, -1), (0, 16, 0), (OPAL, 2, 4)),
        ("shard_b", (2, 4, 2), (-5, -2, -1), (0, 16, 0), (OPAL, 1, 3)),
        ("shard_c", (2, 4, 2), (-1, -2, 3), (0, 16, 0), (OPAL, 2, 4)),
    ]),
    "nullwing": ((64, 64), [
        ("body", (6, 4, 10), (-3, -2, -5), (0, 18, 0), (INK, 0, 2)),
        ("head", (5, 4, 5), (-2.5, -2, -5), (0, 18, -5), (INK, 1, 3)),
        ("eyes", (4, 1, 1), (-2, -1.5, -6), (0, 18, -5), (VIOLET, 4, 4, "glow")),
        ("wing_l", (12, 1, 7), (0, 0, -3.5), (3, 17, -1), (VIOLET, 0, 2)),
        ("wing_r", (12, 1, 7), (-12, 0, -3.5), (-3, 17, -1), (VIOLET, 0, 2)),
        ("tail", (2, 2, 8), (-1, -1, 0), (0, 18, 5), (INK, 0, 1)),
    ]),
    "eclipse_moth": ((64, 64), [
        ("body", (2, 2, 5), (-1, -1, -2.5), (0, 20, 0), (IVORY, 1, 2)),
        ("head", (3, 3, 3), (-1.5, -1.5, -3), (0, 20, -2.5), (IVORY, 2, 3)),
        ("wing_l", (10, 1, 8), (0, 0, -4), (1, 19.5, 0), (OPAL, 2, 4)),
        ("wing_r", (10, 1, 8), (-10, 0, -4), (-1, 19.5, 0), (OPAL, 2, 4)),
        ("glow_l", (4, 1, 4), (3, -0.4, -2), (1, 19.5, 0), (GOLD, 3, 4, "glow")),
        ("glow_r", (4, 1, 4), (-7, -0.4, -2), (-1, 19.5, 0), (GOLD, 3, 4, "glow")),
    ]),
    "concordance_guardian": ((128, 64), [
        ("torso", (12, 14, 7), (-6, -14, -3.5), (0, 12, 0), (IVORY, 1, 3)),
        ("head", (8, 8, 8), (-4, -8, -4), (0, -2, 0), (IVORY, 2, 3)),
        ("arm_l", (5, 16, 5), (-2.5, -2, -2.5), (8.5, 0, 0), (IVORY, 1, 3)),
        ("arm_r", (5, 16, 5), (-2.5, -2, -2.5), (-8.5, 0, 0), (IVORY, 1, 3)),
        ("shoulder_l", (8, 3, 8), (-4, -1.5, -4), (8.5, -2, 0), (GOLD, 1, 3)),
        ("shoulder_r", (8, 3, 8), (-4, -1.5, -4), (-8.5, -2, 0), (GOLD, 1, 3)),
        ("leg_l", (5, 12, 5), (-2.5, 0, -2.5), (3, 12, 0), (IVORY, 0, 2)),
        ("leg_r", (5, 12, 5), (-2.5, 0, -2.5), (-3, 12, 0), (IVORY, 0, 2)),
        ("rune", (4, 4, 1), (-2, -2, -1), (0, 4, -3.5), (TEAL, 3, 4, "glow")),
        ("eyes", (6, 2, 1), (-3, -1, -1), (0, -6, -4), (TEAL, 4, 4, "glow")),
    ]),
    "sealed_custodian": ((128, 64), [
        ("torso", (11, 12, 6), (-5.5, -12, -3), (0, 15, 0), (TEAL, 0, 2)),
        ("head", (7, 7, 7), (-3.5, -7, -3.5), (0, 3, 0), (TEAL, 1, 3)),
        ("hood", (9, 3, 9), (-4.5, -2, -4.5), (0, -2, 0), (INK, 1, 2)),
        ("arm_l", (4, 12, 4), (-2, -1, -2), (7.5, 4, 0), (TEAL, 0, 2)),
        ("arm_r", (4, 12, 4), (-2, -1, -2), (-7.5, 4, 0), (TEAL, 0, 2)),
        ("leg_l", (5, 9, 5), (-2.5, 0, -2.5), (3, 15, 0), (INK, 1, 2)),
        ("leg_r", (5, 9, 5), (-2.5, 0, -2.5), (-3, 15, 0), (INK, 1, 2)),
        ("tablet", (8, 14, 2), (-4, -7, 0), (0, 9, 3), (GOLD, 1, 3)),
        ("keyhole", (3, 3, 1), (-1.5, -1.5, -1), (0, 9, -3), (GOLD, 4, 4, "glow")),
        ("eyes", (4, 1, 1), (-2, -0.5, -1), (0, 0, -3.5), (GOLD, 4, 4, "glow")),
    ]),
    "rune_warden": ((128, 64), [
        ("torso", (10, 12, 5), (-5, -12, -2.5), (0, 12, 0), (IVORY, 0, 2)),
        ("head", (7, 7, 7), (-3.5, -7, -3.5), (0, 0, 0), (IVORY, 1, 2)),
        ("crown", (8, 2, 8), (-4, -2, -4), (0, -7, 0), (GOLD, 2, 4)),
        ("arm_l", (4, 14, 4), (-2, -1, -2), (7, 1, 0), (IVORY, 0, 2)),
        ("arm_r", (4, 14, 4), (-2, -1, -2), (-7, 1, 0), (IVORY, 0, 2)),
        ("leg_l", (4, 12, 4), (-2, 0, -2), (2.5, 12, 0), (IVORY, 0, 1)),
        ("leg_r", (4, 12, 4), (-2, 0, -2), (-2.5, 12, 0), (IVORY, 0, 1)),
        ("rune_chest", (6, 6, 1), (-3, -3, -1), (0, 6, -2.5), (VIOLET, 3, 4, "glow")),
        ("rune_eyes", (5, 1, 1), (-2.5, -0.5, -1), (0, -3.5, -3.5), (VIOLET, 4, 4, "glow")),
        ("rune_arm_l", (1, 8, 1), (-0.5, 1, -3), (7, 0, 0), (VIOLET, 3, 4, "glow")),
        ("rune_arm_r", (1, 8, 1), (-0.5, 1, -3), (-7, 0, 0), (VIOLET, 3, 4, "glow")),
    ]),
    "cycle_herald": ((128, 64), [
        ("robe_lower", (10, 12, 6), (-5, 0, -3), (0, 12, 0), (VIOLET, 0, 2)),
        ("robe_upper", (8, 10, 5), (-4, -10, -2.5), (0, 12, 0), (VIOLET, 1, 2)),
        ("hood", (8, 8, 8), (-4, -8, -4), (0, 3, 0), (INK, 0, 1)),
        ("face", (4, 2, 1), (-2, -1, -1), (0, -1, -4), (OPAL, 4, 4, "glow")),
        ("arm_l", (3, 10, 3), (-1.5, -1, -1.5), (6.5, 4, 0), (VIOLET, 0, 2)),
        ("arm_r", (3, 10, 3), (-1.5, -1, -1.5), (-6.5, 4, 0), (VIOLET, 0, 2)),
        ("ring", (14, 1, 14), (-7, 0, -7), (0, 20, 0), (VIOLET, 3, 4, "glow")),
        ("ring_top", (10, 1, 10), (-5, 0, -5), (0, -8, 0), (OPAL, 3, 4, "glow")),
        ("lance", (2, 2, 14), (-1, -1, -12), (-6.5, 12, -2), (OPAL, 4, 4, "glow")),
    ]),
}


def region(w, h, d):
    return 2 * d + 2 * w, d + h


def pack(tex, parts):
    tw, th = tex
    x = y = row = 0
    out = []
    for name, (w, h, d), off, piv, style in parts:
        rw, rh = region(w, h, d)
        if x + rw > tw:
            x, y, row = 0, y + row, 0
        assert y + rh <= th and rw <= tw, "texture too small for " + name
        out.append((name, x, y))
        x += rw
        row = max(row, rh)
    return out


def paint_part(img, u, v, size, style, seed, stone=False):
    w, h, d = size
    ramp, lo, hi = style[0], style[1], style[2]
    glow = len(style) > 3
    if glow and stone:
        ramp, lo, hi, glow = IVORY, 0, 1, False
    rng = random.Random(seed)
    faces = [  # (x, y, w, h, shade)
        (u + d, v, w, d, 1), (u + d + w, v, w, d, -1),
        (u, v + d, d, h, 0), (u + d, v + d, w, h, 0), (u + d + w, v + d, d, h, 0), (u + 2 * d + w, v + d, w, h, -1),
    ]
    for fx, fy, fw, fh, shade in faces:
        for yy in range(fy, fy + fh):
            for xx in range(fx, fx + fw):
                idx = rng.randint(lo, hi) + (0 if glow else shade)
                edge = (xx in (fx, fx + fw - 1) or yy in (fy, fy + fh - 1)) and not glow and fw > 2 and fh > 2
                if edge:
                    idx -= 1
                idx = max(0, min(len(ramp) - 1, idx))
                img.putpixel((xx, yy), rgba(ramp[idx]))
    return glow


def build(mob, spec, stone=False):
    tex, parts = spec
    img = Image.new("RGBA", tex, CLEAR)
    glow_img = Image.new("RGBA", tex, CLEAR)
    placed = {n: (u, v) for n, u, v in pack(tex, parts)}
    for i, (name, size, off, piv, style) in enumerate(parts):
        u, v = placed[name]
        seed = hash_seed(mob, name)
        paint_part(img, u, v, size, style, seed, stone)
        if len(style) > 3 and not stone:
            paint_part(glow_img, u, v, size, style, seed)
    return img, glow_img


def hash_seed(mob, name):
    return sum((i + 1) * ord(c) for i, c in enumerate(mob + ":" + name))


def num(v):
    return ("%g" % v) + "f"


def java():
    lines = [
        "package com.relicweave.vestige.client.entity.ethereal;",
        "",
        "/** GENERATED by tools/textures/gen_ethereal_mobs.py; do not edit by hand. Box-UV layout of every T08 model. */",
        "public final class EtherealLayouts {",
        "    /** One cuboid: UV origin, box offset and size, and the pivot it rotates around. */",
        "    public record Part(String name, int u, int v, float x, float y, float z, int w, int h, int d, float px, float py, float pz) {}",
        "",
        "    /** All parts of one model plus its texture size. */",
        "    public record Layout(int texW, int texH, Part[] parts) {}",
        "",
        "    private EtherealLayouts() {}",
        "",
    ]
    for mob, (tex, parts) in MOBS.items():
        placed = {n: (u, v) for n, u, v in pack(tex, parts)}
        lines.append("    public static final Layout %s = new Layout(%d, %d, new Part[] {" % (mob.upper(), tex[0], tex[1]))
        for name, (w, h, d), (ox, oy, oz), (px, py, pz), style in parts:
            u, v = placed[name]
            lines.append('        new Part("%s", %d, %d, %s, %s, %s, %d, %d, %d, %s, %s, %s),'
                         % (name, u, v, num(ox), num(oy), num(oz), w, h, d, num(px), num(py), num(pz)))
        lines.append("    });")
        lines.append("")
    lines[-1] = "}"
    JAVA_OUT.parent.mkdir(parents=True, exist_ok=True)
    JAVA_OUT.write_text("\n".join(lines) + "\n")


def main():
    for mob, spec in MOBS.items():
        img, glow = build(mob, spec)
        save(img, "entity/" + mob)
        if glow.getbbox():
            save(glow, "entity/" + mob + "_glow")
    dormant, _ = build("rune_warden", MOBS["rune_warden"], stone=True)
    save(dormant, "entity/rune_warden_dormant")
    java()


if __name__ == "__main__":
    main()
