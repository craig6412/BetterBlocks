package com.betterblocks.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The seeded generator is the foundation for Daily Challenge and Gauntlet:
 * fairness depends on two players with the same seed getting an identical
 * sequence. These tests pin that contract.
 */
class SeededBlockGeneratorTest {

    private fun streamIds(seed: Long, draws: Int = 20): List<String> {
        val gen = SeededBlockGenerator(seed)
        return (0 until draws).flatMap { gen.next(3) }.map { it.id }
    }

    @Test
    fun sameSeed_producesIdenticalSequence() {
        assertEquals(streamIds(12345L), streamIds(12345L))
    }

    @Test
    fun differentSeeds_produceDifferentSequences() {
        assertNotEquals(streamIds(1L), streamIds(2L))
    }

    @Test
    fun next_returnsRequestedCount_fromKnownShapes() {
        val drawn = SeededBlockGenerator(99L).next(3)
        assertEquals(3, drawn.size)
        assertTrue(drawn.all { EngineShapes.byId(it.id) != null })
    }

    @Test
    fun seedForDay_isStablePerDayAndDiffersAcrossDays() {
        assertEquals(SeededBlockGenerator.seedForDay(20000), SeededBlockGenerator.seedForDay(20000))
        assertNotEquals(SeededBlockGenerator.seedForDay(20000), SeededBlockGenerator.seedForDay(20001))
    }
}
