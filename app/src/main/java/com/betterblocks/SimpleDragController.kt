// Kotlin
// File: `app/src/main/java/com/betterblocks/SimpleDragController.kt`
package com.betterblocks

import android.util.Log
import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import kotlin.math.roundToInt

/**
 * Block Blast style drag controller.
 *
 * SINGLE coordinate space: ROOT coordinates.
 *  - Board origin (gridTopLeft) MUST be captured with positionInRoot().
 *  - Finger position MUST be delivered in root coordinates by the gesture layer
 *    (GameScreen names these callbacks fingerPosRoot — keep that contract).
 *  - The floating preview is DRAWN from exactly the same previewTopLeft that the
 *    snap candidate is derived from, so the visual always matches placement.
 *
 * The preview floats freely (never grid-locked, never clamped to legal bounds)
 * DURING the drag. Clamping to legal bounds happens ONLY when deriving the
 * release candidate row/col, and never moves the drawn piece.
 */
class SimpleDragController {

    var isDragging by mutableStateOf(false)
        private set

    var draggedBlock by mutableStateOf<Block?>(null)
        private set

    // Finger position in ROOT coordinates.
    var fingerPosition by mutableStateOf(Offset.Zero)
        private set

    // How far above the finger the block floats (px).
    private var liftOffset = 0f

    // Vector from the block's top-left to the finger grab point (px).
    // Captured at drag start and rebuilt on rotate / metric change.
    // Centers the piece under the finger so behavior is identical for tall & wide pieces.
    private var grabOffset = Offset.Zero

    // ---- Grid metrics. ALL in ROOT coordinates. ----
    var gridTopLeft by mutableStateOf(Offset.Zero)
    var gridSizePx by mutableStateOf(0f)
    var cellSizePx by mutableStateOf(0f)

    // Kept only for source compatibility with setGridMetrics callers.
    // Intentionally IGNORED in placement math (the LazyVerticalGrid content is not
    // inset by the board border, so this must be Offset.Zero).
    var gridVisualOffset by mutableStateOf(Offset.Zero)

    /**
     * Snapped placement (row, col). Null when the piece is NOT over the board.
     * Name kept ("ghostPosition") because GameScreen / AnimatedGameBoard consume it.
     * This is the truthful row/col used both for the line-clear tint preview and on drop.
     */
    var ghostPosition: Pair<Int, Int>? by mutableStateOf(null)
        private set

    // Snapped top-left / center in root coords. Kept for compatibility only.
    // NOTE: these are NOT used to draw the floating preview anymore.
    var ghostTopLeftPx: Offset? by mutableStateOf(null)
        private set
    var ghostCenterPx: Offset? by mutableStateOf(null)
        private set

    // Hysteresis so the committed cell does not twitch on tiny finger movement.
    private var committedSnapRow: Int? = null
    private var committedSnapCol: Int? = null

    // 0.50 = plain nearest-cell snapping. 0.58 is slightly stickier / more Block-Blast-like.
    private val snapHysteresisThreshold = 0.58f

    fun setGridMetrics(
        topLeft: Offset,
        totalGridPx: Float,
        cellPx: Float,
        visualOffset: Offset = Offset.Zero
    ) {
        gridTopLeft = topLeft
        gridSizePx = totalGridPx
        cellSizePx = cellPx
        gridVisualOffset = visualOffset // retained but intentionally ignored below
        Log.d(
            "SimpleDrag",
            "setGridMetrics: gridTopLeft=$gridTopLeft gridSizePx=$gridSizePx cellPx=$cellPx (visualOffset ignored=$visualOffset)"
        )
        rebuildGrabOffset()
        updateSnapPosition()
    }

    fun startDrag(block: Block, fingerPosRoot: Offset, liftPx: Float) {
        Log.d("SimpleDrag", "START block=${block.name} id=${block.id} fingerRoot=$fingerPosRoot lift=$liftPx")
        Log.d("SimpleDrag", "grid topLeft=$gridTopLeft sizePx=$gridSizePx cellPx=$cellSizePx")
        isDragging = true
        draggedBlock = block
        fingerPosition = fingerPosRoot
        liftOffset = liftPx
        committedSnapRow = null
        committedSnapCol = null
        rebuildGrabOffset()
        updateSnapPosition()
    }

    fun updatePosition(newFingerPosRoot: Offset) {
        if (!isDragging) {
            Log.w("SimpleDrag", "updatePosition called but not dragging! newPos=$newFingerPosRoot")
            return
        }
        fingerPosition = newFingerPosRoot
        updateSnapPosition()
    }

    fun endDrag(board: GameGrid): Pair<Int, Int>? {
        val block = draggedBlock
        val pos = ghostPosition
        val result = if (block != null && pos != null && isValidPlacement(board, block, pos)) {
            Log.d("DragReleaseSnap", "VALID drop at $pos previewTopLeft=${getBlockTopLeft()}")
            pos
        } else {
            Log.d("DragReleaseSnap", "INVALID drop pos=$pos previewTopLeft=${getBlockTopLeft()} -> return to tray")
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
        liftOffset = 0f
        grabOffset = Offset.Zero
        clearSnapPosition()
    }

    /**
     * The free, UNCLAMPED preview top-left in ROOT coordinates.
     * This is the single source of truth for the visual AND for the snap candidate.
     *
     *   previewTopLeft = fingerPosition - grabOffset - liftOffset
     */
    fun getBlockTopLeft(): Offset {
        return fingerPosition - grabOffset - Offset(0f, liftOffset)
    }

    fun getBlockCenter(): Offset {
        val tl = getBlockTopLeft()
        val (w, h) = blockSizePx()
        return Offset(tl.x + w / 2f, tl.y + h / 2f)
    }

    private fun blockSizePx(): Pair<Float, Float> {
        val block = draggedBlock
        val cell = cellSizePx.coerceAtLeast(1f)
        val w = (block?.boundingBoxWidth ?: 1) * cell
        val h = (block?.boundingBoxHeight ?: 1) * cell
        return w to h
    }

    private fun rebuildGrabOffset() {
        val (w, h) = blockSizePx()
        // Center the piece under the finger. Same rule for tall and wide pieces,
        // so nothing gets pinned or jumps.
        grabOffset = Offset(w / 2f, h / 2f)
    }

    private fun clearSnapPosition() {
        ghostPosition = null
        ghostTopLeftPx = null
        ghostCenterPx = null
        committedSnapRow = null
        committedSnapCol = null
    }

    private fun updateSnapPosition() {
        val block = draggedBlock
        if (block == null || !isDragging || gridSizePx <= 0f || cellSizePx <= 0f) {
            clearSnapPosition()
            return
        }

        // Candidate is derived from the SAME free top-left used to draw the preview.
        val previewTopLeft = getBlockTopLeft()
        val (wPx, hPx) = blockSizePx()
        val previewCenter = Offset(previewTopLeft.x + wPx / 2f, previewTopLeft.y + hPx / 2f)

        // Gate: only show a placement when the piece CENTER is actually over the board.
        // Outside the board -> no stale snap (the preview keeps floating with the finger).
        if (previewCenter.x < gridTopLeft.x ||
            previewCenter.y < gridTopLeft.y ||
            previewCenter.x > gridTopLeft.x + gridSizePx ||
            previewCenter.y > gridTopLeft.y + gridSizePx
        ) {
            clearSnapPosition()
            return
        }

        val maxCol = 9 - block.boundingBoxWidth
        val maxRow = 9 - block.boundingBoxHeight
        if (maxCol < 0 || maxRow < 0) {
            clearSnapPosition()
            return
        }

        // Board-relative top-left in cells. NOTE: top-left based, NOT center based,
        // so the candidate origin matches the drawn top-left exactly.
        val originColFloat = (previewTopLeft.x - gridTopLeft.x) / cellSizePx
        val originRowFloat = (previewTopLeft.y - gridTopLeft.y) / cellSizePx

        val rawCol = originColFloat.roundToInt().coerceIn(0, maxCol)
        val rawRow = originRowFloat.roundToInt().coerceIn(0, maxRow)

        val snappedCol = commitWithHysteresis(committedSnapCol, rawCol, originColFloat, 0, maxCol)
        val snappedRow = commitWithHysteresis(committedSnapRow, rawRow, originRowFloat, 0, maxRow)
        committedSnapCol = snappedCol
        committedSnapRow = snappedRow

        val snappedTopLeft = Offset(
            x = gridTopLeft.x + snappedCol * cellSizePx,
            y = gridTopLeft.y + snappedRow * cellSizePx
        )

        ghostPosition = snappedRow to snappedCol
        ghostTopLeftPx = snappedTopLeft
        ghostCenterPx = Offset(snappedTopLeft.x + wPx / 2f, snappedTopLeft.y + hPx / 2f)

        Log.d(
            "DragReleaseSnap",
            "candidate=$ghostPosition origin=($originRowFloat,$originColFloat) raw=($rawRow,$rawCol) previewTopLeft=$previewTopLeft"
        )
    }

    private fun commitWithHysteresis(
        current: Int?,
        rawRounded: Int,
        rawFloat: Float,
        minValue: Int,
        maxValue: Int
    ): Int {
        if (current == null) return rawRounded.coerceIn(minValue, maxValue)

        var committed = current.coerceIn(minValue, maxValue)

        // Large jump (fast finger): catch up immediately.
        if (rawRounded > committed + 1 || rawRounded < committed - 1) {
            return rawRounded.coerceIn(minValue, maxValue)
        }
        // Step into the next/previous cell only once the origin is deep enough.
        while (committed < maxValue && rawFloat >= committed + snapHysteresisThreshold) committed++
        while (committed > minValue && rawFloat <= committed - snapHysteresisThreshold) committed--

        return committed.coerceIn(minValue, maxValue)
    }

    private fun isValidPlacement(board: GameGrid, block: Block, origin: Pair<Int, Int>): Boolean {
        val (row, col) = origin
        if (row + block.boundingBoxHeight > 9 || col + block.boundingBoxWidth > 9) return false
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

    // Rotate the currently dragged block clockwise and rebuild the truthful snap.
    fun rotateDraggedBlockClockwise() {
        draggedBlock = draggedBlock?.rotate()
        committedSnapRow = null
        committedSnapCol = null
        rebuildGrabOffset()        // shape changed -> recenter under finger
        updateSnapPosition()
        Log.d("SimpleDragController", "rotateDraggedBlockClockwise -> draggedBlock now=${draggedBlock?.id}")
    }
}
