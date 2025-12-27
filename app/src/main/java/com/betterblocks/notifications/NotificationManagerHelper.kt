package com.betterblocks.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import androidx.core.content.ContextCompat
import android.app.NotificationChannel
import android.app.NotificationManager
import java.util.Calendar
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import android.app.TaskStackBuilder
import com.betterblocks.HighScoreActivity
import com.betterblocks.R

// Minimal, namespaced constants to avoid magic numbers spread across the codebase
internal const val PREFS_NAME = "bb_notifications_prefs"
internal const val PREF_ENABLED = "notification_enabled"
internal const val PREF_DAILY_HOUR = "daily_hour"
internal const val PREF_DAILY_MINUTE = "daily_minute"

internal const val CHANNEL_ID_REMINDERS = "bb_channel_reminders"
internal const val ACTION_DAILY_REMINDER = "com.betterblocks.notifications.ACTION_DAILY_REMINDER"
internal const val NOTIFICATION_ID_DAILY = 1001

/**
 * NotificationManagerHelper
 * Responsibilities:
 *  - create notification channels
 *  - persist enabled state in SharedPreferences (default OFF)
 *  - schedule and cancel alarms via AlarmManager
 *  - expose simple functions the UI can call
 *
 * Important: This class never holds a Context reference beyond the call lifetime.
 * Always pass applicationContext from the caller (see initialize()).
 */
object NotificationManagerHelper {

    // Initialize notification channels and internal state. Safe to call repeatedly.
    fun initialize(context: Context) {
        val appContext = context.applicationContext
        createNotificationChannels(appContext)
        // Do NOT schedule anything automatically here — wait for explicit user action.
    }

    /**
     * Display a highscore/tier notification received via FCM.
     * This is intentionally small and stateless: the FCM service calls this when a message arrives.
     */
    fun showHighscoreNotification(context: Context, title: String, body: String) {
        val appContext = context.applicationContext

        // Respect runtime permission on Android 13+
        val canNotify = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(appContext, android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED
        } else true

        if (!canNotify) return

        val pendingIntent = TaskStackBuilder.create(appContext).run {
            addNextIntentWithParentStack(Intent(appContext, HighScoreActivity::class.java))
            getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT or if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_IMMUTABLE else 0)
        }

        val notifId = (System.currentTimeMillis() % Int.MAX_VALUE).toInt()
        val builder = NotificationCompat.Builder(appContext, CHANNEL_ID_REMINDERS)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        NotificationManagerCompat.from(appContext).notify(notifId, builder.build())
    }

    // Public API for UI wiring
    fun enableNotifications(context: Context) {
        val appContext = context.applicationContext
        prefs(appContext).edit().putBoolean(PREF_ENABLED, true).apply()
        // TODO: The UI should request POST_NOTIFICATIONS permission (Android 13+) before scheduling.
    }

    fun disableNotifications(context: Context) {
        val appContext = context.applicationContext
        prefs(appContext).edit().putBoolean(PREF_ENABLED, false).apply()
        cancelAllNotifications(appContext)
    }

    fun areNotificationsEnabled(context: Context): Boolean {
        return prefs(context.applicationContext).getBoolean(PREF_ENABLED, false)
    }

    // Return the persisted scheduled time if present, otherwise null.
    fun getScheduledTime(context: Context): Pair<Int, Int>? {
        val p = prefs(context.applicationContext)
        return if (p.contains(PREF_DAILY_HOUR) && p.contains(PREF_DAILY_MINUTE)) {
            Pair(p.getInt(PREF_DAILY_HOUR, 9), p.getInt(PREF_DAILY_MINUTE, 0))
        } else null
    }

    // Schedules a daily reminder at local hour/minute. This schedules the next occurrence only and
    // relies on the receiver to reschedule the next day after firing. This avoids long-running repeat
    // alarms crossing DST boundaries.
    fun scheduleDailyReminder(context: Context, hour: Int, minute: Int) {
        val appContext = context.applicationContext
        prefs(appContext).edit()
            .putInt(PREF_DAILY_HOUR, hour)
            .putInt(PREF_DAILY_MINUTE, minute)
            .apply()

        // Only schedule if the user has enabled notifications
        if (!areNotificationsEnabled(appContext)) return

        val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(ACTION_DAILY_REMINDER).apply {
            // Use explicit class name to avoid compile-time resolution ordering issues in some tools
            setClassName(appContext, "com.betterblocks.notifications.NotificationReceiver")
            putExtra(PREF_DAILY_HOUR, hour)
            putExtra(PREF_DAILY_MINUTE, minute)
        }

        val pending = PendingIntent.getBroadcast(
            appContext,
            0,
            intent,
            pendingIntentFlags()
        )

        val triggerAt = nextTriggerMillis(hour, minute)

        // Use exact alarm where possible to make reminders feel timely. setExactAndAllowWhileIdle
        // helps with Doze; acceptable for once-per-day reminders.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAt, pending)
        } else {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAt, pending)
        }
    }

    // Cancel scheduled alarms and clear persisted schedule
    fun cancelAllNotifications(context: Context) {
        val appContext = context.applicationContext
        val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(ACTION_DAILY_REMINDER).apply {
            setClassName(appContext, "com.betterblocks.notifications.NotificationReceiver")
        }
        val pending = PendingIntent.getBroadcast(appContext, 0, intent, pendingIntentFlags())
        alarmManager.cancel(pending)

        prefs(appContext).edit()
            .remove(PREF_DAILY_HOUR)
            .remove(PREF_DAILY_MINUTE)
            .apply()

        // Optionally clear enabled flag? We keep the enabled flag as the source of truth and
        // only update it when disableNotifications() is called explicitly.
    }

    // Helper: compute next trigger time in millis for a given local hour/minute
    private fun nextTriggerMillis(hour: Int, minute: Int): Long {
        val now = Calendar.getInstance()
        val next = Calendar.getInstance()
        next.set(Calendar.HOUR_OF_DAY, hour)
        next.set(Calendar.MINUTE, minute)
        next.set(Calendar.SECOND, 0)
        next.set(Calendar.MILLISECOND, 0)

        if (!next.after(now)) {
            // schedule for next day
            next.add(Calendar.DATE, 1)
        }
        return next.timeInMillis
    }

    private fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Reminders channel: for daily and game-based reminders. Use default importance so users
        // can control it via system settings.
        val channel = NotificationChannel(
            CHANNEL_ID_REMINDERS,
            "Reminders",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Daily and inactivity reminders from BetterBlocks"
        }

        manager.createNotificationChannel(channel)
    }

    // Expose helper for the UI / Activity to check runtime permission state.
    fun isPostNotificationsPermissionGranted(context: Context): Boolean {
        // On Android 13+ this checks runtime permission; on older platforms notifications are implicitly allowed.
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    // Read SharedPreferences instance scoped to this helper
    private fun prefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    // Flags for PendingIntent with API compatibility
    private fun pendingIntentFlags(): Int {
        var flags = PendingIntent.FLAG_UPDATE_CURRENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            flags = flags or PendingIntent.FLAG_IMMUTABLE
        }
        return flags
    }
}
