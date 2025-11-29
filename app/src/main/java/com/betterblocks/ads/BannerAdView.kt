package com.betterblocks.ads

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdRequest

@Composable
fun BannerAdView(modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            AdView(context).apply {
                adUnitId = AdManager.bannerAdUnitId
                setAdSize(AdSize.BANNER)

                // Load only once on create
                loadAd(AdRequest.Builder().build())
            }
        },
        update = { view ->
            // Do NOT set size again — only reload ad safely
            view.loadAd(AdRequest.Builder().build())
        }
    )
}
