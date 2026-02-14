# Draft: Block ID to 256 Color Mapping

## Research Findings
- **No existing color mapping**: Searched codebase for `MapColor`, `BlockColor`, etc. Found UI colors (Compose) and bitwise operations, but no Minecraft block-to-color logic.
- **AnvilRW**: The project has a custom Anvil reader (`anvilrw`), so we have access to raw chunk data (NBT).

## Technical: Block ID to Texture Chain
The relationship is **indirect**, not 1-to-1. You must resolve a chain of JSON files to find the texture.

**The Resolution Chain:**
1.  **Block ID** (`minecraft:grass_block`)
    *   Look up `assets/minecraft/blockstates/grass_block.json`
2.  **Blockstate** (Selects a model based on state, e.g., `snowy=false`)
    *   Points to Model: `minecraft:block/grass_block`
3.  **Model** (`assets/minecraft/models/block/grass_block.json`)
    *   Defines textures: `"top": "minecraft:block/grass_block_top"`, `"side": "minecraft:block/grass_block_side"`, etc.
    *   *Note: Models can inherit from parents! You may need to recursively load parent models.*
4.  **Texture**
    *   Final path: `assets/minecraft/textures/block/grass_block_top.png`

**Shortcut for Simple Blocks:**
For ~70% of blocks (like `stone`, `dirt`, `gold_block`), the texture name matches the block ID:
`minecraft:diamond_block` -> `textures/block/diamond_block.png`

## Decision: Static Color List
User selected **Static Color List** approach.
- **Requirement**: A simple JSON/CSV mapping `block_id -> color_int`.
- **Implementation Location**: `common` module (shared logic).

## Implementation Plan
1.  **Data Source**: Acquire a standard block-color JSON (Dynmap default or vanilla MapColors).
2.  **Resource File**: Place in `common/src/main/resources/data/block_colors.json`.
3.  **Loader**: Create `BlockColorRegistry` in `common`.
    - Load JSON on startup.
    - Provide `getColor(id: String): Int` function.
    - Fallback: Deterministic hash or default gray for unknown blocks.
