package com.betterblocks.engine

/**
 * Adapter between the legacy board representation (`Array<Array<Int?>>`, where the
 * Int is a color/drawable id and `null` is empty) and the engine's [EngineGrid].
 *
 * This is the strangler-pattern hinge: the GameViewModel can keep its current
 * board as the source of truth and call into the tested engine per placement by
 * bridging across, with no big-bang rewrite. Note that the legacy type carries
 * no per-cell age, so a round-trip through it cannot preserve Living Board state
 * — persistent crystallization requires the board to store [EngineCell] directly
 * (the planned representation change). Combo scoring and clears work fine over
 * the bridge today.
 */
object BoardBridge {

    /** Legacy color grid -> engine grid. All cells start at age 0. */
    fun toEngine(legacy: Array<Array<Int?>>): EngineGrid =
        legacy.map { row -> row.map { v -> v?.let { EngineCell(colorId = it) } } }

    /** Engine grid -> legacy color grid (drops age/crystal metadata). */
    fun toLegacy(grid: EngineGrid): Array<Array<Int?>> =
        Array(grid.size) { r ->
            Array(grid[r].size) { c -> grid[r][c]?.colorId }
        }
}
