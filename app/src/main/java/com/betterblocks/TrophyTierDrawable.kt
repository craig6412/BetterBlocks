package com.betterblocks.model

import com.betterblocks.R

fun TrophyTier.drawableRes(): Int {
    return when (this) {
        TrophyTier.BRONZE -> R.drawable.bronze
        TrophyTier.SILVER -> R.drawable.silver
        TrophyTier.GOLD -> R.drawable.gold
        TrophyTier.PLATINUM -> R.drawable.platinum
        TrophyTier.DIAMOND -> R.drawable.diamond
        TrophyTier.ELITE -> R.drawable.elite
        TrophyTier.UNRANKED -> R.drawable.bronze // fallback (or a locked icon later)
    }
}
