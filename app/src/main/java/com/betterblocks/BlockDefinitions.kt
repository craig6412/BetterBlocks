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
        )),

        // NEW: 2-high Staircase (only allowed when board has space)
        BlockShape("ST_2", "2-High Staircase", listOf(
            Pair(0, 0),
            Pair(1, 1)

        )),

        BlockShape(
            "U_5",
            "U-Shape",
            listOf(
                Pair(0, 0), Pair(0, 2),
                Pair(1, 0), Pair(1, 1), Pair(1, 2)

            )),

        BlockShape("J_3", "3-Cell Reverse L", listOf(
            Pair(0, 1), Pair(1, 1), Pair(1, 0)
        ))


    )

    /**
     * @return The complete, unmodifiable list of all predefined BlockShapes.
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
     * Rotate a shape 90 degrees clockwise around origin (normalize to min row/col = 0)
     */
    private fun rotateShape90(shape: List<Pair<Int, Int>>): List<Pair<Int, Int>> {
        val rotated = shape.map { (r, c) -> Pair(c, -r) }
        val minRow = rotated.minOf { it.first }
        val minCol = rotated.minOf { it.second }
        return rotated.map { Pair(it.first - minRow, it.second - minCol) }
    }

    /**
     * Produce up to 4 unique rotation variants for a given shape (0/90/180/270). Normalized.
     */
    private fun allRotations(shape: List<Pair<Int, Int>>): List<List<Pair<Int, Int>>> {
        val outs = mutableListOf<List<Pair<Int, Int>>>()
        var current = shape.map { it } // copy
        repeat(4) {
            // normalize current
            val minRow = current.minOf { it.first }
            val minCol = current.minOf { it.second }
            val norm = current.map { Pair(it.first - minRow, it.second - minCol) }
            if (!outs.any { a -> a.toSet() == norm.toSet() }) outs.add(norm)
            current = rotateShape90(current)
        }
        return outs
    }

    /**
     * Returns true if `shape` can be placed somewhere on `board` (board: Array<Array<Int?>>)
     * i.e., there exists a translation + rotation where every cell is within bounds and null.
     */
    fun isShapePlaceableOnBoard(shape: BlockShape, board: Array<Array<Int?>>): Boolean {
        val rows = board.size
        val cols = if (rows > 0) board[0].size else 0
        val rotations = allRotations(shape.shape)
        for (variant in rotations) {
            // compute bounding sizes
            val height = (variant.maxOfOrNull { it.first } ?: 0) + 1
            val width = (variant.maxOfOrNull { it.second } ?: 0) + 1
            for (r in 0..(rows - height)) {
                for (c in 0..(cols - width)) {
                    var fits = true
                    for ((sr, sc) in variant) {
                        val br = r + sr
                        val bc = c + sc
                        if (br !in 0 until rows || bc !in 0 until cols) { fits = false; break }
                        if (board[br][bc] != null) { fits = false; break }
                    }
                    if (fits) return true
                }
            }
        }
        return false
    }

    /**
     * SMART INVENTORY GENERATOR
     *
     * Goals (see design notes):
     * - Fair: avoid multiple large/awkward blocks and guarantee an obviously-placeable small block.
     * - Forgiving early: bias toward small shapes in early game or when board is tight.
     * - Gradual challenge: allow medium/large shapes more often as player progresses.
     *
     * Inputs:
     * - board: current board grid (Array<Array<Int?>>) where null=empty
     * - score / linesCleared: soft-gating metrics for large shapes
     * - maxAttempts: how many times to try generating a valid inventory before falling back
     *
     * Returns: exactly 3 BlockShape instances.
     */
    fun getSmartInventory(
        board: Array<Array<Int?>>,
        score: Int = 0,
        linesCleared: Int = 0,
        maxAttempts: Int = 8
    ): List<BlockShape> {
        // --- Tunables (can be adjusted for live balance) ---
        val LARGE_SCORE_THRESHOLD = 20_000        // soft gate for large blocks
        val LARGE_LINES_THRESHOLD = 40           // alt soft gate
        val DENSE_BOARD_THRESHOLD = 0.28f        // if emptiness < this -> board is dense (favor small)
        val OPEN_BOARD_THRESHOLD = 0.50f         // emptiness > this -> board is open (allow medium/large)

        // --- Precompute helpers ---
        fun emptinessFraction(): Float {
            val rows = board.size
            if (rows == 0) return 1f
            val cols = board[0].size
            var empty = 0
            for (r in 0 until rows) for (c in 0 until cols) if (board[r][c] == null) empty++
            return empty.toFloat() / (rows * cols).toFloat()
        }

        val emptiness = emptinessFraction()

        // Partition shapes by size
        val smallShapes = ALL_BLOCK_SHAPES.filter { it.numCells <= 4 }
        val mediumShapes = ALL_BLOCK_SHAPES.filter { it.numCells in 5..6 }
        val largeShapes = ALL_BLOCK_SHAPES.filter { it.numCells >= 7 }

        // Define "guaranteed recovery" candidates (explicit small forgiving shapes)
        val guaranteedIds = setOf("O_1x1", "I_1x2", "O_2x2", "I_1x3")
        val guaranteedPool = ALL_BLOCK_SHAPES.filter { guaranteedIds.contains(it.id) }

        // Define awkward set (limit to 1 per inventory). We treat large shapes as awkward and include ST_2 if present
        val awkwardIds = setOf("ST_2")
        val awkwardPool = ALL_BLOCK_SHAPES.filter { it.id in awkwardIds } + largeShapes

        val rnd = Random

        // Weighted selection function that tries to produce a placeable shape from the provided lists
        fun pickPlaceableFrom(list: List<BlockShape>): BlockShape? {
            if (list.isEmpty()) return null
            val shuffled = list.shuffled(rnd)
            for (candidate in shuffled) {
                if (isShapePlaceableOnBoard(candidate, board)) return candidate
            }
            return null
        }

        // Weighted sampler for category selection with dynamic biases
        fun sampleCategory(pSmall: Float, pMedium: Float, pLarge: Float): List<Int> {
            // returns a list of 2 category choices encoded as 0=small,1=medium,2=large
            val cats = mutableListOf<Int>()
            repeat(2) {
                val x = rnd.nextFloat()
                when {
                    x < pSmall -> cats.add(0)
                    x < pSmall + pMedium -> cats.add(1)
                    else -> cats.add(2)
                }
            }
            return cats
        }

        // Main generation loop with retries for safety
        repeat(maxAttempts) {
            val picked = mutableListOf<BlockShape>()
            var awkwardCount = 0
            var largeCount = 0

            // 1) Pick guaranteed recovery block: must be placeable
            val placeableGuaranteed = guaranteedPool.shuffled(rnd).firstOrNull { isShapePlaceableOnBoard(it, board) }
            if (placeableGuaranteed == null) {
                // If none of the guaranteed recovery shapes are placeable, try a more general small placeable
                val fallbackSmall = smallShapes.shuffled(rnd).firstOrNull { isShapePlaceableOnBoard(it, board) }
                if (fallbackSmall == null) return@repeat // try again
                picked.add(fallbackSmall)
            } else {
                picked.add(placeableGuaranteed)
            }

            // 2) Decide dynamic weights for rest of the inventory
            // Base weights (target composition ~ 65% small, 27.5% medium, 7.5% large)
            var pSmall = 0.65f
            var pMedium = 0.275f
            var pLarge = 0.075f

            // If the board is dense (few empty cells), bias heavily toward small
            if (emptiness < DENSE_BOARD_THRESHOLD) {
                pSmall = 0.82f
                pMedium = 0.17f
                pLarge = 0.03f
            } else if (emptiness > OPEN_BOARD_THRESHOLD) {
                // If board is open, allow slightly more medium/large
                pSmall = 0.60f
                pMedium = 0.32f
                pLarge = 0.08f
            }

            // Soft-gate large blocks if player is early in progression
            if (score < LARGE_SCORE_THRESHOLD || linesCleared < LARGE_LINES_THRESHOLD || emptiness < 0.40f) {
                // reduce large probability to very low but don't hard ban entirely
                pLarge = (pLarge * 0.25f).coerceAtLeast(0.02f)
                // normalize remaining probability into small/medium
                val remaining = 1f - pLarge
                val mid = pMedium / (pSmall + pMedium)
                pSmall = remaining * (1f - mid)
                pMedium = remaining * mid
            }

            // Normalize just in case
            val total = pSmall + pMedium + pLarge
            pSmall /= total; pMedium /= total; pLarge /= total

            // 3) Sample categories for remaining two slots
            val categories = sampleCategory(pSmall, pMedium, pLarge)

            for (cat in categories) {
                var candidate: BlockShape? = null
                when (cat) {
                    0 -> candidate = pickPlaceableFrom(smallShapes)
                    1 -> candidate = pickPlaceableFrom(mediumShapes)
                    2 -> candidate = pickPlaceableFrom(largeShapes)
                }

                // If preferred category produced nothing placeable, fallback to small
                if (candidate == null) candidate = pickPlaceableFrom(smallShapes)
                if (candidate == null) candidate = pickPlaceableFrom(mediumShapes)
                if (candidate == null) candidate = pickPlaceableFrom(largeShapes)

                // If still null then this attempt fails — break and retry
                if (candidate == null) return@repeat

                // Enforce rules: no more than one awkward/large
                if (candidate in awkwardPool || candidate.numCells >= 7) {
                    if (awkwardCount + 1 > 1 || largeCount + 1 > 1) {
                        // try to pick a small instead
                        val alt = pickPlaceableFrom(smallShapes)
                        if (alt != null) {
                            picked.add(alt)
                        } else {
                            // cannot satisfy constraint this attempt
                            return@repeat
                        }
                    } else {
                        picked.add(candidate)
                        if (candidate.numCells >= 7) largeCount++
                        if (candidate in awkwardPool) awkwardCount++
                    }
                } else {
                    picked.add(candidate)
                }
            }

            // 4) Final sanity: ensure we have 3 unique placeable blocks
            if (picked.size != 3) return@repeat
            // ensure uniqueness by id; if duplicates, try replace duplicates with other smalls
            if (picked.map { it.id }.toSet().size != 3) {
                val unique = mutableListOf<BlockShape>()
                for (p in picked) {
                    if (unique.none { it.id == p.id }) unique.add(p)
                }
                // fill missing slots with small placeable shapes
                while (unique.size < 3) {
                    val add = pickPlaceableFrom(smallShapes) ?: break
                    if (unique.none { it.id == add.id }) unique.add(add) else break
                }
                if (unique.size == 3) return unique
                return@repeat
            }

            // All checks passed — return the picked inventory
            return picked
        }

        // --- Fallback: if all attempts failed, return safest deterministic set ---
        val safe = mutableListOf<BlockShape>()
        val anySmall = smallShapes.firstOrNull { isShapePlaceableOnBoard(it, board) }
        if (anySmall != null) safe.add(anySmall)
        val anyLine = ALL_BLOCK_SHAPES.firstOrNull { (it.id.startsWith("I_") || it.id.contains("1x2")) && isShapePlaceableOnBoard(it, board) }
        if (anyLine != null) safe.add(anyLine)
        // fill remaining with any placeable smalls
        val moreSmall = smallShapes.filter { isShapePlaceableOnBoard(it, board) }.filter { it !in safe }
        for (s in moreSmall) {
            if (safe.size >= 3) break
            safe.add(s)
        }
        // If still <3, relax placeability and just pick distinct small shapes
        if (safe.size < 3) {
            for (s in smallShapes) {
                if (safe.size >= 3) break
                if (safe.none { it.id == s.id }) safe.add(s)
            }
        }

        return safe.take(3)
    }

    /**
     * Convert authored BlockShape entries into runtime `com.betterblocks.Block` instances.
     * This provides the bridge between the authored shapes and the runtime Block objects
     * used by the rest of the game. The function name and signature match how `GameModel`
     * constructs `BLOCK_MANAGER` so the unresolved reference is resolved.
     */
    fun toGameBlocks(startingId: Int = 1): List<com.betterblocks.Block> {
        val out = mutableListOf<com.betterblocks.Block>()
        var id = startingId

        // Use COLOR_WIPE_DRAWABLES (exactly 7 playable colors) as the canonical color pool
        val colorPool = try {
            COLOR_WIPE_DRAWABLES
        } catch (_: Throwable) {
            // Fallback if constant not available for any reason
            BLOCK_DRAWABLES.subList(0, 7)
        }

        for ((index, shape) in ALL_BLOCK_SHAPES.withIndex()) {
            val coords = shape.shape.map { Pair -> com.betterblocks.Coord(Pair.first, Pair.second) }
            val color = colorPool[index % colorPool.size]
            out += com.betterblocks.Block(
                id = id++,
                name = shape.name,
                colorResId = color,
                shape = coords,
                isSpecial = false
            )
        }

        return out
    }

    // Make existing board-aware helper delegate to the canonical generator so call sites converge.
    fun getRandomInventoryForBoard(board: Array<Array<Int?>>): List<BlockShape> {
        // We forward to the smarter generator with defaults; preserves API but centralizes logic.
        return getSmartInventory(board = board)
    }
}
