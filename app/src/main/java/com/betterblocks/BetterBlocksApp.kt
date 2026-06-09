package com.betterblocks

import android.app.Application
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.betterblocks.ads.AdManager
import com.betterblocks.notifications.NotificationManagerHelper
import com.google.firebase.messaging.FirebaseMessaging

class BetterBlocksApp : Application() {
    override fun onCreate() {
        super.onCreate()

        try {
            // Keep only the minimum app-wide setup synchronous. Heavy/network-backed work is staggered
            // so first launch and resume do not fight Compose, WebView, ad media, and Firebase all at once.
            FirestoreManager.init(applicationContext)
            NotificationManagerHelper.initialize(applicationContext)
            AdManager.initialize(applicationContext)
            // Pre-decode all block textures once so AnimatedBoardCell never allocates Bitmaps during drag.
            BlockTextureCache.init(applicationContext)

            Handler(Looper.getMainLooper()).postDelayed({
                preloadAdsSafely()
            }, 750L)

            Handler(Looper.getMainLooper()).postDelayed({
                subscribeToHighscoresSafely()
            }, 1_500L)

            Log.d("BetterBlocksApp", "Initialized core app services; deferred ads and FCM startup work")
        } catch (t: Throwable) {
            Log.w("BetterBlocksApp", "Failed to initialize in Application.onCreate: ${t.message}")
        }
    }

    private fun preloadAdsSafely() {
        try {
            AdManager.preloadRewarded(applicationContext)
            Log.d("BetterBlocksApp", "Deferred rewarded ad preload requested")
        } catch (t: Throwable) {
            Log.w("BetterBlocksApp", "Deferred rewarded preload failed: ${t.message}")
        }
    }

    private fun subscribeToHighscoresSafely() {
        try {
            // Subscription doesn't require notification permission and can safely be done after launch.
            FirebaseMessaging.getInstance().subscribeToTopic("highscores")
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Log.d("BetterBlocksApp", "Subscribed to highscores topic")
                    } else {
                        Log.w("BetterBlocksApp", "Failed to subscribe to highscores topic", task.exception)
                    }
                }
        } catch (t: Throwable) {
            Log.w("BetterBlocksApp", "Highscores topic subscription crashed: ${t.message}")
        }
    }
}
