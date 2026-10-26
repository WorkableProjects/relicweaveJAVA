# Changelog

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

### Configuration, UI and guide
- New server config `relicweave-vestige-server.toml`: gate, cycle, hazards, mobs, bosses, relics and Loom.
- New client config `relicweave-client.toml`: music, ambience, particle density, distortion, hazard HUD.
- Debug commands for operators (permission level 2): `/relicweave gate|cycle|hazard|world ...`.
- **Guidebook:** grows from 24 to 45 pages. Players with an older guide get the new edition once.
- Two creative tabs: RelicWeave, and RelicWeave: The Vestige.
- The network gained a separate `relicweave:vestige` channel, so clients and servers must both run 0.0.4.

### Tests
- TEST_COUNT GameTests (102 in 0.0.3). They cover the mapping baseline, worldgen, gate travel, the cycle, hazards, all mobs, structures, bosses, gear, the Loom, relics, enchantments and infusions.
- A dedicated server boots with no client-only class loading.

### Known limitations
- Nothing client-side has been looked at in a running game client yet: models, textures, animations, HUDs, fog, the Loom screen, reveal outlines and JEI layouts. All of it compiles and follows the Forge/JEI APIs.
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
