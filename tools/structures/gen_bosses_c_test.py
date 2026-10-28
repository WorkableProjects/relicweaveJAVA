"""Test template for the T13 boss GameTests: a 27x6x27 stone floor (y = 0), air above. Writes
data/relicweave/structures/bosses_c_arena.nbt (a plain test template, not a worldgen structure)."""
import sys, pathlib; sys.path.insert(0, str(pathlib.Path(__file__).parent))
from nbtgen import Template

SIZE = 27
t = Template(SIZE, 6, SIZE)
t.fill(0, 0, 0, SIZE - 1, 0, SIZE - 1, "minecraft:stone")
path = t.save("bosses_c_arena")
target = path.parent.parent / path.name  # data/relicweave/structures/, next to platform.nbt
path.replace(target)
print(target)
