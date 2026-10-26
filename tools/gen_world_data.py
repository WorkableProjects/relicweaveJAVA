import json, os, copy
ROOT = "/home/user/wt-T01"
RES = ROOT + "/src/main/resources"
FRAG = ROOT + "/tools/fragments/T01"
DUMP = "/home/user/relicweaveJAVA/.claude/delegation/v004/assets/dump_overworld.json"
NS = "relicweave"

def w(path, data):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w") as f:
        json.dump(data, f, indent=2)
        f.write("\n")

def data(rel, obj): w(f"{RES}/data/{NS}/{rel}", obj)
def asset(rel, obj): w(f"{RES}/assets/{NS}/{rel}", obj)

# ------------------------------------------------------------ dimension
data("dimension_type/the_vestige.json", {
    "ultrawarm": False, "natural": False, "coordinate_scale": 1.0, "has_skylight": True, "has_ceiling": False,
    "ambient_light": 0.1, "piglin_safe": False, "bed_works": True, "respawn_anchor_works": False, "has_raids": False,
    "min_y": -64, "height": 384, "logical_height": 384, "infiniburn": "#minecraft:infiniburn_overworld",
    "effects": "relicweave:the_vestige",
    "monster_spawn_light_level": {"type": "minecraft:uniform", "value": {"min_inclusive": 0, "max_inclusive": 7}},
    "monster_spawn_block_light_limit": 0})

def rng(a, b=None):
    return [a, b] if b is not None else a

# biome climate points: (T, H, C, E, W)
FULL = (-1, 1)
BIOMES = {
 "crownfall_basin": [((-0.4, 0.4), (-1, 0.05), (-0.25, 0.55), (-0.35, 1), FULL)],
 "glassroot_wilds": [((-0.45, 0.1), (0.05, 1), (-0.25, 0.55), (-0.35, 1), FULL),
                     ((-1, -0.4), (0.05, 1), (-0.25, 0.55), (-0.35, 1), FULL)],
 "aurelian_canopy": [((0.1, 1), (0.05, 1), (-0.25, 0.55), (-0.35, 1), FULL)],
 "hushfen": [((-0.45, 0.45), FULL, (-1, -0.25), FULL, FULL)],
 "sundered_reach": [(FULL, FULL, (-0.1, 0.55), (-1, -0.35), FULL)],
 "starfall_caldera": [((0.4, 1), (-1, 0.05), (-1, 0.55), (-0.35, 1), FULL)],
 "gloam_archive": [((-1, -0.4), (-1, 0.05), (-0.25, 0.55), (-0.35, 1), FULL)],
 "crown_of_concordance": [(FULL, FULL, (CROWN_C0 := 0.6, 1), (-1, -0.35), FULL)],
}
def params(p):
    t, h, c, e, wd = p
    return {"temperature": list(t), "humidity": list(h), "continentalness": list(c), "erosion": list(e),
            "depth": 0.0, "weirdness": list(wd), "offset": 0.0}
biome_entries = []
for b, pts in BIOMES.items():
    for p in pts:
        biome_entries.append({"biome": f"{NS}:{b}", "parameters": params(p)})
data("dimension/the_vestige.json", {
    "type": "relicweave:the_vestige",
    "generator": {"type": "minecraft:noise", "settings": "relicweave:the_vestige",
                  "biome_source": {"type": "minecraft:multi_noise", "biomes": biome_entries}}})

# ------------------------------------------------------------ noise settings
def blk(name, **props):
    d = {"Name": name}
    if props: d["Properties"] = props
    return d
def cond(if_true, then_run): return {"type": "minecraft:condition", "if_true": if_true, "then_run": then_run}
def seq(*rules): return {"type": "minecraft:sequence", "sequence": list(rules)}
def block_rule(name, **props): return {"type": "minecraft:block", "result_state": blk(name, **props)}
def biome_is(b): return {"type": "minecraft:biome", "biome_is": [f"{NS}:{b}"]}
def stone_depth(add, ):
    return {"type": "minecraft:stone_depth", "offset": 0, "add_surface_depth": add, "secondary_depth_range": 0, "surface_type": "floor"}
WATER = {"type": "minecraft:water", "offset": -1, "surface_depth_multiplier": 0, "add_stone_depth": False}
def noise(n, lo, hi=1000.0): return {"type": "minecraft:noise_threshold", "noise": n, "min_threshold": lo, "max_threshold": hi}

def biome_rule(b, top, sub=None, water_check=True, patch=None):
    """top/sub are block names; patch = (block, noise, min)"""
    top_rule = block_rule(top)
    if patch:
        top_rule = seq(cond(noise(patch[1], patch[2]), block_rule(patch[0])), top_rule)
    if water_check:
        top_rule = cond(WATER, top_rule)
    rules = [cond(stone_depth(False), top_rule)]
    if sub:
        rules.append(cond(stone_depth(True), block_rule(sub)))
    return cond(biome_is(b), seq(*rules))

v = lambda n: f"{NS}:{n}"
ground = [
  biome_rule("crownfall_basin", v("aureate_moss"), patch=(v("concordant_paving"), "minecraft:surface_swamp", 0.35)),
  biome_rule("glassroot_wilds", v("aureate_moss")),
  biome_rule("aurelian_canopy", v("aureate_moss")),
  biome_rule("hushfen", v("echo_clay"), sub=v("echo_clay"), water_check=False, patch=("minecraft:mud", "minecraft:surface_swamp", 0.1)),
  biome_rule("sundered_reach", v("fractured_vestige_stone"), water_check=False),
  biome_rule("starfall_caldera", v("starfall_slag"), sub=v("starfall_slag"), water_check=False, patch=("minecraft:basalt", "minecraft:gravel", 0.3)),
  biome_rule("gloam_archive", v("gloam_stone"), water_check=False),
  biome_rule("crown_of_concordance", v("concordant_paving"), water_check=False, patch=(v("vestige_bricks"), "minecraft:calcite", 0.2)),
]
ns = json.load(open(DUMP))
bedrock = ns["surface_rule"]["sequence"][0]
assert "bedrock" in json.dumps(bedrock)
ns["default_block"] = blk(v("vestige_stone"))
ns["default_fluid"] = blk("minecraft:water", level="0")
ns["sea_level"] = 48
ns["aquifers_enabled"] = False
ns["ore_veins_enabled"] = False
ns["legacy_random_source"] = False
ns["surface_rule"] = seq(bedrock, cond({"type": "minecraft:above_preliminary_surface"}, seq(*ground)))
c = BIOMES["crownfall_basin"][0]
ns["spawn_target"] = [{"temperature": list(c[0]), "humidity": list(c[1]), "continentalness": list(c[2]),
                       "erosion": list(c[3]), "weirdness": list(c[4]), "depth": 0.0, "offset": 0.0}]
data("worldgen/noise_settings/the_vestige.json", ns)

# ------------------------------------------------------------ features
def state(name, **props): return blk(name, **props)
def sp(name, **props): return {"type": "minecraft:simple_state_provider", "state": blk(name, **props)}
AIR = {"type": "minecraft:matching_blocks", "blocks": "minecraft:air"}
def cf(name, obj): data(f"worldgen/configured_feature/vestige_{name}.json", obj)
def pf(name, placement, feature=None):
    data(f"worldgen/placed_feature/vestige_{name}.json", {"feature": f"{NS}:vestige_{name}", "placement": placement})
def surface(): return [{"type": "minecraft:in_square"}, {"type": "minecraft:heightmap", "heightmap": "WORLD_SURFACE_WG"}, {"type": "minecraft:biome"}]
def height(lo, hi):
    return {"type": "minecraft:height_range", "height": {"type": "minecraft:uniform", "min_inclusive": {"absolute": lo}, "max_inclusive": {"absolute": hi}}}
def ore(name, size, count, lo, hi):
    cf(name, {"type": "minecraft:ore", "config": {"size": size, "discard_chance_on_air_exposure": 0.0, "targets": [
        {"target": {"predicate_type": "minecraft:block_match", "block": v("vestige_stone")}, "state": blk(v("aetherite_ore"))}]}})
    pf(name, [{"type": "minecraft:count", "count": count}, {"type": "minecraft:in_square"}, height(lo, hi), {"type": "minecraft:biome"}])
ore("ore_aetherite_common", 8, 14, -56, 128)
ore("ore_aetherite", 5, 5, -56, 128)

def patch(name, block, tries, spread, count, below=None):
    survive = {"type": "minecraft:would_survive", "state": blk(v(block))}
    cf(name, {"type": "minecraft:random_patch", "config": {"tries": tries, "xz_spread": spread, "y_spread": 3, "feature": {
        "feature": {"type": "minecraft:simple_block", "config": {"to_place": sp(v(block))}},
        "placement": [{"type": "minecraft:block_predicate_filter", "predicate": {"type": "minecraft:all_of", "predicates": [AIR, survive]}}]}}})
    pf(name, [{"type": "minecraft:count", "count": count}] + surface())
patch("aurelian_bloom", "aurelian_bloom", 20, 6, 2)
patch("aurelian_bloom_dense", "aurelian_bloom", 28, 7, 6)
patch("glassroot_sprout", "glassroot_sprout", 24, 6, 5)
patch("hushfen_reed", "hushfen_reed", 32, 6, 6)

# riftglass clusters
cf("riftglass_cluster", {"type": "minecraft:random_patch", "config": {"tries": 14, "xz_spread": 6, "y_spread": 2, "feature": {
    "feature": {"type": "minecraft:simple_block", "config": {"to_place": sp(v("riftglass_cluster"), facing="up", waterlogged="false")}},
    "placement": [{"type": "minecraft:block_predicate_filter", "predicate": {"type": "minecraft:all_of", "predicates": [
        AIR, {"type": "minecraft:solid", "offset": [0, -1, 0]}]}}]}}})
pf("riftglass_cluster", [{"type": "minecraft:count", "count": 3}] + surface())

def tree(name, log, leaves, base, ra, rb, leaf_h, below, count_obj):
    cf(name, {"type": "minecraft:tree", "config": {
        "trunk_provider": sp(v(log), axis="y"), "trunk_placer": {"type": "minecraft:giant_trunk_placer", "base_height": base, "height_rand_a": ra, "height_rand_b": rb},
        "foliage_provider": sp(v(leaves)), "foliage_placer": {"type": "minecraft:jungle_foliage_placer", "radius": 2, "offset": 0, "height": leaf_h},
        "dirt_provider": sp(v("aureate_moss")), "minimum_size": {"type": "minecraft:two_layers_feature_size", "limit": 1, "lower_size": 1, "upper_size": 2},
        "decorators": [], "ignore_vines": True}})
    pf(name, [{"type": "minecraft:count", "count": count_obj}] + surface() + [
        {"type": "minecraft:block_predicate_filter", "predicate": {"type": "minecraft:matching_blocks", "blocks": v("aureate_moss"), "offset": [0, -1, 0]}}])
tree("glassroot_tree", "glassroot_log", "glassroot_crystal", 10, 2, 6, 2, None, 3)
tree("canopy_tree", "canopy_log", "canopy_leaves", 14, 3, 8, 2, None, 4)

# pillars, rubble, page stacks
def column(name, block, lo, hi, rarity, count=1, **props):
    cf(name, {"type": "minecraft:block_column", "config": {"direction": "up", "allowed_placement": AIR, "prioritize_tip": False,
        "layers": [{"height": {"type": "minecraft:uniform", "value": {"min_inclusive": lo, "max_inclusive": hi}}, "provider": sp(v(block), **props)}]}})
    placement = ([{"type": "minecraft:rarity_filter", "chance": rarity}] if rarity > 1 else []) + [{"type": "minecraft:count", "count": count}] + surface() + [
        {"type": "minecraft:block_predicate_filter", "predicate": {"type": "minecraft:all_of", "predicates": [AIR, {"type": "minecraft:solid", "offset": [0, -1, 0]}]}}]
    pf(name, placement)
column("broken_pillar", "vestige_pillar", 2, 6, 3, axis="y")
column("page_stack", "gloam_bricks", 1, 4, 1, count=2)
def rock(name, block, count_obj, rarity):
    cf(name, {"type": "minecraft:forest_rock", "config": {"state": blk(v(block))}})
    pf(name, ([{"type": "minecraft:rarity_filter", "chance": rarity}] if rarity > 1 else []) + [{"type": "minecraft:count", "count": count_obj}] + surface())
rock("rubble_carved", "carved_vestige_stone", 1, 4)
rock("rubble_cracked", "cracked_vestige_bricks", 1, 4)

# ------------------------------------------------------------ biomes
ORDER = {  # global (step, feature) ordering to avoid feature-cycle problems
    "ore_aetherite_common": 6, "ore_aetherite": 6,
    "glassroot_tree": 9, "canopy_tree": 9, "aurelian_bloom": 9, "aurelian_bloom_dense": 9, "glassroot_sprout": 9, "hushfen_reed": 9,
}
FEATURE_ORDER = list(ORDER.keys())
def features_for(names):
    steps = [[] for _ in range(11)]
    for n in FEATURE_ORDER:
        if n in names:
            steps[ORDER[n]].append(f"{NS}:vestige_{n}")
    while steps and not steps[-1]: steps.pop()
    return steps
CAVE = {"air": ["minecraft:cave"]}
def rgb(r, g, b): return (r << 16) | (g << 8) | b
B = {
 "crownfall_basin": dict(temp=0.7, down=0.3, rain=False, fog=rgb(0xB8,0xC8,0xBE), sky=rgb(0x8F,0xB8,0xD0), water=rgb(0x3F,0x9E,0x9A), wfog=rgb(0x1A,0x4A,0x52),
     grass=rgb(0xC8,0xB4,0x5A), foliage=rgb(0xB0,0xA0,0x48), particle=("minecraft:white_ash",0.002), feats=["ore_aetherite","broken_pillar","rubble_carved","aurelian_bloom"], carvers=CAVE),
 "glassroot_wilds": dict(temp=0.55, down=0.8, rain=True, fog=rgb(0xA4,0xCC,0xCC), sky=rgb(0x86,0xB0,0xCC), water=rgb(0x3A,0xA8,0xB8), wfog=rgb(0x16,0x50,0x60),
     grass=rgb(0x9C,0xC8,0x8C), foliage=rgb(0x8C,0xC0,0xB4), particle=("minecraft:spore_blossom_air",0.004), feats=["ore_aetherite_common","glassroot_tree","glassroot_sprout","rubble_cracked"], carvers=CAVE),
 "aurelian_canopy": dict(temp=0.85, down=0.7, rain=False, fog=rgb(0xD8,0xC8,0x90), sky=rgb(0x9C,0xC0,0xD8), water=rgb(0x4C,0xA8,0x90), wfog=rgb(0x20,0x58,0x48),
     grass=rgb(0xD4,0xB8,0x48), foliage=rgb(0xE0,0xB8,0x3C), particle=("minecraft:spore_blossom_air",0.006), feats=["ore_aetherite","canopy_tree","aurelian_bloom_dense"], carvers=CAVE),
 "hushfen": dict(temp=0.6, down=1.0, rain=True, fog=rgb(0x8C,0xA0,0xA8), sky=rgb(0x74,0x90,0xA8), water=rgb(0x28,0x48,0x68), wfog=rgb(0x10,0x20,0x38),
     grass=rgb(0x6C,0x8C,0x6C), foliage=rgb(0x5C,0x7C,0x68), particle=("minecraft:ash",0.003), feats=["ore_aetherite","hushfen_reed","rubble_carved"], carvers=CAVE),
 "sundered_reach": dict(temp=0.3, down=0.2, rain=False, fog=rgb(0xC8,0xC4,0xB8), sky=rgb(0xA0,0xB8,0xD8), water=rgb(0x4C,0x88,0xB0), wfog=rgb(0x1C,0x3C,0x58),
     grass=rgb(0xB8,0xB0,0x88), foliage=rgb(0xA4,0xA0,0x7C), particle=("minecraft:white_ash",0.004), feats=["ore_aetherite","broken_pillar","rubble_cracked"], carvers=CAVE),
 "starfall_caldera": dict(temp=1.2, down=0.0, rain=False, fog=rgb(0x9A,0x84,0xA8), sky=rgb(0x6C,0x60,0x94), water=rgb(0x58,0x48,0x88), wfog=rgb(0x24,0x1C,0x44),
     grass=rgb(0x9C,0x88,0x70), foliage=rgb(0x88,0x78,0x6C), particle=("minecraft:ash",0.006), feats=["ore_aetherite","riftglass_cluster"], carvers=CAVE),
 "gloam_archive": dict(temp=0.25, down=0.1, rain=False, fog=rgb(0x4C,0x58,0x80), sky=rgb(0x3C,0x4C,0x7C), water=rgb(0x34,0x50,0x88), wfog=rgb(0x10,0x1C,0x40),
     grass=rgb(0x58,0x6C,0x98), foliage=rgb(0x4C,0x60,0x8C), particle=("minecraft:white_ash",0.003), feats=["ore_aetherite","page_stack","broken_pillar"], carvers=CAVE),
 "crown_of_concordance": dict(temp=0.5, down=0.0, rain=False, fog=rgb(0xB4,0xA8,0xC8), sky=rgb(0x80,0x84,0xC0), water=rgb(0x5C,0x70,0xA8), wfog=rgb(0x20,0x28,0x50),
     grass=rgb(0xC0,0xB0,0x70), foliage=rgb(0xA8,0x9C,0x68), particle=("minecraft:white_ash",0.005), feats=["broken_pillar","rubble_carved"], carvers={}),
}
for name, b in B.items():
    ptype, prob = b["particle"]
    data(f"worldgen/biome/{name}.json", {
        "has_precipitation": b["rain"], "temperature": b["temp"], "downfall": b["down"],
        "effects": {"fog_color": b["fog"], "sky_color": b["sky"], "water_color": b["water"], "water_fog_color": b["wfog"],
                    "grass_color": b["grass"], "foliage_color": b["foliage"],
                    "ambient_sound": f"relicweave:ambient.vestige.{name}",
                    "mood_sound": {"sound": "minecraft:ambient.cave", "tick_delay": 6000, "block_search_extent": 8, "offset": 2.0},
                    "music": {"sound": "relicweave:music.vestige", "min_delay": 12000, "max_delay": 24000, "replace_current_music": False},
                    "particle": {"options": {"type": ptype}, "probability": prob}},
        "spawners": {k: [] for k in ["monster", "creature", "ambient", "axolotls", "underground_water_creature", "water_creature", "water_ambient", "misc"]},
        "spawn_costs": {}, "carvers": b["carvers"], "features": features_for(b["feats"])})

# ------------------------------------------------------------ biome tags
def btag(name, ids): data(f"tags/worldgen/biome/{name}.json", {"replace": False, "values": [f"{NS}:{i}" for i in ids]})
btag("is_vestige", list(B.keys()))
btag("rift_saturated", ["starfall_caldera", "crown_of_concordance"])
btag("gravity_shear", ["sundered_reach"])
btag("starfall", ["starfall_caldera"])
btag("hushfield", ["hushfen", "gloam_archive"])

# ============================================================ blocks
def mdl(name, obj): asset(f"models/block/{name}.json", obj)
def item_parent(name, parent=None):
    asset(f"models/item/{name}.json", {"parent": parent or f"{NS}:block/{name}"})
def item_generated(name):
    asset(f"models/item/{name}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"{NS}:block/{name}"}})
def bs(name, obj): asset(f"blockstates/{name}.json", obj)
def tex(n): return f"{NS}:block/{n}"

def cube(name, texname=None, render=None):
    m = {"parent": "minecraft:block/cube_all", "textures": {"all": tex(texname or name)}}
    if render: m["render_type"] = render
    mdl(name, m); bs(name, {"variants": {"": {"model": f"{NS}:block/{name}"}}}); item_parent(name)

PREDEF = ["vestige_stone", "vestige_bricks", "cracked_vestige_bricks", "carved_vestige_stone", "gilded_vestige_bricks",
          "rune_vestige_bricks", "gloam_stone"]
for n in PREDEF: cube(n)
for n in ["polished_vestige_stone", "gloam_bricks", "concordant_paving", "echo_clay", "starfall_slag", "fractured_vestige_stone",
          "aetherite_ore", "canopy_planks"]: cube(n)
cube("riftglass_block", render="minecraft:translucent")
cube("glassroot_crystal", render="minecraft:translucent")
cube("canopy_leaves", render="minecraft:cutout_mipped")
mdl("canopy_leaves", {"parent": "minecraft:block/leaves", "textures": {"all": tex("canopy_leaves")}, "render_type": "minecraft:cutout_mipped"})

# pillars / logs
for n, side, end in [("vestige_pillar", "vestige_pillar", "vestige_pillar_top"), ("glassroot_log", "glassroot_log", "glassroot_log_top"),
                     ("canopy_log", "canopy_log", "canopy_log_top")]:
    mdl(n, {"parent": "minecraft:block/cube_column", "textures": {"end": tex(end), "side": tex(side)}})
    mdl(n + "_horizontal", {"parent": "minecraft:block/cube_column_horizontal", "textures": {"end": tex(end), "side": tex(side)}})
    bs(n, {"variants": {"axis=y": {"model": f"{NS}:block/{n}"},
                        "axis=z": {"model": f"{NS}:block/{n}_horizontal", "x": 90},
                        "axis=x": {"model": f"{NS}:block/{n}_horizontal", "x": 90, "y": 90}}})
    item_parent(n)

# moss: distinct top
mdl("aureate_moss", {"parent": "minecraft:block/cube_bottom_top", "textures": {"top": tex("aureate_moss_top"), "side": tex("aureate_moss_side"), "bottom": tex("vestige_stone")}})
bs("aureate_moss", {"variants": {"": {"model": f"{NS}:block/aureate_moss"}}}); item_parent("aureate_moss")

# stairs / slab / wall (material vestige_bricks)
T = tex("vestige_bricks")
st = {"bottom": T, "top": T, "side": T}
n = "vestige_brick_stairs"
for suf, parent in [("", "stairs"), ("_inner", "inner_stairs"), ("_outer", "outer_stairs")]:
    mdl(n + suf, {"parent": f"minecraft:block/{parent}", "textures": st})
variants = {}
for facing, ybase in [("east", 0), ("south", 90), ("west", 180), ("north", 270)]:
    for half in ["bottom", "top"]:
        for shape in ["straight", "inner_left", "inner_right", "outer_left", "outer_right"]:
            model = n + ("" if shape == "straight" else "_inner" if shape.startswith("inner") else "_outer")
            y = ybase
            if shape != "straight":
                if half == "bottom": y = ybase if shape.endswith("right") else (ybase + 270) % 360
                else: y = (ybase + 90) % 360 if shape.endswith("right") else ybase
            entry = {"model": f"{NS}:block/{model}"}
            if half == "top": entry["x"] = 180
            if y: entry["y"] = y
            if half == "top" or y: entry["uvlock"] = True
            variants[f"facing={facing},half={half},shape={shape}"] = entry
bs(n, {"variants": variants}); item_parent(n)

n = "vestige_brick_slab"
mdl(n, {"parent": "minecraft:block/slab", "textures": st})
mdl(n + "_top", {"parent": "minecraft:block/slab_top", "textures": st})
bs(n, {"variants": {"type=bottom": {"model": f"{NS}:block/{n}"}, "type=top": {"model": f"{NS}:block/{n}_top"},
                    "type=double": {"model": f"{NS}:block/vestige_bricks"}}}); item_parent(n)

n = "vestige_brick_wall"
wt = {"wall": T}
mdl(n + "_post", {"parent": "minecraft:block/template_wall_post", "textures": wt})
mdl(n + "_side", {"parent": "minecraft:block/template_wall_side", "textures": wt})
mdl(n + "_side_tall", {"parent": "minecraft:block/template_wall_side_tall", "textures": wt})
mdl(n + "_inventory", {"parent": "minecraft:block/wall_inventory", "textures": wt})
parts = [{"when": {"up": "true"}, "apply": {"model": f"{NS}:block/{n}_post"}}]
for d, y in [("north", 0), ("east", 90), ("south", 180), ("west", 270)]:
    for h, suf in [("low", "_side"), ("tall", "_side_tall")]:
        ap = {"model": f"{NS}:block/{n}{suf}", "uvlock": True}
        if y: ap["y"] = y
        parts.append({"when": {d: h}, "apply": ap})
bs(n, {"multipart": parts}); item_parent(n, f"{NS}:block/{n}_inventory")

# cross plants
for n, rt in [("glassroot_sprout", "minecraft:cutout"), ("aurelian_bloom", "minecraft:cutout"), ("hushfen_reed", "minecraft:cutout")]:
    mdl(n, {"parent": "minecraft:block/cross", "textures": {"cross": tex(n)}, "render_type": rt})
    bs(n, {"variants": {"": {"model": f"{NS}:block/{n}"}}}); item_generated(n)
# cluster
n = "riftglass_cluster"
mdl(n, {"parent": "minecraft:block/cross", "textures": {"cross": tex(n)}, "render_type": "minecraft:cutout"})
M = f"{NS}:block/{n}"
bs(n, {"variants": {"facing=up": {"model": M}, "facing=down": {"model": M, "x": 180},
    "facing=north": {"model": M, "x": 90}, "facing=east": {"model": M, "x": 90, "y": 90},
    "facing=south": {"model": M, "x": 90, "y": 180}, "facing=west": {"model": M, "x": 90, "y": 270}}})
item_generated(n)

# ------------------------------------------------------------ loot tables
def loot(name, pools): data(f"loot_tables/blocks/{name}.json", {"type": "minecraft:block", "pools": pools})
SURV = [{"condition": "minecraft:survives_explosion"}]
SILK = {"condition": "minecraft:match_tool", "predicate": {"enchantments": [{"enchantment": "minecraft:silk_touch", "levels": {"min": 1}}]}}
def drops_self(name): loot(name, [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": f"{NS}:{name}"}], "conditions": SURV}])
ALL_NEW = ["polished_vestige_stone", "vestige_pillar", "vestige_brick_stairs", "vestige_brick_slab", "vestige_brick_wall", "gloam_bricks",
           "concordant_paving", "aureate_moss", "echo_clay", "starfall_slag", "fractured_vestige_stone", "aetherite_ore", "riftglass_cluster",
           "riftglass_block", "glassroot_log", "glassroot_crystal", "glassroot_sprout", "canopy_log", "canopy_planks", "canopy_leaves",
           "aurelian_bloom", "hushfen_reed"]
for n in PREDEF + ALL_NEW:
    if n in ("vestige_brick_slab", "aureate_moss", "aetherite_ore", "riftglass_cluster", "canopy_leaves"): continue
    drops_self(n)
loot("vestige_brick_slab", [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": f"{NS}:vestige_brick_slab", "functions": [
    {"function": "minecraft:set_count", "count": 2, "add": False, "conditions": [{"condition": "minecraft:block_state_property", "block": f"{NS}:vestige_brick_slab", "properties": {"type": "double"}}]},
    {"function": "minecraft:explosion_decay"}]}]}])
loot("aureate_moss", [{"rolls": 1, "entries": [{"type": "minecraft:alternatives", "children": [
    {"type": "minecraft:item", "conditions": [SILK], "name": f"{NS}:aureate_moss"},
    {"type": "minecraft:item", "conditions": SURV, "name": f"{NS}:vestige_stone"}]}]}])
loot("aetherite_ore", [{"rolls": 1, "entries": [{"type": "minecraft:alternatives", "children": [
    {"type": "minecraft:item", "conditions": [SILK], "name": f"{NS}:aetherite_ore"},
    {"type": "minecraft:item", "name": f"{NS}:aetherite_shard", "functions": [
        {"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": 1, "max": 2}},
        {"function": "minecraft:apply_bonus", "enchantment": "minecraft:fortune", "formula": "minecraft:ore_drops"},
        {"function": "minecraft:explosion_decay"}]}]}]}])
loot("riftglass_cluster", [{"rolls": 1, "entries": [{"type": "minecraft:alternatives", "children": [
    {"type": "minecraft:item", "conditions": [SILK], "name": f"{NS}:riftglass_cluster"},
    {"type": "minecraft:item", "name": f"{NS}:riftglass", "conditions": [{"condition": "minecraft:match_tool", "predicate": {"tag": "minecraft:pickaxes"}}], "functions": [
        {"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": 1, "max": 3}},
        {"function": "minecraft:apply_bonus", "enchantment": "minecraft:fortune", "formula": "minecraft:ore_drops"},
        {"function": "minecraft:explosion_decay"}]},
    {"type": "minecraft:item", "name": f"{NS}:riftglass", "functions": [{"function": "minecraft:set_count", "count": 1}, {"function": "minecraft:explosion_decay"}]}]}]}])
SHEARS_OR_SILK = {"condition": "minecraft:any_of", "terms": [{"condition": "minecraft:match_tool", "predicate": {"items": ["minecraft:shears"]}}, SILK]}
loot("canopy_leaves", [
    {"rolls": 1, "entries": [{"type": "minecraft:item", "name": f"{NS}:canopy_leaves"}], "conditions": [SHEARS_OR_SILK]},
    {"rolls": 1, "entries": [{"type": "minecraft:item", "name": "minecraft:stick", "functions": [
        {"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": 1, "max": 2}}, {"function": "minecraft:explosion_decay"}]}],
     "conditions": [{"condition": "minecraft:inverted", "term": SHEARS_OR_SILK},
                    {"condition": "minecraft:table_bonus", "enchantment": "minecraft:fortune", "chances": [0.02, 0.022222223, 0.025, 0.033333335, 0.1]}]}])

# ------------------------------------------------------------ recipes
def recipe(name, obj): data(f"recipes/vestige/world/{name}.json", obj)
def it(n): return f"{NS}:{n}"
def shaped(name, pattern, key, result, count=1):
    recipe(name, {"type": "minecraft:crafting_shaped", "category": "building", "pattern": pattern,
                  "key": {k: {"item": x} for k, x in key.items()}, "result": {"item": result, "count": count}})
def cut(name, src, result, count=1):
    recipe(name, {"type": "minecraft:stonecutting", "ingredient": {"item": it(src)}, "result": result, "count": count})
shaped("vestige_bricks", ["SS", "SS"], {"S": it("vestige_stone")}, it("vestige_bricks"), 4)
shaped("polished_vestige_stone", ["SS", "SS"], {"S": it("vestige_stone")}, it("polished_vestige_stone"), 4)
shaped("vestige_pillar", ["P", "P"], {"P": it("polished_vestige_stone")}, it("vestige_pillar"), 2)
shaped("vestige_brick_stairs", ["B  ", "BB ", "BBB"], {"B": it("vestige_bricks")}, it("vestige_brick_stairs"), 4)
shaped("vestige_brick_slab", ["BBB"], {"B": it("vestige_bricks")}, it("vestige_brick_slab"), 6)
shaped("vestige_brick_wall", ["BBB", "BBB"], {"B": it("vestige_bricks")}, it("vestige_brick_wall"), 6)
shaped("gloam_bricks", ["SS", "SS"], {"S": it("gloam_stone")}, it("gloam_bricks"), 4)
shaped("concordant_paving", ["SG", "GS"], {"S": it("polished_vestige_stone"), "G": "minecraft:gold_nugget"}, it("concordant_paving"), 4)
recipe("canopy_planks", {"type": "minecraft:crafting_shapeless", "category": "building", "ingredients": [{"item": it("canopy_log")}], "result": {"item": it("canopy_planks"), "count": 4}})
for n in ["polished_vestige_stone", "vestige_bricks", "vestige_pillar", "carved_vestige_stone"]:
    cut(f"stonecut_{n}", "vestige_stone", it(n), 2 if n == "vestige_pillar" else 1)
cut("stonecut_vestige_brick_stairs", "vestige_bricks", it("vestige_brick_stairs"))
cut("stonecut_vestige_brick_slab", "vestige_bricks", it("vestige_brick_slab"), 2)
cut("stonecut_vestige_brick_wall", "vestige_bricks", it("vestige_brick_wall"))
cut("stonecut_cracked_from_bricks", "vestige_bricks", it("cracked_vestige_bricks"))
cut("stonecut_gloam_bricks", "gloam_stone", it("gloam_bricks"))
cut("stonecut_polished_from_bricks", "vestige_bricks", it("polished_vestige_stone"))
def cook(name, kind, src, result, xp, time):
    recipe(name, {"type": f"minecraft:{kind}", "category": "misc", "ingredient": {"item": src}, "result": result, "experience": xp, "cookingtime": time})
cook("smelt_aetherite_ore", "smelting", it("aetherite_ore"), it("aetherite_shard"), 0.7, 200)
cook("blast_aetherite_ore", "blasting", it("aetherite_ore"), it("aetherite_shard"), 0.7, 100)
cook("smelt_aetherite_shard", "smelting", it("aetherite_shard"), it("aetherite_ingot"), 0.7, 200)
cook("blast_aetherite_shard", "blasting", it("aetherite_shard"), it("aetherite_ingot"), 0.7, 100)
recipe("aetherite_dust", {"type": "minecraft:crafting_shapeless", "category": "misc", "ingredients": [{"item": it("aetherite_shard")}], "result": {"item": it("aetherite_dust"), "count": 2}})
shaped("riftglass_block", ["RR", "RR"], {"R": it("riftglass")}, it("riftglass_block"))
recipe("riftglass_from_block", {"type": "minecraft:crafting_shapeless", "category": "misc", "ingredients": [{"item": it("riftglass_block")}], "result": {"item": it("riftglass"), "count": 4}})

# ------------------------------------------------------------ tag + lang + sounds fragments
def ftag(rel, values): w(f"{FRAG}/data/{rel}.json", {"replace": False, "values": values})
def ids(*ns_): return [it(x) for x in ns_]
STONEISH = PREDEF + ["polished_vestige_stone", "vestige_pillar", "vestige_brick_stairs", "vestige_brick_slab", "vestige_brick_wall",
    "gloam_bricks", "concordant_paving", "starfall_slag", "fractured_vestige_stone", "aetherite_ore", "riftglass_cluster", "riftglass_block", "glassroot_crystal"]
both = lambda path, vals: (ftag(f"minecraft/tags/blocks/{path}", vals), ftag(f"minecraft/tags/items/{path}", vals))
ftag("minecraft/tags/blocks/mineable/pickaxe", ids(*STONEISH))
ftag("minecraft/tags/blocks/mineable/axe", ids("glassroot_log", "canopy_log", "canopy_planks"))
ftag("minecraft/tags/blocks/mineable/shovel", ids("echo_clay", "aureate_moss"))
ftag("minecraft/tags/blocks/mineable/hoe", ids("canopy_leaves"))
ftag("minecraft/tags/blocks/needs_iron_tool", ids("aetherite_ore"))
both("logs", ids("glassroot_log", "canopy_log"))
both("leaves", ids("canopy_leaves"))
both("planks", ids("canopy_planks"))
both("walls", ids("vestige_brick_wall"))
both("slabs", ids("vestige_brick_slab"))
both("stairs", ids("vestige_brick_stairs"))
both("small_flowers", ids("aurelian_bloom"))
ftag("minecraft/tags/blocks/base_stone_overworld", ids("vestige_stone"))
ftag("minecraft/tags/blocks/overworld_carver_replaceables", ids("aureate_moss", "echo_clay", "starfall_slag", "fractured_vestige_stone", "gloam_stone", "concordant_paving"))
ftag("forge/tags/blocks/ores", ids("aetherite_ore")); ftag("forge/tags/items/ores", ids("aetherite_ore"))
ftag("forge/tags/blocks/ores/aetherite", ids("aetherite_ore")); ftag("forge/tags/items/ores/aetherite", ids("aetherite_ore"))

LANG = {"vestige_stone": "Vestige Stone", "vestige_bricks": "Vestige Bricks", "cracked_vestige_bricks": "Cracked Vestige Bricks",
 "carved_vestige_stone": "Carved Vestige Stone", "gilded_vestige_bricks": "Gilded Vestige Bricks", "rune_vestige_bricks": "Rune Vestige Bricks",
 "gloam_stone": "Gloam Stone", "polished_vestige_stone": "Polished Vestige Stone", "vestige_pillar": "Vestige Pillar",
 "vestige_brick_stairs": "Vestige Brick Stairs", "vestige_brick_slab": "Vestige Brick Slab", "vestige_brick_wall": "Vestige Brick Wall",
 "gloam_bricks": "Gloam Bricks", "concordant_paving": "Concordant Paving", "aureate_moss": "Aureate Moss", "echo_clay": "Echo Clay",
 "starfall_slag": "Starfall Slag", "fractured_vestige_stone": "Fractured Vestige Stone", "aetherite_ore": "Aetherite Ore",
 "riftglass_cluster": "Riftglass Cluster", "riftglass_block": "Block of Riftglass", "glassroot_log": "Glassroot Log",
 "glassroot_crystal": "Glassroot Crystal", "glassroot_sprout": "Glassroot Sprout", "canopy_log": "Canopy Log", "canopy_planks": "Canopy Planks",
 "canopy_leaves": "Canopy Leaves", "aurelian_bloom": "Aurelian Bloom", "hushfen_reed": "Hushfen Reed"}
lang = {f"block.{NS}.{k}": val for k, val in LANG.items()}
for k, val in {"crownfall_basin": "Crownfall Basin", "glassroot_wilds": "Glassroot Wilds", "aurelian_canopy": "Aurelian Canopy", "hushfen": "Hushfen",
               "sundered_reach": "Sundered Reach", "starfall_caldera": "Starfall Caldera", "gloam_archive": "Gloam Archive",
               "crown_of_concordance": "Crown of Concordance"}.items():
    lang[f"biome.{NS}.{k}"] = val
w(f"{FRAG}/assets/{NS}/lang/en_us.json", lang)

AMB = {"crownfall_basin": ("minecraft:ambient.warped_forest.loop", 0.4, 0.8), "glassroot_wilds": ("minecraft:ambient.crimson_forest.loop", 0.4, 1.0),
       "aurelian_canopy": ("minecraft:ambient.warped_forest.loop", 0.45, 1.2), "hushfen": ("minecraft:ambient.underwater.loop", 0.5, 0.7),
       "sundered_reach": ("minecraft:ambient.basalt_deltas.loop", 0.4, 1.0), "starfall_caldera": ("minecraft:ambient.nether_wastes.loop", 0.45, 0.8),
       "gloam_archive": ("minecraft:ambient.soul_sand_valley.loop", 0.4, 0.8), "crown_of_concordance": ("minecraft:ambient.soul_sand_valley.loop", 0.5, 1.2)}
sounds = {"music.vestige": {"sounds": [{"name": "minecraft:music.end", "type": "event", "volume": 0.7}, {"name": "minecraft:music.nether.soul_sand_valley", "type": "event", "volume": 0.7}]}}
for k, (ev, vol, pitch) in AMB.items():
    sounds[f"ambient.vestige.{k}"] = {"sounds": [{"name": ev, "type": "event", "volume": vol, "pitch": pitch}]}
w(f"{FRAG}/assets/{NS}/sounds.json", sounds)
print("ok")
