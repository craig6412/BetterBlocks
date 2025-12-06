package com.betterblocks.animation.blockblast

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.platform.LocalDensity

/**
 * LAYER A — Static yellow highlight for lines that will clear
 * Shown BEFORE the piece is dropped
 */
@Composable
fun PreClearHighlight(
    highlightedRows: Set<Int>,
    highlightedCols: Set<Int>,
    cellDp: Dp,
    gridSize: Int,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val cellSizePx = with(density) { cellDp.toPx() }

    // Block Blast yellow highlight color
    val highlightColor = Color(0xFFFFD75A).copy(alpha = 0.65f)

    Canvas(modifier = modifier.fillMaxSize()) {
        // Draw highlighted rows
        highlightedRows.forEach { row ->
            drawRect(
                color = highlightColor,
                topLeft = Offset(0f, row * cellSizePx),
                size = Size(gridSize * cellSizePx, cellSizePx)
            )
        }

        // Draw highlighted columns
        highlightedCols.forEach { col ->
            drawRect(
                color = highlightColor,
                topLeft = Offset(col * cellSizePx, 0f),
                size = Size(cellSizePx, gridSize * cellSizePx)
            )
        }
    }
}

