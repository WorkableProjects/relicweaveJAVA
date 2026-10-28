"""T11 textures: Thorned Regent, Choir Unheard and their helper entities (thorn_root, thorn_spike).

UV layout mirrors client/boss/a/*Model.java: boxes are shelf-packed left-to-right, top-to-bottom in declaration order,
each taking a 2*(d+w) x (d+h) rectangle (standard cube unwrapping). Keep the SPECS tables in sync with the BOXES tables
in ThornedRegentModel, ChoirUnheardModel and PropModels.
"""
import pathlib
import random
import sys

sys.path.insert(0, str(pathlib.Path(__file__).parent))
from vestige_palette import *  # noqa: F401,F403


def layout(tex_w, tex_h, dims):
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


def box(img, u, v, w, h, d, ramp, rng, lo=1, hi=3):
    f = faces(u, v, w, h, d)
    for rect in f.values():
        fill(img, rect, ramp, rng, lo, hi)
    fill(img, f["bottom"], ramp, rng, 0, 1)
    fill(img, f["top"], ramp, rng, hi, min(4, hi + 1))
    return f


def px(img, x, y, color):
    if 0 <= x < img.width and 0 <= y < img.height:
        img.putpixel((x, y), rgba(color))


def vlines(img, rect, color, step, rng, chance=0.8):
    x0, y0, w, h = rect
    for x in range(x0 + 1, x0 + w, step):
        for y in range(y0, y0 + h):
            if rng.random() < chance:
                px(img, x, y, color)


# ---- Thorned Regent: bark body, moss, gold crown, ember core ----
REGENT = [(6, 16, 6), (16, 20, 10), (10, 10, 10), (12, 2, 12), (2, 7, 2), (5, 20, 5), (6, 6, 2), (7, 5, 7)]


def regent():
    rng = random.Random(1101)
    img = new_rgba(128, 64)
    glow = new_rgba(128, 64)
    uv = layout(128, 64, REGENT)
    leg = box(img, *uv[0], *REGENT[0], MOSS, rng, 0, 2)
    body = box(img, *uv[1], *REGENT[1], MOSS, rng, 0, 2)
    head = box(img, *uv[2], *REGENT[2], MOSS, rng, 1, 3)
    for i in (3, 4):
        box(img, *uv[i], *REGENT[i], GOLD, rng, 1, 3)
    arm = box(img, *uv[5], *REGENT[5], MOSS, rng, 0, 2)
    pad = box(img, *uv[7], *REGENT[7], GOLD, rng, 0, 2)
    # Bark grain.
    for f in (leg["front"], body["front"], body["back"], arm["front"], arm["back"]):
        vlines(img, f, MOSS[0], 3, rng)
    # Leaf specks on the shoulder pads and head.
    for f in (pad["top"], head["top"], body["top"]):
        for _ in range(6):
            px(img, f[0] + rng.randrange(f[2]), f[1] + rng.randrange(f[3]), MOSS[4])
    # Face: two amber eyes and a dark mouth slit.
    hf = head["front"]
    for ex in (hf[0] + 2, hf[0] + hf[2] - 3):
        px(img, ex, hf[1] + 3, EMBER[4])
        px(img, ex + 1, hf[1] + 3, EMBER[3])
    for x in range(hf[0] + 3, hf[0] + hf[2] - 3):
        px(img, x, hf[1] + 7, MOSS[0])
    # Gold belt across the body.
    bf = body["front"]
    for x in range(bf[0], bf[0] + bf[2]):
        px(img, x, bf[1] + 12, GOLD[2])
        px(img, x, bf[1] + 13, GOLD[1])
    # Core: dull bark plate with an ember seam; the glow texture holds the lit version.
    core = faces(*uv[6], *REGENT[6])
    for name, rect in core.items():
        fill(img, rect, MOSS, rng, 0, 1)
        fill(glow, rect, EMBER, rng, 2, 4)
    cf = core["front"]
    for y in range(cf[1], cf[1] + cf[3]):
        px(img, cf[0] + cf[2] // 2, y, EMBER[1])
    for x in range(cf[0] + 1, cf[0] + cf[2] - 1):
        px(glow, x, cf[1] + cf[3] // 2, GOLD[4])
    save(img, "entity/thorned_regent")
    save(glow, "entity/thorned_regent_glow")


# ---- Choir Unheard: ink robe, teal veil, opal masks, violet halo ----
CHOIR = [(12, 30, 8), (8, 8, 8), (9, 10, 1), (3, 16, 3), (6, 8, 2), (10, 1, 10)]


def choir():
    rng = random.Random(1102)
    img = new_rgba(128, 64)
    uv = layout(128, 64, CHOIR)
    robe = box(img, *uv[0], *CHOIR[0], INK, rng, 0, 2)
    head = box(img, *uv[1], *CHOIR[1], OPAL, rng, 1, 3)
    veil = box(img, *uv[2], *CHOIR[2], TEAL, rng, 1, 3)
    arm = box(img, *uv[3], *CHOIR[3], INK, rng, 1, 3)
    mask = box(img, *uv[4], *CHOIR[4], OPAL, rng, 2, 4)
    halo = box(img, *uv[5], *CHOIR[5], VIOLET, rng, 3, 4)
    # Robe: teal trim at the hem and a vertical stole.
    rf = robe["front"]
    for x in range(rf[0], rf[0] + rf[2]):
        px(img, x, rf[1] + rf[3] - 1, TEAL[3])
        px(img, x, rf[1] + rf[3] - 2, TEAL[2])
    for y in range(rf[1], rf[1] + rf[3] - 2):
        px(img, rf[0] + rf[2] // 2, y, TEAL[2])
        px(img, rf[0] + rf[2] // 2 - 1, y, INK[3])
    for f in (robe["back"], robe["left"], robe["right"]):
        vlines(img, f, INK[0], 3, rng, 0.6)
    # Head: blank face behind the veil, painted eyes closed.
    hf = head["front"]
    px(img, hf[0] + 2, hf[1] + 4, INK[1])
    px(img, hf[0] + hf[2] - 3, hf[1] + 4, INK[1])
    # Veil: fine vertical threads.
    vlines(img, veil["front"], TEAL[4], 2, rng, 0.7)
    vlines(img, veil["back"], TEAL[0], 2, rng, 0.7)
    # Masks: dark eye holes and an open mouth (the "voice").
    mf = mask["front"]
    px(img, mf[0] + 1, mf[1] + 2, INK[0])
    px(img, mf[0] + mf[2] - 2, mf[1] + 2, INK[0])
    for y in range(mf[1] + 4, mf[1] + 7):
        px(img, mf[0] + 2, y, INK[1])
        px(img, mf[0] + 3, y, INK[1])
    # Arm cuffs.
    af = arm["front"]
    for x in range(af[0], af[0] + af[2]):
        px(img, x, af[1] + af[3] - 1, TEAL[3])
    save(img, "entity/choir_unheard")


# ---- Helpers ----
ROOT_BOXES = [(14, 4, 14), (10, 26, 10), (2, 10, 2)]


def thorn_root():
    rng = random.Random(1103)
    img = new_rgba(128, 64)
    uv = layout(128, 64, ROOT_BOXES)
    box(img, *uv[0], *ROOT_BOXES[0], MOSS, rng, 0, 1)
    trunk = box(img, *uv[1], *ROOT_BOXES[1], MOSS, rng, 0, 2)
    box(img, *uv[2], *ROOT_BOXES[2], GOLD, rng, 1, 3)
    for f in (trunk["front"], trunk["back"], trunk["left"], trunk["right"]):
        vlines(img, f, MOSS[0], 2, rng, 0.7)
        for _ in range(4):
            px(img, f[0] + rng.randrange(f[2]), f[1] + rng.randrange(f[3]), MOSS[4])
    save(img, "entity/thorn_root")


def thorn_spike():
    rng = random.Random(1104)
    img = new_rgba(32, 16)
    uv = layout(32, 16, [(12, 2, 2)])
    shaft = box(img, *uv[0], 12, 2, 2, MOSS, rng, 2, 4)
    # Bright amber tip on the leading end.
    f = shaft["front"]
    for x in range(f[0] + f[2] - 3, f[0] + f[2]):
        for y in range(f[1], f[1] + f[3]):
            px(img, x, y, EMBER[3])
    save(img, "entity/thorn_spike")


def new_rgba(w, h):
    from PIL import Image
    return Image.new("RGBA", (w, h), CLEAR)


def main():
    regent()
    choir()
    thorn_root()
    thorn_spike()


if __name__ == "__main__":
    main()
