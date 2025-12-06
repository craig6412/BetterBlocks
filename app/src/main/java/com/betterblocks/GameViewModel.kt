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
        val restoredBlocks = blocksJson?.let { runCatching { BlockSerializer.blocksFromJson(it) }.getOrNull() }
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
        val savedIsLastChance = if (savedIsGameOver) false else prefs.getBoolean(KEY_IS_LAST_CHANCE, false)
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

    private fun restoreSelectedBlock(savedId: Int?, availableBlocks: List<Block>, hasRainbowBlock: Boolean): Block? {
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
                val colorWipeDelay = (BLOCK_CLEAR_DELAY_MS * COLOR_WIPE_ANIMATION_SPEED_MULTIPLIER).toLong()
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
        val updatedState = currentState.copy(isLastChance = false, isGameOver = true, selectedBlock = null)
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
            handleRainbowWipe(RAINBOW_BLOCK, 0, 0, currentState.copy(rainbowBlockCount = newRainbowCount))
        }
    }

    fun selectBlock(block: Block) {
        if (_uiState.value.isGameOver || _uiState.value.isLastChance) return

        val currentState = _uiState.value
        val newSelectedBlock = if (currentState.selectedBlock == block) null else block
        _uiState.update { it.copy(selectedBlock = newSelectedBlock) }
        saveSelectedBlockId(newSelectedBlock?.id)

        // Clear any preview highlights immediately when the selection changes
        updatePreviewClear(emptyList(), true)
    }

    // Public API used by UI - kept for backward compatibility
    fun onGridCellClicked(row: Int, col: Int) {
        placeBlock(row, col)
    }

    fun selectRainbowBlock() {
        val currentState = _uiState.value
        if (currentState.isLastChance || currentState.isGameOver || currentState.rainbowBlockCount <= 0) return

        val rainbowBlock = RAINBOW_BLOCK
        val newSelectedBlock = if (currentState.selectedBlock?.id == rainbowBlock.id) null else rainbowBlock
        _uiState.update { it.copy(selectedBlock = newSelectedBlock) }
        saveSelectedBlockId(newSelectedBlock?.id)

        // Clear any preview highlights immediately when the selection changes
        updatePreviewClear(emptyList(), true)
    }

    /**
     * Update previewed clear indices (rows or columns) during ghost-drag.
     * UI layer should call this when hovering a ghost placement and also
     * call with empty list on drag end.
     */
    fun updatePreviewClear(rowsOrCols: List<Int>, isRow: Boolean) {
        _uiState.update { it.copy(previewClearIndices = rowsOrCols, previewIsRow = isRow) }
    }

    fun rotateSelectedBlock() {
        val currentState = _uiState.value
        if (currentState.isGameOver || currentState.selectedBlock == null || currentState.isLastChance) return

        val currentBlock = currentState.selectedBlock!!

        // --- ROTATION COST LOGIC ---
        var newCoins = currentState.coins
        var newFreeRotations = currentState.freeRotations

        // Check if we already paid for this block (Unlimited rotations for THIS block instance)
        val isAlreadyPaidFor = currentState.lastRotatedBlockId == currentBlock.id

        if (!isAlreadyPaidFor) {
            if (newFreeRotations > 0) {
                newFreeRotations -= 1
            } else if (newCoins >= ROTATION_COST) {
                newCoins -= ROTATION_COST
                saveCoins(newCoins) // Save new coin balance
            } else {
                // Not enough resources to rotate
                return
            }
        }
        // --- ROTATION LOGIC END ---

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

    fun placeBlock(row: Int, col: Int) {
        val currentState = _uiState.value
        if (currentState.isGameOver || currentState.selectedBlock == null) return

        val block = currentState.selectedBlock!!
        if (!canPlaceBlock(block, row, col, currentState.board)) return

        if (block.isSpecial) {
            viewModelScope.launch { handleRainbowWipe(block, row, col, currentState) }
            return
        }

        val newBoard = placeBlockOnBoard(currentState.board, block, row, col)
        val remainingBlocks = currentState.availableBlocks.filter { it.id != block.id }
        // Removed base placement scoring (was block.shape.size)
        val placementPoints = 0

        val clearResult = checkForClears(newBoard)
        val totalClears = clearResult.totalClears
        val boardAfterClears = clearResult.newBoard

        viewModelScope.launch {
            if (totalClears > 0) {
                val clearingCoords = findClearingCells(newBoard, boardAfterClears)

                // CRITICAL FIX: Update the board to show the newly placed block BEFORE the animation
                // This ensures the full block is visible, and only the clearing cells will animate away
                _uiState.update { it.copy(board = newBoard, effectCells = clearingCoords) }

                delay(BLOCK_CLEAR_DELAY_MS)
            }

            val lineScore = calculateLineClearScore(totalClears)
            val totalPoints = placementPoints + lineScore // effectively lineScore only
            val comboCount = if (totalClears > 0) (_uiState.value.scoreState.comboCount + 1) else 0

            updateScoreAndState(
                currentState = currentState,
                newAvailableBlocks = remainingBlocks,
                finalBlocks = if (remainingBlocks.isEmpty()) generateNewBlocks() else remainingBlocks,
                pointsToAdd = totalPoints,
                newBoard = boardAfterClears,
                totalClears = totalClears,
                clearSelection = true,
                comboCount = comboCount
            )

            if (totalClears > 0) {
                onSpecialMeterFilled(currentState.rainbowBlockCount, currentState.specialMeterValue)
                // Clear the effect layer after the animation and state update
                _uiState.update { it.copy(effectCells = emptySet()) }
            }
        }
    }

    /**
     * Logic for the Rainbow Block: Visual placement, Full Board Wipe animation, then Clear.
     */
    private suspend fun handleRainbowWipe(block: Block, startRow: Int, startCol: Int, currentState: GameUiState) {
        // 1. Create temporary board filled with Rainbow Texture
        val boardWithRainbowOverlay = Array(GRID_SIZE) { Array<Int?>(GRID_SIZE) { null } }
        val allOccupiedCells = mutableSetOf<Coord>()

        for (r in 0 until GRID_SIZE) {
            for (c in 0 until GRID_SIZE) {
                boardWithRainbowOverlay[r][c] = block.colorResId
                allOccupiedCells.add(Coord(r, c))
            }
        }

        // 2. Update UI to trigger cascade animation
        _uiState.update {
            it.copy(
                board = boardWithRainbowOverlay,
                effectCells = allOccupiedCells, // Use effect layer for rainbow wipe
                isRainbowWipeActive = true
            )
        }

        if (currentState.isSoundEnabled) {
            SoundManager.playRainbowClear()
        }

        // Haptic feedback for rainbow wipe
        HapticManager.vibrateHeavy(getApplication())

        // 3. Wait for animation
        delay(BLOCK_CLEAR_DELAY_MS)

        // 4. Clear the board
        val emptyBoard = Array(GRID_SIZE) { Array<Int?>(GRID_SIZE) { null } }

        updateScoreAndState(
            currentState = currentState,
            newAvailableBlocks = currentState.availableBlocks,
            finalBlocks = currentState.availableBlocks,
            pointsToAdd = RAINBOW_BLOCK_SCORE,
            newBoard = emptyBoard,
            newRainbowCount = currentState.rainbowBlockCount,
            clearSelection = true
        )
        // Clear effect layer after state update
        _uiState.update { it.copy(effectCells = emptySet()) }
    }

    /**
     * Consolidated logic for updating scores, high scores, coins, and game over state.
     */
    fun updateScoreAndState(
        currentState: GameUiState,
        newAvailableBlocks: List<Block>,
        finalBlocks: List<Block>,
        pointsToAdd: Int,
        newBoard: GameGrid,
        newRainbowCount: Int? = null,
        totalClears: Int = 0,
        clearSelection: Boolean = false,
        comboCount: Int = 0
    ) {
        val newScore = currentState.score + pointsToAdd
        val lifetimeCoinsBeforeUpdate = prefs.getInt(KEY_LIFETIME_COINS, currentState.coins)

        // Coins calculation first so we can use updated numbers for tier logic
        val scoreBefore = currentState.score
        val coinsEarned = ((newScore / COIN_REWARD_THRESHOLD) - (scoreBefore / COIN_REWARD_THRESHOLD)) * COINS_PER_REWARD
        val newTotalCoins = currentState.coins + coinsEarned
        if (coinsEarned > 0) {
            saveCoins(newTotalCoins)
            saveLifetimeCoinsIfHigher(newTotalCoins)
        }
        val lifetimeCoins = max(lifetimeCoinsBeforeUpdate, prefs.getInt(KEY_LIFETIME_COINS, newTotalCoins))

        val newTrophy = getPlayerTier(
            bestScore = max(newScore, currentState.highScore),
            coins = lifetimeCoins,
            prefs = prefs
        )

        if (newTrophy != currentState.trophyTier) {
            saveTrophyTier(newTrophy)

            _uiState.update {
                it.copy(
                    showTierPromotionDialog = true,
                    newlyUnlockedTier = newTrophy
                )
            }

            // Haptic feedback for tier unlock
            HapticManager.vibrateHeavy(getApplication())
        }
        // High Score Check
        var currentHighScore = currentState.highScore
        var triggerHighScoreAnim = false
        if (newScore > scoreToBeat) {
            currentHighScore = newScore
            saveHighScore(currentHighScore)

            if (!currentState.showHighScoreAnim && scoreToBeat != Int.MAX_VALUE) {
                triggerHighScoreAnim = true
                scoreToBeat = Int.MAX_VALUE
            }
            submitScoreToLeaderboard(newScore, newTrophy)
        } else if (newScore > currentHighScore) {
            currentHighScore = newScore
            saveHighScore(currentHighScore)
            submitScoreToLeaderboard(newScore, newTrophy)
        }

        if (newTotalCoins == 0 && !zeroCoinsShown) {
            zeroCoinsShown = true
            prefs.edit().putBoolean("zero_coins_shown", true).apply()
        }
        val showZeroCoinsDialog = zeroCoinsShown && newTotalCoins == 0

        val hasRainbowBlock = (newRainbowCount ?: currentState.rainbowBlockCount) > 0
        val isHardGameOver = !isMovePossible(finalBlocks, newBoard)
        var triggerLastChance = false
        var finalIsGameOver = false

        if (isHardGameOver) {
            if (hasRainbowBlock) {
                triggerLastChance = true
            } else {
                finalIsGameOver = true
                handleGameOver()
            }
        }

        // Track lines cleared for game summary
        if (totalClears > 0) {
            trackLineClears(totalClears)
        }

        val currentRainbowCount = newRainbowCount ?: currentState.rainbowBlockCount
        var newMeterValue = currentState.specialMeterValue
        var finalRainbowCount = currentRainbowCount



        // Meter Logic
        if (totalClears >= 2) {
            onSpecialMeterFilled(currentRainbowCount, newMeterValue)
            // Refetch updated state
            val finalStateAfterMeterCheck = _uiState.value
            newMeterValue = finalStateAfterMeterCheck.specialMeterValue
            finalRainbowCount = finalStateAfterMeterCheck.rainbowBlockCount
        }

        if (finalRainbowCount != currentState.rainbowBlockCount) {
            saveRainbowCount(finalRainbowCount)
        }

        persistGameSnapshot(
            board = newBoard,
            blocks = finalBlocks,
            coins = newTotalCoins,
            rainbowCount = finalRainbowCount,
            colorWipeCount = _uiState.value.colorWipeCount,
            score = newScore,
            meterValue = newMeterValue,
            freeRotations = _uiState.value.freeRotations,
            lastRotatedBlockId = currentState.lastRotatedBlockId,
            selectedBlockId = if (clearSelection) null else currentState.selectedBlock?.id,
            isGameOver = finalIsGameOver,
            isLastChance = triggerLastChance
        )

        _uiState.update {
            it.copy(
                board = newBoard,
                availableBlocks = finalBlocks,
                score = newScore,
                highScore = currentHighScore,
                coins = newTotalCoins,
                isGameOver = finalIsGameOver,
                isLastChance = triggerLastChance,
                selectedBlock = if (clearSelection) null else currentState.selectedBlock,
                lastRotatedBlockId = currentState.lastRotatedBlockId,
                clearingCells = emptySet(),
                isRainbowWipeActive = false,
                showHighScoreAnim = triggerHighScoreAnim,
                rainbowBlockCount = finalRainbowCount,
                specialMeterValue = newMeterValue,
                showZeroCoinsDialog = showZeroCoinsDialog,
                trophyTier = newTrophy,
                coinsEarnedThisUpdate = coinsEarned,  // Track coins for animation
                // Game Summary
                showGameSummaryDialog = finalIsGameOver && !triggerLastChance,
                linesClearedThisGame = totalLinesCleared,
                coinsEarnedThisGame = newTotalCoins - initialCoinsForGame,
                scoreState = it.scoreState.copy(comboCount = comboCount),
                // Reset any preview highlights after a successful placement and increment move counter
                previewClearIndices = emptyList(),
                previewIsRow = true,
                moveNumber = it.moveNumber + 1
            )
        }

        if (pointsToAdd > 0) {
            triggerScoreAnimation(pointsToAdd, comboCount)
        }

        if (triggerHighScoreAnim) {
            viewModelScope.launch {
                delay(3000)
                _uiState.update { it.copy(showHighScoreAnim = false) }
            }
        }

        // Trigger haptic feedback for game events
        if (totalClears > 0) {
            if (totalClears >= 2) {
                HapticManager.vibrateMedium(getApplication())
            } else {
                HapticManager.vibrateShort(getApplication())
            }
        }

        if (finalIsGameOver) {
            HapticManager.vibrateHeavy(getApplication())
        }
    }

    private fun triggerScoreAnimation(baseScore: Int, comboCount: Int) {
        android.util.Log.d("ScoreAnimation", "triggerScoreAnimation called: baseScore=$baseScore, comboCount=$comboCount")

        _uiState.update {
            it.copy(scoreState = it.scoreState.copy(isAnimating = true, comboCount = comboCount, currentScore = 0))
        }

        scoreAnimator.start(
            baseScore = baseScore,
            comboCount = comboCount,
            onUpdate = { animatedScore ->
                android.util.Log.d("ScoreAnimation", "Score update: $animatedScore")
                _uiState.update {
                    it.copy(scoreState = it.scoreState.copy(currentScore = animatedScore))
                }
            },
            onFinished = {
                android.util.Log.d("ScoreAnimation", "Animation finished")
                _uiState.update {
                    it.copy(scoreState = it.scoreState.copy(isAnimating = false, currentScore = 0))
                }
            },
            onScoreIncrementSound = { /* TODO: Add sound call */ },
            onComboSound = { /* TODO: Add sound call */ }
        )

        addFloatingPopup(baseScore)
    }

    private fun addFloatingPopup(amount: Int) {
        viewModelScope.launch {
            val popup = FloatingScorePopup(System.currentTimeMillis(), amount, 0f)
            _uiState.update {
                it.copy(scoreState = it.scoreState.copy(floatPopups = it.scoreState.floatPopups + popup))
            }

            // Animate progress
            var time = 0L
            while (time < 450) {
                val progress = time / 450f
                _uiState.update { state ->
                    val updatedPopups = state.scoreState.floatPopups.map { p ->
                        if (p.id == popup.id) p.copy(progress = progress) else p
                    }
                    state.copy(scoreState = state.scoreState.copy(floatPopups = updatedPopups))
                }
                delay(16)
                time += 16
            }

            // Remove popup
            _uiState.update {
                it.copy(scoreState = it.scoreState.copy(floatPopups = it.scoreState.floatPopups.filter { p -> p.id != popup.id }))
            }
        }
    }

    private fun placeBlockOnBoard(board: GameGrid, block: Block, row: Int, col: Int): GameGrid {
        val newBoard = board.map { it.clone() }.toTypedArray()
        for (coord in block.shape) {
            newBoard[row + coord.row][col + coord.col] = block.colorResId
        }
        return newBoard
    }

    private fun findClearingCells(boardBefore: GameGrid, boardAfter: GameGrid): Set<Coord> {
        val clearingCells = mutableSetOf<Coord>()
        for (r in 0 until GRID_SIZE) {
            for (c in 0 until GRID_SIZE) {
                if (boardBefore[r][c] != null && boardAfter[r][c] == null) {
                    clearingCells.add(Coord(r, c))
                }
            }
        }
        return clearingCells
    }

    private fun calculateLineClearScore(totalClears: Int): Int {
        return when (totalClears) {
            1 -> 100
            2 -> 300
            3 -> 600
            4 -> 1000
            else -> 0
        }
    }

    private fun handleGameOver() {
        val finalState = _uiState.value
        val finalScore = finalState.score
        val coinsEarned = (finalScore / COIN_REWARD_THRESHOLD) * COINS_PER_REWARD

        viewModelScope.launch {
            uidJob.join()
            // Persistence hook: call Firestore or analytics here if needed.
            // Left intentionally empty to avoid calling a missing API.
        }
    }

    /**
     * Checks if any move is possible with the current available blocks.
     *
     * Uses the Smart Preview Generator's optimized canBlockFit function
     * which checks all rotations and positions efficiently.
     *
     * @param blocks The current available blocks to check
     * @param board The current game board
     * @return true if at least one block can be placed, false otherwise
     */
    private fun isMovePossible(blocks: List<Block>, board: GameGrid): Boolean {
        if (blocks.isEmpty()) return false

        // Check each block with all its rotations
        for (block in blocks) {
            var currentBlock = block
            repeat(4) {
                if (canBlockFit(board, currentBlock)) {
                    return true
                }
                currentBlock = currentBlock.rotate()
            }
        }
        return false
    }

    // --- Private Game Logic Helpers --

    private fun getCoordsToAnimate(clearedBoard: GameGrid, placementBoard: GameGrid): Set<Coord> {
        val coords = mutableSetOf<Coord>()
        for (r in 0 until GRID_SIZE) {
            for (c in 0 until GRID_SIZE) {
                if (placementBoard[r][c] != null && clearedBoard[r][c] == null) {
                    coords.add(Coord(r, c))
                }
            }
        }
        android.util.Log.d("VM_Clear", "getCoordsToAnimate -> ${coords.size} cells: $coords")
        return coords
    }

    private fun canPlaceBlock(block: Block, startRow: Int, startCol: Int, board: GameGrid): Boolean {
        val ignoreCollision = block.isSpecial

        for (coord in block.shape) {
            val boardRow = startRow + coord.row
            val boardCol = startCol + coord.col
            if (boardRow < 0 || boardRow >= GRID_SIZE || boardCol < 0 || boardCol >= GRID_SIZE) return false
            if (!ignoreCollision && board[boardRow][boardCol] != null) return false
        }
        return true
    }

    private fun placeBlock(block: Block, startRow: Int, startCol: Int, board: GameGrid): Pair<GameGrid, Int> {
        val newBoard = board.map { it.clone() }.toTypedArray()
        for (coord in block.shape) {
            newBoard[startRow + coord.row][startCol + coord.col] = block.colorResId
        }
        return Pair(newBoard, 0)
    }

    private fun checkForClears(board: GameGrid): ClearResult {
        val cellsToClear = mutableSetOf<Coord>()
        // Check Rows
        for (r in 0 until GRID_SIZE) {
            if (board[r].all { it != null }) {
                (0 until GRID_SIZE).forEach { c -> cellsToClear.add(Coord(r, c)) }
            }
        }
        // Check Columns
        for (c in 0 until GRID_SIZE) {
            if ((0 until GRID_SIZE).all { r -> board[r][c] != null }) {
                (0 until GRID_SIZE).forEach { r -> cellsToClear.add(Coord(r, c)) }
            }
        }

        if (cellsToClear.isEmpty()) return ClearResult(board, 0)

        val uniqueRows = cellsToClear.map { it.row }.toSet().filter { r -> (0 until GRID_SIZE).all { c -> cellsToClear.contains(Coord(r, c)) } }.size
        val uniqueCols = cellsToClear.map { it.col }.toSet().filter { c -> (0 until GRID_SIZE).all { r -> cellsToClear.contains(Coord(r, c)) } }.size

        val totalClears = uniqueRows + uniqueCols

        val clearedBoard = board.map { it.clone() }.toTypedArray()
        for (cell in cellsToClear) {
            clearedBoard[cell.row][cell.col] = null
        }

        return ClearResult(clearedBoard, totalClears)
    }




    private fun submitScoreToLeaderboard(score: Int, tier: TrophyTier) {
        val userId = firebaseUserId.ifBlank { "UNKNOWN" }
         if (!uidJob.isCompleted) {
             uidJob.invokeOnCompletion { submitScoreToLeaderboard(score, tier) }
             return
         }
         try {
             FirestoreManager.updateLeaderboard(userId = userId, score = score, tier = tier)
         } catch (_: Exception) {
         }
     }

     fun dismissTierPromotion() {
         _uiState.update {
             it.copy(
                 showTierPromotionDialog = false,
                 newlyUnlockedTier = null
             )
         }
     }

     fun shareTierAchievement(context: Context, tier: TrophyTier) {
         val shareText = "I just reached the ${tier.name} tier in Better Blocks!"
         val intent = Intent(Intent.ACTION_SEND).apply {
             type = "text/plain"
             putExtra(Intent.EXTRA_TEXT, shareText)
         }
         val chooser = Intent.createChooser(intent, "Share Achievement").apply {
             addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
         }
         context.startActivity(chooser)
     }

     // =====================================================
     // DAILY REWARD SYSTEM
     // =====================================================

     /**
      * Checks if daily reward is available and shows dialog if applicable
      */
     fun checkDailyReward() {
         val lastClaimDate = prefs.getString(KEY_DAILY_REWARD_DATE, null)
         val currentStreak = prefs.getInt(KEY_DAILY_STREAK, 0)
         val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())

         // Debug: Uncomment to always show daily reward for testing
         // val today = today + "_debug_${System.currentTimeMillis()}"

         if (lastClaimDate == today) {
             // Already claimed today
             return
         }

         // Calculate new streak
         val newStreak = if (lastClaimDate == null) {
             1  // First time ever
         } else {
             try {
                 val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                 val lastDate = dateFormat.parse(lastClaimDate)
                 val todayDate = dateFormat.parse(today)
                 if (lastDate != null && todayDate != null) {
                     val diffInMillis = todayDate.time - lastDate.time
                     val diffInDays = (diffInMillis / (1000 * 60 * 60 * 24)).toInt()

                     when (diffInDays) {
                         1 -> currentStreak + 1 // Consecutive day
                         else -> 1 // Streak broken
                     }
                 } else {
                     1
                 }
             } catch (e: Exception) {
                 1
             }
         }

         // Determine which day in the 7-day cycle
         val dayInCycle = ((newStreak - 1) % 7) + 1

         // Calculate rewards
         val coinsReward = when (dayInCycle) {
             1 -> 150
             2 -> 200
             3 -> 300
             4 -> 400
             5 -> 500
             6 -> 600
             7 -> 1000
             else -> 150
         }
         val rainbowReward = dayInCycle == 7

         // Show the dialog
         _uiState.update {
             it.copy(
                 showDailyRewardDialog = true,
                 dailyRewardDay = dayInCycle,
                 dailyRewardStreak = newStreak,
                 dailyRewardCoins = coinsReward,
                 dailyRewardRainbow = rainbowReward
             )
         }
     }

     /**
      * Claims the daily reward and updates persistence
      */
     fun claimDailyReward() {
         val state = _uiState.value
         if (!state.showDailyRewardDialog) return

         // Add coins
         val newCoins = state.coins + state.dailyRewardCoins
         saveCoins(newCoins)

         // Add rainbow wipe if day 7
         var newRainbowCount = state.rainbowBlockCount
         if (state.dailyRewardRainbow) {
             newRainbowCount += 1
             saveRainbowCount(newRainbowCount)
         }

         // Save claim date and streak
         val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
         prefs.edit()
             .putString(KEY_DAILY_REWARD_DATE, today)
             .putInt(KEY_DAILY_STREAK, state.dailyRewardStreak)
             .apply()

         // Update state
         _uiState.update {
             it.copy(
                 coins = newCoins,
                 rainbowBlockCount = newRainbowCount,
                 showDailyRewardDialog = false,
                 dailyRewardDay = 0,
                 dailyRewardStreak = 0,
                 dailyRewardCoins = 0,
                 dailyRewardRainbow = false
             )
         }

        // Trigger haptic feedback
        HapticManager.vibrateMedium(getApplication())
    }

    /**
     * Refreshes user stats from SharedPreferences
     * Call this when returning to MainActivity from other activities (e.g., Shop)
     */
    fun refreshUserStats() {
        val savedCoins = prefs.getInt(KEY_COINS, DEV_INITIAL_COINS)
        val savedRainbowCount = prefs.getInt(KEY_RAINBOW_COUNT, DEV_INITIAL_RAINBOW)
        val savedColorWipeCount = prefs.getInt(KEY_COLOR_WIPE_COUNT, DEV_INITIAL_COLOR_WIPE)
        val savedHighScore = prefs.getInt(KEY_HIGH_SCORE, 0)
        val lifetimeCoins = prefs.getInt(KEY_LIFETIME_COINS, savedCoins)
        val savedTrophyTier = getPlayerTier(savedHighScore, lifetimeCoins, prefs)

        _uiState.update {
            it.copy(
                coins = savedCoins,
                rainbowBlockCount = savedRainbowCount,
                colorWipeCount = savedColorWipeCount,
                highScore = savedHighScore,
                trophyTier = savedTrophyTier
            )
        }

        // Apply developer test values if any are set
        applyDeveloperTestValues()
    }

    /**
     * Applies developer test values from GameSettings to current game state
     * Call this after returning from Developer screen
     */
    fun applyDeveloperTestValues() {
        val testRainbow = GameSettings.testRainbowCount.value
        val testColorWipe = GameSettings.testColorWipeCount.value
        val testCoins = GameSettings.testCoins.value
        val testScore = GameSettings.testScore.value

        // Only apply if values are non-zero (indicating they were intentionally set)
        if (testRainbow > 0 || testColorWipe > 0 || testCoins > 0 || testScore > 0) {
            _uiState.update {
                it.copy(
                    rainbowBlockCount = if (testRainbow > 0) testRainbow else it.rainbowBlockCount,
                    colorWipeCount = if (testColorWipe > 0) testColorWipe else it.colorWipeCount,
                    coins = if (testCoins > 0) testCoins else it.coins,
                    score = if (testScore > 0) testScore else it.score
                )
            }

            // Persist the changes
            if (testRainbow > 0) saveRainbowCount(testRainbow)
            if (testColorWipe > 0) saveColorWipeCount(testColorWipe)
            if (testCoins > 0) saveCoins(testCoins)
            if (testScore > 0) {
                prefs.edit().putInt(KEY_SAVED_SCORE, testScore).apply()
            }
        }
    }

    // =====================================================
    // GAME SUMMARY DIALOG
    // =====================================================

     /**
      * Shows game over summary dialog with stats
      */
     fun dismissGameSummary() {
         _uiState.update {
             it.copy(showGameSummaryDialog = false)
         }
     }

     /**
      * Resets game from summary dialog
      */
     fun playAgainFromSummary() {
         dismissGameSummary()
         restartGame()
     }

     /**
      * Track lines cleared during game
      */
     private var totalLinesCleared = 0
     private var initialCoinsForGame = 0

     private fun trackLineClears(lineCount: Int) {
         totalLinesCleared += lineCount
     }

     private fun resetGameTracking() {
         totalLinesCleared = 0
         initialCoinsForGame = _uiState.value.coins
     }

     // Ghost snapshot: single source of truth updated during drag, read on drop
    private val _ghostRow = mutableStateOf<Int?>(null)
    private val _ghostCol = mutableStateOf<Int?>(null)
    private val _isGhostValid = mutableStateOf(false)

    val ghostRow: Int? get() = _ghostRow.value
    val ghostCol: Int? get() = _ghostCol.value
    val isGhostValid: Boolean get() = _isGhostValid.value

    fun updateGhostSnapshot(row: Int?, col: Int?, valid: Boolean) {
        _ghostRow.value = row
        _ghostCol.value = col
        _isGhostValid.value = valid
        Log.d("🎯 DRAG", "SNAPSHOT VM: row=$row col=$col valid=$valid")
    }

    fun clearGhostSnapshot() {
        updateGhostSnapshot(null, null, false)
    }
 }
