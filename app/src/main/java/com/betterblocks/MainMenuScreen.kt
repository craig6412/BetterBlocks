package com.betterblocks.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build // Icon for Developer
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.betterblocks.R
import com.betterblocks.GameSettings // Import the settings

// Define the gradient colors based on the banner image
val BannerBlueTop = Color(0xFF2A5092) // Brighter blue from the top of the banner
val BannerBlueBottom = Color(0xFF1E3A71) // Darker blue from the bottom of the banner

@Composable
fun MainMenuScreen(
    onPlayClicked: () -> Unit,
    onShopClicked: () -> Unit,
    onHighScoresClicked: () -> Unit,
    onSettingsClicked: () -> Unit,
    onDeveloperClicked: () -> Unit // New callback
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.Transparent
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(
                    colors = listOf(BannerBlueTop, BannerBlueBottom)
                ))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // --- Banner Image Section ---
            Spacer(modifier = Modifier.weight(0.8f))

            Box(contentAlignment = Alignment.Center) {
                Image(
                    painter = painterResource(id = R.drawable.banner),
                    contentDescription = "Better Blocks Game Title",
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .aspectRatio(3.2f)
                        // APPLY THE SCALE SETTING HERE
                        .scale(GameSettings.bannerScale.value),
                    contentScale = ContentScale.Fit
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            // --- Menu Buttons ---

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

            // New Developer Button
            Spacer(modifier = Modifier.height(16.dp))

            MenuButton(
                text = "DEVELOPER",
                icon = Icons.Default.Build,
                onClick = onDeveloperClicked,
                containerColor = Color(0xFF607D8B), // Grey-Blue to distinguish it
                height = 50.dp
            )

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = "v1.0",
                color = Color.Gray,
                fontSize = 12.sp,
                fontFamily = Oswald
            )
        }
    }
}

@Composable
fun MenuButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    containerColor: Color,
    contentColor: Color = LightText,
    height: androidx.compose.ui.unit.Dp = 60.dp
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
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp, pressedElevation = 2.dp)
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
    MainMenuScreen(
        onPlayClicked = {},
        onShopClicked = {},
        onHighScoresClicked = {},
        onSettingsClicked = {},
        onDeveloperClicked = {}
    )
}