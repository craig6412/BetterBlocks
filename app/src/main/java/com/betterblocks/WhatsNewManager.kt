package com.betterblocks

import android.content.Context
import android.content.SharedPreferences
import android.os.Build

object WhatsNewManager {
    private const val PREFS_FILE = "whats_new_prefs"
    private const val KEY_LAST_SEEN_VERSION_CODE = "last_seen_version_code"

    fun shouldShowWhatsNew(context: Context): Boolean {
        val currentVersionCode = getCurrentVersionCode(context)
        val prefs = prefs(context)
        val lastSeen = prefs.getLong(KEY_LAST_SEEN_VERSION_CODE, 0L)

        // Fresh install behavior: show once is OK; if you want "don't show on fresh installs",
        // change this to return false when lastSeen == 0L.
        return currentVersionCode > lastSeen
    }

    fun markWhatsNewSeen(context: Context) {
        val currentVersionCode = getCurrentVersionCode(context)
        prefs(context).edit().putLong(KEY_LAST_SEEN_VERSION_CODE, currentVersionCode).apply()
    }

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)

    private fun getCurrentVersionCode(context: Context): Long {
        val pm = context.packageManager
        val pkgName = context.packageName

        @Suppress("DEPRECATION")
        val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getPackageInfo(pkgName, android.content.pm.PackageManager.PackageInfoFlags.of(0))
        } else {
            pm.getPackageInfo(pkgName, 0)
        }

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            info.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            info.versionCode.toLong()
        }
    }
}

