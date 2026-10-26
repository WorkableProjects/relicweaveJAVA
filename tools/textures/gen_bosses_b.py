"""T12 textures: Gravity Maw and Starforged Colossus (entity/<id>.png plus the Colossus heat glow overlay).

The UV layout mirrors the Java models exactly (shelf packing, see client/boss/b/TexLayout.java): the SPECS box order
here must match client/boss/b/GravityMawModel.java and StarforgedColossusModel.java. Re-run to regenerate; seeded.
"""
import random
import sys
import pathlib

sys.path.insert(0, str(pathlib.Path(__file__).parent))
from vestige_palette import *  # noqa: F401,F403
from gen_beast_mobs import layout, faces, fill, paint_box, px, hline, speckle  # noqa: E402
from PIL import Image  # noqa: E402

# Rock boxes (w, h, d) in the same order as GravityMawModel.ROCKS.
ROCKS = [(5, 5, 5), (4, 6, 4), (6, 4, 5), (5, 5, 5), (4, 4, 6), (6, 6, 4)]

SPECS = {
    "gravity_maw": (128, 64, [("upper", 16, 8, 16), ("lower", 16, 6, 16), ("tooth_u", 14, 2, 2), ("tooth_l", 14, 2, 2),
                              ("back", 12, 12, 8), ("eye", 4, 4, 2)]
                    + [(f"rock{i}", w, h, d) for i, (w, h, d) in enumerate(ROCKS)]),
    "starforged_colossus": (256, 128, [("legR", 12, 28, 12), ("legL", 12, 28, 12), ("torso", 30, 32, 18),
                                       ("head", 16, 16, 16), ("armR", 10, 30, 10), ("armL", 10, 30, 10),
                                       ("shoulderR", 14, 6, 14), ("shoulderL", 14, 6, 14), ("furnace", 12, 10, 2),
                                       ("ventR", 4, 10, 4), ("ventL", 4, 10, 4), ("visor", 12, 3, 1)]),
}


def boxes_with_uv(name):
    tex_w, tex_h, boxes = SPECS[name]
    uv = layout(tex_w, tex_h, [(w, h, d) for _, w, h, d in boxes])
    return tex_w, tex_h, {b[0]: (u, v, b[1], b[2], b[3]) for b, (u, v) in zip(boxes, uv)}


def front(rect):
    u, v, w, h, d = rect
    return faces(u, v, w, h, d)["front"]


def maw():
    tex_w, tex_h, rects = boxes_with_uv("gravity_maw")
    img = Image.new("RGBA", (tex_w, tex_h), CLEAR)
    rng = random.Random(1201)
    for name, (u, v, w, h, d) in rects.items():
        if name.startswith("rock"):
            paint_box(img, u, v, w, h, d, INK, rng, 1, 3)
            speckle(img, (u, v, 2 * (d + w), d + h), VIOLET[2], 4, rng)
        elif name.startswith("tooth"):
            paint_box(img, u, v, w, h, d, IVORY, rng, 2, 4)
        elif name == "eye":
            paint_box(img, u, v, w, h, d, TEAL, rng, 3, 4)
            fx, fy, fw, fh = front(rects["eye"])
            for x in range(fx + 1, fx + fw - 1):
                for y in range(fy + 1, fy + fh - 1):
                    px(img, x, y, OPAL[4])
        elif name == "back":
            paint_box(img, u, v, w, h, d, VIOLET, rng, 0, 2)
        else:
            paint_box(img, u, v, w, h, d, VIOLET, rng, 1, 3)
            # Fractured gold veins along the jaws and a dark inner mouth on the bottom/top faces.
            fx, fy, fw, fh = front(rects[name])
            for i in range(0, fw, 3):
                px(img, fx + i, fy + rng.randrange(fh), GOLD[3])
            speckle(img, (u, v, 2 * (d + w), d + h), TEAL[3], 6, rng)
    inner = faces(*rects["upper"])["bottom"]
    fill(img, inner, INK, rng, 0, 0)
    inner = faces(*rects["lower"])["top"]
    fill(img, inner, INK, rng, 0, 0)
    save(img, "entity/gravity_maw")


def colossus():
    tex_w, tex_h, rects = boxes_with_uv("starforged_colossus")
    img = Image.new("RGBA", (tex_w, tex_h), CLEAR)
    glow = Image.new("RGBA", (tex_w, tex_h), CLEAR)
    rng = random.Random(1202)
    for name, (u, v, w, h, d) in rects.items():
        if name.startswith("shoulder"):
            paint_box(img, u, v, w, h, d, GOLD, rng, 1, 3)
        elif name.startswith("vent"):
            paint_box(img, u, v, w, h, d, INK, rng, 0, 2)
            top = faces(u, v, w, h, d)["top"]
            fill(img, top, EMBER, rng, 2, 3)
            fill(glow, top, EMBER, rng, 3, 4)
        elif name == "furnace":
            paint_box(img, u, v, w, h, d, EMBER, rng, 1, 3)
            for part in faces(u, v, w, h, d).values():
                fill(glow, part, EMBER, rng, 3, 4)
        elif name == "visor":
            paint_box(img, u, v, w, h, d, EMBER, rng, 3, 4)
            for part in faces(u, v, w, h, d).values():
                fill(glow, part, EMBER, rng, 4, 4)
        else:
            ramp = INK if name in ("torso", "head") else IVORY if name.startswith("arm") else INK
            paint_box(img, u, v, w, h, d, ramp, rng, 1, 3)
            if name == "head":
                paint_box(img, u, v, w, h, d, INK, rng, 0, 2)
            # Rivet rows and heat veins on the broad faces.
            fx, fy, fw, fh = front(rects[name])
            for y in range(fy + 2, fy + fh, 6):
                for x in range(fx + 2, fx + fw, 5):
                    px(img, x, y, GOLD[2])
            vein_x = fx + fw // 2
            for y in range(fy, fy + fh):
                x = vein_x + int(2 * ((y * 7) % 3 - 1))
                if name in ("torso", "legR", "legL", "armR", "armL") and (y % 5) != 0:
                    px(img, x, y, EMBER[2])
                    px(glow, x, y, EMBER[3])
    save(img, "entity/starforged_colossus")
    save(glow, "entity/starforged_colossus_glow")


def main():
    maw()
    colossus()
    print("wrote 3 boss textures")


if __name__ == "__main__":
    main()
