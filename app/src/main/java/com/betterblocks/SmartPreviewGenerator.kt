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

// Centralized difficulty enum and config
enum class Difficulty { EASY, NORMAL, HARD }

object DifficultyConfig {
    // Tunables per difficulty (probability modifiers / thresholds)
    val largeProbabilityModifier: Map<Difficulty, Float> = mapOf(
        Difficulty.EASY to 0.4f,    // reduce chance of large blocks
        Difficulty.NORMAL to 1.0f,
        Difficulty.HARD to 1.6f      // increase chance of large blocks on hard
    )

    // Minimum occupied cells required before considering ST_2 perfect-fit logic
    val st2OccupiedThreshold: Map<Difficulty, Int> = mapOf(
        Difficulty.EASY to 12,
        Difficulty.NORMAL to 6,
        Difficulty.HARD to 4
    )

    // Maximum number of awkward/large blocks allowed in a single inventory
    val maxAwkwardPerInventory: Int = 1
}

// Helper to determine if a block is 'awkward' (ST_2 or large)
private fun isAwkwardBlock(block: Block): Boolean {
    if (block.name == "2-High Staircase") return true
    return block.shape.size >= 7
}

// Update: ensure perfect-fit logic is only applied to ST_2 by name
fun hasPerfectFitForST2(board: GameGrid, block: Block, difficulty: Difficulty = Difficulty.NORMAL): Boolean {
    if (block.name != "2-High Staircase") return false

    val size = board.size

    // Prevent early / empty-board ST_2 unless difficulty allows lower threshold
    val occupied = board.sumOf { row -> row.count { it != null } }
    val threshold = DifficultyConfig.st2OccupiedThreshold[difficulty] ?: 6
    if (occupied < threshold) return false

    for (row in 0 until size) {
        for (col in 0 until size) {

            var fits = true
            for (cell in block.shape) {
                val r = row + cell.row
                val c = col + cell.col
                if (r !in 0 until size || c !in 0 until size || board[r][c] != null) {
                    fits = false
                    break
                }
            }
            if (!fits) continue

            var constraints = 0
            for (cell in block.shape) {
                val r = row + cell.row
                val c = col + cell.col

                val neighbors = listOf(
                    r - 1 to c,
                    r + 1 to c,
                    r to c - 1,
                    r to c + 1
                )

                for ((nr, nc) in neighbors) {
                    if (nr !in 0 until size || nc !in 0 until size || board[nr][nc] != null) {
                        constraints++
                    }
                }
            }

            if (constraints >= 3) return true
        }
    }
    return false
}

/* ============================ */
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
    val fittingBlocks = allBlocks.filter { block ->
        if (!canBlockFit(board, block)) return@filter false
        if (block.name == "2-High Staircase") hasPerfectFitForST2(board, block) else true
    }

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
    val fittingBlocks = allBlocks.filter { block ->
        if (!canBlockFit(board, block)) return@filter false
        if (block.name == "2-High Staircase") hasPerfectFitForST2(board, block) else true
    }

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

/**
 * Non-mutating theoretical placement check for Block (runtime Block).
 * Returns true if given Block can be placed anywhere on `board`.
 */
fun canPlaceBlock(board: GameGrid, block: Block): Boolean {
    val size = board.size
    if (size == 0) return false

    for (row in 0 until size) {
        for (col in 0 until size) {
            var fits = true
            for (offset in block.shape) {
                val r = row + offset.row
                val c = col + offset.col
                if (r !in 0 until size || c !in 0 until size || board[r][c] != null) {
                    fits = false
                    break
                }
            }
            if (fits) return true
        }
    }

    return false
}

/**
 * Attempts up to [maxAttempts] to generate a 3-block inventory where at least one
 * block is placeable somewhere on the board. Falls back to a random inventory
 * if no such inventory can be generated (allowing legitimate game-over).
 */

fun getSmartInventory(
    board: GameGrid,
    allBlocks: List<Block>,
    maxAttempts: Int = 12,
    difficulty: Difficulty = Difficulty.NORMAL
): List<Block> {

    // Helper probability modifiers
    val largeModifier = DifficultyConfig.largeProbabilityModifier[difficulty] ?: 1.0f

    // Weighted category helpers
    val smallBlocks = allBlocks.filter { it.shape.size <= 4 }
    val mediumBlocks = allBlocks.filter { it.shape.size in 5..6 }
    val largeBlocks = allBlocks.filter { it.shape.size >= 7 }

    // Ensure at least one placeable block in returned inventory
    fun inventoryHasPlaceable(inv: List<Block>): Boolean = inv.any { canPlaceBlock(board, it) }

    // Try random inventories with bias towards difficulty
    repeat(maxAttempts) {
        val pool = mutableListOf<Block>().apply { addAll(allBlocks) }

        // Create a weighted shuffled pool: inflate large pool by modifier factor
        val weighted = mutableListOf<Block>()
        weighted.addAll(smallBlocks) // small always present once
        // Add medium once
        weighted.addAll(mediumBlocks)
        // Add large blocks multiple times based on modifier (rounded)
        val repeatLarge = (largeModifier).coerceAtLeast(1f).toInt()
        repeat(repeatLarge) { weighted.addAll(largeBlocks) }

        val inventory = weighted.shuffled().take(3)

        // Enforce awkward cap: ensure no more than configured awkwards
        if (inventory.count { isAwkwardBlock(it) } > DifficultyConfig.maxAwkwardPerInventory) {
            // reject this candidate
        } else if (inventoryHasPlaceable(inventory)) {
            return inventory
        }
    }

    // If random attempts failed, build a guaranteed-fit inventory using deterministic rules
    val fittingBlocks = allBlocks.filter { block ->
        if (!canBlockFit(board, block)) return@filter false
        // apply ST_2 perfect-fit check only for ST_2 (by name)
        if (block.name == "2-High Staircase") return@filter hasPerfectFitForST2(board, block, difficulty)
        true
    }

    if (fittingBlocks.isNotEmpty()) {
        // Choose a guaranteed fit block, preferring non-awkward unless none
        val preferred = fittingBlocks.firstOrNull { !isAwkwardBlock(it) } ?: fittingBlocks.random()

        // Fill remaining two slots with the easiest fitting blocks while enforcing awkward cap
        val others = (allBlocks.filter { it != preferred }
            .filter { canBlockFit(board, it) }
            .shuffled()
            .filter { !isAwkwardBlock(it) }
            .take(2)).toMutableList()

        // If we couldn't find non-awkward fillers, allow one awkward if needed (but cap total awkwards)
        while (others.size < 2) {
            val candidate = allBlocks.filter { it != preferred }.shuffled().firstOrNull()
            if (candidate == null) break
            if (listOf(preferred).count { isAwkwardBlock(it) } + others.count { isAwkwardBlock(it) } >= DifficultyConfig.maxAwkwardPerInventory) {
                // try to pick a non-awkward next
                val nonawk = allBlocks.filter { it != preferred && !isAwkwardBlock(it) }.firstOrNull()
                if (nonawk != null) others.add(nonawk) else break
            } else {
                others.add(candidate)
            }
        }

        val inv = listOf(preferred) + others
        // ensure placeable
        if (inventoryHasPlaceable(inv)) return inv.take(3)
    }

    // Absolute fallback: return 3 easiest blocks that exist (guaranteed smalls) or any 3
    val easy = allBlocks.filter { it.shape.size <= 4 }
    if (easy.isNotEmpty()) {
        // If any easy block can be placed, guarantee at least one is placeable
        val placeableEasy = easy.firstOrNull { canPlaceBlock(board, it) }
        if (placeableEasy != null) {
            val others = (easy.filter { it != placeableEasy }.shuffled().take(2)).toMutableList()
            return listOf(placeableEasy) + others
        }
        return easy.shuffled().take(3)
    }
    return allBlocks.shuffled().take(3)
}
