package com.betterblocks.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import com.betterblocks.BuildConfig
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.rewarded.RewardItem
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

object AdManager {

    private const val TAG = "AdManager"

    private var rewardedAd: RewardedAd? = null
    private var zeroCoinsRewardedAd: RewardedAd? = null

    val isRewardedLoaded = mutableStateOf(false)

    val bannerAdUnitId: String
        get() = BuildConfig.BANNER_AD_UNIT_ID

    val rewardedAdUnitId: String
        get() = BuildConfig.REWARDED_AD_UNIT_ID

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
    // Rewarded Ads
    // -----------------------------
    fun loadRewarded(context: Context) {
        val request = AdRequest.Builder().build()

        RewardedAd.load(
            context,
            rewardedAdUnitId,
            request,
            object : RewardedAdLoadCallback() {

                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    isRewardedLoaded.value = true
                    Log.d(TAG, "Rewarded ad loaded")
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    rewardedAd = null
                    isRewardedLoaded.value = false
                    Log.e(TAG, "Failed to load rewarded: ${error.message}")
                }
            }
        )
    }

    fun preloadZeroCoinsRewarded(context: Context) {
        val request = AdRequest.Builder().build()

        RewardedAd.load(
            context,
            rewardedAdUnitId,
            request,
            object : RewardedAdLoadCallback() {

                override fun onAdLoaded(ad: RewardedAd) {
                    zeroCoinsRewardedAd = ad
                    Log.d(TAG, "Zero coins rewarded loaded")
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    zeroCoinsRewardedAd = null
                    Log.e(TAG, "Zero coins rewarded failed: ${error.message}")
                }
            }
        )
    }

    // -----------------------------
    // Show Rewarded
    // (old AdMob API)
    // -----------------------------
    fun showRewarded(activity: Activity, onEarned: (RewardItem) -> Unit) {
        val ad = rewardedAd ?: run {
            Log.e(TAG, "Rewarded ad not loaded")
            return
        }

        ad.show(activity) { rewardItem ->
            onEarned(rewardItem)
        }

        rewardedAd = null
        isRewardedLoaded.value = false

        loadRewarded(activity)
    }

    // -----------------------------
    // Show Zero Coins Rewarded
    // -----------------------------
    fun showZeroCoinsRewarded(
        activity: Activity,
        onEarned: (RewardItem) -> Unit,
        onFailed: (() -> Unit)? = null
    ) {
        val ad = zeroCoinsRewardedAd ?: run {
            onFailed?.invoke()
            Log.e(TAG, "Zero coins rewarded not loaded")
            return
        }

        ad.show(activity) { reward ->
            onEarned(reward)
        }

        zeroCoinsRewardedAd = null

        preloadZeroCoinsRewarded(activity)
    }
}
