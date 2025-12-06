package com.betterblocks.animation.blockblast

import androidx.compose.animation.core.Easing
import com.betterblocks.Coord
import kotlin.math.max
import kotlin.math.min

class SweepAnimator {
    private val sweeps = mutableListOf<SweepState>()
    var lastSweepEndPosition: Float = 0f
        private set

    fun startSweep(index: Int, isRow: Boolean, gridSize: Int, duration: Float, easing: Easing) {
        sweeps += SweepState(index, isRow, gridSize, duration, easing)
    }

    fun update(deltaTimeMs: Float) {
        val iterator = sweeps.iterator()
        while (iterator.hasNext()) {
            val sweep = iterator.next()
            sweep.update(deltaTimeMs)
            if (sweep.isFinished) {
                lastSweepEndPosition = sweep.currentPosition
                iterator.remove()
            }
        }
    }

    fun getImpactedTiles(): List<Coord> {
        return sweeps.flatMap { it.consumeImpacts() }
    }

    fun forEachSweep(action: (index: Int, isRow: Boolean, position: Float, alpha: Float) -> Unit) {
        sweeps.forEach { sweep ->
            action(sweep.index, sweep.isRow, sweep.progress, sweep.alpha)
        }
    }

    private class SweepState(
        val index: Int,
        val isRow: Boolean,
        val gridSize: Int,
        val durationMs: Float,
        val easing: Easing
    ) {
        private var elapsedMs = 0f
        private var lastProgress = 0f
        val impacts = mutableSetOf<Coord>()
        var progress = 0f
            private set
        val alpha: Float
            get() = 1f
        val currentPosition: Float
            get() = progress * gridSize
        val isFinished: Boolean
            get() = elapsedMs >= durationMs

        fun update(deltaTimeMs: Float) {
            if (isFinished) return
            elapsedMs += deltaTimeMs
            val t = (elapsedMs / durationMs).coerceIn(0f, 1f)
            progress = easing.transform(t)
            collectImpacts(lastProgress, progress)
            lastProgress = progress
        }

        private fun collectImpacts(last: Float, current: Float) {
            val start = min(last, current)
            val end = max(last, current)
            val startCell = (start * gridSize).toInt()
            val endCell = (end * gridSize).toInt()
            for (i in startCell..endCell) {
                val coord = if (isRow) Coord(index, i) else Coord(i, index)
                if (i in 0 until gridSize) {
                    impacts += coord
                }
            }
        }

        fun consumeImpacts(): List<Coord> {
            if (impacts.isEmpty()) return emptyList()
            val list = impacts.toList()
            impacts.clear()
            return list
        }
    }
}

