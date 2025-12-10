package com.betterblocks

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.betterblocks.ui.DeveloperScreen
import com.betterblocks.ui.theme.BetterBlocksTheme

class DeveloperActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Load saved settings
        loadDeveloperSettings()

        setContent {
            BetterBlocksTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    DeveloperScreen(
                        onBackClicked = {
                            // Save settings before closing
                            saveDeveloperSettings()
                            finish()
                        }
                    )
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        // Auto-save whenever the activity is paused (user leaves screen)
        saveDeveloperSettings()
    }

    private fun loadDeveloperSettings() {
        val prefs = getSharedPreferences("developer_settings", Context.MODE_PRIVATE)

        // Load all settings
        GameSettings.gridOffsetX.floatValue = prefs.getFloat("gridOffsetX", 0f)
        GameSettings.gridOffsetY.floatValue = prefs.getFloat("gridOffsetY", -50f)
        GameSettings.headerVerticalPadding.floatValue = prefs.getFloat("headerVerticalPadding", 0f)
        GameSettings.availableBlocksRowHeight.floatValue = prefs.getFloat("availableBlocksRowHeight", 90f)
        GameSettings.bottomBarVerticalPadding.value = prefs.getInt("bottomBarVerticalPadding", 12)
        GameSettings.rotateButtonScale.floatValue = prefs.getFloat("rotateButtonScale", 1.0f)

        // Load drag offsets
        GameSettings.visualDragOffsetY.floatValue = prefs.getFloat("visualDragOffsetY", 150f)
        GameSettings.visualDragOffsetX.floatValue = prefs.getFloat("visualDragOffsetX", 20f)
        GameSettings.matchingDragOffsetY.floatValue = prefs.getFloat("matchingDragOffsetY", 125f)
        GameSettings.matchingDragOffsetX.floatValue = prefs.getFloat("matchingDragOffsetX", 55f)

        // Load placement correction offsets
        GameSettings.blockPlacementCorrectionX.floatValue = prefs.getFloat("blockPlacementCorrectionX", 5f)
        GameSettings.blockPlacementCorrectionY.floatValue = prefs.getFloat("blockPlacementCorrectionY", 0f)

        // Load inventory test values
        GameSettings.testRainbowCount.value = prefs.getInt("testRainbowCount", 0)
        GameSettings.testColorWipeCount.value = prefs.getInt("testColorWipeCount", 0)
        GameSettings.testCoins.value = prefs.getInt("testCoins", 0)
        GameSettings.testScore.value = prefs.getInt("testScore", 0)
    }

    private fun saveDeveloperSettings() {
        val prefs = getSharedPreferences("developer_settings", Context.MODE_PRIVATE)
        val editor = prefs.edit()

        // Save all settings
        editor.putFloat("gridOffsetX", GameSettings.gridOffsetX.floatValue)
        editor.putFloat("gridOffsetY", GameSettings.gridOffsetY.floatValue)
        editor.putFloat("headerVerticalPadding", GameSettings.headerVerticalPadding.floatValue)
        editor.putFloat("availableBlocksRowHeight", GameSettings.availableBlocksRowHeight.floatValue)
        editor.putInt("bottomBarVerticalPadding", GameSettings.bottomBarVerticalPadding.value)
        editor.putFloat("rotateButtonScale", GameSettings.rotateButtonScale.floatValue)

        // Save drag offsets
        editor.putFloat("visualDragOffsetY", GameSettings.visualDragOffsetY.floatValue)
        editor.putFloat("visualDragOffsetX", GameSettings.visualDragOffsetX.floatValue)
        editor.putFloat("matchingDragOffsetY", GameSettings.matchingDragOffsetY.floatValue)
        editor.putFloat("matchingDragOffsetX", GameSettings.matchingDragOffsetX.floatValue)

        // Save placement correction offsets
        editor.putFloat("blockPlacementCorrectionX", GameSettings.blockPlacementCorrectionX.floatValue)
        editor.putFloat("blockPlacementCorrectionY", GameSettings.blockPlacementCorrectionY.floatValue)

        // Save inventory test values
        editor.putInt("testRainbowCount", GameSettings.testRainbowCount.value)
        editor.putInt("testColorWipeCount", GameSettings.testColorWipeCount.value)
        editor.putInt("testCoins", GameSettings.testCoins.value)
        editor.putInt("testScore", GameSettings.testScore.value)

        editor.apply()

        // Notify running app that developer settings were saved so ViewModel can apply overrides
        val intent = Intent("com.betterblocks.ACTION_DEV_SETTINGS_CHANGED")
        sendBroadcast(intent)
    }
}