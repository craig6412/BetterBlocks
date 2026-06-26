package com.betterblocks.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DailyChallengeTest {

    @Test
    fun sameDay_yieldsSameGeneratorSequence() {
        fun ids(epochDay: Long) =
            DailyChallenge.generatorForDay(epochDay).next(3).map { it.id }
        assertEquals(ids(20000), ids(20000))
        assertNotEquals(ids(20000), ids(20001))
    }

    @Test
    fun sharedSeedReproducesSequence() {
        val seed = 998877L
        val a = DailyChallenge.generatorForSeed(seed).next(6).map { it.id }
        val b = DailyChallenge.generatorForSeed(seed).next(6).map { it.id }
        assertEquals(a, b)
    }

    @Test
    fun encodeDecode_roundTrips() {
        for (seed in listOf(0L, 1L, -1L, 123456789L, Long.MAX_VALUE, Long.MIN_VALUE)) {
            val code = DailyChallenge.encode(seed)
            assertTrue("code is prefixed", code.startsWith("BB-"))
            assertEquals("round-trip for $seed", seed, DailyChallenge.decode(code))
        }
    }

    @Test
    fun decode_isCaseInsensitiveAndToleratesWhitespace() {
        val code = DailyChallenge.encode(424242L)
        assertEquals(424242L, DailyChallenge.decode("  ${code.lowercase()}  "))
    }

    @Test
    fun decode_malformedReturnsNull() {
        assertNull(DailyChallenge.decode("BB-!!!notbase36!!!"))
    }
}
