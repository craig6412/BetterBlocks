package com.betterblocks.engine

/**
 * Pure rules engine for BetterBlocks.
 *
 * Everything here is deterministic and Android-free so it can be exhaustively
 * unit-tested (the game handles money via score->coin thresholds, so the rules
 * that drive score must be tested). It is built ALONGSIDE the existing
 * [com.betterblocks.GameViewModel] god-object rather than replacing it in one
 * step — the strangler pattern. Call sites migrate to it incrementally.
 *
 * It also introduces the game's ownable differentiator, the "Living Board":
 * occupied cells age over time and eventually CRYSTALLIZE. A crystallized cell
 * is worth a big bonus when finally cleared, but it resists one clear (it
 * downgrades to a plain cell instead of vanishing), creating a "cash in now vs
 * grow it bigger and risk choking the board" decision no clone has.
 */

const val ENGINE_GRID_SIZE = 9

/**
 * A single occupied cell. `null` in the board means empty.
 *
 * @property colorId rendering/color identifier (opaque to the engine).
 * @property age number of placements survived since this cell was placed.
 * @property crystallized whether the cell has aged into a crystal.
 */
data class EngineCell(
    val colorId: Int,
    val age: Int = 0,
    val crystallized: Boolean = false
)

/** Immutable 9x9 board. Rows of nullable cells; `null` == empty. */
typealias EngineGrid = List<List<EngineCell?>>

/** Tuning for the Living Board differentiator. Exposed so it can be A/B'd. */
data class LivingBoardConfig(
    val enabled: Boolean = false,
    /** Cells reach this age (placements survived) to crystallize. */
    val crystallizeAge: Int = 6,
    /**
     * Grace period: no cell crystallizes during the first N placements of a
     * game, so new players are never punished before they understand the board.
     */
    val graceMoves: Int = 4
)

/** Tuning for scoring. Defaults reproduce the legacy flat formula exactly. */
data class ScoringConfig(
    val pointsPerPlacedCell: Int = 1,
    val pointsPerLine: Int = 100,
    /** When false, scoring is identical to the legacy game (no multiplier). */
    val comboMultiplierEnabled: Boolean = false,
    /** Each additional consecutive clearing placement adds this to the multiplier. */
    val comboStep: Double = 0.5,
    val maxComboMultiplier: Double = 5.0,
    /** Bonus points awarded per crystallized cell that is cleared. */
    val crystalClearBonus: Int = 150
)

/** Result of resolving a placement that may have cleared lines. */
data class ClearResult(
    val board: EngineGrid,
    val rowsCleared: Set<Int>,
    val colsCleared: Set<Int>,
    /** Cells that were fully removed this clear. */
    val clearedCells: Set<Pair<Int, Int>>,
    /** Crystallized cells that were cleared (drive the crystal bonus). */
    val crystalsCleared: Int
) {
    val linesCleared: Int get() = rowsCleared.size + colsCleared.size
    val hadClear: Boolean get() = linesCleared > 0
}

object RulesEngine {

    fun emptyBoard(size: Int = ENGINE_GRID_SIZE): EngineGrid =
        List(size) { List<EngineCell?>(size) { null } }

    /** True if [shape] (already translated to absolute cells) fits on the board. */
    fun canPlace(board: EngineGrid, absoluteCells: List<Pair<Int, Int>>): Boolean {
        val size = board.size
        return absoluteCells.all { (r, c) ->
            r in 0 until size && c in 0 until size && board[r][c] == null
        }
    }

    /**
     * Place a block (given as absolute cells) of [colorId] onto the board.
     * Newly placed cells start at age 0. Does NOT resolve clears — call
     * [resolveClears] next. Throws if the placement is illegal.
     */
    fun place(board: EngineGrid, absoluteCells: List<Pair<Int, Int>>, colorId: Int): EngineGrid {
        require(canPlace(board, absoluteCells)) { "Illegal placement at $absoluteCells" }
        val occupied = absoluteCells.toHashSet()
        return board.mapIndexed { r, row ->
            row.mapIndexed { c, cell ->
                if ((r to c) in occupied) EngineCell(colorId = colorId, age = 0) else cell
            }
        }
    }

    fun fullRows(board: EngineGrid): Set<Int> =
        board.indices.filterTo(HashSet()) { r -> board[r].all { it != null } }

    fun fullCols(board: EngineGrid): Set<Int> {
        val size = board.size
        return (0 until size).filterTo(HashSet()) { c -> board.all { row -> row[c] != null } }
    }

    /**
     * Resolve all full rows/cols. Living Board semantics: a crystallized cell in
     * a clearing line RESISTS the first clear — it is not removed but downgrades
     * to a plain cell (age reset) and yields the crystal bonus. Plain cells in a
     * clearing line are removed normally.
     */
    fun resolveClears(board: EngineGrid, living: LivingBoardConfig = LivingBoardConfig()): ClearResult {
        val rows = fullRows(board)
        val cols = fullCols(board)
        if (rows.isEmpty() && cols.isEmpty()) {
            return ClearResult(board, emptySet(), emptySet(), emptySet(), 0)
        }

        val inClearingLine: (Int, Int) -> Boolean = { r, c -> r in rows || c in cols }
        val clearedCells = HashSet<Pair<Int, Int>>()
        var crystalsCleared = 0

        val newBoard = board.mapIndexed { r, row ->
            row.mapIndexed { c, cell ->
                if (cell != null && inClearingLine(r, c)) {
                    if (living.enabled && cell.crystallized) {
                        // Resist: downgrade crystal to a fresh plain cell and pay the bonus.
                        crystalsCleared++
                        cell.copy(crystallized = false, age = 0)
                    } else {
                        clearedCells.add(r to c)
                        null
                    }
                } else {
                    cell
                }
            }
        }
        return ClearResult(newBoard, rows, cols, clearedCells, crystalsCleared)
    }

    /**
     * Living Board tick: run after each placement+clear. Ages every occupied cell
     * by one and crystallizes those at/over the threshold (after the grace
     * period). No-op when the Living Board is disabled.
     *
     * @param moveNumber 1-based count of placements made this game.
     */
    fun tickLivingBoard(board: EngineGrid, moveNumber: Int, living: LivingBoardConfig): EngineGrid {
        if (!living.enabled) return board
        val pastGrace = moveNumber > living.graceMoves
        return board.map { row ->
            row.map { cell ->
                if (cell == null) {
                    null
                } else {
                    val agedCell = cell.copy(age = cell.age + 1)
                    if (pastGrace && !agedCell.crystallized && agedCell.age >= living.crystallizeAge) {
                        agedCell.copy(crystallized = true)
                    } else {
                        agedCell
                    }
                }
            }
        }
    }

    /**
     * Combo streak transition: a placement that clears at least one line extends
     * the streak; a placement that clears nothing breaks it.
     */
    fun nextComboStreak(current: Int, linesClearedThisPlacement: Int): Int =
        if (linesClearedThisPlacement > 0) current + 1 else 0

    /**
     * The active multiplier for a given streak. The first clearing placement of a
     * streak (streak == 1) is 1.0x; each subsequent consecutive clear adds
     * [ScoringConfig.comboStep], capped at [ScoringConfig.maxComboMultiplier].
     */
    fun comboMultiplier(streak: Int, config: ScoringConfig): Double {
        if (!config.comboMultiplierEnabled || streak <= 1) return 1.0
        val raw = 1.0 + (streak - 1) * config.comboStep
        return minOf(raw, config.maxComboMultiplier)
    }

    /**
     * Score a single placement.
     *
     * With the default [ScoringConfig] (combo disabled, no crystals) this exactly
     * reproduces the legacy formula: `placedCells + linesCleared * 100`.
     *
     * @param comboStreak the streak value AFTER this placement (see [nextComboStreak]).
     */
    fun scorePlacement(
        placedCellCount: Int,
        linesCleared: Int,
        comboStreak: Int,
        crystalsCleared: Int,
        config: ScoringConfig = ScoringConfig()
    ): Int {
        var points = placedCellCount * config.pointsPerPlacedCell
        if (linesCleared > 0) {
            val linePoints = linesCleared * config.pointsPerLine
            val multiplier = comboMultiplier(comboStreak, config)
            points += (linePoints * multiplier).toInt()
            points += crystalsCleared * config.crystalClearBonus
        }
        return points
    }
}
