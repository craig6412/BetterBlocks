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

/**
 * Central AdMob controller that chooses the correct Ad Unit ID per build type.
 */
object AdManager {

    private const val RELEASE_BANNER_ID = "ca-app-pub-1555261975639574/3521751106"
    private const val RELEASE_REWARDED_ID = "ca-app-pub-1555261975639574/7839435910"

    private const val TEST_BANNER_ID = "ca-app-pub-3940256099942544/6300978111"
    private const val TEST_REWARDED_ID = "ca-app-pub-3940256099942544/5224354917"

    private const val TAG = "AdManager"

    private var rewardedAd: RewardedAd? = null

    val isRewardedLoaded = mutableStateOf(false)

    private fun getBannerId(): String =
        if (BuildConfig.DEBUG) TEST_BANNER_ID else RELEASE_BANNER_ID

    private fun getRewardedId(): String =
        if (BuildConfig.DEBUG) TEST_REWARDED_ID else RELEASE_REWARDED_ID

    fun initialize(context: Context) {
        MobileAds.initialize(context) {}
    }

    fun loadBanner(adView: AdView) {
        adView.adUnitId = getBannerId()
        adView.setAdSize(AdSize.BANNER)
        adView.loadAd(AdRequest.Builder().build())
    }

    fun loadRewarded(context: Context) {
        val adId = getRewardedId()

        RewardedAd.load(
            context,
            adId,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    isRewardedLoaded.value = true
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.e(TAG, "Rewarded failed: ${error.message}")
                    rewardedAd = null
                    isRewardedLoaded.value = false
                }
            }
        )
    }

    fun showRewarded(activity: Activity, onEarned: (RewardItem) -> Unit) {
        rewardedAd?.show(activity) { rewardItem ->
            onEarned(rewardItem)
            isRewardedLoaded.value = false
            loadRewarded(activity)
        } ?: run {
            Log.e(TAG, "Rewarded ad not loaded")
        }
    }
}
