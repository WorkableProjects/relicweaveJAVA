"""Vestige structures, part 1 (T09): waystone_monolith, archivists_outpost, ruined_aether_aqueduct,
gilded_civic_temple, glassroot_warren, hush_archive, sundered_observatory.

Run: python3 tools/structures/gen_part1.py   (deterministic; regenerates templates, worldgen JSON, loot tables, lang
fragment). Conventions: floor blocks sit at the template's stand level minus one; stand level is the y a player
stands at. Unset positions keep the terrain, so air is written explicitly where space must be cleared.
"""
import json
import math
import random
import sys
import pathlib

sys.path.insert(0, str(pathlib.Path(__file__).parent))
from nbtgen import Template, register_structure, DATA, ROOT  # noqa: E402

AIR = "minecraft:air"
R = "relicweave:"
VS, POL, CARVED, FRAC = R + "vestige_stone", R + "polished_vestige_stone", R + "carved_vestige_stone", R + "fractured_vestige_stone"
VB, CVB, GILDED, RUNE = R + "vestige_bricks", R + "cracked_vestige_bricks", R + "gilded_vestige_bricks", R + "rune_vestige_bricks"
PAVING, PILLAR = R + "concordant_paving", R + "vestige_pillar"
GLOAM_B, GLOAM_S = R + "gloam_bricks", R + "gloam_stone"
LOG, CRYSTAL = R + "glassroot_log", R + "glassroot_crystal"
FALSE_WALL, GLYPH, SEAL, TERMINAL = R + "false_wall", R + "hidden_glyph", R + "glyph_seal", R + "ancient_terminal"
ANCHOR, RIFTGLASS_BLOCK, ORE = R + "gravity_anchor", R + "riftglass_block", R + "aetherite_ore"
LANTERN = "minecraft:lantern"
PERSIST = {"PersistenceRequired": True}


# ---------------------------------------------------------------- helpers

def pick(rng, palette):
    """palette: list of (weight, block, props)."""
    w = [p[0] for p in palette]
    p = rng.choices(palette, weights=w)[0]
    return p[1], p[2]


def mixfill(t, rng, x0, y0, z0, x1, y1, z1, palette):
    for x in range(min(x0, x1), max(x0, x1) + 1):
        for y in range(min(y0, y1), max(y0, y1) + 1):
            for z in range(min(z0, z1), max(z0, z1) + 1):
                b, p = pick(rng, palette)
                t.set(x, y, z, b, p)


def air(t, x0, y0, z0, x1, y1, z1):
    t.fill(x0, y0, z0, x1, y1, z1, AIR)


def room(t, rng, x0, z0, x1, z1, yf, yr, wall, floor, roof):
    """Walled room: floor at yf, roof at yr, perimeter walls between, interior air."""
    mixfill(t, rng, x0, yf, z0, x1, yf, z1, floor)
    for y in range(yf + 1, yr):
        for x in range(x0, x1 + 1):
            for z in range(z0, z1 + 1):
                if x in (x0, x1) or z in (z0, z1):
                    b, p = pick(rng, wall)
                    t.set(x, y, z, b, p)
                else:
                    t.set(x, y, z, AIR)
    mixfill(t, rng, x0, yr, z0, x1, yr, z1, roof)


def ring_cells(r_in, r_out, cx, cz, size):
    for x in range(size):
        for z in range(size):
            d = math.hypot(x - cx, z - cz)
            if r_in <= d <= r_out:
                yield x, z, d


VB_PAL = [(6, VB, None), (3, CVB, None), (1, POL, None)]
GILD_PAL = [(5, GILDED, None), (3, VB, None), (2, CVB, None)]
FLOOR_PAL = [(5, POL, None), (3, VB, None), (2, CVB, None)]


# ---------------------------------------------------------------- loot tables

def _entry(item, lo=1, hi=1, weight=1):
    e = {"type": "minecraft:item", "name": item, "weight": weight}
    if (lo, hi) != (1, 1):
        e["functions"] = [{"function": "minecraft:set_count",
                           "count": {"type": "minecraft:uniform", "min": lo, "max": hi}}]
    return e


def _pool(entries, rolls=1, chance=None):
    p = {"rolls": rolls, "entries": entries}
    if chance is not None:
        p["conditions"] = [{"condition": "minecraft:random_chance", "chance": chance}]
    return p


def write_loot(name, pools):
    path = DATA / "loot_tables" / "chests" / "vestige" / f"{name}.json"
    path.parent.mkdir(parents=True, exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump({"type": "minecraft:chest", "pools": pools}, f, indent=2)
        f.write("\n")


def loot_tables():
    I = lambda n: R + n  # noqa: E731
    write_loot("waystone_monolith", [_pool([_entry(I("inscribed_page"), 1, 2, 4), _entry(I("aetherite_shard"), 1, 3, 4),
                                            _entry("minecraft:bread", 1, 3, 3)], rolls={"type": "minecraft:uniform", "min": 2, "max": 3})])
    write_loot("archivists_outpost", [
        _pool([_entry(I("inscribed_page"), 2, 5)]),
        _pool([_entry(I("aetherite_shard"), 2, 6)]),
        _pool([_entry(I("ward_salve"))], chance=0.6),
        _pool([_entry(I("aetherite_ingot"), 0, 1)]),
    ])
    write_loot("archivists_outpost_cache", [
        _pool([_entry(I("aetherite_shard"), 3, 8)]),
        _pool([_entry(I("inscribed_page"), 1, 3)]),
        _pool([_entry("minecraft:gold_ingot", 1, 3)], chance=0.5),
    ])
    write_loot("ruined_aether_aqueduct_cache", [
        _pool([_entry(I("aurelian_resin"), 2, 4)]),
        _pool([_entry(I("aetherite_ingot"), 1, 2)]),
        _pool([_entry(I("riftglass"), 1, 3)]),
        _pool([_entry(I("mnemonic_filament"))], chance=0.08),
    ])
    write_loot("gilded_civic_temple_sanctum", [
        _pool([_entry(I("aetherite_ingot"), 2, 5)]),
        _pool([_entry(I("reliquary_shard"), 1, 3)]),
        _pool([_entry(I("mnemonic_filament"))], chance=0.25),
        _pool([_entry("minecraft:gold_ingot", 2, 6)]),
    ])
    write_loot("glassroot_warren", [
        _pool([_entry("minecraft:iron_ingot", 1, 3, 4), _entry("minecraft:gold_nugget", 3, 8, 3),
               _entry("minecraft:string", 2, 5, 3), _entry("minecraft:stick", 3, 8, 3),
               _entry(I("aetherite_shard"), 1, 3, 4), _entry(I("inscribed_page"), 1, 2, 2)],
              rolls={"type": "minecraft:uniform", "min": 3, "max": 5}),
        _pool([_entry(I("aurelian_resin"), 1, 3)], chance=0.4),
    ])
    write_loot("hush_archive_vault", [
        _pool([_entry(I("sealed_page"), 1, 3)]),
        _pool([_entry(I("inscribed_page"), 2, 4)]),
        _pool([_entry(I("mnemonic_filament"))], chance=0.15),
        _pool([_entry(I("aetherite_shard"), 1, 4)], chance=0.6),
    ])
    write_loot("sundered_observatory", [
        _pool([_entry(I("riftglass_lens"))]),
        _pool([_entry(I("riftglass"), 2, 4)]),
        _pool([_entry("minecraft:ender_pearl", 1, 3, 3), _entry("minecraft:phantom_membrane", 1, 3, 3),
               _entry("minecraft:chorus_fruit", 2, 5, 3), _entry(I("aetherite_shard"), 2, 5, 4)],
              rolls={"type": "minecraft:uniform", "min": 2, "max": 3}),
    ])


# ---------------------------------------------------------------- 1. waystone_monolith

def waystone_monolith():
    rng = random.Random(40401)
    t = Template(13, 14, 13)
    air(t, 0, 1, 0, 12, 13, 12)
    for x, z, d in ring_cells(0, 6.4, 6, 6, 13):
        if max(abs(x - 6), abs(z - 6)) <= 2:
            continue
        t.set(x, 0, z, POL if rng.random() < 0.15 else PAVING)
    for x in range(4, 9):
        for z in range(4, 9):
            t.set(x, 0, z, CARVED if (x + z) % 2 == 0 else RUNE)
    for x, z in ((4, 4), (8, 4), (4, 8), (8, 8)):
        t.set(x, 1, z, POL)
    for x, z in ((4, 6), (8, 6), (6, 8)):
        t.set(x, 0, z, GLYPH)
    for y in range(1, 5):
        for x in range(5, 8):
            for z in range(5, 8):
                t.set(x, y, z, RUNE if y == 2 else CARVED)
    for y in range(5, 11):
        t.set(6, y, 6, RUNE if y % 3 == 0 else CARVED)
    t.set(6, 11, 6, RIFTGLASS_BLOCK)
    t.set(6, 1, 4, TERMINAL)
    t.barrel(6, 1, 8, "relicweave:chests/vestige/waystone_monolith")
    t.save("waystone_monolith")
    register_structure("waystone_monolith", ["relicweave:crownfall_basin", "relicweave:glassroot_wilds",
                                             "relicweave:aurelian_canopy"], 20, 6, 40400101, y_offset=-1)


# ---------------------------------------------------------------- 2. archivists_outpost

def archivists_outpost():
    rng = random.Random(40402)
    t = Template(18, 8, 11)
    # surface of the template = stand level 1 (floor block y=0)
    air(t, 0, 1, 0, 17, 7, 10)
    room(t, rng, 0, 0, 7, 10, 0, 5, VB_PAL, FLOOR_PAL, VB_PAL)
    room(t, rng, 7, 0, 14, 10, 0, 5, VB_PAL, FLOOR_PAL, VB_PAL)
    # ruin the upper walls and roofs
    for x in range(0, 15):
        for z in range(0, 11):
            if t.get(x, 5, z) and rng.random() < 0.45:
                t.set(x, 5, z, AIR)
            if x in (0, 7, 14) or z in (0, 10):
                for y, p in ((4, 0.30), (3, 0.10)):
                    if t.get(x, y, z) and rng.random() < p and not (x == 14 and 6 <= z <= 9):
                        t.set(x, y, z, AIR)
    # doors: south of room A, between rooms, east of room B
    air(t, 3, 1, 10, 4, 3, 10)
    air(t, 7, 1, 4, 7, 3, 5)
    air(t, 14, 1, 2, 14, 3, 3)
    # cache closet behind the east wall of room B
    mixfill(t, rng, 14, 0, 6, 17, 4, 10, VB_PAL)
    air(t, 15, 1, 7, 16, 2, 9)
    t.set(14, 1, 8, FALSE_WALL)
    t.set(14, 2, 8, FALSE_WALL)
    t.set(14, 1, 7, GLYPH)
    t.chest(16, 1, 8, "relicweave:chests/vestige/archivists_outpost_cache", "west")
    # main chest and mobs
    t.chest(1, 1, 1, "relicweave:chests/vestige/archivists_outpost", "south")
    t.set(6, 1, 9, LANTERN, {"hanging": "false"})
    t.set(8, 1, 1, LANTERN, {"hanging": "false"})
    t.entity(3.5, 1, 4.5, "relicweave:reliquary_sentinel", PERSIST)
    t.entity(11.5, 1, 5.5, "relicweave:reliquary_sentinel", PERSIST)
    t.save("archivists_outpost")
    register_structure("archivists_outpost", ["relicweave:crownfall_basin", "relicweave:hushfen"], 24, 8, 40400102,
                       y_offset=-1)


# ---------------------------------------------------------------- 3. ruined_aether_aqueduct

def ruined_aether_aqueduct():
    rng = random.Random(40403)
    t = Template(40, 18, 10)
    air(t, 0, 1, 0, 39, 17, 9)
    piers = [2, 10, 18, 26, 34]
    deck_end = 38
    broken = (21, 22)       # two-block gap in the deck (jumpable)
    for c in piers:
        for y in range(0, 13):
            for x in range(c - 1, c + 2):
                for z in range(2, 7):
                    b, p = pick(rng, VB_PAL)
                    t.set(x, y, z, b, p)
    # arches in the spans between piers
    for a, b_ in zip(piers, piers[1:]):
        cx = (a + b_) / 2.0
        for x in range(a + 2, b_ - 1):
            for y in range(8, 13):
                if (x - cx) ** 2 + (y - 8) ** 2 > 6.25:
                    for z in range(2, 7):
                        bl, pr = pick(rng, VB_PAL)
                        t.set(x, y, z, bl, pr)
    # deck (floor y=13), channel walls (y=14,15)
    for x in range(0, deck_end + 1):
        if broken[0] <= x <= broken[1]:
            continue
        for z in range(1, 8):
            b, p = pick(rng, VB_PAL)
            t.set(x, 13, z, b, p)
        for z in (1, 7):
            for y in (14, 15):
                if rng.random() < (0.25 if y == 15 else 0.08):
                    continue
                b, p = pick(rng, VB_PAL)
                t.set(x, y, z, b, p)
    # the broken span: clear arch fill under the gap and leave stubs
    for x in range(broken[0] - 1, broken[1] + 2):
        for y in range(8, 13):
            for z in range(2, 7):
                if t.get(x, y, z) and not (x in (broken[0] - 1, broken[1] + 1) and y <= 9):
                    t.set(x, y, z, AIR)
    # ladder column inside the second pier (x=10)
    air(t, 10, 1, 3, 10, 12, 3)
    air(t, 10, 1, 2, 10, 2, 2)
    for y in range(1, 14):
        t.set(10, y, 3, "minecraft:ladder", {"facing": "east"})
    # cache alcove behind a false wall in the south channel wall (x=30)
    mixfill(t, rng, 29, 13, 8, 31, 13, 9, VB_PAL)
    for x, z in ((29, 8), (31, 8), (30, 9), (29, 9), (31, 9)):
        for y in (14, 15):
            t.set(x, y, z, VB)
    for x in (29, 30, 31):
        for z in (8, 9):
            t.set(x, 16, z, VB)
    for x in (29, 30, 31):
        t.set(x, 16, 7, VB)
    air(t, 30, 14, 8, 30, 15, 8)
    t.set(30, 14, 7, FALSE_WALL)
    t.set(30, 15, 7, FALSE_WALL)
    t.set(28, 15, 7, GLYPH)
    t.chest(30, 14, 8, "relicweave:chests/vestige/ruined_aether_aqueduct_cache", "north")
    t.set(36, 14, 2, LANTERN, {"hanging": "false"})
    t.entity(36.5, 14, 4.5, "relicweave:scriptbound_scribe", PERSIST)
    t.entity(19.5, 15, 4.5, "relicweave:nullwing", PERSIST)
    t.save("ruined_aether_aqueduct")
    register_structure("ruined_aether_aqueduct", ["relicweave:aurelian_canopy", "relicweave:crownfall_basin"], 32, 10,
                       40400103, y_offset=-1)


# ---------------------------------------------------------------- 4. gilded_civic_temple

def gilded_civic_temple():
    rng = random.Random(40404)
    t = Template(31, 14, 25)
    air(t, 0, 1, 0, 30, 13, 24)
    # portico plinth and columns
    mixfill(t, rng, 0, 0, 2, 2, 0, 22, FLOOR_PAL)
    for z in (4, 8, 16, 20):
        for y in range(1, 7):
            t.set(1, y, z, PILLAR, {"axis": "y"})
    mixfill(t, rng, 0, 7, 3, 2, 7, 21, VB_PAL)
    for z in range(3, 22):
        for x in range(0, 3):
            if rng.random() < 0.25:
                t.set(x, 7, z, AIR)
    hall_floor = [(4, VB, None), (3, POL, None), (2, CVB, None)]
    sanct_floor = [(1, PAVING, None), (1, GILDED, None)]
    room(t, rng, 3, 2, 11, 22, 0, 7, GILD_PAL, hall_floor, VB_PAL)
    room(t, rng, 11, 3, 29, 21, 0, 10, GILD_PAL, sanct_floor, VB_PAL)
    for x in range(12, 29):
        for z in range(4, 21):
            t.set(x, 0, z, PAVING if (x + z) % 2 == 0 else GILDED)
    # doors
    air(t, 3, 1, 10, 3, 5, 14)
    air(t, 11, 1, 11, 11, 4, 13)
    air(t, 29, 1, 11, 29, 4, 13)
    mixfill(t, rng, 30, 0, 10, 30, 0, 14, FLOOR_PAL)
    # hall columns
    for x in (6, 9):
        for z in (7, 17):
            for y in range(1, 7):
                t.set(x, y, z, PILLAR, {"axis": "y"})
    # engaged columns in the sanctum walls
    for x in (14, 18, 22, 26):
        for y in range(1, 10):
            t.set(x, y, 3, PILLAR, {"axis": "y"})
            t.set(x, y, 21, PILLAR, {"axis": "y"})
    for z in (6, 16):
        for y in range(1, 10):
            t.set(29, y, z, PILLAR, {"axis": "y"})
    # damaged walls with aetherite ore deposits
    wall_cells = [(x, y, z) for (x, y, z) in list(t.blocks)
                  if 1 <= y <= 5 and (x in (3, 29) or z in (2, 22, 3, 21)) and t.get(x, y, z) in (GILDED, VB, CVB)]
    wall_cells.sort()
    for (x, y, z) in rng.sample(wall_cells, 16):
        t.set(x, y, z, ORE)
    for (x, y, z) in rng.sample(wall_cells, 8):
        if t.get(x, y, z) != ORE and y >= 3:
            t.set(x, y, z, AIR)
    # clear the sanctum interior again of anything the damage pass touched
    air(t, 12, 1, 4, 28, 9, 20)
    for x in (13, 27):
        for z in (5, 19):
            t.set(x, 1, z, LANTERN, {"hanging": "false"})
    t.chest(28, 1, 5, "relicweave:chests/vestige/gilded_civic_temple_sanctum", "west")
    t.entity(20.5, 1, 12.5, "relicweave:concordance_guardian", PERSIST)
    for x, z in ((5.5, 5.5), (7.5, 12.5), (9.5, 19.5)):
        t.entity(x, 1, z, "relicweave:gilded_husk", PERSIST)
    t.save("gilded_civic_temple")
    register_structure("gilded_civic_temple", ["relicweave:crownfall_basin", "relicweave:aurelian_canopy",
                                               "relicweave:crown_of_concordance"], 36, 12, 40400104,
                       terrain="beard_box", y_offset=-1)


# ---------------------------------------------------------------- 5. glassroot_warren

WALLP = [(40, LOG, {"axis": "y"}), (25, "minecraft:moss_block", None), (20, "minecraft:rooted_dirt", None),
         (10, "minecraft:dirt", None), (5, CRYSTAL, None)]
FLOORP = [(5, "minecraft:moss_block", None), (3, "minecraft:rooted_dirt", None), (2, "minecraft:coarse_dirt", None)]
ROOFP = [(5, LOG, {"axis": "x"}), (3, LOG, {"axis": "z"}), (3, "minecraft:moss_block", None), (1, CRYSTAL, None)]


def glassroot_warren():
    rng = random.Random(40405)
    t = Template(48, 18, 48)
    ramp = [7, 7, 6, 6, 5, 5, 4, 4, 3]     # stand level by distance m from the outer edge (m = 0..8)
    # --- solids
    mixfill(t, rng, 9, 0, 9, 37, 15, 37, WALLP)                       # court mass
    mixfill(t, rng, 11, 0, 1, 44, 7, 6, WALLP)                        # burrow chamber
    mixfill(t, rng, 21, 0, 6, 25, 7, 9, WALLP)                        # north tunnel
    mixfill(t, rng, 40, 0, 1, 44, 7, 25, WALLP)                       # east tunnel
    mixfill(t, rng, 37, 0, 21, 44, 7, 25, WALLP)                      # east corridor to the court
    mixfill(t, rng, 0, 0, 21, 8, 9, 25, WALLP)                        # west ramp mass
    mixfill(t, rng, 21, 0, 38, 25, 9, 47, WALLP)                      # south ramp mass
    # --- carve
    air(t, 10, 2, 10, 36, 14, 36)                                     # Root-Crown Court
    air(t, 12, 2, 2, 43, 6, 5)                                        # burrow chamber
    air(t, 22, 2, 6, 24, 5, 9)                                        # north tunnel (and court door at z=9)
    air(t, 41, 2, 2, 43, 5, 24)                                       # east tunnel
    air(t, 37, 2, 22, 40, 5, 24)                                      # east corridor, court door at x=37
    for m in range(9):
        s = ramp[m]
        x = m
        z = 47 - m
        top = 17 if m <= 4 else s + 3
        air(t, x, s, 22, x, top, 24)
        air(t, 22, s, z, 24, top, z)
        # floor of the ramp
        for zz in (22, 23, 24):
            t.set(x, s - 1, zz, "minecraft:rooted_dirt")
        for xx in (22, 23, 24):
            t.set(xx, s - 1, z, "minecraft:rooted_dirt")
    air(t, 9, 2, 22, 9, 5, 24)                                        # west court door
    air(t, 22, 2, 37, 24, 5, 37)                                      # south court door
    air(t, 22, 2, 38, 24, 5, 38)
    # --- floors, roof
    mixfill(t, rng, 10, 1, 10, 36, 1, 36, FLOORP)
    for (x0, z0, x1, z1) in ((12, 2, 43, 5), (22, 6, 24, 9), (41, 6, 43, 24), (37, 22, 40, 24)):
        mixfill(t, rng, x0, 1, z0, x1, 1, z1, FLOORP)
    mixfill(t, rng, 9, 15, 9, 37, 15, 37, ROOFP)
    mixfill(t, rng, 9, 16, 9, 37, 16, 37, [(1, "minecraft:moss_block", None)])
    # --- pillars in the court (cover)
    for x, z in ((15, 15), (31, 15), (15, 31), (31, 31), (23, 16), (23, 30), (16, 23), (30, 23)):
        for y in range(2, 15):
            t.set(x, y, z, LOG, {"axis": "y"})
        t.set(x, 8, z, CRYSTAL)
    # --- chamber details
    for x in (18, 36):
        for y in range(2, 7):
            t.set(x, y, 3, LOG, {"axis": "y"})
    for x, z in ((12, 5), (43, 5), (30, 5), (43, 20)):
        t.set(x, 2, z, LANTERN, {"hanging": "false"})
    for x, z in ((11, 11), (35, 11), (11, 35), (35, 35)):
        t.set(x, 2, z, LANTERN, {"hanging": "false"})
    t.chest(13, 2, 2, "relicweave:chests/vestige/glassroot_warren", "south")
    t.chest(42, 2, 2, "relicweave:chests/vestige/glassroot_warren", "south")
    t.entity(27.5, 2, 3.5, "relicweave:archive_mimic", dict(PERSIST, Dormant=True))
    # --- altar at the court edge, boss spawns at the centre (23, 2, 23)
    t.altar(12, 2, 12, "relicweave:thorned_regent", spawn_offset=(11, 0, 11))
    # --- mobs
    t.entity(30.5, 2, 3.5, "relicweave:glassroot_stalker", PERSIST)
    t.entity(42.5, 2, 14.5, "relicweave:glassroot_stalker", PERSIST)
    t.entity(23.5, 2, 7.5, "relicweave:glassroot_stalker", PERSIST)
    t.save("glassroot_warren")
    register_structure("glassroot_warren", ["relicweave:glassroot_wilds"], 40, 14, 40400105, terrain="beard_box",
                       y_offset=-8)


# ---------------------------------------------------------------- 6. hush_archive

GLOAM_WALL = [(6, GLOAM_B, None), (3, GLOAM_S, None), (1, "minecraft:bookshelf", None)]
GLOAM_FLOOR = [(5, GLOAM_B, None), (3, GLOAM_S, None)]
GLOAM_ROOF = [(5, GLOAM_B, None), (2, GLOAM_S, None)]
WATER = "minecraft:water"


def hush_archive():
    rng = random.Random(40406)
    t = Template(48, 17, 48)
    air(t, 0, 1, 0, 47, 14, 47)
    # west column of library halls: north (z 0..16), middle (16..32), south (32..47)
    room(t, rng, 0, 0, 18, 16, 1, 8, GLOAM_WALL, GLOAM_FLOOR, GLOAM_ROOF)
    room(t, rng, 0, 16, 18, 32, 1, 8, GLOAM_WALL, GLOAM_FLOOR, GLOAM_ROOF)
    room(t, rng, 0, 32, 18, 47, 1, 8, GLOAM_WALL, GLOAM_FLOOR, GLOAM_ROOF)
    # vault in the north-west corner, arena to the east
    room(t, rng, 0, 0, 8, 8, 1, 8, GLOAM_WALL, GLOAM_FLOOR, GLOAM_ROOF)
    room(t, rng, 18, 9, 47, 37, 1, 15, GLOAM_WALL, GLOAM_FLOOR, GLOAM_ROOF)
    mixfill(t, rng, 0, 0, 0, 47, 0, 47, [(1, GLOAM_S, None)])
    # shallow water (1 deep) in the lower halls
    for (z0, z1) in ((17, 31), (33, 46)):
        for x in range(1, 18):
            for z in range(z0, z1 + 1):
                t.set(x, 2, z, WATER)
    # doors (sills stop the water from spreading through doorways between wet and dry areas)
    air(t, 9, 3, 47, 10, 5, 47)       # entrance (south), sill at y=2
    t.fill(9, 2, 47, 10, 2, 47, GLOAM_B)
    air(t, 9, 3, 32, 10, 4, 32)       # south hall -> middle hall (both wet)
    t.fill(9, 2, 32, 10, 2, 32, WATER)
    air(t, 9, 3, 16, 10, 5, 16)       # middle hall -> north hall (dry), sill
    t.fill(9, 2, 16, 10, 2, 16, GLOAM_B)
    air(t, 18, 3, 23, 18, 5, 24)      # middle hall -> arena, sill
    t.fill(18, 2, 23, 18, 2, 24, GLOAM_B)
    air(t, 47, 2, 22, 47, 5, 24)      # arena east exit
    air(t, 32, 2, 37, 34, 5, 37)      # arena south exit
    # forecourt outside the south exit: stand 2 at z 38..39, stairs at z=40, stand 3 (surface) beyond
    for x in range(28, 39):
        for z in range(38, 48):
            t.set(x, 1, z, GLOAM_B if z <= 40 else GLOAM_S)
            for y in range(2, 8):
                t.set(x, y, z, AIR)
        for z in range(41, 48):
            t.set(x, 2, z, GLOAM_B)
        t.set(x, 2, 40, R + "vestige_brick_stairs", {"facing": "south", "half": "bottom", "shape": "straight"})
    # vault: seal door (blocks access from the north hall), hidden second route through a false wall
    t.set(8, 2, 4, SEAL)
    t.set(8, 3, 4, SEAL)
    t.set(4, 2, 8, FALSE_WALL)
    t.set(4, 3, 8, FALSE_WALL)
    t.set(3, 2, 8, GLYPH)
    t.chest(1, 2, 2, "relicweave:chests/vestige/hush_archive_vault", "east")
    t.chest(1, 2, 6, "relicweave:chests/vestige/hush_archive_vault", "east")
    t.entity(4.5, 2, 4.5, "relicweave:sealed_custodian", PERSIST)
    # library furnishings: bookshelf rows along the west wall of the halls
    for (z0, z1) in ((10, 15), (18, 22), (26, 30), (34, 38), (42, 45)):
        for z in range(z0, z1 + 1):
            for y in (2, 3, 4):
                if z0 <= z <= z1 and (z - z0) % 3 != 2:
                    t.set(1, y, z, "minecraft:bookshelf")
    for z in (12, 28):
        for x in (6, 13):
            for y in range(2, 8):
                t.set(x, y, z, GLOAM_B)
    t.set(17, 2, 21, TERMINAL)
    t.entity(14.5, 2, 3.5, "relicweave:archive_mimic", dict(PERSIST, Dormant=True))
    t.entity(5.5, 2, 24.5, "relicweave:echo_weaver", PERSIST)
    t.entity(13.5, 2, 28.5, "relicweave:echo_weaver", PERSIST)
    # Drowned Conservatory (arena interior x 19..46, z 10..36, stand 2, clear y 2..14)
    for x in range(19, 47):
        for z in range(10, 37):
            t.set(x, 1, z, GLOAM_FLOOR[0][1] if (x + z) % 3 else GLOAM_S)
            for y in range(2, 15):
                t.set(x, y, z, AIR)
    for (x0, z0, x1, z1) in ((19, 33, 22, 36), (43, 10, 46, 13), (19, 10, 21, 12)):
        for x in range(x0, x1 + 1):
            for z in range(z0, z1 + 1):
                t.set(x, 1, z, WATER)
    for x, z in ((24, 16), (41, 16), (24, 30), (41, 30), (28, 23), (37, 23)):
        for y in range(2, 15):
            t.set(x, y, z, PILLAR, {"axis": "y"})
    for x in range(19, 47):
        for z in range(10, 37):
            if (x * 7 + z * 3) % 11 == 0 and 14 <= z <= 32 and 22 <= x <= 44:
                t.set(x, 15, z, RIFTGLASS_BLOCK)
    t.altar(33, 2, 10, "relicweave:choir_unheard", spawn_offset=(0, 0, 13))
    t.save("hush_archive")
    register_structure("hush_archive", ["relicweave:hushfen", "relicweave:gloam_archive"], 40, 14, 40400106,
                       terrain="beard_box", y_offset=-3)


# ---------------------------------------------------------------- 7. sundered_observatory

def sundered_observatory():
    rng = random.Random(40407)
    t = Template(37, 18, 37)
    plate_pal = [(4, FRAC, None), (3, POL, None), (2, VS, None)]
    ring_pal = [(5, VB, None), (3, CVB, None), (1, FRAC, None)]
    cx = cz = 18
    for x in range(37):
        for z in range(37):
            d = math.hypot(x - cx, z - cz)
            if d <= 17.6:
                air(t, x, 3, z, x, 17, z)
                mixfill(t, rng, x, 0, z, x, 2, z, plate_pal)
            if 13.6 <= d <= 15.6:
                for y in range(3, 8):
                    if y == 7 and rng.random() < 0.25:
                        continue
                    if y == 6 and rng.random() < 0.08:
                        continue
                    b, p = pick(rng, ring_pal)
                    t.set(x, y, z, b, p)
                if abs(x - cx) <= 1 or abs(z - cz) <= 1:
                    air(t, x, 3, z, x, 5, z)   # four gates
            if d < 12.5:
                air(t, x, 1, z, x, 2, z)       # the pit: floor at y=0 (stand 1)
    # platforms along the centre line, each one block higher; stepping stones with gravity anchors in the gaps
    def platform(z0, z1, top):
        for x in range(16, 21):
            for z in range(z0, z1 + 1):
                mixfill(t, rng, x, 0, z, x, top, z, plate_pal)

    platform(25, 30, 2)
    platform(16, 21, 3)
    platform(8, 12, 4)
    for z, top in ((23, 2), (14, 3)):
        mixfill(t, rng, 18, 0, z, 18, top - 1, z, plate_pal)
        t.set(18, top, z, ANCHOR)
    t.set(18, 3, 27, ANCHOR)
    t.set(18, 4, 18, ANCHOR)
    t.set(16, 5, 11, ANCHOR)
    # steps that let a player climb out of the pit onto the ledge (west and east)
    for x in (6, 30):
        mixfill(t, rng, x, 1, 17, x, 1, 19, plate_pal)
    for z in (26, 27, 28):
        for x in (15, 21):
            mixfill(t, rng, x, 1, z, x, 1, z, plate_pal)
    # stairs from the third platform up into the top room
    for z, top in ((10, 5), (9, 6), (8, 7)):
        for x in range(17, 20):
            mixfill(t, rng, x, 0, z, x, top - 1, z, plate_pal)
            t.set(x, top, z, R + "vestige_brick_stairs", {"facing": "north", "half": "bottom", "shape": "straight"})
    # top room (pedestal, floor y=7, walls, roof y=13)
    mixfill(t, rng, 13, 0, 1, 23, 6, 7, plate_pal)
    room(t, rng, 13, 1, 23, 7, 7, 13, ring_pal, [(3, POL, None), (2, PAVING, None)], ring_pal)
    air(t, 17, 8, 7, 19, 10, 7)
    for x in (14, 22):
        for y in (9, 10):
            t.set(x, y, 7, RIFTGLASS_BLOCK)
    t.set(14, 8, 2, LANTERN, {"hanging": "false"})
    t.set(22, 8, 2, LANTERN, {"hanging": "false"})
    t.chest(18, 8, 2, "relicweave:chests/vestige/sundered_observatory", "south")
    t.entity(18.5, 6, 20.5, "relicweave:fracture_wisp", PERSIST)
    t.entity(14.5, 5, 27.5, "relicweave:fracture_wisp", PERSIST)
    t.save("sundered_observatory")
    register_structure("sundered_observatory", ["relicweave:sundered_reach"], 36, 12, 40400107, y_offset=-2)


# ---------------------------------------------------------------- lang fragment

NAMES = {
    "waystone_monolith": "Waystone Monolith",
    "archivists_outpost": "Archivist's Outpost",
    "ruined_aether_aqueduct": "Ruined Aether Aqueduct",
    "gilded_civic_temple": "Gilded Civic Temple",
    "glassroot_warren": "Glassroot Warren",
    "hush_archive": "Hush Archive",
    "sundered_observatory": "Sundered Observatory",
}


def lang_fragment():
    path = ROOT / "tools" / "fragments" / "T09" / "assets" / "relicweave" / "lang" / "en_us.json"
    path.parent.mkdir(parents=True, exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump({f"structure.relicweave.{k}": v for k, v in NAMES.items()}, f, indent=2)
        f.write("\n")


if __name__ == "__main__":
    loot_tables()
    waystone_monolith()
    archivists_outpost()
    ruined_aether_aqueduct()
    gilded_civic_temple()
    glassroot_warren()
    hush_archive()
    sundered_observatory()
    lang_fragment()
    print("part 1 structures generated")
