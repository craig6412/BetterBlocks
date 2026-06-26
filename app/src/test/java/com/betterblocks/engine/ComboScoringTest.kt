package com.betterblocks.engine

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Tests the new combo-multiplier scoring — the core dopamine loop the game was
 * missing (previously combos only filled the special meter, never the score).
 */
class ComboScoringTest {

    private val comboOn = ScoringConfig(comboMultiplierEnabled = true, comboStep = 0.5, maxComboMultiplier = 5.0)

    @Test
    fun streakAdvancesOnlyWhenLinesClear() {
        var streak = 0
        streak = RulesEngine.nextComboStreak(streak, 1); assertEquals(1, streak)
        streak = RulesEngine.nextComboStreak(streak, 2); assertEquals(2, streak)
        streak = RulesEngine.nextComboStreak(streak, 0); assertEquals(0, streak) // broken
        streak = RulesEngine.nextComboStreak(streak, 1); assertEquals(1, streak)
    }

    @Test
    fun firstClearOfStreak_isOneX() {
        assertEquals(1.0, RulesEngine.comboMultiplier(1, comboOn), 0.0001)
    }

    @Test
    fun multiplierGrowsPerConsecutiveClear() {
        assertEquals(1.5, RulesEngine.comboMultiplier(2, comboOn), 0.0001)
        assertEquals(2.0, RulesEngine.comboMultiplier(3, comboOn), 0.0001)
        assertEquals(3.0, RulesEngine.comboMultiplier(5, comboOn), 0.0001)
    }

    @Test
    fun multiplierIsCapped() {
        assertEquals(5.0, RulesEngine.comboMultiplier(50, comboOn), 0.0001)
    }

    @Test
    fun comboMultiplierAppliesToScore() {
        // 1-cell block, clears 1 line, streak 3 -> 2.0x on the 100 line points.
        // 1 (cell) + (100 * 2.0) = 201.
        assertEquals(201, RulesEngine.scorePlacement(1, 1, 3, 0, comboOn))
    }
}
