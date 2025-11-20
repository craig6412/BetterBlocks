package com.betterblocks

import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.unit.dp

/**
 * A simple singleton to hold game settings in memory for this session.
 * In a full app, you would save these to SharedPreferences or DataStore.
 */
object GameSettings {
    // Main Menu Banner Scale
    var bannerScale = mutableFloatStateOf(2.0f)

    // Game Screen: Rotate Button Scale
    var rotateButtonScale = mutableFloatStateOf(1.0f)

    // Game Screen: Grid Position Offsets (in Dp)
    // We store these as Float here for Sliders, but convert to Dp in UI
    var gridOffsetX = mutableFloatStateOf(0f)
    var gridOffsetY = mutableFloatStateOf(0f)
}