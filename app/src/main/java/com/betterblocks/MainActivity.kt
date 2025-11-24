package com.betterblocks

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.betterblocks.ui.DarkBackground
import com.betterblocks.ui.MainMenuScreen
import com.betterblocks.ui.theme.BetterBlocksTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // --- THEME CONSISTENCY ---
        // Set System Bar Colors to match Game Theme immediately on launch
        window.statusBarColor = android.graphics.Color.parseColor("#1E214A")
        window.navigationBarColor = android.graphics.Color.parseColor("#1E214A")

        setContent {
            BetterBlocksTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    // Use our specific DarkBackground instead of the default Material theme background
                    color = DarkBackground
                ) {


                    // Display the Main Menu UI with functional callbacks
                    MainMenuScreen(
                        onPlayClicked = {
                            // Launch the Game
                            val intent = Intent(this, GameActivity::class.java)
                            startActivity(intent)
                        },
                        onShopClicked = {
                            // Launch the Shop (Now fully implemented)
                            val intent = Intent(this, ShopActivity::class.java)
                            startActivity(intent)
                        },
                        onHighScoresClicked = {
                            // Launch High Scores (Coming Soon screen)
                            val intent = Intent(this, HighScoreActivity::class.java)
                            startActivity(intent)
                        },
                        onSettingsClicked = {
                            // Launch Settings (Coming Soon screen)
                            val intent = Intent(this, SettingsActivity::class.java)
                            startActivity(intent)
                        },
                        onDeveloperClicked = {
                            // Launch the Developer Activity
                            val intent = Intent(this, DeveloperActivity::class.java)
                            startActivity(intent)
                        }
                    )
                }
            }
        }
    }
}