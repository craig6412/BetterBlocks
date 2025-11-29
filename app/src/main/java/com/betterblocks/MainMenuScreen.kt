package com.betterblocks.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import com.betterblocks.BuildConfig
import com.betterblocks.R
import com.betterblocks.GameSettings
import com.betterblocks.GameUiState
import com.betterblocks.GameViewModel
import com.betterblocks.ui.PowerUpsPopup
import com.betterblocks.ui.hasShownPowerUpPopup
import com.betterblocks.ui.setPowerUpPopupShown


// Gradient colors for background
val BannerBlueTop = Color(0xFF2A5092)
val BannerBlueBottom = Color(0xFF1E3A71)


@Composable
fun PowerupHeader(uiState: GameUiState) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("💰", fontSize = 22.sp)
            Spacer(Modifier.width(6.dp))
            Text(uiState.coins.toString(), color = Color.White, fontSize = 18.sp)
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("🌈", fontSize = 22.sp)
            Spacer(Modifier.width(6.dp))
            Text(uiState.rainbowBlockCount.toString(), color = Color.White, fontSize = 18.sp)
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(id = R.drawable.ic_palette_colorwipe),
                contentDescription = "Color Wipe",
                tint = Color.Unspecified,
                modifier = Modifier.size(26.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(uiState.colorWipeCount.toString(), color = Color.White, fontSize = 18.sp)
        }
    }
}


@Composable
fun MainMenuScreen(
    viewModel: GameViewModel,
    onPlayClicked: () -> Unit,
    onShopClicked: () -> Unit,
    onHighScoresClicked: () -> Unit,
    onSettingsClicked: () -> Unit,
    onDeveloperClicked: () -> Unit,
    banner: @Composable (() -> Unit)? = null
) {
    val context = LocalContext.current
    val uiState = viewModel.uiState.collectAsState().value

    var showPopup by remember {
        mutableStateOf(!hasShownPowerUpPopup(context))
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
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            PowerupHeader(uiState)

            Spacer(modifier = Modifier.weight(0.8f))

            Image(
                painter = painterResource(id = R.drawable.banner),
                contentDescription = "Better Blocks Game Title",
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .aspectRatio(3.2f)
                    .scale(GameSettings.bannerScale.value),
                contentScale = ContentScale.Fit
            )

            Spacer(modifier = Modifier.height(48.dp))

            MenuButton(
                text = "PLAY",
                icon = Icons.Default.PlayArrow,
                onClick = onPlayClicked,
                containerColor = Color(0xFF4CAF50),
                contentColor = Color.White,
                height = 80.dp
            )

            Spacer(modifier = Modifier.height(24.dp))

            MenuButton(
                text = "SHOP",
                icon = Icons.Default.ShoppingCart,
                onClick = onShopClicked,
                containerColor = DeepBlue
            )

            Spacer(modifier = Modifier.height(16.dp))

            MenuButton(
                text = "HIGH SCORES",
                icon = Icons.Default.EmojiEvents,
                onClick = onHighScoresClicked,
                containerColor = DeepBlue
            )

            Spacer(modifier = Modifier.height(16.dp))

            MenuButton(
                text = "SETTINGS",
                icon = Icons.Default.Settings,
                onClick = onSettingsClicked,
                containerColor = DeepBlue
            )

            Spacer(modifier = Modifier.height(16.dp))

            MenuButton(
                text = "DEVELOPER",
                icon = Icons.Default.Build,
                onClick = onDeveloperClicked,
                containerColor = Color(0xFF607D8B),
                height = 50.dp
            )

            Spacer(modifier = Modifier.weight(1f))

            if (BuildConfig.DEBUG) {
                Text("Test Ad", color = Color.Gray, fontSize = 10.sp)
            }

            banner?.invoke()

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
        onSettingsClicked = {},
        onDeveloperClicked = {}
    )
}
