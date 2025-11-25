package com.betterblocks.ads

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdView

@Composable
fun BannerAdView(modifier: Modifier = Modifier) {
    AndroidView(
        factory = { context ->
            AdView(context).apply {
                AdManager.loadBanner(this)
            }
        },
        update = { adView ->
            AdManager.loadBanner(adView)
        },
        modifier = modifier
    )
}

