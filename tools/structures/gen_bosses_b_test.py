"""GameTest templates for T12 (Gravity Maw, Starforged Colossus): a 21 x 8 x 21 stone floor with one Boss Altar.

Writes data/relicweave/structures/bosses_b_maw.nbt and bosses_b_colossus.nbt (flat, no 'vestige/' prefix, so tests can
reference them as plain template names). Deterministic: run `python3 tools/structures/gen_bosses_b_test.py`.
"""
import shutil
import sys
import pathlib

sys.path.insert(0, str(pathlib.Path(__file__).parent))
from nbtgen import Template, DATA  # noqa: E402


def build(name, boss):
    t = Template(21, 8, 21)
    t.fill(0, 1, 0, 20, 7, 20, "minecraft:air")
    t.fill(0, 0, 0, 20, 0, 20, "minecraft:stone")
    # Altar on the south edge; the boss appears 6 blocks in front of it, at arena centre height.
    t.altar(10, 1, 3, boss, spawn_offset=(0, 0, 6))
    saved = t.save("_tmp_" + name)
    target = DATA / "structures" / f"{name}.nbt"
    shutil.move(str(saved), str(target))
    try:
        saved.parent.rmdir()
    except OSError:
        pass   # the vestige/ folder holds other templates
    print("wrote", target)


if __name__ == "__main__":
    build("bosses_b_maw", "relicweave:gravity_maw")
    build("bosses_b_colossus", "relicweave:starforged_colossus")
