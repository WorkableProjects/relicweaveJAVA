#!/usr/bin/env python3
"""Generates the entity textures and the UV layout class for the T13 bosses (Warden of Seals, Archivist-Prime).

Single source of truth: the MOBS part tables below (same scheme as gen_ethereal_mobs.py). Running this script
  * packs every part's box-UV region into the model's texture (shelf packing, deterministic),
  * paints the regions from the palette ramps (seeded randomness only),
  * writes <id>.png (+ <id>_glow.png holding only the glowing parts, rendered full-bright),
  * writes entity/index_page.png (the Glyph Volley projectile),
  * writes src/main/java/com/relicweave/vestige/client/boss/c/BossCLayouts.java so the Java models use exactly the
    same offsets and sizes (it reuses the EtherealLayouts.Layout/Part records).

Part tuple: (name, (w, h, d), (ox, oy, oz), (px, py, pz), style) with style = (ramp, lo, hi) or
(ramp, lo, hi, 'glow') or (ramp, lo, hi, 'glow', (base_ramp, blo, bhi)); the last form paints a dim version of the
glowing part into the base texture (so a node looks dormant until its glow layer is drawn).
Model space is Minecraft's: y = 24 is the ground, y grows downwards.
"""
import sys
import pathlib

sys.path.insert(0, str(pathlib.Path(__file__).parent))
from vestige_palette import *  # noqa: E402,F401
from PIL import Image  # noqa: E402
import random  # noqa: E402

JAVA_OUT = ROOT / "src/main/java/com/relicweave/vestige/client/boss/c/BossCLayouts.java"

MOBS = {
    "warden_of_seals": ((128, 64), [
        ("torso", (10, 12, 6), (-5, -12, -3), (0, 12, 0), (INK, 1, 3)),
        ("head", (8, 8, 8), (-4, -8, -4), (0, 0, 0), (INK, 2, 3)),
        ("crest", (2, 5, 9), (-1, -13, -4.5), (0, 0, 0), (GOLD, 2, 4)),
        ("pauldron_l", (6, 4, 6), (-3, -2, -3), (7, 1, 0), (GOLD, 1, 3)),
        ("pauldron_r", (6, 4, 6), (-3, -2, -3), (-7, 1, 0), (GOLD, 1, 3)),
        ("arm_l", (4, 12, 4), (-2, 0, -2), (7, 2, 0), (INK, 1, 2)),
        ("arm_r", (4, 12, 4), (-2, 0, -2), (-7, 2, 0), (INK, 1, 2)),
        ("leg_l", (4, 12, 4), (-2, 0, -2), (2.5, 12, 0), (INK, 0, 2)),
        ("leg_r", (4, 12, 4), (-2, 0, -2), (-2.5, 12, 0), (INK, 0, 2)),
        ("tabard", (6, 9, 1), (-3, -10, -4), (0, 12, 0), (IVORY, 2, 4)),
        ("chest_rune", (4, 4, 1), (-2, -8, -5), (0, 12, 0), (TEAL, 3, 4, "glow")),
        ("eyes", (6, 1, 1), (-3, -4, -4.5), (0, 0, 0), (TEAL, 4, 4, "glow")),
        ("halo_a", (4, 6, 1), (-2, -3, -0.5), (0, 6, 0), (TEAL, 3, 4, "glow")),
        ("halo_b", (4, 6, 1), (-2, -3, -0.5), (0, 6, 0), (TEAL, 3, 4, "glow")),
        ("halo_c", (4, 6, 1), (-2, -3, -0.5), (0, 6, 0), (TEAL, 3, 4, "glow")),
        ("halo_d", (4, 6, 1), (-2, -3, -0.5), (0, 6, 0), (TEAL, 3, 4, "glow")),
    ]),
    "archivist_prime": ((128, 64), [
        ("robe_lower", (10, 12, 6), (-5, 0, -3), (0, 12, 0), (VIOLET, 0, 2)),
        ("robe_upper", (8, 12, 5), (-4, -12, -2.5), (0, 12, 0), (VIOLET, 1, 3)),
        ("collar", (12, 3, 8), (-6, -13, -4), (0, 12, 0), (GOLD, 2, 4)),
        ("head", (8, 8, 8), (-4, -8, -4), (0, 0, 0), (IVORY, 2, 3)),
        ("hood", (10, 3, 10), (-5, -9, -5), (0, 0, 0), (INK, 0, 2)),
        ("crown", (12, 2, 12), (-6, -11, -6), (0, 0, 0), (GOLD, 3, 4)),
        ("face", (4, 2, 1), (-2, -5, -4.5), (0, 0, 0), (OPAL, 4, 4, "glow")),
        ("arm_l", (3, 13, 3), (-1.5, 0, -1.5), (6.5, 1, 0), (VIOLET, 1, 2)),
        ("arm_r", (3, 13, 3), (-1.5, 0, -1.5), (-6.5, 1, 0), (VIOLET, 1, 2)),
        ("page_a", (4, 6, 1), (-2, -3, -0.5), (0, 8, 0), (OPAL, 3, 4, "glow")),
        ("page_b", (4, 6, 1), (-2, -3, -0.5), (0, 8, 0), (OPAL, 3, 4, "glow")),
        ("page_c", (4, 6, 1), (-2, -3, -0.5), (0, 8, 0), (GOLD, 3, 4, "glow")),
        ("page_d", (4, 6, 1), (-2, -3, -0.5), (0, 8, 0), (OPAL, 3, 4, "glow")),
        ("page_e", (4, 6, 1), (-2, -3, -0.5), (0, 8, 0), (OPAL, 3, 4, "glow")),
        ("page_f", (4, 6, 1), (-2, -3, -0.5), (0, 8, 0), (GOLD, 3, 4, "glow")),
    ]),
    "seal_node": ((64, 64), [
        ("pedestal", (10, 6, 10), (-5, -6, -5), (0, 24, 0), (IVORY, 1, 3)),
        ("column", (4, 6, 4), (-2, -12, -2), (0, 24, 0), (GOLD, 1, 3)),
        ("crystal", (6, 12, 6), (-3, -6, -3), (0, 10, 0), (TEAL, 3, 4, "glow", (INK, 0, 1))),
        ("ring", (12, 1, 12), (-6, -0.5, -6), (0, 10, 0), (GOLD, 3, 4, "glow", (GOLD, 0, 1))),
    ]),
    "index_node": ((64, 64), [
        ("pedestal", (8, 4, 8), (-4, -4, -4), (0, 24, 0), (INK, 1, 3)),
        ("spire", (3, 12, 3), (-1.5, -14, -1.5), (0, 24, 0), (GOLD, 1, 3)),
        ("core", (5, 5, 5), (-2.5, -2.5, -2.5), (0, 8, 0), (VIOLET, 3, 4, "glow")),
        ("page_a", (4, 6, 1), (-2, -3, -0.5), (0, 8, 0), (OPAL, 3, 4, "glow")),
        ("page_b", (4, 6, 1), (-2, -3, -0.5), (0, 8, 0), (OPAL, 3, 4, "glow")),
        ("page_c", (4, 6, 1), (-2, -3, -0.5), (0, 8, 0), (GOLD, 3, 4, "glow")),
    ]),
}


def region(w, h, d):
    return int(2 * d + 2 * w + 0.999), int(d + h + 0.999)


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


def paint_part(img, u, v, size, ramp, lo, hi, glow, seed):
    w, h, d = size
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


def hash_seed(mob, name):
    return sum((i + 1) * ord(c) for i, c in enumerate(mob + ":" + name))


def build(mob, spec):
    tex, parts = spec
    img = Image.new("RGBA", tex, CLEAR)
    glow_img = Image.new("RGBA", tex, CLEAR)
    placed = {n: (u, v) for n, u, v in pack(tex, parts)}
    for name, size, off, piv, style in parts:
        u, v = placed[name]
        seed = hash_seed(mob, name)
        ramp, lo, hi = style[0], style[1], style[2]
        glow = len(style) > 3
        if glow and len(style) > 4:
            base_ramp, blo, bhi = style[4]
            paint_part(img, u, v, size, base_ramp, blo, bhi, False, seed)
        else:
            paint_part(img, u, v, size, ramp, lo, hi, glow, seed)
        if glow:
            paint_part(glow_img, u, v, size, ramp, lo, hi, True, seed)
    return img, glow_img


def num(v):
    return ("%g" % v) + "f"


def java():
    lines = [
        "package com.relicweave.vestige.client.boss.c;",
        "",
        "import com.relicweave.vestige.client.entity.ethereal.EtherealLayouts.Layout;",
        "import com.relicweave.vestige.client.entity.ethereal.EtherealLayouts.Part;",
        "",
        "/** GENERATED by tools/textures/gen_bosses_c.py; do not edit by hand. Box-UV layout of every T13 model. */",
        "public final class BossCLayouts {",
        "    private BossCLayouts() {}",
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


def index_page():
    """16x16 billboard: a gold-edged ivory page with teal glyph lines."""
    img = new(16)
    rng = random.Random(1301)
    for x in range(3, 13):
        for y in range(1, 15):
            edge = x in (3, 12) or y in (1, 14)
            img.putpixel((x, y), rgba(GOLD[3] if edge else IVORY[rng.randint(3, 4)]))
    for y in (4, 6, 8, 10):
        for x in range(5, 11 - (y % 4 == 0) * 2):
            img.putpixel((x, y), rgba(TEAL[3] if (x + y) % 3 else TEAL[4]))
    save(img, "entity/index_page")


def main():
    for mob, spec in MOBS.items():
        img, glow = build(mob, spec)
        save(img, "entity/" + mob)
        if glow.getbbox():
            save(glow, "entity/" + mob + "_glow")
    index_page()
    java()


if __name__ == "__main__":
    main()
