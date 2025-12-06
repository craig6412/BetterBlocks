package com.betterblocks

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * HapticManager - Provides haptic feedback for user interactions
 */
object HapticManager {

    /**
     * Short haptic feedback (e.g., block placement)
     */
    fun vibrateShort(context: Context) {
        vibrate(context, 30L)
    }

    /**
     * Medium haptic feedback (e.g., line clear, IAP purchase)
     */
    fun vibrateMedium(context: Context) {
        vibrate(context, 50L)
    }

    /**
     * Heavy haptic feedback (e.g., rainbow wipe, tier unlock, game over)
     */
    fun vibrateHeavy(context: Context) {
        vibrate(context, 100L)
    }

    private fun vibrate(context: Context, durationMs: Long) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val isHapticEnabled = prefs.getBoolean(KEY_HAPTIC_ENABLED, true)

        if (!isHapticEnabled) return

        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }

            vibrator?.let {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    it.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    it.vibrate(durationMs)
                }
            }
        } catch (e: Exception) {
            // Ignore vibration errors (device might not support it)
        }
    }
}

