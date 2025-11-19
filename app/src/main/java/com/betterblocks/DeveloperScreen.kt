package com.betterblocks.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
    // Read the current scale directly from our singleton
    val currentScale = GameSettings.bannerScale.value

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = DarkBackground // Reusing your theme color
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
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
                    text = "Developer Menu",
                    color = LightText,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = Oswald,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            // --- Banner Scale Setting ---
            Card(
                colors = CardDefaults.cardColors(containerColor = DeepBlue),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Main Menu Banner Scale: ${(currentScale * 100).toInt()}%",
                        color = LightText,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Slider(
                        value = currentScale,
                        onValueChange = { newValue ->
                            // Update the global setting immediately
                            GameSettings.bannerScale.value = newValue
                        },
                        valueRange = 0.5f..2.0f, // Allow scaling from 50% to 200%
                        colors = SliderDefaults.colors(
                            thumbColor = CoinGold,
                            activeTrackColor = CoinGold,
                            inactiveTrackColor = Color.Gray
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "More dev tools can be added here...",
                color = Color.Gray,
                fontSize = 14.sp
            )
        }
    }
}