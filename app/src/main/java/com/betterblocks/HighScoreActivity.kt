package com.betterblocks

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.sp
import com.betterblocks.model.TrophyTier
import com.betterblocks.model.getPlayerTier
import com.betterblocks.ui.theme.BetterBlocksTheme
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.random.Random
import androidx.compose.ui.tooling.preview.Preview


class HighScoreActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BetterBlocksTheme {
                // Assuming you have a dark theme, forcing a dark surface for safety
                Surface(color = DarkBackground, modifier = Modifier.fillMaxSize()) {
                    HighscoreScreen(onBack = { finish() })
                }
            }
        }
    }
}

// ---------------------------------------------------------
// HIGH SCORE SCREEN — Refined UI
// ---------------------------------------------------------

private val PLAYER_NAME_REGEX = Regex("^[A-Za-z0-9 ]+")

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HighscoreScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val savedName = prefs.getString(KEY_PLAYER_NAME, null)
    val prompted = prefs.getBoolean(KEY_PLAYER_NAME_PROMPTED, false)
    var showNameDialog by remember { mutableStateOf(savedName == null && !prompted) }
    var playerName by remember { mutableStateOf(savedName ?: "") }

    val lifetimeCoins = prefs.getInt(KEY_LIFETIME_COINS, 0)
    val bestScore = prefs.getInt(KEY_HIGH_SCORE, 0)

    val playerTier = getPlayerTier(bestScore, lifetimeCoins, prefs)

    // Data Loading Logic (Unchanged)
    val purchasedPremiumTierNames = prefs.getString(KEY_PREMIUM_TIERS, "") ?: ""
    val purchasedPremiumTiers = purchasedPremiumTierNames
        .split(",")
        .filter { it.isNotBlank() }
        .mapNotNull { runCatching { TrophyTier.valueOf(it) }.getOrNull() }
        .toSet()

    val tiers = TrophyTier.values().toList()

    val saved = prefs.getString(KEY_SELECTED_TROPHY_TAB, playerTier.name)
    val initialTier = runCatching { TrophyTier.valueOf(saved!!) }.getOrDefault(playerTier)
    val initialIndex = tiers.indexOf(initialTier).coerceAtLeast(0)

    val pagerState = rememberPagerState(
        initialPage = initialIndex,
        pageCount = { tiers.size }
    )
    val coroutineScope = rememberCoroutineScope()

    // A subtle gradient background
    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(DarkBackground, DeepBlue.copy(alpha = 0.8f))
    )

    Column(
        Modifier
            .fillMaxSize()
            .background(backgroundBrush)
            .statusBarsPadding() // Ensures we don't overlap system tray
    ) {

        // 1. TOP BAR
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp)
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = LightText
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                "HIGH SCORES",
                fontFamily = Oswald,
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                color = LightText
            )
            Spacer(modifier = Modifier.weight(1f))
            TextButton(onClick = { showNameDialog = true }) {
                Text(
                    "Change Name",
                    color = LightText.copy(alpha = 0.8f),
                    fontFamily = Oswald,
                    fontSize = 14.sp
                )
            }
        }

        // 2. PLAYER STATS (HERO CARD)
        PlayerStatsCard(lifetimeCoins, playerTier)

        Spacer(Modifier.height(16.dp))

        // 3. SCROLLABLE TABS (Fixes the vertical text issue)
        ScrollableTabRow(
            selectedTabIndex = pagerState.currentPage,
            containerColor = Color.Transparent,
            contentColor = LightText,
            edgePadding = 16.dp, // Adds spacing at the start so first tab isn't smashed
            divider = {}, // Removes the default line
            indicator = { tabPositions ->
                val current = tabPositions.getOrNull(pagerState.currentPage)
                if (current != null) {
                    Box(
                        Modifier
                            .wrapContentSize(unbounded = true)
                            .offset { IntOffset(current.left.roundToPx(), 0) }
                            .width(current.width)
                            .height(4.dp)
                            .background(
                                trophyColorForIndex(pagerState.currentPage),
                                RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)
                            )
                    )
                }
            }
        ) {
            tiers.forEachIndexed { index, tier ->
                val isSelected = pagerState.currentPage == index
                val color = if (isSelected) trophyColorForTier(tier) else LightText.copy(alpha = 0.5f)

                Tab(
                    selected = isSelected,
                    onClick = {
                        coroutineScope.launch { pagerState.animateScrollToPage(index) }
                        prefs.edit().putString(KEY_SELECTED_TROPHY_TAB, tier.name).apply()
                    },
                    text = {
                        Text(
                            text = tier.name,
                            fontFamily = Oswald, // Assuming you have this font variable
                            fontSize = 14.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = color
                        )
                    }
                )
            }
        }

        Divider(color = LightText.copy(alpha = 0.1f), thickness = 1.dp)

        // 4. PAGER CONTENT
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val tier = tiers[page]

            // Sync preference on swipe
            LaunchedEffect(pagerState.currentPage) {
                if (pagerState.currentPage == page) {
                    prefs.edit().putString(KEY_SELECTED_TROPHY_TAB, tier.name).apply()
                }
            }

            LeaderboardLoader(tier)
        }
    }

    if (!LocalInspectionMode.current && showNameDialog) {
        PlayerNameDialog(
            currentName = playerName.takeIf { it.isNotBlank() },
            onSave = { chosenName ->
                val finalName = chosenName.ifBlank { generateFallbackPlayerName() }
                playerName = finalName
                prefs.edit().putString(KEY_PLAYER_NAME, finalName).putBoolean(KEY_PLAYER_NAME_PROMPTED, true).apply()
                showNameDialog = false

                val userId = prefs.getString(KEY_FIREBASE_USER_ID, null)
                val currentScore = prefs.getInt(KEY_HIGH_SCORE, 0)
                if (!userId.isNullOrBlank()) {
                    FirestoreManager.updateLeaderboard(
                        userId = userId,
                        score = currentScore,
                        tier = playerTier,
                        playerNameOverride = finalName
                    )
                }
            },
            onCancel = { showNameDialog = false }
        )
    }
}

// ---------------------------------------------------------
// COMPOSABLES
// ---------------------------------------------------------

@Composable
fun PlayerStatsCard(lifetimeCoins: Int, playerTier: TrophyTier) {
    val formatter = NumberFormat.getIntegerInstance()
    val tierColor = trophyColorForTier(playerTier)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = DeepBlue),
        elevation = CardDefaults.cardElevation(8.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Tier Icon with glow effect container
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(56.dp)
                    .background(tierColor.copy(alpha = 0.2f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.EmojiEvents,
                    contentDescription = null,
                    tint = tierColor,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(Modifier.width(16.dp))

            Column {
                Text(
                    text = "YOUR RANKING",
                    fontFamily = Oswald,
                    fontSize = 12.sp,
                    color = LightText.copy(alpha = 0.6f),
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = playerTier.name,
                    fontFamily = Oswald,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = LightText
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "${formatter.format(lifetimeCoins)} Coins",
                    fontFamily = Oswald,
                    fontSize = 16.sp,
                    color = Pink_Jackie // Assuming Pink_Jackie exists, else use Color(0xFFFFD700)
                )
            }
        }
    }
}

// ---------------- LEADERBOARD LOGIC & LIST --------------------

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
            FirestoreManager.getUnranked(
                onResult = { entries = it; isLoading = false },
                onError = { errorMsg = "Failed to load"; isLoading = false }
            )
        } else {
            FirestoreManager.getLeaderboardForTier(
                tier = tier,
                onResult = { entries = it; isLoading = false },
                onError = { errorMsg = "Failed to load"; isLoading = false }
            )
        }
    }

    Box(Modifier.fillMaxSize()) {
        if (isLoading) {
            CircularProgressIndicator(Modifier.align(Alignment.Center), color = trophyColorForTier(tier))
        } else if (errorMsg != null) {
            Text(errorMsg!!, Modifier.align(Alignment.Center), color = Color.Red)
        } else if (entries.isEmpty()) {
            EmptyState(tier)
        } else {
            LeaderboardList(entries, tier)
        }
    }
}

@Composable
fun EmptyState(tier: TrophyTier) {
    Column(
        Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.EmojiEvents,
            contentDescription = null,
            tint = LightText.copy(alpha = 0.2f),
            modifier = Modifier.size(64.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "No Champions Yet",
            fontFamily = Oswald,
            fontSize = 18.sp,
            color = LightText.copy(alpha = 0.5f)
        )
        Text(
            "Be the first to reach ${tier.name}!",
            fontFamily = Oswald,
            fontSize = 14.sp,
            color = LightText.copy(alpha = 0.3f)
        )
    }
}

@Composable
fun LeaderboardList(entries: List<FirestoreManager.LeaderboardEntry>, tier: TrophyTier) {
    LazyColumn(
        contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)
    ) {
        // Table Header
        item {
            Row(Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
                Text("#", Modifier.width(40.dp), color = LightText.copy(0.5f), fontSize = 12.sp)
                Text("PLAYER", Modifier.weight(1f), color = LightText.copy(0.5f), fontSize = 12.sp)
                Text("SCORE", color = LightText.copy(0.5f), fontSize = 12.sp)
            }
        }

        itemsIndexed(entries) { index, entry ->
            LeaderboardRow(rank = index + 1, entry = entry, tier = tier)
        }
    }
}

@Composable
fun LeaderboardRow(rank: Int, entry: FirestoreManager.LeaderboardEntry, tier: TrophyTier) {
    val isTopThree = rank <= 3
    val rankColor = when (rank) {
        1 -> Color(0xFFFFD700) // Gold
        2 -> Color(0xFFC0C0C0) // Silver
        3 -> Color(0xFFCD7F32) // Bronze
        else -> LightText
    }

    val backgroundColor = if (isTopThree) DeepBlue.copy(alpha = 0.6f) else DeepBlue.copy(alpha = 0.3f)

    Card(
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(0.dp) // Flat look
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // RANK COLUMN
            Box(
                modifier = Modifier.width(40.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = "#$rank",
                    fontFamily = Oswald,
                    fontWeight = FontWeight.Bold,
                    fontSize = if (isTopThree) 18.sp else 16.sp,
                    color = rankColor
                )
            }

            // PLAYER INFO COLUMN
            Column(Modifier.weight(1f)) {
                val nameToDisplay = if (entry.playerName.isNotBlank()) entry.playerName else entry.userId.take(8)

                Text(
                    text = nameToDisplay,
                    fontFamily = Oswald,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    color = LightText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                val dateStr = entry.updatedAt?.toDate()?.let {
                    SimpleDateFormat("MMM dd", Locale.getDefault()).format(it)
                } ?: ""

                Text(
                    text = dateStr,
                    fontSize = 12.sp,
                    color = LightText.copy(alpha = 0.5f)
                )
            }

            // SCORE COLUMN
            Text(
                text = NumberFormat.getIntegerInstance().format(entry.score),
                fontFamily = Oswald,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = if(isTopThree) rankColor else LightText,
                textAlign = TextAlign.End
            )
        }
    }
}

// ---------------------------------------------------------
// HELPERS (Kept exactly as provided)
// ---------------------------------------------------------

fun trophyForIndex(index: Int): TrophyTier = when (index) {
    0 -> TrophyTier.UNRANKED
    1 -> TrophyTier.BRONZE
    2 -> TrophyTier.SILVER
    3 -> TrophyTier.GOLD
    4 -> TrophyTier.PLATINUM
    5 -> TrophyTier.DIAMOND
    6 -> TrophyTier.ELITE
    else -> TrophyTier.UNRANKED
}

fun trophyColorForIndex(index: Int) = trophyColorForTier(trophyForIndex(index))

fun trophyColorForTier(tier: TrophyTier): Color = when (tier) {
    TrophyTier.UNRANKED -> LightText
    TrophyTier.BRONZE -> Color(0xFFCD7F32)
    TrophyTier.SILVER -> Color(0xFFC0C0C0)
    TrophyTier.GOLD -> Pink_Jackie
    TrophyTier.PLATINUM -> Color(0xFFE5E4E2)
    TrophyTier.DIAMOND -> Color(0xFF0FF0FC)
    TrophyTier.ELITE -> Color(0xFFFFD700)
}



@Preview(
    name = "Tablet – Portrait",
    showBackground = true,
    showSystemUi = true,
    device = "spec:width=800dp,height=1280dp,dpi=480"
)
@Composable
fun HighscoreScreenPreview() {
    HighscoreScreen(onBack = {})
}
