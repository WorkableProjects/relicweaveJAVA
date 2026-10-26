"""T15 textures: the six Vestige relics as 16x16 item icons.

Run from anywhere: python3 tools/textures/gen_relics.py
Deterministic: re-running regenerates identical PNGs.
"""
import pathlib
import sys

sys.path.insert(0, str(pathlib.Path(__file__).parent))
from vestige_palette import *  # noqa: F401,F403


def put(img, x, y, color):
    if 0 <= x < img.width and 0 <= y < img.height:
        img.putpixel((x, y), rgba(color))


def rect(img, x0, y0, x1, y1, color):
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            put(img, x, y, color)


def disc(img, cx, cy, r, ramp, ring=None):
    """Disc lit from the top-left; optional (r_in, r_out, color) ring band."""
    for y in range(img.height):
        for x in range(img.width):
            d = ((x + 0.5 - cx) ** 2 + (y + 0.5 - cy) ** 2) ** 0.5
            if ring and ring[0] <= d <= ring[1]:
                put(img, x, y, ring[2])
            elif d <= r:
                t = max(0.0, min(0.999, 0.5 + ((x - cx) + (y - cy)) / (-4.0 * r) * 1.0))
                put(img, x, y, ramp[int(t * len(ramp))])


def line(img, x0, y0, x1, y1, color):
    steps = max(abs(x1 - x0), abs(y1 - y0), 1) * 2
    for i in range(steps + 1):
        t = i / steps
        put(img, round(x0 + (x1 - x0) * t), round(y0 + (y1 - y0) * t), color)


def finish(img, name):
    outline(img, INK[0])
    save(img, "item/" + name)


def surveyors_lens():
    img = new(16)
    line(img, 3, 13, 7, 9, GOLD[2])  # handle
    line(img, 4, 13, 8, 9, GOLD[1])
    disc(img, 9.5, 6.5, 5, OPAL, ring=(3.6, 5.0, GOLD[3]))
    put(img, 8, 5, TEAL[4])
    put(img, 9, 5, TEAL[4])
    put(img, 8, 6, TEAL[3])
    finish(img, "surveyors_lens")


def gravity_nail():
    img = new(16)
    rect(img, 6, 1, 9, 2, GOLD[3])  # head
    rect(img, 7, 3, 8, 11, IVORY[3])
    rect(img, 8, 3, 8, 11, IVORY[1])
    put(img, 7, 12, IVORY[2])
    put(img, 8, 12, IVORY[0])
    put(img, 7, 13, IVORY[1])
    for i, y in enumerate((5, 7, 9)):  # violet pull-rings
        rect(img, 4 - (i % 2), y, 5 - (i % 2), y, VIOLET[3])
        rect(img, 10 + (i % 2), y, 11 + (i % 2), y, VIOLET[2])
    finish(img, "gravity_nail")


def cycle_dial():
    img = new(16)
    disc(img, 8, 8, 6.5, GOLD, ring=(4.6, 5.6, INK[2]))
    disc(img, 8, 8, 4.4, OPAL)
    for x, y in ((8, 4), (12, 8), (8, 12), (4, 8)):  # quarter marks
        put(img, x, y, GOLD[0])
    line(img, 8, 8, 8, 5, INK[1])
    line(img, 8, 8, 11, 9, TEAL[1])
    put(img, 8, 8, VIOLET[3])
    finish(img, "cycle_dial")


def riftglass_lantern():
    img = new(16)
    rect(img, 6, 1, 9, 1, GOLD[2])  # cap and ring
    rect(img, 7, 0, 8, 0, GOLD[1])
    rect(img, 4, 2, 11, 3, GOLD[2])
    rect(img, 5, 4, 10, 11, OPAL[2])  # glass body
    rect(img, 6, 5, 7, 9, OPAL[4])
    rect(img, 7, 6, 8, 9, TEAL[4])
    rect(img, 7, 7, 8, 8, VIOLET[4])
    rect(img, 9, 4, 10, 11, OPAL[1])
    rect(img, 4, 12, 11, 13, GOLD[1])
    finish(img, "riftglass_lantern")


def mnemonic_spindle():
    img = new(16)
    line(img, 3, 13, 12, 2, IVORY[3])  # shaft
    line(img, 4, 13, 13, 2, IVORY[1])
    for t in (0.35, 0.5, 0.65):  # wound filament
        x = round(3 + 9 * t)
        y = round(13 - 11 * t)
        rect(img, x - 2, y - 1, x + 1, y + 1, VIOLET[2])
        put(img, x - 2, y - 1, VIOLET[4])
    put(img, 12, 2, GOLD[4])
    put(img, 13, 1, GOLD[3])
    finish(img, "mnemonic_spindle")


def sealkeepers_signet():
    img = new(16)
    disc(img, 8, 9, 5.5, GOLD, ring=(3.2, 4.4, GOLD[0]))
    disc(img, 8, 9, 3.2, TEAL)
    rect(img, 7, 7, 8, 11, GOLD[4])  # seal bar
    rect(img, 6, 9, 9, 9, GOLD[4])
    rect(img, 6, 1, 9, 3, GOLD[3])  # band top
    rect(img, 7, 2, 8, 2, GOLD[1])
    put(img, 7, 1, GOLD[4])
    finish(img, "sealkeepers_signet")


RELICS = [surveyors_lens, gravity_nail, cycle_dial, riftglass_lantern, mnemonic_spindle, sealkeepers_signet]

if __name__ == "__main__":
    for make in RELICS:
        make()
