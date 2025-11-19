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
const val ANIMATION_DURATION_MS = 900L
const val COIN_REWARD_THRESHOLD = 1000
const val COINS_PER_REWARD = 10
const val ROTATION_COST = 10
const val INITIAL_FREE_ROTATIONS = 3

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
            showHighScoreAnim = false // Banner hidden by default
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

    // --- Public Actions (Called by the UI) ---

    fun resetGame() {
        val currentCoins = _uiState.value.coins
        val currentHighScore = _uiState.value.highScore

        // Reset UI state
        _uiState.value = createInitialState().copy(
            coins = currentCoins,
            highScore = currentHighScore,
            freeRotations = INITIAL_FREE_ROTATIONS,
            showHighScoreAnim = false
        )

        // Reset the score to beat back to the high score
        scoreToBeat = currentHighScore
    }

    fun selectBlock(block: Block) {
        if (_uiState.value.isGameOver) return

        _uiState.update { currentState ->
            val newSelectedBlock = if (currentState.selectedBlock == block) null else block
            currentState.copy(selectedBlock = newSelectedBlock)
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

        val newAvailableBlocks = currentState.availableBlocks.map {
            if (it.id == currentBlock.id) rotatedBlock else it
        }

        _uiState.update {
            it.copy(
                availableBlocks = newAvailableBlocks,
                selectedBlock = rotatedBlock,
                coins = newCoins,
                freeRotations = newFreeRotations,
                lastRotatedBlockId = currentBlock.id
            )
        }
    }

    /**
     * Main game loop trigger: handles placement, animation, and scoring.
     */
    fun onGridCellClicked(startRow: Int, startCol: Int) {
        val currentState = _uiState.value
        val selectedBlock = currentState.selectedBlock

        if (selectedBlock == null || currentState.isGameOver) return

        if (!canPlaceBlock(selectedBlock, startRow, startCol, currentState.board)) {
            return // Invalid placement
        }

        viewModelScope.launch {
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
            val newScore = currentState.score + clearScore

            // --- HIGH SCORE CHECK ---
            var currentHighScore = currentState.highScore
            var triggerHighScoreAnim = false

            if (newScore > scoreToBeat) {
                // New High Score achieved!
                currentHighScore = newScore
                saveHighScore(currentHighScore)

                // Trigger animation and ensure we don't trigger it again this session
                triggerHighScoreAnim = true
                scoreToBeat = Int.MAX_VALUE
            } else if (newScore > currentHighScore) {
                // Just updating the number if we are already past the "beat" threshold
                currentHighScore = newScore
                saveHighScore(currentHighScore)
            }

            // --- Calculate Coins Earned ---
            val scoreBefore = currentState.score
            val coinsEarned = ((newScore / COIN_REWARD_THRESHOLD) - (scoreBefore / COIN_REWARD_THRESHOLD)) * COINS_PER_REWARD
            val newTotalCoins = currentState.coins + coinsEarned
            if (coinsEarned > 0) saveCoins(newTotalCoins)

            val isGameOver = isGameOver(finalBlocks, boardAfterClears)

            // Commit final state to UI
            _uiState.update {
                it.copy(
                    board = boardAfterClears,
                    availableBlocks = finalBlocks,
                    score = newScore,
                    highScore = currentHighScore,
                    coins = newTotalCoins,
                    isGameOver = isGameOver,
                    selectedBlock = null, // Deselect block
                    lastRotatedBlockId = null, // Reset rotation status
                    clearingCells = emptySet(), // Clear animation state
                    showHighScoreAnim = triggerHighScoreAnim // Show banner if true
                )
            }

            // --- 4. HIDE BANNER AFTER DELAY ---
            if (triggerHighScoreAnim) {
                delay(3000) // Show banner for 3 seconds
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
        for (coord in block.shape) {
            val boardRow = startRow + coord.row
            val boardCol = startCol + coord.col
            if (boardRow < 0 || boardRow >= GRID_SIZE || boardCol < 0 || boardCol >= GRID_SIZE) return false
            if (board[boardRow][boardCol] != null) return false
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