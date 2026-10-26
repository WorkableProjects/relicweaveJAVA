"""Procedural structure templates for The Vestige (RelicWeave 0.0.4).

Writes vanilla structure-template NBT files (gzip, DataVersion 3465 = 1.20.1) and the data-driven worldgen JSON that
places them (jigsaw structure with a single-piece pool, structure set, biome tag). No third-party packages needed.

Example (a script in tools/structures/, e.g. gen_part1.py):

    import sys, pathlib; sys.path.insert(0, str(pathlib.Path(__file__).parent))
    from nbtgen import Template, register_structure

    t = Template(9, 8, 9)
    t.fill(0, 0, 0, 8, 0, 8, "relicweave:vestige_bricks")
    t.fill(0, 1, 0, 8, 6, 8, "relicweave:vestige_bricks", hollow=True)
    t.chest(4, 1, 4, "relicweave:chests/vestige/archivists_outpost", facing="south")
    t.entity(2.5, 1, 2.5, "relicweave:reliquary_sentinel", {"PersistenceRequired": True})
    t.altar(4, 1, 6, "relicweave:thorned_regent", spawn_offset=(0, 1, 4))
    t.save("archivists_outpost")                       # data/relicweave/structures/vestige/archivists_outpost.nbt
    register_structure("archivists_outpost", biomes=["relicweave:crownfall_basin"], spacing=24, separation=8,
                       salt=40400101)

Coordinates are template-local (0..size-1). Unset positions are left as they are in the world (structure void):
only blocks you set are placed, so put "minecraft:air" explicitly where the terrain must be cleared.
Block states: t.set(x, y, z, "minecraft:oak_stairs", {"facing": "north", "half": "bottom"}).
"""
import gzip
import io
import json
import struct
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
DATA = ROOT / "src" / "main" / "resources" / "data" / "relicweave"
DATA_VERSION = 3465

# ---------------------------------------------------------------- NBT writer

TAG_END, TAG_BYTE, TAG_SHORT, TAG_INT, TAG_LONG, TAG_FLOAT, TAG_DOUBLE = 0, 1, 2, 3, 4, 5, 6
TAG_BYTE_ARRAY, TAG_STRING, TAG_LIST, TAG_COMPOUND, TAG_INT_ARRAY = 7, 8, 9, 10, 11


class Byte(int):
    pass


class Short(int):
    pass


class Long(int):
    pass


class Float(float):
    pass


class IntArray(list):
    pass


class TypedList(list):
    """A list whose element tag type is given explicitly (needed for empty lists)."""

    def __init__(self, tag_type, items=()):
        super().__init__(items)
        self.tag_type = tag_type


def _tag_type(v):
    if isinstance(v, bool) or isinstance(v, Byte):
        return TAG_BYTE
    if isinstance(v, Short):
        return TAG_SHORT
    if isinstance(v, Long):
        return TAG_LONG
    if isinstance(v, int):
        return TAG_INT
    if isinstance(v, Float):
        return TAG_FLOAT
    if isinstance(v, float):
        return TAG_DOUBLE
    if isinstance(v, str):
        return TAG_STRING
    if isinstance(v, IntArray):
        return TAG_INT_ARRAY
    if isinstance(v, (list, tuple)):
        return TAG_LIST
    if isinstance(v, dict):
        return TAG_COMPOUND
    raise TypeError(f"Unsupported NBT value {v!r}")


def _write_payload(out, t, v):
    if t == TAG_BYTE:
        out.write(struct.pack(">b", int(v)))
    elif t == TAG_SHORT:
        out.write(struct.pack(">h", v))
    elif t == TAG_INT:
        out.write(struct.pack(">i", v))
    elif t == TAG_LONG:
        out.write(struct.pack(">q", v))
    elif t == TAG_FLOAT:
        out.write(struct.pack(">f", v))
    elif t == TAG_DOUBLE:
        out.write(struct.pack(">d", v))
    elif t == TAG_STRING:
        b = v.encode("utf-8")
        out.write(struct.pack(">H", len(b)))
        out.write(b)
    elif t == TAG_INT_ARRAY:
        out.write(struct.pack(">i", len(v)))
        for i in v:
            out.write(struct.pack(">i", i))
    elif t == TAG_LIST:
        et = v.tag_type if isinstance(v, TypedList) else (_tag_type(v[0]) if v else TAG_END)
        out.write(struct.pack(">bi", et, len(v)))
        for item in v:
            _write_payload(out, et, item)
    elif t == TAG_COMPOUND:
        for k, item in v.items():
            it = _tag_type(item)
            out.write(struct.pack(">b", it))
            _write_payload(out, TAG_STRING, k)
            _write_payload(out, it, item)
        out.write(struct.pack(">b", TAG_END))


def write_nbt(path, root):
    buf = io.BytesIO()
    buf.write(struct.pack(">b", TAG_COMPOUND))
    _write_payload(buf, TAG_STRING, "")
    _write_payload(buf, TAG_COMPOUND, root)
    path.parent.mkdir(parents=True, exist_ok=True)
    # mtime=0 keeps output byte-identical across runs.
    with open(path, "wb") as f, gzip.GzipFile(fileobj=f, mode="wb", mtime=0) as gz:
        gz.write(buf.getvalue())


def json_to_nbt(v):
    """Convert plain JSON-like data to NBT values: bool->Byte, int->Int, float->Double, lists/dicts recursively."""
    if isinstance(v, bool):
        return Byte(1 if v else 0)
    if isinstance(v, dict):
        return {k: json_to_nbt(x) for k, x in v.items()}
    if isinstance(v, (list, tuple)) and not isinstance(v, IntArray):
        items = [json_to_nbt(x) for x in v]
        return TypedList(_tag_type(items[0]) if items else TAG_COMPOUND, items)
    return v

# ---------------------------------------------------------------- Template builder


class Template:
    def __init__(self, sx, sy, sz):
        self.size = (sx, sy, sz)
        self.blocks = {}      # (x, y, z) -> (palette key, nbt or None)
        self.entities = []

    def _check(self, x, y, z):
        sx, sy, sz = self.size
        if not (0 <= x < sx and 0 <= y < sy and 0 <= z < sz):
            raise ValueError(f"({x},{y},{z}) outside template size {self.size}")

    def set(self, x, y, z, block, props=None, nbt=None):
        self._check(x, y, z)
        key = (block, tuple(sorted((props or {}).items())))
        self.blocks[(x, y, z)] = (key, nbt)

    def get(self, x, y, z):
        e = self.blocks.get((x, y, z))
        return e[0][0] if e else None

    def fill(self, x0, y0, z0, x1, y1, z1, block, props=None, hollow=False):
        for x in range(min(x0, x1), max(x0, x1) + 1):
            for y in range(min(y0, y1), max(y0, y1) + 1):
                for z in range(min(z0, z1), max(z0, z1) + 1):
                    edge = x in (x0, x1) or y in (y0, y1) or z in (z0, z1)
                    if hollow and not edge:
                        self.set(x, y, z, "minecraft:air")
                    else:
                        self.set(x, y, z, block, props)

    def chest(self, x, y, z, loot_table, facing="north"):
        """A chest with a loot table (rolled when first opened)."""
        self.set(x, y, z, "minecraft:chest", {"facing": facing, "type": "single", "waterlogged": "false"},
                 {"id": "minecraft:chest", "LootTable": loot_table})

    def barrel(self, x, y, z, loot_table, facing="up"):
        self.set(x, y, z, "minecraft:barrel", {"facing": facing, "open": "false"},
                 {"id": "minecraft:barrel", "LootTable": loot_table})

    def altar(self, x, y, z, boss, spawn_offset=(0, 1, 3)):
        """Boss Altar summoning `boss` (entity id) at altar + spawn_offset."""
        self.set(x, y, z, "relicweave:boss_altar", None,
                 {"id": "relicweave:boss_altar", "Boss": boss, "SpawnOffset": IntArray(list(spawn_offset))})

    def entity(self, x, y, z, entity_id, nbt=None):
        """An entity at template-local position (doubles). Use PersistenceRequired so it never despawns."""
        data = json_to_nbt(dict(nbt or {}))
        data["id"] = entity_id
        self.entities.append((float(x), float(y), float(z), data))

    def save(self, name):
        """Writes data/relicweave/structures/vestige/<name>.nbt and returns its path."""
        palette_index = {}
        palette = []
        blocks = []
        for (x, y, z), (key, nbt) in sorted(self.blocks.items(), key=lambda kv: (kv[0][1], kv[0][2], kv[0][0])):
            if key not in palette_index:
                palette_index[key] = len(palette)
                name_, props = key
                entry = {"Name": name_}
                if props:
                    entry["Properties"] = {k: str(v) for k, v in props}
                palette.append(entry)
            b = {"state": palette_index[key], "pos": TypedList(TAG_INT, [x, y, z])}
            if nbt:
                b["nbt"] = json_to_nbt(nbt)
            blocks.append(b)
        entities = []
        for x, y, z, data in self.entities:
            entities.append({
                "pos": TypedList(TAG_DOUBLE, [x, y, z]),
                "blockPos": TypedList(TAG_INT, [int(x // 1), int(y // 1), int(z // 1)]),
                "nbt": data,
            })
        root = {
            "DataVersion": DATA_VERSION,
            "size": TypedList(TAG_INT, list(self.size)),
            "palette": TypedList(TAG_COMPOUND, palette),
            "blocks": TypedList(TAG_COMPOUND, blocks),
            "entities": TypedList(TAG_COMPOUND, entities),
        }
        path = DATA / "structures" / "vestige" / f"{name}.nbt"
        write_nbt(path, root)
        return path

# ---------------------------------------------------------------- Worldgen JSON


def _write_json(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(data, f, indent=2)
        f.write("\n")


def register_structure(name, biomes, spacing, separation, salt, terrain="beard_thin", y_offset=0,
                       step="surface_structures", project_to_surface=True, absolute_y=None, frequency=1.0):
    """Writes worldgen/structure, template_pool, structure_set and tags/worldgen/biome/has_structure/<name>.json.

    biomes: list of biome ids (e.g. "relicweave:hushfen").
    spacing/separation: chunks (random_spread); separation < spacing. salt: unique int per structure.
    terrain: "beard_thin" | "beard_box" | "bury" | "none".
    y_offset: added to the surface height (negative buries the structure), when project_to_surface.
    absolute_y: fixed Y (use with project_to_surface=False, e.g. floating ruins).
    """
    if separation >= spacing:
        raise ValueError("separation must be < spacing")
    start_height = {"absolute": absolute_y if absolute_y is not None else y_offset}
    structure = {
        "type": "minecraft:jigsaw",
        "biomes": f"#relicweave:has_structure/{name}",
        "step": step,
        "spawn_overrides": {},
        "terrain_adaptation": terrain,
        "start_pool": f"relicweave:vestige/{name}",
        "size": 1,
        "start_height": start_height,
        "max_distance_from_center": 80,
        "use_expansion_hack": False,
    }
    if project_to_surface:
        structure["project_start_to_heightmap"] = "WORLD_SURFACE_WG"
    _write_json(DATA / "worldgen" / "structure" / f"{name}.json", structure)
    _write_json(DATA / "worldgen" / "template_pool" / "vestige" / f"{name}.json", {
        "name": f"relicweave:vestige/{name}",
        "fallback": "minecraft:empty",
        "elements": [{
            "weight": 1,
            "element": {
                "element_type": "minecraft:single_pool_element",
                "location": f"relicweave:vestige/{name}",
                "processors": "minecraft:empty",
                "projection": "rigid",
            },
        }],
    })
    placement = {"type": "minecraft:random_spread", "spacing": spacing, "separation": separation, "salt": salt}
    if frequency < 1.0:
        placement["frequency"] = frequency
    _write_json(DATA / "worldgen" / "structure_set" / f"{name}.json", {
        "structures": [{"structure": f"relicweave:{name}", "weight": 1}],
        "placement": placement,
    })
    _write_json(DATA / "tags" / "worldgen" / "biome" / "has_structure" / f"{name}.json", {
        "replace": False, "values": list(biomes)})
