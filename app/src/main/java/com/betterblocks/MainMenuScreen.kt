package com.betterblocks.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.betterblocks.R
import com.betterblocks.GameSettings
import com.betterblocks.GameUiState
import com.betterblocks.GameViewModel
import com.betterblocks.BuildConfig
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.zIndex




// Gradient colors for background
val BannerBlueTop = Color(0xFF2A5092)
val BannerBlueBottom = Color(0xFF1E3A71)


@Composable
fun PowerupHeader(uiState: GameUiState, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp) // Reduced padding significantly
            .scale(0.85f), // Scale down to 85% of original size
        shape = RoundedCornerShape(12.dp), // Slightly smaller corners
        colors = CardDefaults.cardColors(
            containerColor = DeepBlue.copy(alpha = 0.9f)
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 6.dp
        ),
        border = BorderStroke(1.5.dp, CoinGold.copy(alpha = 0.3f)) // Thinner border
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
                .padding(horizontal = 12.dp, vertical = 8.dp), // Reduced internal padding
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Coins
            StatItem(
                emoji = "💰",
                value = uiState.coins.toString(),
                backgroundColor = CoinGold.copy(alpha = 0.2f)
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
                        shape = RoundedCornerShape(8.dp) // Smaller radius
                    )
                    .padding(horizontal = 10.dp, vertical = 6.dp), // Reduced padding
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_palette_colorwipe),
                    contentDescription = "Color Wipe",
                    tint = Color.Unspecified,
                    modifier = Modifier.size(22.dp) // Slightly smaller icon
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = uiState.colorWipeCount.toString(),
                    color = Color.White,
                    fontSize = 17.sp, // Reduced font size
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
                shape = RoundedCornerShape(8.dp) // Smaller radius
            )
            .padding(horizontal = 10.dp, vertical = 6.dp), // Reduced padding
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text = emoji,
            fontSize = 18.sp // Reduced from 24sp
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = value,
            color = Color.White,
            fontSize = 17.sp, // Reduced from 20sp
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
    val context = LocalContext.current
    val uiState = viewModel.uiState.collectAsState().value

    var showPopup by remember {
        mutableStateOf(!hasShownPowerUpPopup(context))
    }

    // Check daily reward on first composition
    LaunchedEffect(Unit) {
        viewModel.checkDailyReward()
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.Transparent
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
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // PowerupHeader at top with proper spacing
            Spacer(modifier = Modifier.height(4.dp))

            PowerupHeader(
                uiState = uiState,
                modifier = Modifier.padding(horizontal = 0.dp)
            )

            Spacer(modifier = Modifier.height(12.dp)) // Space between header and banner

            // Flexible space before banner
            Spacer(modifier = Modifier.weight(0.6f))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .zIndex(50f)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.banner),
                    contentDescription = "Better Blocks Game Title",
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .aspectRatio(3.2f)
                        .scale(GameSettings.bannerScale.value)
                        .zIndex(50f),
                    contentScale = ContentScale.Fit
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            MenuButton(
                text = "PLAY",
                icon = Icons.Default.PlayArrow,
                onClick = onPlayClicked,
                containerColor = Color(0xFF4CAF50),
                contentColor = Color.White,
                height = 80.dp
            )

            Spacer(modifier = Modifier.height(20.dp))

            MenuButton(
                text = "SHOP",
                icon = Icons.Default.ShoppingCart,
                onClick = onShopClicked,
                containerColor = DeepBlue
            )

            Spacer(modifier = Modifier.height(14.dp))

            MenuButton(
                text = "HIGH SCORES",
                icon = Icons.Default.EmojiEvents,
                onClick = onHighScoresClicked,
                containerColor = DeepBlue
            )

            Spacer(modifier = Modifier.height(14.dp))

            MenuButton(
                text = "STATS",
                icon = Icons.Default.Assessment,
                onClick = onStatsClicked,
                containerColor = DeepBlue
            )

            Spacer(modifier = Modifier.height(14.dp))

            MenuButton(
                text = "SETTINGS",
                icon = Icons.Default.Settings,
                onClick = onSettingsClicked,
                containerColor = DeepBlue
            )

            Spacer(modifier = Modifier.height(14.dp))

            MenuButton(
                text = "DEVELOPER",
                icon = Icons.Default.Build,
                onClick = onDeveloperClicked,
                containerColor = Color(0xFF607D8B),
                height = 50.dp
            )

            Spacer(modifier = Modifier.weight(0.8f))

            if (!LocalInspectionMode.current && BuildConfig.DEBUG) {
                Text("Test Ad", color = Color.Gray, fontSize = 10.sp)
            }

            Spacer(modifier = Modifier.height(8.dp))

            banner?.invoke()

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "v1.0",
                color = Color.Gray,
                fontSize = 12.sp,
                fontFamily = Oswald
            )

            if (showPopup) {
                PowerUpsPopup(
                    onDismiss = {
                        setPowerUpPopupShown(context)
                        showPopup = false
                    }
                )
            }

            // Daily Reward Dialog
            if (uiState.showDailyRewardDialog) {
                DailyRewardDialog(
                    day = uiState.dailyRewardDay,
                    streak = uiState.dailyRewardStreak,
                    coins = uiState.dailyRewardCoins,
                    hasRainbowWipe = uiState.dailyRewardRainbow,
                    onClaimReward = { viewModel.claimDailyReward() }
                )
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
    height: Dp = 60.dp
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(height),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 8.dp,
            pressedElevation = 2.dp
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = text,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = Oswald,
                letterSpacing = 1.sp
            )
        }
    }
}


@Preview(showBackground = true)
@Composable
fun MainMenuScreenPreview() {
    val vm: GameViewModel = viewModel()

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
