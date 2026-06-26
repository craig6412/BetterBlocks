package com.betterblocks.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

/**
 * Locks the SAFETY GUARANTEE for wiring the engine into GameViewModel: with the
 * shipped default tuning, scoring must be byte-for-byte identical to the legacy
 * game and the Living Board must be inert. Activating either mechanic is then a
 * single, deliberate flag flip — not a silent behavior change.
 */
class GameTuningTest {

    @Test
    fun defaultScoring_isCombosOff_andReproducesLegacyFormula() {
        assertFalse(GameTuning.SCORING.comboMultiplierEnabled)
        // Mirror the exact call GameViewModel makes (crystalsCleared = 0), across
        // a no-clear placement, a single line, and a combined row+col clear.
        assertEquals(4, RulesEngine.scorePlacement(4, 0, 0, 0, GameTuning.SCORING))
        assertEquals(101, RulesEngine.scorePlacement(1, 1, 1, 0, GameTuning.SCORING))
        assertEquals(201, RulesEngine.scorePlacement(1, 2, 1, 0, GameTuning.SCORING))
        // Even a long streak must not change score while combos are disabled.
        assertEquals(101, RulesEngine.scorePlacement(1, 1, 9, 0, GameTuning.SCORING))
    }

    @Test
    fun defaultLivingBoard_isDisabledAndInert() {
        assertFalse(GameTuning.LIVING_BOARD.enabled)
        val board = List(ENGINE_GRID_SIZE) { r ->
            List<EngineCell?>(ENGINE_GRID_SIZE) { c -> if (r == 0 && c == 0) EngineCell(1, age = 99) else null }
        }
        val ticked = RulesEngine.tickLivingBoard(board, moveNumber = 100, GameTuning.LIVING_BOARD)
        assertEquals(99, ticked[0][0]!!.age) // no aging
        assertFalse(ticked[0][0]!!.crystallized) // no crystallization
    }
}
