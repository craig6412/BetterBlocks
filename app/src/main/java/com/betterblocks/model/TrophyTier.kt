package com.betterblocks.model

import android.content.SharedPreferences
import com.betterblocks.KEY_HIGHEST_TIER_UNLOCKED

enum class TrophyTier {
    UNRANKED,
    BRONZE,
    SILVER,
    GOLD,
    PLATINUM,
    DIAMOND,
    ELITE;

    companion object {
        fun fromOrdinalSafe(ordinal: Int): TrophyTier = values().getOrNull(ordinal) ?: UNRANKED
    }
}

// Trophy Tier Score Thresholds
const val BRONZE_SCORE = 1_000
const val SILVER_SCORE = 50_000
const val GOLD_SCORE = 100_000
const val PLATINUM_SCORE = 250_000
const val DIAMOND_SCORE = 500_000

//first person to reach on a natural level was Nana.
// probably should consider bringing it up some.
const val ELITE_SCORE = 1_000_000

// Premium Tier Unlock Costs (in coins)
const val PLATINUM_COINS = 15_625
const val DIAMOND_COINS = 62_500
const val ELITE_COINS = 250_000

// fuck those that didn't think we would get here today... we made it my ninja. 


/**
 * Determines the player's unlocked tier based on score and lifetime coins.
 *
 * RULES:
 * 1. Coins and score are treated as the same "overall progression value"
 * 2. Player gets placed into a tier when they reach the score threshold OR purchase with coins
 * 3. Platinum, Diamond, and Elite must be unlocked IN ORDER (no skipping)
 * 4. Elite tier requires BOTH score >= 1,000,000 AND 250,000 coin purchase (OR just the purchase)
 */
fun determineUnlockedTier(
    bestScore: Int,
    lifetimeCoins: Int,
    previouslyUnlocked: TrophyTier
): TrophyTier {
    var tier = previouslyUnlocked

    // Free tiers - unlock by score only
    if (tier == TrophyTier.UNRANKED && bestScore >= BRONZE_SCORE) tier = TrophyTier.BRONZE
    if (tier >= TrophyTier.BRONZE && bestScore >= SILVER_SCORE) tier = TrophyTier.SILVER
    if (tier >= TrophyTier.SILVER && bestScore >= GOLD_SCORE) tier = TrophyTier.GOLD

    // Premium tiers - must be unlocked in order, require score OR coin payment
    // PLATINUM: Must have GOLD first, then reach score or pay coins
    if (tier >= TrophyTier.GOLD && (bestScore >= PLATINUM_SCORE || lifetimeCoins >= PLATINUM_COINS)) {
        tier = TrophyTier.PLATINUM
    }

    // DIAMOND: Must have PLATINUM first, then reach score or pay coins
    if (tier >= TrophyTier.PLATINUM && (bestScore >= DIAMOND_SCORE || lifetimeCoins >= DIAMOND_COINS)) {
        tier = TrophyTier.DIAMOND
    }

    // Elite: Must have DIAMOND first, then reach score or pay coins
    if (tier >= TrophyTier.DIAMOND && (bestScore >= ELITE_SCORE || lifetimeCoins >= ELITE_COINS)) {
        tier = TrophyTier.ELITE
    }

    return tier
}

fun getPlayerTier(bestScore: Int, coins: Int, prefs: SharedPreferences): TrophyTier {
    val previousOrdinal = prefs.getInt(KEY_HIGHEST_TIER_UNLOCKED, TrophyTier.UNRANKED.ordinal)
    val previouslyUnlocked = TrophyTier.fromOrdinalSafe(previousOrdinal)
    val newTier = determineUnlockedTier(bestScore, coins, previouslyUnlocked)
    // NOTE: This function is pure now and does NOT persist tier changes. Callers
    // that need to persist an unlocked tier should call ShopRepository.unlockTier(newTier.ordinal).
    return newTier
}
