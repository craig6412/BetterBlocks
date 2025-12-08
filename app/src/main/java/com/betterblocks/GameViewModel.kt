package com.betterblocks

import android.app.Application
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.betterblocks.animation.LineClearAnimator
import com.betterblocks.animation.ScoreAnimator
import com.betterblocks.model.FloatingScorePopup
import com.betterblocks.model.TrophyTier
import com.betterblocks.model.getPlayerTier
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.random.Random
import com.betterblocks.KEY_SAVED_SELECTED
import com.betterblocks.KEY_HIGH_SCORE



// --- Constants ---
const val GRID_SIZE = 9
const val BLOCKS_PER_ROUND = 3

// Animation Timing Constants
const val BLOCK_CLEAR_DELAY_MS = LineClearAnimator.SWEEP_DURATION.toLong()
const val ANIMATION_DURATION_MS = (LineClearAnimator.SWEEP_DURATION + LineClearAnimator.PARTICLE_FADE_DURATION).toLong()

// Color Wipe Animation Speed Control
// Set this to slow down the color wipe animation (2.0 = twice as slow, 1.5 = 50% slower, etc.)
const val COLOR_WIPE_ANIMATION_SPEED_MULTIPLIER = 2.0f  // ← ADJUST THIS to fine-tune color wipe animation speed

const val COIN_REWARD_THRESHOLD = 1000 // Points needed for reward
const val COINS_PER_REWARD = 10 // Coins given per threshold
const val ROTATION_COST = 10 // Cost to rotate a block
const val INITIAL_FREE_ROTATIONS = 3
const val RAINBOW_BLOCK_SCORE = 1000 // Special score for board wipe

const val INITIAL_RAINBOW_COUNT = 3 // Standard start count
const val SPECIAL_METER_MAX = 50 // Meter fills in 5 combo steps

// --- SHOP COSTS ---
const val RAINBOW_WIPE_COST = 1000 // Coins to buy a Rainbow Wipe
const val COLOR_WIPE_COST = 75 // Coins to buy a Color Wipe

// --- DEVELOPER OVERRIDES ---
private const val DEV_INITIAL_COINS = 1500 // $9,999 for development
private const val DEV_INITIAL_RAINBOW = 10 // 99 blocks for development
private const val DEV_INITIAL_COLOR_WIPE = 15 // Start with 5 Color Wipes

// Persistence Keys

const val KEY_HIGH_SCORE = "high_score"
const val KEY_COINS = "user_coins"
const val KEY_RAINBOW_COUNT = "user_rainbow_count"
const val KEY_COLOR_WIPE_COUNT = "user_color_wipe_count"

const val KEY_MUSIC_ENABLED = "music_enabled"
const val KEY_SAVED_BOARD = "saved_board"
const val KEY_SAVED_BLOCKS = "saved_blocks"
const val KEY_SAVED_SCORE = "saved_score"
const val KEY_SAVED_SELECTED = "saved_selected"
const val KEY_SAVED_METER = "saved_meter"
const val KEY_FREE_ROTATIONS = "free_rotations"
const val KEY_LAST_ROTATED_ID = "last_rotated_id"
const val KEY_IS_GAME_OVER = "is_game_over"
const val KEY_IS_LAST_CHANCE = "is_last_chance"

const val KEY_TROPHY_TIER = "trophy_tier"

// Distinguish between simple taps and drag initiation on a block.
enum class InteractionType {
    TAP,
    DRAG_START
}

class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val scoreAnimator = ScoreAnimator(viewModelScope)
    private var zeroCoinsShown = prefs.getBoolean("zero_coins_shown", false)
    private var firebaseUserId: String = "UNKNOWN"
    private var uidJob = viewModelScope.launch {
        runCatching { AuthManager.getOrCreateUserId(getApplication()) }
            .onSuccess { firebaseUserId = it }
    }

    // --- State Management ---
    private val _uiState = MutableStateFlow(createInitialState())
    val uiState: StateFlow<GameUiState> = _uiState

    // Internal tracking to trigger the animation only once per session/high score beat
    private var scoreToBeat: Int = 0

    /**
     * Creates the initial state for a new game, loading saved data.
     */
    private fun createInitialState(): GameUiState {
        val savedHighScore = prefs.getInt(KEY_HIGH_SCORE, 0)
        val savedCoins = prefs.getInt(KEY_COINS, DEV_INITIAL_COINS)
        val lifetimeCoins = prefs.getInt(KEY_LIFETIME_COINS, savedCoins)

        val savedTrophyTier = getPlayerTier(savedHighScore, lifetimeCoins, prefs)
        val savedRainbowCount = prefs.getInt(KEY_RAINBOW_COUNT, DEV_INITIAL_RAINBOW)
        val savedColorWipeCount = prefs.getInt(KEY_COLOR_WIPE_COUNT, DEV_INITIAL_COLOR_WIPE)
        val savedSound = prefs.getBoolean(KEY_SOUND_ENABLED, true)
        val savedMusic = prefs.getBoolean(KEY_MUSIC_ENABLED, true)

        // NEW — Load board JSON if it exists
        val boardJson = prefs.getString(KEY_SAVED_BOARD, null)
        val savedBoard = if (boardJson != null) {
            BoardSerializer.fromJson(boardJson)
        } else {
            Array(GRID_SIZE) { Array<Int?>(GRID_SIZE) { null } }
        }

        val blocksJson = prefs.getString(KEY_SAVED_BLOCKS, null)
        val restoredBlocks =
            blocksJson?.let { runCatching { BlockSerializer.blocksFromJson(it) }.getOrNull() }
        val blocksAreValid = areBlocksRestorable(restoredBlocks)
        val finalBlocks = if (blocksAreValid) restoredBlocks!! else generateNewBlocks()
        if (!blocksAreValid) {
            saveBlocks(finalBlocks)
        }

        val savedScore = prefs.getInt(KEY_SAVED_SCORE, 0)
        val savedMeter = prefs.getInt(KEY_SAVED_METER, 0)
        val savedSelectedId = prefs.getInt(KEY_SAVED_SELECTED, -1).takeIf { it != -1 }
        val savedFreeRotations = prefs.getInt(KEY_FREE_ROTATIONS, INITIAL_FREE_ROTATIONS)
        val savedLastRotatedId = prefs.getInt(KEY_LAST_ROTATED_ID, -1).takeIf { it != -1 }
        val savedIsGameOver = prefs.getBoolean(KEY_IS_GAME_OVER, false)
        val savedIsLastChance =
            if (savedIsGameOver) false else prefs.getBoolean(KEY_IS_LAST_CHANCE, false)
        val restoredSelectedBlock = if (!savedIsGameOver) {
            restoreSelectedBlock(savedSelectedId, finalBlocks, savedRainbowCount > 0)
        } else null
        if (savedSelectedId != null && restoredSelectedBlock == null) {
            saveSelectedBlockId(null)
        }

        scoreToBeat = savedHighScore

        // Restore selected block if possible
        return GameUiState(
            board = savedBoard,
            availableBlocks = finalBlocks,
            score = savedScore,
            highScore = savedHighScore,
            coins = savedCoins,
            trophyTier = savedTrophyTier,
            freeRotations = savedFreeRotations,
            lastRotatedBlockId = savedLastRotatedId,
            isGameOver = savedIsGameOver,
            isLastChance = savedIsLastChance,
            selectedBlock = restoredSelectedBlock,
            clearingCells = emptySet(),
            showHighScoreAnim = false,
            rainbowBlockCount = savedRainbowCount,
            specialMeterValue = savedMeter,
            colorWipeCount = savedColorWipeCount,
            isSoundEnabled = savedSound,
            isMusicEnabled = savedMusic
        )
    }

    /**
     * Generates new preview blocks using the Smart Preview Generator.
     *
     * This intelligently adapts to the current board state:
     * - Normal gameplay: Returns 3 random fitting blocks
     * - Crowded board: Prioritizes smaller blocks that fit
     * - Critical state: Guarantees tiny blocks for recovery
     *
     * @return List of 3 blocks optimized for current board state
     */
    private fun generateNewBlocks(): List<Block> {
        return generateSmartPreview(
            board = _uiState.value.board,
            allBlocks = BLOCK_MANAGER
        )
    }

    // --- Persistence Helpers ---

    private fun saveHighScore(score: Int) {
        prefs.edit().putInt(KEY_HIGH_SCORE, score).apply()
    }

    private fun saveCoins(coins: Int) {
        prefs.edit().putInt(KEY_COINS, coins).apply()
    }

    private fun saveLifetimeCoinsIfHigher(totalCoins: Int) {
        val previous = prefs.getInt(KEY_LIFETIME_COINS, totalCoins)
        if (totalCoins > previous) {
            prefs.edit().putInt(KEY_LIFETIME_COINS, totalCoins).apply()
        }
    }

    private fun saveRainbowCount(count: Int) {
        prefs.edit().putInt(KEY_RAINBOW_COUNT, count).apply()
    }

    private fun saveColorWipeCount(count: Int) {
        prefs.edit().putInt(KEY_COLOR_WIPE_COUNT, count).apply()
    }

    private fun saveSoundEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SOUND_ENABLED, enabled).apply()
    }

    private fun saveMusicEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_MUSIC_ENABLED, enabled).apply()
    }

    private fun clearSavedGameSnapshot() {
        prefs.edit().apply {
            remove(KEY_SAVED_BOARD)
            remove(KEY_SAVED_BLOCKS)
            remove(KEY_SAVED_SCORE)
            remove(KEY_SAVED_SELECTED)
            remove(KEY_SAVED_METER)
            remove(KEY_FREE_ROTATIONS)
            remove(KEY_LAST_ROTATED_ID)
            remove(KEY_IS_GAME_OVER)
            remove(KEY_IS_LAST_CHANCE)
        }.apply()
        zeroCoinsShown = false
    }

    private fun saveBoard(board: Array<Array<Int?>>) {
        val json = BoardSerializer.toJson(board)
        prefs.edit().putString(KEY_SAVED_BOARD, json).apply()
    }

    private fun saveBlocks(blocks: List<Block>) {
        val json = BlockSerializer.blocksToJson(blocks)
        prefs.edit().putString(KEY_SAVED_BLOCKS, json).apply()
    }

    private fun saveSelectedBlockId(id: Int?) {
        prefs.edit().putInt(KEY_SAVED_SELECTED, id ?: -1).apply()
    }

    private fun saveTrophyTier(tier: TrophyTier) {
        prefs.edit().putString(KEY_TROPHY_TIER, tier.name).apply()
    }

    private fun persistGameSnapshot(
        board: GameGrid,
        blocks: List<Block>,
        coins: Int,
        rainbowCount: Int,
        colorWipeCount: Int,
        score: Int,
        meterValue: Int,
        freeRotations: Int,
        lastRotatedBlockId: Int?,
        selectedBlockId: Int?,
        isGameOver: Boolean,
        isLastChance: Boolean
    ) {
        saveBoard(board)
        saveBlocks(blocks)
        saveCoins(coins)
        saveRainbowCount(rainbowCount)
        saveColorWipeCount(colorWipeCount)
        prefs.edit()
            .putInt(KEY_SAVED_SCORE, score)
            .putInt(KEY_SAVED_METER, meterValue)
            .putInt(KEY_FREE_ROTATIONS, freeRotations)
            .putInt(KEY_LAST_ROTATED_ID, lastRotatedBlockId ?: -1)
            .putInt(KEY_SAVED_SELECTED, selectedBlockId ?: -1)
            .putBoolean(KEY_IS_GAME_OVER, isGameOver)
            .putBoolean(KEY_IS_LAST_CHANCE, isLastChance)
            .apply()
    }

    private fun areBlocksRestorable(blocks: List<Block>?): Boolean {
        if (blocks == null) return false
        val validIds = BLOCK_MANAGER.map { it.id }.toHashSet()
        return blocks.all { it.isSpecial || validIds.contains(it.id) }
    }

    private fun restoreSelectedBlock(
        savedId: Int?,
        availableBlocks: List<Block>,
        hasRainbowBlock: Boolean
    ): Block? {
        if (savedId == null) return null
        if (hasRainbowBlock && savedId == RAINBOW_BLOCK.id) return RAINBOW_BLOCK
        return availableBlocks.firstOrNull { it.id == savedId }
    }

    // --- Persistence Debugging ---

    fun clearPrefsForDebugging() {
        prefs.edit().clear().apply()
    }

    /**
     * Debug function: Reset daily reward so it shows again
     * Call this from Developer Screen to test the daily reward dialog
     */
    fun resetDailyRewardForTesting() {
        prefs.edit()
            .remove(KEY_DAILY_REWARD_DATE)
            .remove(KEY_DAILY_STREAK)
            .apply()
    }

    // --- UI Actions ---

    fun toggleSound() {
        val newState = !_uiState.value.isSoundEnabled
        saveSoundEnabled(newState)
        _uiState.update { it.copy(isSoundEnabled = newState) }
    }

    fun toggleMusic() {
        val newState = !_uiState.value.isMusicEnabled
        saveMusicEnabled(newState)
        _uiState.update { it.copy(isMusicEnabled = newState) }
    }

    fun dismissRainbowEarnedDialog() {
        _uiState.update { it.copy(showRainbowEarnedDialog = false) }
    }

    fun dismissPurchaseSuccessDialog() {
        _uiState.update { it.copy(showPurchaseSuccessDialog = false, purchaseCoinsAwarded = 0) }
    }

    fun clearCoinEarnedAnimation() {
        _uiState.update { it.copy(coinsEarnedThisUpdate = 0) }
    }

    fun dismissShopPurchaseBubble() {
        _uiState.update { it.copy(showShopPurchaseBubble = false, shopPurchaseMessage = "") }
    }

    fun restartGame() {
        clearSavedGameSnapshot()
        saveSelectedBlockId(null)
        scoreToBeat = prefs.getInt(KEY_HIGH_SCORE, 0)
        _uiState.value = createInitialState()
        resetGameTracking()
    }

    private fun resetGameTracking() {
        // Minimal no-op implementation to satisfy references.
        // If you had custom tracking (streaks, analytics), restore it here.
    }

    /**
     * Called when a player achieves a combo clear of 2 or more lines.
     */
    private fun onSpecialMeterFilled(currentRainbowCount: Int, currentMeterValue: Int) {
        if (currentMeterValue + 1 >= SPECIAL_METER_MAX) {
            // Award block and reset meter
            val newCount = currentRainbowCount + 1
            saveRainbowCount(newCount)
            _uiState.update {
                it.copy(
                    rainbowBlockCount = newCount,
                    specialMeterValue = 0,
                    showRainbowEarnedDialog = true  // Show celebration dialog
                )
            }
        } else {
            // Fill meter
            _uiState.update {
                it.copy(
                    specialMeterValue = currentMeterValue + 1
                )
            }
        }
    }

    /**
     * Triggered when the Color Wheel stops spinning.
     */
    fun onColorWipeSpinResult(colorIndex: Int) {
        val currentState = _uiState.value
        if (currentState.colorWipeCount <= 0) return

        // 1. Deduct Inventory immediately
        val newCount = currentState.colorWipeCount - 1
        saveColorWipeCount(newCount)
        _uiState.update { it.copy(colorWipeCount = newCount) }
        saveSelectedBlockId(_uiState.value.selectedBlock?.id)

        // 2. Find all matching blocks
        val targetDrawableId = BLOCK_DRAWABLES[colorIndex]
        val cellsToClear = mutableSetOf<Coord>()
        for (r in 0 until GRID_SIZE) {
            for (c in 0 until GRID_SIZE) {
                if (currentState.board[r][c] == targetDrawableId) {
                    cellsToClear.add(Coord(r, c))
                }
            }
        }

        viewModelScope.launch {
            if (cellsToClear.isNotEmpty()) {
                // 3. CRITICAL FIX: Keep the board state unchanged during animation
                // The board already has the blocks, so we just need to set effectCells
                // and isColorWipeAnimating flag WITHOUT changing the board
                _uiState.update {
                    it.copy(
                        board = currentState.board,  // Keep the original board with blocks visible
                        effectCells = cellsToClear,
                        isColorWipeAnimating = true
                    )
                }

                // Use slower animation duration for color wipe
                val colorWipeDelay =
                    (BLOCK_CLEAR_DELAY_MS * COLOR_WIPE_ANIMATION_SPEED_MULTIPLIER).toLong()
                delay(colorWipeDelay)

                // 4. Remove from board after animation
                val newBoard = currentState.board.map { it.clone() }.toTypedArray()
                for (cell in cellsToClear) {
                    newBoard[cell.row][cell.col] = null
                }

                // 5. Score: 50 points per block
                val points = cellsToClear.size * 50

                updateScoreAndState(
                    currentState = currentState,
                    newAvailableBlocks = currentState.availableBlocks,
                    finalBlocks = currentState.availableBlocks,
                    pointsToAdd = points,
                    newBoard = newBoard,
                    clearSelection = true
                )
                // Clear effect layer and color wipe flag after state update
                _uiState.update { it.copy(effectCells = emptySet(), isColorWipeAnimating = false) }
            }
        }
    }

    fun onLastChanceUsed() {
        val currentState = _uiState.value
        if (currentState.rainbowBlockCount <= 0) return
        val newCount = currentState.rainbowBlockCount - 1
        val updatedState = currentState.copy(
            isLastChance = false,
            selectedBlock = RAINBOW_BLOCK,
            rainbowBlockCount = newCount
        )
        persistGameSnapshot(
            board = updatedState.board,
            blocks = updatedState.availableBlocks,
            coins = updatedState.coins,
            rainbowCount = updatedState.rainbowBlockCount,
            colorWipeCount = updatedState.colorWipeCount,
            score = updatedState.score,
            meterValue = updatedState.specialMeterValue,
            freeRotations = updatedState.freeRotations,
            lastRotatedBlockId = updatedState.lastRotatedBlockId,
            selectedBlockId = updatedState.selectedBlock?.id,
            isGameOver = updatedState.isGameOver,
            isLastChance = updatedState.isLastChance
        )
        _uiState.update { updatedState }
    }

    fun onLastChanceDeclined() {
        val currentState = _uiState.value
        val updatedState =
            currentState.copy(isLastChance = false, isGameOver = true, selectedBlock = null)
        persistGameSnapshot(
            board = updatedState.board,
            blocks = updatedState.availableBlocks,
            coins = updatedState.coins,
            rainbowCount = updatedState.rainbowBlockCount,
            colorWipeCount = updatedState.colorWipeCount,
            score = updatedState.score,
            meterValue = updatedState.specialMeterValue,
            freeRotations = updatedState.freeRotations,
            lastRotatedBlockId = updatedState.lastRotatedBlockId,
            selectedBlockId = null,
            isGameOver = updatedState.isGameOver,
            isLastChance = updatedState.isLastChance
        )
        _uiState.update { updatedState }
    }

    fun useRainbowWipeImmediately() {
        val currentState = _uiState.value
        if (currentState.isGameOver || currentState.isLastChance || currentState.rainbowBlockCount <= 0) return

        viewModelScope.launch {
            val newRainbowCount = currentState.rainbowBlockCount - 1
            saveRainbowCount(newRainbowCount)
            _uiState.update { it.copy(selectedBlock = null, rainbowBlockCount = newRainbowCount) }
            saveSelectedBlockId(null)

            // NEW: full-board rainbow wipe using line clear animation
            handleFullBoardRainbowWipe(currentState.copy(rainbowBlockCount = newRainbowCount))
        }
    }

    /**
     * Full-board Rainbow Wipe: mark all occupied cells as clearing, run animation, then clear board.
     */
    private fun handleFullBoardRainbowWipe(state: GameUiState) {
        val occupiedCells = mutableSetOf<Coord>()
        for (r in 0 until GRID_SIZE) {
            for (c in 0 until GRID_SIZE) {
                if (state.board[r][c] != null) {
                    occupiedCells.add(Coord(r, c))
                }
            }
        }

        // No occupied cells, nothing to animate or clear
        if (occupiedCells.isEmpty()) {
            return
        }

        viewModelScope.launch {
            // 1) Expose clearing cells for the LineClearAnimator / Rainbow wipe visuals
            _uiState.update {
                it.copy(
                    clearingCells = occupiedCells,
                    isRainbowWipeActive = true
                )
            }

            // 2) Wait for the line clear animation duration
            delay(ANIMATION_DURATION_MS)

            // 3) Actually clear the board and reset animation flags
            val clearedBoard = Array(GRID_SIZE) { Array<Int?>(GRID_SIZE) { null } }
            val points = occupiedCells.size * 50

            updateScoreAndState(
                currentState = _uiState.value,
                newAvailableBlocks = _uiState.value.availableBlocks,
                finalBlocks = _uiState.value.availableBlocks,
                pointsToAdd = points,
                newBoard = clearedBoard,
                clearSelection = true
            )

            _uiState.update {
                it.copy(
                    clearingCells = emptySet(),
                    isRainbowWipeActive = false
                )
            }
        }
    }

    private fun handleRainbowWipe(block: Block, row: Int, col: Int, state: GameUiState) {
        val board = state.board
        if (row !in 0 until GRID_SIZE || col !in 0 until GRID_SIZE) return

        val newBoard = board.map { it.clone() }.toTypedArray()
        var cleared = 0
        for (c in 0 until GRID_SIZE) {
            if (newBoard[row][c] != null) {
                newBoard[row][c] = null
                cleared++
            }
        }
        for (r in 0 until GRID_SIZE) {
            if (newBoard[r][col] != null) {
                newBoard[r][col] = null
                cleared++
            }
        }
        val points = cleared * 50
        updateScoreAndState(
            currentState = state,
            newAvailableBlocks = state.availableBlocks,
            finalBlocks = state.availableBlocks,
            pointsToAdd = points,
            newBoard = newBoard,
            clearSelection = true
        )
    }

    private fun updateScoreAndState(
        currentState: GameUiState,
        newAvailableBlocks: List<Block>,
        finalBlocks: List<Block>,
        pointsToAdd: Int,
        newBoard: GameGrid,
        clearSelection: Boolean
    ) {
        // Minimal safe implementation: update board, availableBlocks, score, and optionally clear selection.
        _uiState.update {
            it.copy(
                board = newBoard,
                availableBlocks = finalBlocks,
                score = it.score + pointsToAdd,
                selectedBlock = if (clearSelection) null else it.selectedBlock
            )
        }
        // Persist a snapshot with updated score/board/blocks.
        val updated = _uiState.value
        persistGameSnapshot(
            board = updated.board,
            blocks = updated.availableBlocks,
            coins = updated.coins,
            rainbowCount = updated.rainbowBlockCount,
            colorWipeCount = updated.colorWipeCount,
            score = updated.score,
            meterValue = updated.specialMeterValue,
            freeRotations = updated.freeRotations,
            lastRotatedBlockId = updated.lastRotatedBlockId,
            selectedBlockId = updated.selectedBlock?.id,
            isGameOver = updated.isGameOver,
            isLastChance = updated.isLastChance
        )
    }

    // --- MAIN MENU / STATS HELPERS ---

    // Refresh user-facing stats when returning to main menu (placeholder; extend as needed)
    fun refreshUserStats() {
        val high = prefs.getInt(KEY_HIGH_SCORE, _uiState.value.highScore)
        val coins = prefs.getInt(KEY_COINS, _uiState.value.coins)
        _uiState.update { it.copy(highScore = high, coins = coins) }
    }

    // Daily reward check; currently just ensures dialog flags are consistent
    fun checkDailyReward() {
        // Hook your original daily reward logic back in here if needed.
        // For now this is a no-op that leaves existing uiState fields unchanged.
    }

    // Claim daily reward: apply whatever is in uiState.dailyReward* and hide dialog
    fun claimDailyReward() {
        val state = _uiState.value
        val newCoins = state.coins + state.dailyRewardCoins
        saveCoins(newCoins)
        _uiState.update {
            it.copy(
                coins = newCoins,
                showDailyRewardDialog = false,
                dailyRewardCoins = 0,
                dailyRewardRainbow = false
            )
        }
    }

    // Simple coin ad reward entry point (used by free coins button)
    fun addCoins(amount: Int) {
        val newCoins = _uiState.value.coins + amount
        saveCoins(newCoins)
        _uiState.update { it.copy(coins = newCoins) }
    }

    // Public API for rotation button
    fun rotateSelectedBlock() {
        val currentState = _uiState.value
        if (currentState.isGameOver || currentState.selectedBlock == null || currentState.isLastChance) return

        val currentBlock = currentState.selectedBlock

        var newCoins = currentState.coins
        var newFreeRotations = currentState.freeRotations
        val isAlreadyPaidFor = currentState.lastRotatedBlockId == currentBlock.id

        if (!isAlreadyPaidFor) {
            if (newFreeRotations > 0) {
                newFreeRotations -= 1
            } else if (newCoins >= ROTATION_COST) {
                newCoins -= ROTATION_COST
                saveCoins(newCoins)
            } else {
                return
            }
        }

        val rotatedBlock = currentBlock.rotate()
        val newAvailableBlocks = if (!currentBlock.isSpecial) {
            currentState.availableBlocks.map { if (it.id == currentBlock.id) rotatedBlock else it }
        } else {
            currentState.availableBlocks
        }

        val updatedState = currentState.copy(
            availableBlocks = newAvailableBlocks,
            selectedBlock = rotatedBlock,
            coins = newCoins,
            freeRotations = newFreeRotations,
            lastRotatedBlockId = currentBlock.id
        )
        _uiState.value = updatedState
        persistGameSnapshot(
            board = updatedState.board,
            blocks = updatedState.availableBlocks,
            coins = updatedState.coins,
            rainbowCount = updatedState.rainbowBlockCount,
            colorWipeCount = updatedState.colorWipeCount,
            score = updatedState.score,
            meterValue = updatedState.specialMeterValue,
            freeRotations = updatedState.freeRotations,
            lastRotatedBlockId = updatedState.lastRotatedBlockId,
            selectedBlockId = updatedState.selectedBlock?.id,
            isGameOver = updatedState.isGameOver,
            isLastChance = updatedState.isLastChance
        )
    }

    // Simple tap-based selection used by GameActivity / GameScreen
    fun selectBlock(block: Block) {
        val current = _uiState.value
        if (current.isGameOver || current.isLastChance) return
        val newSelected = if (current.selectedBlock == block) null else block
        _uiState.update { it.copy(selectedBlock = newSelected) }
        saveSelectedBlockId(newSelected?.id)
    }

    // Toggle the special Rainbow Wipe block selection from UI
    fun selectRainbowBlock() {
        val current = _uiState.value
        if (current.isLastChance || current.isGameOver || current.rainbowBlockCount <= 0) return
        val rainbow = RAINBOW_BLOCK
        val newSelected = if (current.selectedBlock?.id == rainbow.id) null else rainbow
        _uiState.update { it.copy(selectedBlock = newSelected) }
        saveSelectedBlockId(newSelected?.id)
    }

    // Dismiss tier promotion dialog (if used by UI)
    fun dismissTierPromotion() {
        _uiState.update { it.copy(showTierPromotionDialog = false, newlyUnlockedTier = null) }
    }

    // Core placement entry used by GameActivity / GameScreen
    fun placeBlock(row: Int, col: Int) {
        val current = _uiState.value
        val block = current.selectedBlock ?: return
        if (current.isGameOver || current.isLastChance) return
        if (block.isSpecial) {
            // Special blocks are handled via separate flows (rainbow/color wipe buttons)
            return
        }

        // Validate placement
        val board = current.board
        for (offset in block.shape) {
            val r = row + offset.row
            val c = col + offset.col
            if (r !in 0 until GRID_SIZE || c !in 0 until GRID_SIZE) return
            if (board[r][c] != null) return
        }

        // Apply block
        val newBoard = board.map { it.clone() }.toTypedArray()
        for (offset in block.shape) {
            val r = row + offset.row
            val c = col + offset.col
            newBoard[r][c] = block.colorResId
        }

        // Detect full rows/columns
        val rowsToClear = mutableSetOf<Int>()
        val colsToClear = mutableSetOf<Int>()
        for (r in 0 until GRID_SIZE) {
            if (newBoard[r].all { it != null }) rowsToClear.add(r)
        }
        for (c in 0 until GRID_SIZE) {
            var full = true
            for (r in 0 until GRID_SIZE) {
                if (newBoard[r][c] == null) { full = false; break }
            }
            if (full) colsToClear.add(c)
        }

        // Build animation clearing set for LineClearAnimator
        val cellsToClear = mutableSetOf<Coord>()
        rowsToClear.forEach { r -> for (c in 0 until GRID_SIZE) cellsToClear.add(Coord(r, c)) }
        colsToClear.forEach { c -> for (r in 0 until GRID_SIZE) cellsToClear.add(Coord(r, c)) }

        var finalBoard = newBoard
        var points = block.shape.size
        if (cellsToClear.isNotEmpty()) {
            finalBoard = newBoard.map { it.clone() }.toTypedArray()
            cellsToClear.forEach { coord -> finalBoard[coord.row][coord.col] = null }
            points += (rowsToClear.size + colsToClear.size) * 100
        }

        val remainingBlocks = current.availableBlocks.filter { it.id != block.id }
        val nextBlocks = if (remainingBlocks.isEmpty()) generateNewBlocks() else remainingBlocks

        _uiState.update {
            it.copy(
                board = finalBoard,
                availableBlocks = nextBlocks,
                selectedBlock = null,
                score = it.score + points,
                clearingCells = cellsToClear  // drive line-clear animation
            )
        }

        val updated = _uiState.value
        persistGameSnapshot(
            board = updated.board,
            blocks = updated.availableBlocks,
            coins = updated.coins,
            rainbowCount = updated.rainbowBlockCount,
            colorWipeCount = updated.colorWipeCount,
            score = updated.score,
            meterValue = updated.specialMeterValue,
            freeRotations = updated.freeRotations,
            lastRotatedBlockId = updated.lastRotatedBlockId,
            selectedBlockId = updated.selectedBlock?.id,
            isGameOver = updated.isGameOver,
            isLastChance = updated.isLastChance
        )
    }
}
