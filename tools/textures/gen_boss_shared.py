"""Textures for shared boss infrastructure: Boss Altar, Seal Gate, Vestige Upgrade Template."""
import sys, pathlib; sys.path.insert(0, str(pathlib.Path(__file__).parent))
from vestige_palette import *

# Boss altar: ink stone with teal rune ring on top, gold rim on the sides.
top = noise_fill(new(16), INK, seed=401, lo=1, hi=2)
for i in range(16):
    for j in range(16):
        d = ((i - 7.5) ** 2 + (j - 7.5) ** 2) ** 0.5
        if 4.5 < d < 6.0:
            top.putpixel((i, j), rgba(TEAL[4] if (i + j) % 3 else TEAL[3]))
        if d < 1.5:
            top.putpixel((i, j), rgba(VIOLET[4]))
save(top, "block/boss_altar_top")
side = noise_fill(new(16), INK, seed=402, lo=1, hi=2)
for x in range(16):
    side.putpixel((x, 0), rgba(GOLD[3])); side.putpixel((x, 15), rgba(GOLD[1]))
    if x % 4 == 1:
        for y in range(5, 11):
            side.putpixel((x, y), rgba(TEAL[3]))
save(side, "block/boss_altar_side")

# Seal gate: gold bars over violet void, front face with a seal disc.
gate = noise_fill(new(16), VIOLET, seed=403, lo=0, hi=1)
for x in range(16):
    for y in range(16):
        if x % 4 in (0, 1) or y in (0, 15):
            gate.putpixel((x, y), rgba(GOLD[2] if (x + y) % 2 else GOLD[3]))
for x in range(5, 11):
    for y in range(5, 11):
        if ((x - 7.5) ** 2 + (y - 7.5) ** 2) < 9:
            gate.putpixel((x, y), rgba(TEAL[4] if (x + y) % 2 else GOLD[4]))
save(gate, "block/seal_gate_front")
side = noise_fill(new(16), GOLD, seed=404, lo=1, hi=2)
save(side, "block/seal_gate_side")

# Upgrade template: like vanilla templates, ivory slab with teal-gold rune.
tpl = new(16)
for x in range(3, 13):
    for y in range(1, 15):
        tpl.putpixel((x, y), rgba(IVORY[3] if (x + y) % 5 else IVORY[2]))
for x, y in [(7, 4), (8, 4), (6, 5), (9, 5), (6, 6), (9, 6), (7, 7), (8, 7), (7, 8), (8, 8), (6, 9), (9, 9), (7, 10), (8, 10), (7, 11), (8, 11)]:
    tpl.putpixel((x, y), rgba(TEAL[3] if y < 8 else GOLD[3]))
outline(tpl, IVORY[0])
save(tpl, "item/vestige_upgrade_template")
