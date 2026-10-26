"""T07 textures: beast and creature Vestige mob skins (entity/<id>.png).

The UV layout mirrors the Java model exactly: boxes are packed left-to-right, top-to-bottom ("shelf" packing) in
declaration order, each box taking a (2*(d+w)) x (d+h) rectangle, standard Minecraft cube unwrapping. The box order is
head, leg, body, extras for the quadruped mobs and base, lid, lower teeth, upper teeth, keyhole for the Archive Mimic.
Keep the SPECS table in sync with client/entity/beast/BeastModels.java.
"""
import random
import sys
import pathlib

sys.path.insert(0, str(pathlib.Path(__file__).parent))
from vestige_palette import *  # noqa: F401,F403


def layout(tex_w, tex_h, dims):
    """Shelf-pack (w, h, d) boxes; returns [(u, v)] and asserts everything fits."""
    x = y = row = 0
    out = []
    for w, h, d in dims:
        bw, bh = 2 * (d + w), d + h
        if x + bw > tex_w:
            x, y, row = 0, y + row, 0
        out.append((x, y))
        x += bw
        row = max(row, bh)
    assert y + row <= tex_h, f"layout overflow {y + row} > {tex_h}"
    return out


def faces(u, v, w, h, d):
    return {
        "top": (u + d, v, w, d), "bottom": (u + d + w, v, w, d),
        "right": (u, v + d, d, h), "front": (u + d, v + d, w, h),
        "left": (u + d + w, v + d, d, h), "back": (u + 2 * d + w, v + d, w, h),
    }


def fill(img, rect, ramp, rng, lo=1, hi=3):
    x0, y0, w, h = rect
    for y in range(y0, y0 + h):
        for x in range(x0, x0 + w):
            img.putpixel((x, y), rgba(ramp[rng.randint(lo, hi)]))


def paint_box(img, u, v, w, h, d, ramp, rng, lo=1, hi=3):
    for rect in faces(u, v, w, h, d).values():
        fill(img, rect, ramp, rng, lo, hi)
    # Shade the underside and darken the bottom face.
    fill(img, faces(u, v, w, h, d)["bottom"], ramp, rng, 0, 1)


def px(img, x, y, color):
    if 0 <= x < img.width and 0 <= y < img.height:
        img.putpixel((x, y), rgba(color))


def eyes(img, u, v, w, h, d, color):
    fx, fy, fw, fh = faces(u, v, w, h, d)["front"]
    ey = fy + max(1, fh // 3)
    px(img, fx + 1, ey, color)
    px(img, fx + fw - 2, ey, color)


def speckle(img, rect, color, count, rng):
    x0, y0, w, h = rect
    for _ in range(count):
        px(img, x0 + rng.randrange(w), y0 + rng.randrange(h), color)


def hline(img, x0, y, n, color):
    for i in range(n):
        px(img, x0 + i, y, color)


# name -> (texW, texH, [(box name, w, h, d)]) in layout order.
SPECS = {
    "glassroot_stalker": (64, 64, [("head", 8, 8, 8), ("leg", 3, 12, 3), ("body", 8, 8, 16),
                                   ("spine1", 2, 6, 2), ("spine2", 2, 8, 2), ("spine3", 2, 6, 2), ("horn", 2, 5, 2)]),
    "ruin_hound": (64, 64, [("head", 6, 6, 6), ("leg", 3, 8, 3), ("body", 6, 7, 11),
                            ("snout", 3, 3, 4), ("ear1", 2, 3, 1), ("ear2", 2, 3, 1), ("tail", 2, 2, 7)]),
    "aether_grazer": (64, 64, [("head", 7, 7, 7), ("leg", 3, 7, 3), ("body", 10, 9, 12),
                               ("horn1", 2, 3, 2), ("horn2", 2, 3, 2), ("fleece", 12, 10, 14)]),
    "shardback_brute": (128, 64, [("head", 10, 10, 10), ("leg", 5, 12, 5), ("body", 14, 14, 20),
                                  ("horn1", 2, 6, 2), ("horn2", 2, 6, 2),
                                  ("plate1", 10, 3, 4), ("plate2", 10, 3, 4), ("plate3", 10, 3, 4)]),
    "starfall_strider": (64, 64, [("head", 6, 6, 7), ("leg", 3, 13, 3), ("body", 7, 8, 14),
                                  ("crest", 2, 5, 6), ("tail", 2, 2, 9), ("fin1", 1, 4, 3), ("fin2", 1, 4, 3)]),
    "gravity_leech": (64, 64, [("head", 8, 6, 6), ("leg", 2, 3, 2), ("body", 8, 6, 18),
                               ("sucker", 6, 1, 6), ("tendril1", 1, 1, 8), ("tendril2", 1, 1, 8)]),
    "archive_mimic": (64, 64, [("base", 16, 10, 16), ("lid", 16, 6, 16), ("teeth_low", 14, 2, 2),
                               ("teeth_up", 14, 2, 2), ("keyhole", 2, 3, 1)]),
}

# Per box: (ramp, lo, hi). Anything not listed uses the mob default.
STYLES = {
    "glassroot_stalker": {"default": (OPAL, 1, 3), "head": (OPAL, 2, 4), "leg": (TEAL, 1, 3), "body": (OPAL, 1, 3),
                          "spine1": (TEAL, 3, 4), "spine2": (TEAL, 3, 4), "spine3": (TEAL, 3, 4), "horn": (TEAL, 3, 4)},
    "ruin_hound": {"default": (INK, 0, 2), "head": (IVORY, 1, 3), "leg": (INK, 1, 2), "body": (INK, 1, 2),
                   "snout": (INK, 0, 1), "ear1": (INK, 0, 1), "ear2": (INK, 0, 1), "tail": (INK, 1, 2)},
    "aether_grazer": {"default": (IVORY, 1, 3), "head": (IVORY, 2, 3), "leg": (IVORY, 0, 2), "body": (IVORY, 1, 3),
                      "horn1": (GOLD, 2, 3), "horn2": (GOLD, 2, 3), "fleece": (OPAL, 2, 4)},
    "shardback_brute": {"default": (INK, 0, 2), "head": (INK, 1, 2), "leg": (INK, 0, 2), "body": (INK, 0, 2),
                        "horn1": (IVORY, 2, 4), "horn2": (IVORY, 2, 4),
                        "plate1": (VIOLET, 2, 4), "plate2": (VIOLET, 2, 4), "plate3": (VIOLET, 2, 4)},
    "starfall_strider": {"default": (INK, 0, 1), "head": (INK, 1, 2), "leg": (INK, 0, 1), "body": (INK, 0, 2),
                         "crest": (EMBER, 2, 4), "tail": (EMBER, 1, 3), "fin1": (EMBER, 2, 4), "fin2": (EMBER, 2, 4)},
    "gravity_leech": {"default": (VIOLET, 0, 2), "head": (VIOLET, 1, 3), "leg": (VIOLET, 1, 2), "body": (VIOLET, 0, 2),
                      "sucker": (VIOLET, 3, 4), "tendril1": (VIOLET, 2, 3), "tendril2": (VIOLET, 2, 3)},
    "archive_mimic": {"default": (EMBER, 0, 2), "base": (EMBER, 0, 2), "lid": (EMBER, 1, 2),
                      "teeth_low": (OPAL, 3, 4), "teeth_up": (OPAL, 3, 4), "keyhole": (VIOLET, 2, 3)},
}


def accents(name, img, rects, rng):
    """Mob-specific details painted over the base fill. rects: box name -> (u, v, w, h, d)."""
    if name == "glassroot_stalker":
        eyes(img, *rects["head"], VIOLET[4])
        bf = faces(*rects["body"])
        speckle(img, bf["right"], TEAL[3], 6, rng)
        speckle(img, bf["left"], TEAL[3], 6, rng)
        speckle(img, bf["top"], OPAL[4], 8, rng)
    elif name == "ruin_hound":
        eyes(img, *rects["head"], GOLD[4])
        bf = faces(*rects["body"])
        for rect in (bf["right"], bf["left"]):
            for i in range(1, 5):          # ribs showing through
                hline(img, rect[0] + 1, rect[1] + i, rect[2] - 2, IVORY[2 if i % 2 else 1])
        lf = faces(*rects["leg"])
        fill(img, (lf["front"][0], lf["front"][1] + lf["front"][3] - 2, lf["front"][2], 2), IVORY, rng, 2, 3)
        hf = faces(*rects["head"])
        hline(img, hf["front"][0], hf["front"][1] + hf["front"][3] - 1, hf["front"][2], IVORY[4])
    elif name == "aether_grazer":
        eyes(img, *rects["head"], INK[0])
        ff = faces(*rects["fleece"])
        speckle(img, ff["top"], OPAL[4], 14, rng)
        speckle(img, ff["right"], OPAL[0], 8, rng)
        speckle(img, ff["left"], OPAL[0], 8, rng)
    elif name == "shardback_brute":
        eyes(img, *rects["head"], EMBER[4])
        bf = faces(*rects["body"])
        speckle(img, bf["top"], VIOLET[3], 14, rng)
        for _ in range(3):                  # glowing fractures
            x = bf["right"][0] + rng.randrange(bf["right"][2] - 6)
            y = bf["right"][1] + rng.randrange(bf["right"][3] - 3)
            hline(img, x, y, 5, VIOLET[4])
    elif name == "starfall_strider":
        eyes(img, *rects["head"], EMBER[4])
        bf = faces(*rects["body"])
        for rect in (bf["right"], bf["left"]):
            for i in range(2, rect[3], 3):
                hline(img, rect[0] + rng.randrange(2), rect[1] + i, rect[2] - 3, EMBER[3])
        lf = faces(*rects["leg"])
        fill(img, (lf["front"][0], lf["front"][1] + lf["front"][3] - 3, lf["front"][2], 3), EMBER, rng, 2, 3)
    elif name == "gravity_leech":
        eyes(img, *rects["head"], OPAL[4])
        hf = faces(*rects["head"])
        hline(img, hf["front"][0] + 1, hf["front"][1] + hf["front"][3] - 2, hf["front"][2] - 2, BLACK)
        bf = faces(*rects["body"])
        speckle(img, bf["top"], INK[2], 16, rng)
        speckle(img, bf["top"], VIOLET[4], 5, rng)
    elif name == "archive_mimic":
        u, v, w, h, d = rects["base"]
        bf = faces(u, v, w, h, d)
        for key in ("front", "back", "right", "left"):
            x0, y0, fw, fh = bf[key]
            for x in range(x0 + 3, x0 + fw, 4):          # plank gaps
                for y in range(y0, y0 + fh):
                    px(img, x, y, EMBER[0])
            hline(img, x0, y0, fw, GOLD[2])                # gold bands top and bottom
            hline(img, x0, y0 + fh - 1, fw, GOLD[1])
            for x in (x0, x0 + fw - 1):
                for y in range(y0, y0 + fh):
                    px(img, x, y, GOLD[1])
        lu, lv, lw, lh, ld = rects["lid"]
        lf = faces(lu, lv, lw, lh, ld)
        for key in ("front", "back", "right", "left"):
            x0, y0, fw, fh = lf[key]
            hline(img, x0, y0 + fh - 1, fw, BLACK)         # the visible seam where lid meets base
            hline(img, x0, y0, fw, GOLD[2])
            for x in range(x0 + 3, x0 + fw, 4):
                for y in range(y0 + 1, y0 + fh - 1):
                    px(img, x, y, EMBER[0])
        fill(img, lf["top"], EMBER, rng, 1, 2)
        for i in range(0, lf["top"][2], 4):
            for y in range(lf["top"][1], lf["top"][1] + lf["top"][3]):
                px(img, lf["top"][0] + i, y, EMBER[0])
        # Faint violet keyhole glow around the lock on the front face.
        fx, fy, fw, fh = bf["front"]
        px(img, fx + fw // 2 - 1, fy + 1, VIOLET[1])
        px(img, fx + fw // 2, fy + 1, VIOLET[1])
        ku, kv, kw, kh, kd = rects["keyhole"]
        fill(img, faces(ku, kv, kw, kh, kd)["front"], VIOLET, rng, 3, 4)


def build(name):
    tex_w, tex_h, boxes = SPECS[name]
    uv = layout(tex_w, tex_h, [(w, h, d) for _, w, h, d in boxes])
    img = Image_new(tex_w, tex_h)
    rng = random.Random(7000 + sum(map(ord, name)))
    styles = STYLES[name]
    rects = {}
    for (bname, w, h, d), (u, v) in zip(boxes, uv):
        ramp, lo, hi = styles.get(bname, styles["default"])
        paint_box(img, u, v, w, h, d, ramp, rng, lo, hi)
        rects[bname] = (u, v, w, h, d)
    accents(name, img, rects, rng)
    save(img, "entity/" + name)


def Image_new(w, h):
    from PIL import Image
    return Image.new("RGBA", (w, h), CLEAR)


def main():
    for name in SPECS:
        build(name)
    print("wrote", len(SPECS), "entity textures")


if __name__ == "__main__":
    main()
