package com.betterblocks.ads

import android.app.Activity
import android.content.Context
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

    // Rewarded #1 and Rewarded #2 for double-chain
    private var rewardedAd1: RewardedAd? = null
    private var rewardedAd2: RewardedAd? = null

    // Track loaded state for UI
    val isRewardedLoaded = mutableStateOf(false)

    // Interstitial (for every-other-game-over logic)
    private var interstitialAd: InterstitialAd? = null
    private var gameOverCounter = 0

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
    // -----------------------------
    fun preloadDoubleRewarded(context: Context) {
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
                    Log.d(TAG, "RewardedAd #1 loaded")

                    // Load Ad #2 ONLY after #1 is loaded
                    loadSecondRewarded(context)
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    rewardedAd1 = null
                    rewardedAd2 = null
                    isRewardedLoaded.value = false
                    Log.e(TAG, "RewardedAd #1 failed: ${error.message}")
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
                    isRewardedLoaded.value = true
                    Log.d(TAG, "RewardedAd #2 loaded — DOUBLE ADS READY")
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    rewardedAd2 = null
                    isRewardedLoaded.value = false
                    Log.e(TAG, "RewardedAd #2 failed: ${error.message}")
                }
            }
        )
    }

    // ---------------------------------------------------
    // SHOW DOUBLE REWARDED (User must watch BOTH ads)
    // ---------------------------------------------------
    fun showDoubleRewarded(
        activity: Activity,
        onCompletedBoth: () -> Unit,
        onFailed: (() -> Unit)? = null
    ) {
        val ad1 = rewardedAd1
        val ad2 = rewardedAd2

        if (ad1 == null || ad2 == null) {
            Log.e(TAG, "Double rewarded not ready")
            onFailed?.invoke()
            return
        }

        // Play Ad #1
        ad1.show(activity) {
            Log.d(TAG, "User finished rewarded #1")

            // After rewarded #1, play rewarded #2:
            ad2.show(activity) {
                Log.d(TAG, "User finished rewarded #2 — REWARD NOW")
                onCompletedBoth()
            }
        }

        // Reset state and preload again
        rewardedAd1 = null
        rewardedAd2 = null
        isRewardedLoaded.value = false
        preloadDoubleRewarded(activity)
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
