package com.betterblocks.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.betterblocks.DarkBackground
import com.betterblocks.DeepBlue
import com.betterblocks.GameSettings
import com.betterblocks.LightText
import com.betterblocks.Oswald
import com.betterblocks.Pink_Jackie
import androidx.compose.ui.tooling.preview.Preview
import com.betterblocks.ui.sdp
import com.betterblocks.ui.ssp
import com.betterblocks.ui.sw

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
                .padding(sdp(0.03f))
                .verticalScroll(scrollState) // Make scrollable
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = sdp(0.03f))
            ) {
                IconButton(onClick = onBackClicked) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = LightText)
                }
                Text(
                    text = "Developer Tools",
                    color = LightText,
                    fontSize = ssp(0.03f),
                    fontWeight = FontWeight.Bold,
                    fontFamily = Oswald,
                    modifier = Modifier.padding(start = sdp(0.015f))
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

            Spacer(modifier = Modifier.height(sdp(0.02f)))

            // --- 3. Grid Position X (Left/Right) ---
            DevSettingCard(title = "Grid X Offset: ${currentGridX.toInt()} dp") {
                Slider(
                    value = currentGridX,
                    onValueChange = { GameSettings.gridOffsetX.value = it },
                    valueRange = -100f..100f,
                    colors = devSliderColors()
                )
            }

            Spacer(modifier = Modifier.height(sdp(0.02f)))

            // --- 4. Grid Position Y (Up/Down) ---
            DevSettingCard(title = "Grid Y Offset: ${currentGridY.toInt()} dp") {
                Slider(
                    value = currentGridY,
                    onValueChange = { GameSettings.gridOffsetY.value = it },
                    valueRange = -200f..200f,
                    colors = devSliderColors()
                )
            }

            Spacer(modifier = Modifier.height(sdp(0.02f)))

            // --- 5. Header Vertical Padding ---
            DevSettingCard(title = "headerVerticalPadding: ${GameSettings.headerVerticalPadding.value.toInt()} dp") {
                Slider(
                    value = GameSettings.headerVerticalPadding.value,
                    onValueChange = { GameSettings.headerVerticalPadding.value = it },
                    valueRange = -50f..50f,
                    colors = devSliderColors()
                )
            }

            Spacer(modifier = Modifier.height(sdp(0.02f)))

            DevSettingCard(title = "availableBlocksRowHeight: ${GameSettings.availableBlocksRowHeight.value.toInt()} dp") {
                Slider(
                    value = GameSettings.availableBlocksRowHeight.value,
                    onValueChange = { GameSettings.availableBlocksRowHeight.value = it },
                    valueRange = -50f..200f,
                    colors = devSliderColors()
                )
            }
            Spacer(modifier = Modifier.height(sdp(0.02f)))

            DevSettingCard(title = "bottomBarVerticalPadding: ${GameSettings.bottomBarVerticalPadding.value.toInt()} dp") {
                Slider(
                    value = GameSettings.bottomBarVerticalPadding.value.toFloat(),
                    onValueChange = { GameSettings.bottomBarVerticalPadding.value = it.toInt() },
                    valueRange = -50f..50f,
                    colors = devSliderColors()
                )
            }
            Spacer(modifier = Modifier.height(sdp(0.02f)))

            DevSettingCard(title = "Icon Spacing: ${GameSettings.bottomBarIconSpacing.value.value.toInt()} dp") {
                Slider(
                    value = GameSettings.bottomBarIconSpacing.value.value,
                    onValueChange = { GameSettings.bottomBarIconSpacing.value = it.dp },
                    valueRange = 0f..80f,
                    colors = devSliderColors()
                )
            }
            Spacer(modifier = Modifier.height(sdp(0.02f)))

// --- BUTTON BORDER WIDTH ---
            DevSettingCard(title = "Button Border: ${GameSettings.bottomBarButtonBorderWidth.value.value.toInt()} dp") {
                Slider(
                    value = GameSettings.bottomBarButtonBorderWidth.value.value,
                    onValueChange = { GameSettings.bottomBarButtonBorderWidth.value = it.dp },
                    valueRange = 0f..10f,
                    colors = devSliderColors()
                )
            }
            Spacer(modifier = Modifier.height(sdp(0.02f)))

// --- BADGE SCALE ---
            DevSettingCard(title = "Badge Scale: ${"%.2f".format(GameSettings.bottomBarBadgeScale.value)}") {
                Slider(
                    value = GameSettings.bottomBarBadgeScale.value,
                    onValueChange = { GameSettings.bottomBarBadgeScale.value = it },
                    valueRange = 0.4f..2.0f,
                    colors = devSliderColors()
                )
            }



            Spacer(modifier = Modifier.height(sdp(0.03f)))

            // === DRAG & DROP OFFSET CONTROLS ===
            Text(
                text = "🎯 DRAG & DROP ALIGNMENT",
                color = Pink_Jackie,
                fontSize = ssp(0.02f),
                fontWeight = FontWeight.Bold,
                fontFamily = Oswald,
                modifier = Modifier.padding(vertical = sdp(0.008f))
            )

            DevSettingCard(title = "Visual Drag Offset Y: ${GameSettings.visualDragOffsetY.floatValue.toInt()} dp (How far ABOVE finger)") {
                Slider(
                    value = GameSettings.visualDragOffsetY.floatValue,
                    onValueChange = { GameSettings.visualDragOffsetY.floatValue = it },
                    valueRange = -200f..200f,
                    colors = devSliderColors()
                )
            }
            Spacer(modifier = Modifier.height(sdp(0.02f)))

            DevSettingCard(title = "Visual Drag Offset X: ${GameSettings.visualDragOffsetX.floatValue.toInt()} dp (How far RIGHT of finger)") {
                Slider(
                    value = GameSettings.visualDragOffsetX.floatValue,
                    onValueChange = { GameSettings.visualDragOffsetX.floatValue = it },
                    valueRange = -200f..200f,
                    colors = devSliderColors()
                )
            }
            Spacer(modifier = Modifier.height(sdp(0.02f)))

            DevSettingCard(title = "Matching Drag Offset Y: ${GameSettings.matchingDragOffsetY.floatValue.toInt()} dp (Ghost Y alignment)") {
                Slider(
                    value = GameSettings.matchingDragOffsetY.floatValue,
                    onValueChange = { GameSettings.matchingDragOffsetY.floatValue = it },
                    valueRange = -200f..200f,
                    colors = devSliderColors()
                )
            }
            Spacer(modifier = Modifier.height(sdp(0.02f)))

            DevSettingCard(title = "Matching Drag Offset X: ${GameSettings.matchingDragOffsetX.floatValue.toInt()} dp (Ghost X alignment)") {
                Slider(
                    value = GameSettings.matchingDragOffsetX.floatValue,
                    onValueChange = { GameSettings.matchingDragOffsetX.floatValue = it },
                    valueRange = -200f..200f,
                    colors = devSliderColors()
                )
            }

            Spacer(modifier = Modifier.height(sdp(0.03f)))

            // === BLOCK PLACEMENT FINE-TUNING ===
            Text(
                text = "🎯 PLACEMENT CORRECTION (Ghost → Drop Fix)",
                color = Color(0xFFFF5722), // Orange/Red for emphasis
                fontSize = ssp(0.02f),
                fontWeight = FontWeight.Bold,
                fontFamily = Oswald,
                modifier = Modifier.padding(vertical = sdp(0.008f))
            )

            Text(
                text = "Use these to fix misalignment when ghost is green but block won't place",
                color = LightText.copy(alpha = 0.7f),
                fontSize = ssp(0.012f),
                fontFamily = Oswald,
                modifier = Modifier.padding(bottom = sdp(0.008f))
            )

            DevSettingCard(title = "🔧 Placement Correction X: ${GameSettings.blockPlacementCorrectionX.floatValue.toInt()} dp") {
                Slider(
                    value = GameSettings.blockPlacementCorrectionX.floatValue,
                    onValueChange = { GameSettings.blockPlacementCorrectionX.floatValue = it },
                    valueRange = -12f..12f,
                    colors = devSliderColors()
                )
            }
            Spacer(modifier = Modifier.height(sdp(0.02f)))

            DevSettingCard(title = "🔧 Placement Correction Y: ${GameSettings.blockPlacementCorrectionY.floatValue.toInt()} dp") {
                Slider(
                    value = GameSettings.blockPlacementCorrectionY.floatValue,
                    onValueChange = { GameSettings.blockPlacementCorrectionY.floatValue = it },
                    valueRange = -12f..12f,
                    colors = devSliderColors()
                )
            }

            Spacer(modifier = Modifier.height(sdp(0.03f)))

            // === INVENTORY TESTING CONTROLS ===
            Text(
                text = "🎁 INVENTORY TESTING",
                color = Pink_Jackie,
                fontSize = ssp(0.02f),
                fontWeight = FontWeight.Bold,
                fontFamily = Oswald,
                modifier = Modifier.padding(vertical = sdp(0.008f))
            )

            DevSettingCard(title = "Rainbow Wipe Count: ${GameSettings.testRainbowCount.value}") {
                Slider(
                    value = GameSettings.testRainbowCount.value.toFloat(),
                    onValueChange = { GameSettings.testRainbowCount.value = it.toInt() },
                    valueRange = 0f..100f, // 0–100 for easier dev control
                    colors = devSliderColors()
                )
            }
            Spacer(modifier = Modifier.height(sdp(0.02f)))

            DevSettingCard(title = "Color Wipe Count: ${GameSettings.testColorWipeCount.value}") {
                Slider(
                    value = GameSettings.testColorWipeCount.value.toFloat(),
                    onValueChange = { GameSettings.testColorWipeCount.value = it.toInt() },
                    valueRange = 0f..100f, // 0–100
                    colors = devSliderColors()
                )
            }
            Spacer(modifier = Modifier.height(sdp(0.02f)))

            DevSettingCard(title = "Coins: ${GameSettings.testCoins.value}") {
                Slider(
                    value = GameSettings.testCoins.value.toFloat(),
                    onValueChange = { GameSettings.testCoins.value = it.toInt() },
                    valueRange = 0f..100f, // 0–100
                    colors = devSliderColors()
                )
            }

            Spacer(modifier = Modifier.height(sdp(0.02f)))

            // === SCORE TEXT FIELD ===
            DevSettingCard(title = "Current Game Score") {
                var scoreText by remember { mutableStateOf(GameSettings.testScore.value.toString()) }

                OutlinedTextField(
                    value = scoreText,
                    onValueChange = { newText ->
                        scoreText = newText
                        // Parse and update score (only if valid number)
                        val parsedScore = newText.toIntOrNull()
                        if (parsedScore != null && parsedScore in 0..1000000) {
                            GameSettings.testScore.value = parsedScore
                        }
                    },
                    label = { Text("Enter Score (0-1,000,000)", color = LightText) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Pink_Jackie,
                        unfocusedBorderColor = LightText,
                        focusedLabelColor = Pink_Jackie,
                        unfocusedLabelColor = LightText,
                        cursorColor = Pink_Jackie,
                        focusedTextColor = LightText,
                        unfocusedTextColor = LightText
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(sdp(0.008f)))

                Text(
                    text = "💡 Tip: This will update the current game score when you return to the game",
                    color = LightText.copy(alpha = 0.7f),
                    fontSize = ssp(0.012f),
                    fontFamily = Oswald
                )
            }

            Spacer(modifier = Modifier.height(sdp(0.02f))

            )


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
        Column(modifier = Modifier.padding(sdp(0.03f))) {
            Text(text = title, color = LightText, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = sdp(0.008f)))
            content()
        }
    }
}

@Composable
fun devSliderColors() = SliderDefaults.colors(
    thumbColor = Pink_Jackie,
    activeTrackColor = Pink_Jackie,
    inactiveTrackColor = Color.Gray
)

@Preview(
    name = "Tablet – Portrait",
    showBackground = true,
    showSystemUi = true,
    device = "spec:width=800dp,height=1280dp,dpi=480"
)
@Composable
fun DeveloperScreenPreview() {
    DeveloperScreen(onBackClicked = {})
}
