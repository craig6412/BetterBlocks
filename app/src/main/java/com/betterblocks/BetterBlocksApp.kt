package com.betterblocks

import android.app.Application
import android.util.Log
import com.betterblocks.ads.AdManager
import com.betterblocks.notifications.NotificationManagerHelper
import com.google.firebase.messaging.FirebaseMessaging

class BetterBlocksApp : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            // Initialize FirestoreManager early so other components can use it safely
            FirestoreManager.init(applicationContext)

            // Initialize notification channels and internal state; do not assume permission is granted.
            NotificationManagerHelper.initialize(applicationContext)

            // Subscribe to highscores topic for broadcast notifications. Subscription doesn't require
            // notification permission and can safely be done at app start.
            FirebaseMessaging.getInstance().subscribeToTopic("highscores")
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Log.d("BetterBlocksApp", "Subscribed to highscores topic")
                    } else {
                        Log.w("BetterBlocksApp", "Failed to subscribe to highscores topic", task.exception)
                    }
                }

            // NOTE: Do NOT request runtime POST_NOTIFICATIONS or schedule reminders from Application.onCreate.
            // Those flows require an Activity context to show the system permission dialog. Scheduling
            // will be started by MainActivity when permission is granted or when the user enables via UI.

            AdManager.initialize(applicationContext)
            // Preload double rewarded chain as early as possible
            AdManager.preloadDoubleRewarded(applicationContext)
            Log.d("BetterBlocksApp", "Initialized FirestoreManager and AdManager")
        } catch (t: Throwable) {
            Log.w("BetterBlocksApp", "Failed to initialize in Application.onCreate: ${t.message}")
        }
    }
}
