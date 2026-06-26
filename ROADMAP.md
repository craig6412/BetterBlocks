# BetterBlocks — Product Roadmap

> Outcome of a four-agent design review (Senior Game Dev · Marketing/Psychology ·
> Independent QA/Success Analyst · Executive Producer). The combined plan was
> scored against a 75% success-probability gate. The first flat plan scored 58%
> and was rejected; the revised, phased plan below scored **76% — approved**.

## The bet: an ownable identity, not another clone

The genre (Block Blast, Woodoku, 1010!) is saturated with polished clones. Polish
is necessary but not sufficient. BetterBlocks needs **one ownable differentiator**,
shipped on a foundation that can hold a live-ops game.

- **Living Board (the mechanic / identity).** Occupied cells age; left uncleared
  too long they **crystallize**. A crystal is worth a big bonus when finally
  cleared, but it **resists one clear** (downgrades to a plain cell instead of
  vanishing). This creates a "cash in now vs. grow it bigger and risk choking the
  board" decision no competitor has, and crystal-clears become the new "perfect
  clear" that supercharges the combo multiplier and the juice.
- **Gauntlet (the distribution / moat, Phase 2).** Share the exact seeded board of
  a great run as a replayable challenge to a specific friend. The durable moat is
  the accumulating library of player-authored boards plus the social graph —
  which a competitor cannot clone by copying the feature.

## Guardrails (from the QA gate)

1. **Tests before the money refactor.** The `GameViewModel` god-object (~1,574 LOC)
   is refactored via the strangler pattern, never big-bang. Characterization tests
   pin the economy first.
2. **One-line-WOM exit gate** right after the Living Board spike: if it can't be
   pitched in a single sentence and a 5-second clip, fall back to the cheaper
   "Resonance/Echo lines" mechanic before sinking more effort in.
3. **Identity comes with cuts**, not on top — Phase 1 is hard-capped at 6 items.

## Phase 1 — CRITICAL (foundation + fastest ROI)

Build order (critical path 1→4; 5–6 + zero-dev ASO copy run in parallel):

1. **Seedable rules engine slice + economy/billing characterization tests.** ✅ *Done — see below.*
2. **Combo / chain score multiplier + on-screen feedback.** ✅ *Scoring path delegated to the
   tested engine; combo multiplier wired into `GameViewModel`, shipped OFF behind a flag so live
   score is unchanged. On-screen "+points / COMBO ×N" feedback now wired too — the previously dead
   `ScorePopupRenderer` is mounted and driven by the combo streak. Flip the flag to activate the
   score multiplier after A/B.*
3. **Living Board rules + tests, behind a flag.** ✅ *Rules + full lifecycle tests done at the engine
   layer (inert by default). Board-state integration awaits the cell-representation change.* *WOM exit gate here.*
4. **Daily Challenge** as a supporting seeded mode + the crystallization A/B harness.
5. **Wire up `HapticManager`** (already written, was zero call sites). ✅ *Done — short buzz on
   every placement, medium on a single-line clear, heavy on a multi-line combo clear; respects
   the user's haptic preference.*
6. **Revive-via-rewarded-ad at game over** (the missing high-eCPM placement; the
   ad pipe already exists).

## Phase 2 — HIGH IMPACT

Gauntlet (UGC + network moat, on the existing Firestore/seed plumbing) · share
card + "challenge a friend" deep link · economy re-tune + real persistence repo ·
cosmetics (themes / skins / clear VFX, incl. crystal VFX) · Remove-Ads / Plus IAP ·
re-shoot store screenshots + preview video of the living board · notification /
streak fixes (reminders on by default) · cheap animation hot-path fixes.

## Phase 3 — NICE TO HAVE

Daily/weekly quests + streak-freeze · 28-day battle pass (free + premium) ·
strangler refactor of the remaining god-object · full animation-pipeline rewrite
if still janky on low-end · broader test coverage.

## Residual risks to watch during execution

1. **Crystallization tuning is the whole ballgame** — "shard on failed clear" must
   not read as a punishing tax; the Phase 1 A/B harness has to find the fun.
2. **Keep the WOM exit gate a real gate** — define the pass/fail one-line criterion
   before building, or it's theater.
3. **Virality is unproven** — treat Phase 1 → 2 as a data-driven go/no-go.
4. **Revenue is deferred by design** — watch runway across Phase 1 → 2.
5. **The mechanic alone is copyable** — the durable moat is the Gauntlet UGC layer;
   don't let Phase 2 slip.

---

## Phase 1, Step 1 — DELIVERED

A pure, Android-free, fully unit-tested rules engine now lives in
`app/src/main/java/com/betterblocks/engine/`, built **alongside** the existing
`GameViewModel` (strangler pattern — the live app is untouched). Call sites will
migrate onto it incrementally.

| File | Purpose |
|---|---|
| `engine/EngineShapes.kt` | Canonical, Android-free block geometry (mirrors `BlockDefinitions.kt`). |
| `engine/SeededBlockGenerator.kt` | Deterministic block sequences — the foundation for Daily Challenge & Gauntlet. |
| `engine/RulesEngine.kt` | Pure board ops, line detection/clears, **combo multiplier**, and the **Living Board** cell-age / crystallize / resist model. |
| `engine/GameTuning.kt` | Central one-flip activation flags (combo multiplier, Living Board) — both ship OFF. |
| `engine/GameEngine.kt` | `resolvePlacement()` — the one-call orchestration seam the UI wires into (place → clear → combo → score → age). |
| `engine/BoardBridge.kt` | Adapter between the legacy `Array<Array<Int?>>` board and `EngineGrid` (strangler hinge). |
| `engine/DailyChallenge.kt` | Daily seed + shareable challenge code (the Gauntlet substrate). |

`GameViewModel` now scores via the engine (`RulesEngine.scorePlacement`) with a runtime combo
streak; with combos disabled this is byte-for-byte identical to the legacy formula.

Tests (run as fast JVM unit tests, **23 passing**):

| File | Covers |
|---|---|
| `economy/EconomyConfigCharacterizationTest.kt` | Locks every real-money grant, power-up cost, and trophy threshold before any refactor. |
| `engine/SeededBlockGeneratorTest.kt` | Determinism: same seed → identical sequence; per-day seeding. |
| `engine/RulesEngineTest.kt` | Placement, line detection/clears, and a characterization test pinning the **legacy** scoring formula. |
| `engine/ComboScoringTest.kt` | The new combo multiplier (growth, cap, application to score). |
| `engine/LivingBoardTest.kt` | Cell aging, grace period, crystallization threshold, one-clear resist, crystal bonus. |

Scoring is backward-compatible: with the default `ScoringConfig` the engine
reproduces the current game's score exactly (`placedCells + linesCleared * 100`);
the combo multiplier and Living Board are opt-in via config flags so they can be
A/B-tuned before they ship to everyone.
