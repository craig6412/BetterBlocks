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

    // 5. Rotate Button Scale
    var rotateButtonScale = mutableFloatStateOf(1.0f)


    var bannerScale = mutableStateOf(2.0f)

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
    var visualDragOffsetY = mutableFloatStateOf(0f)  // dp - block appears 80dp above finger
    var visualDragOffsetX = mutableFloatStateOf(0f)    // dp - block centered horizontally on finger

    // Ghost/Drop matching offsets: Alignment between ghost and actual drop position
    // Kept independent so ghost/drop calculation can be tuned separately from visual preview
    var matchingDragOffsetY = mutableFloatStateOf(0f) // dp - ghost Y alignment
    var matchingDragOffsetX = mutableFloatStateOf(0f)   // dp - ghost X alignment (centered)

    // --- BLOCK PLACEMENT CORRECTION (Fine-tuning for exact placement) ---
    // This applies a small correction to the drop position calculation to ensure
    // blocks place exactly where the green ghost appears
    var blockPlacementCorrectionX = mutableFloatStateOf(0f) // dp - ZEROED for clean baseline
    var blockPlacementCorrectionY = mutableFloatStateOf(0f) // dp - ZEROED for clean baseline

    // --- DRAGGED BLOCK SCALE ---
    // 1.0f = same size as placed blocks on the game grid
    var draggedBlockScale = mutableFloatStateOf(1.0f) // Scale of the block preview while dragging
    // Final correction offsets applied to the drag preview & ghost alignment
    var dragCorrectionX = mutableFloatStateOf(0f)   // in dp
    var dragCorrectionY = mutableFloatStateOf(0f)   // in dp
    // --- INVENTORY CONTROLS (for Testing) ---
    var testRainbowCount = mutableStateOf(0)     // 0 to 1,000,000
    var testColorWipeCount = mutableStateOf(0)   // 0 to 1,000,000
    var testCoins = mutableStateOf(0)            // 0 to 1,000,000
    var testScore = mutableStateOf(0)            // 0 to 1,000,000 - Affects current game score

    // --- Season control (single epoch all users share on release) ---
    /**
     * Set this to the epoch millis you want the season to start for everyone.
     * Example: 1704067200000L == 2024-01-01T00:00:00Z
     */
    @JvmStatic
    var seasonStartEpochMs: Long = 1704067200000L

    @JvmStatic
    var seasonLengthDays: Int = 90

    fun seasonEndEpochMs(): Long = seasonStartEpochMs + seasonLengthDays * 24L * 60L * 60L * 1000L

    fun isSeasonActive(now: Long = System.currentTimeMillis()): Boolean =
        now >= seasonStartEpochMs && now < seasonEndEpochMs()
}

        // Bottom bar padding between icons
