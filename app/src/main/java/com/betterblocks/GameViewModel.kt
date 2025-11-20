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

const val INITIAL_RAINBOW_COUNT = 3 // <-- UPDATED: Start with 3
const val SPECIAL_METER_MAX = 5 // <-- NEW: Meter fills in 5 combo steps

// Persistence Keys
const val PREFS_NAME = "BetterBlocksPrefs"
const val KEY_HIGH_SCORE = "high_score"
const val KEY_COINS = "user_coins"

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
        val savedCoins = prefs.getInt(KEY_COINS, 100) // Default 100 coins if new

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
            selectedBlock = null,
            clearingCells = emptySet(),
            showHighScoreAnim = false, // Banner hidden by default
            rainbowBlockCount = INITIAL_RAINBOW_COUNT, // <-- UPDATED to 3
            specialMeterValue = 0 // <-- ADDED
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

    // --- Special Meter Logic ---

    /**
     * Called when a player achieves a combo clear of 2 or more lines.
     * Fills the meter, or awards a Rainbow Block if full.
     */
    private fun onSpecialMeterFilled(currentRainbowCount: Int, currentMeterValue: Int) {
        // Since this is called only if totalClears >= 2, we increment by 1 each time.
        if (currentMeterValue + 1 >= SPECIAL_METER_MAX) {
            // Award block and reset meter
            _uiState.update {
                it.copy(
                    rainbowBlockCount = currentRainbowCount + 1,
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

    // --- Public Actions (Called by the UI) ---

    fun selectBlock(block: Block) {
        if (_uiState.value.isGameOver) return

        _uiState.update { currentState ->
            val newSelectedBlock = if (currentState.selectedBlock == block) null else block
            currentState.copy(selectedBlock = newSelectedBlock)
        }
    }

    /**
     * Selects the Rainbow Block for placement if available.
     */
    fun selectRainbowBlock() {
        val currentState = _uiState.value
        if (currentState.isGameOver || currentState.rainbowBlockCount <= 0) return

        val rainbowBlock = RAINBOW_BLOCK // Defined in GameModel.kt

        _uiState.update {
            // Toggle selection
            val newSelectedBlock = if (it.selectedBlock?.id == rainbowBlock.id) null else rainbowBlock
            it.copy(selectedBlock = newSelectedBlock)
        }
    }

    fun rotateSelectedBlock() {
        val currentState = _uiState.value
        if (currentState.isGameOver || currentState.selectedBlock == null) return

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

    /**
     * Main game loop trigger: handles placement, animation, and scoring.
     */
    fun onGridCellClicked(startRow: Int, startCol: Int) {
        val currentState = _uiState.value
        val selectedBlock = currentState.selectedBlock ?: return

        if (currentState.isGameOver) return

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
            // Placement score is 0
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

            // Use helper to update everything, passing totalClears
            updateScoreAndState(currentState, newAvailableBlocks, finalBlocks, clearScore, boardAfterClears, totalClears = clearResult.totalClears)
        }
    }

    /**
     * Logic for the Rainbow Block: Visual placement, Full Board Wipe animation, then Clear.
     */
    private suspend fun handleRainbowWipe(block: Block, startRow: Int, startCol: Int, currentState: GameUiState) {
        // 1. Temporarily place the rainbow block so the user sees it
        val (boardWithRainbow, _) = placeBlock(block, startRow, startCol, currentState.board)

        // 2. Trigger "Wipe" animation: Set ALL occupied cells to be clearing
        val allOccupiedCells = mutableSetOf<Coord>()
        for(r in 0 until GRID_SIZE) {
            for(c in 0 until GRID_SIZE) {
                if (boardWithRainbow[r][c] != null) {
                    allOccupiedCells.add(Coord(r, c))
                }
            }
        }

        _uiState.update {
            it.copy(
                board = boardWithRainbow,
                clearingCells = allOccupiedCells
            )
        }

        // 3. Wait for animation
        delay(ANIMATION_DURATION_MS)

        // 4. Clear the entire board
        val emptyBoard = Array(GRID_SIZE) { Array<Int?>(GRID_SIZE) { null } }

        // 5. Decrease rainbow count
        val newRainbowCount = currentState.rainbowBlockCount - 1

        // Reuse the update helper
        updateScoreAndState(
            currentState,
            currentState.availableBlocks,
            currentState.availableBlocks, // Don't change available blocks for special move
            RAINBOW_BLOCK_SCORE,
            emptyBoard,
            newRainbowCount
        )
    }

    /**
     * Consolidated logic for updating scores, high scores, coins, and game over state.
     */
    private fun updateScoreAndState(
        currentState: GameUiState,
        newAvailableBlocks: List<Block>, // Just used for Game Over check
        finalBlocks: List<Block>,       // The blocks to show next
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

            // Only trigger animation once per threshold beat
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

        val isGameOver = isGameOver(finalBlocks, newBoard)

        val currentRainbowCount = newRainbowCount ?: currentState.rainbowBlockCount
        var newMeterValue = currentState.specialMeterValue

        // --- SPECIAL METER CHECK ---
        if (totalClears >= 2) {
            // This will award the block or increment the meter, and performs its own update() call.
            onSpecialMeterFilled(currentRainbowCount, newMeterValue)

            // Re-fetch the state after the meter update has potentially awarded a block/reset the meter.
            val finalStateAfterMeterCheck = _uiState.value

            // Use these finalized values for the main update call below
            newMeterValue = finalStateAfterMeterCheck.specialMeterValue
            // If the meter awarded a block, finalStateAfterMeterCheck.rainbowBlockCount is +1,
            // but we must use the original 'newRainbowCount' if it was set (i.e., by handleRainbowWipe).
        }


        _uiState.update {
            it.copy(
                board = newBoard,
                availableBlocks = finalBlocks,
                score = newScore,
                highScore = currentHighScore,
                coins = newTotalCoins,
                isGameOver = isGameOver,
                selectedBlock = null, // Deselect block
                lastRotatedBlockId = null, // Reset rotation status
                clearingCells = emptySet(), // Clear animation state
                showHighScoreAnim = triggerHighScoreAnim,
                rainbowBlockCount = if (totalClears >= 2 && newRainbowCount == null) _uiState.value.rainbowBlockCount else (newRainbowCount ?: currentState.rainbowBlockCount),
                specialMeterValue = newMeterValue // Use the potentially updated value
            )
        }

        // Hide banner after delay
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
        // SPECIAL: Rainbow block ignores overlap, only checks bounds
        val ignoreCollision = block.isSpecial

        for (coord in block.shape) {
            val boardRow = startRow + coord.row
            val boardCol = startCol + coord.col
            if (boardRow < 0 || boardRow >= GRID_SIZE || boardCol < 0 || boardCol >= GRID_SIZE) return false

            // Standard logic: If not special, check for overlap
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

        // Count clears BEFORE physically clearing the board
        val uniqueRows = cellsToClear.map { it.row }.toSet().filter { r -> (0 until GRID_SIZE).all { c -> cellsToClear.contains(Coord(r, c)) } }.size
        val uniqueCols = cellsToClear.map { it.col }.toSet().filter { c -> (0 until GRID_SIZE).all { r -> cellsToClear.contains(Coord(r, c)) } }.size

        val totalClears = uniqueRows + uniqueCols

        // Create the final cleared board state
        val clearedBoard = board.map { it.clone() }.toTypedArray()
        for (cell in cellsToClear) {
            clearedBoard[cell.row][cell.col] = null
        }

        return ClearResult(clearedBoard, totalClears)
    }

    private fun calculateClearScore(totalClears: Int): Int {
        if (totalClears == 0) return 0
        val basePoints = 100
        // Quadratic multiplier: 1 line=100, 2=400, 3=900, 4=1600
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