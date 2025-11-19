package com.betterblocks


import android.graphics.Color
import kotlin.random.Random

/**
 * Data class representing a unique block shape.
 *
 * @property name A descriptive name for the block (e.g., "3x3 Square").
 * @property shape The list of (row, col) coordinates defining the block's cells,
 * relative to a (0,0) top-left anchor.
 * @property numCells The total number of cells in the block.
 */
data class BlockShape(
    val id: String,
    val name: String,
    val shape: List<Pair<Int, Int>>,
    val numCells: Int = shape.size
)

/**
 * Singleton object responsible for managing all predefined block shapes.
 * This holds the complete inventory of 19 blocks required for a full game.
 */
object BlockManager {

    // --- Private list of all 19 standard block shapes ---
    private val ALL_BLOCK_SHAPES = listOf(
        // 1. Single Cell
        BlockShape("O_1x1", "1x1 Dot", listOf(Pair(0, 0))),

        // 2. Two-Cell Shapes (Lines)
        BlockShape("I_1x2", "1x2 Horizontal Line", listOf(Pair(0, 0), Pair(0, 1))),
        BlockShape("I_2x1", "2x1 Vertical Line", listOf(Pair(0, 0), Pair(1, 0))),

        // 3. Three-Cell Shapes
        BlockShape("I_1x3", "1x3 Horizontal Line", listOf(Pair(0, 0), Pair(0, 1), Pair(0, 2))),
        BlockShape("I_3x1", "3x1 Vertical Line", listOf(Pair(0, 0), Pair(1, 0), Pair(2, 0))),
        // L-shape (3-cell corner)
        BlockShape("L_3", "3-Cell L-Shape", listOf(Pair(0, 0), Pair(1, 0), Pair(1, 1))),
        // T-shape (3-cell T)
        BlockShape("T_3", "3-Cell T-Shape", listOf(Pair(0, 1), Pair(1, 0), Pair(1, 1))),

        // 4. Four-Cell Shapes (Tetrominoes and 2x2 Square)
        BlockShape("O_2x2", "2x2 Square", listOf(
            Pair(0, 0), Pair(0, 1), Pair(1, 0), Pair(1, 1)
        )),
        BlockShape("I_1x4", "1x4 Horizontal Line", listOf(
            Pair(0, 0), Pair(0, 1), Pair(0, 2), Pair(0, 3)
        )),
        BlockShape("I_4x1", "4x1 Vertical Line", listOf(
            Pair(0, 0), Pair(1, 0), Pair(2, 0), Pair(3, 0)
        )),
        // L-Tetromino
        BlockShape("L_4", "L-Tetromino", listOf(
            Pair(0, 0), Pair(1, 0), Pair(2, 0), Pair(2, 1)
        )),
        // J-Tetromino (Reverse L)
        BlockShape("J_4", "J-Tetromino", listOf(
            Pair(0, 1), Pair(1, 1), Pair(2, 1), Pair(2, 0)
        )),
        // T-Tetromino (Cross shape)
        BlockShape("T_4", "T-Tetromino", listOf(
            Pair(0, 1), Pair(1, 0), Pair(1, 1), Pair(1, 2)
        )),
        // S-Tetromino (Zig-Zag)
        BlockShape("S_4", "S-Tetromino", listOf(
            Pair(0, 1), Pair(0, 2), Pair(1, 0), Pair(1, 1)
        )),
        // Z-Tetromino (Reverse Zig-Zag)
        BlockShape("Z_4", "Z-Tetromino", listOf(
            Pair(0, 0), Pair(0, 1), Pair(1, 1), Pair(1, 2)
        )),

        // 5. Five-Cell Shapes (Lines)
        BlockShape("I_1x5", "1x5 Horizontal Line", listOf(
            Pair(0, 0), Pair(0, 1), Pair(0, 2), Pair(0, 3), Pair(0, 4)
        )),
        BlockShape("I_5x1", "5x1 Vertical Line", listOf(
            Pair(0, 0), Pair(1, 0), Pair(2, 0), Pair(3, 0), Pair(4, 0)
        )),

        // 6. Rectangles and Large Squares
        BlockShape("R_2x3", "2x3 Rectangle", listOf(
            Pair(0, 0), Pair(0, 1), Pair(0, 2), Pair(1, 0), Pair(1, 1), Pair(1, 2)
        )),
        BlockShape("O_3x3", "3x3 Square (Waffle-9)", listOf(
            Pair(0, 0), Pair(0, 1), Pair(0, 2),
            Pair(1, 0), Pair(1, 1), Pair(1, 2),
            Pair(2, 0), Pair(2, 1), Pair(2, 2)
        ))
    )

    /**
     * @return The complete, unmodifiable list of all 19 predefined BlockShapes.
     */
    fun getAllShapes(): List<BlockShape> = ALL_BLOCK_SHAPES

    /**
     * Randomly selects 3 unique block shapes from the complete inventory
     * to be used for the player's next available moves.
     * @return A list of 3 random BlockShape instances.
     */
    fun getRandomInventory(): List<BlockShape> {
        return ALL_BLOCK_SHAPES.shuffled(Random).take(3)
    }

    /**
     * A utility function to map the resource IDs we defined in the XML to the
     * actual block shapes for initialization.
     * This is temporary and will be replaced by the random generation logic later.
     */
    fun getShapeById(id: String): BlockShape? {
        return ALL_BLOCK_SHAPES.find { it.id == id }
    }
}