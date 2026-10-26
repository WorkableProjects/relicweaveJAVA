"""Generates the Concordance Gate textures (T02): frame, keystone (inactive/active), return waystone, threshold sigil."""
import sys
import pathlib

sys.path.insert(0, str(pathlib.Path(__file__).parent))
from vestige_palette import *  # noqa: E402,F403


def frame():
    img = new(16)
    noise_fill(img, VIOLET, seed=201, lo=1, hi=3)
    for i in range(16):  # opal edge lines
        for (x, y) in ((i, 0), (i, 15), (0, i), (15, i)):
            img.putpixel((x, y), rgba(OPAL[1]))
    for i in range(3, 13):  # gold inlay cross
        img.putpixel((i, 8), rgba(GOLD[2]))
        img.putpixel((8, i), rgba(GOLD[2]))
    save(img, "block/concordance_frame")


def keystone(active, suffix):
    side = new(16)
    noise_fill(side, VIOLET if not active else TEAL, seed=211 + active, lo=0, hi=2)
    for i in range(16):
        for (x, y) in ((i, 0), (i, 15), (0, i), (15, i)):
            side.putpixel((x, y), rgba(GOLD[1]))
    glow = OPAL[4] if active else VIOLET[3]
    for (x, y) in ((8, 5), (7, 6), (8, 6), (9, 6), (6, 7), (7, 7), (8, 7), (9, 7), (10, 7), (7, 8), (8, 8), (9, 8), (8, 9)):
        side.putpixel((x, y), rgba(glow))
    save(side, "block/gate_keystone_side" + suffix)
    top = new(16)
    noise_fill(top, VIOLET if not active else TEAL, seed=221 + active, lo=0, hi=2)
    for r in range(3, 13):
        for c in range(3, 13):
            if r in (3, 12) or c in (3, 12):
                top.putpixel((c, r), rgba(GOLD[2]))
    for r in range(6, 10):
        for c in range(6, 10):
            top.putpixel((c, r), rgba(glow))
    save(top, "block/gate_keystone_top" + suffix)


def waystone():
    img = new(16)
    noise_fill(img, IVORY, seed=231, lo=1, hi=3)
    for y in (0, 15):
        for x in range(16):
            img.putpixel((x, y), rgba(GOLD[1]))
    for y in range(3, 12):
        img.putpixel((3, y), rgba(TEAL[3]))
        img.putpixel((2, y), rgba(TEAL[2]))
    save(img, "block/return_waystone")


def sigil():
    img = new(16)
    cx = cy = 7.5
    for y in range(16):
        for x in range(16):
            d = abs(x - cx) + abs(y - cy)
            if d <= 6.5:
                img.putpixel((x, y), rgba(VIOLET[2] if d > 4 else VIOLET[3]))
    for (x, y) in ((7, 4), (8, 4), (7, 11), (8, 11), (4, 7), (4, 8), (11, 7), (11, 8)):
        img.putpixel((x, y), rgba(GOLD[3]))
    for (x, y) in ((7, 7), (8, 7), (7, 8), (8, 8)):
        img.putpixel((x, y), rgba(OPAL[4]))
    outline(img, INK[0])
    save(img, "item/threshold_sigil")


if __name__ == "__main__":
    frame()
    keystone(0, "")
    keystone(1, "_active")
    waystone()
    sigil()
