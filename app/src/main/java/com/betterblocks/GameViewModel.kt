package com.betterblocks

import androidx.lifecycle.ViewModel
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
const val ROTATION_COST = 10         // Cost in coins
const val INITIAL_FREE_ROTATIONS = 3 // Free rotations per game

class GameViewModel : ViewModel() {

    // --- State Management ---
    private val _uiState = MutableStateFlow(createInitialState())
    val uiState: StateFlow<GameUiState> = _uiState

    /**
     * Creates the initial state for a new game.
     */
    private fun createInitialState(): GameUiState {
        return GameUiState(
            board = Array(GRID_SIZE) { Array(GRID_SIZE) { null } },
            availableBlocks = generateNewBlocks(),
            score = 0,
            coins = 9999, // Starting coins (can be changed later)
            freeRotations = INITIAL_FREE_ROTATIONS,
            lastRotatedBlockId = null,
            isGameOver = false,
            selectedBlock = null,
            clearingCells = emptySet() // Initialize the animation set
        )
    }

    /**
     * Generates a new list of 3 random blocks from the BLOCK_MANAGER.
     */
    private fun generateNewBlocks(): List<Block> {
        return BLOCK_MANAGER.shuffled(Random).take(BLOCKS_PER_ROUND)
    }

    // --- Public Actions (Called by the UI) ---

    fun resetGame() {
        // Preserve coins across resets, but reset free rotations
        val currentCoins = _uiState.value.coins
        _uiState.value = createInitialState().copy(coins = currentCoins, freeRotations = INITIAL_FREE_ROTATIONS)
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
                lastRotatedBlockId = currentBlock.id // Mark as paid/used
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
            // MERGE: Placement score is now 0 (ignored) as per request
            val (boardAfterPlacement, _) = placeBlock(selectedBlock, startRow, startCol, currentState.board)
            val clearResult = checkForClears(boardAfterPlacement)

            // --- 2. START ANIMATION SEQUENCE ---
            if (clearResult.totalClears > 0) {

                // --- a) Update UI to show which cells are clearing (Animation starts here) ---
                _uiState.update {
                    it.copy(
                        // Temporarily place the block onto the grid for the clear animation to run
                        board = boardAfterPlacement,
                        // Set the coordinates that the UI should vaporize sequentially
                        clearingCells = getCoordsToAnimate(clearResult.newBoard, boardAfterPlacement)
                    )
                }

                // --- b) Pause execution for the duration of the animation ---
                // The UI's animation uses a stagger that adds up to roughly 600-900ms
                delay(ANIMATION_DURATION_MS)
            }

            // --- 3. FINAL STATE UPDATE (Post-Animation) ---

            // Get the final cleared board (which was calculated earlier)
            val boardAfterClears = clearResult.newBoard

            // Update Inventory
            val newAvailableBlocks = currentState.availableBlocks.filter { it.id != selectedBlock.id }
            val finalBlocks = if (newAvailableBlocks.isEmpty()) generateNewBlocks() else newAvailableBlocks

            // MERGE: Calculate Score (Only based on Clears)
            val clearScore = calculateClearScore(clearResult.totalClears)
            val newScore = currentState.score + clearScore

            // --- Calculate Coins Earned ---
            // Check how many 1000-point thresholds were crossed
            val scoreBefore = currentState.score
            val coinsEarned = ((newScore / COIN_REWARD_THRESHOLD) - (scoreBefore / COIN_REWARD_THRESHOLD)) * COINS_PER_REWARD
            val newTotalCoins = currentState.coins + coinsEarned

            // Check Game Over
            val isGameOver = isGameOver(finalBlocks, boardAfterClears)

            // Commit final state to UI
            _uiState.update {
                it.copy(
                    board = boardAfterClears, // Cleared board
                    availableBlocks = finalBlocks,
                    score = newScore,
                    coins = newTotalCoins, // Update coin count
                    isGameOver = isGameOver,
                    selectedBlock = null, // Deselect block
                    lastRotatedBlockId = null, // Reset rotation status on placement
                    clearingCells = emptySet() // Clear animation state
                )
            }
        }
    }

    // --- Private Game Logic Helpers ---

    /**
     * Determines which cells were cleared by comparing the board before and after clearing.
     */
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
            // Uses colorResId (Int) from GameModel.kt
            newBoard[startRow + coord.row][startCol + coord.col] = block.colorResId
        }
        // MERGE: Return 0 for placement score. Points are only awarded for clears.
        return Pair(newBoard, 0)
    }

    /**
     * Scans the board for any complete rows or columns (1010! style).
     */
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

    /**
     * MERGE: Calculates score with a quadratic multiplier for combos.
     * Formula: LinesCleared * 100 * LinesCleared
     * 1 Line = 100
     * 2 Lines = 400
     * 3 Lines = 900
     * 4 Lines = 1600
     */
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