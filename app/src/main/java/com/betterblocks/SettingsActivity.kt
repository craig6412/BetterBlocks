package com.betterblocks

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.betterblocks.ui.ComingSoonScreen
import com.betterblocks.ui.theme.BetterBlocksTheme

class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BetterBlocksTheme {
                ComingSoonScreen(title = "SETTINGS", onBack = { finish() })
            }
        }
    }
}