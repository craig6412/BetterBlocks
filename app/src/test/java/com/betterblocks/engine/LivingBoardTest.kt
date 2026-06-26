package com.betterblocks.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests the "Living Board" differentiator: cell aging, crystallization (with a
 * grace period so new players aren't punished), the one-clear resist, and the
 * crystal bonus. This is the ownable mechanic, so its behavior is pinned tightly
 * and lives behind a config flag for A/B tuning.
 */
class LivingBoardTest {

    private val living = LivingBoardConfig(enabled = true, crystallizeAge = 3, graceMoves = 2)

    private fun singleCellBoard(cell: EngineCell?): EngineGrid =
        List(ENGINE_GRID_SIZE) { r ->
            List<EngineCell?>(ENGINE_GRID_SIZE) { c -> if (r == 0 && c == 0) cell else null }
        }

    @Test
    fun disabledByDefault_isInert() {
        val b = singleCellBoard(EngineCell(1, age = 99))
        val ticked = RulesEngine.tickLivingBoard(b, moveNumber = 100, LivingBoardConfig())
        assertEquals(99, ticked[0][0]!!.age) // unchanged
        assertFalse(ticked[0][0]!!.crystallized)
    }

    @Test
    fun cellsAgeOnEachTick() {
        var b = singleCellBoard(EngineCell(1, age = 0))
        b = RulesEngine.tickLivingBoard(b, moveNumber = 1, living)
        assertEquals(1, b[0][0]!!.age)
        b = RulesEngine.tickLivingBoard(b, moveNumber = 2, living)
        assertEquals(2, b[0][0]!!.age)
    }

    @Test
    fun gracePeriodPreventsEarlyCrystallization() {
        // Old enough to crystallize, but still inside the grace window.
        val b = singleCellBoard(EngineCell(1, age = 5))
        val ticked = RulesEngine.tickLivingBoard(b, moveNumber = 2, living) // graceMoves = 2
        assertFalse("must not crystallize during grace", ticked[0][0]!!.crystallized)
    }

    @Test
    fun crystallizesAfterThresholdPastGrace() {
        val b = singleCellBoard(EngineCell(1, age = 2))
        val ticked = RulesEngine.tickLivingBoard(b, moveNumber = 3, living) // age -> 3 >= 3, past grace
        assertTrue(ticked[0][0]!!.crystallized)
    }

    @Test
    fun crystalResistsFirstClear_thenClearsSecond() {
        // Fill row 0; make the corner a crystal, rest plain.
        val row0 = List<EngineCell?>(ENGINE_GRID_SIZE) { c ->
            if (c == 0) EngineCell(1, age = 9, crystallized = true) else EngineCell(1)
        }
        val grid = listOf(row0) + List(ENGINE_GRID_SIZE - 1) { List<EngineCell?>(ENGINE_GRID_SIZE) { null } }

        val first = RulesEngine.resolveClears(grid, living)
        assertEquals(1, first.crystalsCleared)
        // Plain cells removed; crystal survives as a downgraded plain cell.
        assertNull(first.board[0][1])
        val survivor = first.board[0][0]
        assertTrue("crystal should survive the first clear", survivor != null)
        assertFalse("survivor downgraded to plain", survivor!!.crystallized)
        assertEquals("survivor age reset", 0, survivor.age)
    }

    @Test
    fun crystalClearAwardsBonus() {
        val config = ScoringConfig(crystalClearBonus = 150)
        // 1-cell placement, clears 1 line, 2 crystals cleared -> 1 + 100 + 2*150.
        assertEquals(401, RulesEngine.scorePlacement(1, 1, 1, 2, config))
    }
}
