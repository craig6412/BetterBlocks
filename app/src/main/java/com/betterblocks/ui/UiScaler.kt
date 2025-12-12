package com.betterblocks.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun sw(percent: Float): Dp {
    val w = LocalConfiguration.current.screenWidthDp
    return (w * percent).dp
}

@Composable
fun sh(percent: Float): Dp {
    val h = LocalConfiguration.current.screenHeightDp
    return (h * percent).dp
}

@Composable
fun sdp(percent: Float): Dp = sw(percent)

@Composable
fun ssp(percent: Float): TextUnit {
    val w = LocalConfiguration.current.screenWidthDp
    return (w * percent).sp
}

