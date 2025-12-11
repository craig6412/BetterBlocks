package com.betterblocks

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.betterblocks.animation.LineClearAnimator
import com.betterblocks.animation.ScoreAnimator
import com.betterblocks.model.TrophyTier
import com.betterblocks.model.getPlayerTier
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import android.content.SharedPreferences
import kotlin.apply
import kotlin.compareTo

// --- Constants ---
const val GRID_SIZE = 9
const val BLOCKS_PER_ROUND = 3

// Broadcast action sent by DeveloperActivity when developer knobs are saved
const val ACTION_DEV_SETTINGS_CHANGED = "com.betterblocks.ACTION_DEV_SETTINGS_CHANGED"

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
private const val DEV_INITIAL_COINS = 150// $9,999 for development
private const val DEV_INITIAL_RAINBOW = 2 // starting rainbow wipes for new installs
private const val DEV_INITIAL_COLOR_WIPE = 5 // Start with 5 Color Wipes

// Prefs schema version to force default reset on first run after update
const val PREFS_SCHEMA_VERSION = 3
const val KEY_PREFS_SCHEMA_VERSION = "prefs_schema_version"

// --- GAME VIEWMODEL ---
class GameViewModel(application: Application) : AndroidViewModel(application) {


    private val prefs = application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    // Keep in-memory UI state in sync with direct SharedPreferences writes from other Activities
    private val prefsListener = SharedPreferences.OnSharedPreferenceChangeListener { sp, key ->
        try {
            when (key) {
                KEY_COINS -> {
                    val new = sp.getInt(KEY_COINS, _uiState.value.coins)
                    Log.d("GameViewModel", "prefsListener: KEY_COINS changed -> $new")
                    _uiState.update { it.copy(coins = new) }
                }
                KEY_RAINBOW_COUNT -> {
                    val new = sp.getInt(KEY_RAINBOW_COUNT, _uiState.value.rainbowBlockCount)
                    Log.d("GameViewModel", "prefsListener: KEY_RAINBOW_COUNT changed -> $new")
                    _uiState.update { it.copy(rainbowBlockCount = new) }
                }
                KEY_COLOR_WIPE_COUNT -> {
                    val new = sp.getInt(KEY_COLOR_WIPE_COUNT, _uiState.value.colorWipeCount)
                    Log.d("GameViewModel", "prefsListener: KEY_COLOR_WIPE_COUNT changed -> $new")
                    _uiState.update { it.copy(colorWipeCount = new) }
                }
                KEY_FREE_ROTATIONS -> {
                    val new = sp.getInt(KEY_FREE_ROTATIONS, _uiState.value.freeRotations)
                    Log.d("GameViewModel", "prefsListener: KEY_FREE_ROTATIONS changed -> $new")
                    _uiState.update { it.copy(freeRotations = new) }
                }
                KEY_HIGH_SCORE -> {
                    val new = sp.getInt(KEY_HIGH_SCORE, _uiState.value.highScore)
                    Log.d("GameViewModel", "prefsListener: KEY_HIGH_SCORE changed -> $new")
                    _uiState.update { it.copy(highScore = new) }
                }
                KEY_SAVED_SELECTED -> {
                    val sel = sp.getInt(KEY_SAVED_SELECTED, -1).takeIf { it != -1 }
                    Log.d("GameViewModel", "prefsListener: KEY_SAVED_SELECTED changed -> $sel")
                    _uiState.update { it.copy(selectedBlock = restoreSelectedBlock(sel, it.availableBlocks, sp.getInt(KEY_RAINBOW_COUNT, it.rainbowBlockCount) > 0)) }
                }
                else -> Log.d("GameViewModel", "prefsListener: other key changed -> $key")
            }
        } catch (t: Throwable) {
            Log.w("GameViewModel", "prefsListener error for key=$key: ${t.message}")
        }
    }

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

    // NEW: runtime-only counter used to tie rotation payments to a specific selection session
    private var selectionCounter: Long = 0L
    // NEW: runtime-only flag to ensure we offer the "last chance" rainbow dialog only once per game
    private var lastChanceOfferedThisGame: Boolean = false
    // Persisted mirror so that recreation of the ViewModel doesn't re-offer the dialog
    private var lastChanceOfferedPersisted: Boolean = prefs.getBoolean(KEY_LAST_CHANCE_OFFERED, false)

    init {
        // Register listener before initial checks so external writes during startup are observed
        prefs.registerOnSharedPreferenceChangeListener(prefsListener)
        // Intentionally empty init; developer overrides can be triggered from the dev UI actions which call
        // `applyDeveloperInventoryOverrides()` directly. Avoid dynamic broadcast receiver registration here.
        // Run initial checks for daily reward and season start on startup
        viewModelScope.launch {
            checkDailyReward()
            checkSeasonStart()
        }
    }
    override fun onCleared() {
        try { prefs.unregisterOnSharedPreferenceChangeListener(prefsListener) } catch (_: Throwable) {}
        super.onCleared()
    }

    /**
     * Creates the initial state for a new game, loading saved data.
     */
    private fun createInitialState(): GameUiState {
        // Schema bump OR truly fresh install: ensure developer defaults are applied to prefs
        val storedSchema = prefs.getInt(KEY_PREFS_SCHEMA_VERSION, 0)
        val isFreshInstall = !prefs.contains(KEY_COINS)
        // DEBUG: Log current prefs state
        val currentCoins = prefs.getInt(KEY_COINS, -1)  // -1 to detect if key exists
        val currentRainbow = prefs.getInt(KEY_RAINBOW_COUNT, -1)
        val currentColorWipe = prefs.getInt(KEY_COLOR_WIPE_COUNT, -1)
        Log.d("GameViewModel", "createInitialState: storedSchema=$storedSchema isFreshInstall=$isFreshInstall currentCoins=$currentCoins currentRainbow=$currentRainbow currentColorWipe=$currentColorWipe")

        // FORCE OVERWRITE if values are below defaults (handles backup restore on "fresh" install)
        val forceOverwrite = currentCoins < DEV_INITIAL_COINS || currentRainbow < DEV_INITIAL_RAINBOW || currentColorWipe < DEV_INITIAL_COLOR_WIPE
        if (storedSchema < PREFS_SCHEMA_VERSION || isFreshInstall || forceOverwrite) {
            Log.d("GameViewModel", "createInitialState: Overwriting defaults (schema bump or fresh install or forceOverwrite=$forceOverwrite)")
            prefs.edit()
                .putInt(KEY_COINS, DEV_INITIAL_COINS)
                .putInt(KEY_RAINBOW_COUNT, DEV_INITIAL_RAINBOW)
                .putInt(KEY_COLOR_WIPE_COUNT, DEV_INITIAL_COLOR_WIPE)
                .putInt(KEY_PREFS_SCHEMA_VERSION, PREFS_SCHEMA_VERSION)
                .apply()
        } else {
            Log.d("GameViewModel", "createInitialState: Skipping overwrite (schema up-to-date and not fresh)")
        }

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
        val finalBlocks = if (blocksAreValid) restoredBlocks!! else generateNewBlocks(savedBoard)

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
            // Show first-install welcome dialog for fresh installs or when we just bumped schema
            showFirstInstallFreeCoinsDialog = isFreshInstall || (storedSchema < PREFS_SCHEMA_VERSION),
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
    private fun generateNewBlocks(board: GameGrid): List<Block> {
        return generateSmartPreview(
            board = board,
            allBlocks = BLOCK_MANAGER
        )
    }

    // --- Persistence Helpers ---

    private fun saveHighScore(score: Int) {
        prefs.edit().putInt(KEY_HIGH_SCORE, score).apply()
    }

    private fun saveCoins(coins: Int) {
        val prev = prefs.getInt(KEY_COINS, -9999)
        prefs.edit().putInt(KEY_COINS, coins).apply()
        Log.d("GameViewModel", "saveCoins: prev=$prev -> new=$coins")
    }

    private fun saveLifetimeCoinsIfHigher(totalCoins: Int) {
        val previous = prefs.getInt(KEY_LIFETIME_COINS, totalCoins)
        if (totalCoins > previous) {
            prefs.edit().putInt(KEY_LIFETIME_COINS, totalCoins).apply()
        }
    }

    private fun saveRainbowCount(count: Int) {
        val prev = prefs.getInt(KEY_RAINBOW_COUNT, -9999)
        prefs.edit().putInt(KEY_RAINBOW_COUNT, count).apply()
        Log.d("GameViewModel", "saveRainbowCount: prev=$prev -> new=$count")
    }

    private fun saveColorWipeCount(count: Int) {
        val prev = prefs.getInt(KEY_COLOR_WIPE_COUNT, -9999)
        prefs.edit().putInt(KEY_COLOR_WIPE_COUNT, count).apply()
        Log.d("GameViewModel", "saveColorWipeCount: prev=$prev -> new=$count")
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
            // don't overwrite first-game-over flag here
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
        // Reset runtime-only tracking for a fresh session
        lastChanceOfferedThisGame = false
        lastChanceOfferedPersisted = false
        prefs.edit().putBoolean(KEY_LAST_CHANCE_OFFERED, false).apply()
        // If you had other session-only trackers, reset them here as well.
    }

    /**
     * Called when a player achieves a combo clear of 2 or more lines.
     */
    private fun onSpecialMeterFilled(currentRainbowCount: Int, currentMeterValue: Int) {
        val newMeter = currentMeterValue + 1
        if (newMeter >= SPECIAL_METER_MAX) {
            // Award block and reset meter
            val newCount = currentRainbowCount + 1
            Log.d("GameViewModel", "onSpecialMeterFilled: meter reached threshold -> awarding rainbow. prevMeter=$currentMeterValue newMeter=0 newRainbowCount=$newCount")
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
            Log.d("GameViewModel", "onSpecialMeterFilled: incrementing meter prevMeter=$currentMeterValue newMeter=${newMeter}")
            _uiState.update {
                it.copy(
                    specialMeterValue = newMeter
                )
            }
        }
    }

    /**
     * Triggered when the Color Wheel stops spinning.
     */
    // Patch: Add diagnostics and clearer logging to onColorWipeSpinResult with deep-copying to avoid aliasing bugs
    fun onColorWipeSpinResult(colorIndex: Int) {
        val currentState = _uiState.value
        Log.d("GameViewModel", "onColorWipeSpinResult called colorIndex=$colorIndex colorWipeCount=${currentState.colorWipeCount}")

        if (currentState.colorWipeCount <= 0) {
            Log.d("GameViewModel", "onColorWipeSpinResult aborting: no color wipes available")
            return
        }

        // Deduct Inventory immediately
        val newCount = currentState.colorWipeCount - 1
        saveColorWipeCount(newCount)
        _uiState.update { it.copy(colorWipeCount = newCount) }
        saveSelectedBlockId(_uiState.value.selectedBlock?.id)
        Log.d("GameViewModel", "Color wipe consumed -> newCount=$newCount")

        // 2. Find all matching blocks
        val targetDrawableId = try {
            BLOCK_DRAWABLES[colorIndex]
        } catch (t: Throwable) {
            Log.e("GameViewModel", "Invalid colorIndex=$colorIndex", t)
            return
        }

        val cellsToClear = mutableSetOf<Coord>()
        for (r in 0 until GRID_SIZE) {
            for (c in 0 until GRID_SIZE) {
                if (currentState.board[r][c] == targetDrawableId) {
                    cellsToClear.add(Coord(r, c))
                }
            }
        }

        Log.d("GameViewModel", "Color wipe targetDrawableId=$targetDrawableId matchedCells=${cellsToClear.size}")

        viewModelScope.launch {
            if (cellsToClear.isNotEmpty()) {
                // Make a deep copy snapshot of the board to avoid aliasing with the live UI state.
                val boardSnapshot: GameGrid = currentState.board.map { it.clone() }.toTypedArray()

                // 3. Keep the board state unchanged during animation; set effect layer using the snapshot.
                _uiState.update {
                    it.copy(
                        board = boardSnapshot,         // use the copy so UI/logic don't share references
                        effectCells = cellsToClear,
                        isColorWipeAnimating = true
                    )
                }
                Log.d("GameViewModel", "Effect layer set; starting color wipe animation for ${cellsToClear.size} cells")

                // Use slower animation duration for color wipe
                val colorWipeDelay = (BLOCK_CLEAR_DELAY_MS * COLOR_WIPE_ANIMATION_SPEED_MULTIPLIER).toLong()
                delay(colorWipeDelay)

                // 4. Remove from the snapshot after animation (not the original array)
                val newBoard = boardSnapshot.map { it.clone() }.toTypedArray()
                for (cell in cellsToClear) {
                    newBoard[cell.row][cell.col] = null
                }

                // 5. Score: 50 points per block
                val points = cellsToClear.size * 50

                Log.d("GameViewModel", "Applying board changes: cleared=${cellsToClear.size} points=$points")
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
                Log.d("GameViewModel", "Color wipe finished; effect layer cleared")
                checkGameOverOrLastChance()
            } else {
                // No matching cells — nothing to animate; already consumed an item above.
                Log.d("GameViewModel", "No matching blocks found for colorIndex=$colorIndex; nothing cleared")
                // Ensure UI isn't left in animating state
                _uiState.update { it.copy(effectCells = emptySet(), isColorWipeAnimating = false) }
            }
        }
    }


    private fun handleFullBoardRainbowWipe(state: GameUiState) {
        // Compute occupied cells from the snapshot 'state' only
        val occupiedCells = mutableSetOf<Coord>()
        for (r in 0 until GRID_SIZE) {
            for (c in 0 until GRID_SIZE) {
                if (state.board[r][c] != null) {
                    occupiedCells.add(Coord(r, c))
                }
            }
        }

        // No occupied cells, nothing to animate or clear
        if (occupiedCells.isEmpty()) return

        // FIXED: Set animation flags but keep board intact during animation
        // The board will be cleared by onClearAnimationFinished when animation completes
        _uiState.update {
            it.copy(
                clearingCells = occupiedCells,
                isRainbowWipeActive = true,
                isLastChance = false,
                selectedBlock = null
            )
        }
    }

    /**
     * Called by the UI when a line-clear or rainbow-wipe animation has completed.
     * This clears the board, resets animation flags, and generates new blocks if needed.
     */
    fun onClearAnimationFinished() {
        val currentState = _uiState.value

        // If we were doing a rainbow wipe, clear the board now
        if (currentState.isRainbowWipeActive && currentState.clearingCells.isNotEmpty()) {
            val clearedBoard: GameGrid = Array(GRID_SIZE) { Array<Int?>(GRID_SIZE) { null } }
            val newBlocks = generateNewBlocks(clearedBoard)

            val points = currentState.clearingCells.size * 50

            _uiState.update {
                it.copy(
                    board = clearedBoard,
                    availableBlocks = newBlocks,
                    score = it.score + points,
                    clearingCells = emptySet(),
                    isRainbowWipeActive = false,
                    isColorWipeAnimating = false,
                    selectedBlock = null
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
                selectedBlockId = null,
                isGameOver = false,
                isLastChance = false
            )

            // After clearing, check for game over
            checkGameOverOrLastChance()
        } else {
            // For normal line clears, just reset the flags
            _uiState.update {
                it.copy(
                    clearingCells = emptySet(),
                    isRainbowWipeActive = false,
                    isColorWipeAnimating = false
                )
            }
            checkGameOverOrLastChance()
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

    // Called when the player accepts the last-chance rainbow wipe
    fun onLastChanceUsed() {
        val state = _uiState.value
        if (!state.isLastChance || state.rainbowBlockCount <= 0) return

        // Mark that we've offered last-chance this game (defensive)
        lastChanceOfferedThisGame = true
        lastChanceOfferedPersisted = true
        prefs.edit().putBoolean(KEY_LAST_CHANCE_OFFERED, true).apply()

        val newCount = state.rainbowBlockCount - 1
        saveRainbowCount(newCount)
        _uiState.update { it.copy(rainbowBlockCount = newCount, isLastChance = false, selectedBlock = null) }

        // Start the full-board rainbow wipe animation flow using the current snapshot
        handleFullBoardRainbowWipe(_uiState.value)

        // Persist the inventory and snapshot
        val post = _uiState.value
        persistGameSnapshot(
            board = post.board,
            blocks = post.availableBlocks,
            coins = post.coins,
            rainbowCount = post.rainbowBlockCount,
            colorWipeCount = post.colorWipeCount,
            score = post.score,
            meterValue = post.specialMeterValue,
            freeRotations = post.freeRotations,
            lastRotatedBlockId = post.lastRotatedBlockId,
            selectedBlockId = post.selectedBlock?.id,
            isGameOver = post.isGameOver,
            isLastChance = post.isLastChance
        )
    }

    // Called when the user declines the last-chance rainbow wipe -> finalize game over
    fun onLastChanceDeclined() {
        val state = _uiState.value
        if (!state.isLastChance) return

        // Mark that we've offered last-chance this game (defensive)
        lastChanceOfferedThisGame = true
        lastChanceOfferedPersisted = true
        prefs.edit().putBoolean(KEY_LAST_CHANCE_OFFERED, true).apply()

        _uiState.update { it.copy(isLastChance = false, isGameOver = true, selectedBlock = null, showGameSummaryDialog = true) }

        val post = _uiState.value
        persistGameSnapshot(
            board = post.board,
            blocks = post.availableBlocks,
            coins = post.coins,
            rainbowCount = post.rainbowBlockCount,
            colorWipeCount = post.colorWipeCount,
            score = post.score,
            meterValue = post.specialMeterValue,
            freeRotations = post.freeRotations,
            lastRotatedBlockId = post.lastRotatedBlockId,
            selectedBlockId = post.selectedBlock?.id,
            isGameOver = post.isGameOver,
            isLastChance = post.isLastChance
        )
    }

    // Public API to immediately use one rainbow wipe (from dialogs / quick actions)
    fun useRainbowWipeImmediately() {
        val state = _uiState.value
        if (state.rainbowBlockCount <= 0) return

        val newCount = state.rainbowBlockCount - 1
        saveRainbowCount(newCount)
        _uiState.update { it.copy(rainbowBlockCount = newCount, selectedBlock = null) }

        // Start full-board wipe animation
        handleFullBoardRainbowWipe(_uiState.value)

        // Persist snapshot
        val post = _uiState.value
        persistGameSnapshot(
            board = post.board,
            blocks = post.availableBlocks,
            coins = post.coins,
            rainbowCount = post.rainbowBlockCount,
            colorWipeCount = post.colorWipeCount,
            score = post.score,
            meterValue = post.specialMeterValue,
            freeRotations = post.freeRotations,
            lastRotatedBlockId = post.lastRotatedBlockId,
            selectedBlockId = post.selectedBlock?.id,
            isGameOver = post.isGameOver,
            isLastChance = post.isLastChance
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
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val dayMs = 24L * 60L * 60L * 1000L
            val lastClaim = prefs.getLong(KEY_DAILY_REWARD_DATE, 0L)

            val todayIndex = now / dayMs
            val lastIndex = if (lastClaim == 0L) -1L else lastClaim / dayMs

            val dayPassed = lastIndex < todayIndex
            if (dayPassed) {
                val streakPrev = prefs.getInt(KEY_DAILY_STREAK, 0)
                val newStreak = if (lastIndex == todayIndex - 1L) streakPrev + 1 else 1

                val dayNumber = (newStreak - 1) % 7 + 1
                val coins = when (dayNumber) {
                    1 -> 50
                    2 -> 75
                    3 -> 100
                    4 -> 125
                    5 -> 150
                    6 -> 175
                    7 -> 250
                    else -> 50
                }
                val hasRainbow = dayNumber == 7

                _uiState.update {
                    it.copy(
                        showDailyRewardDialog = true,
                        dailyRewardDay = dayNumber,
                        dailyRewardStreak = newStreak,
                        dailyRewardCoins = coins,
                        dailyRewardRainbow = hasRainbow
                    )
                }
            } else {
                _uiState.update { it.copy(showDailyRewardDialog = false) }
            }
        }
    }

    // Claim daily reward: apply whatever is in uiState.dailyReward* and hide dialog
    fun claimDailyReward() {
        val state = _uiState.value
        val newCoins = state.coins + state.dailyRewardCoins
        saveCoins(newCoins)
        // Persist date and streak
        val now = System.currentTimeMillis()
        prefs.edit()
            .putLong(KEY_DAILY_REWARD_DATE, now)
            .putInt(KEY_DAILY_STREAK, state.dailyRewardStreak)
            .apply()

        _uiState.update {
            it.copy(
                coins = newCoins,
                showDailyRewardDialog = false,
                dailyRewardCoins = 0,
                dailyRewardRainbow = false
            )
        }
    }

    // Season start wiring
    fun checkSeasonStart() {
        viewModelScope.launch {
            val started = prefs.getBoolean(KEY_SEASON_STARTED, false)
            val now = System.currentTimeMillis()
            if (!started && now >= GameSettings.seasonStartEpochMs) {
                startSeason()
            }
        }
    }

    private fun startSeason() {
        viewModelScope.launch {
            prefs.edit().putBoolean(KEY_SEASON_STARTED, true).apply()
            Log.d("GameViewModel", "Season started at configured epoch: ${GameSettings.seasonStartEpochMs}")
            // Optionally update uiState if you have season fields
        }
    }

    // --- GAME LOGIC ---

    /**
     * Called when a player places a block.
     *
     * This handles normal blocks, special blocks (rainbow/color wipe), and detects clears.
     */
    fun onBlockPlaced(row: Int, col: Int) {
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

        // --- SPECIAL METER: increment when player clears more than one line/column at once ---
        val clearedLines = rowsToClear.size + colsToClear.size
        if (clearedLines > 1) {
            Log.d("GameViewModel", "placeBlock: combo clear detected clearedLines=$clearedLines -> increment special meter (currentMeter=${current.specialMeterValue})")
            // Use current state's values; onSpecialMeterFilled will award a rainbow if meter completes
            onSpecialMeterFilled(current.rainbowBlockCount, current.specialMeterValue)
        }

        var finalBoard = newBoard
        var points = block.shape.size
        if (cellsToClear.isNotEmpty()) {
            finalBoard = newBoard.map { it.clone() }.toTypedArray()
            cellsToClear.forEach { coord -> finalBoard[coord.row][coord.col] = null }
            points += (rowsToClear.size + colsToClear.size) * 100
        }

        val remainingBlocks = current.availableBlocks.filter { it.id != block.id }
        val nextBlocks = if (remainingBlocks.isEmpty()) generateNewBlocks(finalBoard)
        else remainingBlocks

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

        // After a normal placement, check for game over / last chance
        checkGameOverOrLastChance()
    }

    // --- GAME OVER / MOVE AVAILABILITY LOGIC ---

    /** Returns true if there is at least one legal placement for any non-special block. */
    private fun hasAnyValidMove(state: GameUiState): Boolean {
        val board = state.board

        for (block in state.availableBlocks) {
            if (block.isSpecial) continue

            var candidate = block
            repeat(4) { rotationIndex ->
                // Bounding quick check
                if (candidate.boundingBoxHeight > GRID_SIZE || candidate.boundingBoxWidth > GRID_SIZE) {
                    candidate = candidate.rotate()
                    return@repeat
                }

                for (row in 0 until GRID_SIZE) {
                    for (col in 0 until GRID_SIZE) {
                        if (row + candidate.boundingBoxHeight > GRID_SIZE || col + candidate.boundingBoxWidth > GRID_SIZE) continue

                        var fits = true
                        for (offset in candidate.shape) {
                            val r = row + offset.row
                            val c = col + offset.col
                            if (r !in 0 until GRID_SIZE || c !in 0 until GRID_SIZE || board[r][c] != null) {
                                fits = false
                                break
                            }
                        }

                        if (fits) {
                            Log.d("GameViewModel", "hasAnyValidMove -> TRUE blockId=${block.id} rotation=$rotationIndex origin=($row,$col)")
                            return true
                        }
                    }
                }

                candidate = candidate.rotate()
            }
        }

        Log.d("GameViewModel", "hasAnyValidMove -> FALSE")
        return false
    }

    /**
     * Check for game over after a move or board-clear action.
     * If there are no valid moves left:
     *  - If player has rainbow wipes, trigger last-chance flow (isLastChance = true).
     *  - Otherwise hard-set isGameOver = true.
     */
    private fun checkGameOverOrLastChance() {
        val state = _uiState.value
        if (state.isGameOver || state.isLastChance) return

        val anyMove = hasAnyValidMove(state)
        if (anyMove) return

        // Re-evaluate persisted flag at call time to handle ViewModel recreation
        val persistedFlag = prefs.getBoolean(KEY_LAST_CHANCE_OFFERED, false)
        val alreadyOffered = lastChanceOfferedThisGame || persistedFlag
        Log.d("GameViewModel", "checkGameOverOrLastChance -> state.rainbow=${state.rainbowBlockCount} lastChanceThisGame=$lastChanceOfferedThisGame persistedLastChance=$persistedFlag isLastChance=${state.isLastChance} isGameOver=${state.isGameOver}")

        val updated = if (state.rainbowBlockCount > 0 && !alreadyOffered) {
            // Offer last-chance once per game
            lastChanceOfferedThisGame = true
            // Persist immediately so recreation doesn't re-offer
            prefs.edit().putBoolean(KEY_LAST_CHANCE_OFFERED, true).apply()
            Log.d("GameViewModel", "checkGameOverOrLastChance -> OFFER last chance dialog")
            state.copy(isLastChance = true, selectedBlock = null)
        } else {
            // Either no rainbows, or we've already offered the last chance this game; finalize game over
            Log.d("GameViewModel", "checkGameOverOrLastChance -> FINALIZE game over (alreadyOffered=$alreadyOffered rainbow=${state.rainbowBlockCount})")
            state.copy(isGameOver = true, selectedBlock = null)
        }

        _uiState.value = updated
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

        // FIRST-TIME GAME OVER: award 3 free rainbow wipes on initial install and show dialog
        try {
            val firstShown = prefs.getBoolean(KEY_FIRST_GAME_OVER_SHOWN, false)
            if (updated.isGameOver && !firstShown) {
                val newCount = updated.rainbowBlockCount + 3
                saveRainbowCount(newCount)
                prefs.edit().putBoolean(KEY_FIRST_GAME_OVER_SHOWN, true).apply()
                _uiState.update {
                    it.copy(
                        rainbowBlockCount = newCount,
                        showFirstGameOverDialog = true,
                        selectedBlock = null
                    )
                }
                val post = _uiState.value
                persistGameSnapshot(
                    board = post.board,
                    blocks = post.availableBlocks,
                    coins = post.coins,
                    rainbowCount = post.rainbowBlockCount,
                    colorWipeCount = post.colorWipeCount,
                    score = post.score,
                    meterValue = post.specialMeterValue,
                    freeRotations = post.freeRotations,
                    lastRotatedBlockId = post.lastRotatedBlockId,
                    selectedBlockId = post.selectedBlock?.id,
                    isGameOver = post.isGameOver,
                    isLastChance = post.isLastChance
                )
            }
        } catch (t: Throwable) {
            Log.w("GameViewModel", "Failed to award first-game rainbow: ${t.message}")
        }
    }

    // Dismiss the first-game-over reward dialog (UI call)
    fun dismissFirstGameOverDialog() {
        _uiState.update { it.copy(showFirstGameOverDialog = false) }
        // Persist the flag in prefs already set when awarding; snapshot persisted already
    }

    // DEV-ONLY: Apply values from GameSettings test knobs into the live game state.
    // This lets DeveloperActivity adjust coins / score / inventory for quick testing.
    fun applyDeveloperInventoryOverrides() {
        try {
            val devCoins = GameSettings.testCoins.value
            val devScore = GameSettings.testScore.value
            val devRainbow = GameSettings.testRainbowCount.value
            val devColorWipe = GameSettings.testColorWipeCount.value

            _uiState.update { current ->
                current.copy(
                    coins = devCoins.takeIf { it >= 0 } ?: current.coins,
                    score = devScore.takeIf { it >= 0 } ?: current.score,
                    rainbowBlockCount = devRainbow.takeIf { it >= 0 } ?: current.rainbowBlockCount,
                    colorWipeCount = devColorWipe.takeIf { it >= 0 } ?: current.colorWipeCount
                )
            }

            // Persist these so they survive process death like normal game state
            if (devCoins >= 0) saveCoins(devCoins)
            if (devRainbow >= 0) saveRainbowCount(devRainbow)
            if (devColorWipe >= 0) saveColorWipeCount(devColorWipe)
            prefs.edit().putInt(KEY_SAVED_SCORE, devScore).apply()

            Log.d("GameViewModel", "Applied developer inventory overrides: coins=$devCoins score=$devScore rainbows=$devRainbow colorWipes=$devColorWipe")
        } catch (t: Throwable) {
            Log.w("GameViewModel", "applyDeveloperInventoryOverrides failed: ${t.message}")
        }
    }

    /** Public wrapper used by Activities to apply developer overrides (inventory + tuning). */
    fun applyDeveloperOverrides() {
        // Apply inventory overrides
        applyDeveloperInventoryOverrides()
        // Future: apply other GameSettings-based overrides here
    }

    // --- Activity-facing wrapper APIs (fix unresolved reference errors) ---

    /** Backwards-compatible alias used by GameActivity */
    fun placeBlock(row: Int, col: Int) = onBlockPlaced(row, col)

    /** Select a preview block (from bottom row/available blocks) */
    fun selectBlock(block: Block) {
        Log.d("GameViewModel", "selectBlock -> id=${block.id} name=${block.name}")
        _uiState.update {
            it.copy(
                selectedBlock = block,
                selectionToken = it.selectionToken + 1L,
                rotation = 0, // reset rotation when a new block is selected
                rotationPaidSelectionToken = 0L // clear rotation payment marker for new selection
            )
        }
        // Persist selected preview for snapshot
        saveSelectedBlockId(block.id)
    }

    /** Rotate the currently selected block (consumes a free rotation or charges coins). */
    fun rotateSelectedBlock() {
        val currentState = _uiState.value
        Log.d("GameViewModel", "rotateSelectedBlock called selected=${currentState.selectedBlock?.id} selectionToken=${currentState.selectionToken} freeRotations=${currentState.freeRotations} coins=${currentState.coins} rotationPaid=${currentState.rotationPaidSelectionToken}")

        // Disallow rotation in game over/last chance or while board animations are running
        if (currentState.isGameOver || currentState.selectedBlock == null || currentState.isLastChance) return
        if (currentState.isColorWipeAnimating || currentState.isRainbowWipeActive || currentState.clearingCells.isNotEmpty()) return

        val currentBlock = currentState.selectedBlock

        // Determine if we've already consumed payment/free-rotation for this selection token
        val alreadyPaidForThisSelection = currentState.rotationPaidSelectionToken != 0L && currentState.rotationPaidSelectionToken == currentState.selectionToken

        // Don't rotate special blocks
        if (currentBlock.isSpecial) return

        var newCoins = currentState.coins
        var newFreeRotations = currentState.freeRotations

        if (!alreadyPaidForThisSelection) {
            if (newFreeRotations > 0) {
                newFreeRotations -= 1
                // persist free rotations immediately so survives process death
                saveFreeRotations(newFreeRotations)
            } else if (newCoins >= ROTATION_COST) {
                newCoins -= ROTATION_COST
                saveCoins(newCoins)
                saveLifetimeCoinsIfHigher(newCoins)
            } else {
                // No free rotations and not enough coins — show the zero-coins dialog prompting shop or ad
                _uiState.update { it.copy(showZeroCoinsDialog = true) }
                return
            }
        }

        // Perform rotation on the selected block (90deg clockwise)
        val rotated = currentBlock.rotate()

        // If we consumed payment/free-rotation now, mark the selection token so we don't charge again for same selection
        if (!alreadyPaidForThisSelection) {
            _uiState.update { it.copy(rotationPaidSelectionToken = currentState.selectionToken) }
        }

        Log.d("GameViewModel", "rotateSelectedBlock -> rotatedId=${rotated.id} newFreeRotations=$newFreeRotations newCoins=$newCoins")

        // Update last rotated id in prefs and update UI state
        prefs.edit().putInt(KEY_LAST_ROTATED_ID, rotated.id).apply()

        _uiState.update {
            it.copy(
                selectedBlock = rotated,
                freeRotations = newFreeRotations,
                coins = newCoins,
                lastRotatedBlockId = rotated.id
            )
        }

        // Persist snapshot after rotation
        val post = _uiState.value
        persistGameSnapshot(
            board = post.board,
            blocks = post.availableBlocks,
            coins = post.coins,
            rainbowCount = post.rainbowBlockCount,
            colorWipeCount = post.colorWipeCount,
            score = post.score,
            meterValue = post.specialMeterValue,
            freeRotations = post.freeRotations,
            lastRotatedBlockId = post.lastRotatedBlockId,
            selectedBlockId = post.selectedBlock?.id,
            isGameOver = post.isGameOver,
            isLastChance = post.isLastChance
        )
    }

    /** Select the Rainbow special block as the active preview (if available). */
    fun selectRainbowBlock() {
        val state = _uiState.value
        if (state.rainbowBlockCount <= 0) {
            _uiState.update { it.copy(showZeroCoinsDialog = true) }
            return
        }
        _uiState.update {
            it.copy(selectedBlock = RAINBOW_BLOCK, selectionToken = it.selectionToken + 1L,
                rotation = 0, // ensure rainbow selection also resets rotation state
                rotationPaidSelectionToken = 0L
            )
        }
        saveSelectedBlockId(RAINBOW_BLOCK.id)
    }

    /** Dismiss the trophy / tier promotion dialog. */
    fun dismissTierPromotion() {
        _uiState.update { it.copy(showTierPromotionDialog = false, newlyUnlockedTier = null) }
    }

    /** Add coins to player (used for rewards / ads). */
    fun addCoins(amount: Int) {
        if (amount <= 0) return
        val newTotal = _uiState.value.coins + amount
        saveCoins(newTotal)
        saveLifetimeCoinsIfHigher(newTotal)
        _uiState.update { it.copy(coins = newTotal, coinsEarnedThisUpdate = amount) }
    }

    /** Hide the zero-coins dialog. */
    fun dismissZeroCoinsDialog() {
        _uiState.update { it.copy(showZeroCoinsDialog = false) }
    }

    // --- Small helper to persist free rotations count ---
    private fun saveFreeRotations(count: Int) {
        prefs.edit().putInt(KEY_FREE_ROTATIONS, count).apply()
    }

    // Dismiss the first-install free coins dialog and persist that it was shown
    fun dismissFirstInstallFreeCoinsDialog() {
        prefs.edit().putInt(KEY_PREFS_SCHEMA_VERSION, PREFS_SCHEMA_VERSION).apply()
        _uiState.update { it.copy(showFirstInstallFreeCoinsDialog = false) }
    }

}
