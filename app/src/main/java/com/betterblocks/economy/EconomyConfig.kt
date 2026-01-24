package com.betterblocks.economy

/**
 * Single source of truth for all in-game economy tuning.
 *
 * IMPORTANT:
 * - Do NOT change Google Play Console product IDs or pricing here.
 * - This file only controls in-game values: score thresholds, coin grants, and coin costs.
 */
object EconomyConfig {

    // ---------------------------------------------------------------------
    // Trophy tier score thresholds (AUTHORITATIVE)
    // ---------------------------------------------------------------------
    const val BRONZE_SCORE = 2_000
    const val SILVER_SCORE = 50_000
    const val GOLD_SCORE = 150_000
    const val PLATINUM_SCORE = 350_000
    const val DIAMOND_SCORE = 750_000
    const val ELITE_SCORE = 1_500_000

    // ---------------------------------------------------------------------
    // Trophy tier unlock costs (AUTHORITATIVE)
    // ---------------------------------------------------------------------
    const val PLATINUM_COINS = 75_000
    const val DIAMOND_COINS = 200_000
    const val ELITE_COINS = 500_000

    // ---------------------------------------------------------------------
    // Google Play Billing coin pack grants (IMPORTANT)
    // Product IDs must match Play Console exactly.
    // ---------------------------------------------------------------------
    val COIN_PACK_GRANTS: Map<String, Int> = mapOf(
        "coins_small" to 2_500,
        "coins_medium" to 12_000,
        "coins_large" to 40_000,
        "coins_mega" to 90_000
    )

    // ---------------------------------------------------------------------
    // Power-up costs (AUTHORITATIVE)
    // ---------------------------------------------------------------------
    const val COLOR_WIPE_COST = 100
    const val RAINBOW_WIPE_COST = 1_250

    // ---------------------------------------------------------------------
    // Other in-game coin costs
    // ---------------------------------------------------------------------
    const val ROTATION_COST = 10
}
