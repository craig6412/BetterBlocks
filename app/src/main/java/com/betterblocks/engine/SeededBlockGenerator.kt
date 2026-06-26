package com.betterblocks.engine

import kotlin.random.Random

/**
 * Deterministic block-sequence generator.
 *
 * This is the foundation for the Daily Challenge and (later) the Gauntlet
 * sharing system: a given [seed] always produces the exact same sequence of
 * blocks, so two players who play the same seed face an identical board and can
 * be fairly ranked. It is pure and Android-free for cheap unit testing.
 *
 * The legacy random inventory (`BlockManager.getRandomInventory`) is fine for
 * Endless mode, but it cannot be replayed or shared. Seeded mode routes through
 * here instead.
 */
class SeededBlockGenerator(
    seed: Long,
    private val shapes: List<ShapeDef> = EngineShapes.ALL
) {
    private val rng = Random(seed)

    /**
     * Draw the next [count] shapes for a round. Default of 3 matches
     * BLOCKS_PER_ROUND. Successive calls advance the deterministic stream.
     */
    fun next(count: Int = 3): List<ShapeDef> = List(count) { shapes[rng.nextInt(shapes.size)] }

    companion object {
        /**
         * Derive a stable seed for a given UTC day, so "today's challenge" is the
         * same for every player regardless of device. [epochDay] is days since
         * the Unix epoch (e.g. from `LocalDate.toEpochDay()`).
         */
        fun seedForDay(epochDay: Long): Long {
            // Mix the day with a fixed salt so consecutive days don't produce
            // trivially-correlated streams.
            var h = epochDay * 0x9E3779B97F4A7C15uL.toLong()
            h = h xor (h ushr 30)
            h *= -0x40a7b892e31b1a47L
            h = h xor (h ushr 27)
            return h
        }
    }
}
