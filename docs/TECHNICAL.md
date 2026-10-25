# RelicWeave

A Minecraft Forge mod that turns items and catalysts into enchanted books carrying custom enchantments. It also adds relics (charms, totems and steaks) and a Celestial material tier above Netherite, with armor you can infuse.

| | |
|---|---|
| Version | `0.0.2` |
| Minecraft | Java Edition `1.20.1` |
| Forge | `47.3.0+` |
| Java | `17` |
| Mod ID | `relicweave` |
| Authors | Dutch and Claude |

## Features

### Transmutation Table (`relicweave:transmutation_table`)

The table has three slots: **Input** (top left), **Catalyst** (bottom left), and **Output** (right). When the input and catalyst match a transmutation recipe and the output slot has room, the arrow fills. When it finishes, the table consumes the ingredients and puts the enchanted book in the output slot.

- All processing runs on the server. The GUI only shows progress.
- Ingredients are consumed only in the tick the result is inserted, so nothing is lost or duplicated. If the recipe stops matching or the output fills up partway through, progress goes back to zero and nothing is consumed.
- The inventory and progress are saved with the world. Breaking the table drops its contents.
- The input and catalyst slots only accept items used by some loaded recipe. The output slot only allows taking items out. Shift-clicking follows the same rules.
- Mine it with any pickaxe. You can get it from the *Functional Blocks* or *RelicWeave* creative tab, or craft it:

  ```
   B      B = Book
  ADA     A = Amethyst Shard, D = Diamond
  OOO     O = Obsidian
  ```

### Enchantments

The table is the only source of these enchantments: they never show up at enchanting tables, in loot, or from villagers. Put the book on an item at an anvil, as usual.

| Enchantment | Applies to | Max level | Effect |
|---|---|---|---|
| **Keen Edge** | Swords, Axes | III | +1 melee damage per level (`KeenEdgeEvents.DAMAGE_PER_LEVEL`). Only counts direct player melee hits, including sweeps. It doesn't apply to projectiles, thorns, mobs holding the weapon, or any other damage source. |
| **Purling** | Boots | I | Press the Purling key to throw a conjured Ender Pearl. You don't need to carry pearls and none are used. Pearls thrown this way do no teleport landing damage. Cooldown is 30 seconds (600 ticks), enforced by the server. |
| **Cobbled Up** | Pickaxes | III | Mining stone or deepslate has a 20% chance per level to drop an extra Cobblestone or Cobbled Deepslate. |
| **Oxidation** | Swords, Axes | III | Direct melee hits apply Slowness I for 2 s + 1 s per level. At III they also apply Weakness I for 3 s. |
| **Conductive** | Pickaxes, Axes, Shovels, Hoes | III | +25% mining speed per level on blocks the tool is meant for. Incompatible with Efficiency. |
| **Golemed** | Chestplates | III | +1 Armor and +0.1 Knockback Resistance per level. |
| **Piglinified** | Helmets | I | Piglins won't start attacking you, as if you wore gold. They still fight back if you hit them. Piglin Brutes are unaffected. |
| **Villager's Best Friend** | Leggings | II | Talking to a villager gives you Hero of the Village (level = enchantment level) for 5 s, so its trades are discounted. |
| **The Necromancer** | Swords, Axes | III | Killing a monster in melee has a 15% chance per level to raise a **Risen Minion**: a Husk that fights hostile mobs for you for 30 s. It never targets players and drops no loot or XP. |
| **Necromanced** | Helmets | I | Undead mobs won't target you unless you attack them (bosses excepted). Immune to Wither. |
| **Upbound** | Boots | II | Press Jump in mid-air to jump again, once per level, before landing. Each air jump resets fall distance. |

#### Purling details

- The client only sends a "key pressed" packet. The server checks that you're alive and not in spectator mode, are wearing Purling boots, and are off cooldown.
- Purling never needs or uses Ender Pearls (since 0.0.1c). The cooldown is its only limit.
- Pressing the key during the cooldown does nothing except show the time remaining on the action bar.
- Only pearls thrown by Purling lose their landing damage. Normal pearls and fall damage are unaffected.
- Cooldowns are kept per player while the server runs. They carry over through death, respawn, and relogging, and reset when the server restarts.

#### Upbound details

- The client applies the air jump immediately, because player movement is client-side in vanilla, and tells the server. The server checks the boots and the per-level jump budget before resetting fall distance, and the budget refills when you land.

### Relics

| Relic | Where it works | Effect |
|---|---|---|
| **Copper Charm** | Anywhere in your inventory | +5% Movement Speed |
| **Iron Charm** | Anywhere in your inventory | +2 Armor |
| **Gold Charm** | Anywhere in your inventory | +2 Luck |
| **Diamond Charm** | Anywhere in your inventory | +2 Max Health |
| **Netherite Charm** | Anywhere in your inventory | Fire Resistance, +0.1 Knockback Resistance |
| **Diamond Totem** | Offhand | Passive +4 Max Health. On death: survive with 4 health, Resistance II and Regeneration II for 5 s. 5 min cooldown. |
| **Netherite Totem** | Offhand | Passive +4 Max Health and Fire Resistance. On death: survive at half health, gain Resistance II and Regeneration II for 10 s and Fire Resistance for 20 s, and knock nearby mobs back. 3 min cooldown. |
| **Ultra Totem** | Either hand | Passive +8 Max Health, Fire Resistance and Regeneration. On death: full health, harmful effects removed, Absorption IV, Resistance II, Regeneration III and Strength I, plus Fire Resistance for 60 s. 1 min cooldown. |

- Each charm type counts once, however many you carry. Different charm types stack.
- RelicWeave totems are never used up. After saving you they go on cooldown, shown on the item like an Ender Pearl cooldown. A vanilla Totem of Undying in your hand triggers first. Void and `/kill` deaths can't be prevented.
- Only the highest-tier qualifying totem gives its passive.

| Food | Hunger | Effects |
|---|---|---|
| **Diamond Steak** | 10 (high saturation) | Regeneration I 10 s, Absorption I 60 s. Can be eaten when full. |
| **Netherite Steak** | 12 (very high saturation) | Regeneration II 10 s, Absorption II 2 min, Fire Resistance 2 min, Resistance I 60 s. Can be eaten when full. |

### Celestial (a tier above Netherite)

- **Ore:** Celestial Ore generates in End Stone, about 5 blocks per chunk in the End. Nether Celestial Ore generates in Netherrack and is rarer, about 1 block per chunk. Both need a diamond pickaxe or better and drop **Celestial Shards** (Fortune applies, Silk Touch gives the ore). You can also smelt or blast the ore into shards.
- **Ingot:** 4 Celestial Shards + 1 Netherite Ingot, shapeless. Nine ingots craft a **Block of Celestial**, which works as a beacon base. The ingot is a beacon payment.
- **Gear:** in a Smithing Table, combine a Netherite Upgrade template, a Netherite tool or armor piece, and a Celestial Ingot. This gives the Celestial version, for all 5 tools and 4 armor pieces.
  - Tools: 2531 durability, mining speed 10, +1 attack damage over Netherite, enchantability 18, mines everything Netherite can.
  - Armor: 4 / 9 / 7 / 4 defense, 4 toughness per piece, 0.15 knockback resistance per piece, durability ×42, works with armor trims.
  - Fire-resistant as items, like Netherite.

### Celestial Infuser (`relicweave:celestial_infuser`)

Works like the Transmutation Table, but takes **Celestial armor + an infusion material** and returns the same piece with an infusion added. Only Celestial armor is accepted. Each piece holds up to **3 different infusions**. The bonus is the same on every slot, and the same infusion on several pieces stacks.

| Material | Infusion | Bonus per piece |
|---|---|---|
| 32 Cobblestone | Cobbled | +1 Armor |
| 1 Block of Copper | Oxidized | +4% Movement Speed |
| 1 Block of Redstone | Conductive | +6% Attack Speed |
| 1 Block of Iron | Golemed | +0.05 Knockback Resistance |
| 1 Block of Gold | Piglinified | +1 Luck |
| 1 Block of Emerald | Villager's Best Friend | +2 Max Health |
| 1 Netherite Ingot | The Necromancer | +1 Attack Damage |
| 1 Wither Skeleton Skull *or* 32 Soul Sand | Necromanced | +1 Armor Toughness |
| 1 Nether Star | Upbound | −6% Gravity (higher jumps, slower falls) |

Infusing takes 10 s. The bonuses show in the armor tooltip. Craft the infuser with `" I "`, `"ATA"`, `"OOO"`: I = Celestial Ingot, A = Amethyst Shard, T = Transmutation Table, O = Crying Obsidian.

### Guide book

Each player receives a written **RelicWeave Guide** (13 pages) once, on their first login to a world. It covers how to use the Transmutation Table and Celestial Infuser and lists every recipe and infusion. The "already received" flag is stored in Forge's persisted player data, so it survives death. The page text lives in the lang file (`book.relicweave.guide.page1` to `page13`).

## Controls

| Action | Default key |
|---|---|
| Purling: Throw Ender Pearl | `V` |

You can rebind the Purling key in game under *Options → Controls → Key Binds → RelicWeave*. Key-binding mods such as [Controlling](https://modrinth.com/mod/controlling) also list it, and you can search for it, rebind it, reset it, or check it for conflicts there. Your choice is saved in `options.txt` as `key_key.relicweave.purling`.

## Recipes

The built-in recipes are in `src/main/resources/data/relicweave/recipes/transmutation/`:

| File | Input | Catalyst | Result |
|---|---|---|---|
| `keen_edge_1.json` | 1 Diamond | 1 Amethyst Shard | Keen Edge I book |
| `purling_1.json` | 1 Ender Pearl | 1 Amethyst Shard | Purling I book |
| `cobbled_up_1.json` | 32 Cobblestone | 1 Amethyst Shard | Cobbled Up I book |
| `oxidation_1.json` | 8 Copper Ingots | 1 Amethyst Shard | Oxidation I book |
| `conductive_1.json` | 16 Redstone Dust | 1 Amethyst Shard | Conductive I book |
| `golemed_1.json` | 8 Iron Ingots | 2 Amethyst Shards | Golemed I book |
| `piglinified_1.json` | 8 Gold Ingots | 2 Amethyst Shards | Piglinified I book |
| `villagers_best_friend_1.json` | 8 Emeralds | 2 Amethyst Shards | Villager's Best Friend I book |
| `the_necromancer_1.json` | 1 Netherite Ingot | 1 Echo Shard | The Necromancer I book (10 s) |
| `necromanced_1.json` | 1 Wither Skeleton Skull | 8 Soul Sand | Necromanced I book (10 s) |
| `upbound_1.json` | 1 Nether Star | 2 Echo Shards | Upbound I book (15 s) |

Higher levels come from combining books at the anvil.

Other recipe folders: `recipes/relic/` (charms, totems, steaks), `recipes/celestial/` (ingot, block, smelting, smithing upgrades, infuser), and `recipes/infusion/` (infusion materials, type `relicweave:infusion`).

| Relic recipe | Pattern |
|---|---|
| Copper / Iron / Gold / Diamond Charm | `" S "`, `"MAM"`, `" M "`: S = String, M = the material, A = Amethyst Shard |
| Netherite Charm / Netherite Totem | Smithing: Netherite Upgrade template + Diamond Charm / Diamond Totem + Netherite Ingot |
| Diamond Totem | 8 Diamonds around a Totem of Undying |
| Ultra Totem | `"ESE"`, `"TNT"`, `"EHE"`: E = Echo Shard, S = Nether Star, T = Totem of Undying, N = Netherite Totem, H = Heart of the Sea |
| Diamond Steak | 4 Diamonds around Cooked Beef (plus shape) |
| Netherite Steak | 4 Netherite Scrap around a Diamond Steak (plus shape) |

To add or override recipes, put JSON files in any datapack under `data/<namespace>/recipes/...`. You don't need to change any Java code. Run `/reload` to apply them.

```json
{
  "type": "relicweave:transmutation",
  "input": { "item": "minecraft:diamond" },
  "input_count": 1,
  "catalyst": { "item": "minecraft:amethyst_shard" },
  "catalyst_count": 1,
  "enchantment": "relicweave:keen_edge",
  "level": 1,
  "process_time": 100
}
```

| Field | Required | Rules |
|---|---|---|
| `input`, `catalyst` | yes | Standard ingredient: `{"item": ...}`, `{"tag": ...}`, or an array of these |
| `input_count`, `catalyst_count` | no (default 1) | 1–64 |
| `enchantment` | yes | Any registered enchantment ID |
| `level` | no (default 1) | 1 up to the enchantment's max level |
| `process_time` | no (default 100) | Ticks, 1–32767 |

If a recipe is malformed or out of range, it's rejected and the error is logged with the recipe ID. The other recipes still load.

## Building and running

```bash
./gradlew build              # builds build/libs/relicweave-<version>.jar
./gradlew runClient          # dev client
./gradlew runServer          # dev dedicated server (accept run/eula.txt first)
./gradlew runGameTestServer  # runs the automated GameTests headlessly, exits non-zero on failure
./gradlew runClient -PwithControlling  # dev client with Controlling + Searchables from Modrinth, for testing key binds
```

`-PwithControlling` is optional and only affects dev runs. RelicWeave never bundles or requires Controlling.

You need a JDK 17 or newer to run Gradle. The build compiles with a Java 17 toolchain and downloads one through the Foojay resolver if none is installed.

## Automated tests

There are 49 Forge GameTests in `com.relicweave.gametest`: `RelicWeaveGameTests` (11), `EnchantmentGameTests`, `RelicGameTests`, `InfuserGameTests`, `CelestialGameTests` and `UpboundGameTests`. They run on a dedicated-server dist, so they also confirm that no client-only class loads on a server. They cover:

- Both recipes produce exactly one correctly enchanted book after `process_time`, and consume exactly the right ingredients.
- A full output slot blocks further processing without consuming anything.
- Mismatched ingredients do nothing. Removing the catalyst mid-process resets progress with no loss.
- Inventory and progress survive an NBT save/load round trip.
- Slot rules, including output-insert rejection and shift-click routing.
- Recipe JSON validation: missing fields, unknown enchantment or item, out-of-range level, count, or time.
- Keen Edge: the bonus at levels I and III, no bonus for non-melee damage, unenchanted weapons, or ineligible items.
- Purling: works without pearls and never uses carried pearls, ignores spam during the cooldown, the cooldown ends at 600 ticks, nothing happens without boots, and landing-damage removal only applies to Purling pearls.
- 0.0.2 enchantments:
  - Oxidation: melee-only debuffs.
  - Golemed: attribute values.
  - Conductive: break-speed scaling.
  - Necromanced: undead targeting cancelled and Wither immunity.
  - Piglinified: piglin targeting.
  - The Necromancer: minion tagging, expiry, and no targeting of players.
  - All 9 transmutation recipes load.
- Relics:
  - Each charm's modifier, with one modifier per type and health clamped on removal.
  - Totem passives by hand.
  - Death saves: health, cooldown, totem kept, refused during the cooldown, and void deaths not saved.
  - All relic recipes load.
- Infuser:
  - Infusing a helmet.
  - The 3-infusion cap, duplicate and non-Celestial armor refused, and an occupied output blocking work.
  - Save/load, slot rules, recipe validation, and the infused attribute being applied.
- Celestial:
  - Recipes, ore loot (shards and Silk Touch), worldgen feature registration, tool tier, and armor stats.
- Upbound:
  - Jump budget per level, fall-distance reset, refusal on the ground, and the budget refilling on landing.

### Manual in-client check

0.0.2, checked in a dev client:
- Item and armor textures render, including Celestial armor on the player.
- The Celestial Infuser GUI infused a Celestial Helmet with Golemed. The tooltip lists the infusion and the attribute is applied.
- Upbound II gave two air jumps.
- Ore placement, measured by replacing ore in 9 freshly generated chunks: 47 Celestial Ore in the End and 9 Nether Celestial Ore in the Nether.

0.0.1b: with Controlling 12.0.2 installed, I searched for the Purling key, rebound it from `V` to `G`, and confirmed that `G` throws the pearl while `V` no longer does. The new binding was saved to `options.txt`.

Checked in a single-player dev client: the block model and textures, opening the GUI, shift-click insertion, the progress arrow, Keen Edge I books produced and the output slot blocking further work, combining Keen Edge I + I into II at the anvil, applying it to an iron sword, and Purling (no landing damage, cooldown message on a second press).

## Known limitations

- **Combining at the anvil:** Keen Edge books combine normally (I + I → II, up to III). Purling has a max level of I, so two Purling books can't be combined into a higher level.
- **Multiplayer:** this build hasn't been tested with two or more real players on a dedicated server. All Purling and table state is server-side and per-player or per-block, and the GameTests run on a dedicated-server dist.
- The table doesn't expose an item-handler capability, so hoppers and pipes can't insert or extract items.
- Purling cooldowns aren't saved across server restarts. Totem cooldowns use the vanilla item cooldown, which also resets when you relog.
- Neither infusion block (Transmutation Table, Celestial Infuser) exposes an item-handler capability, so hoppers can't automate them.
- Risen Minions are removed when their chunk unloads and reloads, because their AI would reset and they'd turn hostile.
- Villager's Best Friend uses the Hero of the Village effect (hidden icon, 5 s), so it also briefly counts for other Hero of the Village checks.
- Textures for the 0.0.2 items are recolors of vanilla textures (placeholder-quality art).
