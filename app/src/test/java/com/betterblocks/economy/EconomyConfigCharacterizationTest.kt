package com.betterblocks.economy

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * CHARACTERIZATION tests for the economy.
 *
 * These pin the EXACT current money-affecting values so that the planned
 * refactor of the GameViewModel god-object cannot silently change real-money
 * grants, power-up costs, or trophy thresholds. If a value legitimately
 * changes, that is a deliberate product decision and this test should be
 * updated in the same commit — never quietly.
 */
class EconomyConfigCharacterizationTest {

    @Test
    fun trophyThresholds_areLocked() {
        assertEquals(2_000, EconomyConfig.BRONZE_SCORE)
        assertEquals(50_000, EconomyConfig.SILVER_SCORE)
        assertEquals(150_000, EconomyConfig.GOLD_SCORE)
        assertEquals(350_000, EconomyConfig.PLATINUM_SCORE)
        assertEquals(750_000, EconomyConfig.DIAMOND_SCORE)
        assertEquals(1_500_000, EconomyConfig.ELITE_SCORE)
    }

    @Test
    fun trophyUnlockCosts_areLocked() {
        assertEquals(75_000, EconomyConfig.PLATINUM_COINS)
        assertEquals(200_000, EconomyConfig.DIAMOND_COINS)
        assertEquals(500_000, EconomyConfig.ELITE_COINS)
    }

    @Test
    fun billingCoinPackGrants_areLocked() {
        // Product IDs MUST match Play Console exactly; grants must not drift.
        assertEquals(4, EconomyConfig.COIN_PACK_GRANTS.size)
        assertEquals(2_500, EconomyConfig.COIN_PACK_GRANTS["coins_small"])
        assertEquals(12_000, EconomyConfig.COIN_PACK_GRANTS["coins_medium"])
        assertEquals(40_000, EconomyConfig.COIN_PACK_GRANTS["coins_large"])
        assertEquals(90_000, EconomyConfig.COIN_PACK_GRANTS["coins_mega"])
    }

    @Test
    fun powerUpAndRotationCosts_areLocked() {
        assertEquals(100, EconomyConfig.COLOR_WIPE_COST)
        assertEquals(1_250, EconomyConfig.RAINBOW_WIPE_COST)
        assertEquals(10, EconomyConfig.ROTATION_COST)
    }
}
