package com.betterblocks.ui

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.positionChange
import kotlin.math.sqrt

/**
 * Simple drag-or-tap gesture detector.
 * Uses total movement distance so horizontal and diagonal drags start correctly.
 */
suspend fun PointerInputScope.detectSimpleDragOrTap(
    onDragStart: (startPos: Offset) -> Unit,
    onDrag: (currentPos: Offset) -> Unit,
    onDragEnd: () -> Unit,
    onTap: () -> Unit
) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        val downPosition = down.position

        var totalDragX = 0f
        var totalDragY = 0f
        var hasDragged = false
        val dragThresholdPx = 12f

        drag(down.id) { change ->
            val delta = change.positionChange()
            totalDragX += delta.x
            totalDragY += delta.y

            val totalDistance = sqrt(totalDragX * totalDragX + totalDragY * totalDragY)
            if (!hasDragged && totalDistance > dragThresholdPx) {
                hasDragged = true
                onDragStart(downPosition)
            }

            if (hasDragged) {
                onDrag(change.position)
                change.consume()
            }
        }

        if (hasDragged) {
            onDragEnd()
        } else {
            onTap()
        }
    }
}
