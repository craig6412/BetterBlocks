package com.betterblocks.engine

/**
 * Central, one-flip activation switches for the new engine-driven mechanics.
 *
 * Both default to OFF so the live game's behavior is identical to today: with
 * [SCORING] combo-multiplier disabled, [RulesEngine.scorePlacement] reproduces
 * the legacy formula exactly (placedCells + linesCleared * 100), and with
 * [LIVING_BOARD] disabled the aging/crystal system is fully inert.
 *
 * These flags are flipped only after the Daily Challenge A/B harness and the
 * "one-line-WOM" exit gate validate the feel (see ROADMAP.md). Keeping the
 * activation to a single, well-tested boolean is what makes wiring the engine
 * into the GameViewModel a low-risk change.
 */
object GameTuning {

    /** Scoring rules. Flip `comboMultiplierEnabled` to true to activate combos. */
    val SCORING: ScoringConfig = ScoringConfig(
        comboMultiplierEnabled = false,
        comboStep = 0.5,
        maxComboMultiplier = 5.0,
        crystalClearBonus = 150
    )

    /** Living Board rules. Flip `enabled` to true to activate crystallization. */
    val LIVING_BOARD: LivingBoardConfig = LivingBoardConfig(
        enabled = false,
        crystallizeAge = 6,
        graceMoves = 4
    )
}
