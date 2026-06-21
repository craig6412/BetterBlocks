package com.betterblocks.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Reschedules the daily reminder alarm after a device reboot, because AlarmManager
 * alarms do not survive power cycles. Registered for ACTION_BOOT_COMPLETED in the manifest.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return
        val appContext = context.applicationContext
        if (!NotificationManagerHelper.areNotificationsEnabled(appContext)) return
        NotificationManagerHelper.getScheduledTime(appContext)?.let { (hour, minute) ->
            NotificationManagerHelper.scheduleDailyReminder(appContext, hour, minute)
        }
    }
}
