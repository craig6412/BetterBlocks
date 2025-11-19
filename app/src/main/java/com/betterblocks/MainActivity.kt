package com.betterblocks

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.betterblocks.ui.MainMenuScreen
import com.betterblocks.ui.theme.BetterBlocksTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            BetterBlocksTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Display the Main Menu UI with functional callbacks
                    MainMenuScreen(
                        onPlayClicked = {
                            // Launch the Game Activity
                            val intent = Intent(this, GameActivity::class.java)
                            startActivity(intent)
                        },
                        onShopClicked = {
                            // Launch Shop Activity (Placeholder for now)
                            val intent = Intent(this, ShopActivity::class.java)
                            startActivity(intent)
                        },
                        onHighScoresClicked = {
                            // Launch High Scores Activity (Placeholder for now)
                            val intent = Intent(this, HighscoresActivity::class.java)
                            startActivity(intent)
                        },
                        onSettingsClicked = {
                            // Launch Settings Activity (Placeholder for now)
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