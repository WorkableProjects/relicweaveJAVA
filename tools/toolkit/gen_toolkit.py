#!/usr/bin/env python3
"""0.0.4.5 data and assets: Multitools, Super Furnaces and Scuba Gear.

Writes recipes, tags, loot tables, blockstates, models and the English names/tooltips into
assets/relicweave/lang/en_us.json. Deterministic: re-running produces identical files.
Textures come from tools/textures/gen_toolkit.py.
"""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
RES = ROOT / "src" / "main" / "resources"
DATA = RES / "data" / "relicweave"
ASSETS = RES / "assets" / "relicweave"
LANG = ASSETS / "lang" / "en_us.json"
RW = "relicweave:"

FURNACES = ["iron", "gold", "diamond", "netherite", "celestial"]
MULTITOOLS = ["iron", "diamond", "netherite", "celestial"]
SCUBA = ["scuba_mask", "scuba_tank", "scuba_wetsuit", "scuba_flippers"]


def write(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2) + "\n", encoding="utf-8")


def item(i):
    return {"tag": i[1:]} if i.startswith("#") else {"item": i}


def shaped(name, pattern, key, result):
    write(DATA / "recipes" / "toolkit" / f"{name}.json", {
        "type": "minecraft:crafting_shaped", "category": "misc", "pattern": pattern,
        "key": {k: item(v) for k, v in key.items()}, "result": {"item": RW + result}})


def shapeless(name, ingredients, result):
    write(DATA / "recipes" / "toolkit" / f"{name}.json", {
        "type": "minecraft:crafting_shapeless", "category": "equipment",
        "ingredients": [item(i) for i in ingredients], "result": {"item": RW + result}})


def smithing(name, base, addition, result):
    write(DATA / "recipes" / "toolkit" / f"{name}.json", {
        "type": "minecraft:smithing_transform",
        "template": {"item": "minecraft:netherite_upgrade_smithing_template"},
        "base": item(base), "addition": item(addition), "result": {"item": RW + result}})


# ---------------------------------------------------------------- recipes
shapeless("iron_multitool", ["minecraft:iron_sword", "minecraft:iron_axe", "minecraft:iron_pickaxe", "minecraft:iron_ingot"],
          "iron_multitool")
shapeless("diamond_multitool", ["minecraft:diamond_sword", "minecraft:diamond_axe", "minecraft:diamond_pickaxe",
                                "minecraft:diamond"], "diamond_multitool")
smithing("netherite_multitool_smithing", RW + "diamond_multitool", "minecraft:netherite_ingot", "netherite_multitool")
smithing("celestial_multitool_smithing", RW + "netherite_multitool", RW + "celestial_ingot", "celestial_multitool")

shaped("iron_super_furnace", ["III", "IBI", "SSS"],
       {"I": "minecraft:iron_ingot", "B": "minecraft:blast_furnace", "S": "minecraft:smooth_stone"}, "iron_super_furnace")
shaped("gold_super_furnace", ["MGM", "GFG", "MGM"],
       {"M": "minecraft:magma_block", "G": "minecraft:gold_ingot", "F": RW + "iron_super_furnace"}, "gold_super_furnace")
shaped("diamond_super_furnace", ["BDB", "DFD", "BDB"],
       {"B": "minecraft:blaze_rod", "D": "minecraft:diamond", "F": RW + "gold_super_furnace"}, "diamond_super_furnace")
smithing("netherite_super_furnace_smithing", RW + "diamond_super_furnace", "minecraft:netherite_ingot",
         "netherite_super_furnace")
smithing("celestial_super_furnace_smithing", RW + "netherite_super_furnace", RW + "celestial_ingot",
         "celestial_super_furnace")

shaped("scuba_mask", ["LLL", "GSG"],
       {"L": "minecraft:leather", "G": "minecraft:glass_pane", "S": "minecraft:slime_ball"}, "scuba_mask")
shaped("scuba_tank", ["L L", "IBI", "IKI"],
       {"L": "minecraft:leather", "I": "minecraft:iron_ingot", "B": "minecraft:bucket", "K": "minecraft:dried_kelp"},
       "scuba_tank")
shaped("scuba_wetsuit", ["SLS", "L L", "K K"],
       {"S": "minecraft:slime_ball", "L": "minecraft:leather", "K": "minecraft:dried_kelp"}, "scuba_wetsuit")
shaped("scuba_flippers", ["L L", "S S"], {"L": "minecraft:leather", "S": "minecraft:slime_ball"}, "scuba_flippers")

# ---------------------------------------------------------------- tags
write(DATA / "tags" / "blocks" / "mineable" / "multitool.json",
      {"replace": False, "values": ["#minecraft:mineable/pickaxe", "#minecraft:mineable/axe"]})
write(DATA / "tags" / "items" / "super_furnace_multiplied.json",
      {"replace": False, "values": [{"id": "#forge:raw_materials", "required": False},
                                    {"id": "#forge:ores", "required": False}]})
pickaxe = RES / "data" / "minecraft" / "tags" / "blocks" / "mineable" / "pickaxe.json"
tag = json.loads(pickaxe.read_text(encoding="utf-8"))
for f in FURNACES:
    if RW + f + "_super_furnace" not in tag["values"]:
        tag["values"].append(RW + f + "_super_furnace")
write(pickaxe, tag)
for vanilla_tag, ids in (("swords", MULTITOOLS), ("axes", MULTITOOLS), ("pickaxes", MULTITOOLS)):
    write(RES / "data" / "minecraft" / "tags" / "items" / f"{vanilla_tag}.json",
          {"replace": False, "values": [RW + m + "_multitool" for m in ids]})
write(RES / "data" / "minecraft" / "tags" / "items" / "freeze_immune_wearables.json",
      {"replace": False, "values": [RW + "scuba_wetsuit", RW + "scuba_flippers"]})

# ---------------------------------------------------------------- blocks: loot, blockstates, models
for f in FURNACES:
    name = f + "_super_furnace"
    write(DATA / "loot_tables" / "blocks" / f"{name}.json", {"type": "minecraft:block", "pools": [{
        "rolls": 1, "bonus_rolls": 0.0,
        "entries": [{"type": "minecraft:item", "name": RW + name,
                     "functions": [{"function": "minecraft:copy_name", "source": "block_entity"}]}],
        "conditions": [{"condition": "minecraft:survives_explosion"}]}]})
    variants = {}
    for facing, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
        for lit in ("false", "true"):
            v = {"model": f"relicweave:block/{name}" + ("_on" if lit == "true" else "")}
            if y:
                v["y"] = y
            variants[f"facing={facing},lit={lit}"] = v
    write(ASSETS / "blockstates" / f"{name}.json", {"variants": variants})
    for suffix, front in (("", "front"), ("_on", "front_on")):
        write(ASSETS / "models" / "block" / f"{name}{suffix}.json", {
            "parent": "minecraft:block/orientable",
            "textures": {"top": f"relicweave:block/{name}_top", "front": f"relicweave:block/{name}_{front}",
                         "side": f"relicweave:block/{name}_side"}})
    write(ASSETS / "models" / "item" / f"{name}.json", {"parent": f"relicweave:block/{name}"})

for m in MULTITOOLS:
    write(ASSETS / "models" / "item" / f"{m}_multitool.json",
          {"parent": "minecraft:item/handheld", "textures": {"layer0": f"relicweave:item/{m}_multitool"}})
for s in SCUBA:
    write(ASSETS / "models" / "item" / f"{s}.json",
          {"parent": "minecraft:item/generated", "textures": {"layer0": f"relicweave:item/{s}"}})

# ---------------------------------------------------------------- lang
lang = json.loads(LANG.read_text(encoding="utf-8"))
entries = {
    "item.relicweave.iron_multitool": "Iron Multitool",
    "item.relicweave.diamond_multitool": "Diamond Multitool",
    "item.relicweave.netherite_multitool": "Netherite Multitool",
    "item.relicweave.celestial_multitool": "Celestial Multitool",
    "tooltip.relicweave.multitool": "Sword, axe and pickaxe in one",
    "block.relicweave.iron_super_furnace": "Iron Super Furnace",
    "block.relicweave.gold_super_furnace": "Gold Super Furnace",
    "block.relicweave.diamond_super_furnace": "Diamond Super Furnace",
    "block.relicweave.netherite_super_furnace": "Netherite Super Furnace",
    "block.relicweave.celestial_super_furnace": "Celestial Super Furnace",
    "tooltip.relicweave.super_furnace.tier": "Tier %s",
    "tooltip.relicweave.super_furnace.speed": "Smelts %sx faster",
    "tooltip.relicweave.super_furnace.fuel": "Fuel goes %sx as far",
    "tooltip.relicweave.super_furnace.no_fuel": "Needs no fuel",
    "tooltip.relicweave.super_furnace.raw_output": "%sx output from ores and raw materials",
    "tooltip.relicweave.super_furnace.experience": "%sx experience",
    "item.relicweave.scuba_mask": "Scuba Mask",
    "item.relicweave.scuba_tank": "Scuba Air Tank",
    "item.relicweave.scuba_wetsuit": "Scuba Wetsuit",
    "item.relicweave.scuba_flippers": "Scuba Flippers",
    "tooltip.relicweave.scuba_mask": "Clear vision and full mining speed underwater",
    "tooltip.relicweave.scuba_tank": "Never run out of air",
    "tooltip.relicweave.scuba_wetsuit": "Swim faster",
    "tooltip.relicweave.scuba_flippers": "Swim much faster",
    "tooltip.relicweave.scuba.no_armor": "Gives no armor protection",
    "jei.relicweave.info.multitool": "A sword, an axe and a pickaxe in one tool. Mines anything a pickaxe or an axe "
                                     "can, cuts cobwebs, sweeps like a sword, strips logs and disables shields. Takes "
                                     "sword, axe and pickaxe enchantments and has twice its tier's durability.",
    "jei.relicweave.info.super_furnace": "A faster Furnace with the same slots, recipe book and hopper sides. Each "
                                         "tier is crafted from the one before it. Tier I Iron: 2x speed. Tier II "
                                         "Gold: 3x speed, fuel goes 1.25x as far, 2x experience. Tier III Diamond: "
                                         "4x speed, fuel 1.5x, 2x output from ores and raw materials. Tier IV "
                                         "Netherite: 6x speed, fuel 2x, 2x output, 3x experience. Tier V Celestial: "
                                         "10x speed, no fuel needed, 3x output, 3x experience.",
    "jei.relicweave.info.scuba": "Scuba Gear gives no armor protection. Mask: clear underwater vision and full "
                                 "mining speed underwater, even while floating. Air Tank: you never run out of air. "
                                 "Wetsuit and Flippers: swim and walk underwater faster.",
}
lang.update(entries)
LANG.write_text(json.dumps(lang, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
print("toolkit data written")
