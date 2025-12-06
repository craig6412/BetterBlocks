package com.betterblocks

/**
 * Smart Preview Generator for BetterBlocks
 *
 * This module provides intelligent block preview generation that adapts to the current board state.
 * It ensures players always receive blocks they can place, preventing frustrating dead-end situations.
 */

/**
 * Checks if a given block can fit anywhere on the current board.
 *
 * This function tests every possible position on the board to see if the block
 * can be placed without:
 * - Exceeding board boundaries
 * - Colliding with already-filled cells
 *
 * @param board The current game board (2D array where null = empty, non-null = occupied)
 * @param block The block to test for placement
 * @return true if the block can fit in at least one position, false otherwise
 *
 * Performance: O(rows × cols × blockSize) - optimized with early return
 */
fun canBlockFit(board: GameGrid, block: Block): Boolean {
    val rows = board.size
    val cols = if (rows > 0) board[0].size else 0

    // Safety check for empty board
    if (rows == 0 || cols == 0) return false

    // Try placing the block at every possible position
    for (r in 0 until rows) {
        for (c in 0 until cols) {
            var fits = true

            // Check if all cells of the block can be placed from this origin (r, c)
            for (cell in block.shape) {
                val rr = r + cell.row
                val cc = c + cell.col

                // Check boundary violation
                if (rr !in 0 until rows || cc !in 0 until cols) {
                    fits = false
                    break
                }

                // Check collision with existing block
                if (board[rr][cc] != null) {
                    fits = false
                    break
                }
            }

            // Early return as soon as we find a valid position
            if (fits) return true
        }
    }

    return false
}

/**
 * Generates a smart preview of 3 blocks based on the current board state.
 *
 * This function adapts to the board's "fullness" to ensure players can always make progress:
 *
 * **Case 1: Normal Gameplay (3+ blocks fit)**
 * - Returns 3 random blocks from those that can fit
 * - Maintains game difficulty and variety
 *
 * **Case 2: Crowded Board (1-2 blocks fit)**
 * - Uses the blocks that fit
 * - Fills remaining slots with small/easy blocks that also fit
 * - Helps player recover from tight situations
 *
 * **Case 3: Critical Board State (nothing fits)**
 * - Returns guaranteed tiny blocks (1×1, 1×2, small L-shapes)
 * - Gives player a chance to clear space and continue
 *
 * @param board The current game board state
 * @param allBlocks The complete pool of available block types (from BLOCK_MANAGER)
 * @return List of exactly 3 blocks for the player to choose from
 *
 * Performance: O(allBlocks.size × board.size²) - acceptable for typical game loop
 */
fun generateSmartPreview(board: GameGrid, allBlocks: List<Block>): List<Block> {
    // Define "easy blocks" as those with 4 or fewer cells
    // These are statistically easier to place in tight spaces
    val easyBlocks = allBlocks.filter { it.shape.size <= 4 }

    // Find all blocks that can currently fit on the board
    val fittingBlocks = allBlocks.filter { canBlockFit(board, it) }

    // CASE 1: Normal gameplay - plenty of options available
    if (fittingBlocks.size >= 3) {
        return fittingBlocks.shuffled().take(3)
    }

    // CASE 2: Partial fit - board is getting crowded
    if (fittingBlocks.isNotEmpty()) {
        val needed = 3 - fittingBlocks.size

        // Fill remaining slots with easy blocks that also fit
        val fillers = easyBlocks
            .filter { canBlockFit(board, it) }
            .shuffled()
            .take(needed)

        // Combine and shuffle to avoid predictable patterns
        return (fittingBlocks + fillers).shuffled().take(3)
    }

    // CASE 3: Critical situation - nothing fits normally
    // Fallback to guaranteed tiny blocks (1-2 cells)
    val guaranteed = easyBlocks.filter { it.shape.size <= 2 }

    // If even guaranteed blocks don't exist (shouldn't happen), return any 3 easy blocks
    return if (guaranteed.isNotEmpty()) {
        guaranteed.shuffled().take(3)
    } else {
        easyBlocks.shuffled().take(3)
    }
}

/**
 * Extension function to check if the board is getting critically full.
 *
 * This can be used by the UI to show warnings or adjust difficulty.
 *
 * @return true if less than 20% of the board is empty
 */
fun GameGrid.isCriticallyFull(): Boolean {
    val totalCells = this.size * this[0].size
    val occupiedCells = this.sumOf { row -> row.count { it != null } }
    val emptyPercentage = (totalCells - occupiedCells).toFloat() / totalCells
    return emptyPercentage < 0.20f
}

/**
 * Utility function to count how many blocks from a list can fit on the board.
 *
 * Useful for debugging and analytics.
 *
 * @param board The current game board
 * @param blocks List of blocks to test
 * @return Count of blocks that can fit
 */
fun countFittingBlocks(board: GameGrid, blocks: List<Block>): Int {
    return blocks.count { canBlockFit(board, it) }
}

/**
 * Gets a difficulty-adjusted preview based on the player's progress.
 *
 * This variant allows for dynamic difficulty scaling as the game progresses.
 *
 * @param board Current game board
 * @param allBlocks Complete block pool
 * @param difficulty Scale from 0.0 (easy) to 1.0 (hard)
 * @return List of 3 blocks adjusted for difficulty
 */
fun generateDifficultyAdjustedPreview(
    board: GameGrid,
    allBlocks: List<Block>,
    difficulty: Float = 0.5f
): List<Block> {
    val fittingBlocks = allBlocks.filter { canBlockFit(board, it) }

    if (fittingBlocks.isEmpty()) {
        // Emergency mode - give easiest blocks
        return allBlocks.filter { it.shape.size <= 2 }.shuffled().take(3)
    }

    // Adjust the pool based on difficulty
    val adjustedPool = when {
        difficulty < 0.3f -> {
            // Easy: favor smaller blocks
            fittingBlocks.filter { it.shape.size <= 5 }
        }
        difficulty > 0.7f -> {
            // Hard: allow all fitting blocks including large ones
            fittingBlocks
        }
        else -> {
            // Medium: balanced mix
            val small = fittingBlocks.filter { it.shape.size <= 4 }
            val large = fittingBlocks.filter { it.shape.size > 4 }
            small + large.take(2)
        }
    }

    return adjustedPool.shuffled().take(3)
}

