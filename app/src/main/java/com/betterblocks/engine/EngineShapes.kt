package com.betterblocks.engine

/**
 * Pure, Android-free geometry for block shapes.
 *
 * This is the canonical shape table for the rules engine. It intentionally has
 * NO dependency on Android (`android.graphics.Color`, drawables, etc.) so the
 * engine and its tests can run as fast JVM unit tests. The legacy
 * [com.betterblocks.BlockManager] holds the rendering-aware shapes; over time
 * that class can delegate its geometry here (strangler pattern) so there is a
 * single source of truth.
 *
 * Coordinates are (row, col) offsets relative to a (0,0) top-left anchor, matching
 * the authored shapes in BlockDefinitions.kt.
 */
data class ShapeDef(
    val id: String,
    val cells: List<Pair<Int, Int>>
) {
    val size: Int get() = cells.size
}

object EngineShapes {

    /**
     * The full inventory of authored shapes. Order and ids mirror
     * BlockDefinitions.kt so a seeded sequence here corresponds to the same
     * shapes the renderer knows about.
     */
    val ALL: List<ShapeDef> = listOf(
        ShapeDef("O_1x1", listOf(0 to 0)),
        ShapeDef("I_1x2", listOf(0 to 0, 0 to 1)),
        ShapeDef("I_2x1", listOf(0 to 0, 1 to 0)),
        ShapeDef("I_1x3", listOf(0 to 0, 0 to 1, 0 to 2)),
        ShapeDef("I_3x1", listOf(0 to 0, 1 to 0, 2 to 0)),
        ShapeDef("L_3", listOf(0 to 0, 1 to 0, 1 to 1)),
        ShapeDef("T_3", listOf(0 to 1, 1 to 0, 1 to 1)),
        ShapeDef("O_2x2", listOf(0 to 0, 0 to 1, 1 to 0, 1 to 1)),
        ShapeDef("I_1x4", listOf(0 to 0, 0 to 1, 0 to 2, 0 to 3)),
        ShapeDef("I_4x1", listOf(0 to 0, 1 to 0, 2 to 0, 3 to 0)),
        ShapeDef("L_4", listOf(0 to 0, 1 to 0, 2 to 0, 2 to 1)),
        ShapeDef("J_4", listOf(0 to 1, 1 to 1, 2 to 1, 2 to 0)),
        ShapeDef("T_4", listOf(0 to 1, 1 to 0, 1 to 1, 1 to 2)),
        ShapeDef("S_4", listOf(0 to 1, 0 to 2, 1 to 0, 1 to 1)),
        ShapeDef("Z_4", listOf(0 to 0, 0 to 1, 1 to 1, 1 to 2)),
        ShapeDef("I_1x5", listOf(0 to 0, 0 to 1, 0 to 2, 0 to 3, 0 to 4)),
        ShapeDef("I_5x1", listOf(0 to 0, 1 to 0, 2 to 0, 3 to 0, 4 to 0)),
        ShapeDef("R_2x3", listOf(0 to 0, 0 to 1, 0 to 2, 1 to 0, 1 to 1, 1 to 2)),
        ShapeDef(
            "O_3x3",
            listOf(
                0 to 0, 0 to 1, 0 to 2,
                1 to 0, 1 to 1, 1 to 2,
                2 to 0, 2 to 1, 2 to 2
            )
        ),
        ShapeDef("ST_2", listOf(0 to 0, 1 to 1)),
        ShapeDef("U_5", listOf(0 to 0, 0 to 2, 1 to 0, 1 to 1, 1 to 2)),
        ShapeDef("J_3", listOf(0 to 1, 1 to 1, 1 to 0))
    )

    private val byId: Map<String, ShapeDef> = ALL.associateBy { it.id }

    fun byId(id: String): ShapeDef? = byId[id]
}
