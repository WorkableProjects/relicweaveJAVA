"""T03 textures: rift fissure block, ward salve item, effect icons, cycle and hazard HUD glyph sheets."""
import random
import sys
import pathlib

sys.path.insert(0, str(pathlib.Path(__file__).parent))
from vestige_palette import *  # noqa: F401,F403
from PIL import Image


def rift_fissure():
    img = new(16)
    noise_fill(img, INK, seed=301, lo=0, hi=2)
    rng = random.Random(302)
    # A jagged violet crack running top to bottom, with a glowing core.
    x = 8
    for y in range(16):
        x += rng.choice((-1, 0, 0, 1))
        x = max(4, min(11, x))
        for dx, ramp_i in ((-2, 0), (-1, 1), (0, 4), (1, 3), (2, 0)):
            px = x + dx
            if 0 <= px < 16:
                img.putpixel((px, y), rgba(VIOLET[ramp_i] if ramp_i != 4 else OPAL[3]))
    for _ in range(14):
        img.putpixel((rng.randrange(16), rng.randrange(16)), rgba(VIOLET[rng.randint(0, 2)]))
    save(img, "block/rift_fissure")


def ward_salve():
    img = new(16)
    # Glass bottle with a teal liquid.
    for y in range(5, 14):
        for x in range(4, 12):
            edge = x in (4, 11) or y in (5, 13)
            if edge:
                img.putpixel((x, y), rgba(OPAL[1]))
            elif y >= 8:
                img.putpixel((x, y), rgba(TEAL[2 if (x + y) % 3 else 3]))
            else:
                img.putpixel((x, y), rgba(OPAL[4], 90))
    for y in range(2, 5):
        for x in range(6, 10):
            img.putpixel((x, y), rgba(OPAL[2]))
    for x in range(6, 10):
        img.putpixel((x, 1), rgba(GOLD[2]))
    img.putpixel((6, 9), rgba(TEAL[4]))
    img.putpixel((7, 10), rgba(OPAL[4]))
    outline(img, INK[0])
    save(img, "item/ward_salve")


def effect_icon(name, ramp, glyph):
    img = new(18)
    for y in range(18):
        for x in range(18):
            if (x - 8.5) ** 2 + (y - 8.5) ** 2 <= 8.5 ** 2:
                img.putpixel((x, y), rgba(ramp[1 if (x + y) % 5 else 0]))
    glyph(img, ramp)
    save(img, "mob_effect/" + name)


def warded_glyph(img, ramp):
    # Shield.
    for y in range(4, 14):
        w = 5 if y < 9 else max(0, 5 - (y - 8))
        for x in range(9 - w, 9 + w):
            img.putpixel((x, y), rgba(OPAL[3] if y in (4, 5) or x in (9 - w, 8 + w) else ramp[3]))


def sickness_glyph(img, ramp):
    # Violet fissure.
    for y in range(4, 15):
        x = 9 + (1 if y % 3 == 0 else -1 if y % 3 == 1 else 0)
        img.putpixel((x, y), rgba(OPAL[4]))
        img.putpixel((x + 1, y), rgba(ramp[4]))
        img.putpixel((x - 1, y), rgba(ramp[0]))


def sheet(name, width, painters):
    img = Image.new("RGBA", (width, 9), CLEAR)
    for i, paint in enumerate(painters):
        paint(img, i * 9)
    save(img, "gui/vestige/" + name)


def disc(ramp):
    def paint(img, ox):
        for y in range(9):
            for x in range(9):
                d = (x - 4) ** 2 + (y - 4) ** 2
                if d <= 16:
                    img.putpixel((ox + x, y), rgba(ramp[0] if d > 12 else ramp[3 if d < 9 else 1]))
    return paint


def diamond(ramp):
    def paint(img, ox):
        for y in range(9):
            for x in range(9):
                d = abs(x - 4) + abs(y - 4)
                if d <= 4:
                    img.putpixel((ox + x, y), rgba(ramp[0] if d == 4 else ramp[3 if d < 2 else 2]))
    return paint


def star(ramp):
    def paint(img, ox):
        for i in range(9):
            img.putpixel((ox + i, 4), rgba(ramp[2]))
            img.putpixel((ox + 4, i), rgba(ramp[2]))
        for i in range(1, 8):
            img.putpixel((ox + i, i), rgba(ramp[1]))
            img.putpixel((ox + 8 - i, i), rgba(ramp[1]))
        for x in range(3, 6):
            for y in range(3, 6):
                img.putpixel((ox + x, y), rgba(ramp[4]))
    return paint


def hazard_icons():
    def rift(img, ox):
        for y in range(9):
            x = 4 + (1 if y % 3 == 0 else -1 if y % 3 == 1 else 0)
            img.putpixel((ox + x, y), rgba(VIOLET[4]))
            img.putpixel((ox + x + 1, y), rgba(VIOLET[2]))
            img.putpixel((ox + x - 1, y), rgba(VIOLET[0]))

    def null_pocket(img, ox):
        disc(INK)(img, ox)
        for y in range(2, 7):
            for x in range(2, 7):
                if (x + y) % 2 == 0:
                    img.putpixel((ox + x, y), rgba(OPAL[2]))

    def gravity(img, ox):
        for y in range(1, 8):
            img.putpixel((ox + 4, y), rgba(GOLD[3]))
        for i in range(1, 4):
            img.putpixel((ox + 4 - i, 4 + i), rgba(GOLD[3]))
            img.putpixel((ox + 4 + i, 4 + i), rgba(GOLD[3]))

    def hush(img, ox):
        for y in (2, 4, 6):
            for x in range(1, 8):
                img.putpixel((ox + x, y), rgba(TEAL[3 if x % 2 else 2]))
        for i in range(9):
            img.putpixel((ox + i, 8 - i), rgba(INK[4]))

    def sickness(img, ox):
        disc(VIOLET)(img, ox)
        for i in range(2, 7):
            img.putpixel((ox + i, 4), rgba(OPAL[4]))

    # Order matches ICONS in HazardHud: rift, null pocket, gravity, hush, sickness.
    sheet("hazard_icons", 45, [rift, null_pocket, gravity, hush, sickness])


if __name__ == "__main__":
    rift_fissure()
    ward_salve()
    effect_icon("warded", TEAL, warded_glyph)
    effect_icon("rift_sickness", VIOLET, sickness_glyph)
    sheet("cycle_glyphs", 27, [disc(TEAL), diamond(GOLD), star(VIOLET)])
    hazard_icons()
