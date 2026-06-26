# Engine Integration & Phase 1 Hand-off

This documents the new `com.betterblocks.engine` package and exactly how to finish
the remaining Phase 1 work. The engine is pure and fully unit-tested; the items
below are the parts that touch Android UI/state and therefore must be built and
run on a machine with the Android SDK before they can be trusted.

## What already landed (tested, on this branch)

- `engine/RulesEngine.kt` — pure board ops, line clears, combo multiplier, Living
  Board (cell age → crystallize → one-clear resist).
- `engine/GameEngine.kt` — `resolvePlacement()` resolves a full turn in one call.
- `engine/BoardBridge.kt` — `Array<Array<Int?>>` ⇄ `EngineGrid` adapter.
- `engine/SeededBlockGenerator.kt` + `engine/DailyChallenge.kt` — deterministic
  seeds and shareable `BB-XXXX` codes.
- `engine/GameTuning.kt` — `SCORING` and `LIVING_BOARD` flags, both **OFF**.
- `GameViewModel` already scores via `RulesEngine.scorePlacement` (combo streak
  tracked in the private `comboStreak` var, reset in `resetGameTracking()`,
  surfaced as `GameUiState.comboStreak`), and fires haptics at placement/clear.

### Verify the engine
```
./gradlew :app:testDebugUnitTest
```
All `com.betterblocks.engine.*` and `EconomyConfigCharacterizationTest` tests must
stay green. (They were also verified standalone with the Kotlin 2.0 compiler: 37
passing.)

## Remaining Phase 1 work

### Step 2 (finish) — On-screen combo feedback + activate combos
1. ✅ Done: `ScorePopupRenderer` (previously never mounted, like HapticManager) is
   now mounted in the full-screen Box in `GameScreen.kt` and driven by
   `GameViewModel.showComboPopup(points, comboStreak)` on every clear — it shows
   "+points" and "COMBO ×N" (from the 2nd consecutive clear). Verify on device
   that the popup position/scale reads well; tune padding in `ScorePopupRenderer`
   if needed.
2. Activate the score multiplier by setting
   `GameTuning.SCORING.comboMultiplierEnabled = true`. Do this **after** wiring the
   Daily Challenge A/B harness (below) and passing the one-line-WOM exit gate.
   Until then, score is identical to legacy but the popup still gives feedback.

### Step 3b — Living Board board-state integration (the differentiator)
This is the one item that needs the board to carry per-cell age, so it is the
largest change. Recommended approach:
1. Change the live board from `Array<Array<Int?>>` to `EngineGrid`
   (`List<List<EngineCell?>>`) — or store a parallel age grid if a full type
   change is too invasive for one pass. `BoardBridge` exists to ease the
   transition incrementally (render via `toLegacy()` first, migrate callers after).
2. Replace the inline place/clear/score block in `GameViewModel.placeBlock` with a
   single `GameEngine.resolvePlacement(board, absoluteCells, colorId,
   comboStreak, moveNumber, living = GameTuning.LIVING_BOARD)` call and consume
   the returned `PlacementOutcome` (board, points, streak, crystalsCleared).
3. Render crystals in `AnimatedBoardRenderer` (the cell already supports
   `scale`/`alpha`); give crystal-clears a distinct VFX + `hapticBigClear()`.
4. Flip `GameTuning.LIVING_BOARD.enabled = true` only behind the A/B harness.

### Step 4 — Daily Challenge (supporting mode + A/B harness)
1. Add a "Daily Challenge" entry on `MainMenuScreen`.
2. Start a game whose blocks come from `DailyChallenge.generatorForDay(epochDay)`
   (use `LocalDate.now(ZoneOffset.UTC).toEpochDay()`), instead of the random
   `BlockManager` inventory.
3. Use this seeded mode as the crystallization A/B harness: enable
   `LIVING_BOARD` for daily runs first, compare retention/score distributions
   before enabling it in Endless.

### Step 6 — Revive-via-rewarded-ad
At game over (`GameViewModel` sets `isGameOver = true` around the game-over flow),
offer "Watch an ad to continue." `ads/AdManager.kt` already has the rewarded-ad
pipe (`REWARDED_*`); on reward, clear part of the board / grant one continue
instead of granting coins. This is the missing high-eCPM placement.

## Guardrails (from the QA gate — do not skip)
- Keep the **WOM exit gate** real: define the one-line-pitch pass/fail before
  building the Living Board feel; if it fails, fall back to "Resonance/Echo".
- Treat Phase 1 → 2 as a **data-driven go/no-go** on real retention/share numbers.
- Any change to economy/billing values must update
  `EconomyConfigCharacterizationTest` in the same commit — never silently.
