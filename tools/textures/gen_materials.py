"""T05 textures: 18 Vestige crafting materials and boss components as 16x16 item icons.

Run from anywhere: python3 tools/textures/gen_materials.py
Deterministic: re-running regenerates identical PNGs.
"""
import math
import pathlib
import random
import sys

sys.path.insert(0, str(pathlib.Path(__file__).parent))
from vestige_palette import *  # noqa: F401,F403


def tone(ramp, x, y):
    """Top-left light: ramps run dark to light, so light falls on low x+y."""
    t = (x + y) / 30.0
    idx = len(ramp) - 1 - int(t * len(ramp))
    return ramp[max(0, min(len(ramp) - 1, idx))]


def put(img, x, y, color):
    if 0 <= x < img.width and 0 <= y < img.height:
        img.putpixel((x, y), rgba(color))


def fill_poly(img, pts, ramp=None, color=None):
    """Fill a convex polygon (points in order). Shaded from ramp, or flat color."""
    n = len(pts)
    for y in range(img.height):
        for x in range(img.width):
            px, py = x + 0.5, y + 0.5
            crosses = []
            for i in range(n):
                x1, y1 = pts[i]
                x2, y2 = pts[(i + 1) % n]
                crosses.append((x2 - x1) * (py - y1) - (y2 - y1) * (px - x1))
            if all(c >= 0 for c in crosses) or all(c <= 0 for c in crosses):
                put(img, x, y, color if color is not None else tone(ramp, x, y))
    return img


def rect(img, x0, y0, x1, y1, color):
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            put(img, x, y, color)


def circle(img, cx, cy, r, ramp=None, color=None, ring=None):
    """Filled disc. If ring=(r_inner, r_outer, color) is given, that band is drawn in color instead."""
    for y in range(img.height):
        for x in range(img.width):
            d = ((x + 0.5 - cx) ** 2 + (y + 0.5 - cy) ** 2) ** 0.5
            if ring and ring[0] <= d <= ring[1]:
                put(img, x, y, ring[2])
            elif d <= r:
                put(img, x, y, color if color is not None else tone(ramp, x, y))
    return img


def line(img, x0, y0, x1, y1, color):
    steps = max(abs(x1 - x0), abs(y1 - y0), 1) * 2
    for i in range(steps + 1):
        t = i / steps
        put(img, round(x0 + (x1 - x0) * t), round(y0 + (y1 - y0) * t), color)


def finish(img, name):
    outline(img, INK[0])
    save(img, "item/" + name)


def ingot(name, top, front, side):
    """Vanilla-like ingot: lit top face, front face, darker side face."""
    img = new(16)
    fill_poly(img, [(2, 9), (5, 5), (14, 5), (11, 9)], color=top)
    fill_poly(img, [(2, 9), (11, 9), (11, 13), (2, 13)], color=front)
    fill_poly(img, [(11, 9), (14, 5), (14, 9), (11, 13)], color=side)
    finish(img, name)


def aetherite_shard():
    img = new(16)
    fill_poly(img, [(8, 2), (12, 6), (11, 13), (8, 14), (4, 11), (5, 5)], ramp=TEAL)
    line(img, 8, 3, 8, 12, OPAL[4])
    finish(img, "aetherite_shard")


def aetherite_dust():
    img = new(16)
    rng = random.Random(501)
    for y in range(16):
        for x in range(16):
            if ((x - 8) / 6.0) ** 2 + ((y - 11) / 3.0) ** 2 <= 1.0:
                put(img, x, y, TEAL[rng.randint(1, 3)])
    for _ in range(10):
        put(img, rng.randint(3, 12), rng.randint(8, 12), OPAL[rng.randint(2, 4)])
    finish(img, "aetherite_dust")


def aetherite_ingot():
    ingot("aetherite_ingot", TEAL[4], TEAL[2], TEAL[1])


def riftglass():
    img = new(16)
    fill_poly(img, [(8, 1), (12, 6), (10, 14), (6, 14), (4, 8), (5, 4)], ramp=VIOLET)
    line(img, 7, 3, 6, 9, OPAL[4])
    finish(img, "riftglass")


def riftglass_lens():
    img = new(16)
    circle(img, 8, 8, 6.5, color=GOLD[3])
    circle(img, 8, 8, 5.5, ramp=OPAL)
    put(img, 5, 5, OPAL[4])
    put(img, 6, 5, OPAL[4])
    finish(img, "riftglass_lens")


def _parchment(img):
    rect(img, 3, 2, 12, 13, IVORY[4])
    rect(img, 3, 2, 12, 2, IVORY[1])
    rect(img, 3, 13, 12, 13, IVORY[1])
    rng = random.Random(502)
    for y in range(4, 12, 2):
        for x in range(5, 11):
            if rng.random() < 0.7:
                put(img, x, y, TEAL[1] if (x + y) % 3 else INK[2])


def inscribed_page():
    img = new(16)
    _parchment(img)
    finish(img, "inscribed_page")


def sealed_page():
    img = new(16)
    _parchment(img)
    circle(img, 8, 11, 2.2, ramp=GOLD)
    put(img, 8, 11, TEAL[3])
    finish(img, "sealed_page")


def mnemonic_filament():
    img = new(16)
    for x in range(1, 15):
        y = 8 + round(3 * math.sin(x * 0.9))
        put(img, x, y, TEAL[3])
        put(img, x, y + 1, TEAL[1])
        if x % 4 == 0:
            put(img, x, y - 1, OPAL[3])
    finish(img, "mnemonic_filament")


def reliquary_shard():
    img = new(16)
    fill_poly(img, [(6, 1), (8, 1), (8, 14), (3, 12), (2, 6)], ramp=TEAL)
    fill_poly(img, [(8, 1), (11, 3), (14, 9), (9, 14), (8, 14)], ramp=GOLD)
    finish(img, "reliquary_shard")


def aurelian_resin():
    img = new(16)
    fill_poly(img, [(8, 2), (11, 8), (12, 11), (8, 14), (4, 11), (5, 8)], ramp=GOLD)
    put(img, 6, 9, GOLD[4])
    finish(img, "aurelian_resin")


def aether_down():
    img = new(16)
    line(img, 3, 13, 13, 2, IVORY[4])
    for t in range(2, 10):
        x = 3 + t
        y = 13 - t
        line(img, x, y, x - 3, y + 2, OPAL[3])
        line(img, x, y, x + 2, y - 1, OPAL[4])
    finish(img, "aether_down")


def sovereign_thorn():
    img = new(16)
    fill_poly(img, [(2, 13), (13, 2), (7, 13)], ramp=MOSS)
    line(img, 4, 11, 11, 4, GOLD[3])
    put(img, 3, 12, GOLD[4])
    finish(img, "sovereign_thorn")


def liminal_chord():
    img = new(16)
    rect(img, 3, 1, 4, 7, VIOLET[3])
    rect(img, 11, 1, 12, 7, VIOLET[3])
    rect(img, 3, 7, 12, 8, VIOLET[2])
    rect(img, 7, 8, 8, 14, VIOLET[2])
    put(img, 3, 1, VIOLET[4])
    put(img, 11, 1, VIOLET[4])
    finish(img, "liminal_chord")


def gravity_heart():
    img = new(16)
    circle(img, 8, 8, 6.5, ramp=INK)
    circle(img, 8, 8, 2.5, ramp=VIOLET)
    put(img, 7, 7, VIOLET[4])
    finish(img, "gravity_heart")


def starforged_core():
    img = new(16)
    circle(img, 8, 8, 6.5, ramp=EMBER)
    circle(img, 8, 8, 2.5, color=EMBER[4])
    put(img, 7, 7, (255, 255, 240))
    finish(img, "starforged_core")


def concordant_seal():
    img = new(16)
    circle(img, 8, 8, 6.5, ramp=GOLD)
    for x, y in ((8, 5), (8, 6), (8, 10), (8, 11), (5, 8), (6, 8), (10, 8), (11, 8), (8, 8)):
        put(img, x, y, TEAL[3])
    finish(img, "concordant_seal")


def prime_mnemonic_core():
    img = new(16)
    for y in range(16):
        for x in range(16):
            d = ((x + 0.5 - 8) ** 2 + (y + 0.5 - 8) ** 2) ** 0.5
            if d > 6.5:
                continue
            if 4.2 <= d <= 5.2:
                put(img, x, y, GOLD[3])
            elif 2.6 <= d <= 3.3:
                put(img, x, y, VIOLET[2])
            else:
                put(img, x, y, tone(OPAL, x, y))
    finish(img, "prime_mnemonic_core")


def concordant_alloy():
    ingot("concordant_alloy", GOLD[4], TEAL[2], TEAL[1])


MATERIALS = [
    aetherite_shard,
    aetherite_dust,
    aetherite_ingot,
    riftglass,
    riftglass_lens,
    inscribed_page,
    sealed_page,
    mnemonic_filament,
    reliquary_shard,
    aurelian_resin,
    aether_down,
    sovereign_thorn,
    liminal_chord,
    gravity_heart,
    starforged_core,
    concordant_seal,
    prime_mnemonic_core,
    concordant_alloy,
]

if __name__ == "__main__":
    for make in MATERIALS:
        make()
