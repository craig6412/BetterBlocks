package com.betterblocks.engine

/**
 * High-level orchestration over [RulesEngine] — the single seam the game's UI /
 * view-model wires into. One call resolves a whole placement: place the block,
 * resolve line clears (with Living Board crystal-resist), advance the combo
 * streak, score it, and age the surviving board.
 *
 * Keeping the full turn in one pure, tested function means the eventual
 * GameViewModel integration is a thin, obviously-correct delegation rather than
 * a re-implementation of the rules.
 */
data class PlacementOutcome(
    /** Board after placement, clears, and Living Board aging. */
    val board: EngineGrid,
    val pointsScored: Int,
    /** Combo streak AFTER this placement (feed back in as `comboStreakBefore` next turn). */
    val comboStreak: Int,
    val rowsCleared: Set<Int>,
    val colsCleared: Set<Int>,
    val crystalsCleared: Int
) {
    val linesCleared: Int get() = rowsCleared.size + colsCleared.size
    val hadClear: Boolean get() = linesCleared > 0
}

object GameEngine {

    /**
     * Resolve a single placement end to end.
     *
     * @param absoluteCells the block's cells already translated to board coords.
     * @param moveNumber 1-based count of placements made this game (drives the
     *        Living Board grace period). Pass the move index of THIS placement.
     */
    fun resolvePlacement(
        board: EngineGrid,
        absoluteCells: List<Pair<Int, Int>>,
        colorId: Int,
        comboStreakBefore: Int,
        moveNumber: Int,
        scoring: ScoringConfig = GameTuning.SCORING,
        living: LivingBoardConfig = GameTuning.LIVING_BOARD
    ): PlacementOutcome {
        val placed = RulesEngine.place(board, absoluteCells, colorId)
        val clear = RulesEngine.resolveClears(placed, living)
        val streak = RulesEngine.nextComboStreak(comboStreakBefore, clear.linesCleared)
        val points = RulesEngine.scorePlacement(
            placedCellCount = absoluteCells.size,
            linesCleared = clear.linesCleared,
            comboStreak = streak,
            crystalsCleared = clear.crystalsCleared,
            config = scoring
        )
        // Age/crystallize the cells that survived the clear.
        val aged = RulesEngine.tickLivingBoard(clear.board, moveNumber, living)
        return PlacementOutcome(
            board = aged,
            pointsScored = points,
            comboStreak = streak,
            rowsCleared = clear.rowsCleared,
            colsCleared = clear.colsCleared,
            crystalsCleared = clear.crystalsCleared
        )
    }
}
