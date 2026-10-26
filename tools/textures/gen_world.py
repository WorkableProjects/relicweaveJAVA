"""Block textures for The Vestige's terrain, flora, ores and stone family (T01).

Run from anywhere: python3 tools/textures/gen_world.py
"""
import random
import sys
import pathlib

sys.path.insert(0, str(pathlib.Path(__file__).parent))
from vestige_palette import *  # noqa: E402,F401


def stone(seed, ramp=IVORY, lo=1, hi=3):
    img = new()
    noise_fill(img, ramp, seed, lo, hi)
    return img


def speckle(img, color, seed, count):
    rng = random.Random(seed)
    for _ in range(count):
        img.putpixel((rng.randrange(16), rng.randrange(16)), rgba(color))
    return img


def cracks(img, color, seed, n=3):
    rng = random.Random(seed)
    for _ in range(n):
        x, y = rng.randrange(16), rng.randrange(16)
        for _ in range(rng.randint(4, 8)):
            img.putpixel((x % 16, y % 16), rgba(color))
            x += rng.choice((-1, 0, 1))
            y += rng.choice((0, 1))
    return img


def rect(img, x0, y0, x1, y1, color):
    for y in range(y0, y1):
        for x in range(x0, x1):
            img.putpixel((x, y), rgba(color))


def plain_stone(name, seed):
    save(stone(seed), f"block/{name}")


# ---- predefined stone family ----
save(stone(1), "block/vestige_stone")
save(bricks(new(), IVORY, 2), "block/vestige_bricks")
save(cracks(bricks(new(), IVORY, 3), IVORY[0], 31, 4), "block/cracked_vestige_bricks")

img = stone(4, IVORY, 2, 4)
rect(img, 3, 3, 13, 5, IVORY[0]); rect(img, 3, 11, 13, 13, IVORY[0])
rect(img, 3, 3, 5, 13, IVORY[0]); rect(img, 11, 3, 13, 13, IVORY[0])
rect(img, 6, 6, 10, 10, IVORY[1]); img.putpixel((7, 7), rgba(IVORY[4]))
save(img, "block/carved_vestige_stone")

img = bricks(new(), IVORY, 5)
for y in range(16):
    for x in range(16):
        if (x + y) % 8 == 0 or (x * 3 + y) % 11 == 0:
            img.putpixel((x, y), rgba(GOLD[2 + (x + y) % 3]))
save(img, "block/gilded_vestige_bricks")

img = bricks(new(), IVORY, 6, mortar=TEAL[0])
rect(img, 5, 5, 11, 11, TEAL[0]); rect(img, 6, 6, 10, 10, TEAL[2])
for p in ((7, 6), (8, 7), (7, 8), (8, 9), (6, 7)):
    img.putpixel(p, rgba(TEAL[4]))
save(img, "block/rune_vestige_bricks")

save(stone(7, INK, 1, 3), "block/gloam_stone")

# ---- stone family extras ----
img = stone(8, IVORY, 2, 4)
rect(img, 0, 0, 16, 1, IVORY[1]); rect(img, 0, 15, 16, 16, IVORY[1]); rect(img, 0, 0, 1, 16, IVORY[1]); rect(img, 15, 0, 16, 16, IVORY[1])
save(img, "block/polished_vestige_stone")

img = new(); noise_fill(img, IVORY, 9, 2, 4)
for y in range(16):
    for x in (0, 1, 14, 15):
        img.putpixel((x, y), rgba(IVORY[0] if x in (0, 15) else IVORY[1]))
for y in (2, 7, 12):
    rect(img, 2, y, 14, y + 1, IVORY[1])
save(img, "block/vestige_pillar")
img = stone(10, IVORY, 2, 4)
rect(img, 2, 2, 14, 14, IVORY[1]); rect(img, 4, 4, 12, 12, IVORY[3]); rect(img, 6, 6, 10, 10, GOLD[2])
save(img, "block/vestige_pillar_top")

save(bricks(new(), INK, 11, mortar=INK[0], rows=4), "block/gloam_bricks")

img = new()
for y in range(16):
    for x in range(16):
        edge = x % 8 in (0, 7) or y % 8 in (0, 7)
        img.putpixel((x, y), rgba(GOLD[1] if edge else IVORY[2 + ((x // 8 + y // 8) % 2)]))
speckle(img, GOLD[3], 12, 10)
save(img, "block/concordant_paving")

# ---- surfaces ----
img = new(); noise_fill(img, MOSS, 13, 1, 3)
speckle(img, GOLD[2], 14, 22); speckle(img, GOLD[4], 15, 6); speckle(img, TEAL[3], 16, 6)
save(img, "block/aureate_moss_top")
img = stone(17, IVORY, 1, 3)
rng = random.Random(18)
for x in range(16):
    depth = rng.randint(3, 6)
    for y in range(depth):
        img.putpixel((x, y), rgba(MOSS[rng.randint(1, 3)] if y < depth - 1 else GOLD[1]))
save(img, "block/aureate_moss_side")

img = new(); noise_fill(img, OPAL, 19, 0, 2)
for y in range(16):
    for x in range(16):
        if (x + 2 * y) % 7 == 0:
            img.putpixel((x, y), rgba(TEAL[2]))
speckle(img, OPAL[4], 20, 6)
save(img, "block/echo_clay")

img = new(); noise_fill(img, [BLACK, (30, 26, 34), (52, 44, 58), (74, 62, 80), (96, 82, 100)], 21, 0, 3)
speckle(img, EMBER[2], 22, 10); speckle(img, EMBER[3], 23, 5); speckle(img, VIOLET[3], 24, 4)
save(img, "block/starfall_slag")

img = stone(25, IVORY, 0, 2)
cracks(img, BLACK, 26, 5); cracks(img, VIOLET[2], 27, 2)
save(img, "block/fractured_vestige_stone")

# ---- ores and crystal ----
img = stone(28, IVORY, 1, 3)
for cx, cy in ((4, 4), (10, 5), (6, 10), (12, 12), (3, 12)):
    for dx, dy in ((0, 0), (1, 0), (0, 1), (1, 1)):
        img.putpixel((cx + dx, cy + dy), rgba(TEAL[2 + (dx + dy) % 2 * 2]))
    img.putpixel((cx, cy), rgba(OPAL[4]))
save(img, "block/aetherite_ore")

img = new()
for base, h in ((8, 11), (4, 7), (12, 6), (6, 4), (10, 3)):
    for y in range(16 - h, 16):
        wd = 1 + (y - (16 - h)) // 4
        for x in range(base - wd // 2, base - wd // 2 + wd + 1):
            if 0 <= x < 16:
                img.putpixel((x, y), rgba(VIOLET[2 + (x + y) % 3]) if x != base else rgba(VIOLET[4]))
    img.putpixel((base, 16 - h), rgba(OPAL[4]))
save(img, "block/riftglass_cluster")

img = new(); noise_fill(img, VIOLET, 29, 2, 4)
for y in range(16):
    for x in range(16):
        if x in (0, 15) or y in (0, 15):
            img.putpixel((x, y), rgba(VIOLET[1], 230))
        elif (x - y) % 6 == 0:
            img.putpixel((x, y), rgba(OPAL[4], 220))
        else:
            r, g, b, _ = img.getpixel((x, y)); img.putpixel((x, y), (r, g, b, 200))
save(img, "block/riftglass_block")

# ---- flora and wood ----
def log_side(seed, ramp, accent):
    img = new(); rng = random.Random(seed)
    for x in range(16):
        col = rng.randint(1, 3)
        for y in range(16):
            img.putpixel((x, y), rgba(ramp[max(0, min(4, col + rng.randint(-1, 1)))]))
    for _ in range(4):
        img.putpixel((rng.randrange(16), rng.randrange(16)), rgba(accent))
    return img

def log_top(seed, ramp, core):
    img = new(); noise_fill(img, ramp, seed, 1, 2)
    rect(img, 2, 2, 14, 14, ramp[3]); rect(img, 4, 4, 12, 12, ramp[2]); rect(img, 6, 6, 10, 10, core)
    return img

save(log_side(30, OPAL, TEAL[3]), "block/glassroot_log")
save(log_top(31, OPAL, TEAL[2]), "block/glassroot_log_top")
save(log_side(32, GOLD, MOSS[2]), "block/canopy_log")
save(log_top(33, GOLD, GOLD[4]), "block/canopy_log_top")

img = new(); rng = random.Random(34)
for y in range(16):
    for x in range(16):
        edge = y % 4 == 3
        img.putpixel((x, y), rgba(GOLD[0] if edge else GOLD[1 + rng.randint(0, 2)]))
save(img, "block/canopy_planks")

img = new(); rng = random.Random(35)
for y in range(16):
    for x in range(16):
        if rng.random() < 0.22:
            continue
        img.putpixel((x, y), rgba(GOLD[2 + rng.randint(0, 2)] if rng.random() < 0.6 else MOSS[3]))
save(img, "block/canopy_leaves")

img = new(); rng = random.Random(36)
for y in range(16):
    for x in range(16):
        r = OPAL[1 + rng.randint(0, 2)]
        img.putpixel((x, y), (r[0], r[1], r[2], 190))
for p in range(16):
    img.putpixel((p, 0), (*TEAL[3], 230)); img.putpixel((p, 15), (*TEAL[3], 230))
    img.putpixel((0, p), (*TEAL[3], 230)); img.putpixel((15, p), (*TEAL[3], 230))
for i in range(3, 13):
    img.putpixel((i, i), (*OPAL[4], 230))
save(img, "block/glassroot_crystal")

img = new()
for x, h in ((4, 7), (8, 10), (11, 6), (6, 5)):
    for y in range(16 - h, 16):
        img.putpixel((x, y), rgba(OPAL[2 + (y % 2)]))
        if y < 16 - h + 2:
            img.putpixel((x - 1, y), rgba(TEAL[3])); img.putpixel((x + 1, y), rgba(TEAL[3]))
    img.putpixel((x, 16 - h - 1), rgba(OPAL[4]))
save(img, "block/glassroot_sprout")

img = new()
for y in range(8, 16):
    img.putpixel((7, y), rgba(MOSS[2])); img.putpixel((8, y), rgba(MOSS[1]))
img.putpixel((6, 12), rgba(MOSS[3])); img.putpixel((9, 10), rgba(MOSS[3]))
for cx, cy in ((7, 5), (8, 5)):
    for dx, dy in ((-2, 0), (2, 0), (0, -2), (0, 2), (-1, -1), (1, -1), (-1, 1), (1, 1), (0, 0)):
        img.putpixel((cx + dx, cy + dy), rgba(GOLD[3] if (dx or dy) else GOLD[4]))
save(img, "block/aurelian_bloom")

img = new()
for x, h in ((3, 12), (6, 14), (9, 11), (12, 13)):
    for y in range(16 - h, 16):
        img.putpixel((x, y), rgba(MOSS[2 if y % 3 else 3]))
    for y in range(16 - h, 16 - h + 3):
        img.putpixel((x, y), rgba(GOLD[1])); img.putpixel((x + 1, y), rgba(GOLD[0]))
save(img, "block/hushfen_reed")
print("textures written")
