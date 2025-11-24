package com.betterblocks

import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

object GameSettings {
    var gridOffsetX = mutableFloatStateOf(0f)
    var gridOffsetY = mutableFloatStateOf(-50f)

    // --- TUNING VARIABLES ---

    // 1. Header Compactness
    var headerVerticalPadding = mutableFloatStateOf(0f)

    // 2. Available Blocks Container Height
    // REDUCED: From 120f down to 90f to remove the "Gap" between grid and blocks
    var availableBlocksRowHeight = mutableFloatStateOf(90f)

    // 3. Block Preview Scale
    // Set to 1.3f to fit nicely in the 90dp height
    var availableBlockScale = mutableFloatStateOf(0.9f)

    // 4. Bottom Bar Padding
    var bottomBarPadding = mutableFloatStateOf(4f)

    // 5. Rotate Button Scale
    var rotateButtonScale = mutableFloatStateOf(1.0f)


    var bannerScale = mutableFloatStateOf(2.0f)

    // --- Bottom Bar Layout Controls ---
    var bottomBarVerticalPadding = mutableStateOf(12)        // dp
    var bottomBarHorizontalPadding = mutableStateOf(24)      // dp

    // --- Bottom Bar Button Size Controls ---
    var bottomBarButtonSize = mutableStateOf(55)             // dp

    // --- Icon Scale Controls (applies to rotate + wipe + rainbow) ---
    var bottomBarIconScale = mutableStateOf(.75f)            // scale multiplier
    var bottomBarIconSize = mutableStateOf(36)               // dp

    // --- Counter Badge Scale ---

    var bottomBarBadgeScale = mutableStateOf(0.78f)           // 1.0f = default size

    val bottomBarIconSpacing = mutableStateOf(24.dp)

    // Border around buttons (rainbow/color wipe)
    val bottomBarButtonBorderWidth = mutableStateOf(2.dp)

    // Badge scale ("x63", "x0")


    // Icon sizes

    val bottomBarSpecialIconSize = mutableStateOf(55.dp)

    // Button corner radius
    val bottomBarButtonCorner = mutableStateOf(16.dp)

    // Padding for the bottom bar row

}

        // Bottom bar padding between icons




