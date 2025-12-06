package com.betterblocks.animation.blockblast

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.platform.LocalDensity
import kotlin.random.Random

/**
 * LAYER B — Block Blast style sweep bar
 * Thick glowing orange bar with white core, edge flares, and floating particles
 */
@Composable
fun SweepBarRenderer(
    sweepState: SweepBarState?,
    cellDp: Dp,
    gridSize: Int,
    modifier: Modifier = Modifier
) {
    if (sweepState == null || sweepState.alpha <= 0f) return

    val density = LocalDensity.current
    val cellSizePx = with(density) { cellDp.toPx() }
    val totalSizePx = cellSizePx * gridSize

    Canvas(modifier = modifier.fillMaxSize()) {
        val barThicknessPx = cellSizePx * 1.2f
        val coreThicknessPx = barThicknessPx * 0.7f

        if (sweepState.isRow) {
            // Horizontal sweep (left to right)
            val barY = sweepState.lineIndex * cellSizePx
            val barX = sweepState.position * totalSizePx

            // Outer glow layer (orange bloom)
            val outerBrush = Brush.verticalGradient(
                0f to Color(0xFFFF8A00).copy(alpha = 0f),
                0.2f to Color(0xFFFF8A00).copy(alpha = 0.4f * sweepState.alpha),
                0.5f to Color(0xFFFF8A00).copy(alpha = 0.8f * sweepState.alpha),
                0.8f to Color(0xFFFF8A00).copy(alpha = 0.4f * sweepState.alpha),
                1f to Color(0xFFFF8A00).copy(alpha = 0f)
            )

            drawRect(
                brush = outerBrush,
                topLeft = Offset(barX - barThicknessPx * 0.5f, barY - barThicknessPx * 0.2f),
                size = Size(barThicknessPx, cellSizePx + barThicknessPx * 0.4f),
                blendMode = BlendMode.Plus
            )

            // Inner white-hot core
            val coreBrush = Brush.verticalGradient(
                0f to Color.White.copy(alpha = 0f),
                0.3f to Color.White.copy(alpha = 0.9f * sweepState.alpha),
                0.7f to Color.White.copy(alpha = 0.9f * sweepState.alpha),
                1f to Color.White.copy(alpha = 0f)
            )

            drawRect(
                brush = coreBrush,
                topLeft = Offset(barX - coreThicknessPx * 0.3f, barY + cellSizePx * 0.15f),
                size = Size(coreThicknessPx * 0.6f, cellSizePx * 0.7f),
                blendMode = BlendMode.Plus
            )

            // Left edge flare
            drawCircle(
                color = Color.White.copy(alpha = 0.8f * sweepState.alpha),
                radius = cellSizePx * 0.15f,
                center = Offset(barX - barThicknessPx * 0.4f, barY + cellSizePx * 0.5f),
                blendMode = BlendMode.Plus
            )

            // Right edge flare
            drawCircle(
                color = Color.White.copy(alpha = 0.6f * sweepState.alpha),
                radius = cellSizePx * 0.12f,
                center = Offset(barX + barThicknessPx * 0.4f, barY + cellSizePx * 0.5f),
                blendMode = BlendMode.Plus
            )

            // Floating squares inside bar
            sweepState.floatingSquares.forEach { square ->
                drawRect(
                    color = Color(0xFFFFD75A).copy(alpha = square.alpha * sweepState.alpha),
                    topLeft = Offset(
                        barX + square.offsetX - square.size * 0.5f,
                        barY + cellSizePx * square.offsetY - square.size * 0.5f
                    ),
                    size = Size(square.size, square.size)
                )
            }

            // Trailing dust particles
            sweepState.dustParticles.forEach { dust ->
                drawCircle(
                    color = Color(0xFFFFAA33).copy(alpha = dust.alpha * sweepState.alpha),
                    radius = dust.size,
                    center = Offset(
                        barX + dust.offsetX,
                        barY + cellSizePx * dust.offsetY
                    ),
                    blendMode = BlendMode.Plus
                )
            }

        } else {
            // Vertical sweep (top to bottom)
            val barX = sweepState.lineIndex * cellSizePx
            val barY = sweepState.position * totalSizePx

            // Outer glow layer (orange bloom)
            val outerBrush = Brush.horizontalGradient(
                0f to Color(0xFFFF8A00).copy(alpha = 0f),
                0.2f to Color(0xFFFF8A00).copy(alpha = 0.4f * sweepState.alpha),
                0.5f to Color(0xFFFF8A00).copy(alpha = 0.8f * sweepState.alpha),
                0.8f to Color(0xFFFF8A00).copy(alpha = 0.4f * sweepState.alpha),
                1f to Color(0xFFFF8A00).copy(alpha = 0f)
            )

            drawRect(
                brush = outerBrush,
                topLeft = Offset(barX - barThicknessPx * 0.2f, barY - barThicknessPx * 0.5f),
                size = Size(cellSizePx + barThicknessPx * 0.4f, barThicknessPx),
                blendMode = BlendMode.Plus
            )

            // Inner white-hot core
            val coreBrush = Brush.horizontalGradient(
                0f to Color.White.copy(alpha = 0f),
                0.3f to Color.White.copy(alpha = 0.9f * sweepState.alpha),
                0.7f to Color.White.copy(alpha = 0.9f * sweepState.alpha),
                1f to Color.White.copy(alpha = 0f)
            )

            drawRect(
                brush = coreBrush,
                topLeft = Offset(barX + cellSizePx * 0.15f, barY - coreThicknessPx * 0.3f),
                size = Size(cellSizePx * 0.7f, coreThicknessPx * 0.6f),
                blendMode = BlendMode.Plus
            )

            // Top edge flare
            drawCircle(
                color = Color.White.copy(alpha = 0.8f * sweepState.alpha),
                radius = cellSizePx * 0.15f,
                center = Offset(barX + cellSizePx * 0.5f, barY - barThicknessPx * 0.4f),
                blendMode = BlendMode.Plus
            )

            // Bottom edge flare
            drawCircle(
                color = Color.White.copy(alpha = 0.6f * sweepState.alpha),
                radius = cellSizePx * 0.12f,
                center = Offset(barX + cellSizePx * 0.5f, barY + barThicknessPx * 0.4f),
                blendMode = BlendMode.Plus
            )

            // Floating squares inside bar
            sweepState.floatingSquares.forEach { square ->
                drawRect(
                    color = Color(0xFFFFD75A).copy(alpha = square.alpha * sweepState.alpha),
                    topLeft = Offset(
                        barX + cellSizePx * square.offsetY - square.size * 0.5f,
                        barY + square.offsetX - square.size * 0.5f
                    ),
                    size = Size(square.size, square.size)
                )
            }

            // Trailing dust particles
            sweepState.dustParticles.forEach { dust ->
                drawCircle(
                    color = Color(0xFFFFAA33).copy(alpha = dust.alpha * sweepState.alpha),
                    radius = dust.size,
                    center = Offset(
                        barX + cellSizePx * dust.offsetY,
                        barY + dust.offsetX
                    ),
                    blendMode = BlendMode.Plus
                )
            }
        }
    }
}

/**
 * State for the sweep bar animation
 */
data class SweepBarState(
    val isRow: Boolean,
    val lineIndex: Int,
    val position: Float, // 0.0 to 1.0 across the line
    val alpha: Float, // For fade in/out
    val floatingSquares: List<FloatingSquare>,
    val dustParticles: List<DustParticle>
)

data class FloatingSquare(
    val offsetX: Float, // Relative to bar position
    val offsetY: Float, // 0.0 to 1.0 within cell height
    val size: Float,
    val alpha: Float
)

data class DustParticle(
    val offsetX: Float,
    val offsetY: Float,
    val size: Float,
    val alpha: Float
)

/**
 * Generate floating squares and dust for the sweep bar
 */
fun generateSweepBarEffects(random: Random = Random): Pair<List<FloatingSquare>, List<DustParticle>> {
    val squares = List(6) {
        FloatingSquare(
            offsetX = random.nextFloat() * 30f - 15f,
            offsetY = random.nextFloat(),
            size = random.nextFloat() * 8f + 4f,
            alpha = random.nextFloat() * 0.6f + 0.4f
        )
    }

    val dust = List(12) {
        DustParticle(
            offsetX = random.nextFloat() * -40f - 10f, // Trail behind
            offsetY = random.nextFloat(),
            size = random.nextFloat() * 3f + 1f,
            alpha = random.nextFloat() * 0.5f + 0.2f
        )
    }

    return squares to dust
}

