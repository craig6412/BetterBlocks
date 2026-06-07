package com.betterblocks.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import android.app.Activity
import android.util.Log
import com.betterblocks.BuildConfig
import com.betterblocks.DeepBlue
import com.betterblocks.DeviceClass
import com.betterblocks.GameUiState
import com.betterblocks.GameViewModel
import com.betterblocks.LightText
import com.betterblocks.Oswald
import com.betterblocks.Pink_Jackie
import com.betterblocks.PreviewGameViewModel
import com.betterblocks.R
import com.betterblocks.SpecialPurple
import com.betterblocks.SuccessGreen
import com.betterblocks.ads.AdManager
import com.betterblocks.trophyColorForTier
import com.betterblocks.trophyDisplayName
import com.betterblocks.trophyRes
import com.betterblocks.ui.sdp
import com.betterblocks.ui.sh
import com.betterblocks.ui.ssp
import com.betterblocks.ui.sw
import kotlinx.coroutines.delay

// Gradient colors for background
val BannerBlueTop = Color(0xFF011133)
val BannerBlueBottom = Color(0xFF021B4F)

// Small typed callback holder to avoid reflection in previews
data class ViewModelCallbacks(
    val claimDailyReward: () -> Unit = {},
    val addCoins: (Int) -> Unit = { _ -> },
    val dismissZeroCoins: () -> Unit = {},
    val checkDailyReward: () -> Unit = {}
)

// -----------------------
// Device helpers
// Use the shared `DeviceClass` enum declared in GameScreen.kt (same package).
// -----------------------

private fun classifyDevice(widthDp: Dp, heightDp: Dp): DeviceClass {
    val smallest = minOf(widthDp, heightDp)
    val largest = maxOf(widthDp, heightDp)
    val aspect = largest / smallest

    return when {
        smallest >= 600.dp && aspect < 1.35f -> DeviceClass.Foldable
        smallest >= 600.dp -> DeviceClass.Tablet
        else -> DeviceClass.Phone
    }
}

@Composable
private fun deviceCategory(forcePreviewFold: Boolean = false): DeviceClass {
    if (forcePreviewFold) return DeviceClass.Foldable
    val width = sw(1f)
    val height = sh(1f)
    return classifyDevice(width, height)
}

@Composable
fun PowerupHeader(uiState: GameUiState, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = sw(0.03f), vertical = sh(0.004f))
            .width(sw(0.9f)),
        shape = RoundedCornerShape(sdp(0.02f)),
        colors = CardDefaults.cardColors(
            containerColor = DeepBlue.copy(alpha = 0.9f)
        ),
        elevation = CardDefaults.cardElevation(sdp(0.006f)),
        border = BorderStroke(sdp(0.0015f), Pink_Jackie.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            DeepBlue.copy(alpha = 0.8f),
                            DeepBlue.copy(alpha = 1f),
                            DeepBlue.copy(alpha = 0.8f)
                        )
                    )
                )
                .padding(horizontal = sw(0.03f), vertical = sh(0.01f)),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatItem(
                emoji = "💰",
                value = uiState.coins.toString(),
                backgroundColor = Pink_Jackie.copy(alpha = 0.2f)
            )

            StatItem(
                emoji = "🌈",
                value = uiState.rainbowBlockCount.toString(),
                backgroundColor = SpecialPurple.copy(alpha = 0.2f)
            )

            Row(
                modifier = Modifier
                    .background(
                        color = SuccessGreen.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(sdp(0.01f))
                    )
                    .padding(horizontal = sdp(0.02f), vertical = sdp(0.01f)),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_palette_colorwipe),
                    contentDescription = "Color Wipe",
                    tint = Color.Unspecified,
                    modifier = Modifier.size(sdp(0.03f))
                )
                Spacer(Modifier.width(sdp(0.005f)))
                Text(
                    text = uiState.colorWipeCount.toString(),
                    color = Color.White,
                    fontSize = ssp(0.02f),
                    fontWeight = FontWeight.Bold,
                    fontFamily = Oswald
                )
            }
        }
    }
}

@Composable
fun StatItem(emoji: String, value: String, backgroundColor: Color) {
    Row(
        modifier = Modifier
            .background(
                color = backgroundColor,
                shape = RoundedCornerShape(sdp(0.01f))
            )
            .padding(horizontal = sdp(0.02f), vertical = sdp(0.01f)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text = emoji,
            fontSize = ssp(0.018f)
        )
        Spacer(Modifier.width(sdp(0.005f)))
        Text(
            text = value,
            color = Color.White,
            fontSize = ssp(0.02f),
            fontWeight = FontWeight.Bold,
            fontFamily = Oswald
        )
    }
}

/**
 * Concept-style hero card:
 * Left side = current trophy. Right side = full clickable Free Coins rewarded-ad CTA.
 */
@Composable
fun MainMenuHeroCard(
    uiState: GameUiState,
    viewModelBackedCallbacks: ViewModelCallbacks,
    onGoToShop: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ctx = LocalContext.current
    val activity = remember(ctx) { ctx as? Activity }
    val rewardedLoaded = AdManager.isRewardedLoaded.value

    var isLoadingAd by remember { mutableStateOf(false) }
    var showAdUnavailableDialog by remember { mutableStateOf(false) }
    var loadRequestId by remember { mutableStateOf(0) }

    val tier = uiState.trophyTier
    val tierColor = trophyColorForTier(tier)

    fun awardFreeCoins() {
        try {
            viewModelBackedCallbacks.addCoins(AdManager.REWARDED_COINS)
            Log.d("MainMenuHeroCard", "Reward completed: added ${AdManager.REWARDED_COINS} coins")
        } catch (t: Throwable) {
            Log.w("MainMenuHeroCard", "Failed to add rewarded coins", t)
        }
    }

    fun showRewardedOrFail(act: Activity) {
        isLoadingAd = false
        AdManager.showRewarded(
            act,
            onRewardEarned = { awardFreeCoins() },
            onFailed = {
                isLoadingAd = false
                showAdUnavailableDialog = true
                AdManager.preloadRewarded(ctx)
            }
        )
    }

    fun requestReward() {
        val act = activity
        if (act == null) {
            AdManager.preloadRewarded(ctx)
            showAdUnavailableDialog = true
            return
        }

        showAdUnavailableDialog = false

        if (AdManager.isRewardedLoaded.value) {
            showRewardedOrFail(act)
        } else {
            isLoadingAd = true
            val requestId = loadRequestId + 1
            loadRequestId = requestId

            AdManager.preloadRewarded(
                ctx,
                onLoaded = {
                    if (isLoadingAd && loadRequestId == requestId) {
                        showRewardedOrFail(act)
                    }
                },
                onFailed = {
                    if (loadRequestId == requestId) {
                        isLoadingAd = false
                        showAdUnavailableDialog = true
                    }
                }
            )
        }
    }

    LaunchedEffect(isLoadingAd, rewardedLoaded) {
        if (isLoadingAd && rewardedLoaded) {
            activity?.let { showRewardedOrFail(it) }
        }
    }

    LaunchedEffect(isLoadingAd, loadRequestId) {
        if (isLoadingAd) {
            delay(12_000L)
            if (isLoadingAd) {
                isLoadingAd = false
                showAdUnavailableDialog = true
                AdManager.preloadRewarded(ctx)
            }
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(sh(0.102f)),
        shape = RoundedCornerShape(sdp(0.024f)),
        colors = CardDefaults.cardColors(
            containerColor = DeepBlue.copy(alpha = 0.94f)
        ),
        elevation = CardDefaults.cardElevation(sdp(0.01f)),
        border = BorderStroke(sdp(0.0022f), Color(0xFF7C4DFF).copy(alpha = 0.9f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            tierColor.copy(alpha = 0.18f),
                            DeepBlue.copy(alpha = 0.98f),
                            Color(0xFF160A3D).copy(alpha = 0.96f),
                            Pink_Jackie.copy(alpha = 0.13f)
                        )
                    )
                )
                .padding(horizontal = sw(0.028f), vertical = sh(0.007f)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // LEFT SIDE: current trophy progression
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(sdp(0.064f))
                        .background(
                            color = tierColor.copy(alpha = 0.18f),
                            shape = RoundedCornerShape(sdp(0.016f))
                        )
                        .border(
                            BorderStroke(sdp(0.0015f), tierColor.copy(alpha = 0.65f)),
                            shape = RoundedCornerShape(sdp(0.016f))
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = trophyRes(tier)),
                        contentDescription = trophyDisplayName(tier),
                        modifier = Modifier.size(sdp(0.047f)),
                        contentScale = ContentScale.Fit
                    )
                }

                Spacer(modifier = Modifier.width(sw(0.018f)))

                Column(
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "CURRENT TROPHY",
                        color = LightText.copy(alpha = 0.68f),
                        fontSize = ssp(0.0115f),
                        fontWeight = FontWeight.Bold,
                        fontFamily = Oswald,
                        letterSpacing = ssp(0.0015f)
                    )
                    Spacer(modifier = Modifier.height(sh(0.001f)))
                    Text(
                        text = trophyDisplayName(tier).uppercase(),
                        color = tierColor,
                        fontSize = ssp(0.019f),
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = Oswald,
                        maxLines = 1
                    )
                }
            }

            // Center neon divider
            Box(
                modifier = Modifier
                    .fillMaxHeight(0.68f)
                    .width(sdp(0.0022f))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color(0xFF7C4DFF).copy(alpha = 0.95f),
                                Pink_Jackie.copy(alpha = 0.8f),
                                Color.Transparent
                            )
                        )
                    )
            )

            // RIGHT SIDE: full-panel clickable rewarded-ad CTA
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable(
                        enabled = !isLoadingAd,
                        indication = LocalIndication.current,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { requestReward() },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.free_coins),
                        contentDescription = "Free Coins",
                        modifier = Modifier.size(sdp(0.052f)),
                        contentScale = ContentScale.Fit
                    )

                    Spacer(modifier = Modifier.width(sw(0.014f)))

                    Column(
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(
                            text = "FREE COINS",
                            color = Pink_Jackie,
                            fontSize = ssp(0.019f),
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = Oswald,
                            letterSpacing = ssp(0.0015f)
                        )
                        Text(
                            text = if (isLoadingAd) "LOADING AD..." else "WATCH AD",
                            color = LightText.copy(alpha = 0.78f),
                            fontSize = ssp(0.0115f),
                            fontWeight = FontWeight.Bold,
                            fontFamily = Oswald
                        )
                        Text(
                            text = "+${AdManager.REWARDED_COINS}",
                            color = SuccessGreen,
                            fontSize = ssp(0.021f),
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = Oswald
                        )
                    }
                }
            }
        }
    }

    if (isLoadingAd) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Loading Reward") },
            text = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(28.dp))
                    Text("Getting your free coins video ready...")
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(
                    onClick = {
                        isLoadingAd = false
                        AdManager.preloadRewarded(ctx)
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showAdUnavailableDialog) {
        AlertDialog(
            onDismissRequest = { showAdUnavailableDialog = false },
            title = { Text("Reward unavailable") },
            text = { Text("No reward video is ready right now. Try again in a moment, or visit the shop.") },
            confirmButton = {
                TextButton(onClick = { showAdUnavailableDialog = false }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showAdUnavailableDialog = false
                        onGoToShop()
                    }
                ) {
                    Text("Shop")
                }
            }
        )
    }
}

@Composable
fun MainMenuScreen(
    viewModel: GameViewModel,
    onPlayClicked: () -> Unit,
    onShopClicked: () -> Unit,
    onHighScoresClicked: () -> Unit,
    onStatsClicked: () -> Unit,
    onSettingsClicked: () -> Unit,
    onDeveloperClicked: () -> Unit,
    banner: @Composable (() -> Unit)? = null
) {
    val uiState = viewModel.uiState.collectAsState().value
    MainMenuScreenContent(
        uiState = uiState,
        onPlayClicked = onPlayClicked,
        onShopClicked = onShopClicked,
        onHighScoresClicked = onHighScoresClicked,
        onStatsClicked = onStatsClicked,
        onSettingsClicked = onSettingsClicked,
        onDeveloperClicked = onDeveloperClicked,
        banner = banner,
        viewModelBackedCallbacks = ViewModelCallbacks(
            claimDailyReward = { viewModel.claimDailyReward() },
            addCoins = { amt -> viewModel.addCoins(amt) },
            dismissZeroCoins = { viewModel.dismissZeroCoinsDialog() },
            checkDailyReward = { viewModel.checkDailyReward() }
        )
    )
}

// Overload used by previews with PreviewGameViewModel
@Composable
fun MainMenuScreen(
    viewModel: PreviewGameViewModel,
    onPlayClicked: () -> Unit,
    onShopClicked: () -> Unit,
    onHighScoresClicked: () -> Unit,
    onStatsClicked: () -> Unit,
    onSettingsClicked: () -> Unit,
    onDeveloperClicked: () -> Unit,
    banner: @Composable (() -> Unit)? = null
) {
    val uiState = viewModel.uiState.collectAsState().value
    MainMenuScreenContent(
        uiState = uiState,
        onPlayClicked = onPlayClicked,
        onShopClicked = onShopClicked,
        onHighScoresClicked = onHighScoresClicked,
        onStatsClicked = onStatsClicked,
        onSettingsClicked = onSettingsClicked,
        onDeveloperClicked = onDeveloperClicked,
        banner = banner,
        viewModelBackedCallbacks = ViewModelCallbacks(
            claimDailyReward = { viewModel.claimDailyReward() },
            addCoins = { amt -> viewModel.addCoins(amt) },
            dismissZeroCoins = { viewModel.dismissZeroCoinsDialog() },
            checkDailyReward = { viewModel.checkDailyReward() }
        )
    )
}

// Pure UI function that does not touch Android APIs directly — accepts UI state and callbacks.
@Composable
private fun MainMenuScreenContent(
    uiState: GameUiState,
    onPlayClicked: () -> Unit,
    onShopClicked: () -> Unit,
    onHighScoresClicked: () -> Unit,
    onStatsClicked: () -> Unit,
    onSettingsClicked: () -> Unit,
    onDeveloperClicked: () -> Unit,
    banner: @Composable (() -> Unit)? = null,
    viewModelBackedCallbacks: ViewModelCallbacks
) {
    val context = LocalContext.current

    // Check daily reward on first composition
    LaunchedEffect(Unit) {
        try {
            viewModelBackedCallbacks.checkDailyReward()
        } catch (_: Throwable) {
        }
    }

    // Preload rewarded ads proactively so the Free Coins hero panel can show immediately
    LaunchedEffect(Unit) {
        AdManager.preloadRewarded(context)
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(BannerBlueTop, BannerBlueBottom)
                        )
                    )
                    .padding(WindowInsets.systemBars.asPaddingValues())
                    .padding(horizontal = sdp(0.03f)),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(sh(0.001f)))

                PowerupHeader(
                    uiState = uiState,
                    modifier = Modifier.padding(horizontal = 0.dp)
                )

                Spacer(modifier = Modifier.height(sh(0.008f)))

                MainMenuHeroCard(
                    uiState = uiState,
                    viewModelBackedCallbacks = viewModelBackedCallbacks,
                    onGoToShop = onShopClicked,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = sw(0.01f))
                )

                Spacer(modifier = Modifier.height(sh(0.006f)))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(sh(0.245f)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.banner),
                        contentDescription = "Better Blocks Game Title",
                        modifier = Modifier
                            .fillMaxWidth(0.78f)
                            .aspectRatio(1.5f),
                        contentScale = ContentScale.Fit
                    )
                }

                Spacer(modifier = Modifier.height(sh(0.006f)))

                MenuButton(
                    text = "PLAY",
                    icon = Icons.Default.PlayArrow,
                    onClick = onPlayClicked,
                    containerColor = Color(0xFF673AB7),
                    contentColor = Color.White,
                    height = sh(0.064f)
                )

                Spacer(modifier = Modifier.height(sh(0.009f)))

                MenuButton(
                    text = "SHOP",
                    icon = Icons.Default.ShoppingCart,
                    onClick = onShopClicked,
                    containerColor = DeepBlue,
                    border = BorderStroke(sdp(0.003f), Color(0xFF673AB7))
                )

                Spacer(modifier = Modifier.height(sh(0.009f)))

                Button(
                    onClick = onHighScoresClicked,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(sh(0.058f)),
                    shape = RoundedCornerShape(sdp(0.02f)),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DeepBlue,
                        contentColor = LightText
                    ),
                    border = BorderStroke(sdp(0.003f), Color(0xFF673AB7)),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = sdp(0.008f),
                        pressedElevation = sdp(0.0025f)
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Image(
                            painter = painterResource(id = trophyRes(uiState.trophyTier)),
                            contentDescription = null,
                            modifier = Modifier.size(sdp(0.035f)),
                            contentScale = ContentScale.Fit
                        )
                        Spacer(modifier = Modifier.width(sdp(0.015f)))
                        Text(
                            text = "HIGH SCORES",
                            fontSize = ssp(0.025f),
                            fontWeight = FontWeight.Bold,
                            fontFamily = Oswald,
                            letterSpacing = ssp(0.0015f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(sh(0.009f)))

                MenuButton(
                    text = "STATS",
                    icon = Icons.Default.Assessment,
                    onClick = onStatsClicked,
                    containerColor = DeepBlue,
                    border = BorderStroke(sdp(0.003f), Color(0xFF673AB7))
                )

                Spacer(modifier = Modifier.height(sh(0.009f)))

                MenuButton(
                    text = "SETTINGS",
                    icon = Icons.Default.Settings,
                    onClick = onSettingsClicked,
                    containerColor = DeepBlue,
                    border = BorderStroke(sdp(0.003f), Color(0xFF673AB7))
                )

                Spacer(modifier = Modifier.height(sh(0.009f)))

                if (BuildConfig.DEBUG) {
                    MenuButton(
                        text = "DEVELOPER",
                        icon = Icons.Default.Build,
                        onClick = onDeveloperClicked,
                        containerColor = Color(0xFF607D8B),
                        height = sh(0.04f)
                    )
                }

                Spacer(modifier = Modifier.height(sh(0.004f)))

                if (!LocalInspectionMode.current && BuildConfig.DEBUG) {
                    Text("Test Ad", color = Color.Gray, fontSize = ssp(0.010f))
                }

                banner?.let {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(sh(0.052f)),
                        contentAlignment = Alignment.Center
                    ) {
                        it.invoke()
                    }
                }

                Text(
                    text = "v2.7",
                    color = Color.Gray,
                    fontSize = ssp(0.012f),
                    fontFamily = Oswald
                )

                val suppressOtherDialogs = false

                if (!LocalInspectionMode.current && uiState.showDailyRewardDialog && !suppressOtherDialogs) {
                    DailyRewardDialog(
                        day = uiState.dailyRewardDay,
                        streak = uiState.dailyRewardStreak,
                        coins = uiState.dailyRewardCoins,
                        hasRainbowWipe = uiState.dailyRewardRainbow,
                        onClaimReward = {
                            try {
                                viewModelBackedCallbacks.claimDailyReward()
                            } catch (_: Throwable) {
                            }
                        }
                    )
                }
            }
        }
    }
}

// ----------------------
// Menu Button Composable
// ----------------------
@Composable
fun MenuButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    containerColor: Color,
    contentColor: Color = LightText,
    height: Dp = 54.dp,
    border: BorderStroke? = null
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(height),
        shape = RoundedCornerShape(sdp(0.02f)),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = sdp(0.008f),
            pressedElevation = sdp(0.0025f)
        ),
        border = border
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(sdp(0.035f))
            )
            Spacer(modifier = Modifier.width(sdp(0.015f)))
            Text(
                text = text,
                fontSize = ssp(0.023f),
                fontWeight = FontWeight.Bold,
                fontFamily = Oswald,
                letterSpacing = ssp(0.0015f)
            )
        }
    }
}

@Preview(
    name = "Tablet – Portrait",
    showBackground = true,
    showSystemUi = true,
    device = "spec:width=800dp,height=1280dp,dpi=480"
)

// need to check that the main menu is structured in a way that it scales correctly across all devices.
//also
@Composable
fun MainMenuScreenPreview() {
    val vm = PreviewGameViewModel()
    MainMenuScreen(
        viewModel = vm,
        onPlayClicked = {},
        onShopClicked = {},
        onHighScoresClicked = {},
        onStatsClicked = {},
        onSettingsClicked = {},
        onDeveloperClicked = {}
    )
}
 