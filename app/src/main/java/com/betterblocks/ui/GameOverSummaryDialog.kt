package com.betterblocks.ui

import android.content.Context
import android.content.Intent
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.window.Dialog
import com.betterblocks.DarkBackground
import com.betterblocks.DeepBlue
import com.betterblocks.LightText
import com.betterblocks.Oswald
import com.betterblocks.Pink_Jackie
import com.betterblocks.SuccessGreen
import com.betterblocks.model.TrophyTier
import com.betterblocks.model.drawableRes
import com.betterblocks.trophyColorForTier
import kotlinx.coroutines.delay
import kotlin.random.Random
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale

/**
 * GAME OVER SUMMARY DIALOG
 * Shows when game ends with complete stats
 */
@Composable
fun GameOverSummaryDialog(
    finalScore: Int,
    highScore: Int,
    totalLinesCleared: Int,
    coinsEarned: Int,
    trophyTier: TrophyTier,
    isNewHighScore: Boolean,
    onPlayAgain: () -> Unit,
    onMainMenu: () -> Unit,
    onShare: () -> Unit
) {
    val context = LocalContext.current

    // Entrance animation
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(100)
        visible = true
    }

    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.8f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )

    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(300),
        label = "alpha"
    )

    // Confetti particles for new high score
    var showConfetti by remember { mutableStateOf(isNewHighScore) }

    Dialog(onDismissRequest = { }) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // Confetti effect
            if (showConfetti && isNewHighScore) {
                ConfettiParticles()
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .scale(scale)
                    .alpha(alpha),
                shape = RoundedCornerShape(20.dp),
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
                        .padding(28.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Game Over Title
                        Text(
                            text = "GAME OVER",
                            fontFamily = Oswald,
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isNewHighScore) Pink_Jackie else LightText,
                            textAlign = TextAlign.Center
                        )

                        if (isNewHighScore) {
                            Text(
                                text = "🎉 NEW HIGH SCORE! 🎉",
                                fontFamily = Oswald,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Pink_Jackie,
                                textAlign = TextAlign.Center
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Stats Section
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = DarkBackground.copy(alpha = 0.6f)
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Final Score
                                StatRow(
                                    label = "Final Score",
                                    value = finalScore.toString(),
                                    color = LightText,
                                    highlight = true
                                )

                                // High Score
                                StatRow(
                                    label = "High Score",
                                    value = highScore.toString(),
                                    color = Pink_Jackie,
                                    highlight = false
                                )

                                HorizontalDivider(
                                    color = LightText.copy(alpha = 0.2f),
                                    thickness = 1.dp
                                )

                                // Lines Cleared
                                StatRow(
                                    label = "Lines Cleared",
                                    value = totalLinesCleared.toString(),
                                    color = LightText.copy(alpha = 0.9f),
                                    highlight = false,
                                    icon = "📊"
                                )

                                // Coins Earned
                                StatRow(
                                    label = "Coins Earned",
                                    value = coinsEarned.toString(),
                                    color = Pink_Jackie,
                                    highlight = false,
                                    icon = "💰"
                                )

                                HorizontalDivider(
                                    color = LightText.copy(alpha = 0.2f),
                                    thickness = 1.dp
                                )

                                // Trophy Tier
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Trophy Tier",
                                        fontFamily = Oswald,
                                        fontSize = 16.sp,
                                        color = LightText.copy(alpha = 0.8f)
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Image(
                                            painter = painterResource(com.betterblocks.trophyRes(trophyTier)),
                                            contentDescription = "Trophy",
                                            modifier = Modifier.size(24.dp),
                                            contentScale = ContentScale.Fit
                                        )
                                        Text(
                                            text = trophyTier.name,
                                            fontFamily = Oswald,
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = trophyColorForTier(trophyTier)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Buttons
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Share Button
                            Button(
                                onClick = onShare,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF2196F3) // Blue
                                ),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Share",
                                    tint = LightText
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "SHARE",
                                    fontFamily = Oswald,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = LightText
                                )
                            }

                            // Play Again Button
                            Button(
                                onClick = onPlayAgain,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = SuccessGreen
                                ),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Play Again",
                                    tint = Color.Black

                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "PLAY AGAIN",
                                    fontFamily = Oswald,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = LightText
                                )
                            }

                            // Main Menu Button
                            OutlinedButton(
                                onClick = onMainMenu,
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = LightText
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Home,
                                    contentDescription = "Main Menu",
                                    tint = LightText
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "MAIN MENU",
                                    fontFamily = Oswald,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = LightText
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatRow(
    label: String,
    value: String,
    color: Color,
    highlight: Boolean,
    icon: String? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (icon != null) {
                Text(text = icon, fontSize = 18.sp)
            }
            Text(
                text = label,
                fontFamily = Oswald,
                fontSize = if (highlight) 18.sp else 16.sp,
                fontWeight = if (highlight) FontWeight.Bold else FontWeight.Normal,
                color = LightText.copy(alpha = if (highlight) 1f else 0.8f)
            )
        }
        Text(
            text = value,
            fontFamily = Oswald,
            fontSize = if (highlight) 24.sp else 20.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
private fun ConfettiParticles() {
    // Simple confetti particle burst effect
    val particles = remember { List(20) { GameSummaryConfettiParticle() } }

    Box(modifier = Modifier.fillMaxSize()) {
        particles.forEach { particle ->
            ConfettiParticleItem(particle)
        }
    }
}

private data class GameSummaryConfettiParticle(
    val x: Float = Random.nextFloat(),
    val y: Float = Random.nextFloat() * 0.3f, // Start from top
    val color: Color = listOf(
        Color(0xFFFFD700), // Gold
        Color(0xFFFF69B4), // Pink
        Color(0xFF00CED1), // Cyan
        Color(0xFF32CD32), // Green
        Color(0xFFFF6347)  // Red
    ).random(),
    val size: Float = Random.nextFloat() * 8f + 4f
)

@Composable
private fun ConfettiParticleItem(particle: GameSummaryConfettiParticle) {
    var offsetY by remember { mutableStateOf(particle.y) }

    LaunchedEffect(Unit) {
        while (offsetY < 1.2f) {
            delay(16)
            offsetY += 0.01f
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .wrapContentSize(Alignment.TopStart)
            .offset(
                x = (particle.x * 400).dp,
                y = (offsetY * 800).dp
            )
            .size(particle.size.dp)
            .background(particle.color, shape = androidx.compose.foundation.shape.CircleShape)
    )
}

/**
 * Helper function to create a share intent for game results
 */
fun shareGameResults(context: Context, score: Int, tier: TrophyTier) {
    val shareText = "🎮 I just scored $score points in Better Blocks! My trophy tier: ${tier.name} 🏆\n\nCan you beat my score?"

    val shareIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, shareText)
        type = "text/plain"
    }

    context.startActivity(Intent.createChooser(shareIntent, "Share your score"))
}

@Preview(
    name = "Tablet – Portrait",
    showBackground = true,
    showSystemUi = true,
    device = "spec:width=800dp,height=1280dp,dpi=480"
)
@Composable
fun GameOverSummaryDialogPreview() {
    GameOverSummaryDialog(
        finalScore = 1234,
        highScore = 2000,
        totalLinesCleared = 25,
        coinsEarned = 150,
        trophyTier = TrophyTier.GOLD,
        isNewHighScore = true,
        onPlayAgain = {},
        onMainMenu = {},
        onShare = {}
    )
}
