package com.betterblocks

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.betterblocks.model.TrophyTier
import com.betterblocks.model.getPlayerTier
import com.betterblocks.ui.*
import com.betterblocks.ui.theme.BetterBlocksTheme
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class StatsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set System Bar Colors
        window.statusBarColor = android.graphics.Color.parseColor("#1E214A")
        window.navigationBarColor = android.graphics.Color.parseColor("#1E214A")

        setContent {
            BetterBlocksTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = DarkBackground
                ) {
                    StatsScreen(onBack = { finish() })
                }
            }
        }
    }
}

@Composable
fun StatsScreen(onBack: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // Load stats
    val coins = prefs.getInt(KEY_COINS, 0)
    val lifetimeCoins = prefs.getInt(KEY_LIFETIME_COINS, coins)
    val bestScore = prefs.getInt(KEY_HIGH_SCORE, 0)
    val rainbowCount = prefs.getInt(KEY_RAINBOW_COUNT, 0)
    val colorWipeCount = prefs.getInt(KEY_COLOR_WIPE_COUNT, 0)
    val currentTier = getPlayerTier(bestScore, lifetimeCoins, prefs)
    val firebaseUserId = prefs.getString(KEY_FIREBASE_USER_ID, "Unknown") ?: "Unknown"

    // Daily reward stats
    val lastClaimDate = prefs.getString(KEY_DAILY_REWARD_DATE, null)
    val currentStreak = prefs.getInt(KEY_DAILY_STREAK, 0)
    val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    val hasClaimedToday = lastClaimDate == today

    // Calculate next reward day
    val nextRewardDay = if (hasClaimedToday) {
        (currentStreak % 7) + 1
    } else {
        ((currentStreak + 1) % 7).let { if (it == 0) 7 else it }
    }

    // Season info
    val seasonStartDate = prefs.getLong("season_start_date", System.currentTimeMillis())
    val oneYearInMillis = 365L * 24 * 60 * 60 * 1000
    val seasonEndDate = seasonStartDate + oneYearInMillis
    val timeRemaining = seasonEndDate - System.currentTimeMillis()
    val daysRemaining = TimeUnit.MILLISECONDS.toDays(timeRemaining)
    val hoursRemaining = TimeUnit.MILLISECONDS.toHours(timeRemaining) % 24

    var animateIn by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        animateIn = true
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(DarkBackground, DeepBlue)
                )
            )
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = LightText,
                    modifier = Modifier.size(28.dp)
                )
            }

            Text(
                text = "YOUR STATS",
                color = LightText,
                fontFamily = Oswald,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.width(48.dp)) // Balance
        }

        // Scrollable content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Trophy Tier Card
            AnimatedVisibility(
                visible = animateIn,
                enter = fadeIn() + slideInVertically()
            ) {
                TrophyTierCard(currentTier = currentTier)
            }

            // Player Info Card
            AnimatedVisibility(
                visible = animateIn,
                enter = fadeIn() + slideInVertically()
            ) {
                PlayerInfoCard(
                    firebaseUserId = firebaseUserId,
                    bestScore = bestScore,
                    lifetimeCoins = lifetimeCoins,
                    currentCoins = coins
                )
            }

            // Daily Reward Progress Card
            AnimatedVisibility(
                visible = animateIn,
                enter = fadeIn() + slideInVertically()
            ) {
                DailyRewardProgressCard(
                    currentStreak = currentStreak,
                    nextRewardDay = nextRewardDay,
                    hasClaimedToday = hasClaimedToday
                )
            }

            // Season Info Card
            AnimatedVisibility(
                visible = animateIn,
                enter = fadeIn() + slideInVertically()
            ) {
                SeasonInfoCard(
                    daysRemaining = daysRemaining,
                    hoursRemaining = hoursRemaining
                )
            }

            // Inventory Card
            AnimatedVisibility(
                visible = animateIn,
                enter = fadeIn() + slideInVertically()
            ) {
                InventoryCard(
                    rainbowCount = rainbowCount,
                    colorWipeCount = colorWipeCount
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun TrophyTierCard(currentTier: TrophyTier) {
    val trophyColor = trophyColorForTier(currentTier)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DeepBlue.copy(alpha = 0.9f)),
        elevation = CardDefaults.cardElevation(8.dp),
        border = BorderStroke(2.dp, trophyColor.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Glowing trophy icon
            GlowingTrophy(tier = currentTier)

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "CURRENT TIER",
                color = LightText.copy(alpha = 0.7f),
                fontFamily = Oswald,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = currentTier.name,
                color = trophyColor,
                fontFamily = Oswald,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun PlayerInfoCard(
    firebaseUserId: String,
    bestScore: Int,
    lifetimeCoins: Int,
    currentCoins: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DeepBlue.copy(alpha = 0.7f)),
        elevation = CardDefaults.cardElevation(6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "PLAYER INFO",
                color = LightText,
                fontFamily = Oswald,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            StatRow(label = "Player ID", value = firebaseUserId.take(12) + "...")
            StatRow(label = "Best Score", value = String.format("%,d", bestScore))
            StatRow(label = "Lifetime Coins", value = String.format("%,d", lifetimeCoins))
            StatRow(label = "Current Coins", value = String.format("%,d", currentCoins))
        }
    }
}

@Composable
fun DailyRewardProgressCard(
    currentStreak: Int,
    nextRewardDay: Int,
    hasClaimedToday: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DeepBlue.copy(alpha = 0.7f)),
        elevation = CardDefaults.cardElevation(6.dp),
        border = BorderStroke(1.dp, SpecialPurple.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "DAILY REWARDS",
                    color = LightText,
                    fontFamily = Oswald,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                if (hasClaimedToday) {
                    Surface(
                        color = SuccessGreen.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "✓ Claimed",
                            color = SuccessGreen,
                            fontFamily = Oswald,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Streak display
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Current Streak",
                        color = LightText.copy(alpha = 0.7f),
                        fontFamily = Oswald,
                        fontSize = 12.sp
                    )
                    Text(
                        text = "$currentStreak ${if (currentStreak == 1) "day" else "days"}",
                        color = Pink_Jackie,
                        fontFamily = Oswald,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Next Reward",
                        color = LightText.copy(alpha = 0.7f),
                        fontFamily = Oswald,
                        fontSize = 12.sp
                    )
                    Text(
                        text = "Day $nextRewardDay",
                        color = SpecialPurple,
                        fontFamily = Oswald,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Progress dots
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                repeat(7) { day ->
                    val dayNum = day + 1
                    val isCompleted = dayNum <= (currentStreak % 7).let { if (it == 0 && currentStreak > 0) 7 else it }
                    val isNext = dayNum == nextRewardDay

                    Box(
                        modifier = Modifier
                            .size(if (isNext) 36.dp else 32.dp)
                            .background(
                                color = when {
                                    isCompleted -> SuccessGreen.copy(alpha = 0.3f)
                                    isNext -> SpecialPurple.copy(alpha = 0.3f)
                                    else -> Color.Gray.copy(alpha = 0.2f)
                                },
                                shape = CircleShape
                            )
                            .padding(4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (dayNum == 7) "🌈" else dayNum.toString(),
                            color = when {
                                isCompleted -> SuccessGreen
                                isNext -> SpecialPurple
                                else -> Color.Gray
                            },
                            fontSize = if (dayNum == 7) 16.sp else 14.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = Oswald
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SeasonInfoCard(daysRemaining: Long, hoursRemaining: Long) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DeepBlue.copy(alpha = 0.7f)),
        elevation = CardDefaults.cardElevation(6.dp),
        border = BorderStroke(1.dp, Pink_Jackie.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "⏰",
                    fontSize = 24.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "SEASON COUNTDOWN",
                    color = LightText,
                    fontFamily = Oswald,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = daysRemaining.toString(),
                        color = Pink_Jackie,
                        fontFamily = Oswald,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "DAYS",
                        color = LightText.copy(alpha = 0.7f),
                        fontFamily = Oswald,
                        fontSize = 12.sp
                    )
                }

                Text(
                    text = ":",
                    color = LightText.copy(alpha = 0.5f),
                    fontSize = 32.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = hoursRemaining.toString(),
                        color = Pink_Jackie,
                        fontFamily = Oswald,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "HOURS",
                        color = LightText.copy(alpha = 0.7f),
                        fontFamily = Oswald,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Compete for top leaderboard position!",
                color = LightText.copy(alpha = 0.6f),
                fontFamily = Oswald,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun InventoryCard(rainbowCount: Int, colorWipeCount: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DeepBlue.copy(alpha = 0.7f)),
        elevation = CardDefaults.cardElevation(6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "INVENTORY",
                color = LightText,
                fontFamily = Oswald,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                InventoryItem(
                    icon = "🌈",
                    label = "Rainbow Wipes",
                    count = rainbowCount
                )

                InventoryItem(
                    icon = "🎨",
                    label = "Color Wipes",
                    count = colorWipeCount
                )
            }
        }
    }
}

@Composable
fun InventoryItem(icon: String, label: String, count: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = icon, fontSize = 32.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = count.toString(),
            color = Pink_Jackie,
            fontFamily = Oswald,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            color = LightText.copy(alpha = 0.7f),
            fontFamily = Oswald,
            fontSize = 12.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = LightText.copy(alpha = 0.7f),
            fontFamily = Oswald,
            fontSize = 14.sp
        )
        Text(
            text = value,
            color = LightText,
            fontFamily = Oswald,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

