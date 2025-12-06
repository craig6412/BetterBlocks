package com.betterblocks

/**
 * Rotates the shape of a block based on a rotation value.
 *
 * @param block The block to rotate.
 * @param rotation The number of 90-degree clockwise rotations (0-3).
 * @return A new Block instance with the rotated shape.
 */
fun getRotatedBlock(block: Block, rotation: Int): Block {
    if (rotation == 0) return block

    val rotatedShape = block.shape.map { coord ->
        // Normalize rotation to 0-3 range
        when (((rotation % 4) + 4) % 4) {
            0 -> coord // No rotation
            1 -> Coord(coord.col, -coord.row) // 90 degrees
            2 -> Coord(-coord.row, -coord.col) // 180 degrees
            3 -> Coord(-coord.col, coord.row) // 270 degrees
            else -> coord
        }
    }
    return block.copy(shape = rotatedShape)
}
