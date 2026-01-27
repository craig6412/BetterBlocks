package com.betterblocks.ads

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.mutableStateOf
import com.betterblocks.BuildConfig
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import java.util.UUID

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

    // Track the current in-flight rewarded transaction (for post-dismiss messaging)
    @Volatile private var inFlightRewardTransactionId: String? = null

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

        val transactionId = UUID.randomUUID().toString()
        inFlightRewardTransactionId = transactionId

        Log.d(TAG, "Rewarded flow start txId=$transactionId unitType=rewarded")

        isRewardedInProgress = true
        startRewardedCountdown(60)
        isRewardedLoaded.value = false

        // NOTE: We only grant coins inside onUserEarnedReward.
        // This flag is for flow tracking; reward itself is ledger-based and idempotent.
        var rewardCallbackFired = false

        val ledger = RewardLedger.get(activity)

        fun cleanupAndPreload() {
            Log.d(TAG, "Rewarded flow cleanup txId=$transactionId")
            isRewardedInProgress = false
            clearRewardedCountdown()

            rewardedAd = null
            isRewardedLoaded.value = false

            // Clear in-flight transaction after the flow settles
            inFlightRewardTransactionId = null

            preloadRewarded(activity)
        }

        fun failFlow(reason: String) {
            Log.e(TAG, "Rewarded flow failed txId=$transactionId reason=$reason")
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
                val granted = ledger.wasGranted(transactionId)
                Log.d(
                    TAG,
                    "Rewarded ad dismissed txId=$transactionId unitType=rewarded rewardCallbackFired=$rewardCallbackFired granted=$granted"
                )

                // User-visible feedback on whether the SDK actually triggered reward.
                // (Keep UI intact; just a toast for now.)
                try {
                    val msg = if (granted) "Reward granted" else "Reward not triggered, try again"
                    Toast.makeText(activity, msg, Toast.LENGTH_SHORT).show()
                } catch (_: Throwable) {
                    // ignore toast failures
                }

                if (!granted) {
                    // Treat as failure so callers can retry.
                    failFlow("dismissed without reward")
                    return
                }

                // Success path: coins already persisted in onUserEarnedReward.
                cleanupAndPreload()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                failFlow("failed to show: ${adError.message}")
            }
        }

        ad.show(activity) {
            rewardCallbackFired = true

            // Only grant inside this callback.
            val grantedNow = ledger.grantRewardOnce(
                transactionId = transactionId,
                coinsToGrant = REWARDED_COINS,
                adUnitType = "rewarded"
            )

            Log.d(TAG, "onUserEarnedReward txId=$transactionId unitType=rewarded grantedNow=$grantedNow")

            // Preserve existing UI callbacks, but only after coins have been persisted.
            if (grantedNow) {
                try {
                    onRewardEarned()
                } catch (t: Throwable) {
                    Log.w(TAG, "onRewardEarned threw", t)
                }
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
