// Kotlin
// File: `app/src/main/java/com/betterblocks/SimpleDragController.kt`
package com.betterblocks

import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import android.util.Log
import kotlin.math.floor

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

    // Grid information (set by GameScreen)
    var gridTopLeft by mutableStateOf(Offset.Zero)
    var gridSizePx by mutableStateOf(0f)
    var cellSizePx by mutableStateOf(0f)

    // Visual offset (stroke/padding/inset) that should be added to gridTopLeft
    var gridVisualOffset by mutableStateOf(Offset.Zero)

    // Ghost preview position (row, col)
    var ghostPosition: Pair<Int, Int>? by mutableStateOf(null)
        private set

    // Precise ghost top-left in root pixels (snapped) and its center for rendering
    var ghostTopLeftPx: Offset? by mutableStateOf(null)
        private set
    var ghostCenterPx: Offset? by mutableStateOf(null)
        private set

    fun setGridMetrics(topLeft: Offset, totalGridPx: Float, cellPx: Float, visualOffset: Offset = Offset.Zero) {
        gridTopLeft = topLeft
        gridSizePx = totalGridPx
        cellSizePx = cellPx
        gridVisualOffset = visualOffset
        Log.d("SimpleDrag", "setGridMetrics: gridTopLeft=$gridTopLeft gridSizePx=$gridSizePx cellPx=$cellSizePx visualOffset=$gridVisualOffset")
    }

    fun startDrag(block: Block, fingerPosRoot: Offset, liftPx: Float) {
        Log.d("SimpleDrag", "START called: block=${block.name} id=${block.id} finger=$fingerPosRoot, lift=$liftPx")
        Log.d("SimpleDrag", "Current grid metrics: topLeft=$gridTopLeft gridSizePx=$gridSizePx cellSizePx=$cellSizePx visualOffset=$gridVisualOffset")
        isDragging = true
        draggedBlock = block
        fingerPosition = fingerPosRoot
        liftOffset = liftPx
        updateGhost()
    }

    fun updatePosition(newFingerPos: Offset) {
        if (!isDragging) {
            Log.w("SimpleDrag", "updatePosition called but not dragging! newPos=$newFingerPos")
            return
        }
        Log.d("SimpleDrag", "updatePosition: from=$fingerPosition to=$newFingerPos")
        fingerPosition = newFingerPos
        updateGhost()
    }

    fun endDrag(board: GameGrid): Pair<Int, Int>? {
        Log.d("SimpleDrag", "END called: current ghost=$ghostPosition draggedBlock=${draggedBlock?.id}")
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
        ghostPosition = null
        ghostTopLeftPx = null
        ghostCenterPx = null
        liftOffset = 0f
    }

    /**
     * Returns the center that should be used for rendering the floating preview.
     * If a snapped ghost center exists prefer that so preview and ghost align exactly.
     */
    fun getBlockCenter(): Offset {
        return ghostCenterPx ?: (fingerPosition - Offset(0f, liftOffset))
    }

    private fun updateGhost() {
        val block = draggedBlock
        if (block == null || !isDragging || gridSizePx <= 0f || cellSizePx <= 0f) {
            ghostPosition = null
            ghostTopLeftPx = null
            ghostCenterPx = null
            return
        }

        val blockCenterFromFinger = fingerPosition - Offset(0f, liftOffset)

        // Use corrected top-left that includes any visual inset/padding/stroke
        val boardTopLeft = gridTopLeft + gridVisualOffset

        // Find block center relative to corrected grid top-left
        val relX = blockCenterFromFinger.x - boardTopLeft.x
        val relY = blockCenterFromFinger.y - boardTopLeft.y

        Log.d(
            "SimpleDrag",
            "updateGhost: finger=$fingerPosition blockCenterFromFinger=$blockCenterFromFinger boardTopLeft=$boardTopLeft rel=($relX,$relY) gridSizePx=$gridSizePx cellPx=$cellSizePx visualOffset=$gridVisualOffset"
        )

        // Quick out-of-bounds if center is fully outside extended grid bounds
        if (relX < -cellSizePx || relY < -cellSizePx || relX > gridSizePx + cellSizePx || relY > gridSizePx + cellSizePx) {
            ghostPosition = null
            ghostTopLeftPx = null
            ghostCenterPx = null
            Log.d("SimpleDrag", "updateGhost: center well out of bounds -> null")
            return
        }

        // Compute top-left origin so block's bounding-box aligns to cells.
        val halfWidthPx = (block.boundingBoxWidth * cellSizePx) / 2f
        val halfHeightPx = (block.boundingBoxHeight * cellSizePx) / 2f

        val originColFloat = (relX - halfWidthPx) / cellSizePx
        val originRowFloat = (relY - halfHeightPx) / cellSizePx

        val col = floor(originColFloat).toInt()
        val row = floor(originRowFloat).toInt()

        // Clamp to valid origin range (so block doesn't overflow grid)
        val maxCol = 9 - block.boundingBoxWidth
        val maxRow = 9 - block.boundingBoxHeight

        if (row < 0 || col < 0 || row > maxRow || col > maxCol) {
            ghostPosition = null
            ghostTopLeftPx = null
            ghostCenterPx = null
            Log.d(
                "SimpleDrag",
                "updateGhost: computed origin out of bounds row=$row col=$col maxRow=$maxRow maxCol=$maxCol -> null"
            )
            return
        }

        val snappedRow = row.coerceIn(0, maxRow)
        val snappedCol = col.coerceIn(0, maxCol)
        ghostPosition = Pair(snappedRow, snappedCol)

        // Top-left pixel snapped to integer cell indices (uses corrected boardTopLeft)
        val snappedTopLeft = Offset(boardTopLeft.x + snappedCol * cellSizePx, boardTopLeft.y + snappedRow * cellSizePx)
        ghostTopLeftPx = snappedTopLeft

        // Compute and expose the snapped center for rendering so preview & ghost agree
        val centerPx = Offset(
            x = snappedTopLeft.x + (block.boundingBoxWidth * cellSizePx) / 2f,
            y = snappedTopLeft.y + (block.boundingBoxHeight * cellSizePx) / 2f
        )
        ghostCenterPx = centerPx

        Log.d(
            "SimpleDrag",
            "Ghost updated: origin=($snappedRow,$snappedCol) originFloat=($originRowFloat,$originColFloat) ghostTopLeftPx=$ghostTopLeftPx ghostCenterPx=$ghostCenterPx"
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

    // Rotate the currently dragged block clockwise (90 degrees) and update the ghost.
    fun rotateDraggedBlockClockwise() {
        draggedBlock = draggedBlock?.rotate()
        Log.d("SimpleDragController", "rotateDraggedBlockClockwise -> draggedBlock now=${draggedBlock?.id}")
    }
}