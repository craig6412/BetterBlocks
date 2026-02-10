package com.betterblocks

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import com.betterblocks.notifications.NotificationManagerHelper


class SettingsActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val initialSoundEnabled = prefs.getBoolean(KEY_SOUND_ENABLED, true)
        val initialHapticEnabled = prefs.getBoolean(KEY_HAPTIC_ENABLED, true)
        val initialDarkTheme = prefs.getBoolean(KEY_DARK_THEME, false)
        // Highscore notifications default to ON (per latest request)
        val initialHighscoreNotifications = prefs.getBoolean(KEY_HIGHSCORE_NOTIFICATIONS, true)

        val shopRepo = ShopRepository.get(applicationContext)

        setContent {
            MaterialTheme {
                SettingsScreen(
                    initialSoundEnabled = initialSoundEnabled,
                    initialHapticEnabled = initialHapticEnabled,
                    initialDarkTheme = initialDarkTheme,
                    initialHighscoreNotifications = initialHighscoreNotifications,
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
                    onToggleHighscoreNotifications = {
                        val current = prefs.getBoolean(KEY_HIGHSCORE_NOTIFICATIONS, true)
                        val newValue = !current
                        prefs.edit().putBoolean(KEY_HIGHSCORE_NOTIFICATIONS, newValue).apply()

                        if (newValue) {
                            // Enable notification subsystem and schedule a default daily reminder (20:00).
                            // TODO: request POST_NOTIFICATIONS permission from Activity if on Android 13+ before enabling.
                            NotificationManagerHelper.enableNotifications(applicationContext)
                            NotificationManagerHelper.scheduleDailyReminder(applicationContext, 20, 0)
                        } else {
                            NotificationManagerHelper.disableNotifications(applicationContext)
                        }
                    },
                    onApplyCoupon = { code ->
                        val result = CouponManager.applyCoupon(applicationContext, code, shopRepo)
                        Log.d("COUPON", "apply result=$result")
                        result
                    },
                    onBack = { finish() }
                )
            }
        }
    }
}
