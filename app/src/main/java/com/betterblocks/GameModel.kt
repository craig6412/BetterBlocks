package com.betterblocks

// ---------------------------
// 1. Core Data Models
// ---------------------------

/**
 * Represents a coordinate pair (row, col) on the board or relative to a block's origin.
 */
data class Coord(val row: Int, val col: Int)

/**
 * Type alias for the game board grid: a 2D array of nullable color strings.
 * A non-null string means the cell is occupied by a block of that color.
 */
typealias GameGrid = Array<Array<String?>>

/**
 * Common color palette for blocks, matching a vibrant theme.
 */
val BLOCK_COLOR_HEX = listOf(
    "#3b82f6", // Blue
    "#f59e0b", // Amber
    "#10b981", // Emerald
    "#ef4444", // Red
    "#8b5cf6", // Violet
    "#ec4899", // Pink
    "#06b6d4", // Cyan
    "#eab308", // Yellow
    "#f472b6", // Rose
    "#fbbf24", // Yellow-Amber
    "#9333ea", // Purple
    "#4ade80", // Light Green
    "#f97316", // Orange
    "#a855f7", // Fuchsia
    "#d946ef", // Magenta
    "#6366f1", // Indigo
    "#14b8a6", // Teal
    "#65a30d", // Lime
    "#facc15"  // Gold
)

/**
 * Represents a single block shape and color.
 * @param name A descriptive name for the block shape.
 *S * @param color The hex color string for the block.
 * @param shape A list of coordinates (row, col) relative to the top-left corner (0,0) of the block's bounding box.
 */
data class Block(
    val id: Int,
    val name: String,
    val color: String,
    val shape: List<Coord>
) {
    // Helper to get the bounding dimensions for rendering
    val boundingBoxWidth: Int
        get() = (shape.maxOfOrNull { it.col } ?: -1) + 1

    val boundingBoxHeight: Int
        get() = (shape.maxOfOrNull { it.row } ?: -1) + 1
}

/**
 * Extension function to rotate the block 90 degrees clockwise.
 */
fun Block.rotate(): Block {
    // Find the new height, which will be the old width
    val newHeight = this.boundingBoxWidth

    val newShape = this.shape.map { coord ->
        // Rotation logic: (row, col) -> (col, newHeight - 1 - row)
        // This correctly pivots the block within its bounding box
        Coord(
            row = coord.col,
            col = newHeight - 1 - coord.row
        )
    }

    // Normalize the shape to be at (0,0) origin again
    val minRow = newShape.minOfOrNull { it.row } ?: 0
    val minCol = newShape.minOfOrNull { it.col } ?: 0
    val normalizedShape = newShape.map { Coord(it.row - minRow, it.col - minCol) }

    return this.copy(shape = normalizedShape)
}

/**
 * Holds the entire visible state of the game for the UI to render.
 */
data class GameUiState(
    val board: GameGrid,
    val availableBlocks: List<Block>,
    val score: Int = 0,
    val isGameOver: Boolean = false,
    val selectedBlock: Block? = null
) {
    // Custom equals/hashcode for array comparison
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as GameUiState
        if (!board.contentDeepEquals(other.board)) return false
        if (availableBlocks != other.availableBlocks) return false
        if (score != other.score) return false
        if (isGameOver != other.isGameOver) return false
        if (selectedBlock != other.selectedBlock) return false
        return true
    }

    override fun hashCode(): Int {
        var result = board.contentDeepHashCode()
        result = 31 * result + availableBlocks.hashCode()
        result = 31 * result + score
        result = 31 * result + isGameOver.hashCode()
        result = 31 * result + (selectedBlock?.hashCode() ?: 0)
        return result
    }
}

/**
 * A helper class to return the result of a line-clearing check.
 */
data class ClearResult(
    val newBoard: GameGrid,
    val totalClears: Int // Total number of rows, columns, and/or zones cleared
)

// ---------------------------
// 2. Block Definitions (Full 19 Set)
// ---------------------------

val BLOCK_MANAGER: List<Block> = listOf(
    // 1-Cell
    Block(1, "1x1 Dot", BLOCK_COLOR_HEX[0], listOf(Coord(0, 0))),

    // 2-Cell
    Block(2, "1x2 Bar", BLOCK_COLOR_HEX[1], listOf(Coord(0, 0), Coord(0, 1))),

    // 3-Cell
    Block(3, "1x3 Bar", BLOCK_COLOR_HEX[2], listOf(Coord(0, 0), Coord(0, 1), Coord(0, 2))),
    Block(4, "L Triomino", BLOCK_COLOR_HEX[3], listOf(Coord(0, 0), Coord(1, 0), Coord(1, 1))),

    // 4-Cell (Tetrominos)
    Block(5, "1x4 Bar", BLOCK_COLOR_HEX[4], listOf(Coord(0, 0), Coord(0, 1), Coord(0, 2), Coord(0, 3))),
    Block(6, "Square", BLOCK_COLOR_HEX[5], listOf(Coord(0, 0), Coord(0, 1), Coord(1, 0), Coord(1, 1))),
    Block(7, "L Tetromino", BLOCK_COLOR_HEX[6], listOf(Coord(0, 0), Coord(1, 0), Coord(2, 0), Coord(2, 1))),
    Block(8, "J Tetromino", BLOCK_COLOR_HEX[7], listOf(Coord(0, 1), Coord(1, 1), Coord(2, 1), Coord(2, 0))),
    Block(9, "T Tetromino", BLOCK_COLOR_HEX[8], listOf(Coord(0, 0), Coord(0, 1), Coord(0, 2), Coord(1, 1))),
    Block(10, "S Tetromino", BLOCK_COLOR_HEX[9], listOf(Coord(0, 1), Coord(0, 2), Coord(1, 0), Coord(1, 1))),
    Block(11, "Z Tetromino", BLOCK_COLOR_HEX[10], listOf(Coord(0, 0), Coord(0, 1), Coord(1, 1), Coord(1, 2))),

    // 5-Cell (Pentominos - simplified set for this game)
    Block(12, "1x5 Bar", BLOCK_COLOR_HEX[11], listOf(Coord(0, 0), Coord(0, 1), Coord(0, 2), Coord(0, 3), Coord(0, 4))),
    Block(13, "L Pentomino", BLOCK_COLOR_HEX[12], listOf(Coord(0, 0), Coord(1, 0), Coord(2, 0), Coord(3, 0), Coord(3, 1))),
    Block(14, "Plus Pentomino", BLOCK_COLOR_HEX[13], listOf(Coord(0, 1), Coord(1, 0), Coord(1, 1), Coord(1, 2), Coord(2, 1))),
    Block(15, "U Pentomino", BLOCK_COLOR_HEX[14], listOf(Coord(0, 0), Coord(0, 2), Coord(1, 0), Coord(1, 1), Coord(1, 2))),
    Block(16, "Wide T", BLOCK_COLOR_HEX[15], listOf(Coord(0, 0), Coord(0, 1), Coord(0, 2), Coord(1, 1), Coord(2, 1))),

    // 9-Cell (Large Square)
    Block(17, "3x3 Square", BLOCK_COLOR_HEX[16], listOf(
        Coord(0, 0), Coord(0, 1), Coord(0, 2),
        Coord(1, 0), Coord(1, 1), Coord(1, 2),
        Coord(2, 0), Coord(2, 1), Coord(2, 2)
    )),
    // Misc
    Block(18, "Small L", BLOCK_COLOR_HEX[17], listOf(Coord(0, 0), Coord(1, 0))), // 2-cell L
    Block(19, "Small T", BLOCK_COLOR_HEX[18], listOf(Coord(0, 0), Coord(0, 1), Coord(0, 2), Coord(1, 1))) // Same as T-Tetro
)