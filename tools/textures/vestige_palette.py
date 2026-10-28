"""Shared palette and helpers for procedurally generated 0.0.4 (The Vestige) textures.

Every texture generator script imports this module so the art reads as one family:
aged ivory stone, weathered gold, oxidized teal, ink-blue shadow, translucent opal/crystal,
and restrained violet for unstable reality.

Usage from a generator script in tools/textures/:
    import sys, pathlib; sys.path.insert(0, str(pathlib.Path(__file__).parent))
    from vestige_palette import *
    img = new(16)                    # transparent RGBA 16x16
    noise_fill(img, IVORY, seed=1)   # deterministic per seed
    save(img, "block/vestige_stone") # -> src/main/resources/assets/relicweave/textures/block/vestige_stone.png

All randomness must come from seeded random.Random so re-running a script reproduces identical PNGs.
"""
import random
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
TEXTURES = ROOT / "src" / "main" / "resources" / "assets" / "relicweave" / "textures"

# Ramps: dark -> light. Pick from these; do not invent ad-hoc colors.
IVORY = [(122, 110, 92), (158, 146, 124), (191, 180, 156), (216, 207, 184), (236, 229, 210)]
GOLD = [(110, 78, 28), (156, 114, 40), (196, 152, 58), (226, 190, 92), (247, 225, 150)]
TEAL = [(22, 70, 72), (36, 104, 104), (58, 140, 134), (96, 178, 166), (150, 214, 200)]
INK = [(14, 18, 38), (24, 32, 62), (38, 50, 92), (60, 76, 128), (92, 110, 168)]
OPAL = [(150, 170, 200), (180, 200, 226), (206, 222, 240), (228, 238, 250), (248, 252, 255)]
VIOLET = [(54, 24, 78), (84, 40, 118), (120, 64, 160), (162, 104, 204), (204, 160, 236)]
EMBER = [(90, 30, 12), (150, 58, 18), (210, 104, 30), (242, 160, 60), (255, 220, 130)]
MOSS = [(30, 52, 34), (46, 78, 48), (68, 108, 64), (100, 140, 86), (140, 176, 116)]
BLACK = (10, 10, 14)
CLEAR = (0, 0, 0, 0)


def new(size=16, fill=CLEAR):
    return Image.new("RGBA", (size, size), fill if len(fill) == 4 else fill + (255,))


def rgba(c, a=255):
    return c if len(c) == 4 else (c[0], c[1], c[2], a)


def noise_fill(img, ramp, seed, lo=1, hi=3, box=None):
    """Fill box (default whole image) with random picks from ramp[lo..hi]."""
    rng = random.Random(seed)
    x0, y0, x1, y1 = box or (0, 0, img.width, img.height)
    for y in range(y0, y1):
        for x in range(x0, x1):
            img.putpixel((x, y), rgba(ramp[rng.randint(lo, hi)]))
    return img


def outline(img, color):
    """Draw a 1px outline in color around every opaque pixel (for items)."""
    src = img.copy()
    w, h = img.size
    for y in range(h):
        for x in range(w):
            if src.getpixel((x, y))[3] == 0:
                for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                    nx, ny = x + dx, y + dy
                    if 0 <= nx < w and 0 <= ny < h and src.getpixel((nx, ny))[3] > 0:
                        img.putpixel((x, y), rgba(color))
                        break
    return img


def bricks(img, ramp, seed, mortar=None, rows=4):
    """Running-bond bricks: rows of height size/rows, offset every other row."""
    rng = random.Random(seed)
    w, h = img.size
    rh = h // rows
    mortar = mortar or ramp[0]
    for y in range(h):
        row = y // rh
        for x in range(w):
            off = (rh * 2 if row % 2 else 0)
            if y % rh == rh - 1 or (x + off) % (w // 2) == 0:
                img.putpixel((x, y), rgba(mortar))
            else:
                img.putpixel((x, y), rgba(ramp[rng.randint(1, 3)]))
    return img


def save(img, rel):
    """rel like 'block/vestige_stone' or 'item/aetherite_shard' or 'entity/ruin_hound'."""
    path = TEXTURES / (rel + ".png")
    path.parent.mkdir(parents=True, exist_ok=True)
    img.save(path)
    return path
