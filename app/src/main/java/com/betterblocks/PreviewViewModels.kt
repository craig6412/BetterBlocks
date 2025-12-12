package com.betterblocks

import androidx.lifecycle.ViewModel
import com.betterblocks.model.TrophyTier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
// Preview-only ViewModel implementations. Placed in the same package so they can access
// internal types/constants and be easily imported by preview functions.

class PreviewGameViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(
        GameUiState(
            board = Array(GRID_SIZE) { Array<Int?>(GRID_SIZE) { null } },
            availableBlocks = BLOCK_MANAGER.take(3),
            score = 123,
            highScore = 456,
            coins = 150, // use literal because DEV_INITIAL_COINS is private in GameViewModel
            freeRotations = INITIAL_FREE_ROTATIONS,
            lastRotatedBlockId = null,
            isGameOver = false,
            isLastChance = false,
            selectedBlock = BLOCK_MANAGER.firstOrNull(),
            clearingCells = emptySet(),
            showHighScoreAnim = false,
            rainbowBlockCount = INITIAL_RAINBOW_COUNT,
            colorWipeCount = 5, // use literal because DEV_INITIAL_COLOR_WIPE is private
            showFirstInstallFreeCoinsDialog = false,
            specialMeterValue = 10,
            isSoundEnabled = true,
            isMusicEnabled = true
        )
    )

    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    // No-op or simple implementations to satisfy preview usages
    fun checkDailyReward() {}
    fun claimDailyReward() { _uiState.value = _uiState.value.copy(showDailyRewardDialog = false) }
    fun addCoins(amount: Int) { _uiState.value = _uiState.value.copy(coins = _uiState.value.coins + amount) }
    fun dismissZeroCoinsDialog() { _uiState.value = _uiState.value.copy(showZeroCoinsDialog = false) }
    fun dismissPurchaseSuccessDialog() { _uiState.value = _uiState.value.copy(showPurchaseSuccessDialog = false) }
    // Add other small helpers as needed by previews; keep these lightweight and side-effect free.
}

// Preview stub for Shop-related UI
class PreviewShopViewModel : ViewModel() {
    // Minimal shop UI state can be represented via simple properties used by ShopScreen
    val coins: Int = 5000
    val purchasedTrophies = emptyList<String>()
    // Add functions expected by ShopScreen previews
    fun onPurchase(productId: String) {}
}

// Preview stub for HighScore/Leaderboard screens
class PreviewHighScoreViewModel : ViewModel() {
    val entries = listOf(
        FirestoreManager.LeaderboardEntry(
            userId = "user1",
            score = 9999,
            trophyTier = TrophyTier.GOLD.name,
            updatedAt = null,
            updatedAt_fallback = 0L,
            playerName = "AAA"
        ),
        FirestoreManager.LeaderboardEntry(
            userId = "user2",
            score = 8500,
            trophyTier = TrophyTier.SILVER.name,
            updatedAt = null,
            updatedAt_fallback = 0L,
            playerName = "BBB"
        )
    )
}

// Preview stub for Stats screen
class PreviewStatsViewModel : ViewModel() {
    val lifetimeCoins = 123456
    val bestScore = 7890
}

// Preview stub for Settings screen
class PreviewSettingsViewModel : ViewModel() {
    var isSoundEnabled = true
    var isMusicEnabled = true
}
