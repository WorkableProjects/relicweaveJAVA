"""T04 textures: null_ward and gravity_anchor blocks."""
import random
import sys
import pathlib

sys.path.insert(0, str(pathlib.Path(__file__).parent))
from vestige_palette import *  # noqa: F401,F403


def null_ward():
    img = new(16)
    noise_fill(img, INK, seed=401, lo=0, hi=1)
    # Obsidian-like facets with a glowing violet rune: a ring with a vertical bar.
    rng = random.Random(402)
    for _ in range(18):
        img.putpixel((rng.randrange(16), rng.randrange(16)), rgba(INK[2]))
    for y in range(16):
        for x in range(16):
            d = (x - 7.5) ** 2 + (y - 7.5) ** 2
            if 20 <= d <= 30:
                img.putpixel((x, y), rgba(VIOLET[3 if d < 25 else 2]))
    for y in range(3, 13):
        img.putpixel((7, y), rgba(VIOLET[4]))
        img.putpixel((8, y), rgba(VIOLET[3]))
    for x in range(5, 11):
        img.putpixel((x, 7), rgba(VIOLET[1]))
    for i in range(16):
        for edge in ((i, 0), (i, 15), (0, i), (15, i)):
            img.putpixel(edge, rgba(BLACK))
    save(img, "block/null_ward")


def gravity_anchor_side():
    img = new(16)
    noise_fill(img, INK, seed=411, lo=0, hi=2)
    # Gold bands top and bottom with a central opal stud, like an anvil weight.
    for y in (0, 1, 14, 15):
        for x in range(16):
            img.putpixel((x, y), rgba(GOLD[1 if y in (0, 15) else 2]))
    for y in range(2, 14):
        for x in (0, 1, 14, 15):
            img.putpixel((x, y), rgba(GOLD[1 if x in (0, 15) else 2]))
    for y in range(6, 10):
        for x in range(6, 10):
            img.putpixel((x, y), rgba(GOLD[3 if (x + y) % 2 else 4]))
    img.putpixel((7, 7), rgba(OPAL[4]))
    save(img, "block/gravity_anchor")


def gravity_anchor_top():
    img = new(16)
    noise_fill(img, INK, seed=412, lo=0, hi=2)
    for y in range(16):
        for x in range(16):
            d = max(abs(x - 7.5), abs(y - 7.5))
            if d >= 6.5:
                img.putpixel((x, y), rgba(GOLD[2]))
            elif 3.5 <= d < 4.5:
                img.putpixel((x, y), rgba(GOLD[1]))
    for y in range(6, 10):
        for x in range(6, 10):
            img.putpixel((x, y), rgba(GOLD[4 if (x + y) % 2 else 3]))
    save(img, "block/gravity_anchor_top")


if __name__ == "__main__":
    null_ward()
    gravity_anchor_side()
    gravity_anchor_top()
