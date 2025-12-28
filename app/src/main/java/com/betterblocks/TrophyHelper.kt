package com.betterblocks

import com.betterblocks.model.TrophyTier
import com.betterblocks.model.drawableRes

// Thin helper to avoid extension-import resolution issues across packages
fun trophyRes(tier: TrophyTier): Int = tier.drawableRes()

