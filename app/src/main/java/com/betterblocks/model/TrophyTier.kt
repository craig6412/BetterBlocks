package com.betterblocks.model

enum class TrophyTier {
    UNRANKED,
    BRONZE,
    SILVER,
    GOLD,

    // Premium tiers – purchase only
    PLATINUM,
    DIAMOND,
    GOD
}

/**
 * Determines trophy tier based on lifetime coins and premium purchases.
 *
 * @param coins The player's total lifetime coins.
 * @param purchasedPremiumTiers A set of premium tiers the user has purchased.
 */
fun getTrophyTierForScore(
    coins: Int,
    purchasedPremiumTiers: Set<TrophyTier>
): TrophyTier {

    // Premium tiers override normal tiers
    when {
        TrophyTier.GOD in purchasedPremiumTiers -> return TrophyTier.GOD
        TrophyTier.DIAMOND in purchasedPremiumTiers -> return TrophyTier.DIAMOND
        TrophyTier.PLATINUM in purchasedPremiumTiers -> return TrophyTier.PLATINUM
    }

    // Free tiers unlocked by coins
    return when {
        coins >= 100_000 -> TrophyTier.GOLD
        coins >= 50_000 -> TrophyTier.SILVER
        coins >= 1_000 -> TrophyTier.BRONZE
        else -> TrophyTier.UNRANKED
    }
}
