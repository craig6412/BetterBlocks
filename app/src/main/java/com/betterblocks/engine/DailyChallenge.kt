package com.betterblocks.engine

/**
 * Pure logic for the Daily Challenge (Phase 1 supporting mode) and the seed/code
 * substrate the Phase 2 Gauntlet sharing system reuses.
 *
 * A challenge is fully described by a seed: any two players on the same seed get
 * the identical block sequence (see [SeededBlockGenerator]) and can be ranked
 * fairly. A short, human-shareable code encodes the seed for deep links.
 */
object DailyChallenge {

    /** Seed for a given UTC day (`epochDay` = days since the Unix epoch). */
    fun seedForDay(epochDay: Long): Long = SeededBlockGenerator.seedForDay(epochDay)

    /** A ready-to-play generator for the given day's challenge. */
    fun generatorForDay(epochDay: Long): SeededBlockGenerator =
        SeededBlockGenerator(seedForDay(epochDay))

    /** A generator for an arbitrary shared seed (e.g. a Gauntlet thrown by a friend). */
    fun generatorForSeed(seed: Long): SeededBlockGenerator = SeededBlockGenerator(seed)

    /**
     * Encode a seed as a short uppercase base-36 code for share links/cards,
     * e.g. "BB-3K7QZ1". Reversible via [decode].
     */
    fun encode(seed: Long): String =
        "BB-" + java.lang.Long.toUnsignedString(seed, 36).uppercase()

    /** Parse a code from [encode] back to its seed, or null if malformed. */
    fun decode(code: String): Long? {
        val body = code.trim().uppercase().removePrefix("BB-")
        return try {
            java.lang.Long.parseUnsignedLong(body, 36)
        } catch (_: NumberFormatException) {
            null
        }
    }
}
