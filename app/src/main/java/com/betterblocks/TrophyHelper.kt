package com.betterblocks

import com.betterblocks.economy.EconomyConfig
import com.betterblocks.model.TrophyTier
import com.betterblocks.model.drawableRes
import java.text.NumberFormat
import java.util.Locale

// Thin helper to avoid extension-import resolution issues across packages
fun trophyRes(tier: TrophyTier): Int = tier.drawableRes()

fun trophyDisplayName(tier: TrophyTier): String = when (tier) {
    TrophyTier.UNRANKED -> "Unranked"
    TrophyTier.BRONZE -> "Bronze Trophy"
    TrophyTier.SILVER -> "Silver Trophy"
    TrophyTier.GOLD -> "Gold Trophy"
    TrophyTier.PLATINUM -> "Platinum Trophy"
    TrophyTier.DIAMOND -> "Diamond Trophy"
    TrophyTier.ELITE -> "Elite Trophy"
}

fun trophyRequirementText(tier: TrophyTier): String = when (tier) {
    TrophyTier.UNRANKED -> "Start playing to earn your first trophy."
    TrophyTier.BRONZE -> "Reach " + EconomyConfig.BRONZE_SCORE.formatNumber() + " score to unlock Bronze."
    TrophyTier.SILVER -> "Reach " + EconomyConfig.SILVER_SCORE.formatNumber() + " score to unlock Silver."
    TrophyTier.GOLD -> "Reach " + EconomyConfig.GOLD_SCORE.formatNumber() + " score to unlock Gold."
    TrophyTier.PLATINUM -> "Reach " + EconomyConfig.PLATINUM_SCORE.formatNumber() + " score or unlock with " + EconomyConfig.PLATINUM_COINS.formatNumber() + " lifetime coins."
    TrophyTier.DIAMOND -> "Reach " + EconomyConfig.DIAMOND_SCORE.formatNumber() + " score or unlock with " + EconomyConfig.DIAMOND_COINS.formatNumber() + " lifetime coins."
    TrophyTier.ELITE -> "Reach " + EconomyConfig.ELITE_SCORE.formatNumber() + " score or unlock with " + EconomyConfig.ELITE_COINS.formatNumber() + " lifetime coins."
}

fun trophyEarnedText(tier: TrophyTier): String = when (tier) {
    TrophyTier.UNRANKED -> "Start playing to climb into the trophy ranks."
    TrophyTier.BRONZE -> "You earned Bronze by reaching " + EconomyConfig.BRONZE_SCORE.formatNumber() + " score."
    TrophyTier.SILVER -> "You earned Silver by reaching " + EconomyConfig.SILVER_SCORE.formatNumber() + " score."
    TrophyTier.GOLD -> "You earned Gold by reaching " + EconomyConfig.GOLD_SCORE.formatNumber() + " score."
    TrophyTier.PLATINUM -> "You earned Platinum by score or lifetime coin progression."
    TrophyTier.DIAMOND -> "You earned Diamond by score or lifetime coin progression."
    TrophyTier.ELITE -> "You earned Elite by score or lifetime coin progression."
}

private fun Int.formatNumber(): String = NumberFormat.getIntegerInstance(Locale.US).format(this)
