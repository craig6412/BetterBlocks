package com.betterblocks

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.betterblocks.ads.AdManager
import com.betterblocks.ads.BannerAdView
import com.betterblocks.ui.MainMenuScreen
import com.betterblocks.ui.theme.BetterBlocksTheme
import androidx.core.view.WindowCompat
import android.graphics.Color


class MainActivity : ComponentActivity() {

    // ✔ Create the GameViewModel using Android's viewModel delegate
    private val gameViewModel: GameViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        FirestoreManager.init(this)

        AdManager.initialize(this)
        // Preload an interstitial ad on startup so it's ready at game-over time
        AdManager.preloadInterstitial(this)
        SoundManager.init(this)

        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val darkTheme = prefs.getBoolean(KEY_DARK_THEME, false)

        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT

        // Theme

        setContent {
            BetterBlocksTheme(darkTheme = darkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = DarkBackground
                ) {

                    MainMenuScreen(
                        viewModel = gameViewModel,   // ✔ FIXED — now passed properly

                        onPlayClicked = {
                            startActivity(Intent(this, GameActivity::class.java))
                        },
                        onShopClicked = {
                            startActivity(Intent(this, ShopActivity::class.java))
                        },
                        onHighScoresClicked = {
                            startActivity(Intent(this, HighScoreActivity::class.java))
                        },
                        onStatsClicked = {
                            startActivity(Intent(this, StatsActivity::class.java))
                        },
                        onSettingsClicked = {
                            startActivity(Intent(this, SettingsActivity::class.java))
                        },
                        onDeveloperClicked = {
                            startActivity(Intent(this, DeveloperActivity::class.java))
                        },

                        banner = { BannerAdView(modifier = Modifier.fillMaxWidth()) }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh user stats when returning to main menu (e.g., from Shop)
        gameViewModel.refreshUserStats()
        // Apply any DeveloperActivity overrides (inventory + tuning) into live state
        gameViewModel.applyDeveloperOverrides()
    }
}
