package com.betterblocks

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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
import com.betterblocks.notifications.NotificationManagerHelper


class MainActivity : ComponentActivity() {

    // ✔ Create the GameViewModel using Android's viewModel delegate
    private val gameViewModel: GameViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize the notifications subsystem (creates channels, loads prefs). We do NOT
        // schedule anything here — scheduling occurs only when the user enables and sets a time.
        NotificationManagerHelper.initialize(applicationContext)

        // Request POST_NOTIFICATIONS permission from an Activity-level flow if the app-level
        // highscore notifications pref is ON. NOTE: Android does not allow auto-accepting
        // runtime permissions at install time; the user must explicitly grant them. We request
        // at first run to minimize friction.
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val hsEnabledPref = prefs.getBoolean(KEY_HIGHSCORE_NOTIFICATIONS, true)

        if (hsEnabledPref) {
            // Use ActivityResult API to request permission on Android 13+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val requestPermissionLauncher = registerForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { isGranted: Boolean ->
                    if (isGranted) {
                        // User granted — enable and schedule default reminder (20:00)
                        NotificationManagerHelper.enableNotifications(applicationContext)
                        NotificationManagerHelper.scheduleDailyReminder(applicationContext, 20, 0)
                    } else {
                        // Permission denied — leave pref as-is so the Settings toggle shows state.
                        // TODO: consider showing a rationale or directing user to system settings.
                    }
                }

                if (NotificationManagerHelper.isPostNotificationsPermissionGranted(this)) {
                    // Already granted
                    NotificationManagerHelper.enableNotifications(applicationContext)
                    NotificationManagerHelper.scheduleDailyReminder(applicationContext, 20, 0)
                } else {
                    // Ask the system dialog
                    requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                }
            } else {
                // On older OS versions notifications are implicitly allowed; enable and schedule.
                NotificationManagerHelper.enableNotifications(applicationContext)
                NotificationManagerHelper.scheduleDailyReminder(applicationContext, 20, 0)
            }
        }

        // TODO: When wiring the Settings UI, call NotificationManagerHelper.enableNotifications/disableNotifications
        // and request POST_NOTIFICATIONS permission (Android 13+) from an Activity-level flow.

        WindowCompat.setDecorFitsSystemWindows(window, false)

        FirestoreManager.init(this)

        AdManager.initialize(this)
        // Preload an interstitial ad on startup so it's ready at game-over time
        AdManager.preloadInterstitial(this)
        SoundManager.init(this)

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
        // Apply developer overrides only in debug builds to avoid overwriting prefs with default test values
        if (BuildConfig.DEBUG) {
            gameViewModel.applyDeveloperOverrides()
        }
    }
}
