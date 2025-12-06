package com.betterblocks.animation.blockblast

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.platform.LocalDensity
import com.betterblocks.Coord

/**
 * LAYER C (Visual) — Renders tile pop effects over the board
 * This draws the flash/tint overlay on tiles as they're being cleared
 */
@Composable
fun TileFadeRenderer(
    tileStates: Map<Coord, TilePopState>,
    cellDp: Dp,
    modifier: Modifier = Modifier
) {
    if (tileStates.isEmpty()) return

    val density = LocalDensity.current
    val cellSizePx = with(density) { cellDp.toPx() }

    Canvas(modifier = modifier.fillMaxSize()) {
        tileStates.forEach { (coord, state) ->
            if (state.flashAlpha > 0f) {
                // White flash overlay on tile
                val centerX = coord.col * cellSizePx + cellSizePx * 0.5f
                val centerY = coord.row * cellSizePx + cellSizePx * 0.5f

                // Flash with slight glow
                drawCircle(
                    color = Color.White.copy(alpha = state.flashAlpha * 0.5f),
                    radius = cellSizePx * 0.8f,
                    center = Offset(centerX, centerY),
                    blendMode = BlendMode.Plus
                )

                drawRect(
                    color = Color.White.copy(alpha = state.flashAlpha),
                    topLeft = Offset(coord.col * cellSizePx, coord.row * cellSizePx),
                    size = Size(cellSizePx, cellSizePx),
                    blendMode = BlendMode.Plus
                )
            }
        }
    }
}

