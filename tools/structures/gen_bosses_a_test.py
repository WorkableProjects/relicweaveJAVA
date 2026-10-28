"""Generates the GameTest template for the T11 bosses: a 21x6x21 stone floor (data/relicweave/structures/bosses_a_arena.nbt).

Run from the repository root: python3 tools/structures/gen_bosses_a_test.py
"""
import os
import sys
import pathlib

sys.path.insert(0, str(pathlib.Path(__file__).parent))
from nbtgen import Template, DATA

t = Template(21, 6, 21)
t.fill(0, 0, 0, 20, 0, 20, "minecraft:stone")
t.fill(0, 1, 0, 20, 5, 20, "minecraft:air")
written = t.save("bosses_a_arena")
target = DATA / "structures" / "bosses_a_arena.nbt"
os.replace(written, target)
# Remove the (now empty) vestige/ folder if this script created it.
try:
    written.parent.rmdir()
except OSError:
    pass
print("wrote", target)
