package com.betterblocks

import org.json.JSONArray

object BoardSerializer {

    fun toJson(board: Array<Array<Int?>>): String {
        val outer = JSONArray()
        for (r in board) {
            val rowArray = JSONArray()
            for (cell in r) {
                rowArray.put(cell)
            }
            outer.put(rowArray)
        }
        return outer.toString()
    }

    fun fromJson(json: String): Array<Array<Int?>> {
        val arr = JSONArray(json)
        val result = Array(GRID_SIZE) { Array<Int?>(GRID_SIZE) { null } }
        for (r in 0 until GRID_SIZE) {
            val row = arr.getJSONArray(r)
            for (c in 0 until GRID_SIZE) {
                result[r][c] = if (row.isNull(c)) null else row.getInt(c)
            }
        }
        return result
    }
}
