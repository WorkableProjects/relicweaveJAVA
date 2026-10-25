# Changelog

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
