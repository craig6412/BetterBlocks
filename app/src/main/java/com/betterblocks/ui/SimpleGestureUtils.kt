package com.betterblocks.ui

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.positionChange
import android.util.Log
import kotlin.math.abs

/**
 * Simple drag-or-tap gesture detector
 * Block Blast style: immediate drag response, no touch slop needed
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

        var totalDragY = 0f
        var hasDragged = false

        val dragSuccess = drag(down.id) { change ->
            val delta = change.positionChange()
            totalDragY += delta.y

            // ✅ Y-axis ONLY threshold (tune this value if needed)
            if (!hasDragged && abs(totalDragY) > 12f) {
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
            // Clean exit, no drag ever started
            onTap()
        }
    }
}
