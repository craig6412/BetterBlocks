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

class SettingsActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val initialSoundEnabled = prefs.getBoolean(KEY_SOUND_ENABLED, true)

        setContent {
            MaterialTheme {
                SettingsScreen(
                    initialSoundEnabled = initialSoundEnabled,
                    onToggleSound = {
                        val newValue = !prefs.getBoolean(KEY_SOUND_ENABLED, true)
                        prefs.edit().putBoolean(KEY_SOUND_ENABLED, newValue).apply()
                    },
                    onBack = { finish() }
                )
            }
        }
    }
}
