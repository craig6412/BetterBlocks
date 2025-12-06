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

    // --- DRAG & DROP OFFSET CONTROLS (for Developer Tuning) ---
    // Visual offsets: Where the dragged block appears relative to finger
    // DEFAULT UPDATED: set visual drag Y to 80dp above the finger as requested
    var visualDragOffsetY = mutableFloatStateOf(80f)  // dp - block appears 80dp above finger
    var visualDragOffsetX = mutableFloatStateOf(0f)    // dp - block centered horizontally on finger

    // Ghost/Drop matching offsets: Alignment between ghost and actual drop position
    // Kept independent so ghost/drop calculation can be tuned separately from visual preview
    var matchingDragOffsetY = mutableFloatStateOf(80f) // dp - ghost Y alignment
    var matchingDragOffsetX = mutableFloatStateOf(0f)   // dp - ghost X alignment (centered)

    // --- BLOCK PLACEMENT CORRECTION (Fine-tuning for exact placement) ---
    // This applies a small correction to the drop position calculation to ensure
    // blocks place exactly where the green ghost appears
    var blockPlacementCorrectionX = mutableFloatStateOf(0f) // dp - ZEROED for clean baseline
    var blockPlacementCorrectionY = mutableFloatStateOf(0f) // dp - ZEROED for clean baseline

    // --- DRAGGED BLOCK SCALE ---
    var draggedBlockScale = mutableFloatStateOf(0.7f) // Scale of the block preview while dragging

    // --- INVENTORY CONTROLS (for Testing) ---
    var testRainbowCount = mutableStateOf(0)     // 0 to 1,000,000
    var testColorWipeCount = mutableStateOf(0)   // 0 to 1,000,000
    var testCoins = mutableStateOf(0)            // 0 to 1,000,000
    var testScore = mutableStateOf(0)            // 0 to 1,000,000 - Affects current game score
}

        // Bottom bar padding between icons
