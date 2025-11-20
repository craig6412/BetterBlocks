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
            DevSettingCard(title = "Main Menu Banner Scale: ${(currentBannerScale * 100).toInt()}%") {
                Slider(
                    value = currentBannerScale,
                    onValueChange = { GameSettings.bannerScale.value = it },
                    valueRange = 0.5f..3.0f,
                    colors = devSliderColors()
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

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
        }
    }
}

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