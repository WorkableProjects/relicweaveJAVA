"""Vestige structures, part 2 (T10): gravity_vault, starfall_forge, cycle_observatory, hall_of_nine_seals,
crown_bastion, crown_of_concordance, rift_scar. Deterministic: run `python3 tools/structures/gen_part2.py`.

Writes templates, worldgen JSON, chest loot tables and the lang fragment (tools/fragments/T10/...).
"""
import json
import math
import random
import sys
import pathlib

sys.path.insert(0, str(pathlib.Path(__file__).parent))
from nbtgen import Template, register_structure, DATA, ROOT  # noqa: E402

R = "relicweave:"
AIR = "minecraft:air"
VB, VBC, VBG, VBR = R + "vestige_bricks", R + "cracked_vestige_bricks", R + "gilded_vestige_bricks", R + "rune_vestige_bricks"
VS, VSP, VSF, VSC = R + "vestige_stone", R + "polished_vestige_stone", R + "fractured_vestige_stone", R + "carved_vestige_stone"
PILLAR = R + "vestige_pillar"
GB, GS = R + "gloam_bricks", R + "gloam_stone"
PAVE = R + "concordant_paving"
RG = R + "riftglass_block"
SLAG = R + "starfall_slag"
PERSIST = {"PersistenceRequired": True}


# ------------------------------------------------------------------ helpers

def room(t, x0, y0, z0, x1, y1, z1, wall, floor=None, ceil=None):
    """Hollow box: walls on every face, air inside, optional floor (y0) and ceiling (y1) materials."""
    t.fill(x0, y0, z0, x1, y1, z1, wall, hollow=True)
    if floor:
        t.fill(x0, y0, z0, x1, y0, z1, floor)
    if ceil:
        t.fill(x0, y1, z0, x1, y1, z1, ceil)


def cut(t, x0, y0, z0, x1, y1, z1):
    t.fill(x0, y0, z0, x1, y1, z1, AIR)


def mob(t, x, y, z, eid, nbt=None):
    data = dict(PERSIST)
    data.update(nbt or {})
    t.entity(x + 0.5, y, z + 0.5, eid, data)


def pillar(t, x, z, y0, y1, block=PILLAR):
    t.fill(x, y0, z, x, y1, z, block)


def arena(t, rng, x0, z0, n, h, wall, floors, ceil, boss, altar_dx=0, exits="new", open_roof=False,
          gate=None, light=None, edge_pillars=True):
    """Boss arena: outer box x0..x0+n+1 / z0..z0+n+1, interior n x n, floor y0, interior y1..h, ceiling y h+1.
    Altar on the south interior row (offset by altar_dx), spawn point = arena centre. Exits (3 wide) on the
    sides in `exits` ('n','s','e','w'); `gate` replaces the n/s doorway openings with seal gates.
    Returns (cx, cz) of the arena centre."""
    x1, z1 = x0 + n + 1, z0 + n + 1
    cx, cz = x0 + 1 + n // 2, z0 + 1 + n // 2
    room(t, x0, 0, z0, x1, h + 1, z1, wall, None, None if open_roof else ceil)
    for x in range(x0 + 1, x1):
        for z in range(z0 + 1, z1):
            t.set(x, 0, z, floors(x - cx, z - cz, rng))
    if light and not open_roof:
        for x in range(x0 + 4, x1 - 2, 6):
            for z in range(z0 + 4, z1 - 2, 6):
                t.set(x, h + 1, z, light)
    if edge_pillars:
        for off in (-12, -6, 6, 12):
            for (px, pz) in ((cx + off, z0 + 1), (cx + off, z1 - 1), (x0 + 1, cz + off), (x1 - 1, cz + off)):
                if pz == z1 - 1 and abs(px - (cx + altar_dx)) <= 1:
                    continue
                pillar(t, px, pz, 1, h)
    for side in exits:
        if side == "n":
            xs = (cx - 1, 1, z0, cx + 1, 4, z0)
        elif side == "s":
            xs = (cx - 1, 1, z1, cx + 1, 4, z1)
        elif side == "w":
            xs = (x0, 1, cz - 1, x0, 4, cz + 1)
        else:
            xs = (x1, 1, cz - 1, x1, 4, cz + 1)
        if gate and side in gate:
            t.fill(xs[0], 4, xs[2], xs[3], 4, xs[5], wall)      # closed lintel row above the gate line
            t.fill(xs[0], 1, xs[2], xs[3], 3, xs[5], R + "seal_gate", {"facing": "north" if side == "n" else "south"})
        else:
            cut(t, *xs)
    t.altar(cx + altar_dx, 1, z1 - 1, boss, spawn_offset=(-altar_dx, 1, -(n // 2)))
    return cx, cz


def checker(a, b, c=None, c_chance=0.0):
    def f(dx, dz, rng):
        if c and rng.random() < c_chance:
            return c
        return a if ((dx // 3) + (dz // 3)) % 2 == 0 else b
    return f


def rings(a, b, width=3):
    def f(dx, dz, rng):
        return a if int(math.hypot(dx, dz) // width) % 2 == 0 else b
    return f


# ------------------------------------------------------------------ loot tables

def loot(name, pools):
    """pools: list of (item, min, max, chance). chance None = guaranteed."""
    out = []
    for item, lo, hi, chance in pools:
        entry = {"type": "minecraft:item", "name": item}
        if hi > 1 or lo != hi:
            entry["functions"] = [{"function": "minecraft:set_count",
                                   "count": {"type": "minecraft:uniform", "min": lo, "max": hi} if lo != hi else lo}]
        pool = {"rolls": 1, "bonus_rolls": 0, "entries": [entry]}
        if chance is not None:
            pool["conditions"] = [{"condition": "minecraft:random_chance", "chance": chance}]
        out.append(pool)
    path = DATA / "loot_tables" / "chests" / "vestige" / f"{name}.json"
    path.parent.mkdir(parents=True, exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump({"type": "minecraft:chest", "pools": out}, f, indent=2)
        f.write("\n")


def write_loot():
    loot("gravity_vault", [(R + "riftglass", 1, 3, 0.9), (R + "aetherite_ingot", 1, 2, 0.7),
                           (R + "mnemonic_filament", 1, 1, 0.10), ("minecraft:ender_pearl", 1, 3, 0.8)])
    loot("starfall_forge", [(R + "riftglass", 1, 3, 0.9), (R + "aetherite_ingot", 1, 2, 0.7),
                            ("minecraft:netherite_scrap", 1, 1, 0.10), ("minecraft:gold_ingot", 2, 6, 0.9)])
    loot("cycle_observatory", [(R + "riftglass_lens", 1, 1, None), (R + "inscribed_page", 1, 3, 0.9),
                               (R + "mnemonic_filament", 1, 1, 0.12)])
    loot("hall_of_nine_seals_vault", [(R + "vestige_upgrade_template", 1, 1, None), (R + "sealed_page", 1, 3, None)])
    loot("crown_bastion_treasury", [(R + "aetherite_ingot", 3, 6, None), (R + "reliquary_shard", 2, 4, None),
                                    (R + "mnemonic_filament", 1, 1, 0.25), (R + "sealed_page", 1, 1, 0.9)])
    loot("crown_bastion_cache", [(R + "aetherite_ingot", 2, 4, None), (R + "reliquary_shard", 1, 3, 0.8),
                                 (R + "mnemonic_filament", 1, 1, 0.15), (R + "sealed_page", 1, 2, 0.5)])
    loot("rift_scar", [(R + "riftglass", 2, 5, None), (R + "aetherite_dust", 1, 3, 0.9),
                       (R + "mnemonic_filament", 1, 1, 0.15)])


# ------------------------------------------------------------------ 1. gravity_vault

def gravity_vault():
    rng = random.Random(40400201)
    t = Template(45, 17, 45)
    anchor = R + "gravity_anchor"
    cx, cz = arena(t, rng, 7, 14, 29, 14, VBR, checker(VSP, VB, VBC, 0.15), VBR,
                   R + "gravity_maw", exits="new", light="minecraft:sea_lantern")
    for dx in (-8, 8):                                    # the Gravity Maw's safe anchors
        for dz in (-8, 8):
            pillar(t, cx + dx, cz + dz, 1, 14, anchor)
    # entry hall (z 0..14, x 15..29)
    room(t, 15, 0, 0, 29, 7, 14, VB, VSP, VBR)
    for x, z in ((17, 3), (27, 3), (17, 11), (27, 11)):
        pillar(t, x, z, 1, 6)
    cut(t, 21, 1, 0, 23, 4, 0)            # entry
    cut(t, 21, 1, 14, 23, 4, 14)          # to the arena (arena() also cuts its side)
    mob(t, 22, 1, 7, R + "shardback_brute")
    mob(t, 18, 1, 5, R + "gravity_leech")
    mob(t, 26, 1, 9, R + "gravity_leech")
    # shaft tower (x 1..9, z 2..10) with a spiral of anchor ledges
    room(t, 1, 0, 2, 9, 14, 10, VB, VSP, VB)
    ring = [(x, 4) for x in range(3, 8)] + [(7, z) for z in range(5, 9)] + [(x, 8) for x in range(6, 2, -1)] + \
           [(3, z) for z in range(7, 4, -1)]
    for i, (x, z) in enumerate(ring):
        t.set(x, 1 + i // 2, z, anchor)
    lx, lz = ring[-1]
    t.chest(lx, 1 + (len(ring) - 1) // 2 + 1, lz, R + "chests/vestige/gravity_vault", facing="south")
    room(t, 10, 0, 5, 14, 4, 7, VB, VSP, VB)
    cut(t, 9, 1, 6, 9, 3, 6)
    cut(t, 15, 1, 6, 15, 3, 6)
    cut(t, 10, 1, 6, 14, 3, 6)
    t.save("gravity_vault")
    register_structure("gravity_vault", [R + "sundered_reach"], 44, 16, 40400201, terrain="beard_box")


# ------------------------------------------------------------------ 2. starfall_forge

def starfall_forge():
    rng = random.Random(40400202)
    t = Template(47, 19, 47)
    BAS, BLK = "minecraft:smooth_basalt", "minecraft:blackstone"
    pb = "minecraft:polished_blackstone_bricks"
    cx, cz = arena(t, rng, 7, 14, 31, 16, BLK, checker(SLAG, BAS, "minecraft:magma_block", 0.06), None,
                   R + "starforged_colossus", exits="new", open_roof=True)
    # vent pillars: 3x3x4 with a lever on the side facing the centre, ~10 blocks from the centre
    for (dx, dz, lx, lz, face) in ((10, 0, -2, 0, "west"), (-10, 0, 2, 0, "east"),
                                   (0, 10, 0, -2, "north"), (0, -10, 0, 2, "south")):
        px, pz = cx + dx, cz + dz
        t.fill(px - 1, 1, pz - 1, px + 1, 4, pz + 1, BLK)
        t.fill(px - 1, 4, pz - 1, px + 1, 4, pz + 1, SLAG)
        t.set(px + lx, 2, pz + lz, "minecraft:lever", {"face": "wall", "facing": face, "powered": "false"})
    for dx, dz in ((-6, -6), (6, -6), (-6, 6), (6, 6)):         # extra cover pillars
        t.fill(cx + dx, 1, cz + dz, cx + dx + 1, 3, cz + dz + 1, pb if (dx + dz) % 2 else SLAG)
    # forge complex z 0..14
    room(t, 14, 0, 0, 32, 8, 14, BLK, BAS, BLK)
    room(t, 1, 0, 0, 14, 8, 14, BLK, SLAG, BLK)
    room(t, 32, 0, 0, 45, 8, 14, BLK, SLAG, BLK)
    for x0, x1 in ((2, 13), (33, 44)):                    # riftglass windows high on the outer walls
        for x in range(x0 + 1, x1, 3):
            t.set(x, 5, 0, RG)
            t.set(x, 6, 0, RG)
    for x in range(16, 32, 3):
        t.set(x, 5, 0, RG)
        t.set(x, 6, 0, RG)
    cut(t, 22, 1, 0, 24, 4, 0)
    cut(t, 22, 1, 14, 24, 4, 14)
    cut(t, 14, 1, 6, 14, 3, 8)
    cut(t, 32, 1, 6, 32, 3, 8)
    t.fill(16, 1, 2, 16, 2, 4, "minecraft:iron_bars")
    t.fill(30, 1, 10, 30, 2, 12, "minecraft:iron_bars")
    for x, z in ((5, 11), (8, 11), (38, 3), (41, 3)):
        t.set(x, 0, z, "minecraft:magma_block")
    for x, z in ((4, 3), (10, 3), (36, 11), (43, 11)):
        pillar(t, x, z, 1, 6, BLK)
    mob(t, 5, 1, 6, R + "starfall_strider")
    mob(t, 10, 1, 8, R + "starfall_strider")
    mob(t, 36, 1, 6, R + "starfall_strider")
    mob(t, 41, 1, 7, R + "shardback_brute")
    mob(t, 23, 1, 4, R + "starfall_strider")
    t.chest(44, 1, 2, R + "chests/vestige/starfall_forge", facing="west")
    t.save("starfall_forge")
    register_structure("starfall_forge", [R + "starfall_caldera"], 44, 16, 40400202, terrain="beard_box")


# ------------------------------------------------------------------ 3. cycle_observatory

def cycle_observatory():
    t = Template(29, 16, 29)
    c = 14
    for x in range(29):
        for z in range(29):
            r = math.hypot(x - c, z - c)
            if r <= 13.7:
                ring = int(r // 2)
                t.set(x, 0, z, (VBG, VSP, VBR, VSP)[ring % 4] if ring < 6 else VB)
            for y in range(1, 16):
                d = math.sqrt((x - c) ** 2 + y * y + (z - c) ** 2)
                if d < 12.5:
                    t.set(x, y, z, AIR)
                elif d < 13.7:
                    ang = math.atan2(z - c, x - c) % (2 * math.pi)
                    sector = int(ang / (2 * math.pi) * 16)
                    window = (sector % 2 == 0 and 4 <= y <= 8 and y > 2) or (d >= 12.5 and y >= 12 and r < 3.5)
                    t.set(x, y, z, RG if window else (VBR if y % 5 == 0 else VB))
    cut(t, 13, 1, 0, 15, 4, 2)      # north door
    cut(t, 13, 1, 26, 15, 4, 28)    # south door
    for dx, dz in ((-7, -7), (7, -7), (-7, 7), (7, 7)):
        pillar(t, c + dx, c + dz, 1, 7)
    t.set(c + 6, 1, c, R + "ancient_terminal")
    t.chest(c - 6, 1, c, R + "chests/vestige/cycle_observatory", facing="east")
    mob(t, c, 1, c, R + "rune_warden", {"Dormant": True})
    t.save("cycle_observatory")
    register_structure("cycle_observatory", [R + "crownfall_basin", R + "aurelian_canopy", R + "gloam_archive"],
                       40, 14, 40400203, terrain="beard_thin")


# ------------------------------------------------------------------ 4. hall_of_nine_seals

def hall_of_nine_seals():
    rng = random.Random(40400204)
    t = Template(47, 16, 45)
    cx, cz = arena(t, rng, 7, 12, 31, 14, GB, checker(GS, GB, R + "cracked_vestige_bricks", 0.08), GB,
                   R + "warden_of_seals", exits="new", light="minecraft:sea_lantern")
    for k in range(9):                                   # nine seal pedestals around the edge
        ang = math.radians(20 + k * 40)
        px, pz = cx + round(13 * math.cos(ang)), cz + round(13 * math.sin(ang))
        t.set(px, 1, pz, GB)
        t.set(px, 2, pz, R + "glyph_seal")
    room(t, 14, 0, 0, 32, 7, 12, GB, GS, GB)             # middle hall
    room(t, 1, 0, 0, 14, 7, 12, GB, GS, GB)              # vault
    room(t, 32, 0, 0, 45, 7, 12, GB, GS, GB)             # library
    cut(t, 22, 1, 0, 24, 4, 0)
    cut(t, 22, 1, 12, 24, 4, 12)
    cut(t, 14, 1, 5, 14, 3, 7)
    cut(t, 32, 1, 5, 32, 3, 7)
    for z in (2, 3, 4, 8, 9, 10):
        t.fill(15, 1, z, 15, 2, z, "minecraft:bookshelf")
        t.fill(31, 1, z, 31, 2, z, "minecraft:bookshelf")
    for z in (2, 4, 8, 10):
        t.fill(34, 1, z, 43, 2, z, "minecraft:bookshelf")
    cut(t, 36, 1, 2, 36, 2, 2)
    cut(t, 36, 1, 10, 36, 2, 10)
    t.set(2, 1, 6, "minecraft:lectern", {"facing": "east", "has_book": "false", "powered": "false"})
    t.chest(2, 1, 3, R + "chests/vestige/hall_of_nine_seals_vault", facing="east")
    mob(t, 8, 1, 6, R + "sealed_custodian")
    t.save("hall_of_nine_seals")
    register_structure("hall_of_nine_seals", [R + "gloam_archive"], 48, 16, 40400204, terrain="beard_box")


# ------------------------------------------------------------------ 5. crown_bastion

def crown_bastion():
    rng = random.Random(40400205)
    t = Template(45, 14, 45)
    t.fill(0, 0, 0, 44, 7, 44, VB)
    for x in range(45):
        for z in range(45):
            if rng.random() < 0.12 and (x < 3 or z < 3 or x > 41 or z > 41):
                t.set(x, rng.randint(1, 7), z, VBC if rng.random() < 0.5 else VBG)
    cut(t, 3, 1, 3, 41, 12, 41)
    for x in range(3, 42):
        for z in range(3, 42):
            t.set(x, 0, z, PAVE if (x + z) % 7 else VBG)
    for x in range(45):                                   # crenellations
        for z in range(45):
            if (x in (0, 44) or z in (0, 44)) and (x + z) % 2 == 0:
                t.set(x, 8, z, VB)
    for tx, tz in ((0, 0), (36, 0), (0, 36), (36, 36)):   # corner towers
        t.fill(tx, 0, tz, tx + 8, 7, tz + 8, VBG)
        for x in range(tx, tx + 9):
            for z in range(tz, tz + 9):
                if (x in (tx, tx + 8) or z in (tz, tz + 8)) and (x + z) % 2 == 0:
                    t.set(x, 8, z, VBG)
        for px, pz in ((tx, tz), (tx + 8, tz), (tx, tz + 8), (tx + 8, tz + 8)):
            pillar(t, px, pz, 8, 11)
    cut(t, 21, 1, 42, 23, 4, 44)                          # gate
    t.fill(20, 1, 42, 20, 6, 44, VBG)
    t.fill(24, 1, 42, 24, 6, 44, VBG)
    t.fill(20, 5, 42, 24, 7, 44, VBG)
    cut(t, 21, 1, 42, 23, 4, 44)
    cut(t, 0, 1, 21, 2, 3, 23)                            # postern
    cut(t, 42, 1, 21, 44, 3, 23)
    for i in range(1, 8):                                 # stairs up to the wall walk
        t.fill(10 - i, 1, 10, 10 - i, i, 11, VB)
        t.fill(34 + i, 1, 10, 34 + i, i, 11, VB)
    # keep / treasury
    room(t, 13, 0, 3, 31, 7, 16, VBG, PAVE, VBG)
    cut(t, 21, 1, 16, 23, 4, 16)
    t.chest(15, 1, 5, R + "chests/vestige/crown_bastion_treasury", facing="south")
    t.chest(29, 1, 5, R + "chests/vestige/crown_bastion_treasury", facing="south")
    for x in (17, 27):
        pillar(t, x, 8, 1, 6)
    mob(t, 22, 1, 9, R + "sealed_custodian")
    # caches behind false walls (south towers)
    for tx, sign in ((0, 1), (44, -1)):
        def X(x):
            return x if sign == 1 else 44 - x
        cut(t, X(2), 1, 38, X(6), 4, 42)
        cut(t, X(7), 1, 40, X(7), 2, 40)
        t.fill(X(8), 1, 40, X(8), 2, 40, R + "false_wall")
        t.chest(X(3), 1, 40, R + "chests/vestige/crown_bastion_cache", facing="east" if sign == 1 else "west")
    for x, z in ((12, 24), (32, 24), (12, 34), (32, 34)):
        pillar(t, x, z, 1, 5)
    mob(t, 14, 1, 28, R + "oathbreaker_knight")
    mob(t, 30, 1, 28, R + "oathbreaker_knight")
    mob(t, 22, 1, 36, R + "reliquary_sentinel")
    mob(t, 10, 1, 20, R + "reliquary_sentinel")
    mob(t, 34, 1, 20, R + "reliquary_sentinel")
    mob(t, 10, 8, 1, R + "rift_archer")
    mob(t, 34, 8, 1, R + "rift_archer")
    t.save("crown_bastion")
    register_structure("crown_bastion", [R + "crown_of_concordance"], 40, 14, 40400205, terrain="beard_box")


# ------------------------------------------------------------------ 6. crown_of_concordance

def crown_of_concordance():
    rng = random.Random(40400206)
    t = Template(47, 19, 47)
    cx, cz = arena(t, rng, 4, 8, 37, 16, VBR, rings(PAVE, VBG, 3), VSP, R + "archivist_prime",
                   altar_dx=8, exits="ns", gate="ns", light="minecraft:sea_lantern", edge_pillars=False)
    for dx in (-12, 12):                                  # edge pillars away from the gates / altar
        for dz in (-12, 12):
            pillar(t, cx + dx, cz + dz, 1, 16)
    for dx, dz in ((-9, -9), (9, -9), (-9, 9), (9, 9)):   # low cover walls
        t.fill(cx + dx - 2, 1, cz + dz, cx + dx + 2, 2, cz + dz, VBG)
    t.set(5, 1, cz, R + "null_ward")
    t.set(41, 1, cz, R + "null_ward")
    for x in range(47):                                   # outer court
        for z in range(0, 8):
            t.set(x, 0, z, PAVE if rng.random() > 0.15 else VSF)
    cut(t, 0, 1, 0, 46, 12, 7)
    for x in (4, 12, 34, 42):                             # broken alignment towers
        top = rng.randint(3, 9)
        t.fill(x, 1, 3, x + 1, top, 4, VBR)
        t.fill(x - 1, 1, 2, x - 1, 1, 2, VSF)
        t.set(x + 2, 1, 5, VSF)
    for x in range(8, 40, 4):
        t.set(x, 1, 6, VSF if x % 8 == 0 else AIR)
    mob(t, 14, 1, 5, R + "rift_archer")
    mob(t, 32, 1, 5, R + "rift_archer")
    mob(t, 20, 1, 3, R + "reliquary_sentinel")
    mob(t, 26, 1, 3, R + "reliquary_sentinel")
    t.save("crown_of_concordance")
    register_structure("crown_of_concordance", [R + "crown_of_concordance"], 64, 24, 40400206, terrain="beard_box")


# ------------------------------------------------------------------ 7. rift_scar

def rift_scar():
    rng = random.Random(40400207)
    t = Template(17, 9, 17)
    c = 8
    cells = []
    for x in range(17):
        for z in range(17):
            r = math.hypot(x - c, z - c)
            if r <= 8.4:
                fy = int(3 * (r / 8.4) ** 2)
                t.set(x, fy, z, VSF if rng.random() < 0.8 else "minecraft:cobbled_deepslate")
                for y in range(fy + 1, 9):
                    t.set(x, y, z, AIR)
                if 2 <= r <= 6 and (x, z) != (c, c):
                    cells.append((x, z, fy))
    rng.shuffle(cells)
    placed = []
    for x, z, fy in cells:
        if len(placed) == 5:
            break
        if all(abs(x - px) + abs(z - pz) > 3 for px, pz, _ in placed):
            placed.append((x, z, fy))
    assert 4 <= len(placed) <= 6
    for x, z, fy in placed:
        t.set(x, fy + 1, z, R + "rift_fissure")
    for k in range(8):                                    # riftglass cluster ring
        ang = math.radians(k * 45 + 10)
        x, z = c + round(7 * math.cos(ang)), c + round(7 * math.sin(ang))
        if t.get(x, int(3 * (math.hypot(x - c, z - c) / 8.4) ** 2) + 1, z) is None:
            t.set(x, int(3 * (math.hypot(x - c, z - c) / 8.4) ** 2) + 1, z, R + "riftglass_cluster", {"facing": "up", "waterlogged": "false"})
    t.set(c + 2, 1, c - 1, R + "null_ward")
    t.barrel(c, 1, c, R + "chests/vestige/rift_scar")
    t.save("rift_scar")
    register_structure("rift_scar", [R + "starfall_caldera", R + "sundered_reach", R + "crown_of_concordance"],
                       28, 9, 40400207, terrain="beard_thin", y_offset=-3)


def lang_fragment():
    names = {
        "gravity_vault": "Gravity Vault", "starfall_forge": "Starfall Forge", "cycle_observatory": "Cycle Observatory",
        "hall_of_nine_seals": "Hall of Nine Seals", "crown_bastion": "Crown Bastion",
        "crown_of_concordance": "Crown of Concordance", "rift_scar": "Rift Scar",
    }
    path = ROOT / "tools" / "fragments" / "T10" / "assets" / "relicweave" / "lang" / "en_us.json"
    path.parent.mkdir(parents=True, exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump({f"structure.relicweave.{k}": v for k, v in names.items()}, f, indent=2)
        f.write("\n")


if __name__ == "__main__":
    write_loot()
    gravity_vault()
    starfall_forge()
    cycle_observatory()
    hall_of_nine_seals()
    crown_bastion()
    crown_of_concordance()
    rift_scar()
    lang_fragment()
