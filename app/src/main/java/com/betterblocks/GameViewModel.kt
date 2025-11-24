package com.betterblocks

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random

// --- Constants ---
const val GRID_SIZE = 9
const val BLOCKS_PER_ROUND = 3
// Animation Timing Constants
const val ANIMATION_DURATION_MS = 900L // Total time the animation takes before the cells vanish
const val COIN_REWARD_THRESHOLD = 1000 // Points needed for reward
const val COINS_PER_REWARD = 10 // Coins given per threshold
const val ROTATION_COST = 10
const val INITIAL_FREE_ROTATIONS = 3
const val RAINBOW_BLOCK_SCORE = 1800 // Special score for board wipe

const val INITIAL_RAINBOW_COUNT = 3 // Standard start count
const val SPECIAL_METER_MAX = 5 // Meter fills in 5 combo steps

// --- DEVELOPER OVERRIDES ---
private const val DEV_INITIAL_COINS = 9999 // $9,999 for development
private const val DEV_INITIAL_RAINBOW = 99 // 99 blocks for development
private const val DEV_INITIAL_COLOR_WIPE = 5 // Start with 5 Color Wipes

// Persistence Keys
const val PREFS_NAME = "BetterBlocksPrefs"
const val KEY_HIGH_SCORE = "high_score"
const val KEY_COINS = "user_coins"
const val KEY_RAINBOW_COUNT = "user_rainbow_count"
const val KEY_COLOR_WIPE_COUNT = "user_color_wipe_count"
const val KEY_SOUND_ENABLED = "sound_enabled"
const val KEY_MUSIC_ENABLED = "music_enabled"

class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

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
        val savedCoins = prefs.getInt(KEY_COINS, DEV_INITIAL_COINS) // Use DEV override
        val savedRainbowCount = prefs.getInt(KEY_RAINBOW_COUNT, DEV_INITIAL_RAINBOW) // Use DEV override
        val savedColorWipeCount = prefs.getInt(KEY_COLOR_WIPE_COUNT, DEV_INITIAL_COLOR_WIPE) // Use DEV override
        val savedSound = prefs.getBoolean(KEY_SOUND_ENABLED, true)
        val savedMusic = prefs.getBoolean(KEY_MUSIC_ENABLED, true)

        // Set the target to beat for this session to the current high score
        scoreToBeat = savedHighScore

        return GameUiState(
            board = Array(GRID_SIZE) { Array(GRID_SIZE) { null } },
            availableBlocks = generateNewBlocks(),
            score = 0,
            highScore = savedHighScore,
            coins = savedCoins,
            freeRotations = INITIAL_FREE_ROTATIONS,
            lastRotatedBlockId = null,
            isGameOver = false,
            isLastChance = false, // <-- INIT P1 STATE
            selectedBlock = null,
            clearingCells = emptySet(),
            showHighScoreAnim = false, // Banner hidden by default
            rainbowBlockCount = savedRainbowCount, // <-- SET RAINBOW COUNT
            specialMeterValue = 0,
            isSoundEnabled = savedSound, // <-- SET TOGGLE
            isMusicEnabled = savedMusic // <-- SET TOGGLE
        )
    }

    private fun generateNewBlocks(): List<Block> {
        return BLOCK_MANAGER.shuffled(Random).take(BLOCKS_PER_ROUND)
    }

    // --- Persistence Helpers ---

    private fun saveHighScore(score: Int) {
        prefs.edit().putInt(KEY_HIGH_SCORE, score).apply()
    }

    private fun saveCoins(coins: Int) {
        prefs.edit().putInt(KEY_COINS, coins).apply()
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

    // --- Logic ---

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
                    specialMeterValue = 0
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
                // 3. Animate clearing
                _uiState.update { it.copy(clearingCells = cellsToClear) }
                delay(ANIMATION_DURATION_MS)

                // 4. Remove from board
                val newBoard = currentState.board.map { it.clone() }.toTypedArray()
                for (cell in cellsToClear) {
                    newBoard[cell.row][cell.col] = null
                }

                // 5. Score: 50 points per block
                val points = cellsToClear.size * 50

                updateScoreAndState(
                    _uiState.value,
                    currentState.availableBlocks,
                    currentState.availableBlocks,
                    points,
                    newBoard,
                    null,
                    0
                )
            }
        }
    }

    fun onLastChanceUsed() {
        val newCount = _uiState.value.rainbowBlockCount - 1
        _uiState.update {
            it.copy(
                isLastChance = false,
                selectedBlock = RAINBOW_BLOCK,
                rainbowBlockCount = newCount
            )
        }
        saveRainbowCount(newCount)
    }

    fun onLastChanceDeclined() {
        _uiState.update { it.copy(isLastChance = false, isGameOver = true) }
    }

    fun useRainbowWipeImmediately() {
        val currentState = _uiState.value
        if (currentState.isGameOver || currentState.isLastChance || currentState.rainbowBlockCount <= 0) return

        viewModelScope.launch {
            val newRainbowCount = currentState.rainbowBlockCount - 1
            saveRainbowCount(newRainbowCount)
            _uiState.update { it.copy(selectedBlock = null, rainbowBlockCount = newRainbowCount) }
            handleRainbowWipe(RAINBOW_BLOCK, 0, 0, currentState.copy(rainbowBlockCount = newRainbowCount))
        }
    }

    fun selectBlock(block: Block) {
        if (_uiState.value.isGameOver || _uiState.value.isLastChance) return

        _uiState.update { currentState ->
            val newSelectedBlock = if (currentState.selectedBlock == block) null else block
            currentState.copy(selectedBlock = newSelectedBlock)
        }
    }

    fun selectRainbowBlock() {
        val currentState = _uiState.value
        if (currentState.isLastChance || currentState.isGameOver || currentState.rainbowBlockCount <= 0) return

        val rainbowBlock = RAINBOW_BLOCK

        _uiState.update {
            // Toggle selection
            val newSelectedBlock = if (it.selectedBlock?.id == rainbowBlock.id) null else rainbowBlock
            it.copy(selectedBlock = newSelectedBlock)
        }
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

        // If it's a standard block, update it in the available list
        val newAvailableBlocks = if (!currentBlock.isSpecial) {
            currentState.availableBlocks.map {
                if (it.id == currentBlock.id) rotatedBlock else it
            }
        } else {
            currentState.availableBlocks // Don't update list for special blocks
        }

        _uiState.update {
            it.copy(
                availableBlocks = newAvailableBlocks,
                selectedBlock = rotatedBlock,
                coins = newCoins,
                freeRotations = newFreeRotations,
                lastRotatedBlockId = currentBlock.id // Mark as paid
            )
        }
    }

    fun onGridCellClicked(startRow: Int, startCol: Int) {
        val currentState = _uiState.value
        val selectedBlock = currentState.selectedBlock ?: return

        if (currentState.isGameOver || currentState.isLastChance) return

        if (!canPlaceBlock(selectedBlock, startRow, startCol, currentState.board)) {
            return // Invalid placement
        }

        viewModelScope.launch {

            // --- SPECIAL HANDLING FOR RAINBOW BLOCK ---
            if (selectedBlock.isSpecial && selectedBlock.id == 999) {
                handleRainbowWipe(selectedBlock, startRow, startCol, currentState)
                return@launch
            }

            // --- 1. PLACE BLOCK (Immediate State Change) ---
            val (boardAfterPlacement, _) = placeBlock(selectedBlock, startRow, startCol, currentState.board)
            val clearResult = checkForClears(boardAfterPlacement)

            // --- 2. START ANIMATION SEQUENCE ---
            if (clearResult.totalClears > 0) {
                // Update UI to show which cells are clearing
                _uiState.update {
                    it.copy(
                        board = boardAfterPlacement,
                        clearingCells = getCoordsToAnimate(clearResult.newBoard, boardAfterPlacement)
                    )
                }
                // Wait for animation to play out
                delay(ANIMATION_DURATION_MS)
            }

            // --- 3. FINAL STATE UPDATE (Post-Animation) ---

            val boardAfterClears = clearResult.newBoard
            val newAvailableBlocks = currentState.availableBlocks.filter { it.id != selectedBlock.id }
            val finalBlocks = if (newAvailableBlocks.isEmpty()) generateNewBlocks() else newAvailableBlocks

            // Calculate Score
            val clearScore = calculateClearScore(clearResult.totalClears)

            // Use helper to update everything
            updateScoreAndState(currentState, newAvailableBlocks, finalBlocks, clearScore, boardAfterClears, totalClears = clearResult.totalClears)
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
                clearingCells = allOccupiedCells
            )
        }

        // 3. Wait for animation
        delay(ANIMATION_DURATION_MS)

        // 4. Clear the board
        val emptyBoard = Array(GRID_SIZE) { Array<Int?>(GRID_SIZE) { null } }

        // 5. Final Update (Count already handled by caller)
        updateScoreAndState(
            currentState,
            currentState.availableBlocks,
            currentState.availableBlocks,
            RAINBOW_BLOCK_SCORE,
            emptyBoard,
            currentState.rainbowBlockCount
        )
    }

    /**
     * Consolidated logic for updating scores, high scores, coins, and game over state.
     */
    private fun updateScoreAndState(
        currentState: GameUiState,
        newAvailableBlocks: List<Block>,
        finalBlocks: List<Block>,
        pointsToAdd: Int,
        newBoard: GameGrid,
        newRainbowCount: Int? = null,
        totalClears: Int = 0
    ) {
        val newScore = currentState.score + pointsToAdd

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
        } else if (newScore > currentHighScore) {
            currentHighScore = newScore
            saveHighScore(currentHighScore)
        }

        // Coin Calc
        val scoreBefore = currentState.score
        val coinsEarned = ((newScore / COIN_REWARD_THRESHOLD) - (scoreBefore / COIN_REWARD_THRESHOLD)) * COINS_PER_REWARD
        val newTotalCoins = currentState.coins + coinsEarned
        if (coinsEarned > 0) saveCoins(newTotalCoins)

        val hasRainbowBlock = (newRainbowCount ?: currentState.rainbowBlockCount) > 0
        val isHardGameOver = isGameOver(finalBlocks, newBoard)
        var triggerLastChance = false
        var finalIsGameOver = false

        if (isHardGameOver) {
            if (hasRainbowBlock) {
                triggerLastChance = true
            } else {
                finalIsGameOver = true
            }
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

        _uiState.update {
            it.copy(
                board = newBoard,
                availableBlocks = finalBlocks,
                score = newScore,
                highScore = currentHighScore,
                coins = newTotalCoins,
                isGameOver = finalIsGameOver,
                isLastChance = triggerLastChance,
                selectedBlock = null,
                lastRotatedBlockId = null,
                clearingCells = emptySet(),
                showHighScoreAnim = triggerHighScoreAnim,
                rainbowBlockCount = finalRainbowCount,
                specialMeterValue = newMeterValue
            )
        }

        if (triggerHighScoreAnim) {
            viewModelScope.launch {
                delay(3000)
                _uiState.update { it.copy(showHighScoreAnim = false) }
            }
        }
    }

    // --- Private Game Logic Helpers ---

    private fun getCoordsToAnimate(clearedBoard: GameGrid, placementBoard: GameGrid): Set<Coord> {
        val coords = mutableSetOf<Coord>()
        for (r in 0 until GRID_SIZE) {
            for (c in 0 until GRID_SIZE) {
                if (placementBoard[r][c] != null && clearedBoard[r][c] == null) {
                    coords.add(Coord(r, c))
                }
            }
        }
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

    private fun calculateClearScore(totalClears: Int): Int {
        if (totalClears == 0) return 0
        val basePoints = 100
        return totalClears * basePoints * totalClears
    }

    private fun isGameOver(availableBlocks: List<Block>, board: GameGrid): Boolean {
        if (availableBlocks.isEmpty()) return false
        for (block in availableBlocks) {
            var currentBlock = block
            repeat(4) {
                for (r in 0 until GRID_SIZE) {
                    for (c in 0 until GRID_SIZE) {
                        if (canPlaceBlock(currentBlock, r, c, board)) return false
                    }
                }
                currentBlock = currentBlock.rotate()
            }
        }
        return true
    }
}