package com.betterblocks

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.betterblocks.PREFS_NAME
import com.betterblocks.KEY_SOUND_ENABLED
import com.betterblocks.ui.theme.BetterBlocksTheme


class SettingsActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val initialSoundEnabled = prefs.getBoolean(KEY_SOUND_ENABLED, true)
        val initialHapticEnabled = prefs.getBoolean(KEY_HAPTIC_ENABLED, true)
        val initialDarkTheme = prefs.getBoolean(KEY_DARK_THEME, false)

        setContent {
            MaterialTheme {
                SettingsScreen(
                    initialSoundEnabled = initialSoundEnabled,
                    initialHapticEnabled = initialHapticEnabled,
                    initialDarkTheme = initialDarkTheme,
                    onToggleSound = {
                        val newValue = !prefs.getBoolean(KEY_SOUND_ENABLED, true)
                        prefs.edit().putBoolean(KEY_SOUND_ENABLED, newValue).apply()
                    },
                    onToggleHaptic = {
                        val newValue = !prefs.getBoolean(KEY_HAPTIC_ENABLED, true)
                        prefs.edit().putBoolean(KEY_HAPTIC_ENABLED, newValue).apply()
                    },
                    onToggleTheme = {
                        val newValue = !prefs.getBoolean(KEY_DARK_THEME, false)
                        prefs.edit().putBoolean(KEY_DARK_THEME, newValue).apply()
                        // Recreate activity to apply the theme change immediately
                        recreate()
                    },
                    onBack = { finish() }
                )
            }
        }
    }
}
