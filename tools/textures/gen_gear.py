"""T14 textures: nine Aetherforged items, armor layers, Reliquary Loom block faces and GUI."""
import random
import sys
import pathlib

sys.path.insert(0, str(pathlib.Path(__file__).parent))
from vestige_palette import *  # noqa: F401,F403
from PIL import Image

METAL = TEAL  # dark -> light
HANDLE = EMBER


def plot(img, pts, color):
    for x, y in pts:
        if 0 <= x < img.width and 0 <= y < img.height:
            img.putpixel((x, y), rgba(color))


def handle(img, x0, y0, n):
    for i in range(n):
        plot(img, [(x0 + i, y0 - i)], HANDLE[1 if i % 2 else 2])
    plot(img, [(x0 + n, y0 - n + 1)], HANDLE[0])


def finish(img, name):
    outline(img, INK[0])
    save(img, "item/" + name)


def sword():
    img = new(16)
    handle(img, 1, 14, 3)
    plot(img, [(3, 10), (4, 11), (5, 12)], GOLD[3])
    plot(img, [(4, 10), (5, 11)], GOLD[2])
    for i in range(9):
        x, y = 5 + i, 10 - i
        plot(img, [(x, y)], METAL[4] if i % 3 == 0 else METAL[3])
        plot(img, [(x, y + 1)], METAL[2])
    plot(img, [(14, 1)], METAL[4])
    plot(img, [(7, 8), (9, 6), (11, 4)], GOLD[3])
    finish(img, "aetherforged_sword")


def pickaxe():
    img = new(16)
    arc = [(3, 6), (3, 5), (4, 4), (5, 3), (6, 3), (7, 2), (8, 2), (9, 2), (10, 3), (11, 3), (12, 4), (13, 5), (13, 6)]
    for x, y in arc:
        plot(img, [(x, y)], METAL[3])
        plot(img, [(x, y + 1)], METAL[1])
    plot(img, [(7, 2), (8, 2), (9, 2)], METAL[4])
    handle(img, 2, 14, 9)
    plot(img, [(8, 5)], GOLD[2])
    finish(img, "aetherforged_pickaxe")


def axe():
    img = new(16)
    handle(img, 2, 14, 8)
    for y in range(2, 9):
        for x in range(6, 14):
            inside = (x - 6) + (y - 2) * 0 <= 7 - abs(y - 5) * 1.2 and x - 6 >= abs(y - 5) * 0.5
            if inside:
                plot(img, [(x, y)], METAL[4 if (y == 2 or x == 6) else (3 if (x + y) % 2 else 2)])
    plot(img, [(12, 6), (13, 7)], METAL[1])
    plot(img, [(7, 5)], GOLD[3])
    handle(img, 2, 14, 6)
    finish(img, "aetherforged_axe")


def shovel():
    img = new(16)
    handle(img, 2, 14, 8)
    for y in range(1, 7):
        for x in range(9, 15):
            if (x - 11.5) ** 2 / 6.0 + (y - 3.5) ** 2 / 7.0 <= 1.0:
                plot(img, [(x, y)], METAL[4 if (x + y) % 5 == 0 else (3 if x < 12 else 2)])
    plot(img, [(10, 7), (9, 8)], GOLD[2])
    finish(img, "aetherforged_shovel")


def hoe():
    img = new(16)
    handle(img, 2, 14, 9)
    for x in range(7, 14):
        plot(img, [(x, 3)], METAL[4])
        plot(img, [(x, 4)], METAL[2])
    plot(img, [(7, 5), (7, 6), (8, 5)], METAL[3])
    plot(img, [(7, 7)], METAL[1])
    plot(img, [(11, 5)], GOLD[2])
    finish(img, "aetherforged_hoe")


ARMOR = {
    "helmet": [
        "................",
        "................",
        "....11111111....",
        "...1333333331...",
        "..133444444331..",
        "..134g2222g431..",
        "..13322222231...",
        "..1332....2331..",
        "..132......231..",
        "..12........21..",
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
    ],
    "chestplate": [
        "................",
        "..111......111..",
        ".13321....12331.",
        ".1334211112433..",
        ".133g433334g31..",
        ".1334433334431..",
        "..13322222331...",
        "..13322gg22331..",
        "..13322gg22331..",
        "..13322222331...",
        "..1332222231....",
        "...1222222111...",
        "...111111111....",
        "................",
        "................",
        "................",
    ],
    "leggings": [
        "................",
        "...1111111111...",
        "..1333g22g3331..",
        "..1333322223341.",
        "..1332222222331.",
        "..13321....1331.",
        "..13321....1331.",
        "..13321....1331.",
        "..13321....1331.",
        "..13321....1331.",
        "..1332g1..1g331.",
        "..11221....1221.",
        "..1111......111.",
        "................",
        "................",
        "................",
    ],
    "boots": [
        "................",
        "................",
        "................",
        "................",
        "................",
        "..1111....1111..",
        "..1332....2331..",
        "..1332....2331..",
        "..13g2....2g31..",
        ".1333211112333..",
        ".1334422224433..",
        ".1111111111111..",
        "................",
        "................",
        "................",
        "................",
    ],
}


def armor_piece(name):
    img = new(16)
    cmap = {"1": METAL[0], "2": METAL[1], "3": METAL[2], "4": METAL[3], "g": GOLD[3]}
    for y, row in enumerate(ARMOR[name]):
        for x, ch in enumerate(row):
            if ch in cmap:
                img.putpixel((x, y), rgba(cmap[ch]))
    # Light catch on the top-left edge.
    for y in range(16):
        for x in range(16):
            px = img.getpixel((x, y))
            if px[3] and px[:3] == METAL[2] and (x + y) % 6 == 0:
                img.putpixel((x, y), rgba(METAL[4]))
    finish(img, "aetherforged_" + name)


def armor_layer(index):
    """64x32 humanoid armor layer: teal-steel plates with gold seams and ink panel lines."""
    img = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    rng = random.Random(1400 + index)
    for y in range(32):
        for x in range(64):
            tone = rng.randint(1, 3)
            if y % 8 == 7:
                color = GOLD[1 if (x // 2) % 2 else 2]
            elif x % 8 == 0:
                color = INK[1]
            else:
                color = METAL[tone]
            img.putpixel((x, y), rgba(color))
    path = TEXTURES / "models" / "armor" / f"aetherforged_layer_{index}.png"
    path.parent.mkdir(parents=True, exist_ok=True)
    img.save(path)


def loom_blocks():
    top = new(16)
    noise_fill(top, GOLD, seed=1410, lo=1, hi=3)
    for i in range(16):
        top.putpixel((i, 0), rgba(GOLD[0]))
        top.putpixel((i, 15), rgba(GOLD[0]))
        top.putpixel((0, i), rgba(GOLD[0]))
        top.putpixel((15, i), rgba(GOLD[0]))
    for y in range(3, 13):  # warp threads
        for x in range(3, 13):
            top.putpixel((x, y), rgba(TEAL[1 if x % 2 else 2]))
    for x in range(3, 13):  # weft
        top.putpixel((x, 7), rgba(OPAL[2]))
        top.putpixel((x, 8), rgba(OPAL[1]))
    save(top, "block/reliquary_loom_top")

    side = new(16)
    bricks(side, IVORY, seed=1411, mortar=IVORY[0])
    for x in range(16):
        side.putpixel((x, 0), rgba(GOLD[2]))
        side.putpixel((x, 1), rgba(GOLD[1]))
    for y in range(4, 12):  # hanging threads
        for x in (4, 7, 10):
            side.putpixel((x, y), rgba(TEAL[2 if y % 2 else 3]))
    for y in range(12, 15):
        for x in (3, 4, 5, 6, 7, 8, 9, 10, 11):
            side.putpixel((x, y), rgba(GOLD[2]))
    save(side, "block/reliquary_loom_side")

    bottom = new(16)
    noise_fill(bottom, IVORY, seed=1412, lo=0, hi=2)
    save(bottom, "block/reliquary_loom_bottom")


def panel(img, x, y, w, h, fill, light, dark):
    for yy in range(y, y + h):
        for xx in range(x, x + w):
            img.putpixel((xx, yy), rgba(fill))
    for xx in range(x, x + w):
        img.putpixel((xx, y), rgba(light))
        img.putpixel((xx, y + h - 1), rgba(dark))
    for yy in range(y, y + h):
        img.putpixel((x, yy), rgba(light))
        img.putpixel((x + w - 1, yy), rgba(dark))


def slot_bg(img, x, y):
    """18x18 slot starting at (x-1, y-1) where (x, y) is the item position."""
    panel(img, x - 1, y - 1, 18, 18, (139, 139, 139), (55, 55, 55), (255, 255, 255))


def loom_gui():
    img = Image.new("RGBA", (256, 256), (0, 0, 0, 0))
    panel(img, 0, 0, 256, 222, (198, 198, 198), (255, 255, 255), (55, 55, 55))
    from_menu = [(10, 22), (10, 46), (10, 70)]
    for x, y in from_menu:
        slot_bg(img, x, y)
    for row in range(3):
        for col in range(9):
            slot_bg(img, 40 + col * 18, 140 + row * 18)
    for col in range(9):
        slot_bg(img, 40 + col * 18, 140 + 58)
    # Recessed text panels: right column and left info column.
    panel(img, 97, 15, 154, 52, (176, 170, 160), (110, 104, 94), (236, 229, 210))
    panel(img, 97, 67, 154, 52, (176, 170, 160), (110, 104, 94), (236, 229, 210))
    panel(img, 5, 91, 90, 46, (176, 170, 160), (110, 104, 94), (236, 229, 210))
    img.save(TEXTURES / "gui" / "container" / "reliquary_loom.png")


if __name__ == "__main__":
    sword()
    pickaxe()
    axe()
    shovel()
    hoe()
    for piece in ARMOR:
        armor_piece(piece)
    armor_layer(1)
    armor_layer(2)
    loom_blocks()
    loom_gui()
