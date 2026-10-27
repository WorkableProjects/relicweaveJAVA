#!/usr/bin/env python3
"""Generates recipe-book unlock advancements (data/relicweave/advancements/recipes/**) for every RelicWeave crafting,
smelting, blasting, stonecutting and smithing recipe, the way vanilla does: a recipe unlocks as soon as the player holds
one of its ingredients. The Concordance Gate recipes (and the Traveler's Primer) unlock when the Ender Dragon is
defeated instead; TravelersPrimer also grants them on login to players who defeated it before 0.0.4.

Custom machine recipes (relicweave:transmutation / relicweave:infusion) are not in the recipe book and are skipped;
JEI shows them. Re-running reproduces identical files (old generated files are removed first).
"""
import json
import shutil
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
DATA = ROOT / "src" / "main" / "resources" / "data" / "relicweave"
RECIPES = DATA / "recipes"
OUT = DATA / "advancements" / "recipes"
BOOK_TYPES = {"minecraft:crafting_shaped", "minecraft:crafting_shapeless", "minecraft:smelting", "minecraft:blasting",
              "minecraft:smoking", "minecraft:campfire_cooking", "minecraft:stonecutting", "minecraft:smithing_transform"}
DRAGON_GATED = {"vestige/gate/threshold_sigil", "vestige/gate/concordance_frame", "vestige/gate/gate_keystone",
                "vestige/travelers_primer"}


def ingredients(recipe):
    """Item ids and #tags that appear as ingredients."""
    found = []

    def take(node):
        if isinstance(node, list):
            for n in node:
                take(n)
        elif isinstance(node, dict):
            if "item" in node:
                found.append(node["item"])
            elif "tag" in node:
                found.append("#" + node["tag"])
    for key in ("ingredients", "ingredient", "base", "addition", "template"):
        if key in recipe:
            take(recipe[key])
    if "key" in recipe:
        for v in recipe["key"].values():
            take(v)
    seen = []
    for f in found:
        if f not in seen:
            seen.append(f)
    return seen


def main():
    if OUT.exists():
        shutil.rmtree(OUT)
    count = 0
    for path in sorted(RECIPES.rglob("*.json")):
        recipe = json.loads(path.read_text(encoding="utf-8"))
        if recipe.get("type") not in BOOK_TYPES:
            continue
        rid = path.relative_to(RECIPES).with_suffix("").as_posix()
        criteria = {"has_the_recipe": {"trigger": "minecraft:recipe_unlocked", "conditions": {"recipe": f"relicweave:{rid}"}}}
        if rid in DRAGON_GATED:
            criteria["defeated_dragon"] = {"trigger": "minecraft:player_killed_entity", "conditions": {"entity": [
                {"condition": "minecraft:entity_properties", "entity": "this", "predicate": {"type": "minecraft:ender_dragon"}}]}}
        else:
            for i, ing in enumerate(ingredients(recipe)):
                item = {"tag": ing[1:]} if ing.startswith("#") else {"items": [ing]}
                criteria[f"has_ingredient_{i}"] = {"trigger": "minecraft:inventory_changed", "conditions": {"items": [item]}}
        adv = {
            "parent": "minecraft:recipes/root",
            "criteria": criteria,
            "requirements": [list(criteria)],
            "rewards": {"recipes": [f"relicweave:{rid}"]},
            "sends_telemetry_event": False,
        }
        out = OUT / f"{rid}.json"
        out.parent.mkdir(parents=True, exist_ok=True)
        out.write_text(json.dumps(adv, indent=2) + "\n", encoding="utf-8")
        count += 1
    print(f"wrote {count} recipe unlock advancements")


if __name__ == "__main__":
    main()
