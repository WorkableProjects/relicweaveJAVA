# RelicWeave

A Minecraft Forge mod that turns items and catalysts into enchanted books carrying custom enchantments. It also adds relics (charms, totems and steaks) and a Celestial material tier above Netherite, with armor you can infuse.

| | |
|---|---|
| Version | `0.0.4.5.b` |
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
- The GUI shows a status line (`MachineStatus`): *Working…*, *Missing ingredient* (inputs present but no recipe accepts them, including too few items) or *Output full*. Status is recomputed on the server every tick and synced through the menu's `ContainerData`.
- On completion only, the server plays `relicweave:block.transmutation_table.complete` (with subtitle) once for all nearby players and sends an Enchant-particle rune burst; the GUI briefly flashes the output slot. Failed, blocked or changed inputs never trigger it.
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
| **Reaping** | Hoes | III | Breaking a mature crop (not while crouching) harvests every mature `CropBlock` within radius = level on the same Y (3×3 / 5×5 / 7×7) and replants each one using one seed from its own drops; with no seed in the drops the crop is not replanted. Immature crops are never touched. 1 durability per crop beyond the first; stops if the hoe breaks. No drops or wear in creative. |
| **Undertow** | Tridents | III | Melee or thrown trident hits on a target that is in water apply Slowness (amplifier level − 1) for 2 s + 1 s per level; a wielder in water gets Dolphin's Grace for 3 s. Bosses (`forge:bosses`) are not slowed. |
| **Resonance** | Pickaxes | III | Each consecutive break of the same block type (within 5 s) adds +1% × level mining speed for that block type, capped at +10% × level. Another block type or a 5 s pause resets it. Incompatible with Conductive. The server sends the streak to the miner so dedicated-server clients predict the same speed. |
| **Soulguard** | Chestplates | II | Killing a hostile mob grants 2 × level absorption (half-hearts) for 10 s, at most once per 2 s, through the shared absorption cap. |
| **Steady Draw** | Bows | II | While drawing a bow you move at 50% (I) / 80% (II) of normal speed instead of vanilla's 20%. Client-side movement; no effect on damage or draw time. |
| **Reprisal** | Shields | III | Blocking a melee hit within 10 ticks (0.5 s) of raising the shield gives the attacker Weakness I and Slowness (level − 1) for 2 s + 1 s per level. 3 s internal cooldown. Holding the shield up longer gives nothing. |

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

| **Lodestone Charm** | See *Relic locations* | Toggle with Activate Relic. While on, pulls dropped items within 6 blocks toward you (no pickup delay, and reserved for you or nobody). Vanilla pickup rules still apply. |
| **Tidal Charm** | See *Relic locations* | Water Breathing while your head is underwater; underwater fog distance ×2.5 (at least 96 blocks, client-side). No armor or damage bonus. |
| **Featherwoven Charm** | See *Relic locations* | Fall damage halved. After falling 12+ blocks, Slow Falling for 2 s (10 s cooldown). No flight or hovering. |
| **Wayfarer's Relic** | See *Relic locations* | Sneak + use to bind your position. Activate (key, or use without sneaking) to channel for 3 s and return there. Same dimension only; moving more than 0.5 blocks, taking damage, dying, logging out, changing dimension, losing the relic or an obstructed destination cancels it without a cooldown. 300 s cooldown after a successful return. |
| **Echo Charm** | See *Relic locations* | Activate: hostile mobs within 24 blocks glow for 10 s (vanilla Glowing, so every player sees it). 45 s cooldown. |
| **Guardian Relic** | See *Relic locations* | When a non-lethal hit leaves you at 3 hearts or less, gain a 4-heart absorption shield for 10 s (within the absorption cap). 120 s cooldown. It never prevents death itself. |

- Each charm and relic type counts once, however many you carry or where. Different types stack.

#### Relic locations, activation and cooldowns (0.0.3)

- `charmActivation` (server config) is `INVENTORY_OR_EQUIPPED` by default: charms and relics work anywhere in the main inventory, the offhand, or a Curios slot. `EQUIPPED_ONLY` limits them to the offhand and Curios slots.
- Totems keep their hand requirements. `curiosTotems = true` (off by default) also lets a totem work from a Curios slot.
- **Cycle Relic** (default `Z`; `G` before 0.0.4) picks the next active relic you carry (Lodestone, Wayfarer's, Echo); the selection is shown left of the hotbar with its key and cooldown. **Activate Relic** (default `R`) sends one packet naming the relic; the server checks that you are alive, carry it in a valid location, and that it is off cooldown.
- Cooldowns are stored per player and per relic type in Forge's persisted player data as an absolute overworld game time, so moving, dropping, duplicating or re-equipping a relic, dying, respawning, changing dimension, relogging and server restarts never reset them. The vanilla item cooldown overlay is only a visual hint.
- Guardian Relic, Soulguard and Warding add absorption only through `AbsorptionShields`, so together they never exceed `absorptionCap` (default 8 half-hearts), and any of their absorption left when the shield expires is removed. Absorption from other sources is kept.

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

Works like the Transmutation Table, but takes **Celestial armor + an infusion material** and returns the same piece with an infusion added. Only Celestial armor is accepted. Each piece holds up to **3 different infusions**, validated on the server.

- **Attribute infusions** (0.0.2, table below) fit every piece. The bonus is the same on every slot, and the same infusion on several pieces stacks.
- **Effect infusions** (0.0.3) each fit exactly one piece, so a player can wear at most one of each: identical effects never stack.

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

| Effect infusion | Piece | Material (any one) | Effect |
|---|---|---|---|
| Tidal | Helmet | 16 Prismarine Shards | Water Breathing (3 s, refreshed every 0.5 s) while your head is underwater. Never shortens a longer Water Breathing effect. |
| Warding | Chestplate | 4 Echo Shards *or* 4 Ghast Tears | One ward charge removes 2 hearts from the next hit, then recharges 20 s later. One charge maximum. A new wearer waits 20 s for the first charge. Damage that bypasses invulnerability isn't reduced. |
| Pursuit | Leggings | 8 Phantom Membranes | Killing a hostile mob gives Speed II for 3 s, at most once every 10 s. |
| Anchored | Boots | 32 Copper Ingots *or* 32 Oak Saplings | While crouching, knockback is reduced by 75%. |

Infusing takes 10 s. The tooltip lists each infusion and, for effect infusions, what it does; hold Shift for which effect infusions still fit that piece and the limit and stacking rules. The GUI status line also says *Maximum infusions reached*, *Already has this infusion*, *Wrong armor piece for this infusion*, *Missing ingredient* or *Output full*. A finished infusion plays a deep resonance followed by a chime (with subtitles) and End Rod particles, once. Craft the infuser with `" I "`, `"ATA"`, `"OOO"`: I = Celestial Ingot, A = Amethyst Shard, T = Transmutation Table, O = Crying Obsidian.

### JEI integration

If [Just Enough Items](https://www.curseforge.com/minecraft/mc-mods/jei) is installed, `com.relicweave.compat.jei.RelicWeaveJeiPlugin` adds the following. JEI is a compile-only dependency and is never required at runtime.
- **Transmutation Table** category: input ×count, catalyst ×count, then the enchanted book, with an animated arrow timed to `process_time`.
- **Celestial Infuser** category: each Celestial armor piece, then the same piece with the infusion applied (they cycle together), plus the material ×count, the infusion's bonus, and the 3-per-piece limit.
- Effect infusions show only the armor piece they fit, plus their effect text.
- Each Attunement source item (every item in each `relicweave:attunement_sources/*` tag) has an info page naming its book and its infusion. Its "uses" view lists both recipes.
- Both machines are registered as catalysts. Clicking the progress arrow in either GUI opens its recipes.
- Info pages for both machines, Celestial Ore and Shards, and Celestial armor.

All crafting, smithing and smelting recipes for RelicWeave items show up in JEI's vanilla categories automatically. `./gradlew runClient` includes JEI.

### Guide book

Each player receives a written **RelicWeave Guide** (48 pages since 0.0.4.5) once, on their first login to a world. It covers the Transmutation Table, the Celestial Infuser, Attunement sources, the new enchantments and infusions, relics, cooldowns, controls and Curios. The received edition is stored in Forge's persisted player data, so it survives death; players who received an older edition (0.0.2: 13 pages, 0.0.3: 24 pages, 0.0.4: 45 pages) get the new edition once, with a chat note. The page text lives in the lang file (`book.relicweave.guide.page1` to `page48`).

### Attunement source items (0.0.3)

`com.relicweave.attunement.AttunementMapping` is the canonical list. Each row reads the item tag `relicweave:attunement_sources/<row>`, and both of the row's recipes use that tag, so a datapack can replace a source item by overriding the tag. `AttunementValidator` logs an error on server start and after `/reload` if any source item lacks either pathway, and a GameTest fails on the same condition.

| Row / tag | Default source | Transmutation (`transmutation/<row>_1.json`) | Infusion (`infusion/<infusion>_from_<row>_source.json`) |
|---|---|---|---|
| `undertow` | Prismarine Shard | 8 + 1 Amethyst Shard → Undertow I (5 s) | 16 → Tidal |
| `resonance` | Echo Shard | 2 + 2 Amethyst Shards → Resonance I (10 s) | 4 → Warding |
| `steady_draw` | Phantom Membrane | 4 + 1 Amethyst Shard → Steady Draw I (5 s) | 8 → Pursuit |
| `reprisal` | Copper Ingot | 8 + 4 Lapis Lazuli → Reprisal I (5 s) | 32 → Anchored |
| `soulguard` | Ghast Tear | 2 + 2 Amethyst Shards → Soulguard I (8 s) | 4 → Warding |
| `reaping` | Oak Sapling | 16 + 1 Amethyst Shard → Reaping I (5 s) | 32 → Anchored |

Reprisal uses Lapis Lazuli as its catalyst because 8 Copper Ingots + Amethyst Shard is already Oxidation.

### Dread look (0.0.4.5.b)

Client-only, in `vestige/client/dread` and `vestige/client/world`. `DreadPalette` holds the math (pure, tested by `DreadGameTests`). `VestigeDimensionEffects` draws the dark sky dome and stars (`renderSky`), hides clouds and dims the lightmap by sky light only (`adjustLightmapColors`, scaled by block light). `VestigeSky` sets the fog distances and colour. `DreadHudTextures` re-registers darkened copies of `textures/gui/icons.png` and `widgets.png` (built from the active resource pack, crosshair cell untouched) under the vanilla ids while in The Vestige. Hearts and the XP bar are recoloured with `DreadPalette.voidPurple` and re-uploaded every 2 ticks for the shimmer. It restores the originals on leaving, on `darkHud` off, or on resource reload. Vanilla resets the shader colour before each HUD layer, which is why the HUD is darkened through textures rather than a tint. `DreadOverlay` (registered below every other HUD layer) draws the vignette and two counter-pulsing vein layers (`tools/textures/gen_dread.py`). `DreadAmbience` spawns the ash particles and plays the distant cave sounds. Client config: `dread`, `darkHud`.

### Toolkit (0.0.4.5)

| Part | Code | Notes |
|---|---|---|
| Multitool | `item/MultitoolItem` | A `DiggerItem` over the block tag `relicweave:mineable/multitool` (= `#minecraft:mineable/pickaxe` + `#minecraft:mineable/axe`), sword damage (3 + tier) and speed (-2.4), sword/axe/pickaxe `ToolAction`s, axe `useOn` (strip, scrape, wax off). `canApplyAtEnchantingTable` accepts any enchantment a Diamond Sword, Axe or Pickaxe accepts. Also in the vanilla `swords`, `axes` and `pickaxes` item tags. |
| Super Furnace | `block/furnace/*` | `SuperFurnaceBlock` extends `AbstractFurnaceBlock`; `SuperFurnaceBlockEntity` extends `AbstractFurnaceBlockEntity` (so the vanilla `FurnaceMenu`/`FurnaceScreen`, recipe book, hoppers, JEI transfer and experience all work) but ticks with its own `serverTick`, reaching the vanilla fields through `dataAccess`. `SuperFurnaceTier` holds speed, fuel efficiency, raw-output and experience multipliers. One block entity type for all five blocks. |
| Scuba Gear | `item/ScubaArmorItem`, `item/ScubaEvents`, `client/ScubaClientEvents` | Armor material with 0 defense; Wetsuit/Flippers add `forge:swim_speed`. Air: `LivingBreatheEvent`. Mining: `PlayerEvent.BreakSpeed` undoes the two vanilla /5 penalties. Vision: `ViewportEvent.RenderFog` (water) and `RenderBlockScreenEffectEvent` (water overlay). |

Data comes from `tools/toolkit/gen_toolkit.py` (recipes in `recipes/toolkit/`, tags, loot tables, models, lang) and textures from `tools/textures/gen_toolkit.py`. JEI: the furnaces are catalysts for `RecipeTypes.SMELTING` and `FUELING`, plus info pages.

### The Vestige (0.0.4)

All 0.0.4 code lives under `com.relicweave.vestige` (plus the new rows in `AttunementMapping`, `ModEnchantments`, `InfusionType` and `RelicType`). `VestigeBootstrap` registers every 0.0.4 `DeferredRegister`; each feature area owns its own registry class.

| Area | Package / data | Notes |
|---|---|---|
| IDs | `VestigeKeys` | Dimension `relicweave:the_vestige`, 8 biome keys, 14 structure keys, biome/entity/block tags. Never rename them: worlds store them. |
| Dimension | `data/relicweave/dimension*`, `worldgen/noise_settings/the_vestige.json`, `worldgen/biome/*` | Data-driven. Vanilla overworld noise router, multi-noise source with 8 biomes, `vestige_stone` default block, sea level 48. Client sky and fog in `vestige/client/world`. Terrain blocks: `world/VestigeWorldBlocks`. Data generator: `tools/gen_world_data.py`. |
| Gate | `vestige/gate` | Keystone ritual (block entity), `GateTravel` (return point in persisted player data, safe-spot search, cooldown), `ArrivalPlaza` + `VestigeWorldData` (built once). Advancement `relicweave:vestige/root`. |
| Cycle | `vestige/cycle` | `ConcordanceCycle` is a pure function of world game time. `CycleTicker` fires `VestigeCycleEvent.PhaseChanged` and syncs `CycleSyncPacket`. HUD: `vestige/client/cycle`. |
| Hazards | `vestige/hazard` | `HazardProtection` (contributors combine multiplicatively, capped by `maxHazardProtection`) and `HazardStatus` (HUD flags). Rift Saturation: `hazard/rift` (meter in persisted player data). Gravity, Starfall, Null Pockets, Hush: `hazard/field`. |
| Network | `vestige/network/VestigeNetwork` | Separate channel `relicweave:vestige`, server → client only: cycle, hazard state and block reveals. |
| Mobs | `vestige/entity/{humanoid,beast,ethereal}` | `VestigeSpawnRules` is the single natural-spawn gate (rate, elite rate, local cap). `Telegraph` provides wind-up cues. Spawns come from `forge/biome_modifier/spawns_*.json`. |
| Structures | `tools/structures/nbtgen.py`, `gen_part1.py`, `gen_part2.py` | Python generators write the NBT templates and the jigsaw structure, pool, structure-set and biome-tag JSON. Re-running them reproduces identical files. |
| Puzzle blocks | `vestige/puzzle/PuzzleBlocks` | False Wall, Hidden Glyph (`#relicweave:hidden_glyphs`), Glyph Seal (`#relicweave:breakable_seals`), Ancient Terminal. |
| Bosses | `vestige/boss` (framework), `boss/{a,b,c}` | `VestigeBoss`: arena leash, boss bar, reset, scaling, phases. `BossAltarBlockEntity`: summon, rematch cooldown. `BossRewards`: per-player advancement `relicweave:vestige/<boss>`, awarded once. `SealGateBlock`: lets through only players who beat the Warden. |
| Gear and Loom | `vestige/gear` | Aetherforged tier and perk, smithing recipes, Reliquary Loom (menu buttons validated on the server). JEI: `compat/jei/vestige/VestigeJeiPlugin`. |
| Relics | `relic/RelicType` (appended), `relic/RelicAugment`, `vestige/relic` | Same activation, cooldown and locator code as 0.0.3. Augments are stored as `{"relicweave:augment":{id,v:1}}`; unknown data reads as no augment. |
| Quests and primer | `quest/QuestTrigger`, `quest/QuestEvents`, `vestige/primer/TravelersPrimer`, `tools/quests/gen_quests.py` | Advancement tabs `relicweave:relicweave/*` (22) and `relicweave:vestige/*` (35), generated by the script. Custom steps use the `relicweave:quest` trigger with an `event` name, fired from server code. The primer is a written book given once on `minecraft:end/kill_dragon`, and craftable (the recipe result carries NBT). |
| Enchantments / infusions | `enchantment/vestige`, `infusion/effects/vestige` | Six new `AttunementMapping` rows with source tags and recipes. The 0.0.2/0.0.3 recipes are frozen by `MappingBaselineGameTests`. |

Configs: `relicweave-vestige-server.toml` (`VestigeConfig`: every 0.0.4 balance value) and `relicweave-client.toml` (`RelicWeaveClientConfig`). Resource fragments from parallel work were merged with `tools/merge_fragments.py`. Textures are generated by `tools/textures/gen_*.py` from one shared palette (`vestige_palette.py`).

### Curios (optional)

Curios is a compile-only dependency and is never bundled or required. With Curios 5.x installed, players get one `charm` slot (`data/curios/curios/slots/charm.json`, assigned by `data/relicweave/curios/entities/relicweave.json`); all charms, relics and totems are in the `curios:charm` tag. Only `compat/curios/CuriosAccess` touches the Curios API, and it is loaded only after `ModList` confirms Curios is present.

### Server configuration

`<world>/serverconfig/relicweave-server.toml`:

| Key | Default | Meaning |
|---|---|---|
| `relics.charmActivation` | `INVENTORY_OR_EQUIPPED` | Or `EQUIPPED_ONLY` (offhand and Curios slots) |
| `relics.curiosTotems` | `false` | Let totems work from a Curios slot |
| `cooldowns.wayfarersRelic` / `echoCharm` / `guardianRelic` / `featherwovenCharm` | 300 / 45 / 120 / 10 | Seconds |
| `duplicateEffects.absorptionCap` | 8 | Max half-hearts of absorption Guardian, Soulguard and Warding grant together |

## Controls

| Action | Default key |
|---|---|
| Purling: Throw Ender Pearl | `V` |
| Activate Relic | `R` |
| Cycle Relic | `Z` (was `G` in 0.0.3; moved automatically if it clashes with Curios) |

You can rebind all three keys in game under *Options → Controls → Key Binds → RelicWeave*. Key-binding mods such as [Controlling](https://modrinth.com/mod/controlling) also list them, and you can search for them, rebind, reset, or check for conflicts there. Your choices are saved in `options.txt` as `key_key.relicweave.purling`, `key_key.relicweave.activate_relic` and `key_key.relicweave.cycle_relic`. Tooltips and the relic HUD show the current bindings.

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
| `<row>_1.json` (6 files) | Attunement source tag | see *Attunement source items* | Reaping, Undertow, Resonance, Soulguard, Steady Draw, Reprisal I books |

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
| Lodestone Charm | `" S "`, `"MLM"`, `" M "`: S = String, M = Iron Ingot, L = Lodestone |
| Tidal Charm | `" S "`, `"PHP"`, `" P "`: S = String, P = Prismarine Crystals, H = Heart of the Sea |
| Featherwoven Charm | `" S "`, `"FMF"`, `" F "`: S = String, F = Feather, M = Phantom Membrane |
| Wayfarer's Relic | `" E "`, `"ACA"`, `" A "`: E = Eye of Ender, A = Amethyst Shard, C = Recovery Compass |
| Echo Charm | `" S "`, `"EAE"`, `" E "`: S = String, E = Echo Shard, A = Amethyst Shard |
| Guardian Relic | `"GSG"`, `"SDS"`, `"GSG"`: G = Block of Gold, S = Shield, D = Diamond Charm |

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
./gradlew runClient -PwithCurios       # dev client with Curios, for testing the charm slot
./gradlew runGameTestServer -PwithCurios  # GameTests with Curios installed
```

`-PwithControlling` and `-PwithCurios` are optional and only affect dev runs. RelicWeave never bundles or requires Controlling or Curios.

You need a JDK 17 or newer to run Gradle. The build compiles with a Java 17 toolchain and downloads one through the Foojay resolver if none is installed.

## Automated tests

**0.0.4.5.b:** 359 GameTests (`DreadGameTests` adds 4).

**0.0.4.5:** 355 GameTests, all passing; `ToolkitGameTests` (13) covers the Multitool, Super Furnace and Scuba Gear.

**0.0.4:** 334 GameTests, all passing without Curios and with `-PwithCurios`. The 0.0.4 classes are `MappingBaselineGameTests` (freezes every 0.0.2/0.0.3 machine recipe), `VestigeWorldGameTests`, `GateGameTests`, `CycleGameTests`, `RiftGameTests`, `FieldHazardGameTests`, `SpawnRulesGameTests`, `HumanoidMobGameTests`, `BeastMobGameTests`, `EtherealMobGameTests`, `StructurePart1GameTests`, `StructurePart2GameTests`, `BossesAGameTests`, `BossesBGameTests`, `BossesCGameTests`, `GearGameTests`, `LoomGameTests`, `VestigeRelicGameTests`, `VestigeEnchantmentGameTests`, `VestigeInfusionGameTests` and `QuestGameTests`. GameTests run concurrently in a grid, so tests assert only on the entities they spawned.

The 0.0.3 suite had 102 Forge GameTests in `com.relicweave.gametest`: `RelicWeaveGameTests` (11), `EnchantmentGameTests`, `RelicGameTests`, `InfuserGameTests`, `CelestialGameTests`, `UpboundGameTests`, `GuideGameTests`, and for 0.0.3 `AttunementGameTests`, `MachineFeedbackGameTests`, `AttunementEnchantmentGameTests`, `AttunementInfusionGameTests` and `AttunementRelicGameTests`. All 102 pass both without Curios and with `-PwithCurios`. They run on a dedicated-server dist, so they also confirm that no client-only class loads on a server. They cover:

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
- 0.0.3 Attunement:
  - Every source item has both a transmutation and an infusion pathway; the validator reports a missing side; default source items match the plan.
  - Machine status for every message, completion counted exactly once, and no completion on failure or a full output.
  - Each enchantment's limits (Reaping area, replanting, immature crops and durability; Undertow water/boss rules; Resonance cap and reset; Soulguard cooldown and cap; Steady Draw multipliers; Reprisal timing window and cooldown).
  - Each infusion end to end through the infuser, wrong-piece refusal, the 3-infusion limit with effect infusions, and each effect's numbers.
  - Relics: inventory/offhand locations, Curios slot (with Curios), duplicates counting once, cooldowns persisted and unaffected by moving items, refused activations, Echo radius, Guardian's non-lethal rule, Featherwoven, Wayfarer binding/dimension/cancel, Lodestone toggle and pull, and all relic recipes.
  - The updated guide is given once to players who had the 0.0.2 guide.

### Manual in-client check

0.0.2, checked in a dev client:
- Item and armor textures render, including Celestial armor on the player.
- The Celestial Infuser GUI infused a Celestial Helmet with Golemed. The tooltip lists the infusion and the attribute is applied.
- Upbound II gave two air jumps.
- Ore placement, measured by replacing ore in 9 freshly generated chunks: 47 Celestial Ore in the End and 9 Nether Celestial Ore in the Nether.

0.0.1b: with Controlling 12.0.2 installed, I searched for the Purling key, rebound it from `V` to `G`, and confirmed that `G` throws the pearl while `V` no longer does. The new binding was saved to `options.txt`.

Checked in a single-player dev client: the block model and textures, opening the GUI, shift-click insertion, the progress arrow, Keen Edge I books produced and the output slot blocking further work, combining Keen Edge I + I into II at the anvil, applying it to an iron sword, and Purling (no landing damage, cooldown message on a second press).

## Known limitations

- **0.0.4 client-side content has not been viewed in a client yet:** Vestige sky and fog, mob and boss models and textures, HUDs, the Loom screen, reveal outlines and JEI pages. Structures were verified by their generators and GameTests, not walked in-game.
- GameTest fake players cannot receive advancements, so boss and gate advancements are not tested end to end.
- Sealed Custodian chests can still be emptied by hoppers or explosions.
- **0.0.3 client-side features are not checked in a client yet:** the relic HUD, key handling, Tidal fog, Steady Draw movement, the machine status line and flash, and the JEI layout compile and follow the APIs, but no one has looked at them in a running client.
- Wayfarer's Relic: GameTests confirm the channel completes and starts the cooldown, but the test player cannot actually move, so the teleport itself is untested.
- Echo Charm uses vanilla Glowing, which every player can see.
- Warding's charge state is kept across logout (cleared only by death).
- Resonance streaks and the Pursuit, Soulguard and Reprisal internal cooldowns are kept in memory and reset on server restart (relic cooldowns are saved).

- **Combining at the anvil:** Keen Edge books combine normally (I + I → II, up to III). Purling has a max level of I, so two Purling books can't be combined into a higher level.
- **Multiplayer:** this build hasn't been tested with two or more real players on a dedicated server. All Purling and table state is server-side and per-player or per-block, and the GameTests run on a dedicated-server dist.
- The table doesn't expose an item-handler capability, so hoppers and pipes can't insert or extract items.
- Purling cooldowns aren't saved across server restarts. Totem cooldowns use the vanilla item cooldown, which also resets when you relog.
- Neither infusion block (Transmutation Table, Celestial Infuser) exposes an item-handler capability, so hoppers can't automate them.
- Risen Minions are removed when their chunk unloads and reloads, because their AI would reset and they'd turn hostile.
- Villager's Best Friend uses the Hero of the Village effect (hidden icon, 5 s), so it also briefly counts for other Hero of the Village checks.
- Textures for the 0.0.2 items are recolors of vanilla textures (placeholder-quality art).
