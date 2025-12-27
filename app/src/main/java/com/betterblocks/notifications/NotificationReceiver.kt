package com.betterblocks.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import android.app.PendingIntent
import android.app.TaskStackBuilder
import com.betterblocks.MainActivity
import android.os.Build
import androidx.core.content.ContextCompat

/**
 * Receiver invoked by AlarmManager to show notifications.
 * Registered in AndroidManifest so it will be delivered even if the app process is killed.
 * Keep this receiver lightweight; it should show the notification quickly and reschedule the next alarm.
 */
class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val appContext = context.applicationContext
        val action = intent?.action

        if (action == ACTION_DAILY_REMINDER) {
            // Build a simple notification
            val title = "Your blocks miss you 👀"
            val text = "Open BetterBlocks to play a quick round and earn rewards!"

            // Back stack so tapping notification opens MainActivity
            val pendingIntent = TaskStackBuilder.create(appContext).run {
                addNextIntentWithParentStack(Intent(appContext, MainActivity::class.java))
                getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            }

            // Respect runtime permission on Android 13+
            val canNotify = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(appContext, android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED
            } else {
                true
            }

            if (canNotify) {
                val builder = NotificationCompat.Builder(appContext, CHANNEL_ID_REMINDERS)
                    .setSmallIcon(com.betterblocks.R.mipmap.ic_launcher)
                    .setContentTitle(title)
                    .setContentText(text)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)

                NotificationManagerCompat.from(appContext).notify(NOTIFICATION_ID_DAILY, builder.build())
            }

            // Reschedule the next day's reminder if the helper indicates notifications are enabled
            if (NotificationManagerHelper.areNotificationsEnabled(appContext)) {
                NotificationManagerHelper.getScheduledTime(appContext)?.let { (hour, minute) ->
                    NotificationManagerHelper.scheduleDailyReminder(appContext, hour, minute)
                }
            }
        }
    }
}
