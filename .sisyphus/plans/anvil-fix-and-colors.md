# Anvil NBT Fix & Block Color Map

## TL;DR

> **Quick Summary**: Fixes a critical bug in `Chunk.kt` where NBT data wrapped in an empty string key `""` causes read failures, and implements a static Block ID to 256-color mapping feature.
> 
> **Deliverables**:
> - Fix in `Chunk.kt` (`unwrapRoot` logic)
> - New resource: `block_colors.json` (Minecraft Map Colors)
> - New loader: `BlockColorProvider`
> - Tests for both fix and feature
> 
> **Estimated Effort**: Small
> **Parallel Execution**: YES - 2 waves
> **Critical Path**: Chunk Fix → Chunk Test

---

## Context

### Original Request
1. Fix `Chunk.kt`: "keeps respond invalid chunk format missing xpos/zpos", root has `""` wrapper.
2. "convert minecraft block id to one of 256 colors" -> Static color list requested.

### Metis Review
**Identified Gaps** (addressed):
- **Missed Patch**: `loadNbtData` also needs the fix (not just `parseCoordinatesAndVersion`).
- **Data Write**: `setNbtData` will write unwrapped data (accepted behavior).
- **Missing Tests**: No tests exist in `common`; need to bootstrap `ChunkTest.kt`.
- **Mod Support**: Use fallback color (Purple `0xFF00FF`) for unknown blocks.

---

## Work Objectives

### Core Objective
Make chunk reading robust against NBT wrappers and provide a standard block color lookup service.

### Concrete Deliverables
- `common/src/main/kotlin/calebxzhou/rdi/common/anvilrw/core/Chunk.kt` (modified)
- `common/src/main/resources/assets/rdi/block_colors.json` (new)
- `common/src/main/kotlin/calebxzhou/rdi/common/color/BlockColorProvider.kt` (new)
- `common/src/test/kotlin/calebxzhou/rdi/common/anvilrw/core/ChunkTest.kt` (new)

### Definition of Done
- [ ] `ChunkTest` passes with a wrapped NBT payload
- [ ] `BlockColorProvider.getColor("minecraft:grass_block")` returns correct hex
- [ ] Fallback color works for unknown blocks

### Must Have
- Robust NBT unwrapping (handle `root[""]` vs direct `root`)
- Standard Minecraft Map Color palette

### Must NOT Have (Guardrails)
- Runtime texture extraction (too slow/complex)
- Dependency on heavy rendering libraries

---

## Verification Strategy

### Test Decision
- **Infrastructure exists**: NO (need to bootstrap JUnit/KotlinTest)
- **Automated tests**: YES (TDD style)
- **Framework**: `kotlin.test` / JUnit 5 (standard for Gradle Kotlin)

### Agent-Executed QA Scenarios (MANDATORY)

```
Scenario: Chunk unwraps empty-string root key
  Tool: Bash (gradle test)
  Preconditions: Common module compiles
  Steps:
    1. Create a mocked NBT payload with `{"": {"xPos": 10, "zPos": 20}}`
    2. Initialize Chunk with this payload
    3. Assert chunk.x == 10 and chunk.z == 20
    4. Assert chunk.getNbtData() returns the inner compound
  Expected Result: Test passes, no "missing xPos" exception
  Evidence: Test execution logs

Scenario: BlockColorProvider loads colors and handles missing
  Tool: Bash (gradle test or kotlin REPL)
  Preconditions: block_colors.json exists
  Steps:
    1. Call BlockColorProvider.getColor("minecraft:grass_block") -> Assert 0x7FB238
    2. Call BlockColorProvider.getColor("minecraft:unknown_thing") -> Assert 0xFF00FF
  Expected Result: Correct hex for known, fallback for unknown
  Evidence: Test execution logs
```

---

## Execution Strategy

### Parallel Execution Waves

```
Wave 1 (Start Immediately):
├── Task 1: Fix Chunk.kt (NBT Unwrap) + Test
└── Task 2: Create Block Color JSON + Provider

Wave 2 (Verification):
└── Task 3: Run all tests to confirm stability
```

---

## TODOs

- [ ] 1. Fix Chunk.kt NBT Unwrapping & Add Test

  **What to do**:
  - Create `unwrapRoot(root: NbtCompound): NbtCompound` helper in `Chunk.kt`.
    - Logic: `if (root.size == 1 && "" in root) return root[""] as NbtCompound else return root`
  - Update `parseCoordinatesAndVersion` to use `unwrapRoot`.
  - Update `loadNbtData` to use `unwrapRoot`.
  - Create `common/src/test/kotlin/calebxzhou/rdi/common/anvilrw/core/ChunkTest.kt`.
    - Test case: Load chunk with wrapped NBT -> Verify properties.

  **Recommended Agent Profile**:
  - **Category**: `quick`
  - **Skills**: [`git-master`] (for safe file creation)

  **References**:
  - `common/src/main/kotlin/calebxzhou/rdi/common/anvilrw/core/Chunk.kt` - Target file
  - `Chunk.kt:42` - `parseCoordinatesAndVersion` needs patch
  - `Chunk.kt:91` - `loadNbtData` needs patch

  **Acceptance Criteria**:
  - [ ] `unwrapRoot` handles both wrapped and unwrapped data
  - [ ] `loadNbtData` successfully loads wrapped NBT
  - [ ] `gradlew :common:test` passes

  **QA Scenario**:
  ```
  Scenario: Verify NBT Unwrapping Logic
    Tool: Bash (gradle)
    Preconditions: Mocked NBT data ready
    Steps:
      1. Run: ./gradlew :common:test --tests "*ChunkTest*"
    Expected Result: Tests pass
  ```

- [ ] 2. Implement Block Color System

  **What to do**:
  - Create `common/src/main/resources/assets/rdi/block_colors.json`.
    - Content: Map of `minecraft:id` -> `HexString` (e.g., "7FB238").
    - Include ~50 common blocks (Grass, Stone, Dirt, Water, Sand, Wood, Leaves, etc.).
  - Create `common/src/main/kotlin/calebxzhou/rdi/common/color/BlockColorProvider.kt`.
    - Singleton `object BlockColorProvider`.
    - `fun load()`: Read JSON from resource stream.
    - `fun getColor(id: String): Int`: Return parsed int or `0xFF00FF`.
    - Use `kotlinx.serialization` if available, or simple string parsing (std lib) to avoid deps.

  **Recommended Agent Profile**:
  - **Category**: `quick`
  - **Skills**: [`writing`] (for JSON data)

  **References**:
  - `common/src/main/resources/assets` - Resource root
  - Standard Minecraft Map Colors (Grass=0x7FB238, Water=0x4040FF, etc.)

  **Acceptance Criteria**:
  - [ ] `block_colors.json` exists with valid JSON
  - [ ] `BlockColorProvider` compiles and loads data
  - [ ] `getColor` returns correct values

  **QA Scenario**:
  ```
  Scenario: Verify Color Lookup
    Tool: Bash (gradle)
    Preconditions: block_colors.json populated
    Steps:
      1. Create temp test file calling BlockColorProvider
      2. Run test via gradle
    Expected Result: Correct colors returned
  ```

---

## Success Criteria

### Final Checklist
- [ ] `Chunk.kt` has `unwrapRoot` helper
- [ ] `loadNbtData` calls `unwrapRoot`
- [ ] `block_colors.json` contains standard palette
- [ ] `BlockColorProvider` is accessible in `common`
