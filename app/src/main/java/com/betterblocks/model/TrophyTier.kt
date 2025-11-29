package com.betterblocks.model

enum class TrophyTier {
    UNRANKED,
    BRONZE,
    SILVER,
    GOLD,
    PLATINUM,
    DIAMOND,
    GOD
}

fun getTrophyTierForScore(score: Int): TrophyTier {
    return when {
        score >= 1_000_000 -> TrophyTier.GOD
        score >= 300_000 -> TrophyTier.DIAMOND
        score >= 150_000 -> TrophyTier.PLATINUM
        score >= 50_000 -> TrophyTier.GOLD
        score >= 10_000 -> TrophyTier.SILVER
        score >= 1_000 -> TrophyTier.BRONZE
        else -> TrophyTier.UNRANKED
    }
}
