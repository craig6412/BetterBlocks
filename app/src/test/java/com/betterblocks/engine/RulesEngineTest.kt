package com.betterblocks.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RulesEngineTest {

    private val color = 1

    /** Build a board from a string map: '#' = filled (age 0), '.' = empty. */
    private fun board(vararg rows: String): EngineGrid {
        require(rows.size == ENGINE_GRID_SIZE) { "need $ENGINE_GRID_SIZE rows" }
        return rows.map { row ->
            require(row.length == ENGINE_GRID_SIZE) { "row must be $ENGINE_GRID_SIZE wide: '$row'" }
            row.map { ch -> if (ch == '#') EngineCell(color) else null }
        }
    }

    private val empty = RulesEngine.emptyBoard()

    @Test
    fun place_marksCellsAndRejectsOverlap() {
        val cells = listOf(0 to 0, 0 to 1)
        val b = RulesEngine.place(empty, cells, color)
        assertTrue(b[0][0] != null && b[0][1] != null)
        assertEquals(0, b[0][0]!!.age)
        assertFalse(RulesEngine.canPlace(b, listOf(0 to 1))) // occupied
        assertFalse(RulesEngine.canPlace(b, listOf(0 to 9))) // out of bounds
    }

    @Test
    fun fullRow_isDetectedAndCleared() {
        val almostFull = board(
            "########.",
            ".........",
            ".........",
            ".........",
            ".........",
            ".........",
            ".........",
            ".........",
            "........."
        )
        val placed = RulesEngine.place(almostFull, listOf(0 to 8), color)
        val result = RulesEngine.resolveClears(placed)
        assertEquals(setOf(0), result.rowsCleared)
        assertTrue(result.colsCleared.isEmpty())
        assertEquals(9, result.clearedCells.size)
        assertTrue(result.board[0].all { it == null })
    }

    @Test
    fun simultaneousRowAndColumn_clearAsTwoLines() {
        // Fill row 0 entirely and column 0 entirely except their shared corner,
        // then place the corner to complete both at once.
        val rows = Array(ENGINE_GRID_SIZE) { r ->
            CharArray(ENGINE_GRID_SIZE) { c ->
                if ((r == 0 || c == 0) && !(r == 0 && c == 0)) '#' else '.'
            }.concatToString()
        }
        val b = board(*rows)
        val placed = RulesEngine.place(b, listOf(0 to 0), color)
        val result = RulesEngine.resolveClears(placed)
        assertEquals(setOf(0), result.rowsCleared)
        assertEquals(setOf(0), result.colsCleared)
        assertEquals(2, result.linesCleared)
    }

    /**
     * CHARACTERIZATION: with default config the engine must reproduce the legacy
     * formula exactly -> placedCells + linesCleared * 100, combos add nothing to
     * score. This locks current behavior before the combo multiplier is enabled.
     */
    @Test
    fun legacyScoring_isReproduced() {
        // 4-cell block, no clear -> 4 points.
        assertEquals(4, RulesEngine.scorePlacement(4, 0, 0, 0))
        // 1-cell block completing 1 line -> 1 + 100.
        assertEquals(101, RulesEngine.scorePlacement(1, 1, 1, 0))
        // 1-cell block completing a row AND column -> 1 + 200, NO combo bonus.
        assertEquals(201, RulesEngine.scorePlacement(1, 2, 1, 0))
        // A long streak must not change legacy score (multiplier disabled).
        assertEquals(101, RulesEngine.scorePlacement(1, 1, 9, 0))
    }
}
