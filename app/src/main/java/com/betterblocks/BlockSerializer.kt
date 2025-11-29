package com.betterblocks

import org.json.JSONArray
import org.json.JSONObject
import com.betterblocks.getBlockById
import com.betterblocks.BLOCK_MANAGER



object BlockSerializer {

    fun blocksToJson(blocks: List<Block>): String {
        val array = JSONArray()
        for (block in blocks) {
            val obj = JSONObject()
            obj.put("id", block.id)
            array.put(obj)
        }
        return array.toString()
    }

    fun blocksFromJson(json: String): List<Block> {
        val arr = JSONArray(json)
        val result = mutableListOf<Block>()
        for (i in 0 until arr.length()) {
            val id = arr.getJSONObject(i).getInt("id")
            getBlockById(id)?.let { result.add(it) }
        }
        return result
    }
}
