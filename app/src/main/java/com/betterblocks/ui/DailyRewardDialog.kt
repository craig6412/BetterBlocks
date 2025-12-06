package com.betterblocks.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.delay

/**
 * DAILY REWARD DIALOG
 * Shows when user claims daily reward
 */
@Composable
fun DailyRewardDialog(
    day: Int,
    streak: Int,
    coins: Int,
    hasRainbowWipe: Boolean,
    onClaimReward: () -> Unit
) {
    var showGlow by remember { mutableStateOf(true) }

    // Glow animation
    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val glowScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowScale"
    )

    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    // Sparkle animation
    val sparkleRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sparkle"
    )

    Dialog(onDismissRequest = { }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = DeepBlue
            ),
            elevation = CardDefaults.cardElevation(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                DarkBackground,
                                DeepBlue
                            )
                        )
                    )
                    .padding(28.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Title
                    Text(
                        text = "DAILY REWARD",
                        fontFamily = Oswald,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = LightText,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Day and Streak
                    Text(
                        text = "Day $day",
                        fontFamily = Oswald,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = CoinGold,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = "🔥 Streak: $streak ${if (streak == 1) "day" else "days"}",
                        fontFamily = Oswald,
                        fontSize = 18.sp,
                        color = LightText.copy(alpha = 0.9f),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Reward Display with Glow
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(140.dp)
                    ) {
                        // Glow effect behind icon
                        if (showGlow) {
                            Box(
                                modifier = Modifier
                                    .size(100.dp * glowScale)
                                    .background(
                                        color = CoinGold.copy(alpha = glowAlpha),
                                        shape = androidx.compose.foundation.shape.CircleShape
                                    )
                            )
                        }

                        // Primary Icon
                        Text(
                            text = if (hasRainbowWipe) "🌈" else "💰",
                            fontSize = 72.sp,
                            modifier = Modifier.scale(1f)
                        )

                        // Small floating sparkles
                        if (!hasRainbowWipe) {
                            Text(
                                text = "✨",
                                fontSize = 24.sp,
                                modifier = Modifier
                                    .offset(x = 40.dp, y = (-40).dp)
                                    .scale(0.8f + (sparkleRotation % 60) / 200f)
                            )
                            Text(
                                text = "✨",
                                fontSize = 20.sp,
                                modifier = Modifier
                                    .offset(x = (-50).dp, y = 30.dp)
                                    .scale(0.8f + ((sparkleRotation + 120) % 60) / 200f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Reward description
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "💰",
                                fontSize = 28.sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "+$coins coins",
                                fontFamily = Oswald,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = CoinGold
                            )
                        }

                        if (hasRainbowWipe) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "🌈",
                                    fontSize = 28.sp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "+1 Rainbow Wipe",
                                    fontFamily = Oswald,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFF00FF) // Magenta
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Claim Button
                    Button(
                        onClick = onClaimReward,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SuccessGreen
                        ),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                    ) {
                        Text(
                            text = "CLAIM REWARD",
                            fontFamily = Oswald,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = LightText
                        )
                    }
                }
            }
        }
    }
}

