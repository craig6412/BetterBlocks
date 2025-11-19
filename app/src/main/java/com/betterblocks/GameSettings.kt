package com.betterblocks

import androidx.compose.runtime.mutableFloatStateOf

/**
 * A simple singleton to hold game settings in memory for this session.
 * In a full app, you would save these to SharedPreferences or DataStore.
 */
object GameSettings {
    // Default scale is 1.0f (100%)
    var bannerScale = mutableFloatStateOf(1.0f)
}