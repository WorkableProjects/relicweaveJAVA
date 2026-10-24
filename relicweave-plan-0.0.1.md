# RelicWeave — Development Plan

**Target release:** `0.0.1`  
**Minecraft:** Java Edition `1.20.1`  
**Mod loader:** Minecraft Forge `47.3.0+`  
**Java:** `17`  
**Package:** `com.relicweave`  
**Mod ID:** `relicweave`
**Authors:** `Dutch and Claude`

## 1. Goal

Build the first playable RelicWeave prototype: a **Transmutation Table** that consumes an input item and a catalyst, then produces an Enchanted Book carrying a custom enchantment. Keep the implementation small, server-authoritative, data-driven where practical, and compatible with Forge 1.20.1.

Claude Code should inspect the existing project before making changes. Preserve working project conventions and configuration unless a change is necessary. Implement the phases below in order, building and fixing the project after each meaningful phase. Do not add features that are not listed in this `0.0.1` scope.

## 2. Release Scope

### Required for `0.0.1`

- Register and place a `relicweave:transmutation_table` block.
- Open a working container GUI by interacting with the table.
- Provide three slots: **Input**, **Catalyst**, and **Output**.
- Process recipes on the server, show progress, save inventory/progress, and prevent item duplication.
- Load custom transmutation recipes from datapack JSON.
- Produce enchanted books for two custom enchantments:
  - **Keen Edge I** — transmuted from Diamonds; increases melee damage when using a Sword or Axe.
  - **Purling I** — transmuted from Ender Pearls; when equipped on Boots, allows the player to trigger an Ender Pearl throw through a dedicated keybind, with a 30-second cooldown and reduced or prevented teleport impact damage.
- Support custom enchantment books and normal anvil application/combination where vanilla Forge behavior permits.
- Include a concise README with setup, build, controls, and recipe locations.

### Explicitly out of scope for `0.0.1`

- A custom progression tree, achievements, configuration screen, or large content pack.
- Custom world generation, structures, mobs, or dimensions.
- A custom “Nether Star fragment” item. Use an existing vanilla catalyst until a later version explicitly adds that item.
- Extra enchantments, particle-heavy effects, custom animations, or extensive balancing options.
- A separate table-based enchantment-book upgrade interface. Keep anvil combining as the initial upgrade path; document any Forge/vanilla limitations discovered during implementation.

## 3. Core Gameplay Rules

### Transmutation Table

The table has three slots:

1. **Input:** item consumed by the selected recipe.
2. **Catalyst:** catalyst consumed by the selected recipe.
3. **Output:** result slot; players may take items out but cannot insert items into it.

A recipe may run only if both ingredients and their required counts are present and the output can accept the complete result. Do not consume items until the operation can complete safely. If the recipe or output becomes invalid while processing, reset or pause progress consistently; never duplicate or delete ingredients. Store inventory and progress in the block entity so they survive save/reload.

Use a modest initial processing time (for example, 100 ticks) and expose it as a recipe JSON field. Keep all inventory mutation and completion logic server-side. The GUI may display progress, but the client must not decide when items are consumed or results are created.

### Initial Recipes

Add example datapack recipes for:

- **Diamond + Amethyst Shard → Keen Edge I book**
- **Ender Pearl + Amethyst Shard → Purling I book**

Keep ingredient counts, catalyst counts, processing time, enchantment ID, and level in recipe data rather than hard-coding the two pairings in the block entity. Higher levels can later use multiple input items and rarer catalysts through additional JSON recipes.

### Keen Edge

- Can be applied to Swords and Axes.
- Each level adds a small, predictable melee-damage bonus; start at **+1 damage per level** and keep the value in one clearly named constant.
- Apply the bonus through an appropriate Forge server-side damage event or another verified Forge 1.20.1 hook. Ensure it applies only to direct player melee attacks using a qualifying enchanted item, not unrelated damage sources or projectiles.
- Prevent duplicate application of the bonus in the same attack.

### Purling

- Can be applied to Boots for `0.0.1`.
- Add a client keybind (choose a sensible default and document it) that sends a request to the server. The server validates that the player is alive, has Purling on equipped boots, is not on cooldown, and has an Ender Pearl available.
- Consume one Ender Pearl from the player's inventory when activation succeeds, then launch a vanilla-compatible Ender Pearl projectile.
- Enforce a **30-second (600-tick) cooldown on the server**. Do not trust the client to enforce cooldowns.
- Reduce or prevent the teleport's self-inflicted impact damage without removing ordinary fall damage or unrelated damage. Track the mod-triggered teleport path so the protection is scoped narrowly.
- If the player lacks a pearl or is on cooldown, activation must fail cleanly without consuming items.

## 4. Technical Design

Use Forge 1.20.1 APIs and Java 17. Do not copy newer-version APIs into this project. Confirm method signatures against the project's Forge MDK and mappings before implementing event hooks, menu synchronization, or networking.

### Registries

Use `DeferredRegister` and the appropriate Forge registries for:

- Blocks and block items
- Block entity types
- Menu types
- Enchantments
- Recipe serializers and recipe types

Register the client screen and key mapping only from client-side setup. Keep client-only classes out of common/server initialization paths.

### Suggested Classes

Use these names unless the existing project has an equivalent convention:

- `RelicWeave` — mod entry point and registry/event setup
- `ModBlocks`, `ModItems` — block and item registrations (only create `ModItems` if custom items are actually needed)
- `ModBlockEntities` — block entity type registration
- `ModMenus` — menu type registration
- `ModEnchantments` — custom enchantment registration
- `ModRecipes` — recipe type and serializer registration
- `TransmutationTableBlock` — block interaction and menu opening
- `TransmutationTableBlockEntity` — inventory, processing state, persistence, and server tick
- `TransmutationTableMenu` — slot definitions, validation, and shift-click transfer behavior
- `TransmutationTableScreen` — client GUI and progress display
- `TransmutationRecipe` — input/catalyst matching and result construction
- `TransmutationRecipeSerializer` — JSON parsing and serialization
- `KeenEdgeEnchantment` — enchantment eligibility and level rules
- `PurlingEnchantment` — enchantment eligibility and level rules
- `PurlingNetwork` (or equivalent) — client-to-server activation packet
- `ModKeyMappings` — client keybind registration
- Small, focused event handlers for melee bonus and Purling teleport protection

Avoid creating a class for every tiny operation. Keep responsibilities clear and the implementation easy to extend.

### Recipe JSON

Place recipe files under `src/main/resources/data/relicweave/recipes/transmutation/` if the custom serializer's layout supports that folder. Confirm how Forge 1.20.1 discovers recipe JSON resources; the serializer must be registered under a stable recipe type ID such as `relicweave:transmutation`.

The recipe schema should minimally support:

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

Treat the example as a schema target, not permission to skip validation. Reject malformed JSON with a useful loading error. Build the output as a valid enchanted-book `ItemStack` and apply the requested registered enchantment and level. Recipe matching must verify ingredient counts as well as item identity/tags.

### Enchantments and Anvils

Register `keen_edge` and `purling` as normal Forge enchantments with appropriate rarity, maximum level, and item eligibility. Ensure they can appear on and function from enchanted books. Test anvil application and same-enchantment book combining in-game; do not claim support if the custom enchantment's compatibility rules prevent it.

### Networking and Cooldowns

Use Forge's supported networking API for the Purling activation packet. The packet handler must validate and perform the action on the server thread. Keep cooldown state server-owned and scoped per player; use a simple persisted player-data/capability approach only if required by restart/death semantics, otherwise a server-side cooldown tracker is acceptable for this first release. Avoid loading client-only classes on a dedicated server.

## 5. Implementation Phases

### Phase 1 — Verify Workspace and Build Baseline

1. Inspect the repository structure, Gradle files, Forge version, mappings, mod entry point, and existing registries.
2. Confirm Java 17 is configured and the baseline Gradle build succeeds.
3. Record any existing code conventions and adjust the class names in this plan only when necessary.
4. Add the `0.0.1` version and mod metadata consistently without overwriting unrelated metadata.

**Exit check:** the untouched or minimally adjusted project builds successfully.

### Phase 2 — Registries and Table Block

1. Register the Transmutation Table block and its block item.
2. Register its block entity type and implement placement, interaction, and save/load behavior.
3. Verify the block can be obtained in a test world, placed, broken, and picked up correctly.

**Exit check:** the table registers and behaves as a normal persistent block without client/server errors.

### Phase 3 — Inventory, Menu, and GUI

1. Implement the three-slot inventory and slot restrictions.
2. Implement the menu, item transfer behavior, and client screen.
3. Display input/catalyst/output slots and a processing progress indicator.
4. Ensure menu opening works on a dedicated server and shift-click cannot bypass slot rules.

**Exit check:** items can be inserted, displayed, saved, and removed safely; output insertion is blocked.

### Phase 4 — Recipe Type and Transmutation Processing

1. Register the custom recipe type and serializer.
2. Parse and validate recipe JSON using the defined schema.
3. Match input and catalyst ingredients, counts, processing duration, enchantment ID, and level.
4. Implement server-side processing, output capacity checks, atomic completion, and progress reset/pause behavior.
5. Add the two initial JSON recipes and test resource reloads.

**Exit check:** each initial recipe reliably consumes the correct ingredients and produces exactly one correctly enchanted book, with no duplication or loss in normal use.

### Phase 5 — Keen Edge

1. Register the enchantment and set eligible items to Swords and Axes.
2. Implement the per-level melee bonus using a verified Forge 1.20.1 event/hook.
3. Test held-item changes, non-melee damage, projectiles, multiple enchantment levels, and multiplayer server behavior.

**Exit check:** Keen Edge provides the intended bonus only to qualifying direct melee attacks.

### Phase 6 — Purling

1. Register the Boots enchantment and its eligibility rules.
2. Register the client keybind and activation packet.
3. Validate equipment, cooldown, and Ender Pearl inventory on the server.
4. Launch the pearl, consume inventory correctly, enforce the 600-tick cooldown, and scope impact-damage protection to Purling-triggered teleports.
5. Test missing pearls, cooldown activation, changing equipment, death/respawn, logout, and dedicated-server operation.

**Exit check:** Purling can be activated reliably and cannot be exploited by client-side packet spam.

### Phase 7 — Verification and Documentation

1. Run the Gradle build and fix compile errors before concluding.
2. Test in a client world and, where possible, a dedicated server with at least two players.
3. Verify recipe reloads, table persistence, menu restrictions, book contents, anvil behavior, cooldowns, and damage scoping.
4. Write README instructions for building, running, controls, recipe JSON, and known limitations.
5. Summarize files changed, tests run, build result, and any incomplete items.

**Exit check:** the project builds and the README accurately describes the tested `0.0.1` implementation.

## 6. Acceptance Checklist

- [ ] Mod loads on Minecraft 1.20.1 with the configured Forge version and Java 17.
- [ ] Transmutation Table block, block item, block entity, menu, and screen register correctly.
- [ ] Input and catalyst slots accept valid items; output only allows extraction.
- [ ] Inventory and progress persist across save/reload.
- [ ] Datapack recipe JSON loads, validates, and can be extended without Java edits.
- [ ] Diamond recipe produces a Keen Edge I book.
- [ ] Ender Pearl recipe produces a Purling I book.
- [ ] Keen Edge increases only qualifying direct melee damage.
- [ ] Purling keybind validates on the server, consumes a pearl only on success, and uses a 600-tick cooldown.
- [ ] Purling protection does not suppress unrelated damage.
- [ ] No known item duplication or ingredient-loss issue remains in ordinary table use.
- [ ] Gradle build succeeds and README documents the tested implementation.

## 7. Instructions for Claude Code

- Start by inspecting the actual repository and reading the existing source/configuration. Do not assume the project is empty.
- Implement one phase at a time and keep each change focused.
- Verify Forge 1.20.1 API signatures from the installed MDK/dependencies instead of guessing from other Minecraft versions.
- Do not use Fabric APIs or APIs from newer Minecraft/Forge versions.
- Keep gameplay mutations authoritative on the logical server; use packets only for client input that needs server action.
- Handle malformed data and invalid menu states safely. Avoid silent catches that hide failures.
- After each phase, run the most relevant Gradle task and fix regressions before moving on.
- Do not mark a checklist item complete unless it has been implemented and verified. If the environment prevents an in-game test, state that limitation explicitly.
- Finish with a concise report: implementation completed, key files changed, commands/tests run, build status, and any remaining limitations.
