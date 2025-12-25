package com.betterblocks.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
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
import com.betterblocks.ui.sdp
import com.betterblocks.ui.ssp
import com.betterblocks.ui.sw
import com.betterblocks.ui.sh
import android.app.Activity
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.border
import android.util.Log
import com.betterblocks.R
import com.betterblocks.GameUiState
import com.betterblocks.GameViewModel
import com.betterblocks.BuildConfig
import com.betterblocks.DeepBlue
import com.betterblocks.LightText
import com.betterblocks.Oswald
import com.betterblocks.Pink_Jackie
import androidx.compose.ui.zIndex
import com.betterblocks.SpecialPurple
import com.betterblocks.SuccessGreen
import com.betterblocks.PreviewGameViewModel
import com.betterblocks.ads.AdManager
import com.betterblocks.PREFS_NAME
import com.betterblocks.KEY_PLAYER_NAME
import com.betterblocks.KEY_PLAYER_NAME_PROMPTED
import com.betterblocks.KEY_FIREBASE_USER_ID
import com.betterblocks.KEY_HIGH_SCORE
import com.betterblocks.KEY_LIFETIME_COINS
import com.betterblocks.PlayerNameDialog
import com.betterblocks.DeviceClass


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
// Device helpers + FreeCoinsButton
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
fun FreeCoinsButton(
    viewModelBackedCallbacks: ViewModelCallbacks,
    onGoToShop: () -> Unit,
    forcePreviewFold: Boolean = false
) {
    val ctx = LocalContext.current
    val category = deviceCategory(forcePreviewFold)

    val sizeDp = when (category) {
        DeviceClass.Phone -> sw(0.14f)
        DeviceClass.Tablet -> sw(0.11f)
        DeviceClass.Foldable -> sw(0.16f)
    }
    val topPad = when (category) {
        DeviceClass.Phone -> sh(0.02f)
        DeviceClass.Tablet -> sh(0.035f)
        DeviceClass.Foldable -> sh(0.02f)
    }
    val endPad = when (category) {
        DeviceClass.Phone -> sw(0.03f)
        DeviceClass.Tablet -> sw(0.04f)
        DeviceClass.Foldable -> sw(0.03f)
    }

    // Move down by an additional 10% of the screen height to avoid overlap with top UI
    val extraVerticalOffset = sh(0.10f)

    Box(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(top = topPad + extraVerticalOffset, end = endPad)
            .size(sizeDp)
            .border(width = 1.dp, color = Color.Red, shape = RoundedCornerShape(4.dp)) // DEBUG border
            .zIndex(1000f)
        ) {
            Log.d("FreeCoinsButton", "composed: category=$category size=$sizeDp topPad=${topPad + extraVerticalOffset} endPad=$endPad")
            val activity = remember { (ctx as? Activity) }
            Image(
                painter = painterResource(id = R.drawable.free_coins),
                contentDescription = "Free Coins",
                modifier = Modifier
                    .size(sizeDp)
                    .clickable(
                        indication = LocalIndication.current,
                        interactionSource = remember { MutableInteractionSource() }
                    ) {
                        val act = activity
                        if (act == null) {
                            // Preload for later if no activity available
                            com.betterblocks.ads.AdManager.preloadDoubleRewarded(ctx)
                            onGoToShop()
                            return@clickable
                        }

                        if (com.betterblocks.ads.AdManager.isRewardedLoaded.value) {
                            com.betterblocks.ads.AdManager.showDoubleRewarded(
                                act,
                                onCompletedBoth = { try { viewModelBackedCallbacks.addCoins(com.betterblocks.ads.AdManager.DOUBLE_REWARD_COINS) } catch (_: Throwable) {} },
                                onFailed = { com.betterblocks.ads.AdManager.preloadDoubleRewarded(ctx); onGoToShop() }
                            )
                        } else {
                            com.betterblocks.ads.AdManager.preloadDoubleRewarded(ctx)
                        }
                    }
            )
        }
    }
}

@Composable
fun PowerupHeader(uiState: GameUiState, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = sw(0.03f), vertical = sh(0.004f))
            .width(sw(0.9f)),
        shape = RoundedCornerShape(sdp(0.02f)), // Slightly smaller corners
        colors = CardDefaults.cardColors(
            containerColor = DeepBlue.copy(alpha = 0.9f)
        ),
        elevation = CardDefaults.cardElevation(sdp(0.006f)),
        border = BorderStroke(sdp(0.0015f), Pink_Jackie.copy(alpha = 0.3f)) // Thinner border
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
            // Coins
            StatItem(
                emoji = "💰",
                value = uiState.coins.toString(),
                backgroundColor = Pink_Jackie.copy(alpha = 0.2f)
            )

            // Rainbow Wipes
            StatItem(
                emoji = "🌈",
                value = uiState.rainbowBlockCount.toString(),
                backgroundColor = SpecialPurple.copy(alpha = 0.2f)
            )

            // Color Wipes
            Row(
                modifier = Modifier
                    .background(
                        color = SuccessGreen.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(sdp(0.01f)) // Smaller radius
                    )
                    .padding(horizontal = sdp(0.02f), vertical = sdp(0.01f)), // Reduced padding
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_palette_colorwipe),
                    contentDescription = "Color Wipe",
                    tint = Color.Unspecified,
                    modifier = Modifier.size(sdp(0.03f)) // Slightly smaller icon
                )
                Spacer(Modifier.width(sdp(0.005f)))
                Text(
                    text = uiState.colorWipeCount.toString(),
                    color = Color.White,
                    fontSize = ssp(0.02f), // Reduced font size
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
                shape = RoundedCornerShape(sdp(0.01f)) // Smaller radius
            )
            .padding(horizontal = sdp(0.02f), vertical = sdp(0.01f)), // Reduced padding
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text = emoji,
            fontSize = ssp(0.018f) // Reduced from 24sp
        )
        Spacer(Modifier.width(sdp(0.005f)))
        Text(
            text = value,
            color = Color.White,
            fontSize = ssp(0.02f), // Reduced from 20sp
            fontWeight = FontWeight.Bold,
            fontFamily = Oswald
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
    // Production entrypoint: read state from ViewModel and delegate to pure UI function
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

    var showPopup by remember {
        mutableStateOf(!hasShownPowerUpPopup(context))
    }

    // Check daily reward on first composition — call through the provided callbacks if available
    LaunchedEffect(Unit) {
        try {
            viewModelBackedCallbacks.checkDailyReward()
        } catch (_: Throwable) {}
    }

    // Preload double-rewarded ads proactively so the Free Coins flow can show immediately
    LaunchedEffect(Unit) {
        try {
            com.betterblocks.ads.AdManager.preloadDoubleRewarded(context)
        } catch (_: Throwable) {
            // ignore — preload is best-effort
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.Transparent
    ) {

        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            // Main content column
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
                // PowerupHeader at top with proper spacing
                Spacer(modifier = Modifier.height(sh(0.005f)))

                PowerupHeader(
                    uiState = uiState,
                    modifier = Modifier
                        .padding(horizontal = 0.dp)
                        .offset(
                            x = 0.dp,
                            y = sh(-0.01f)   // move header up by ~1% of screen height
                        )
                )

                Spacer(modifier = Modifier.height(sh(0.015f))) // Space between header and banner

                // Flexible space before banner
                Spacer(modifier = Modifier.weight(0.6f))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .zIndex(50f)
                ) {
                    // Make the banner much taller while preserving width so its visual height is ~3x
                    Image(
                        painter = painterResource(id = R.drawable.banner),
                        contentDescription = "Better Blocks Game Title",
                        modifier = Modifier
                            .fillMaxWidth(0.95f)
                            .aspectRatio(1.5f) // previously 6f (very wide/short); 2f increases banner height ~3x
                            .offset(x = sw(0.01f), y = sh(-0.01f))
                            .zIndex(50f),
                        contentScale = ContentScale.Fit
                    )
                }

                Spacer(modifier = Modifier.height(sh(0.03f)))

                MenuButton(
                    text = "PLAY",
                    icon = Icons.Default.PlayArrow,
                    onClick = onPlayClicked,
                    containerColor = Color(0xFF673AB7),
                    contentColor = Color.White,
                    height = sh(0.08f)
                )

                Spacer(modifier = Modifier.height(sh(0.02f)))

                MenuButton(
                    text = "SHOP",
                    icon = Icons.Default.ShoppingCart,
                    onClick = onShopClicked,
                    containerColor = DeepBlue,
                    border = BorderStroke(sdp(0.003f), Color(0xFF673AB7))
                )

                Spacer(modifier = Modifier.height(sh(0.018f)))

                MenuButton(
                    text = "HIGH SCORES",
                    icon = Icons.Default.EmojiEvents,
                    onClick = onHighScoresClicked,
                    containerColor = DeepBlue,
                    border = BorderStroke(sdp(0.003f), Color(0xFF673AB7))
                )

                Spacer(modifier = Modifier.height(sh(0.018f)))

                MenuButton(
                    text = "STATS",
                    icon = Icons.Default.Assessment,
                    onClick = onStatsClicked,
                    containerColor = DeepBlue,
                    border = BorderStroke(sdp(0.003f), Color(0xFF673AB7))
                )

                Spacer(modifier = Modifier.height(sh(0.018f)))

                MenuButton(
                    text = "SETTINGS",
                    icon = Icons.Default.Settings,
                    onClick = onSettingsClicked,
                    containerColor = DeepBlue,
                    border = BorderStroke(sdp(0.003f), Color(0xFF673AB7))
                )

                Spacer(modifier = Modifier.height(sh(0.018f)))

                // Only show developer entry in debug builds

                if (BuildConfig.DEBUG) {
                    MenuButton(
                        text = "DEVELOPER",
                        icon = Icons.Default.Build,
                        onClick = onDeveloperClicked,
                        containerColor = Color(0xFF607D8B),
                        height = sh(0.05f)
                    )
                }

                Spacer(modifier = Modifier.weight(0.8f))

                if (!LocalInspectionMode.current && BuildConfig.DEBUG) {
                    Text("Test Ad", color = Color.Gray, fontSize = ssp(0.012f))
                }

                Spacer(modifier = Modifier.height(sh(0.01f)))

                banner?.invoke()

                Spacer(modifier = Modifier.height(sh(0.01f)))
//version number location
                Text(
                    text = "v1.8",
                    color = Color.Gray,
                    fontSize = ssp(0.012f),
                    fontFamily = Oswald
                )

                if (!LocalInspectionMode.current && showPopup) {
                    PowerUpsPopup(
                        onDismiss = {
                            setPowerUpPopupShown(context)
                            showPopup = false
                        }
                    )
                }

                // Daily Reward Dialog
                if (!LocalInspectionMode.current && uiState.showDailyRewardDialog) {
                    DailyRewardDialog(
                        day = uiState.dailyRewardDay,
                        streak = uiState.dailyRewardStreak,
                        coins = uiState.dailyRewardCoins,
                        hasRainbowWipe = uiState.dailyRewardRainbow,
                        onClaimReward = { try { viewModelBackedCallbacks.claimDailyReward() } catch (_: Throwable) {} }
                    )
                }

                // Player Name Onboarding: show after daily reward completes or on first run
                val prefs = context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
                val savedPlayerName = prefs.getString(KEY_PLAYER_NAME, null)
                val prompted = prefs.getBoolean(KEY_PLAYER_NAME_PROMPTED, false)

                // If there's no saved name and we haven't prompted yet, show dialog after daily reward is not visible
                var showPlayerNameDialog by remember { mutableStateOf(false) }

                // Keep a snapshot of whether daily reward was showing so we can show name dialog after it finishes
                var dailyWasShowing by remember { mutableStateOf(uiState.showDailyRewardDialog) }
                LaunchedEffect(uiState.showDailyRewardDialog) {
                    // If daily dialog just finished (was showing and now not), and name not prompted, show name dialog
                    if (dailyWasShowing && !uiState.showDailyRewardDialog && !prompted && savedPlayerName.isNullOrBlank()) {
                        showPlayerNameDialog = true
                    }
                    dailyWasShowing = uiState.showDailyRewardDialog
                    // Also, if daily reward never needed and we haven't prompted, show immediately on first launch
                    if (!uiState.showDailyRewardDialog && !prompted && savedPlayerName.isNullOrBlank()) {
                        showPlayerNameDialog = true
                    }
                }

                if (!LocalInspectionMode.current && showPlayerNameDialog) {
                    PlayerNameDialog(
                        currentName = null,
                        onSave = { chosenName ->
                            val finalName = chosenName
                            prefs.edit().putString(KEY_PLAYER_NAME, finalName).putBoolean(KEY_PLAYER_NAME_PROMPTED, true).apply()
                            showPlayerNameDialog = false
                            // Optionally attempt leaderboard update if user id exists
                            val userId = prefs.getString(KEY_FIREBASE_USER_ID, null)
                            val currentScore = prefs.getInt(KEY_HIGH_SCORE, 0)
                            if (!userId.isNullOrBlank()) {
                                try {
                                    com.betterblocks.FirestoreManager.updateLeaderboard(userId = userId, score = currentScore, tier = com.betterblocks.model.getPlayerTier(currentScore, prefs.getInt(KEY_LIFETIME_COINS, 0), prefs), playerNameOverride = finalName)
                                } catch (_: Throwable) {}
                            }
                        },
                        onCancel = {
                            // Mark as prompted so we don't annoy the user again
                            prefs.edit().putBoolean(KEY_PLAYER_NAME_PROMPTED, true).apply()
                            showPlayerNameDialog = false
                        }
                    )
                }

                val config = LocalConfiguration.current
                val screenWidth = config.screenWidthDp.dp
                val screenHeight = config.screenHeightDp.dp

                // Free Coins floating overlay (device-aware placement)
            }

            // Place FreeCoinsButton as sibling to the main Column so it can align to the
            // outer full-screen Box and use TopEnd alignment reliably.
            FreeCoinsButton(
                viewModelBackedCallbacks = viewModelBackedCallbacks,
                onGoToShop = onShopClicked
            )
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
    height: Dp = 60.dp,
    border: BorderStroke? = null // New optional border parameter
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
        border = border // Apply the border if provided
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
                fontSize = ssp(0.025f),
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
