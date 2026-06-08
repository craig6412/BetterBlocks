// Kotlin
// File: `app/src/main/java/com/betterblocks/SimpleDragController.kt`
package com.betterblocks

import android.util.Log
import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import kotlin.math.roundToInt

class SimpleDragController {

    // Current drag state
    var isDragging by mutableStateOf(false)
        private set

    var draggedBlock by mutableStateOf<Block?>(null)
        private set

    // Finger position in root coordinates
    var fingerPosition by mutableStateOf(Offset.Zero)
        private set

    // How far above finger the block floats (px)
    private var liftOffset = 0f

    // Grid information. This should be set from the actual rendered board.
    var gridTopLeft by mutableStateOf(Offset.Zero)
    var gridSizePx by mutableStateOf(0f)
    var cellSizePx by mutableStateOf(0f)

    // Visual offset (stroke/padding/inset) that should be added to gridTopLeft
    var gridVisualOffset by mutableStateOf(Offset.Zero)

    /**
     * Compatibility name kept because GameScreen / AnimatedBoardRenderer already consume it.
     * For the no-ghost system, this is the truthful snapped placement position.
     */
    var ghostPosition: Pair<Int, Int>? by mutableStateOf(null)
        private set

    /**
     * Compatibility name kept because existing code expects it.
     * For the no-ghost system, this is the truthful snapped top-left for the dragged block.
     */
    var ghostTopLeftPx: Offset? by mutableStateOf(null)
        private set

    /**
     * Compatibility name kept because existing code expects it.
     * For the no-ghost system, this is the truthful snapped center for the dragged block.
     */
    var ghostCenterPx: Offset? by mutableStateOf(null)
        private set

    fun setGridMetrics(topLeft: Offset, totalGridPx: Float, cellPx: Float, visualOffset: Offset = Offset.Zero) {
        gridTopLeft = topLeft
        gridSizePx = totalGridPx
        cellSizePx = cellPx
        gridVisualOffset = visualOffset
        Log.d(
            "SimpleDrag",
            "setGridMetrics: gridTopLeft=$gridTopLeft gridSizePx=$gridSizePx cellPx=$cellSizePx visualOffset=$gridVisualOffset"
        )
        updateSnapPosition()
    }

    fun startDrag(block: Block, fingerPosRoot: Offset, liftPx: Float) {
        Log.d("SimpleDrag", "START called: block=${block.name} id=${block.id} finger=$fingerPosRoot, lift=$liftPx")
        Log.d("SimpleDrag", "Current grid metrics: topLeft=$gridTopLeft gridSizePx=$gridSizePx cellSizePx=$cellSizePx visualOffset=$gridVisualOffset")
        isDragging = true
        draggedBlock = block
        fingerPosition = fingerPosRoot
        liftOffset = liftPx
        updateSnapPosition()
    }

    fun updatePosition(newFingerPos: Offset) {
        if (!isDragging) {
            Log.w("SimpleDrag", "updatePosition called but not dragging! newPos=$newFingerPos")
            return
        }
        fingerPosition = newFingerPos
        updateSnapPosition()
    }

    fun endDrag(board: GameGrid): Pair<Int, Int>? {
        Log.d("SimpleDrag", "END called: placement=$ghostPosition draggedBlock=${draggedBlock?.id}")
        val block = draggedBlock
        val pos = ghostPosition

        val result = if (block != null && pos != null && isValidPlacement(board, block, pos)) {
            Log.d("SimpleDrag", "✓ Valid drop at $pos")
            pos
        } else {
            Log.d("SimpleDrag", "✗ Invalid drop - block=$block pos=$pos")
            null
        }

        reset()
        return result
    }

    fun cancel() {
        Log.d("SimpleDrag", "CANCEL")
        reset()
    }

    private fun reset() {
        isDragging = false
        draggedBlock = null
        fingerPosition = Offset.Zero
        clearSnapPosition()
        liftOffset = 0f
    }

    /**
     * The dragged block itself is the preview.
     * Outside the board it follows the finger. Over the board it uses the exact snapped placement center.
     */
    fun getBlockCenter(): Offset {
        return ghostCenterPx ?: getFloatingBlockCenter()
    }

    fun getBlockTopLeft(): Offset {
        val block = draggedBlock
        val snappedTopLeft = ghostTopLeftPx
        if (block != null && snappedTopLeft != null) return snappedTopLeft

        val center = getFloatingBlockCenter()
        val widthPx = ((block?.boundingBoxWidth ?: 1) * cellSizePx).takeIf { it > 0f } ?: cellSizePx.coerceAtLeast(1f)
        val heightPx = ((block?.boundingBoxHeight ?: 1) * cellSizePx).takeIf { it > 0f } ?: cellSizePx.coerceAtLeast(1f)
        return Offset(
            x = center.x - widthPx / 2f,
            y = center.y - heightPx / 2f
        )
    }

    private fun getFloatingBlockCenter(): Offset {
        return fingerPosition - Offset(0f, liftOffset)
    }

    private fun clearSnapPosition() {
        ghostPosition = null
        ghostTopLeftPx = null
        ghostCenterPx = null
    }

    private fun updateSnapPosition() {
        val block = draggedBlock
        if (block == null || !isDragging || gridSizePx <= 0f || cellSizePx <= 0f) {
            clearSnapPosition()
            return
        }

        val blockCenterFromFinger = getFloatingBlockCenter()
        val boardTopLeft = gridTopLeft + gridVisualOffset

        val relX = blockCenterFromFinger.x - boardTopLeft.x
        val relY = blockCenterFromFinger.y - boardTopLeft.y

        // The block is not over the board yet. No stale placement is allowed.
        if (relX < 0f || relY < 0f || relX > gridSizePx || relY > gridSizePx) {
            clearSnapPosition()
            return
        }

        val halfWidthPx = (block.boundingBoxWidth * cellSizePx) / 2f
        val halfHeightPx = (block.boundingBoxHeight * cellSizePx) / 2f

        val originColFloat = (relX - halfWidthPx) / cellSizePx
        val originRowFloat = (relY - halfHeightPx) / cellSizePx

        val maxCol = 9 - block.boundingBoxWidth
        val maxRow = 9 - block.boundingBoxHeight
        if (maxCol < 0 || maxRow < 0) {
            clearSnapPosition()
            return
        }

        val snappedCol = originColFloat.roundToInt().coerceIn(0, maxCol)
        val snappedRow = originRowFloat.roundToInt().coerceIn(0, maxRow)
        val snappedPosition = Pair(snappedRow, snappedCol)
        val snappedTopLeft = Offset(
            x = boardTopLeft.x + snappedCol * cellSizePx,
            y = boardTopLeft.y + snappedRow * cellSizePx
        )
        val snappedCenter = Offset(
            x = snappedTopLeft.x + (block.boundingBoxWidth * cellSizePx) / 2f,
            y = snappedTopLeft.y + (block.boundingBoxHeight * cellSizePx) / 2f
        )

        ghostPosition = snappedPosition
        ghostTopLeftPx = snappedTopLeft
        ghostCenterPx = snappedCenter

        Log.d(
            "SimpleDrag",
            "Snap updated: origin=$snappedPosition originFloat=($originRowFloat,$originColFloat) topLeft=$snappedTopLeft center=$snappedCenter"
        )
    }

    private fun isValidPlacement(board: GameGrid, block: Block, origin: Pair<Int, Int>): Boolean {
        val (row, col) = origin
        if (row + block.boundingBoxHeight > 9 || col + block.boundingBoxWidth > 9) {
            return false
        }
        if (!block.isSpecial) {
            for (cell in block.shape) {
                val r = row + cell.row
                val c = col + cell.col
                if (r < 0 || r >= 9 || c < 0 || c >= 9) return false
                if (board[r][c] != null) return false
            }
        }
        return true
    }

    // Rotate the currently dragged block clockwise (90 degrees) and update the truthful snap position.
    fun rotateDraggedBlockClockwise() {
        draggedBlock = draggedBlock?.rotate()
        updateSnapPosition()
        Log.d("SimpleDragController", "rotateDraggedBlockClockwise -> draggedBlock now=${draggedBlock?.id}")
    }
}
