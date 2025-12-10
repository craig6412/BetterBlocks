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
        // Wait for initial touch
        val down = awaitFirstDown(requireUnconsumed = false)
        val downPosition = down.position

        Log.d("SimpleGesture", "Touch down at: $downPosition")

        var totalDrag = Offset.Zero
        var hasDragged = false

        // Start tracking immediately
        onDragStart(downPosition)

        // Track all pointer movements
        val success = drag(down.id) { change ->
            val delta = change.positionChange()
            totalDrag += delta

            // Consider it a drag if moved more than 5 pixels
            if (!hasDragged && (abs(totalDrag.x) > 5f || abs(totalDrag.y) > 5f)) {
                hasDragged = true
                Log.d("SimpleGesture", "Started dragging")
            }

            if (hasDragged) {
                onDrag(change.position)
                change.consume()
            }
        }

        if (hasDragged) {
            Log.d("SimpleGesture", "Drag ended, success=$success")
            onDragEnd()
        } else {
            Log.d("SimpleGesture", "Detected as tap")
            onDragEnd() // Clean up drag state
            onTap()
        }
    }
}