"""0.0.4.5 textures: Multitools, Super Furnace faces (five tiers) and Scuba Gear (items + armor layers)."""
import random
import sys
import pathlib

sys.path.insert(0, str(pathlib.Path(__file__).parent))
from vestige_palette import *  # noqa: F401,F403
from PIL import Image

IRON = [(62, 62, 68), (100, 100, 106), (146, 146, 152), (194, 194, 198), (232, 232, 234)]
DIAMOND = [(16, 84, 92), (34, 134, 144), (70, 192, 196), (128, 228, 224), (204, 252, 246)]
NETHERITE = [(26, 22, 26), (48, 42, 46), (72, 64, 68), (102, 92, 96), (140, 126, 128)]
CELESTIAL = OPAL
STONE = [(52, 52, 56), (78, 78, 82), (104, 104, 108), (128, 128, 132), (156, 156, 160)]
WOOD = [(58, 38, 20), (88, 60, 32), (120, 84, 46), (150, 110, 64), (180, 140, 90)]
FIRE = EMBER
SOUL = [(16, 64, 74), (30, 120, 128), (60, 190, 200), (120, 230, 236), (210, 255, 255)]
RUBBER = [(14, 16, 20), (26, 30, 36), (40, 46, 54), (58, 66, 76), (84, 94, 106)]
YELLOW = [(120, 84, 10), (180, 130, 16), (226, 178, 30), (246, 212, 70), (255, 238, 140)]
GLASS = [(40, 90, 110), (70, 140, 168), (120, 196, 220), (180, 230, 244), (236, 252, 255)]

TIERS = {"iron": IRON, "gold": GOLD, "diamond": DIAMOND, "netherite": NETHERITE, "celestial": CELESTIAL}
LEVEL = {"iron": 1, "gold": 2, "diamond": 3, "netherite": 4, "celestial": 5}


def plot(img, pts, color):
    for x, y in pts:
        if 0 <= x < img.width and 0 <= y < img.height:
            img.putpixel((x, y), rgba(color))


def rect(img, x0, y0, x1, y1, color):
    for y in range(y0, y1):
        for x in range(x0, x1):
            plot(img, [(x, y)], color)


# ---------------------------------------------------------------- multitool

def multitool(name, metal, accent):
    """Diagonal handle; at its head a pickaxe pick (lower right), an axe blade (upper left) and a sword point."""
    img = new(16)
    for i in range(7):  # handle (2,13) -> (8,7)
        plot(img, [(2 + i, 13 - i)], WOOD[2 if i % 2 else 3])
        plot(img, [(3 + i, 13 - i)], WOOD[1])
    plot(img, [(1, 14)], WOOD[0])
    # sword blade continuing the handle line to the top right
    for i in range(6):
        x, y = 9 + i, 6 - i
        plot(img, [(x, y)], metal[4] if i % 2 == 0 else metal[3])
        plot(img, [(x - 1, y)], metal[2])
    plot(img, [(14, 1), (15, 0)], metal[4])
    # pickaxe pick: anti-diagonal spike down to the right
    for i in range(5):
        plot(img, [(10 + i, 7 + i)], metal[3] if i < 4 else metal[4])
        plot(img, [(10 + i, 8 + i)], metal[1])
    # axe blade: a wedge on the upper left, widest at its cutting edge
    blade = [(7, 5), (6, 5), (6, 4), (5, 4), (5, 3), (4, 3), (4, 2), (5, 2), (3, 2), (3, 3), (3, 4), (4, 4),
             (4, 5), (2, 3), (2, 4), (2, 5), (3, 5), (3, 6), (4, 6), (5, 6), (6, 6), (5, 5)]
    plot(img, blade, metal[2])
    plot(img, [(1, 3), (1, 4), (1, 5), (2, 6), (2, 2), (3, 1), (3, 7), (4, 7)], metal[4])  # edge
    plot(img, [(5, 4), (6, 5)], metal[3])
    # collar where the heads meet
    plot(img, [(8, 6), (9, 7), (8, 7), (9, 6)], accent[3])
    plot(img, [(9, 7)], accent[4])
    outline(img, INK[0])
    save(img, f"item/{name}_multitool")


# ---------------------------------------------------------------- super furnace

def frame(img, metal):
    for i in range(16):
        plot(img, [(i, 0), (i, 15), (0, i), (15, i)], metal[1])
    plot(img, [(1, 1), (14, 1), (1, 14), (14, 14)], metal[4])


def furnace_side(name, metal):
    img = new(16)
    bricks(img, STONE, seed=LEVEL[name] * 97, mortar=STONE[0])
    frame(img, metal)
    for x in range(1, 15):  # metal band
        plot(img, [(x, 7)], metal[3 if x % 3 else 4])
        plot(img, [(x, 8)], metal[2])
    save(img, f"block/{name}_super_furnace_side")


def furnace_top(name, metal):
    img = new(16)
    noise_fill(img, STONE, seed=7 + LEVEL[name], lo=1, hi=3)
    frame(img, metal)
    rect(img, 4, 4, 12, 12, metal[2])
    rect(img, 5, 5, 11, 11, metal[1])
    for x, y in ((4, 4), (11, 4), (4, 11), (11, 11)):
        plot(img, [(x, y)], metal[4])
    rect(img, 6, 6, 10, 10, STONE[0])  # flue
    save(img, f"block/{name}_super_furnace_top")


def furnace_front(name, metal, lit):
    img = new(16)
    bricks(img, STONE, seed=31 + LEVEL[name], mortar=STONE[0])
    frame(img, metal)
    # tier pips along the top
    level = LEVEL[name]
    start = 8 - level
    for i in range(level):
        plot(img, [(start + i * 2 + 1, 2)], metal[4])
        plot(img, [(start + i * 2 + 1, 3)], metal[2])
    # mouth
    rect(img, 3, 7, 13, 14, metal[1])
    rect(img, 4, 8, 12, 13, (18, 16, 16))
    for x in range(4, 12):
        plot(img, [(x, 7)], metal[3])
    if lit:
        flame = SOUL if name == "celestial" else FIRE
        rng = random.Random(level)
        for x in range(4, 12):
            height = rng.randint(2, 4)
            for k in range(height):
                plot(img, [(x, 12 - k)], flame[4 - min(k, 3)] if k < 3 else flame[1])
        plot(img, [(5, 9), (9, 9)], flame[2])
    else:
        for x in range(4, 12, 2):
            plot(img, [(x, 12)], (40, 34, 30))
    save(img, f"block/{name}_super_furnace_front" + ("_on" if lit else ""))


# ---------------------------------------------------------------- scuba items

def scuba_mask():
    img = new(16)
    rect(img, 2, 5, 14, 11, RUBBER[2])
    rect(img, 3, 6, 13, 10, GLASS[2])
    rect(img, 4, 6, 12, 7, GLASS[4])
    plot(img, [(7, 7), (8, 7)], GLASS[3])
    rect(img, 7, 6, 9, 10, RUBBER[1])  # nose bridge
    plot(img, [(1, 7), (1, 8), (14, 7), (14, 8), (0, 7), (15, 7)], RUBBER[3])
    for y in range(1, 9):  # snorkel
        plot(img, [(13, y)], YELLOW[3 if y % 2 else 2])
    plot(img, [(12, 1), (13, 1)], YELLOW[4])
    rect(img, 6, 11, 10, 13, RUBBER[1])
    outline(img, INK[0])
    save(img, "item/scuba_mask")


def scuba_tank():
    img = new(16)
    for x0 in (3, 9):
        rect(img, x0, 3, x0 + 4, 14, YELLOW[2])
        for y in range(3, 14):
            plot(img, [(x0, y)], YELLOW[1])
            plot(img, [(x0 + 1, y)], YELLOW[4] if y < 6 else YELLOW[3])
        rect(img, x0 + 1, 1, x0 + 3, 3, IRON[2])
        plot(img, [(x0 + 1, 1)], IRON[4])
    rect(img, 2, 6, 14, 7, RUBBER[2])  # straps
    rect(img, 2, 11, 14, 12, RUBBER[2])
    plot(img, [(7, 1), (8, 1), (8, 0)], RUBBER[3])  # hose
    outline(img, INK[0])
    save(img, "item/scuba_tank")


def scuba_wetsuit():
    img = new(16)
    rect(img, 3, 2, 13, 5, RUBBER[2])
    rect(img, 3, 5, 7, 15, RUBBER[2])
    rect(img, 9, 5, 13, 15, RUBBER[2])
    for x in range(3, 13):
        plot(img, [(x, 2)], RUBBER[3])
    for y in range(5, 15):  # side stripes
        plot(img, [(3, y), (12, y)], TEAL[3])
    rect(img, 3, 3, 13, 4, YELLOW[3])  # belt
    plot(img, [(8, 3)], IRON[4])
    outline(img, INK[0])
    save(img, "item/scuba_wetsuit")


def scuba_flippers():
    img = new(16)
    for x0 in (1, 9):
        rect(img, x0 + 1, 3, x0 + 4, 8, RUBBER[2])  # foot pocket
        rect(img, x0, 8, x0 + 6, 14, TEAL[2])  # fin
        for y in range(8, 14):
            plot(img, [(x0 + 1, y), (x0 + 3, y)], TEAL[3])
        for x in range(x0, x0 + 6):
            plot(img, [(x, 13)], TEAL[4])
        plot(img, [(x0 + 1, 3), (x0 + 2, 3)], RUBBER[4])
    outline(img, INK[0])
    save(img, "item/scuba_flippers")


# ---------------------------------------------------------------- scuba armor layers

def layer_1():
    """Helmet (mask + snorkel), chestplate (harness + twin tank on the back), boots (flippers)."""
    img = Image.new("RGBA", (64, 32), CLEAR)
    # head: strap all round at eye level, mask glass on the front (8..16, 8..16)
    for x in range(0, 32):
        plot(img, [(x, 11), (x, 12)], RUBBER[2])
    rect(img, 8, 9, 16, 14, RUBBER[1])
    rect(img, 9, 10, 15, 13, GLASS[2])
    rect(img, 9, 10, 15, 11, GLASS[4])
    plot(img, [(11, 12), (12, 12)], RUBBER[1])
    for y in range(8, 16):  # snorkel up the right side (0..8)
        plot(img, [(5, y)], YELLOW[3 if y % 2 else 2])
    plot(img, [(5, 3), (5, 4), (5, 5)], YELLOW[3])  # over the top face
    # body front (20..28, 20..32): harness straps and buckle
    for y in range(20, 32):
        plot(img, [(21, y), (26, y)], RUBBER[2])
    for x in range(20, 28):
        plot(img, [(x, 27)], RUBBER[2])
    plot(img, [(23, 27), (24, 27)], IRON[4])
    # body back (32..40, 20..32): twin yellow cylinders
    rect(img, 32, 20, 40, 32, YELLOW[2])
    for y in range(20, 32):
        plot(img, [(32, y), (36, y)], YELLOW[1])
        plot(img, [(33, y), (37, y)], YELLOW[4] if y < 23 else YELLOW[3])
    for x in range(32, 40):
        plot(img, [(x, 24), (x, 29)], RUBBER[2])
    rect(img, 28, 16, 36, 20, YELLOW[2])  # top of tank (body top/bottom strip)
    # body sides: straps
    for y in range(20, 32):
        plot(img, [(17, y), (30, y)], RUBBER[2])
    # boots: lower leg (0..16, 26..32) as rubber fin pockets
    rect(img, 0, 27, 16, 32, RUBBER[2])
    rect(img, 4, 29, 8, 32, TEAL[2])
    for x in range(0, 16):
        plot(img, [(x, 27)], TEAL[3])
    rect(img, 4, 16, 8, 20, TEAL[2])  # sole/fin underside
    rect(img, 8, 16, 12, 20, TEAL[1])
    img.save(TEXTURES / "models" / "armor" / "scuba_layer_1.png")


def layer_2():
    """Leggings: the wetsuit. Neoprene on the legs and waist with teal side stripes and a yellow belt."""
    img = Image.new("RGBA", (64, 32), CLEAR)
    rng = random.Random(5)
    for y in range(20, 32):  # legs (0..16)
        for x in range(0, 16):
            plot(img, [(x, y)], RUBBER[rng.randint(1, 2)])
    for y in range(20, 32):
        plot(img, [(0, y), (8, y)], TEAL[3])
    for y in range(26, 32):  # body lower half (waist)
        for x in range(16, 40):
            plot(img, [(x, y)], RUBBER[rng.randint(1, 2)])
    for x in range(16, 40):
        plot(img, [(x, 26)], YELLOW[3])
    plot(img, [(23, 26), (24, 26)], IRON[4])
    rect(img, 4, 16, 12, 20, RUBBER[1])
    rect(img, 20, 16, 36, 20, RUBBER[1])
    img.save(TEXTURES / "models" / "armor" / "scuba_layer_2.png")


if __name__ == "__main__":
    accents = {"iron": GOLD, "diamond": GOLD, "netherite": GOLD, "celestial": TEAL}
    for tier in ("iron", "diamond", "netherite", "celestial"):
        multitool(tier, TIERS[tier], accents[tier])
    for tier, metal in TIERS.items():
        furnace_side(tier, metal)
        furnace_top(tier, metal)
        furnace_front(tier, metal, False)
        furnace_front(tier, metal, True)
    scuba_mask()
    scuba_tank()
    scuba_wetsuit()
    scuba_flippers()
    (TEXTURES / "models" / "armor").mkdir(parents=True, exist_ok=True)
    layer_1()
    layer_2()
    print("toolkit textures written")
