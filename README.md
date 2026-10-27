# RelicWeave

## Description

RelicWeave adds a **Transmutation Table** that turns materials into enchanted books with new enchantments. It also adds **relics** (charms, totems and steaks) and **Celestial**, a material tier above Netherite.

### CurseForge

RelicWeave is a Forge mod for Minecraft Java Edition 1.20.1 that adds a new transmutation system, powerful relic gear, and celestial-tier upgrades. It turns materials into enchanted books, grants passive buffs through charms and totems, and lets players push their gear beyond standard Netherite. The mod focuses on progression, experimentation, and utility, with each feature obtainable through survival play and supported by datapack-based recipes. It gives players a deeper sense of customization without sacrificing the vanilla feel. The diverse enchantments, relics, and infusion mechanics are designed to reward exploration, combat, and resource gathering. For modpack creators and players alike, RelicWeave adds a flexible, expansion-focused layer to the endgame.

### Transmutation Table
Put an input material and a catalyst into the table. After a short process, it gives you an enchanted book. Apply the book at an anvil, and combine two books to raise the level. These enchantments are fully compatible with vanilla anvils and can be boosted through combining.

| Enchantment | Made from | Goes on | What it does |
|---|---|---|---|
| Keen Edge (I–III) | Diamond | Swords, Axes | +1 melee damage per level |
| Purling (I) | Ender Pearl | Boots | A key (default **V**, rebindable) throws an Ender Pearl with no fall damage on landing. No pearls needed. 30 s cooldown |
| Cobbled Up (I–III) | Cobblestone | Pickaxes | Chance of extra cobblestone when mining stone |
| Oxidation (I–III) | Copper | Swords, Axes | Melee hits slow the target; level III also weakens it |
| Conductive (I–III) | Redstone | Tools | +25% mining speed per level |
| Golemed (I–III) | Iron | Chestplates | +1 armor and extra knockback resistance per level |
| Piglinified (I) | Gold | Helmets | Piglins stay neutral, as if you wore gold |
| Villager's Best Friend (I–II) | Emerald | Leggings | Discounted villager trades |
| The Necromancer (I–III) | Netherite | Swords, Axes | Slain monsters may rise as a friendly Husk that fights for you for 30 s |
| Necromanced (I) | Wither Skeleton Skull + Soul Sand | Helmets | Undead ignore you unless you attack them; immune to Wither |
| Upbound (I–II) | Nether Star | Boots | Double jump: one extra mid-air jump per level |
| Reaping (I–III) | Oak Sapling | Hoes | Breaking a mature crop harvests and replants mature crops around it (3×3 / 5×5 / 7×7) |
| Undertow (I–III) | Prismarine Shard | Tridents | Hits on targets in water slow them and give you Dolphin's Grace |
| Resonance (I–III) | Echo Shard | Pickaxes | Mining the same block type in a row gets faster, up to +10% per level. Not combinable with Conductive |
| Soulguard (I–II) | Ghast Tear | Chestplates | Killing a hostile mob gives a small, short absorption shield |
| Steady Draw (I–II) | Phantom Membrane | Bows | Walk faster while drawing a bow |
| Reprisal (I–III) | Copper Ingot + Lapis | Shields | Blocking right after raising your shield weakens and slows the attacker |

### Attunement (new in 0.0.3)
Six source items each have **two separate uses**: in the Transmutation Table they make one of the six new enchantments above, and in the Celestial Infuser they make one of four new armor infusions. You pick one use per item; the two effects are never applied together.

| Source item | Transmutation Table | Celestial Infuser |
|---|---|---|
| Prismarine Shard | Undertow | **Tidal** (helmet): water breathing underwater |
| Echo Shard | Resonance | **Warding** (chestplate): a ward softens one hit, recharges every 20 s |
| Phantom Membrane | Steady Draw | **Pursuit** (leggings): short speed burst after killing a hostile mob |
| Copper Ingot | Reprisal | **Anchored** (boots): crouch to take 75% less knockback |
| Ghast Tear | Soulguard | **Warding** (chestplate) |
| Oak Sapling | Reaping | **Anchored** (boots) |

Source items are set by item tags (`relicweave:attunement_sources/<name>`), so datapacks can swap them without changing recipes.

### Relics
- **Charms** (Copper, Iron, Gold, Diamond, Netherite) work from anywhere in your inventory. They give speed, armor, luck, max health, and fire resistance respectively.
- **Totems** (Diamond, Netherite, Ultra) give passive buffs while held in your offhand. They also save you from death. They are **never used up** and go on a cooldown instead. The Ultra Totem works from any slot in inventory, not just the offhand.
- **New relics (0.0.3):** Lodestone Charm (toggle: pulls nearby drops to you), Tidal Charm (water breathing and clearer water), Featherwoven Charm (half fall damage, slow falling on long falls), Wayfarer's Relic (bind a spot, then channel for 3 s to return to it), Echo Charm (nearby hostile mobs glow) and Guardian Relic (an absorption shield when a hit leaves you at 3 hearts or less).
- **One key for all active relics:** **Cycle Relic** (default **Z**) picks which relic to use, shown next to your hotbar; **Activate Relic** (default **R**) uses it. Both can be rebound under *Options → Controls → Key Binds → RelicWeave*.
- Relic cooldowns are kept per player, so swapping, dropping or re-equipping a relic, dying or relogging never resets them.
- **Diamond and Netherite Steaks** are top-tier food with regeneration, absorption and protective effects.

### Celestial
- **Celestial Ore** generates in End Stone, and more rarely in Netherrack. It drops Celestial Shards.
- Combine 4 Celestial Shards and 1 Netherite Ingot into a **Celestial Ingot**.
- Upgrade any Netherite tool or armor piece at a **Smithing Table** with a Celestial Ingot. Celestial gear is more durable, faster and stronger than Netherite.
- The **Celestial Infuser** adds up to 3 different infusions to each Celestial armor piece, for example +1 armor, +2 max health or reduced gravity. There are 13 infusions: 9 attribute infusions that fit any piece and stack across pieces, and 4 effect infusions (0.0.3) that each fit one piece.
- Both machines now say why they stopped (*Missing ingredient*, *Output full*, *Maximum infusions reached*, …) and play a short sound, particles and a flash when they finish.

### Dread (new in 0.0.4.5.b)
The Vestige is now dark and oppressive: a near-black sky, close dark fog, dim daylight (torches stay bright), drifting ash and distant sounds. While you are there the HUD darkens too, with shimmering black-and-purple hearts and XP bar, a dark hotbar and creeping purple veins that pulses at low health. Adjust it with `dread` and `darkHud` in the client config.

### Toolkit (new in 0.0.4.5)
- **Multitool** (Iron, Diamond, Netherite, Celestial): a sword, an axe and a pickaxe in one tool. Takes all three tools' enchantments.
- **Super Furnace**, five tiers (Iron → Gold → Diamond → Netherite → Celestial): 2x to 10x smelting speed, better fuel use, 2x output from ores and raw materials from Diamond up (3x at Celestial), and no fuel at all at Celestial.
- **Scuba Gear** (Mask, Air Tank, Wetsuit, Flippers): no armor protection, but endless air, clear underwater vision, full mining speed underwater and faster swimming.

### The Vestige (new in 0.0.4)
A post-End dimension: the ruins of the Concordance, a civilization that tried to lock its world into one magical cycle.

- **Getting there:** after defeating the Ender Dragon, build a **Concordance Gate** (a Gate Keystone ringed by 8 Concordance Frames) and wake it with a **Threshold Sigil**, made from Amethyst, Chorus Fruit, End Stone Bricks and Dragon's Breath. You arrive at a safe **Arrival Plaza**; its **Return Waystone**, or any gate you build in The Vestige, takes you back.
- **Eight biomes:** Crownfall Basin, Glassroot Wilds, Aurelian Canopy, Hushfen, Sundered Reach, Starfall Caldera, Gloam Archive and the Crown of Concordance.
- **The Concordance Cycle:** Quiet → Resonance → Convergence. Each phase changes spawns and hazards, and a warning comes before Convergence. The phase shows in the top-right corner.
- **Hazards:** Rift Saturation, Gravity Shear, Starfall Impacts, Null Pockets and Hushfield. Each one warns you first and can be countered, and every value is configurable. **Ward Salve**, infusions, relics and Aetherforged armor reduce them, but protection is capped at 80%.
- **22 creatures:** common foes, elites, neutral wildlife, the Archive Mimic, three mini-bosses and the rare Cycle Herald. Every attack has a visible wind-up.
- **14 structures:** waystones, outposts, temples, warrens, archives, observatories, vaults, the Starfall Forge, the Hall of Nine Seals, the Crown Bastion, the Crown of Concordance and Rift Scars.
- **Six bosses**, each woken at a **Boss Altar** in its arena: the Thorned Regent, the Choir Unheard, the Gravity Maw, the Starforged Colossus, the Warden of Seals and Archivist-Prime. Bosses reset if everyone leaves and can be fought again after a cooldown. Defeating the Warden of Seals opens the **Seal Gates** to the final boss.
- **Aetherforged gear:** upgrade Netherite gear at a Smithing Table with a **Vestige Upgrade Template** and **Concordant Alloy** (Aetherite Ingots + a boss core). It is a sidegrade to Netherite with slightly more durability and toughness, and it softens Rift Saturation. Celestial stays the strongest tier.
- **Reliquary Loom:** gives each of the six new relics one of two reversible augments, Exploration or Stability.
- **Six new relics:** Surveyor's Lens, Gravity Nail, Cycle Dial, Riftglass Lantern, Mnemonic Spindle and Sealkeeper's Signet. They use the same Activate/Cycle Relic keys.
- **Getting started:** defeating the Ender Dragon gives you **The Vestige: A Traveler's Primer**, a short spoiler-free book on building and waking a gate and getting home. It can also be crafted from a Book, an Eye of Ender and an Amethyst Shard.
- **Quest line:** two advancement tabs, *RelicWeave* (the machines and earlier content) and *The Vestige* (57 quests in all). They guide you from your first Transmutation Table to Archivist-Prime.
- **New Attunement rows:** six Vestige materials each make one new enchantment *or* one new infusion. The 0.0.2 and 0.0.3 recipes are unchanged.

| Source item | Transmutation Table | Celestial Infuser |
|---|---|---|
| Aetherite Shard | **Riftwalker** (boots) | **Gravitic Tread** (boots) |
| Inscribed Page | **Glyphfinder** (helmet) | **Riftward** (helmet) |
| Sovereign Thorn | **Sovereign's Bane** (sword/axe) | **Sealguard** (chestplate) |
| Mnemonic Filament | **Cyclebound** (leggings) | **Cycleweave** (leggings) |
| Riftglass Lens | **Sealbreaker** (pickaxe) | **Gravitic Tread** (boots) |
| Concordant Seal | **Archivist's Mark** (bow/crossbow) | **Sealguard** (chestplate) |

### Details
- Everything can be obtained in survival. Transmutation and infusion recipes are datapack JSON, so modpack makers can add or change them.
- Every player gets a **RelicWeave Guide** book the first time they join a world. It explains the Transmutation Table, the Celestial Infuser, Attunement, relics, controls and every recipe. Players who had an older guide get the new 45-page edition once.
- **Just Enough Items (JEI) support (optional):** every Transmutation Table and Celestial Infuser recipe has its own JEI page with counts and process times, and each Attunement source item has an info page listing both of its uses. Click the arrow in either machine's GUI to open them. JEI is not required.
- **Curios support (optional):** with [Curios](https://www.curseforge.com/minecraft/mc-mods/curios) installed, players get a charm slot for charms and relics. Curios is not required.
- **Server config** (`serverconfig/relicweave-server.toml`): relic cooldowns, whether charms work anywhere in the inventory or only when equipped, whether totems may sit in a Curios slot, and the absorption cap. **`relicweave-vestige-server.toml`** holds every Vestige setting: gate rules, cycle lengths, hazard strengths, spawn rates, boss scaling and rematches, 0.0.4 relic cooldowns and Loom costs.
- **Client config** (`config/relicweave-client.toml`): Vestige music and ambience, particle density, distortion strength and the hazard HUD style.
- All items are listed in the **RelicWeave** and **RelicWeave: The Vestige** creative tabs.
- Requires Forge. Must be installed on both the client and the server.

## Versions

| | |
|---|---|
| Mod version | 0.0.4.5.b |
| Minecraft | Java Edition 1.20.1 |
| Mod loader | Forge 47.3.0 or newer |
| Java | 17 |

## Authors

Dutch and Claude
