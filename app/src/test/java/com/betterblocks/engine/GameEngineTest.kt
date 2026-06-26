package com.betterblocks.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GameEngineTest {

    private val color = 7

    @Test
    fun placementWithoutClear_scoresPlacedCells_andAddsToBoard() {
        val board = RulesEngine.emptyBoard()
        val outcome = GameEngine.resolvePlacement(
            board = board,
            absoluteCells = listOf(0 to 0, 0 to 1, 0 to 2),
            colorId = color,
            comboStreakBefore = 0,
            moveNumber = 1
        )
        assertFalse(outcome.hadClear)
        assertEquals(3, outcome.pointsScored) // default tuning: 3 placed cells, no lines
        assertEquals(0, outcome.comboStreak)
        assertTrue(outcome.board[0][0] != null)
    }

    @Test
    fun placementCompletingRow_clearsAndScoresLegacy() {
        // Pre-fill row 0 except the last cell.
        val row0 = List<EngineCell?>(ENGINE_GRID_SIZE) { c -> if (c == 8) null else EngineCell(color) }
        val board = listOf(row0) + List(ENGINE_GRID_SIZE - 1) { List<EngineCell?>(ENGINE_GRID_SIZE) { null } }

        val outcome = GameEngine.resolvePlacement(
            board = board,
            absoluteCells = listOf(0 to 8),
            colorId = color,
            comboStreakBefore = 0,
            moveNumber = 5
        )
        assertTrue(outcome.hadClear)
        assertEquals(setOf(0), outcome.rowsCleared)
        // Default tuning (combo off): 1 placed cell + 1 line * 100 = 101.
        assertEquals(101, outcome.pointsScored)
        assertEquals(1, outcome.comboStreak)
        assertTrue("cleared row is empty", outcome.board[0].all { it == null })
    }

    @Test
    fun comboStreakThreads_acrossPlacements() {
        var streak = 0
        // Two clears in a row should advance the streak to 2, then a no-clear resets it.
        val rowA = List<EngineCell?>(ENGINE_GRID_SIZE) { c -> if (c == 8) null else EngineCell(color) }
        val boardA = listOf(rowA) + List(ENGINE_GRID_SIZE - 1) { List<EngineCell?>(ENGINE_GRID_SIZE) { null } }
        streak = GameEngine.resolvePlacement(boardA, listOf(0 to 8), color, streak, 1).comboStreak
        assertEquals(1, streak)

        val rowB = List<EngineCell?>(ENGINE_GRID_SIZE) { c -> if (c == 8) null else EngineCell(color) }
        val boardB = listOf(rowB) + List(ENGINE_GRID_SIZE - 1) { List<EngineCell?>(ENGINE_GRID_SIZE) { null } }
        streak = GameEngine.resolvePlacement(boardB, listOf(0 to 8), color, streak, 2).comboStreak
        assertEquals(2, streak)

        // A placement that clears nothing breaks the streak.
        streak = GameEngine.resolvePlacement(RulesEngine.emptyBoard(), listOf(4 to 4), color, streak, 3).comboStreak
        assertEquals(0, streak)
    }

    @Test
    fun livingBoardEnabled_agesSurvivorsAfterPlacement() {
        val living = LivingBoardConfig(enabled = true, crystallizeAge = 3, graceMoves = 0)
        // Place a single cell on an empty board; it should age to 1 after its turn.
        val outcome = GameEngine.resolvePlacement(
            board = RulesEngine.emptyBoard(),
            absoluteCells = listOf(2 to 2),
            colorId = color,
            comboStreakBefore = 0,
            moveNumber = 1,
            living = living
        )
        assertEquals(1, outcome.board[2][2]!!.age)
    }
}
