#!/usr/bin/env python3
"""Generates the RelicWeave quest line: two vanilla advancement tabs.

- Tab "RelicWeave" (relicweave:relicweave/*): the machines and 0.0.1-0.0.3 content, ending at the Concordance Gate.
- Tab "The Vestige" (relicweave:vestige/*): the 0.0.4 dimension. Keeps the existing ids vestige/root and
  vestige/<boss> (BossRewards awards those), re-parented into the quest chain.

Writes src/main/resources/data/relicweave/advancements/** and the advancement titles/descriptions into
assets/relicweave/lang/en_us.json. Deterministic: re-running produces identical files.
Custom steps use the relicweave:quest trigger (com.relicweave.quest.QuestEvents).
"""
import collections
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
ADV = ROOT / "src" / "main" / "resources" / "data" / "relicweave" / "advancements"
LANG = ROOT / "src" / "main" / "resources" / "assets" / "relicweave" / "lang" / "en_us.json"

RW = "relicweave:"
LEGACY_BOOKS = ["keen_edge", "purling", "cobbled_up", "oxidation", "conductive", "golemed", "piglinified",
                "villagers_best_friend", "the_necromancer", "necromanced", "upbound"]
ATTUNEMENT_BOOKS = ["reaping", "undertow", "resonance", "soulguard", "steady_draw", "reprisal"]
VESTIGE_BOOKS = ["riftwalker", "glyphfinder", "sovereigns_bane", "cyclebound", "sealbreaker", "archivists_mark"]
ATTRIBUTE_INFUSIONS = ["cobbled", "oxidized", "conductive", "golemed", "piglinified", "villagers_best_friend",
                       "necromancer", "necromanced", "upbound"]
EFFECT_INFUSIONS = ["tidal", "warding", "pursuit", "anchored"]
VESTIGE_INFUSIONS = ["riftward", "sealguard", "cycleweave", "gravitic_tread"]
CELESTIAL_ARMOR = [RW + "celestial_" + p for p in ("helmet", "chestplate", "leggings", "boots")]
CELESTIAL_ALL = CELESTIAL_ARMOR + [RW + "celestial_" + p for p in ("sword", "pickaxe", "axe", "shovel", "hoe")]
CHARMS = [RW + c + "_charm" for c in ("copper", "iron", "gold", "diamond", "netherite")]
TOTEMS = [RW + t + "_totem" for t in ("diamond", "netherite", "ultra")]
RELICS_003 = [RW + r for r in ("lodestone_charm", "tidal_charm", "featherwoven_charm", "wayfarers_relic",
                                "echo_charm", "guardian_relic")]
RELICS_004 = [RW + r for r in ("surveyors_lens", "gravity_nail", "cycle_dial", "riftglass_lantern",
                                "mnemonic_spindle", "sealkeepers_signet")]
AETHERFORGED = [RW + "aetherforged_" + p for p in ("sword", "pickaxe", "axe", "shovel", "hoe", "helmet",
                                                   "chestplate", "leggings", "boots")]
BIOMES = ["crownfall_basin", "glassroot_wilds", "aurelian_canopy", "hushfen", "sundered_reach", "starfall_caldera",
          "gloam_archive", "crown_of_concordance"]
STRUCTURES = ["waystone_monolith", "archivists_outpost", "ruined_aether_aqueduct", "gilded_civic_temple",
              "glassroot_warren", "hush_archive", "sundered_observatory", "gravity_vault", "starfall_forge",
              "cycle_observatory", "hall_of_nine_seals", "crown_bastion", "crown_of_concordance", "rift_scar"]
SURFACE_RUINS = ["waystone_monolith", "archivists_outpost", "ruined_aether_aqueduct", "gilded_civic_temple",
                 "sundered_observatory", "cycle_observatory", "rift_scar"]
MOBS = ["reliquary_sentinel", "scriptbound_scribe", "gilded_husk", "mirebound_cantor", "oathbreaker_knight",
        "vestige_drifter", "rift_archer", "echo_weaver", "glassroot_stalker", "ruin_hound", "aether_grazer",
        "shardback_brute", "starfall_strider", "gravity_leech", "archive_mimic", "fracture_wisp", "nullwing",
        "eclipse_moth", "concordance_guardian", "sealed_custodian", "rune_warden", "cycle_herald"]

lang = collections.OrderedDict()


# ---------------------------------------------------------------- criteria helpers

def has(*items):
    return {"trigger": "minecraft:inventory_changed", "conditions": {"items": [{"items": list(items)}]}}


def book(enchantment):
    return {"trigger": "minecraft:inventory_changed", "conditions": {"items": [
        {"items": ["minecraft:enchanted_book"], "stored_enchantments": [{"enchantment": RW + enchantment}]}]}}


def infused(infusion):
    return {"trigger": "minecraft:inventory_changed", "conditions": {"items": [
        {"items": CELESTIAL_ARMOR, "nbt": '{RelicWeaveInfusions:["relicweave:%s"]}' % infusion}]}}


def killed(entity):
    return {"trigger": "minecraft:player_killed_entity", "conditions": {"entity": [
        {"condition": "minecraft:entity_properties", "entity": "this", "predicate": {"type": entity}}]}}


def at(kind, value):
    return {"trigger": "minecraft:location", "conditions": {"player": [
        {"condition": "minecraft:entity_properties", "entity": "this", "predicate": {"location": {kind: value}}}]}}


def consumed(*items):
    return {"trigger": "minecraft:consume_item", "conditions": {"item": {"items": list(items)}}}


def quest(event):
    return {"trigger": "relicweave:quest", "conditions": {"event": event}}


def tick():
    return {"trigger": "minecraft:tick"}


# ---------------------------------------------------------------- writer

def adv(tab, name, parent, icon, title, desc, criteria, mode="all", frame="task", xp=0, loot=None, hidden=False,
        background=None, toast=True, chat=True, keep_lang=False):
    """criteria: dict name -> criterion. mode 'all' = every criterion, 'any' = one of them."""
    display = {
        "icon": {"item": icon},
        "title": {"translate": f"advancements.relicweave.{tab}.{name}.title"},
        "description": {"translate": f"advancements.relicweave.{tab}.{name}.description"},
        "frame": frame,
        "show_toast": toast,
        "announce_to_chat": chat and frame != "task",
        "hidden": hidden,
    }
    if background:
        display["background"] = background
    data = collections.OrderedDict()
    if parent:
        data["parent"] = f"relicweave:{parent}"
    data["display"] = display
    data["criteria"] = criteria
    names = list(criteria)
    data["requirements"] = [names] if mode == "any" else [[n] for n in names]
    rewards = {}
    if xp:
        rewards["experience"] = xp
    if loot:
        rewards["loot"] = loot
    if rewards:
        data["rewards"] = rewards
    path = ADV / tab / f"{name}.json"
    path.parent.mkdir(parents=True, exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(data, f, indent=2, ensure_ascii=False)
        f.write("\n")
    if not keep_lang:
        lang[f"advancements.relicweave.{tab}.{name}.title"] = title
        lang[f"advancements.relicweave.{tab}.{name}.description"] = desc


# ---------------------------------------------------------------- tab 1: RelicWeave (machines, 0.0.1-0.0.3)

T = "relicweave"
adv(T, "root", None, RW + "transmutation_table", "RelicWeave",
    "Relics, transmutation and the stars above Netherite. Your RelicWeave Guide explains every machine.",
    {"joined": tick()}, background="minecraft:textures/block/amethyst_block.png", toast=False)
adv(T, "transmutation_table", "relicweave/root", RW + "transmutation_table", "A Table for Transmutation",
    "Craft a Transmutation Table from a Book, Amethyst, a Diamond and Obsidian", {"table": has(RW + "transmutation_table")}, xp=20)
adv(T, "first_book", "relicweave/transmutation_table", "minecraft:enchanted_book", "Bound in Ink",
    "Transmute an input and a catalyst into a RelicWeave enchanted book",
    {e: book(e) for e in LEGACY_BOOKS + ATTUNEMENT_BOOKS + VESTIGE_BOOKS}, mode="any", xp=30)
adv(T, "bookworm", "relicweave/first_book", "minecraft:bookshelf", "Bookworm",
    "Collect every original RelicWeave enchanted book: Keen Edge, Purling, Cobbled Up, Oxidation, Conductive, Golemed, "
    "Piglinified, Villager's Best Friend, The Necromancer, Necromanced and Upbound",
    {e: book(e) for e in LEGACY_BOOKS}, frame="challenge", xp=250)
adv(T, "attuned", "relicweave/first_book", "minecraft:prismarine_shard", "Attuned",
    "Make an Attunement book: Reaping, Undertow, Resonance, Soulguard, Steady Draw or Reprisal",
    {e: book(e) for e in ATTUNEMENT_BOOKS}, mode="any", xp=40)
adv(T, "attunement_master", "relicweave/attuned", "minecraft:echo_shard", "Two Paths, Six Sources",
    "Collect all six Attunement enchanted books", {e: book(e) for e in ATTUNEMENT_BOOKS}, frame="goal", xp=150)
adv(T, "celestial_shard", "relicweave/root", RW + "celestial_shard", "Stardust in Stone",
    "Mine Celestial Ore in the End (or, rarely, the Nether) for a Celestial Shard", {"shard": has(RW + "celestial_shard")}, xp=30)
adv(T, "celestial_ingot", "relicweave/celestial_shard", RW + "celestial_ingot", "Forged from the Sky",
    "Combine Celestial Shards and a Netherite Ingot into a Celestial Ingot", {"ingot": has(RW + "celestial_ingot")}, xp=50)
adv(T, "celestial_gear", "relicweave/celestial_ingot", RW + "celestial_sword", "Above Netherite",
    "Upgrade a Netherite tool or armor piece to Celestial at a Smithing Table",
    {i.split(":")[1]: has(i) for i in CELESTIAL_ALL}, mode="any", xp=50)
adv(T, "celestial_set", "relicweave/celestial_gear", RW + "celestial_chestplate", "Clad in Starlight",
    "Own a full set of Celestial armor", {i.split(":")[1]: has(i) for i in CELESTIAL_ARMOR}, frame="goal", xp=150)
adv(T, "celestial_infuser", "relicweave/celestial_ingot", RW + "celestial_infuser", "The Celestial Infuser",
    "Craft a Celestial Infuser", {"infuser": has(RW + "celestial_infuser")}, xp=40)
adv(T, "first_infusion", "relicweave/celestial_infuser", "minecraft:nether_star", "Infused",
    "Infuse a piece of Celestial armor",
    {i: infused(i) for i in ATTRIBUTE_INFUSIONS + EFFECT_INFUSIONS + VESTIGE_INFUSIONS}, mode="any", xp=50)
adv(T, "effect_infusion", "relicweave/first_infusion", "minecraft:heart_of_the_sea", "More Than Numbers",
    "Give Celestial armor an effect infusion: Tidal, Warding, Pursuit or Anchored",
    {i: infused(i) for i in EFFECT_INFUSIONS}, mode="any", xp=60)
adv(T, "first_charm", "relicweave/root", RW + "copper_charm", "Lucky Charm",
    "Craft any RelicWeave charm", {c.split(":")[1]: has(c) for c in CHARMS}, mode="any", xp=20)
adv(T, "relic_hunter", "relicweave/first_charm", RW + "wayfarers_relic", "Relic Hunter",
    "Craft a Lodestone, Tidal, Featherwoven or Echo Charm, a Wayfarer's Relic or a Guardian Relic",
    {r.split(":")[1]: has(r) for r in RELICS_003}, mode="any", xp=40)
adv(T, "relic_activated", "relicweave/relic_hunter", RW + "echo_charm", "Pick One, Use It",
    "Select an active relic with Cycle Relic and use it with Activate Relic (rebindable under Controls)",
    {"activated": quest("relic_activated")}, xp=30)
adv(T, "relic_collector", "relicweave/relic_activated", RW + "guardian_relic", "Reliquary",
    "Own all six Attunement relics", {r.split(":")[1]: has(r) for r in RELICS_003}, frame="goal", xp=150)
adv(T, "totem", "relicweave/first_charm", RW + "diamond_totem", "Not Today",
    "Craft a Diamond, Netherite or Ultra Totem", {t.split(":")[1]: has(t) for t in TOTEMS}, mode="any", xp=50)
adv(T, "fine_dining", "relicweave/root", RW + "diamond_steak", "Fine Dining",
    "Eat a Diamond or Netherite Steak", {"steak": consumed(RW + "diamond_steak", RW + "netherite_steak")}, xp=30)
adv(T, "beyond_the_dragon", "relicweave/root", "minecraft:dragon_head", "Rumours Beyond the End",
    "Defeat the Ender Dragon. A Traveler's Primer will find you.",
    {"dragon": killed("minecraft:ender_dragon")}, frame="goal", xp=100)
adv(T, "threshold_sigil", "relicweave/beyond_the_dragon", RW + "threshold_sigil", "On the Threshold",
    "Craft a Threshold Sigil", {"sigil": has(RW + "threshold_sigil")}, xp=50)
adv(T, "gate_woken", "relicweave/threshold_sigil", RW + "gate_keystone", "The Gate Wakes",
    "Wake a Concordance Gate with a Threshold Sigil. The journey continues in The Vestige tab.",
    {"woken": quest("gate_activated")}, frame="goal", xp=100)

# ---------------------------------------------------------------- tab 2: The Vestige (0.0.4)

V = "vestige"
adv(V, "root", None, RW + "gate_keystone", "The Vestige", "Step through a Concordance Gate",
    {"enter_vestige": {"trigger": "minecraft:changed_dimension", "conditions": {"to": "relicweave:the_vestige"}}},
    background="relicweave:textures/block/vestige_bricks.png", xp=50, loot=["relicweave:quests/first_crossing"],
    keep_lang=True)
adv(V, "there_and_back", "vestige/root", RW + "gate_keystone", "There and Back Again",
    "Return home through a Return Waystone or a gate", {"returned": quest("returned_from_vestige")}, xp=30)
adv(V, "witnessed_resonance", "vestige/root", "minecraft:clock", "The World Turns",
    "Be in The Vestige as the Concordance Cycle turns to Resonance", {"resonance": quest("witnessed_resonance")}, xp=40)
adv(V, "outlasted_convergence", "vestige/witnessed_resonance", "minecraft:recovery_compass", "Outlasting the Convergence",
    "Still be standing in The Vestige when a Convergence ends", {"outlasted": quest("outlasted_convergence")},
    frame="goal", xp=100)
adv(V, "warded", "vestige/root", RW + "ward_salve", "Warded",
    "Drink a Ward Salve", {"salve": consumed(RW + "ward_salve")}, xp=30)
adv(V, "rift_sickness", "vestige/warded", RW + "rift_fissure", "Too Close to the Rift",
    "Let Rift Saturation fill up. It warned you.", {"sick": quest("rift_sickness")}, xp=20, hidden=True)
adv(V, "aetherite", "vestige/root", RW + "aetherite_shard", "Aetherite",
    "Mine Aetherite Ore for an Aetherite Shard", {"shard": has(RW + "aetherite_shard")}, xp=30)
adv(V, "aetherite_ingot", "vestige/aetherite", RW + "aetherite_ingot", "Refined",
    "Smelt an Aetherite Ingot", {"ingot": has(RW + "aetherite_ingot")}, xp=30)
adv(V, "riftglass_lens", "vestige/aetherite", RW + "riftglass_lens", "Through the Lens",
    "Craft or find a Riftglass Lens", {"lens": has(RW + "riftglass_lens")}, xp=40)
adv(V, "first_ruin", "vestige/root", RW + "carved_vestige_stone", "Ruins of the Concordance",
    "Enter any Vestige structure", {s: at("structure", RW + s) for s in STRUCTURES}, mode="any", xp=40)
adv(V, "ruin_scholar", "vestige/first_ruin", RW + "ancient_terminal", "Ruin Scholar",
    "Visit a Waystone Monolith, Archivist's Outpost, Ruined Aether Aqueduct, Gilded Civic Temple, Sundered "
    "Observatory, Cycle Observatory and a Rift Scar",
    {s: at("structure", RW + s) for s in SURFACE_RUINS}, frame="goal", xp=200)
adv(V, "cartographer", "vestige/first_ruin", "minecraft:filled_map", "Cartographer of Ruin",
    "Set foot in all eight Vestige biomes", {b: at("biome", RW + b) for b in BIOMES}, frame="challenge", xp=300)
adv(V, "first_blood", "vestige/root", "minecraft:iron_sword", "Natives",
    "Defeat a creature of The Vestige", {"vestige_mob": killed("#relicweave:vestige_mobs")}, xp=30)
adv(V, "elite_slayer", "vestige/first_blood", "minecraft:netherite_sword", "Oathbreaker",
    "Defeat a Vestige elite", {"elite": killed("#relicweave:vestige_elites")}, xp=80)
adv(V, "mimic", "vestige/first_blood", "minecraft:chest", "That Chest Bites",
    "Defeat an Archive Mimic", {"mimic": killed(RW + "archive_mimic")}, xp=60)
adv(V, "bestiary", "vestige/elite_slayer", "minecraft:writable_book", "Complete Bestiary",
    "Defeat every kind of creature that lives in The Vestige", {m: killed(RW + m) for m in MOBS},
    frame="challenge", xp=500)
adv(V, "vestige_relic", "vestige/root", RW + "surveyors_lens", "Tools of the Archivists",
    "Craft a Vestige relic", {r.split(":")[1]: has(r) for r in RELICS_004}, mode="any", xp=50)
adv(V, "decoded", "vestige/vestige_relic", RW + "mnemonic_spindle", "Decoded",
    "Decode an Ancient Terminal with a Mnemonic Spindle", {"decoded": quest("terminal_decoded")}, xp=60)
adv(V, "loom", "vestige/vestige_relic", RW + "reliquary_loom", "The Reliquary Loom",
    "Craft a Reliquary Loom", {"loom": has(RW + "reliquary_loom")}, xp=40)
adv(V, "augmented", "vestige/loom", RW + "mnemonic_filament", "Woven Anew",
    "Give a relic an augment at the Reliquary Loom", {"augment": quest("loom_augment")}, xp=80)
adv(V, "vestige_relics_all", "vestige/augmented", RW + "sealkeepers_signet", "Keeper of the Index",
    "Own all six Vestige relics", {r.split(":")[1]: has(r) for r in RELICS_004}, frame="goal", xp=200)
adv(V, "vestige_book", "vestige/aetherite", "minecraft:enchanted_book", "New Words",
    "Transmute a Vestige material into an enchanted book", {e: book(e) for e in VESTIGE_BOOKS}, mode="any", xp=60)
adv(V, "vestige_infusion", "vestige/vestige_book", RW + "celestial_helmet", "Woven with the Vestige",
    "Infuse Celestial armor with Riftward, Sealguard, Cycleweave or Gravitic Tread",
    {i: infused(i) for i in VESTIGE_INFUSIONS}, mode="any", xp=80)
adv(V, "altar_call", "vestige/first_ruin", RW + "boss_altar", "Something Stirs",
    "Wake a guardian at a Boss Altar", {"summoned": quest("boss_summoned")}, xp=50)
boss_chain = [("thorned_regent", "altar_call", RW + "sovereign_thorn", 200),
              ("choir_unheard", "thorned_regent", RW + "liminal_chord", 250),
              ("gravity_maw", "choir_unheard", RW + "gravity_heart", 300),
              ("starforged_colossus", "gravity_maw", RW + "starforged_core", 350),
              ("warden_of_seals", "starforged_colossus", RW + "concordant_seal", 400)]
for boss, parent, icon, xp in boss_chain:
    adv(V, boss, f"vestige/{parent}", icon, "", "", {"defeated": {"trigger": "minecraft:impossible"}},
        frame="challenge", xp=xp, keep_lang=True)
adv(V, "concordant_alloy", "vestige/thorned_regent", RW + "concordant_alloy", "Concordant Alloy",
    "Alloy Aetherite Ingots with a guardian's core", {"alloy": has(RW + "concordant_alloy")}, xp=80)
adv(V, "upgrade_template", "vestige/warden_of_seals", RW + "vestige_upgrade_template", "The Vestige Upgrade",
    "Obtain a Vestige Upgrade Template", {"template": has(RW + "vestige_upgrade_template")}, xp=80)
adv(V, "aetherforged", "vestige/upgrade_template", RW + "aetherforged_sword", "Aetherforged",
    "Smith a Netherite piece into Aetherforged gear", {a.split(":")[1]: has(a) for a in AETHERFORGED}, mode="any",
    frame="goal", xp=150)
adv(V, "aetherforged_arsenal", "vestige/aetherforged", RW + "aetherforged_chestplate", "Aetherforged Arsenal",
    "Own every Aetherforged tool and armor piece", {a.split(":")[1]: has(a) for a in AETHERFORGED},
    frame="challenge", xp=400)
adv(V, "seal_gate", "vestige/warden_of_seals", RW + "seal_gate", "Through the Seal",
    "Pass through a Seal Gate into the Crown of Concordance", {"passed": quest("seal_gate_passed")}, frame="goal", xp=150)
adv(V, "archivist_prime", "vestige/seal_gate", RW + "prime_mnemonic_core", "", "",
    {"defeated": {"trigger": "minecraft:impossible"}}, frame="challenge", xp=1000, keep_lang=True)

# Loot reward for the first crossing: a little help, nothing that skips progression.
loot_path = ROOT / "src" / "main" / "resources" / "data" / "relicweave" / "loot_tables" / "quests" / "first_crossing.json"
loot_path.parent.mkdir(parents=True, exist_ok=True)
with open(loot_path, "w", encoding="utf-8") as f:
    json.dump({"type": "minecraft:advancement_reward", "pools": [
        {"rolls": 1, "entries": [{"type": "minecraft:item", "name": RW + "ward_salve",
                                  "functions": [{"function": "minecraft:set_count", "count": 2}]}]},
        {"rolls": 1, "entries": [{"type": "minecraft:item", "name": "minecraft:cooked_beef",
                                  "functions": [{"function": "minecraft:set_count", "count": 8}]}]}]}, f, indent=2)
    f.write("\n")

# Lang (merge into the existing file, keeping its order).
data = json.load(open(LANG, encoding="utf-8"), object_pairs_hook=collections.OrderedDict)
data.update(lang)
with open(LANG, "w", encoding="utf-8") as f:
    json.dump(data, f, indent=2, ensure_ascii=False)
    f.write("\n")
print(f"wrote {len(lang) // 2} advancement texts")
