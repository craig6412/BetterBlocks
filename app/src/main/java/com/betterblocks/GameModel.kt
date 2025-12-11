package com.betterblocks


import com.betterblocks.model.ScorePopupState
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
    val rotation: Int = 0, // <-- ADDED: Current rotation for the selected block
    val clearingCells: Set<Coord> = emptySet(),
    val effectCells: Set<Coord> = emptySet(), // Effect layer for animations
    val isRainbowWipeActive: Boolean = false,
    val isColorWipeAnimating: Boolean = false, // Flag to slow down color wipe animation
    val showHighScoreAnim: Boolean = false,
    val rainbowBlockCount: Int = 2,
    val colorWipeCount: Int = 5,  // <-- ADDED: Color Wipe Inventory (match DEV_INITIAL_COLOR_WIPE)
    val showFirstInstallFreeCoinsDialog: Boolean = false,
    val specialMeterValue: Int = 0,
    val isSoundEnabled: Boolean = true,
    val isMusicEnabled: Boolean = true,
    val showZeroCoinsDialog: Boolean = false, // <-- ADDED: Zero Coins Dialog Fla
    val trophyTier: TrophyTier = TrophyTier.UNRANKED,
    val showTierPromotionDialog: Boolean = false,
    val newlyUnlockedTier: TrophyTier? = null,
    // NEW UI STATE FIELDS
    val showRainbowEarnedDialog: Boolean = false,
    val showFirstGameOverDialog: Boolean = false,
    val showPurchaseSuccessDialog: Boolean = false,
    val purchaseCoinsAwarded: Int = 0,
    val coinsEarnedThisUpdate: Int = 0,
    val showShopPurchaseBubble: Boolean = false,
    val shopPurchaseMessage: String = "",
    // DAILY REWARD SYSTEM
    val showDailyRewardDialog: Boolean = false,
    val dailyRewardDay: Int = 0,
    val dailyRewardStreak: Int = 0,
    val dailyRewardCoins: Int = 0,
    val dailyRewardRainbow: Boolean = false,
    // GAME OVER SUMMARY
    val showGameSummaryDialog: Boolean = false,
    val linesClearedThisGame: Int = 0,
    val coinsEarnedThisGame: Int = 0,
    val scoreState: ScorePopupState = ScorePopupState(),

    // NEW FIELDS (needed for pre-clear tint)
    val previewClearIndices: List<Int> = emptyList(),  // which rows/cols will clear
    val previewIsRow: Boolean = true,                   // true=row, false=column
    val moveNumber: Int = 0,                            // increments per block placement, used for tint cycling

    // NEW: selection/rotation session tracking (runtime only)
    val selectionToken: Long = 0L,                    // increments when a block is newly selected
    val rotationPaidSelectionToken: Long = 0L         // token for which rotation payment/free-rotation was consumed
 )

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
   // Block(7, "L Tetromino", BLOCK_DRAWABLES[3], listOf(Coord(0, 0), Coord(1, 0), Coord(2, 0), Coord(2, 1))),
   // Block(8, "J Tetromino", BLOCK_DRAWABLES[4], listOf(Coord(0, 1), Coord(1, 1), Coord(2, 1), Coord(2, 0))),
    //Block(9, "T Tetromino", BLOCK_DRAWABLES[5], listOf(Coord(0, 0), Coord(0, 1), Coord(0, 2), Coord(1, 1))),
    //Block(10, "S Tetromino", BLOCK_DRAWABLES[1], listOf(Coord(0, 1), Coord(0, 2), Coord(1, 0), Coord(1, 1))),
    //Block(11, "Z Tetromino", BLOCK_DRAWABLES[6], listOf(Coord(0, 0), Coord(0, 1), Coord(1, 1), Coord(1, 2))),
    // 5-Cell
    Block(12, "1x5 Bar", BLOCK_DRAWABLES[0], listOf(Coord(0, 0), Coord(0, 1), Coord(0, 2), Coord(0, 3), Coord(0, 4))),
   // Block(13, "L Pentomino", BLOCK_DRAWABLES[3], listOf(Coord(0, 0), Coord(1, 0), Coord(2, 0), Coord(3, 0), Coord(3, 1))),
    Block(14, "Plus Pentomino", BLOCK_DRAWABLES[2], listOf(Coord(0, 1), Coord(1, 0), Coord(1, 1), Coord(1, 2), Coord(2, 1))),
    Block(15, "U Pentomino", BLOCK_DRAWABLES[4], listOf(Coord(0, 0), Coord(0, 2), Coord(1, 0), Coord(1, 1), Coord(1, 2))),
    //Block(16, "Wide T", BLOCK_DRAWABLES[5], listOf(Coord(0, 0), Coord(0, 1), Coord(0, 2), Coord(1, 1), Coord(2, 1))),
    // 9-Cell
    Block(17, "3x3 Square", BLOCK_DRAWABLES[1], listOf(
        Coord(0, 0), Coord(0, 1), Coord(0, 2),
        Coord(1, 0), Coord(1, 1), Coord(1, 2),
        Coord(2, 0), Coord(2, 1), Coord(2, 2)
    )),
    // Misc
    Block(18, "Small L", BLOCK_DRAWABLES[6], listOf(Coord(0, 0), Coord(1, 0))),

    Block(19, "Small T", BLOCK_DRAWABLES[0], listOf(Coord(0, 0), Coord(0, 1), Coord(0, 2), Coord(1, 1))),

    //new blocks we are addding to mimick most style games like this.

    Block(
        21, "3x2 Rectangle", BLOCK_DRAWABLES[3],
        listOf(
            Coord(0,0), Coord(0,1),
            Coord(1,0), Coord(1,1),
            Coord(2,0), Coord(2,1)
        )
    ),
    Block(
        22, "2x4 Rectangle", BLOCK_DRAWABLES[1],
        listOf(
            Coord(0,0), Coord(0,1), Coord(0,2), Coord(0,3),
            Coord(1,0), Coord(1,1), Coord(1,2), Coord(1,3)
        )
    ),
            Block(
            23, "4x2 Rectangle", BLOCK_DRAWABLES[4],
    listOf(
        Coord(0,0), Coord(0,1),
        Coord(1,0), Coord(1,1),
        Coord(2,0), Coord(2,1),
        Coord(3,0), Coord(3,1)
    )
),
    Block(
        24, "2x2 Diagonal", BLOCK_DRAWABLES[6],
        listOf(
            Coord(0,0),
            Coord(1,0), Coord(1,1)
        )
    ),

    Block(
        25, "2x2 Reverse Diagonal", BLOCK_DRAWABLES[5],
        listOf(
            Coord(0,1),
            Coord(1,0), Coord(1,1)
        )
    )



)

fun getBlockById(id: Int): Block? {
    return BLOCK_MANAGER.firstOrNull { it.id == id }
}

// Centralized animation timing so LineClearAnimator and GameViewModel stay in sync
object GameAnimationDurations {
    // Base sweep duration from LineClearAnimator
    const val LINE_CLEAR_MS: Long = com.betterblocks.animation.LineClearAnimator.SWEEP_DURATION.toLong()

    // Particle fade matches LineClearAnimator as well
    const val LINE_CLEAR_PARTICLE_FADE_MS: Long = com.betterblocks.animation.LineClearAnimator.PARTICLE_FADE_DURATION.toLong()

    // Total duration for a full line-clear cycle (sweep + particles)
    val LINE_CLEAR_TOTAL_MS: Long = LINE_CLEAR_MS + LINE_CLEAR_PARTICLE_FADE_MS

    // Color wipe uses the same shape but slowed using the global multiplier
    val COLOR_WIPE_TOTAL_MS: Long = (LINE_CLEAR_TOTAL_MS * COLOR_WIPE_ANIMATION_SPEED_MULTIPLIER).toLong()

    // Full-board rainbow wipe: slightly extended for more dramatic burst
    val RAINBOW_WIPE_TOTAL_MS: Long = (LINE_CLEAR_TOTAL_MS * 1.1f).toLong()
}
