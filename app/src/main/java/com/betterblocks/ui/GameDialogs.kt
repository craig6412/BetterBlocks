package com.betterblocks.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.saveable.rememberSaveable

/**
 * RAINBOW METER FULL CELEBRATION DIALOG
 * Shows when the special meter reaches max and awards a rainbow wipe
 */
@Composable
fun RainbowEarnedDialog(
    onDismiss: () -> Unit
) {
    var showConfetti by remember { mutableStateOf(true) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = DeepBlue
            ),
            elevation = CardDefaults.cardElevation(8.dp)
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
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Glowing Rainbow Icon
                    GlowingRainbowIcon()

                    Text(
                        text = "Rainbow Charged!",
                        fontFamily = Oswald,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = LightText,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = "You earned a Rainbow Wipe!",
                        fontFamily = Oswald,
                        fontSize = 16.sp,
                        color = LightText.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SuccessGreen
                        ),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Text(
                            text = "Awesome!",
                            fontFamily = Oswald,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = LightText
                        )
                    }
                }

                // Simple confetti/sparkle effect
                if (showConfetti) {
                    ConfettiEffect()
                }
            }
        }
    }
}

/**
 * Glowing rainbow icon with pulsing animation
 */
@Composable
fun GlowingRainbowIcon() {
    val infiniteTransition = rememberInfiniteTransition(label = "rainbow_glow")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Box(
        modifier = Modifier.size(80.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Star,
            contentDescription = null,
            modifier = Modifier
                .size(80.dp)
                .scale(scale)
                .alpha(alpha),
            tint = Color(0xFFFF00FF) // Magenta/Rainbow color
        )
    }
}

/**
 * Simple confetti particle effect
 */
@Composable
fun ConfettiEffect() {
    val particles = remember {
        List(20) {
            ConfettiParticle(
                x = Random.nextFloat() * 300f,
                y = -50f,
                color = listOf(
                    Color(0xFFFF0000),
                    Color(0xFF00FF00),
                    Color(0xFF0000FF),
                    Color(0xFFFFFF00),
                    Color(0xFFFF00FF),
                    Color(0xFF00FFFF)
                ).random()
            )
        }
    }

    particles.forEach { particle ->
        var yPos by remember { mutableStateOf(particle.y) }
        var alpha by remember { mutableStateOf(1f) }

        LaunchedEffect(Unit) {
            while (yPos < 500f) {
                delay(16)
                yPos += 3f
                alpha = (1f - (yPos / 500f)).coerceAtLeast(0f)
            }
        }

        Box(
            modifier = Modifier
                .offset(x = particle.x.dp, y = yPos.dp)
                .size(8.dp)
                .alpha(alpha)
                .background(particle.color, shape = RoundedCornerShape(4.dp))
        )
    }
}

data class ConfettiParticle(val x: Float, val y: Float, val color: Color)

/**
 * TIER UNLOCK CELEBRATION DIALOG
 * Shows when player reaches a new tier with trophy and glow effect
 */
@Composable
fun TierUnlockDialog(
    tier: TrophyTier,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = DeepBlue
            ),
            elevation = CardDefaults.cardElevation(8.dp)
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
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Glowing Trophy
                    GlowingTrophy(tier)

                    Text(
                        text = "New Tier Unlocked!",
                        fontFamily = Oswald,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = LightText,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = tier.name,
                        fontFamily = Oswald,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        color = trophyColorForTier(tier),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = trophyColorForTier(tier)
                        ),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Text(
                            text = "Nice!",
                            fontFamily = Oswald,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (tier == TrophyTier.PLATINUM || tier == TrophyTier.DIAMOND)
                                DarkBackground else LightText
                        )
                    }
                }
            }
        }
    }
}

/**
 * Glowing trophy icon with scale and alpha animation
 */
@Composable
fun GlowingTrophy(tier: TrophyTier) {
    val infiniteTransition = rememberInfiniteTransition(label = "trophy_glow")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )

    Box(
        modifier = Modifier.size(100.dp),
        contentAlignment = Alignment.Center
    ) {
        // Glow effect (larger, semi-transparent trophy image tinted by color via layered background)
        Image(
            painter = painterResource(com.betterblocks.trophyRes(tier)),
            contentDescription = null,
            modifier = Modifier
                .size(120.dp)
                .scale(scale * 1.2f)
                .alpha(glowAlpha),
            contentScale = ContentScale.Fit
        )
        // Main trophy
        Image(
            painter = painterResource(com.betterblocks.trophyRes(tier)),
            contentDescription = null,
            modifier = Modifier
                .size(100.dp)
                .scale(scale),
            contentScale = ContentScale.Fit
        )
    }
}

/**
 * IAP PURCHASE SUCCESS CELEBRATION
 * Shows when in-app purchase completes successfully
 */
@Composable
fun PurchaseSuccessDialog(
    coinsAwarded: Int,
    onDismiss: () -> Unit
) {
    var chestOpened by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(300)
        chestOpened = true
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = DeepBlue
            ),
            elevation = CardDefaults.cardElevation(8.dp)
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
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Animated treasure chest
                    AnimatedChest(opened = chestOpened)

                    AnimatedVisibility(
                        visible = chestOpened,
                        enter = fadeIn() + slideInVertically()
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Purchase Successful!",
                                fontFamily = Oswald,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = SuccessGreen,
                                textAlign = TextAlign.Center
                            )

                            Text(
                                text = "You received ${String.format("%,d", coinsAwarded)} coins!",
                                fontFamily = Oswald,
                                fontSize = 18.sp,
                                color = Pink_Jackie,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Pink_Jackie
                        ),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Text(
                            text = "Awesome!",
                            fontFamily = Oswald,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = DeepBlue
                        )
                    }
                }

                // Coin burst particles
                if (chestOpened) {
                    CoinBurstEffect()
                }
            }
        }
    }
}

/**
 * Animated treasure chest that bursts open
 */
@Composable
fun AnimatedChest(opened: Boolean) {
    val scale by animateFloatAsState(
        targetValue = if (opened) 1.2f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "chest_scale"
    )

    Box(
        modifier = Modifier
            .size(80.dp)
            .scale(scale),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "💰",
            fontSize = 64.sp
        )
    }
}

/**
 * Coin burst particle effect
 */
@Composable
fun CoinBurstEffect() {
    val particles = remember {
        List(15) {
            val angle = (it * 24f) * (Math.PI / 180.0)
            CoinParticle(
                startX = 0f,
                startY = 0f,
                velocityX = (Math.cos(angle) * (50 + Random.nextInt(50))).toFloat(),
                velocityY = (Math.sin(angle) * (50 + Random.nextInt(50))).toFloat()
            )
        }
    }

    particles.forEach { particle ->
        var x by remember { mutableStateOf(particle.startX) }
        var y by remember { mutableStateOf(particle.startY) }
        var alpha by remember { mutableStateOf(1f) }

        LaunchedEffect(Unit) {
            var time = 0f
            while (time < 1f) {
                delay(16)
                time += 0.02f
                x = particle.startX + particle.velocityX * time
                y = particle.startY + particle.velocityY * time + (time * time * 100f) // gravity
                alpha = (1f - time).coerceAtLeast(0f)
            }
        }

        Text(
            text = "🪙",
            fontSize = 20.sp,
            modifier = Modifier
                .offset(x = x.dp, y = y.dp)
                .alpha(alpha)
        )
    }
}

data class CoinParticle(
    val startX: Float,
    val startY: Float,
    val velocityX: Float,
    val velocityY: Float
)

/**
 * FLOATING COIN REWARD ANIMATION
 * Shows when player earns coins from score threshold
 */
@Composable
fun FloatingCoinReward(
    amount: Int,
    onComplete: () -> Unit
) {
    var visible by remember { mutableStateOf(true) }
    var yOffset by remember { mutableStateOf(0f) }
    var alpha by remember { mutableStateOf(1f) }

    LaunchedEffect(Unit) {
        var progress = 0f
        while (progress < 1f) {
            delay(16)
            progress += 0.02f
            yOffset = -100f * progress
            alpha = (1f - progress).coerceAtLeast(0f)
        }
        visible = false
        onComplete()
    }

    if (visible) {
        Box(
            modifier = Modifier
                .offset(y = yOffset.dp)
                .alpha(alpha)
        ) {
            Text(
                text = "+$amount Coins!",
                fontFamily = Oswald,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Pink_Jackie,
                modifier = Modifier
                    .background(
                        color = DarkBackground.copy(alpha = 0.8f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
    }
}

/**
 * SHOP PURCHASE FEEDBACK BUBBLE
 * Shows when player buys rainbow wipe or color wipe from shop
 */
@Composable
fun ShopPurchaseBubble(
    message: String,
    icon: String = "✨",
    onComplete: () -> Unit
) {
    var scale by remember { mutableStateOf(0f) }
    var alpha by remember { mutableStateOf(1f) }

    LaunchedEffect(Unit) {
        // Scale in
        var progress = 0f
        while (progress < 1f) {
            delay(16)
            progress += 0.05f
            scale = progress
        }
        delay(1000)
        // Fade out
        progress = 0f
        while (progress < 1f) {
            delay(16)
            progress += 0.05f
            alpha = 1f - progress
        }
        onComplete()
    }

    Box(
        modifier = Modifier
            .scale(scale)
            .alpha(alpha)
    ) {
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = SuccessGreen
            ),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = icon,
                    fontSize = 24.sp
                )
                Text(
                    text = message,
                    fontFamily = Oswald,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = LightText
                )
            }
        }
    }
}

/**
 * "What's New" / economy rebalance dialog.
 *
 * Shown one-time on first launch after the economy rebalance update.
 */
@Composable
fun EconomyUpdateDialog(
    onDismiss: () -> Unit,
    onViewTrophyBoard: (() -> Unit)? = null
) {
    // Make back/outside dismiss behave exactly like pressing "Got it".
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = DeepBlue),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(DarkBackground, DeepBlue)
                        )
                    )
                    .padding(24.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Economy Update",
                        fontFamily = Oswald,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = LightText
                    )

                    Text(
                        text = "We’ve rebalanced coins, power-ups, and tier unlock costs to keep competition fair.",
                        fontFamily = Oswald,
                        fontSize = 16.sp,
                        color = LightText.copy(alpha = 0.85f)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Two ways to unlock Trophy Board tiers:",
                        fontFamily = Oswald,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = LightText
                    )

                    Text(
                        text = "• Score Path: reach the required score through gameplay\n" +
                                "• Instant Unlock Path: unlock tiers using coins (earned or from coin packs)",
                        fontFamily = Oswald,
                        fontSize = 15.sp,
                        color = LightText.copy(alpha = 0.85f)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "What changed:",
                        fontFamily = Oswald,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = LightText
                    )

                    Text(
                        text = "• Coin pack rewards updated\n" +
                                "• Tier unlock coin costs updated\n" +
                                "• Power-up prices adjusted for balance",
                        fontFamily = Oswald,
                        fontSize = 15.sp,
                        color = LightText.copy(alpha = 0.85f)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Important: Your existing leaderboard scores and earned tiers are not reduced.",
                        fontFamily = Oswald,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Pink_Jackie
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        if (onViewTrophyBoard != null) {
                            TextButton(onClick = onViewTrophyBoard) {
                                Text(
                                    text = "View Trophy Board",
                                    fontFamily = Oswald,
                                    fontWeight = FontWeight.Bold,
                                    color = LightText
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                        }

                        Button(
                            onClick = onDismiss,
                            colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                            shape = RoundedCornerShape(18.dp)
                        ) {
                            Text(
                                text = "Got it",
                                fontFamily = Oswald,
                                fontSize = 18.sp,
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
