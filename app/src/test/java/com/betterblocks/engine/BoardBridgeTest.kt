package com.betterblocks.engine

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BoardBridgeTest {

    @Test
    fun legacyToEngine_mapsColorsAndNulls() {
        val legacy = Array(ENGINE_GRID_SIZE) { arrayOfNulls<Int>(ENGINE_GRID_SIZE) }
        legacy[0][0] = 5
        legacy[3][4] = 9
        val grid = BoardBridge.toEngine(legacy)
        assertEquals(5, grid[0][0]!!.colorId)
        assertEquals(0, grid[0][0]!!.age)
        assertEquals(9, grid[3][4]!!.colorId)
        assertNull(grid[0][1])
    }

    @Test
    fun roundTrip_preservesColors() {
        val legacy = Array(ENGINE_GRID_SIZE) { r -> Array<Int?>(ENGINE_GRID_SIZE) { c -> if ((r + c) % 2 == 0) r * 10 + c else null } }
        val back = BoardBridge.toLegacy(BoardBridge.toEngine(legacy))
        for (r in 0 until ENGINE_GRID_SIZE) {
            assertArrayEquals("row $r", legacy[r], back[r])
        }
    }

    @Test
    fun engineToLegacy_dropsAgeButKeepsColor() {
        val grid = List(ENGINE_GRID_SIZE) { r ->
            List<EngineCell?>(ENGINE_GRID_SIZE) { c ->
                if (r == 1 && c == 1) EngineCell(colorId = 42, age = 8, crystallized = true) else null
            }
        }
        val legacy = BoardBridge.toLegacy(grid)
        assertEquals(42, legacy[1][1])
        assertNull(legacy[0][0])
    }
}
