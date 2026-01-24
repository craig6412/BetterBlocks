package com.betterblocks.model

import android.content.SharedPreferences
import com.betterblocks.KEY_HIGHEST_TIER_UNLOCKED
import com.betterblocks.economy.EconomyConfig

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

/**
 * Determines the player's unlocked tier based on score and lifetime coins.
 *
 * RULES:
 * 1. Coins and score are treated as the same "overall progression value"
 * 2. Player gets placed into a tier when they reach the score threshold OR purchase with coins
 * 3. Platinum, Diamond, and Elite must be unlocked IN ORDER (no skipping)
 * 4. Elite tier requires score >= ELITE_SCORE OR ELITE coin purchase
 */
fun determineUnlockedTier(
    bestScore: Int,
    lifetimeCoins: Int,
    previouslyUnlocked: TrophyTier
): TrophyTier {
    var tier = previouslyUnlocked

    // Free tiers - unlock by score only
    if (tier == TrophyTier.UNRANKED && bestScore >= EconomyConfig.BRONZE_SCORE) tier = TrophyTier.BRONZE
    if (tier >= TrophyTier.BRONZE && bestScore >= EconomyConfig.SILVER_SCORE) tier = TrophyTier.SILVER
    if (tier >= TrophyTier.SILVER && bestScore >= EconomyConfig.GOLD_SCORE) tier = TrophyTier.GOLD

    // Premium tiers - must be unlocked in order, require score OR coin payment
    if (tier >= TrophyTier.GOLD && (bestScore >= EconomyConfig.PLATINUM_SCORE || lifetimeCoins >= EconomyConfig.PLATINUM_COINS)) {
        tier = TrophyTier.PLATINUM
    }

    if (tier >= TrophyTier.PLATINUM && (bestScore >= EconomyConfig.DIAMOND_SCORE || lifetimeCoins >= EconomyConfig.DIAMOND_COINS)) {
        tier = TrophyTier.DIAMOND
    }

    if (tier >= TrophyTier.DIAMOND && (bestScore >= EconomyConfig.ELITE_SCORE || lifetimeCoins >= EconomyConfig.ELITE_COINS)) {
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
