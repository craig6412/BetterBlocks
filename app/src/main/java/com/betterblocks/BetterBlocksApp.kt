package com.betterblocks

import android.app.Application
import android.util.Log
import com.betterblocks.ads.AdManager

class BetterBlocksApp : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            AdManager.initialize(applicationContext)
            // Preload double rewarded chain as early as possible
            AdManager.preloadDoubleRewarded(applicationContext)
            Log.d("BetterBlocksApp", "AdManager initialized and preload started")
        } catch (t: Throwable) {
            Log.w("BetterBlocksApp", "Failed to initialize AdManager: ${t.message}")
        }
    }
}

