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

    // Public reward constant (coins granted when double rewarded ads complete)
    const val DOUBLE_REWARD_COINS = 50

    // Rewarded #1 and Rewarded #2 for double-chain
    private var rewardedAd1: RewardedAd? = null
    private var rewardedAd2: RewardedAd? = null

    // Track loaded state for UI
    // - isRewardedLoaded: at least one rewarded ad (ad #1) is loaded and clickable
    // - isDoubleRewardReady: both ads (#1 and #2) are loaded — full double flow available
    val isRewardedLoaded = mutableStateOf(false)
    val isDoubleRewardReady = mutableStateOf(false)

    // Interstitial (for every-other-game-over logic)
    private var interstitialAd: InterstitialAd? = null
    private var gameOverCounter = 0

    private val handler = Handler(Looper.getMainLooper())

    // Polling/wait policy for ad2 while user watches ad1
    private const val AD2_WAIT_MAX_MS = 8000L
    private const val AD2_POLL_INTERVAL_MS = 300L

    val bannerAdUnitId: String
        get() = BuildConfig.BANNER_AD_UNIT_ID

    val rewardedAdUnitId: String
        get() = BuildConfig.REWARDED_AD_UNIT_ID

    val interstitialAdUnitId: String
        get() = BuildConfig.INTERSTITIAL_AD_UNIT_ID   // ← Add to BuildConfig

    fun initialize(context: Context) {
        MobileAds.initialize(context)
    }

    // -----------------------------
    // Banner Ads
    // -----------------------------
    fun loadBanner(adView: AdView) {
        val request = AdRequest.Builder().build()
        adView.loadAd(request)
    }

    // -----------------------------
    // Rewarded Ads — Load both for 2-Ad Chain
    // Improved: make ad #1 load quickly and mark UI clickable; ad #2 loads afterwards.
    // If ad #2 fails to load, we prefer to attempt loading it while ad1 is playing and wait a bit.
    // -----------------------------
    fun preloadDoubleRewarded(context: Context) {
        // Reset double-ready state but keep isRewardedLoaded false until ad1 loads
        isDoubleRewardReady.value = false
        isRewardedLoaded.value = false

        val request = AdRequest.Builder().build()

        // Load Ad #1
        RewardedAd.load(
            context,
            rewardedAdUnitId,
            request,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd1 = ad
                    isRewardedLoaded.value = true
                    Log.d(TAG, "RewardedAd #1 loaded (at least one ready)")

                    // Load Ad #2 ONLY after #1 is loaded
                    loadSecondRewarded(context)
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    rewardedAd1 = null
                    rewardedAd2 = null
                    isRewardedLoaded.value = false
                    isDoubleRewardReady.value = false
                    Log.e(TAG, "RewardedAd #1 failed to load: ${error.message}")
                }
            }
        )
    }

    private fun loadSecondRewarded(context: Context) {
        val request = AdRequest.Builder().build()

        RewardedAd.load(
            context,
            rewardedAdUnitId,
            request,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd2 = ad
                    isDoubleRewardReady.value = true
                    // Keep isRewardedLoaded true (ad1 is also available)
                    isRewardedLoaded.value = true
                    Log.d(TAG, "RewardedAd #2 loaded — DOUBLE ADS READY")
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    // If second ad fails, we still keep ad1 if present so users can watch at least one ad
                    rewardedAd2 = null
                    isDoubleRewardReady.value = false
                    // Don't flip isRewardedLoaded to false here — ad1 might still be present
                    Log.e(TAG, "RewardedAd #2 failed to load: ${error.message}")
                }
            }
        )
    }

    // ---------------------------------------------------
    // SHOW DOUBLE REWARDED (User must watch BOTH ads)
    // Behavior: if both ads are loaded, play ad1 then ad2 immediately.
    // If only ad1 is loaded, start loading ad2 immediately while user watches ad1,
    // and wait up to AD2_WAIT_MAX_MS (polling) after ad1 completion for ad2 to be loaded.
    // If ad2 loads in that window, show it and then call onCompletedBoth.
    // If ad2 does not load in that window, we call onFailed.
    // ---------------------------------------------------
    fun showDoubleRewarded(
        activity: Activity,
        onCompletedBoth: () -> Unit,
        onFailed: (() -> Unit)? = null
    ) {
        val ad1 = rewardedAd1
        val ad2 = rewardedAd2

        if (ad1 == null) {
            Log.e(TAG, "No rewarded ad loaded (ad1 missing)")
            onFailed?.invoke()
            return
        }

        // If both ready, play both immediately
        if (ad2 != null) {
            Log.d(TAG, "Playing double rewarded chain: ad1 -> ad2")

            ad1.show(activity) {
                Log.d(TAG, "User finished rewarded #1")

                ad2.show(activity) {
                    Log.d(TAG, "User finished rewarded #2 — REWARD NOW (double)")
                    try {
                        onCompletedBoth()
                    } catch (t: Throwable) {
                        Log.w(TAG, "onCompletedBoth threw", t)
                    }
                }
            }

            // Reset state and preload again
            rewardedAd1 = null
            rewardedAd2 = null
            isRewardedLoaded.value = false
            isDoubleRewardReady.value = false
            preloadDoubleRewarded(activity)

            return
        }

        // Only ad1 present: attempt to load ad2 while user watches ad1
        Log.d(TAG, "Ad1 available but ad2 not yet loaded — will attempt to load ad2 while user watches ad1")

        // Kick off ad2 load immediately
        loadSecondRewarded(activity)

        // Play ad1
        ad1.show(activity) {
            Log.d(TAG, "User finished rewarded #1 — waiting for ad2 to load up to ${AD2_WAIT_MAX_MS}ms")

            // Poll for rewardedAd2 up to AD2_WAIT_MAX_MS
            val start = System.currentTimeMillis()

            val checkRunnable = object : Runnable {
                override fun run() {
                    val current = rewardedAd2
                    if (current != null) {
                        Log.d(TAG, "Ad2 became available after ad1; proceeding to show ad2")
                        current.show(activity) {
                            Log.d(TAG, "User finished rewarded #2 — REWARD NOW (double, late-loaded)")
                            try {
                                onCompletedBoth()
                            } catch (t: Throwable) {
                                Log.w(TAG, "onCompletedBoth threw", t)
                            }
                        }

                        // Reset and preload
                        rewardedAd1 = null
                        rewardedAd2 = null
                        isRewardedLoaded.value = false
                        isDoubleRewardReady.value = false
                        preloadDoubleRewarded(activity)

                        return
                    }

                    val elapsed = System.currentTimeMillis() - start
                    if (elapsed >= AD2_WAIT_MAX_MS) {
                        Log.e(TAG, "Ad2 did not load within ${AD2_WAIT_MAX_MS}ms after ad1 finished — failing double-ad flow")
                        onFailed?.invoke()

                        // Keep ad1 null/cleared and attempt to preload anew so user can try again
                        rewardedAd1 = null
                        rewardedAd2 = null
                        isRewardedLoaded.value = false
                        isDoubleRewardReady.value = false
                        preloadDoubleRewarded(activity)

                        return
                    }

                    // Not yet loaded, schedule another check
                    handler.postDelayed(this, AD2_POLL_INTERVAL_MS)
                }
            }

            handler.postDelayed(checkRunnable, AD2_POLL_INTERVAL_MS)
        }

        // We don't clear rewardedAd1 here — it will be cleared after flow finishes
    }

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
