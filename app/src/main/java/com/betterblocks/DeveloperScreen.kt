package com.betterblocks.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.betterblocks.GameSettings

@Composable
fun DeveloperScreen(onBackClicked: () -> Unit) {
    // Read settings
    val currentBannerScale = GameSettings.bannerScale.value
    val currentBtnScale = GameSettings.rotateButtonScale.value
    val currentGridX = GameSettings.gridOffsetX.value
    val currentGridY = GameSettings.gridOffsetY.value

    val scrollState = rememberScrollState()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = DarkBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(scrollState) // Make scrollable
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 24.dp)
            ) {
                IconButton(onClick = onBackClicked) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = LightText)
                }
                Text(
                    text = "Developer Tools",
                    color = LightText,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = Oswald,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            // --- 1. Banner Scale ---
           /* DevSettingCard(title = "Main Menu Banner Scale: ${(currentBannerScale * 100).toInt()}%") {
                Slider(
                    value = currentBannerScale,
                    onValueChange = { GameSettings.bannerScale.value = it },
                    valueRange = 0.5f..3.0f,
                    colors = devSliderColors()
                )
            }*/

            // --- 2. Rotate Button Scale ---
            DevSettingCard(title = "Rotate Button Scale: ${(currentBtnScale * 100).toInt()}%") {
                Slider(
                    value = currentBtnScale,
                    onValueChange = { GameSettings.rotateButtonScale.value = it },
                    valueRange = 0.5f..2.0f,
                    colors = devSliderColors()
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- 3. Grid Position X (Left/Right) ---
            DevSettingCard(title = "Grid X Offset: ${currentGridX.toInt()} dp") {
                Slider(
                    value = currentGridX,
                    onValueChange = { GameSettings.gridOffsetX.value = it },
                    valueRange = -100f..100f,
                    colors = devSliderColors()
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- 4. Grid Position Y (Up/Down) ---
            DevSettingCard(title = "Grid Y Offset: ${currentGridY.toInt()} dp") {
                Slider(
                    value = currentGridY,
                    onValueChange = { GameSettings.gridOffsetY.value = it },
                    valueRange = -200f..200f,
                    colors = devSliderColors()
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- 5. Header Vertical Padding ---
            DevSettingCard(title = "headerVerticalPadding: ${GameSettings.headerVerticalPadding.value.toInt()} dp") {
                Slider(
                    value = GameSettings.headerVerticalPadding.value,
                    onValueChange = { GameSettings.headerVerticalPadding.value = it },
                    valueRange = -50f..50f,
                    colors = devSliderColors()
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            DevSettingCard(title = "availableBlocksRowHeight: ${GameSettings.availableBlocksRowHeight.value.toInt()} dp") {
                Slider(
                    value = GameSettings.availableBlocksRowHeight.value,
                    onValueChange = { GameSettings.availableBlocksRowHeight.value = it },
                    valueRange = -50f..200f,
                    colors = devSliderColors()
                )
            }
            Spacer(modifier = Modifier.height(16.dp))

            DevSettingCard(title = "bottomBarVerticalPadding: ${GameSettings.bottomBarVerticalPadding.value.toInt()} dp") {
                Slider(
                    value = GameSettings.bottomBarVerticalPadding.value.toFloat(),
                    onValueChange = { GameSettings.bottomBarVerticalPadding.value = it.toInt() },
                    valueRange = -50f..50f,
                    colors = devSliderColors()
                )
            }
            Spacer(modifier = Modifier.height(16.dp))

            DevSettingCard(title = "Icon Spacing: ${GameSettings.bottomBarIconSpacing.value.value.toInt()} dp") {
                Slider(
                    value = GameSettings.bottomBarIconSpacing.value.value,
                    onValueChange = { GameSettings.bottomBarIconSpacing.value = it.dp },
                    valueRange = 0f..80f,
                    colors = devSliderColors()
                )
            }
            Spacer(modifier = Modifier.height(16.dp))

// --- BUTTON BORDER WIDTH ---
            DevSettingCard(title = "Button Border: ${GameSettings.bottomBarButtonBorderWidth.value.value.toInt()} dp") {
                Slider(
                    value = GameSettings.bottomBarButtonBorderWidth.value.value,
                    onValueChange = { GameSettings.bottomBarButtonBorderWidth.value = it.dp },
                    valueRange = 0f..10f,
                    colors = devSliderColors()
                )
            }
            Spacer(modifier = Modifier.height(16.dp))

// --- BADGE SCALE ---
            DevSettingCard(title = "Badge Scale: ${"%.2f".format(GameSettings.bottomBarBadgeScale.value)}") {
                Slider(
                    value = GameSettings.bottomBarBadgeScale.value,
                    onValueChange = { GameSettings.bottomBarBadgeScale.value = it },
                    valueRange = 0.4f..2.0f,
                    colors = devSliderColors()
                )
            }


            }
        }
    }


/*

var bottomBarPadding: Dp = 12.dp
var bottomBarIconSpacing: Dp = 24.dp
var bottomBarIconScale: Float = 1.0f
var bottomBarIconSize: Dp = 36.dp        // rotation icon size
var bottomBarSpecialIconSize: Dp = 55.dp // rainbow & color wipe buttons
var bottomBarBadgeScale: Float = 1.0f    // x63, x0 text bubbles
var bottomBarButtonCorner: Dp = 16.dp
var bottomBarButtonBorderWidth: Dp = 2.dp

*/


@Composable
fun DevSettingCard(title: String, content: @Composable () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DeepBlue),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = title, color = LightText, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
            content()
        }
    }
}

@Composable
fun devSliderColors() = SliderDefaults.colors(
    thumbColor = CoinGold,
    activeTrackColor = CoinGold,
    inactiveTrackColor = Color.Gray
)