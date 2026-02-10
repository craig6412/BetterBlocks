package com.betterblocks.ads

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import com.betterblocks.BuildConfig
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

object AdManager {

    private const val TAG = "AdManager"

    // Coins granted when a rewarded ad completes.
    const val REWARDED_COINS = 100

    // Countdown (in seconds) for the active rewarded flow; UI can observe this.
    val rewardedSecondsLeft = mutableStateOf(0)

    // Cancel/replace countdown ticks safely
    private var countdownRunnable: Runnable? = null

    // Prevent overlapping rewarded flows.
    private var isRewardedInProgress: Boolean = false

    // Single rewarded ad
    private var rewardedAd: RewardedAd? = null

    // Track loaded state for UI
    val isRewardedLoaded = mutableStateOf(false)

    // Interstitial (for every-other-game-over logic)
    private var interstitialAd: InterstitialAd? = null
    private var gameOverCounter = 0

    private val handler = Handler(Looper.getMainLooper())

    val bannerAdUnitId: String
        get() = BuildConfig.BANNER_AD_UNIT_ID

    val rewardedAdUnitId: String
        get() = BuildConfig.REWARDED_AD_UNIT_ID

    val interstitialAdUnitId: String
        get() = BuildConfig.INTERSTITIAL_AD_UNIT_ID

    fun initialize(context: Context) {
        MobileAds.initialize(context)
    }

    // Helper to start and clear the countdown state (ticks once/second on main thread)
    private fun startRewardedCountdown(totalSeconds: Int) {
        // Cancel any previous countdown loop
        countdownRunnable?.let { handler.removeCallbacks(it) }
        countdownRunnable = null

        rewardedSecondsLeft.value = totalSeconds

        val runnable = object : Runnable {
            override fun run() {
                val current = rewardedSecondsLeft.value
                if (current <= 0) {
                    countdownRunnable = null
                    return
                }
                rewardedSecondsLeft.value = current - 1
                handler.postDelayed(this, 1000L)
            }
        }
        countdownRunnable = runnable
        handler.postDelayed(runnable, 1000L)
    }

    private fun clearRewardedCountdown() {
        countdownRunnable?.let { handler.removeCallbacks(it) }
        countdownRunnable = null
        rewardedSecondsLeft.value = 0
    }

    // -----------------------------
    // Banner Ads
    // -----------------------------
    fun loadBanner(adView: AdView) {
        val request = AdRequest.Builder().build()
        adView.loadAd(request)
    }

    // -----------------------------
    // Rewarded Ads — single
    // -----------------------------
    fun preloadRewarded(context: Context) {
        isRewardedLoaded.value = false

        val request = AdRequest.Builder().build()

        RewardedAd.load(
            context,
            rewardedAdUnitId,
            request,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    isRewardedLoaded.value = true
                    Log.d(TAG, "RewardedAd loaded")
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    rewardedAd = null
                    isRewardedLoaded.value = false
                    Log.e(TAG, "RewardedAd failed to load: ${error.message}")
                }
            }
        )
    }

    fun showRewarded(
        activity: Activity,
        onRewardEarned: () -> Unit,
        onFailed: (() -> Unit)? = null
    ) {
        if (isRewardedInProgress) {
            Log.w(TAG, "showRewarded: flow already running, ignoring extra call")
            onFailed?.invoke()
            return
        }

        val ad = rewardedAd
        if (ad == null) {
            Log.e(TAG, "showRewarded: no rewarded ad loaded")
            isRewardedInProgress = false
            clearRewardedCountdown()
            onFailed?.invoke()
            preloadRewarded(activity)
            return
        }

        Log.d(TAG, "Rewarded flow start")

        isRewardedInProgress = true
        startRewardedCountdown(60)
        isRewardedLoaded.value = false

        var earned = false

        fun cleanupAndPreload() {
            Log.d(TAG, "Rewarded flow cleanup")
            isRewardedInProgress = false
            clearRewardedCountdown()

            rewardedAd = null
            isRewardedLoaded.value = false

            preloadRewarded(activity)
        }

        fun failFlow(reason: String) {
            Log.e(TAG, "Rewarded flow failed: $reason")
            try {
                onFailed?.invoke()
            } catch (t: Throwable) {
                Log.w(TAG, "onFailed threw", t)
            } finally {
                cleanupAndPreload()
            }
        }

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                Log.d(TAG, "Rewarded ad dismissed (earned=$earned)")
                // If user never earned the reward, treat as a failure.
                if (!earned) {
                    failFlow("dismissed without reward")
                    return
                }
                // Success path: we already invoked onRewardEarned in the reward listener.
                cleanupAndPreload()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                failFlow("failed to show: ${adError.message}")
            }
        }

        ad.show(activity) {
            earned = true
            Log.d(TAG, "Reward earned")
            try {
                onRewardEarned()
            } catch (t: Throwable) {
                Log.w(TAG, "onRewardEarned threw", t)
            }
        }
    }

    // Backwards-compatible wrappers (no longer double): keep existing call sites compiling if any remain.
    @Deprecated("Use preloadRewarded(context)")
    fun preloadDoubleRewarded(context: Context) = preloadRewarded(context)

    @Deprecated("Use showRewarded(activity, onRewardEarned, onFailed)")
    fun showDoubleRewarded(
        activity: Activity,
        onCompletedBoth: () -> Unit,
        onFailed: (() -> Unit)? = null
    ) = showRewarded(activity, onCompletedBoth, onFailed)

    // -----------------------------
    // Interstitial – Load
    // -----------------------------
    fun preloadInterstitial(context: Context) {
        val request = AdRequest.Builder().build()

        InterstitialAd.load(
            context,
            interstitialAdUnitId,
            request,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    Log.d(TAG, "Interstitial loaded")
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialAd = null
                    Log.e(TAG, "Interstitial failed: ${error.message}")
                }
            }
        )
    }

    // -----------------------------
    // Interstitial – Show Every OTHER Game Over
    // -----------------------------
    fun tryShowInterstitial(activity: Activity) {
        gameOverCounter++

        if (gameOverCounter % 2 != 0) {
            // odd number → skip
            return
        }

        val ad = interstitialAd ?: run {
            Log.e(TAG, "Interstitial not loaded")
            return
        }

        ad.show(activity)
        interstitialAd = null

        preloadInterstitial(activity)
    }
}
