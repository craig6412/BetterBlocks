package com.betterblocks

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.betterblocks.model.TrophyTier
import com.betterblocks.ui.*
import com.betterblocks.ui.theme.BetterBlocksTheme
import com.google.firebase.Timestamp
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

private const val TAG = "HighScoreActivity"
const val KEY_SELECTED_TROPHY_TAB = "selected_trophy_tab"

class HighScoreActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            BetterBlocksTheme {
                HighscoreScreen(onBack = { finish() })
            }
        }
    }
}

@Composable
fun HighscoreScreen(
    onBack: () -> Unit = {}
) {
    // Read and persist selected tab inside the composable so callers just provide onBack
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }
    val saved = prefs.getString(KEY_SELECTED_TROPHY_TAB, TrophyTier.UNRANKED.name) ?: TrophyTier.UNRANKED.name
    val initialTier = remember { try { TrophyTier.valueOf(saved) } catch (_: Throwable) { TrophyTier.UNRANKED } }

    val (selectedTier, setSelectedTier) = remember { mutableStateOf(initialTier) }

    // Background gradient
    val bg = Brush.verticalGradient(listOf(DarkBackground, DeepBlue))

    Surface(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier
            .fillMaxSize()
            .background(brush = bg)
            .padding(16.dp)) {

            Column(modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(12.dp))
                .background(DarkBackground)
                .padding(16.dp)) {

                // Header
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
                        Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "Back", tint = LightText)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "HIGH SCORES",
                        fontFamily = Oswald,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = LightText,
                        modifier = Modifier.align(Alignment.CenterVertically)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Tabs
                TrophyTabs(selectedIndex = trophyIndexOf(selectedTier), onTabSelected = { idx ->
                    val tier = trophyForIndex(idx)
                    setSelectedTier(tier)
                    // Persist selection
                    try {
                        prefs.edit().putString(KEY_SELECTED_TROPHY_TAB, tier.name).apply()
                    } catch (_: Throwable) {
                        Log.w(TAG, "Failed to save selected tab")
                    }
                })

                Spacer(modifier = Modifier.height(8.dp))

                HorizontalDivider(color = LightText.copy(alpha = 0.12f))

                Spacer(modifier = Modifier.height(12.dp))

                // Leaderboard content loads based on selectedTier
                LeaderboardLoader(tier = selectedTier)
            }
        }
    }
}

@Composable
fun LeaderboardLoader(tier: TrophyTier) {
    var entries by remember { mutableStateOf<List<FirestoreManager.LeaderboardEntry>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(tier) {
        isLoading = true
        errorMsg = null
        entries = emptyList()

        if (tier == TrophyTier.UNRANKED) {
            FirestoreManager.getUnranked(onResult = { list ->
                entries = list
                isLoading = false
            }, onError = { e ->
                Log.e(TAG, "Failed to load unranked: ${e.message}")
                errorMsg = "Failed to load leaderboard"
                isLoading = false
            })
        } else {
            FirestoreManager.getLeaderboardForTier(tier = tier, onResult = { list ->
                entries = list
                isLoading = false
            }, onError = { e ->
                Log.e(TAG, "Failed to load tier ${tier.name}: ${e.message}")
                errorMsg = "Failed to load leaderboard"
                isLoading = false
            })
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            isLoading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = LightText)
            }
            errorMsg != null -> {
                Text(text = errorMsg ?: "Unknown error", color = LightText, modifier = Modifier.align(Alignment.Center))
            }
            entries.isEmpty() -> {
                Text(text = "No players in this tier yet", color = LightText, fontFamily = Oswald, modifier = Modifier.align(Alignment.Center))
            }
            else -> {
                LeaderboardList(entries = entries, tier = tier)
            }
        }
    }
}

@Composable
fun TrophyTabs(selectedIndex: Int, onTabSelected: (Int) -> Unit) {
    val tiers = listOf(
        TrophyTier.UNRANKED,
        TrophyTier.BRONZE,
        TrophyTier.SILVER,
        TrophyTier.GOLD,
        TrophyTier.PLATINUM,
        TrophyTier.DIAMOND,
        TrophyTier.GOD
    )

    ScrollableTabRow(
        selectedTabIndex = selectedIndex,
        edgePadding = 8.dp,
        containerColor = DeepBlue,
        contentColor = LightText,
        // Custom indicator: use the selected tab's left/width to draw a colored underline
        indicator = { tabPositions ->
            val color = trophyColorForIndex(selectedIndex)
            val current = tabPositions.getOrNull(selectedIndex)
            if (current != null) {
                Box(modifier = Modifier
                    .offset(x = current.left)
                    .width(current.width)
                    .height(3.dp)
                    .background(color)
                )
            }
        }
    ) {
        tiers.forEachIndexed { index, tier ->
            val isSelected = index == selectedIndex
            Tab(selected = isSelected, onClick = { onTabSelected(index) }, text = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val icon = if (tier == TrophyTier.UNRANKED) Icons.Filled.Star else Icons.Filled.EmojiEvents
                    Icon(imageVector = icon, contentDescription = tier.name, tint = if (isSelected) LightText else LightText.copy(alpha = 0.6f), modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = tier.name, fontFamily = Oswald, color = if (isSelected) LightText else LightText.copy(alpha = 0.6f), fontSize = 14.sp)
                }
            })
        }
    }
}

@Composable
fun LeaderboardList(entries: List<FirestoreManager.LeaderboardEntry>, tier: TrophyTier) {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(8.dp)) {
        itemsIndexed(entries) { index, entry ->
            LeaderboardRow(index = index + 1, entry = entry, tier = tier)
            Spacer(modifier = Modifier.height(8.dp))
        }
        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun LeaderboardRow(index: Int, entry: FirestoreManager.LeaderboardEntry, tier: TrophyTier) {
    val sdf = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }
    val scoreText = remember(entry.score) { NumberFormat.getIntegerInstance().format(entry.score) }
    val updatedText = remember(entry.updatedAt) {
        val ts = entry.updatedAt
        try {
            if (ts is Timestamp) {
                sdf.format(ts.toDate())
            } else null
        } catch (_: Exception) {
            null
        } ?: "-"
    }

    val trophyColor = trophyColorForTier(tier)

    Card(modifier = Modifier
        .fillMaxWidth()
        .height(68.dp)
        .clip(RoundedCornerShape(12.dp))
        .background(DeepBlue)
        .padding(8.dp),
        colors = CardDefaults.cardColors(containerColor = DeepBlue)
    ) {
        Row(modifier = Modifier
            .fillMaxSize()
            .padding(12.dp), verticalAlignment = Alignment.CenterVertically) {

            // Rank
            Text(text = "#${index}", fontFamily = Oswald, color = LightText, fontSize = 16.sp, modifier = Modifier.width(48.dp))

            // Trophy Icon
            Icon(imageVector = Icons.Filled.EmojiEvents, contentDescription = "trophy", tint = trophyColor, modifier = Modifier.size(28.dp))

            Spacer(modifier = Modifier.width(12.dp))

            // Score and date
            Column(modifier = Modifier.weight(1f)) {
                Text(text = scoreText, fontFamily = Oswald, color = LightText, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "Updated: $updatedText", fontFamily = Oswald, color = LightText.copy(alpha = 0.7f), fontSize = 12.sp)
            }

            // User id (short)
            Text(text = entry.userId.take(8), fontFamily = Oswald, color = LightText.copy(alpha = 0.7f), fontSize = 12.sp)
        }
    }
}

// Helpers
fun trophyForIndex(index: Int): TrophyTier = when(index) {
    0 -> TrophyTier.UNRANKED
    1 -> TrophyTier.BRONZE
    2 -> TrophyTier.SILVER
    3 -> TrophyTier.GOLD
    4 -> TrophyTier.PLATINUM
    5 -> TrophyTier.DIAMOND
    6 -> TrophyTier.GOD
    else -> TrophyTier.UNRANKED
}

fun trophyIndexOf(tier: TrophyTier): Int = when(tier) {
    TrophyTier.UNRANKED -> 0
    TrophyTier.BRONZE -> 1
    TrophyTier.SILVER -> 2
    TrophyTier.GOLD -> 3
    TrophyTier.PLATINUM -> 4
    TrophyTier.DIAMOND -> 5
    TrophyTier.GOD -> 6
}

fun trophyColorForIndex(index: Int): Color = trophyColorForTier(trophyForIndex(index))

fun trophyColorForTier(tier: TrophyTier): Color = when(tier) {
    TrophyTier.UNRANKED -> LightText
    TrophyTier.BRONZE -> Color(0xFFCD7F32)
    TrophyTier.SILVER -> Color(0xFFC0C0C0)
    TrophyTier.GOLD -> CoinGold
    TrophyTier.PLATINUM -> Color(0xFFE5E4E2)
    TrophyTier.DIAMOND -> Color(0xFF0FF0FC)
    TrophyTier.GOD -> Color(0xFFFFD700)
}
