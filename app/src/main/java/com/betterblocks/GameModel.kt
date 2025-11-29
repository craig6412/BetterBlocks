package com.betterblocks

import com.betterblocks.R
import com.betterblocks.model.TrophyTier


// ---------------------------
// 1. Core Data Models
// ---------------------------

/**
 * Represents a coordinate pair (row, col) on the board or relative to a block's origin.
 */
data class Coord(val row: Int, val col: Int)

/**
 * Type alias for the game board grid: a 2D array of nullable Resource IDs (Int).
 * A non-null value means the cell is occupied by a block with that drawable ID.
 */
typealias GameGrid = Array<Array<Int?>>

/**
 * Your custom drawable resources.
 * Index 0 = Blue, 1 = Green, 2 = Pink, 3 = Pumpkin Orange, 4 = Purple, 5 = Red, 6 = Yellow, 7 = Rainbow
 */
val BLOCK_DRAWABLES = listOf(
    R.drawable.blue,           // 0
    R.drawable.green,          // 1
    R.drawable.pink,           // 2
    R.drawable.pumpkin_orange, // 3
    R.drawable.purple,         // 4
    R.drawable.red,            // 5
    R.drawable.yellow,         // 6
    R.drawable.rainbow         // 7 (Rainbow Texture)
)

/**
 * Represents a single block shape and texture.
 * @param colorResId The Drawable Resource ID (Int) for the block's texture.
 */
data class Block(
    val id: Int,
    val name: String,
    val colorResId: Int,
    val shape: List<Coord>,
    val isSpecial: Boolean = false
) {
    val boundingBoxWidth: Int
        get() = (shape.maxOfOrNull { it.col } ?: -1) + 1

    val boundingBoxHeight: Int
        get() = (shape.maxOfOrNull { it.row } ?: -1) + 1
}

/**
 * Extension function to rotate the block 90 degrees clockwise.
 */
fun Block.rotate(): Block {
    val newHeight = this.boundingBoxWidth
    val newShape = this.shape.map { coord ->
        Coord(row = coord.col, col = newHeight - 1 - coord.row)
    }
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
    val highScore: Int = 0,
    val coins: Int = 0,
    val freeRotations: Int = 3,
    val lastRotatedBlockId: Int? = null,
    val isGameOver: Boolean = false,
    val isLastChance: Boolean = false,
    val selectedBlock: Block? = null,
    val clearingCells: Set<Coord> = emptySet(),
    val showHighScoreAnim: Boolean = false,
    val rainbowBlockCount: Int = 3,
    val colorWipeCount: Int = 3, // <-- ADDED: Color Wipe Inventory
    val specialMeterValue: Int = 0,
    val isSoundEnabled: Boolean = true,
    val isMusicEnabled: Boolean = true,
    val showZeroCoinsDialog: Boolean = false, // <-- ADDED: Zero Coins Dialog Fla
    val trophyTier: TrophyTier = TrophyTier.UNRANKED,
    val showTierPromotionDialog: Boolean = false,
    val newlyUnlockedTier: TrophyTier? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as GameUiState

        if (!board.contentDeepEquals(other.board)) return false
        if (availableBlocks != other.availableBlocks) return false
        if (score != other.score) return false
        if (highScore != other.highScore) return false
        if (coins != other.coins) return false
        if (freeRotations != other.freeRotations) return false
        if (lastRotatedBlockId != other.lastRotatedBlockId) return false
        if (isGameOver != other.isGameOver) return false
        if (isLastChance != other.isLastChance) return false
        if (selectedBlock != other.selectedBlock) return false
        if (clearingCells != other.clearingCells) return false
        if (showHighScoreAnim != other.showHighScoreAnim) return false
        if (rainbowBlockCount != other.rainbowBlockCount) return false
        if (colorWipeCount != other.colorWipeCount) return false // <-- ADDED
        if (specialMeterValue != other.specialMeterValue) return false
        if (isSoundEnabled != other.isSoundEnabled) return false
        if (isMusicEnabled != other.isMusicEnabled) return false
        if (showZeroCoinsDialog != other.showZeroCoinsDialog) return false // <-- ADDED
        if (showTierPromotionDialog != other.showTierPromotionDialog) return false // <-- ADDED
        if (newlyUnlockedTier != other.newlyUnlockedTier) return false // <-- ADDED

        return true
    }

    override fun hashCode(): Int {
        var result = board.contentDeepHashCode()
        result = 31 * result + availableBlocks.hashCode()
        result = 31 * result + score
        result = 31 * result + highScore
        result = 31 * result + coins
        result = 31 * result + freeRotations
        result = 31 * result + (lastRotatedBlockId ?: 0)
        result = 31 * result + isGameOver.hashCode()
        result = 31 * result + isLastChance.hashCode()
        result = 31 * result + (selectedBlock?.hashCode() ?: 0)
        result = 31 * result + clearingCells.hashCode()
        result = 31 * result + showHighScoreAnim.hashCode()
        result = 31 * result + rainbowBlockCount
        result = 31 * result + colorWipeCount // <-- ADDED
        result = 31 * result + specialMeterValue
        result = 31 * result + isSoundEnabled.hashCode()
        result = 31 * result + isMusicEnabled.hashCode()
        result = 31 * result + showZeroCoinsDialog.hashCode() // <-- ADDED
        result = 31 * result + showTierPromotionDialog.hashCode() // <-- ADDED
        result = 31 * result + (newlyUnlockedTier?.hashCode() ?: 0) // <-- ADDED
        return result
    }
}

data class ClearResult(val newBoard: GameGrid, val totalClears: Int)

// ---------------------------
// 2. Block Definitions
// ---------------------------

// The Special Rainbow Block (1x9 Line)
val RAINBOW_BLOCK = Block(
    id = 999,
    name = "Rainbow Wipe",
    colorResId = BLOCK_DRAWABLES[7], // Rainbow texture
    shape = listOf(
        Coord(0,0), Coord(0,1), Coord(0,2), Coord(0,3), Coord(0,4),
        Coord(0,5), Coord(0,6), Coord(0,7), Coord(0,8)
    ),
    isSpecial = true
)

val BLOCK_MANAGER: List<Block> = listOf(
    // 1-Cell
    Block(1, "1x1 Dot", BLOCK_DRAWABLES[5], listOf(Coord(0, 0))),
    // 2-Cell
    Block(2, "1x2 Bar", BLOCK_DRAWABLES[0], listOf(Coord(0, 0), Coord(0, 1))),
    // 3-Cell
    Block(3, "1x3 Bar", BLOCK_DRAWABLES[1], listOf(Coord(0, 0), Coord(0, 1), Coord(0, 2))),
    Block(4, "L Triomino", BLOCK_DRAWABLES[6], listOf(Coord(0, 0), Coord(1, 0), Coord(1, 1))),
    // 4-Cell
    Block(5, "1x4 Bar", BLOCK_DRAWABLES[2], listOf(Coord(0, 0), Coord(0, 1), Coord(0, 2), Coord(0, 3))),
    Block(6, "Square", BLOCK_DRAWABLES[4], listOf(Coord(0, 0), Coord(0, 1), Coord(1, 0), Coord(1, 1))),
    Block(7, "L Tetromino", BLOCK_DRAWABLES[3], listOf(Coord(0, 0), Coord(1, 0), Coord(2, 0), Coord(2, 1))),
    Block(8, "J Tetromino", BLOCK_DRAWABLES[4], listOf(Coord(0, 1), Coord(1, 1), Coord(2, 1), Coord(2, 0))),
    Block(9, "T Tetromino", BLOCK_DRAWABLES[5], listOf(Coord(0, 0), Coord(0, 1), Coord(0, 2), Coord(1, 1))),
    Block(10, "S Tetromino", BLOCK_DRAWABLES[1], listOf(Coord(0, 1), Coord(0, 2), Coord(1, 0), Coord(1, 1))),
    Block(11, "Z Tetromino", BLOCK_DRAWABLES[6], listOf(Coord(0, 0), Coord(0, 1), Coord(1, 1), Coord(1, 2))),
    // 5-Cell
    Block(12, "1x5 Bar", BLOCK_DRAWABLES[0], listOf(Coord(0, 0), Coord(0, 1), Coord(0, 2), Coord(0, 3), Coord(0, 4))),
    Block(13, "L Pentomino", BLOCK_DRAWABLES[3], listOf(Coord(0, 0), Coord(1, 0), Coord(2, 0), Coord(3, 0), Coord(3, 1))),
    Block(14, "Plus Pentomino", BLOCK_DRAWABLES[2], listOf(Coord(0, 1), Coord(1, 0), Coord(1, 1), Coord(1, 2), Coord(2, 1))),
    Block(15, "U Pentomino", BLOCK_DRAWABLES[4], listOf(Coord(0, 0), Coord(0, 2), Coord(1, 0), Coord(1, 1), Coord(1, 2))),
    Block(16, "Wide T", BLOCK_DRAWABLES[5], listOf(Coord(0, 0), Coord(0, 1), Coord(0, 2), Coord(1, 1), Coord(2, 1))),
    // 9-Cell
    Block(17, "3x3 Square", BLOCK_DRAWABLES[1], listOf(
        Coord(0, 0), Coord(0, 1), Coord(0, 2),
        Coord(1, 0), Coord(1, 1), Coord(1, 2),
        Coord(2, 0), Coord(2, 1), Coord(2, 2)
    )),
    // Misc
    Block(18, "Small L", BLOCK_DRAWABLES[6], listOf(Coord(0, 0), Coord(1, 0))),
    Block(19, "Small T", BLOCK_DRAWABLES[0], listOf(Coord(0, 0), Coord(0, 1), Coord(0, 2), Coord(1, 1)))
)
fun getBlockById(id: Int): Block? {
    return BLOCK_MANAGER.firstOrNull { it.id == id }
}