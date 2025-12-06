package com.betterblocks.ui

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitTouchSlopOrCancellation
import androidx.compose.foundation.gestures.drag
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputScope

suspend fun PointerInputScope.detectDragOrClick(
    onDragStart: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit,
    onDrag: (change: PointerInputChange, dragAmount: Offset) -> Unit,
    onClick: () -> Unit
) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)

        val drag = awaitTouchSlopOrCancellation(down.id) { change, _ ->
            val positionChange = change.position - change.previousPosition
            if (positionChange.x != 0f || positionChange.y != 0f) {
                change.consume()
            }
        }

        if (drag != null) {
            onDragStart(drag.position)

            if (drag(drag.id) { change ->
                val dragAmount = change.position - change.previousPosition
                onDrag(change, dragAmount)
                change.consume()
            }) {
                onDragEnd()
            } else {
                onDragCancel()
            }
        } else {
            onClick()
        }
    }
}

