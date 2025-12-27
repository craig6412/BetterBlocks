package com.betterblocks

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.betterblocks.notifications.NotificationManagerHelper

/**
 * Receives FCM messages (topic broadcasts for highscores) and shows a local notification
 * using NotificationManagerHelper so all notification UI is centralized.
 */
class HighscoreFirebaseService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // Optional: upload token to your server if you use per-device targeting instead of topic
        Log.d("HighscoreFirebaseService", "FCM token refreshed: $token")
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        try {
            val data = message.data
            val playerName = data["playerName"] ?: message.notification?.title ?: "Player"
            val score = data["score"] ?: ""
            val tier = data["tier"] ?: ""

            val title = when {
                tier.isNotEmpty() -> "${playerName} reached tier $tier"
                score.isNotEmpty() -> "${playerName} got a new highscore"
                else -> message.notification?.title ?: "New Highscore"
            }

            val body = when {
                tier.isNotEmpty() -> "${playerName} reached tier $tier — score: $score"
                score.isNotEmpty() -> "${playerName} scored $score"
                else -> message.notification?.body ?: "Check the leaderboard!"
            }

            NotificationManagerHelper.showHighscoreNotification(applicationContext, title, body)
        } catch (t: Throwable) {
            Log.w("HighscoreFirebaseService", "Failed to process FCM message: ${t.message}")
        }
    }
}

