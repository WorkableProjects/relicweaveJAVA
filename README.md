# RelicWeave

A Minecraft Forge mod that turns items and catalysts into enchanted books carrying custom enchantments.

| | |
|---|---|
| Version | `0.0.1` |
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
- Mine it with any pickaxe. You can get it from the *Functional Blocks* creative tab or craft it:

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
| **Purling** | Boots | I | Press the Purling key to throw an Ender Pearl from your inventory (offhand first, then the main inventory). Pearls thrown this way do no teleport landing damage. Cooldown is 30 seconds (600 ticks), enforced by the server. |

#### Purling details

- The client only sends a "key pressed" packet. The server checks that you're alive and not in spectator mode, are wearing Purling boots, are off cooldown, and have an Ender Pearl.
- A failed attempt (no pearl, or still on cooldown) consumes nothing and doesn't start the cooldown. A message on the action bar says why it failed.
- Creative players keep their pearl, like in vanilla, but still need one in their inventory.
- Only pearls thrown by Purling lose their landing damage. Normal pearls and fall damage are unaffected.
- Cooldowns are kept per player while the server runs. They carry over through death, respawn, and relogging, and reset when the server restarts.

## Controls

| Action | Default key |
|---|---|
| Purling: Throw Ender Pearl | `V` (change it under *Options → Controls → Key Binds → RelicWeave*) |

## Recipes

The built-in recipes are in `src/main/resources/data/relicweave/recipes/transmutation/`:

| File | Input | Catalyst | Result |
|---|---|---|---|
| `keen_edge_1.json` | 1 Diamond | 1 Amethyst Shard | Keen Edge I book |
| `purling_1.json` | 1 Ender Pearl | 1 Amethyst Shard | Purling I book |

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
./gradlew build              # builds build/libs/relicweave-0.0.1.jar
./gradlew runClient          # dev client
./gradlew runServer          # dev dedicated server (accept run/eula.txt first)
./gradlew runGameTestServer  # runs the automated GameTests headlessly, exits non-zero on failure
```

You need a JDK 17 or newer to run Gradle. The build compiles with a Java 17 toolchain and downloads one through the Foojay resolver if none is installed.

## Automated tests

`com.relicweave.gametest.RelicWeaveGameTests` contains 11 Forge GameTests. They run on a dedicated-server dist, so they also confirm that no client-only class loads on a server. They cover:

- Both recipes produce exactly one correctly enchanted book after `process_time`, and consume exactly the right ingredients.
- A full output slot blocks further processing without consuming anything.
- Mismatched ingredients do nothing. Removing the catalyst mid-process resets progress with no loss.
- Inventory and progress survive an NBT save/load round trip.
- Slot rules, including output-insert rejection and shift-click routing.
- Recipe JSON validation: missing fields, unknown enchantment or item, out-of-range level, count, or time.
- Keen Edge: the bonus at levels I and III, no bonus for non-melee damage, unenchanted weapons, or ineligible items.
- Purling: consumes one pearl, ignores spam during the cooldown, the cooldown ends at 600 ticks, nothing happens without boots or pearls, failed attempts don't start the cooldown, and landing-damage removal only applies to Purling pearls.

### Manual in-client check

Checked in a single-player dev client: the block model and textures, opening the GUI, shift-click insertion, the progress arrow, Keen Edge I books produced and the output slot blocking further work, combining Keen Edge I + I into II at the anvil, applying it to an iron sword, and Purling (pearl consumed, no landing damage, cooldown message on a second press).

## Known limitations (0.0.1)

- **Combining at the anvil:** Keen Edge books combine normally (I + I → II, up to III). Purling has a max level of I, so two Purling books can't be combined into a higher level.
- **Multiplayer:** this build hasn't been tested with two or more real players on a dedicated server. All Purling and table state is server-side and per-player or per-block, and the GameTests run on a dedicated-server dist.
- The table doesn't expose an item-handler capability, so hoppers and pipes can't insert or extract items.
- Purling cooldowns aren't saved across server restarts.
