package com.betterblocks

/**
 * Smart Preview Generator for BetterBlocks
 *
 * This module provides intelligent block preview generation that adapts to the current board state.
 * It ensures players receive blocks they can place, and it pre-orients those blocks toward
 * the best board-aware placement most of the time.
 */

// Centralized difficulty enum and config
enum class Difficulty { EASY, NORMAL, HARD }

object DifficultyConfig {
    // Tunables per difficulty (probability modifiers / thresholds)
    val largeProbabilityModifier: Map<Difficulty, Float> = mapOf(
        Difficulty.EASY to 0.4f,    // reduce chance of large blocks
        Difficulty.NORMAL to 1.0f,
        Difficulty.HARD to 1.6f     // increase chance of large blocks on hard
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

private data class PlacementScore(
    val block: Block,
    val row: Int,
    val col: Int,
    val score: Int
)

// Helper to determine if a block is 'awkward' (ST_2 or large)
private fun isAwkwardBlock(block: Block): Boolean {
    if (block.name == "2-High Staircase") return true
    return block.shape.size >= 7
}

private fun normalizedShapeKey(block: Block): String {
    return block.shape
        .sortedWith(compareBy<Coord> { it.row }.thenBy { it.col })
        .joinToString("|") { "${it.row},${it.col}" }
}

private fun uniqueRotations(block: Block): List<Block> {
    val result = mutableListOf<Block>()
    val seen = mutableSetOf<String>()
    var current = block

    repeat(4) {
        val key = normalizedShapeKey(current)
        if (seen.add(key)) {
            result.add(current)
        }
        current = current.rotate()
    }

    return result
}

private fun canPlaceAt(board: GameGrid, block: Block, row: Int, col: Int): Boolean {
    val rows = board.size
    val cols = if (rows > 0) board[0].size else 0

    if (rows == 0 || cols == 0) return false

    for (cell in block.shape) {
        val r = row + cell.row
        val c = col + cell.col

        if (r !in 0 until rows || c !in 0 until cols) return false
        if (board[r][c] != null) return false
    }

    return true
}

private fun simulatePlacement(board: GameGrid, block: Block, row: Int, col: Int): GameGrid {
    val copy = board.map { it.clone() }.toTypedArray()

    for (cell in block.shape) {
        val r = row + cell.row
        val c = col + cell.col
        copy[r][c] = block.colorResId
    }

    return copy
}

private fun countCompletedLines(board: GameGrid): Int {
    val rows = board.size
    val cols = if (rows > 0) board[0].size else 0

    if (rows == 0 || cols == 0) return 0

    var lines = 0

    for (r in 0 until rows) {
        if (board[r].all { it != null }) lines++
    }

    for (c in 0 until cols) {
        var full = true
        for (r in 0 until rows) {
            if (board[r][c] == null) {
                full = false
                break
            }
        }
        if (full) lines++
    }

    return lines
}

private fun countAlmostCompletedLines(board: GameGrid): Int {
    val rows = board.size
    val cols = if (rows > 0) board[0].size else 0

    if (rows == 0 || cols == 0) return 0

    var almost = 0

    for (r in 0 until rows) {
        val filled = board[r].count { it != null }
        if (filled == cols - 1) almost++
    }

    for (c in 0 until cols) {
        var filled = 0
        for (r in 0 until rows) {
            if (board[r][c] != null) filled++
        }
        if (filled == rows - 1) almost++
    }

    return almost
}

private fun countEmptyCells(board: GameGrid): Int {
    return board.sumOf { row -> row.count { it == null } }
}

private fun countRemainingMobility(board: GameGrid, allBlocks: List<Block>): Int {
    return allBlocks.count { candidate ->
        uniqueRotations(candidate).any { rotated ->
            canPlaceBlock(board, rotated)
        }
    }
}

private fun bestOrientedBlockForBoard(
    board: GameGrid,
    block: Block,
    allBlocks: List<Block>,
    difficulty: Difficulty = Difficulty.NORMAL
): PlacementScore? {
    if (block.isSpecial) return null

    if (block.name == "2-High Staircase" && !hasPerfectFitForST2(board, block, difficulty)) {
        return null
    }

    val rows = board.size
    val cols = if (rows > 0) board[0].size else 0

    if (rows == 0 || cols == 0) return null

    val currentEmptyCells = countEmptyCells(board)
    val isCrowded = currentEmptyCells <= 24
    var best: PlacementScore? = null

    for (rotated in uniqueRotations(block)) {
        for (row in 0 until rows) {
            for (col in 0 until cols) {
                if (!canPlaceAt(board, rotated, row, col)) continue

                val simulated = simulatePlacement(board, rotated, row, col)
                val completedLines = countCompletedLines(simulated)
                val almostLines = countAlmostCompletedLines(simulated)
                val mobility = countRemainingMobility(simulated, allBlocks)

                var score = 0

                // Biggest priority: give the player blocks that immediately clear.
                score += completedLines * 10_000

                // Second: set up near-clears.
                score += almostLines * 350

                // Third: preserve future playable options.
                score += mobility * 150

                // Help crowded boards by favoring smaller/easier blocks.
                if (isCrowded) {
                    score += (10 - rotated.shape.size).coerceAtLeast(0) * 120
                }

                // Light bonus for not being awkward.
                if (!isAwkwardBlock(rotated)) {
                    score += 75
                }

                // Small center preference so pieces do not always hug the corner.
                val centerRow = rows / 2
                val centerCol = cols / 2
                val centerDistance = kotlin.math.abs(row - centerRow) + kotlin.math.abs(col - centerCol)
                score -= centerDistance * 5

                val placement = PlacementScore(
                    block = rotated,
                    row = row,
                    col = col,
                    score = score
                )

                if (best == null || placement.score > best.score) {
                    best = placement
                }
            }
        }
    }

    return best
}

/**
 * Checks if a given block can fit anywhere on the current board.
 *
 * This version checks all unique rotations so helper logic can find playable pieces even when
 * the block's default authored orientation would not fit.
 */
fun canBlockFit(board: GameGrid, block: Block): Boolean {
    return uniqueRotations(block).any { rotated ->
        canPlaceBlock(board, rotated)
    }
}

/**
 * Update: ensure perfect-fit logic is only applied to ST_2 by name.
 */
fun hasPerfectFitForST2(board: GameGrid, block: Block, difficulty: Difficulty = Difficulty.NORMAL): Boolean {
    if (block.name != "2-High Staircase") return false

    val rows = board.size
    val cols = if (rows > 0) board[0].size else 0

    if (rows == 0 || cols == 0) return false

    // Prevent early / empty-board ST_2 unless difficulty allows lower threshold
    val occupied = board.sumOf { row -> row.count { it != null } }
    val threshold = DifficultyConfig.st2OccupiedThreshold[difficulty] ?: 6
    if (occupied < threshold) return false

    for (rotated in uniqueRotations(block)) {
        for (row in 0 until rows) {
            for (col in 0 until cols) {
                if (!canPlaceAt(board, rotated, row, col)) continue

                var constraints = 0

                for (cell in rotated.shape) {
                    val r = row + cell.row
                    val c = col + cell.col

                    val neighbors = listOf(
                        r - 1 to c,
                        r + 1 to c,
                        r to c - 1,
                        r to c + 1
                    )

                    for ((nr, nc) in neighbors) {
                        if (nr !in 0 until rows || nc !in 0 until cols || board[nr][nc] != null) {
                            constraints++
                        }
                    }
                }

                if (constraints >= 3) return true
            }
        }
    }

    return false
}

/**
 * Generates a smart preview of 3 blocks based on the current board state.
 *
 * This now uses the same board-aware inventory helper as the main game path, so previews
 * are selected and oriented using the upgraded placement scoring.
 */
fun generateSmartPreview(board: GameGrid, allBlocks: List<Block>): List<Block> {
    return getSmartInventory(
        board = board,
        allBlocks = allBlocks,
        difficulty = Difficulty.NORMAL
    )
}

/**
 * Extension function to check if the board is getting critically full.
 *
 * @return true if less than 20% of the board is empty
 */
fun GameGrid.isCriticallyFull(): Boolean {
    if (this.isEmpty() || this[0].isEmpty()) return false

    val totalCells = this.size * this[0].size
    val occupiedCells = this.sumOf { row -> row.count { it != null } }
    val emptyPercentage = (totalCells - occupiedCells).toFloat() / totalCells

    return emptyPercentage < 0.20f
}

/**
 * Utility function to count how many blocks from a list can fit on the board.
 *
 * Useful for debugging and analytics.
 */
fun countFittingBlocks(board: GameGrid, blocks: List<Block>): Int {
    return blocks.count { canBlockFit(board, it) }
}

/**
 * Gets a difficulty-adjusted preview based on the player's progress.
 *
 * This variant maps the float difficulty into the centralized enum and then uses the
 * board-aware inventory helper.
 */
fun generateDifficultyAdjustedPreview(
    board: GameGrid,
    allBlocks: List<Block>,
    difficulty: Float = 0.5f
): List<Block> {
    val mappedDifficulty = when {
        difficulty < 0.3f -> Difficulty.EASY
        difficulty > 0.7f -> Difficulty.HARD
        else -> Difficulty.NORMAL
    }

    return getSmartInventory(
        board = board,
        allBlocks = allBlocks,
        difficulty = mappedDifficulty
    )
}

/**
 * Non-mutating theoretical placement check for Block (runtime Block).
 * Returns true if given Block can be placed anywhere on `board`.
 */
fun canPlaceBlock(board: GameGrid, block: Block): Boolean {
    val rows = board.size
    val cols = if (rows > 0) board[0].size else 0

    if (rows == 0 || cols == 0) return false

    for (row in 0 until rows) {
        for (col in 0 until cols) {
            if (canPlaceAt(board, block, row, col)) {
                return true
            }
        }
    }

    return false
}

/**
 * Generates a 3-block inventory where the helper analyzes the board, scores candidate
 * placements, and returns blocks already rotated into their strongest orientation.
 *
 * The old version only ensured at least one block was placeable. This version makes the
 * whole preview intentionally helpful while still keeping some difficulty and variety.
 */
fun getSmartInventory(
    board: GameGrid,
    allBlocks: List<Block>,
    maxAttempts: Int = 12,
    difficulty: Difficulty = Difficulty.NORMAL
): List<Block> {
    val currentEmptyCells = countEmptyCells(board)
    val isCrowded = currentEmptyCells <= 24
    val isCritical = currentEmptyCells <= 14

    val scoredCandidates = allBlocks
        .filter { !it.isSpecial }
        .mapNotNull { block ->
            bestOrientedBlockForBoard(
                board = board,
                block = block,
                allBlocks = allBlocks,
                difficulty = difficulty
            )
        }
        .sortedByDescending { it.score }

    if (scoredCandidates.isEmpty()) {
        val emergencyBlocks = allBlocks
            .filter { !it.isSpecial && it.shape.size <= 2 }
            .shuffled()
            .take(BLOCKS_PER_ROUND)

        if (emergencyBlocks.isNotEmpty()) {
            return emergencyBlocks
        }

        return allBlocks
            .filter { !it.isSpecial }
            .shuffled()
            .take(BLOCKS_PER_ROUND)
    }

    val result = mutableListOf<Block>()

    // Always give the single best board-aware block first.
    result.add(scoredCandidates.first().block)

    val remaining = scoredCandidates
        .drop(1)
        .filter { candidate ->
            result.none { it.name == candidate.block.name }
        }

    val easyPool = remaining.filter { it.block.shape.size <= 4 }
    val mediumPool = remaining.filter { it.block.shape.size in 5..6 }
    val largePool = remaining.filter { it.block.shape.size >= 7 }

    when {
        isCritical -> {
            result.addAll(easyPool.take(BLOCKS_PER_ROUND - result.size).map { it.block })
        }

        isCrowded -> {
            result.addAll(easyPool.take(1).map { it.block })

            if (result.size < BLOCKS_PER_ROUND) {
                result.addAll(
                    (easyPool.drop(1) + mediumPool)
                        .filter { candidate -> result.none { it.name == candidate.block.name } }
                        .take(BLOCKS_PER_ROUND - result.size)
                        .map { it.block }
                )
            }
        }

        else -> {
            val largeModifier = DifficultyConfig.largeProbabilityModifier[difficulty] ?: 1.0f

            val balancedPool = mutableListOf<PlacementScore>()
            balancedPool.addAll(easyPool.take(4))
            balancedPool.addAll(mediumPool.take(5))

            if (largeModifier >= 1.0f) {
                balancedPool.addAll(largePool.take(2))
            } else {
                balancedPool.addAll(largePool.take(1))
            }

            result.addAll(
                balancedPool
                    .filter { candidate -> result.none { it.name == candidate.block.name } }
                    .shuffled()
                    .sortedByDescending { it.score }
                    .take(BLOCKS_PER_ROUND - result.size)
                    .map { it.block }
            )
        }
    }

    // Fill any missing slots with the best remaining oriented blocks.
    if (result.size < BLOCKS_PER_ROUND) {
        result.addAll(
            scoredCandidates
                .map { it.block }
                .filter { candidate -> result.none { it.name == candidate.name } }
                .take(BLOCKS_PER_ROUND - result.size)
        )
    }

    // Absolute fallback: keep exactly 3.
    if (result.size < BLOCKS_PER_ROUND) {
        result.addAll(
            allBlocks
                .filter { !it.isSpecial }
                .filter { candidate -> result.none { it.name == candidate.name } }
                .shuffled()
                .take(BLOCKS_PER_ROUND - result.size)
        )
    }

    // Final safety: no more than one awkward block.
    val trimmed = result.take(BLOCKS_PER_ROUND).toMutableList()
    val awkwardCount = trimmed.count { isAwkwardBlock(it) }

    if (awkwardCount > DifficultyConfig.maxAwkwardPerInventory) {
        val replacement = scoredCandidates
            .map { it.block }
            .firstOrNull { candidate ->
                !isAwkwardBlock(candidate) && trimmed.none { it.name == candidate.name }
            }

        if (replacement != null) {
            val index = trimmed.indexOfLast { isAwkwardBlock(it) }
            if (index >= 0) {
                trimmed[index] = replacement
            }
        }
    }

    return trimmed.take(BLOCKS_PER_ROUND)
}
