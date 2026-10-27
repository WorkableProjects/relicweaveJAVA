# Changelog

## 0.0.4.5.b — Dread

Client-side look and HUD only. No gameplay, recipe or save-data changes.

### A darker Vestige
- **Sky:** a near-black dome that fades into the fog at the horizon, with faint cold stars that turn red during Convergence. No sun glare and no clouds. The Concordance rings now glow dimly against the dark.
- **Light:** daylight and ambient light are up to 55% dimmer. Block light (torches, lava, glowing blocks) is as bright as ever, so carrying a light matters. Caves are close to pitch black.
- **Fog:** dark and close. It starts a few blocks away and is a solid wall at about 72 blocks (52 in Convergence). It turns blood red in Convergence. Hushfield fog now thickens it further (before, the Vestige haze stopped Hushfield fog from applying).
- **Ambience:** ash drifts through the air around you (follows *Particle density*). Souls rise during Convergence. Every 1.5–4 minutes a distant cave sound comes from somewhere in the fog (follows *Ambience*).

### A darker HUD (while in The Vestige)
- **Vanilla HUD darkened:** hearts and the XP bar turn black and purple, with a slow purple shimmer moving across them. Hunger, armor, air, the hotbar frame and the selected slot are all darker. The darkened textures are made at runtime from your active resource pack, and the originals come back when you leave. The crosshair is not changed.
- **Dread vignette:** a slowly breathing black vignette and two layers of purple veins that creep in and out at the screen edges (crimson in Convergence). Below 40% health the veins pulse red like a heartbeat.
- **Cycle and hazard readouts** sit on a dark plate with bone-white text and a crimson Convergence warning.

### Settings (`config/relicweave-client.toml`, `[vestige]`)
- `dread` (0–1, default 1): how dark The Vestige looks. 0 is the brighter 0.0.4 look.
- `darkHud` (default true): darkened HUD and the dread vignette.

### Tests
- 4 new GameTests in `DreadGameTests` cover the fog, light and HUD colour math. 359 in total, all passing.

## 0.0.4.5 — The Toolkit Update

Minecraft 1.20.1 · Forge 47.3.0+. Adds three pieces of everyday gear. Nothing from 0.0.4 or earlier changes; recipe IDs, items and player data stay valid.

### Multitool (sword, axe and pickaxe in one)
- Four tiers: **Iron**, **Diamond**, **Netherite** and **Celestial**.
- Mines everything a pickaxe or an axe can, at its tier's speed and mining level. Cuts cobwebs like a sword.
- Hits like a sword: same damage, attack speed and sweeping attack. Can disable shields like an axe.
- Right-click strips logs, scrapes copper and removes wax, like an axe.
- Takes every sword, axe and pickaxe enchantment (Sharpness, Sweeping Edge, Looting, Efficiency, Fortune, Silk Touch and so on), at the enchanting table or the anvil.
- Twice its tier's durability. Each block or hit costs 1.
- **Recipes:** Iron = Iron Sword + Iron Axe + Iron Pickaxe + Iron Ingot (shapeless). Diamond = the same with Diamond tools and a Diamond. Netherite and Celestial: Smithing Table with a Netherite Upgrade template plus a Netherite Ingot or a Celestial Ingot.

### Super Furnace (five tiers)
Works like a Furnace: the same slots, recipe book, hopper sides, comparator output and experience. Each tier is crafted from the one before it.

| Tier | Speed | Fuel goes | Ores and raw materials | Experience | Recipe |
|---|---|---|---|---|---|
| I Iron | 2x | 1x as far | 1x | 1x | Blast Furnace, 5 Iron Ingots, 3 Smooth Stone |
| II Gold | 3x | 1.25x | 1x | 2x | Iron Super Furnace, 4 Gold Ingots, 4 Magma Blocks |
| III Diamond | 4x | 1.5x | **2x** | 2x | Gold Super Furnace, 4 Diamonds, 4 Blaze Rods |
| IV Netherite | 6x | 2x | **2x** | 3x | Smithing: Diamond Super Furnace + Netherite Ingot |
| V Celestial | 10x | **no fuel needed** | **3x** | 3x | Smithing: Netherite Super Furnace + Celestial Ingot |

- "Ores and raw materials" means anything tagged `forge:raw_materials` or `forge:ores` (raw iron, raw gold, iron ore and so on), via the new item tag `relicweave:super_furnace_multiplied`. Other smelting (sand, food, logs) gives the normal amount.
- Netherite and Celestial Super Furnaces are blast-resistant and their items don't burn.
- The tooltip lists each tier's perks.

### Scuba Gear (no armor protection)
- **Scuba Mask:** clear underwater vision (no fog wall or water overlay) and full mining speed underwater, even while floating.
- **Scuba Air Tank:** you never run out of air.
- **Scuba Wetsuit:** +50% swim speed. **Scuba Flippers:** +100% swim speed. Both also speed up walking underwater.
- Every piece gives **0 armor and 0 toughness**. Enchantable, repaired with Dried Kelp.
- **Recipes:** made from Leather, Slime Balls, Dried Kelp, Glass Panes, Iron Ingots and a Bucket.

### JEI
- All recipes show in JEI's Crafting and Smithing categories.
- Every Super Furnace is listed as a catalyst for Smelting and Fuel, so clicking one shows everything it can smelt.
- Info pages for the Multitool, the Super Furnace tiers and Scuba Gear.

### Quests and guide
- 7 new quests in the *RelicWeave* advancement tab: *Jack of All Trades*, *Master of All*, *Hotter Than Usual*, *Twice the Ingots*, *Starfire*, *Deep Breath* and *Under the Sea*.
- Recipe-book entries unlock when you hold an ingredient.
- The RelicWeave Guide gains pages 46-48 (Toolkit). Players with the 0.0.4 guide get the new edition once.

### Fix
- The Concordance Gate's travel sound pointed at a vanilla sound file that does not exist, so it was silent. It now plays the Enderman teleport sound.

### Tests
- 13 new GameTests in `ToolkitGameTests` (355 in total, all passing): Multitool mining, enchantments and log stripping; Super Furnace speed, fuel, 2x/3x output, experience and fuel-free Celestial smelting; Scuba Gear breathing, mining speed and zero armor; recipes load.

## 0.0.4 — The Vestige Update

Minecraft 1.20.1 · Forge 47.3.0+. All 0.0.2 and 0.0.3 items, enchantments, infusions, recipes, recipe IDs and player data are unchanged and stay valid. GameTests freeze every 0.0.2/0.0.3 Transmutation Table and Celestial Infuser recipe, including the Iron Ingot (Golemed) and Nether Star (Upbound) mappings.

### Fix
- **Cycle Relic now works with Curios.** Curios binds *Open Curios Inventory* to `G`, which was also the 0.0.3 Cycle Relic default, so with Curios installed you could never switch relics and only the first activatable one could be used. Cycle Relic now defaults to **`Z`**. On first launch, a player still on `G` whose `G` clashes with another key is moved to `Z` once. A later rebind is never changed, and the key stays rebindable under *Options → Controls → Key Binds → RelicWeave*.
- Pressing a relic key while carrying only passive relics now says which relic is passive, instead of "No activatable relic".

### The Vestige (new dimension, `relicweave:the_vestige`)
- **Entry:** after the *Free the End* advancement, build a **Concordance Gate**: a Gate Keystone with 8 Concordance Frames around it in a flat 3×3 ring. Wake it with a **Threshold Sigil** (Amethyst Shards, Chorus Fruit, End Stone Bricks, Dragon's Breath) during a 3 s ritual. The sigil is used up only if the ritual finishes. Use an active gate to travel.
- **Arrival and return:** the first trip builds a safe **Arrival Plaza** in Crownfall Basin. Its **Return Waystone**, or any gate built inside The Vestige, sends you back to the place you left. If that spot is gone or unsafe, you return to a safe spot nearby or to world spawn. A short teleport cooldown prevents portal loops.
- **8 biomes:** Crownfall Basin, Glassroot Wilds, Aurelian Canopy, Hushfen, Sundered Reach, Starfall Caldera, Gloam Archive and Crown of Concordance (the rarest). Each has its own surface, flora, ores, fog/sky colors, particles and ambience, plus a Vestige music track. 22 new terrain, wood, flora, ore and decoration blocks, with recipes.
- **Materials:** Aetherite ore → Aetherite Shards → Aetherite Dust and Ingots. Also Riftglass, Riftglass Lens, Inscribed and Sealed Pages, Mnemonic Filament, Reliquary Shards, Aurelian Resin, Aether Down, six boss components and Concordant Alloy.

### Concordance Cycle
- Quiet (9 min) → Resonance (6 min) → Convergence (4 min) by default, configurable. The phase is computed from world time, so every player agrees on it and restarts never reset or skip it.
- Phase changes play a cue and show a message. A warning (sound, subtitle and on-screen text) comes 30 s before Convergence. The current phase is shown in the top-right corner while in The Vestige.

### Hazards (only inside The Vestige; every one is telegraphed, counterable and configurable)
- **Rift Saturation:** a 0–100 meter that rises outdoors in rift biomes and near **Rift Fissures**, twice as fast in Convergence. It warns at 50 and 80. When full it gives a short **Rift Sickness** debuff (slower, weaker) and drops to 60. It never deals damage. **Ward Salve** halves it for 3 minutes.
- **Gravity Shear** (Sundered Reach): a 2 s warning, then an upward or sideways push followed by Slow Falling. Crouch to brace (half strength); within 3 blocks of a **Gravity Anchor** you are safe.
- **Starfall Impacts** (Starfall Caldera, Resonance and Convergence): a marked ring strikes after 3 s. Step out of it or get under cover. No block damage.
- **Null Pockets:** near a Null Ward, active relics do nothing. Cooldowns and charges are untouched.
- **Hushfield** (Hushfen, Gloam Archive): denser fog, plus a noise meter (sprinting, jumping, fighting, mining) that sound-sensitive creatures react to.
- All protection sources combine and are capped at 80%, so nothing makes a hazard irrelevant. The HUD shows a small exposure meter and hazard icons (full or minimal mode).

### Creatures (22)
- **Humanoids:** Reliquary Sentinel, Scriptbound Scribe, Gilded Husk, Mirebound Cantor, Oathbreaker Knight (elite), Vestige Drifter (neutral; trades lore for Inscribed Pages), Rift Archer (elite), Echo Weaver.
- **Beasts:** Glassroot Stalker, Ruin Hound (packs), Aether Grazer (neutral, shearable for Aether Down, no breeding), Shardback Brute (elite), Starfall Strider, Gravity Leech, Archive Mimic.
- **Fliers and guardians:** Fracture Wisp, Nullwing, Eclipse Moth (leads you to rift phenomena during Resonance), the mini-bosses Concordance Guardian, Sealed Custodian and Rune Warden, and the rare **Cycle Herald** (Convergence only).
- Every attack has a visible wind-up and a subtitled sound. Natural spawns pass through one gate with a configurable spawn rate, elite rate and local hostile cap.

### Structures (14)
Waystone Monolith, Archivist's Outpost, Ruined Aether Aqueduct, Gilded Civic Temple, Glassroot Warren, Hush Archive, Sundered Observatory, Gravity Vault, Starfall Forge, Cycle Observatory, Hall of Nine Seals, Crown Bastion, Crown of Concordance and Rift Scar. They contain loot tables, guards, puzzles (False Walls, Hidden Glyphs, Glyph Seals, Ancient Terminals) and the six boss arenas. Riftglass Lenses and the Vestige Upgrade Template each have a guaranteed source.

### Bosses (6)
- Each is woken at a **Boss Altar** in its arena. If no one is in the arena for 30 s, the boss heals and returns to its altar, so death or disconnecting never locks a structure. Health scales modestly with the number of players (+35% each, capped at 2.5×). Rematches are allowed after a 5-minute altar cooldown (configurable).
- **The Thorned Regent** (Glassroot Wilds): root walls, thorn volleys, a root strike that exposes its core, stalker calls. Drops Sovereign Thorns.
- **The Choir Unheard** (Hushfen): sonic lances, silence zones, tone shifts. Every cue is visual as well as audible. Drops Liminal Chords.
- **The Gravity Maw** (Sundered Reach): inhale pulls (hold a Gravity Anchor), repulse, bite, debris. Drops Gravity Hearts.
- **The Starforged Colossus** (Starfall Caldera): heavily armored until you pull a vent lever while it overheats; slams, heat pulses (take cover), starfall barrage. Drops Starforged Cores.
- **The Warden of Seals** (Gloam Archive): immune until its exposed seal nodes are broken; slams, rune lances, summoned sentinels. It always drops 2 Concordant Seals and a Vestige Upgrade Template. Defeating it opens the **Seal Gates** (no item is consumed).
- **Archivist-Prime** (Crown of Concordance): three stages, each adding one rule (lances and glyph volleys, then cycle shifts, then a final seal with an aura that keeps you at range). Drops the Prime Mnemonic Core.
- Each boss has a challenge advancement. Advancements are per player and awarded once, so rematches and relogs never duplicate unlocks.

### Aetherforged gear and the Reliquary Loom
- **Aetherforged** sword, pickaxe, axe, shovel, hoe and armor: smith Netherite gear with a **Vestige Upgrade Template** and **Concordant Alloy**. Enchantments and names are kept.
  - Compared with Netherite: 2400 tool durability (Netherite 2031), mining speed 9.5 (9), the same armor values, toughness 3.5 (3), enchantability 16 (15).
  - Perk: each armor piece gives 6% Rift Saturation protection, and the tools mine Vestige blocks 15% faster.
  - Celestial remains the strongest tier.
- **Concordant Alloy:** 4 Aetherite Ingots + 1 boss core → 2 alloy. The template can be duplicated with diamonds and alloy.
- **Reliquary Loom:** gives each of the six new relics one of two augments, Exploration or Stability. The cost is shown before you confirm. Augments are reversible (removing one returns the base relic), never touch cooldowns, and are validated on the server.

### New relics (6)
- **Surveyor's Lens:** outlines hidden glyphs, false walls, seals and terminals nearby. Never containers.
- **Gravity Nail:** anchors you against gravity pulses and pulls for 5 s.
- **Cycle Dial:** shows the cycle countdown and warns before each phase change.
- **Riftglass Lantern:** a small 30 s zone with slower Rift Saturation.
- **Mnemonic Spindle:** decodes an Ancient Terminal to point toward the nearest structure.
- **Sealkeeper's Signet:** briefly stalls nearby non-boss wardens and opens adjacent Glyph Seals.
- All six use the shared Activate/Cycle Relic keys and per-player cooldowns (`relicweave-vestige-server.toml`), and work in a Curios charm slot.

### New enchantments and infusions (Attunement rows for Vestige materials)
| Source item | Transmutation Table | Celestial Infuser |
|---|---|---|
| Aetherite Shard | Riftwalker I–II (boots): weaker gravity pushes | Gravitic Tread (boots) |
| Inscribed Page | Glyphfinder I–II (helmet): periodically outlines hidden glyphs | Riftward (helmet): 30% slower Rift Saturation |
| Sovereign Thorn | Sovereign's Bane I–III (sword/axe): +1.5/level vs Vestige elites, at most +2 vs bosses | Sealguard (chestplate): cancels one Starfall hit, recharges in 45 s |
| Mnemonic Filament | Cyclebound I–II (leggings): +5/10% speed during Resonance | Cycleweave (leggings): Speed II + Jump I for 10 s at each phase change |
| Riftglass Lens | Sealbreaker I–III (pickaxe): breaks Glyph Seals much faster | Gravitic Tread (boots) |
| Concordant Seal | Archivist's Mark I–II (bow/crossbow): marked targets glow and take +10% player damage | Sealguard (chestplate) |

- Riftwalker and Gravitic Tread share a 50% cap. The 3-infusion limit per Celestial piece is unchanged.
- JEI shows all the new machine recipes, an info page per source item, and the Reliquary Loom augments.

### Look, sound and recipes (playtest fixes)
- **Sky:** each biome has its own sky, fog, grass and water colours (teal basin, golden canopy, violet reach, ink-blue archive, ember caldera, opal crown).
  - Four vast rune rings hang in the northern sky, tinted by the cycle phase (teal / gold / violet) and pulsing during the Convergence warning.
  - Soft distance haze and a fog tint that follows the phase.
  - Shader packs may replace the sky and fog with their own.
- **Landmarks:** floating ruins, monumental broken arches, crystal spires, fallen colossal statues, colonnades, giant root arches, Hushfen mirror pools and Sundered Reach shattered plates.
- **Arrival Plaza:** rebuilt as a 17×17 plaza with arches, lantern pillars, an obelisk, a raised waystone dais and planters, and the hillside around it is cut back into a gentle slope. Existing worlds upgrade their plaza once.
- **Sound:**
  - Biome ambience now plays vanilla Nether ambient loops directly, with extra mood and addition sounds.
  - Vestige music comes round much more often.
  - Particles are denser (floating glyphs, ash, motes).
- **Boss music:** each boss has its own looping track on the Music channel while you are in its arena. It holds back other music and fades out when the fight ends; `bossMusic` in the client config turns it off. The tracks are vanilla music for now.
- **Recipe book:**
  - Every RelicWeave recipe now unlocks in the recipe book when you pick up one of its ingredients (96 unlocks).
  - The Threshold Sigil, Concordance Frame, Gate Keystone and Primer recipes unlock when you defeat the Ender Dragon, and players who already did get them at login.
- **New recipes** for Gilded and Rune Vestige Bricks, Gloam Stone, Glyph Seal, Mnemonic Filament, Inscribed and Sealed Pages, Reliquary Shard, Aurelian Resin, Echo Clay, Aureate Moss, Fractured Vestige Stone and Starfall Slag.
- **JEI:** info pages for 23 items that aren't crafted (ores, boss drops, altars and gates) say where to find them.

### Traveler's Primer and quest line
- **The Vestige: A Traveler's Primer**, an 11-page, spoiler-free book on how to start:
  - covers the Threshold Sigil, Concordance Frame and Gate Keystone recipes, how to lay out and wake the gate, crossing over, getting home, packing, and general safety;
  - given once when you defeat the Ender Dragon (players who already have, get it at their next login);
  - can also be crafted from a Book, an Eye of Ender and an Amethyst Shard.
- **Quest line** in two advancement tabs (the vanilla Advancements screen, no extra mod needed):
  - **RelicWeave** (22 quests): the Transmutation Table and its books, Celestial ore and gear, the Celestial Infuser and infusions, charms, relics and totems, steaks, the Ender Dragon, the Threshold Sigil and waking a gate.
  - **The Vestige** (35 quests): first crossing and the way home, the Cycle, hazards and Ward Salves, Aetherite and Riftglass, structures and biomes, creatures (including a full bestiary challenge), the new relics, the Loom and augments, the new enchantments and infusions, all six bosses in order, the Seal Gate, Concordant Alloy, the upgrade template and Aetherforged gear.
  - Quests give experience. The first crossing also gives 2 Ward Salves and some food.
  - Custom steps use a new advancement trigger, `relicweave:quest`.
  - The six boss quests keep their existing ids; they are now chained in order.

### Configuration, UI and guide
- New server config `relicweave-vestige-server.toml`: gate, cycle, hazards, mobs, bosses, relics and Loom.
- New client config `relicweave-client.toml`: music, ambience, particle density, distortion, hazard HUD.
- Debug commands for operators (permission level 2): `/relicweave gate|cycle|hazard|world ...`.
- **Guidebook:** grows from 24 to 45 pages. Players with an older guide get the new edition once.
- Two creative tabs: RelicWeave, and RelicWeave: The Vestige.
- The network gained a separate `relicweave:vestige` channel, so clients and servers must both run 0.0.4.

### Tests
- 342 GameTests (102 in 0.0.3), all passing both without Curios and with `-PwithCurios`. They cover the mapping baseline, worldgen, gate travel, the cycle, hazards, all mobs, structures, bosses, gear, the Loom, relics, enchantments, infusions, the quest line and the primer.
- A dedicated server boots with no client-only class loading.

### Known limitations
- The dimension's sky, fog, landmarks and Arrival Plaza were checked in a real (software-rendered) client. Mob/boss models, HUDs, the Loom screen, reveal outlines and JEI layouts compile and follow the Forge/JEI APIs but have not been looked at yet.
- Structures and arenas were checked by their generators and GameTests, not walked in-game.
- GameTest fake players cannot receive advancements, so boss advancements are covered by the framework's design and partial tests, not end-to-end.
- Sealed Custodian chests can still be emptied by hoppers or explosions. Creative-mode players bypass the seal.
- Sounds reuse vanilla sound files.
- Infusions still apply only to Celestial armor, not to Aetherforged armor.

## 0.0.3 — Attunement Update

Minecraft 1.20.1 · Forge 47.3.0+. Existing 0.0.2 items, enchantments, infusions, recipes and player data are unchanged and stay valid.

### Attunement source items
Each source item has two independent recipes: an enchanted book in the Transmutation Table, or an infusion in the Celestial Infuser. They are never applied together. Final item IDs (all vanilla, unchanged from the plan):

| Source item | Transmutation Table | Celestial Infuser |
|---|---|---|
| `minecraft:prismarine_shard` | 8 + 1 Amethyst Shard → Undertow I | 16 → Tidal (helmet) |
| `minecraft:echo_shard` | 2 + 2 Amethyst Shards → Resonance I | 4 → Warding (chestplate) |
| `minecraft:phantom_membrane` | 4 + 1 Amethyst Shard → Steady Draw I | 8 → Pursuit (leggings) |
| `minecraft:copper_ingot` | 8 + **4 Lapis Lazuli** → Reprisal I | 32 → Anchored (boots) |
| `minecraft:ghast_tear` | 2 + 2 Amethyst Shards → Soulguard I | 4 → Warding (chestplate) |
| `minecraft:oak_sapling` | 16 + 1 Amethyst Shard → Reaping I | 32 → Anchored (boots) |

- Reprisal uses Lapis Lazuli as its catalyst because 8 Copper Ingots + Amethyst Shard is already the Oxidation recipe.
- Source items come from item tags `relicweave:attunement_sources/<name>`, so datapacks can swap them without changing recipes or code.
- The guidebook, JEI pages and recipe validation all read the same mapping. The server logs an error, and a GameTest fails, if any source item is missing either its enchantment recipe or its infusion recipe.

### New enchantments (Transmutation Table)
- **Reaping I–III** (hoes): breaking a mature crop harvests and replants mature crops in a 3×3 / 5×5 / 7×7 area. Each crop replants with one seed from its own drops and costs 1 durability beyond the first. Immature crops are never touched.
- **Undertow I–III** (tridents): hits on targets in water slow them; a wielder in water gets Dolphin's Grace. Bosses are not slowed.
- **Resonance I–III** (pickaxes): mining the same block type in a row gets faster, up to +10% per level. It resets on a different block or after a 5 s pause, and can't be combined with Conductive.
- **Soulguard I–II** (chestplates): killing a hostile mob gives a small 10 s absorption shield, at most once every 2 s.
- **Steady Draw I–II** (bows): walk at 50% / 80% speed while drawing, instead of 20%. Arrow damage and draw time are unchanged.
- **Reprisal I–III** (shields): blocking within 0.5 s of raising the shield weakens and slows the attacker. It has a 3 s internal cooldown.

### New infusions (Celestial Infuser)
- **Tidal** (helmet): water breathing while underwater.
- **Warding** (chestplate): one ward charge removes 2 hearts from the next hit and recharges after 20 s. There's never more than one charge.
- **Pursuit** (leggings): Speed II for 3 s after killing a hostile mob, at most once every 10 s.
- **Anchored** (boots): crouching cuts knockback by 75%.
- Each of these fits only one armor piece, so identical effects never stack. The 0.0.2 attribute infusions still fit every piece and stack as before. The 3-infusion limit per piece is unchanged and enforced on the server.

### New relics
- **Lodestone Charm**: toggle it on to pull nearby dropped items (6 blocks) toward you. Vanilla pickup rules still apply.
- **Tidal Charm**: water breathing and clearer vision underwater.
- **Featherwoven Charm**: fall damage halved, plus a short Slow Falling during long falls.
- **Wayfarer's Relic**: sneak + use to bind a spot. Activate it to channel for 3 s and return there. It only works in the same dimension; moving or taking damage cancels it with no cooldown. 300 s cooldown.
- **Echo Charm**: nearby hostile mobs (24 blocks) glow for 10 s. 45 s cooldown.
- **Guardian Relic**: a 4-heart absorption shield when a non-lethal hit leaves you at 3 hearts or less. 120 s cooldown. It can't save you from a lethal hit.
- New keys: **Activate Relic** (default `R`) and **Cycle Relic** (default `G`). Rebind them under *Options → Controls → Key Binds → RelicWeave*. The selected relic is shown next to the hotbar.
- Relic cooldowns are saved per player and per relic type. Swapping, dropping or re-equipping relics, dying, respawning, changing dimension, relogging and server restarts don't reset them.

### Machines
- Both machines show why they are idle: *Missing ingredient*, *Output full*, *Maximum infusions reached*, *Already has this infusion*, *Wrong armor piece for this infusion*.
- On completion only, both machines play a sound and show particles, and the GUI flashes. The Transmutation Table plays a shimmer with a rune burst; the Celestial Infuser plays a deep resonance and then a chime. The new sounds have subtitles.

### Compatibility and configuration
- **Curios (optional):** players get a charm slot for charms and relics. RelicWeave runs normally without Curios.
- **Server config** `relicweave-server.toml`:
  - Relic cooldowns.
  - Whether charms work anywhere in the inventory or only when equipped (offhand / Curios).
  - Whether totems may be worn in a Curios slot (off by default).
  - A shared cap on absorption from Guardian Relic, Soulguard and Warding.
- **JEI:** new recipes appear in both machine categories. Effect infusions show only the armor piece they fit, and each source item has an info page listing both of its uses.
- **Tooltips:** relics show their effect, valid locations, activation key, cooldown and duplicate rule. Celestial armor shows infusion effects; hold Shift for the stacking and limit rules.
- **Guidebook:** expanded from 13 to 24 pages. Players who had the 0.0.2 guide get the new edition once.
- The network protocol version changed, so clients and servers must both run 0.0.3.

### Tests
- 102 GameTests (up from 51 in 0.0.2). All pass both without Curios and with Curios installed.

### Known limitations
- No one has looked at the new client-side parts in a running game yet: the relic HUD, key handling, Tidal Charm fog, Steady Draw movement, the machine status line and flash, and the JEI layout. They compile and follow the APIs.
- Wayfarer's Relic teleport: the GameTests cover the channel and cooldown, but not the actual movement.
- Echo Charm uses vanilla Glowing, which every player can see.
