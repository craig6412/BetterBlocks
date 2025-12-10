package com.betterblocks.animation

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.betterblocks.*
import com.betterblocks.BoardBackground

import androidx.compose.foundation.shape.RoundedCornerShape
import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.sqrt
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import kotlin.div
import kotlin.math.cos
import kotlin.math.sin

// Core palette for Cyber-Void theme
private val GridPurple = Color(0xFF08060B)             // Game Grid / Empty Slots: Abyssal Purple
private val GridLineColor = Color(0x407F5AF0)         // Grid Lines: Faint Lavender (25% opacity)

// --------------------------------------------------------------------------
// EPIC LIGHTNING SWEEP OVERLAY - Multi-layered energy wave effect
// --------------------------------------------------------------------------





@Composable
fun SweepOverlay(
    progress: Float,
    isRow: Boolean,
    lineIndex: Int,
    cellDp: Dp,
    gridSize: Int,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val cellSizePx = cellDp.toPx()
        val totalSize = gridSize * cellSizePx

        // Multi-phase animation for dramatic effect
        val earlyProgress = (progress * 1.3f).coerceIn(0f, 1f)
        val lateProgress = ((progress - 0.3f) * 1.5f).coerceIn(0f, 1f)

        if (isRow) {
            val y = lineIndex * cellSizePx
            val centerY = y + cellSizePx * 0.5f

            // LAYER 1: Leading electric arc (cyan-white)
            val arcPosition = earlyProgress * size.width
            val arcBrush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFFFFFFF),
                    Color(0xFF00E5FF),
                    Color(0xFF0091EA),
                    Color(0x00000000)
                ),
                center = Offset(arcPosition, centerY),
                radius = cellSizePx * 2.5f
            )
            drawRect(
                brush = arcBrush,
                topLeft = Offset(0f, y),
                size = androidx.compose.ui.geometry.Size(size.width, cellSizePx),
                blendMode = BlendMode.Plus
            )

            // LAYER 2: Energy trail (electric blue shimmer)
            if (progress > 0.1f) {
                val trailStart = (progress - 0.1f) * size.width
                val trailBrush = Brush.horizontalGradient(
                    colors = listOf(
                        Color(0x00000000),
                        Color(0xFF00B0FF).copy(alpha = 0.6f),
                        Color(0xFF00E5FF).copy(alpha = 0.8f),
                        Color(0xFFFFFFFF).copy(alpha = 0.9f)
                    ),
                    startX = trailStart - cellSizePx * 3f,
                    endX = arcPosition
                )
                drawRect(
                    brush = trailBrush,
                    topLeft = Offset(0f, y),
                    size = androidx.compose.ui.geometry.Size(size.width, cellSizePx),
                    blendMode = BlendMode.Screen
                )
            }

            // LAYER 3: Charged particles ahead of wave
            for (i in 0..5) {
                val particleOffset = (i / 5f) * cellSizePx * 2f
                val particleX = arcPosition + particleOffset
                val particleAlpha = (1f - i / 5f) * progress
                if (particleX < size.width) {
                    drawCircle(
                        color = Color(0xFF00FFFF).copy(alpha = particleAlpha * 0.8f),
                        radius = cellSizePx * 0.15f * (1f - i / 8f),
                        center = Offset(particleX, centerY + (i % 2) * cellSizePx * 0.3f - cellSizePx * 0.15f),
                        blendMode = BlendMode.Plus
                    )
                }
            }

            // LAYER 4: Explosive core flash
            if (progress > 0.05f) {
                val coreAlpha = if (progress < 0.3f) {
                    (progress - 0.05f) / 0.25f
                } else {
                    1f - ((progress - 0.3f) / 0.7f)
                }.coerceIn(0f, 1f)

                val coreBrush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = coreAlpha * 0.95f),
                        Color(0xFFFFFF00).copy(alpha = coreAlpha * 0.7f),
                        Color(0xFF00E5FF).copy(alpha = coreAlpha * 0.4f),
                        Color(0x00000000)
                    ),
                    center = Offset(arcPosition, centerY),
                    radius = cellSizePx * 1.2f
                )
                drawRect(
                    brush = coreBrush,
                    topLeft = Offset(0f, y),
                    size = androidx.compose.ui.geometry.Size(size.width, cellSizePx),
                    blendMode = BlendMode.Plus
                )
            }

        } else {
            // VERTICAL SWEEP (top to bottom)
            val x = lineIndex * cellSizePx
            val centerX = x + cellSizePx * 0.5f

            // LAYER 1: Leading electric arc
            val arcPosition = earlyProgress * size.height
            val arcBrush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFFFFFFF),
                    Color(0xFF00E5FF),
                    Color(0xFF0091EA),
                    Color(0x00000000)
                ),
                center = Offset(centerX, arcPosition),
                radius = cellSizePx * 2.5f
            )
            drawRect(
                brush = arcBrush,
                topLeft = Offset(x, 0f),
                size = androidx.compose.ui.geometry.Size(cellSizePx, size.height),
                blendMode = BlendMode.Plus
            )

            // LAYER 2: Energy trail
            if (progress > 0.1f) {
                val trailStart = (progress - 0.1f) * size.height
                val trailBrush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0x00000000),
                        Color(0xFF00B0FF).copy(alpha = 0.6f),
                        Color(0xFF00E5FF).copy(alpha = 0.8f),
                        Color(0xFFFFFFFF).copy(alpha = 0.9f)
                    ),
                    startY = trailStart - cellSizePx * 3f,
                    endY = arcPosition
                )
                drawRect(
                    brush = trailBrush,
                    topLeft = Offset(x, 0f),
                    size = androidx.compose.ui.geometry.Size(cellSizePx, size.height),
                    blendMode = BlendMode.Screen
                )
            }

            // LAYER 3: Charged particles
            for (i in 0..5) {
                val particleOffset = (i / 5f) * cellSizePx * 2f
                val particleY = arcPosition + particleOffset
                val particleAlpha = (1f - i / 5f) * progress
                if (particleY < size.height) {
                    drawCircle(
                        color = Color(0xFF00FFFF).copy(alpha = particleAlpha * 0.8f),
                        radius = cellSizePx * 0.15f * (1f - i / 8f),
                        center = Offset(centerX + (i % 2) * cellSizePx * 0.3f - cellSizePx * 0.15f, particleY),
                        blendMode = BlendMode.Plus
                    )
                }
            }

            // LAYER 4: Explosive core flash
            if (progress > 0.05f) {
                val coreAlpha = if (progress < 0.3f) {
                    (progress - 0.05f) / 0.25f
                } else {
                    1f - ((progress - 0.3f) / 0.7f)
                }.coerceIn(0f, 1f)

                val coreBrush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = coreAlpha * 0.95f),
                        Color(0xFFFFFF00).copy(alpha = coreAlpha * 0.7f),
                        Color(0xFF00E5FF).copy(alpha = coreAlpha * 0.4f),
                        Color(0x00000000)
                    ),
                    center = Offset(centerX, arcPosition),
                    radius = cellSizePx * 1.2f
                )
                drawRect(
                    brush = coreBrush,
                    topLeft = Offset(x, 0f),
                    size = androidx.compose.ui.geometry.Size(cellSizePx, size.height),
                    blendMode = BlendMode.Plus
                )
            }
        }
    }
}

// --------------------------------------------------------------------------
// EPIC SUPERNOVA RADIAL BURST - Full-board explosive clear effect
// --------------------------------------------------------------------------

@Composable
fun RadialBurstOverlay(
    progress: Float,
    gridSize: Int,
    cellDp: Dp,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val cellSizePx = cellDp.toPx()
        val center = Offset(
            (gridSize / 2f) * cellSizePx,
            (gridSize / 2f) * cellSizePx
        )

        val maxRadius = sqrt(center.x * center.x + center.y * center.y)
        val currentRadius = (progress * maxRadius).coerceAtLeast(1f)

        // LAYER 1: Explosive white-hot core
        if (progress < 0.4f && progress > 0.001f) {
            val coreAlpha = if (progress < 0.15f) {
                progress / 0.15f
            } else {
                1f - ((progress - 0.15f) / 0.25f)
            }
            val coreRadius = (cellSizePx * 3f * (1f + progress * 2f)).coerceAtLeast(1f)

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = coreAlpha),
                        Color(0xFFFFFF00).copy(alpha = coreAlpha * 0.8f),
                        Color(0xFFFF6600).copy(alpha = coreAlpha * 0.5f),
                        Color(0x00000000)
                    ),
                    center = center,
                    radius = coreRadius
                ),
                center = center,
                radius = coreRadius,
                blendMode = BlendMode.Plus
            )
        }

        // LAYER 2: Primary expanding shockwave (cyan-white)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0x00000000),
                    Color(0xFF00E5FF).copy(alpha = 0.8f * (1f - progress)),
                    Color(0xFFFFFFFF).copy(alpha = 0.9f * (1f - progress)),
                    Color(0xFF00E5FF).copy(alpha = 0.6f * (1f - progress)),
                    Color(0x00000000)
                ),
                center = center,
                radius = currentRadius
            ),
            center = center,
            radius = currentRadius,
            blendMode = BlendMode.Plus
        )

        // LAYER 3: Secondary shockwave ripples
        for (i in 1..3) {
            val rippleDelay = i * 0.15f
            val rippleProgress = ((progress - rippleDelay) / (1f - rippleDelay)).coerceIn(0f, 1f)
            if (rippleProgress > 0.001f) {
                val rippleRadius = (rippleProgress * maxRadius).coerceAtLeast(1f)
                val rippleAlpha = (1f - rippleProgress) * 0.6f

                drawCircle(
                    color = Color(0xFF00B0FF).copy(alpha = rippleAlpha),
                    radius = rippleRadius,
                    center = center,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                        width = (cellSizePx * 0.3f * (1f + rippleProgress)).coerceAtLeast(1f)
                    ),
                    blendMode = BlendMode.Screen
                )
            }
        }

        // LAYER 4: Spiraling energy particles
        val particleCount = 24
        for (i in 0 until particleCount) {
            val angle = (i / particleCount.toFloat()) * 2 * Math.PI.toFloat() + progress * 3f
            val particleRadius = currentRadius * 0.85f
            val particleX = center.x + cos(angle) * particleRadius
            val particleY = center.y + sin(angle) * particleRadius
            val particleAlpha = (1f - progress) * 0.8f

            drawCircle(
                color = Color(0xFF00FFFF).copy(alpha = particleAlpha),
                radius = (cellSizePx * 0.2f).coerceAtLeast(1f),
                center = Offset(particleX, particleY),
                blendMode = BlendMode.Plus
            )
        }

        // LAYER 5: Pulsing energy rings (multiple concentric)
        val ringColors = listOf(
            Color(0xFF00E5FF),
            Color(0xFF00B0FF),
            Color(0xFF0091EA)
        )

        ringColors.forEachIndexed { i, ringColor ->
            val ringProgress = (progress + i * 0.1f).coerceIn(0f, 1f)
            val ringRadius = (ringProgress * maxRadius * 0.9f).coerceAtLeast(1f)
            val ringAlpha = (1f - ringProgress) * 0.5f

            drawCircle(
                color = ringColor.copy(alpha = ringAlpha),
                radius = ringRadius,
                center = center,
                blendMode = BlendMode.Screen
            )
        }

        // LAYER 6: Scattered impact flashes
        if (progress > 0.3f) {
            for (i in 0..7) {
                val flashAngle = (i / 8f) * 2 * Math.PI.toFloat()
                val flashDist = currentRadius * 0.7f
                val flashX = center.x + cos(flashAngle) * flashDist
                val flashY = center.y + sin(flashAngle) * flashDist
                val flashAlpha = ((1f - progress) * 2f).coerceIn(0f, 1f)

                drawCircle(
                    color = Color.White.copy(alpha = flashAlpha * 0.7f),
                    radius = (cellSizePx * 0.4f).coerceAtLeast(1f),
                    center = Offset(flashX, flashY),
                    blendMode = BlendMode.Plus
                )
            }
        }
    }
}

// --------------------------------------------------------------------------
// MAIN BOARD COMPOSABLE
// --------------------------------------------------------------------------
private val preClearTintColors = listOf(
    Color(0xFF42A5F5), // Blue
    Color(0xFF66BB6A), // Green
    Color(0xFFF06292)  // Pink (requested)
)

private fun pickTintColor(index: Int): Color {
    return preClearTintColors[index % preClearTintColors.size]
}

// kotlin
@Composable
fun AnimatedGameBoard(
    board: GameGrid,
    gridSize: Int,
    cellDp: Dp,
    ghostBlock: Block?,
    ghostOrigin: Pair<Int, Int>?,
    isGhostValid: Boolean = false,
    onCellClick: (row: Int, col: Int) -> Unit,
    uiState: GameUiState,
    effectCells: Set<Coord>,
    onClearAnimationFinished: () -> Unit,
    controller: SimpleDragController // <-- new param
) {
    Log.d("BoardRender", "Using ULTRA animation system (clearing=${effectCells.size})")

    val density = LocalDensity.current
    val cellSizePx = with(density) { cellDp.toPx() }
    val totalBoardPx = cellSizePx * gridSize
    val totalBoardSize = cellDp * gridSize

    // Match the border width used by the Box so logic compensates for the visual inset.
    val borderStrokePx = with(density) { 3.dp.toPx() }
    // visual inset to adjust for border/padding; tweak if necessary
    val visualInset = Offset(x = borderStrokePx, y = borderStrokePx)

    // Animation state for line clears
    val animationSpeed = if (uiState.isColorWipeAnimating) {
        com.betterblocks.COLOR_WIPE_ANIMATION_SPEED_MULTIPLIER
    } else 1f

    val lineClearAnimState = rememberLineClearAnimationState(
        clearingCells = uiState.clearingCells,
        isFullBoardClear = uiState.isRainbowWipeActive,
        gridSize = gridSize,
        animationSpeedMultiplier = animationSpeed,
        onAnimationComplete = onClearAnimationFinished
    )

    val previewClearLines = uiState.previewClearIndices
    val previewTintColor = pickTintColor(uiState.moveNumber)

    var boardWindowPos by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = Modifier
            .size(totalBoardSize)
            .offset(y = 10.dp)
            // capture window coords into local state during layout; apply to controller from LaunchedEffect
            .onGloballyPositioned { coords ->
                // store window pos into a local state (safe during measure)
                boardWindowPos = coords.positionInWindow()
            }
            .clip(RoundedCornerShape(8.dp))
            .background(BoardBackground)
            .border(
                width = 1.dp,
                color = Color(0xFF2196F3),
                shape = RoundedCornerShape(8.dp)
            )
    ) {

        // Defer the controller.setGridMetrics call so we don't write snapshot state during measure/layout
        LaunchedEffect(boardWindowPos, cellSizePx, totalBoardPx) {
            if (boardWindowPos != Offset.Zero) {
                controller.setGridMetrics(
                    topLeft = boardWindowPos,
                    totalGridPx = totalBoardPx,
                    cellPx = cellSizePx,
                    visualOffset = visualInset
                )
                Log.d("BoardRender", "setGridMetrics (deferred) topLeftWindow=$boardWindowPos totalBoardPx=$totalBoardPx cellSizePx=$cellSizePx visualInset=$visualInset")
            }
        }

        // ----------------------------------------------------------------------
        // LAYER 1 — GRID TILES
        // ----------------------------------------------------------------------
        LazyVerticalGrid(
            columns = GridCells.Fixed(gridSize),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(0.dp)
        ) {
            itemsIndexed(board.flatten()) { index, cellValue ->
                val r = index / gridSize
                val c = index % gridSize
                val cell = Coord(r, c)

                val isOccupied = cellValue != null
                val isClearing = lineClearAnimState.affectedCells.contains(cell)
                val animState = lineClearAnimState.cellStates[cell]

                // Ghost block tint suppressed here — handled by dedicated ghost layer.
                val ghostColor = Color.Transparent

                val isPreviewTinted = !isClearing &&
                        if (uiState.previewIsRow) (r in previewClearLines)
                        else (c in previewClearLines)

                AnimatedBoardCell(
                    cell = cell,
                    cellValue = cellValue,
                    isOccupied = isOccupied,
                    isClearing = isClearing,
                    cellAnimState = animState,
                    ghostColor = ghostColor,
                    cellDp = cellDp,
                    tintColor = previewTintColor.copy(alpha = 0.65f),
                    isPreviewTinted = isPreviewTinted,
                    onCellClick = { onCellClick(r, c) }
                )
            }
        }

        // ----------------------------------------------------------------------
        // LAYER 1.25 — CLEAN BLOCK BLAST GHOST (Outline + faint fill)
        // ----------------------------------------------------------------------
        if (ghostBlock != null && ghostOrigin != null) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val outlineColor = if (isGhostValid)
                    Color(0xFF00FFAA)     // neon teal
                else
                    Color(0xFFFF5577)     // neon red/pink

                val fillColor = outlineColor.copy(alpha = 0.08f)
                val strokeWidth = cellSizePx * 0.10f
                val corner = cellSizePx * 0.22f

                ghostBlock.shape.forEach { shapeCell ->
                    val gx = (ghostOrigin.second + shapeCell.col) * cellSizePx
                    val gy = (ghostOrigin.first + shapeCell.row) * cellSizePx

                    // Soft fill behind
                    drawRoundRect(
                        color = fillColor,
                        topLeft = Offset(gx, gy),
                        size = androidx.compose.ui.geometry.Size(cellSizePx, cellSizePx),
                        cornerRadius = CornerRadius(corner, corner)
                    )

                    // Neon outline
                    drawRoundRect(
                        color = outlineColor,
                        topLeft = Offset(gx, gy),
                        size = androidx.compose.ui.geometry.Size(cellSizePx, cellSizePx),
                        cornerRadius = CornerRadius(corner, corner),
                        style = Stroke(width = strokeWidth)
                    )
                }
            }
        }

        // ----------------------------------------------------------------------
        // LAYER 1.5 — GRID LINES
        // ----------------------------------------------------------------------
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 2.dp.toPx()
            val cellPx = cellDp.toPx()
            val boardPx = cellPx * gridSize

            for (c in 1 until gridSize) {
                drawLine(
                    color = GridLineColor,
                    start = Offset(c * cellPx, 0f),
                    end = Offset(c * cellPx, boardPx),
                    strokeWidth = strokeWidth
                )
            }
            for (r in 1 until gridSize) {
                drawLine(
                    color = GridLineColor,
                    start = Offset(0f, r * cellPx),
                    end = Offset(boardPx, r * cellPx),
                    strokeWidth = strokeWidth
                )
            }
        }

        // ----------------------------------------------------------------------
        // LAYER 1.75 — PREVIEW GLOW
        // ----------------------------------------------------------------------
        val infinite = rememberInfiniteTransition()
        val pulseAlpha by infinite.animateFloat(
            initialValue = 0.20f,
            targetValue = 0.30f,
            animationSpec = infiniteRepeatable(
                animation = tween(900, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            )
        )

        for (lineIndex in previewClearLines) {
            Canvas(Modifier.fillMaxSize()) {
                val cellPx = cellDp.toPx()
                val glowColor = previewTintColor.copy(alpha = pulseAlpha)

                if (uiState.previewIsRow) {
                    val y = lineIndex * cellPx
                    drawRect(
                        color = glowColor,
                        topLeft = Offset(0f, y),
                        size = androidx.compose.ui.geometry.Size(size.width, cellPx)
                    )
                } else {
                    val x = lineIndex * cellPx
                    drawRect(
                        color = glowColor,
                        topLeft = Offset(x, 0f),
                        size = androidx.compose.ui.geometry.Size(cellPx, size.height)
                    )
                }
            }
        }

        // ----------------------------------------------------------------------
        // LAYER 2 — SWEEP BAR
        // ----------------------------------------------------------------------
        lineClearAnimState.activeLine?.let { line ->
            if (!line.fullBoardClear) {
                SweepOverlay(
                    progress = lineClearAnimState.sweepProgress,
                    isRow = line.isRow,
                    lineIndex = line.index,
                    cellDp = cellDp,
                    gridSize = gridSize,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // ----------------------------------------------------------------------
        // LAYER 3 — RADIAL BURST
        // ----------------------------------------------------------------------
        if (lineClearAnimState.activeLine?.fullBoardClear == true) {
            RadialBurstOverlay(
                progress = lineClearAnimState.sweepProgress,
                gridSize = gridSize,
                cellDp = cellDp,
                modifier = Modifier.fillMaxSize()
            )
        }

        // ----------------------------------------------------------------------
        // LAYER 4 — PARTICLES
        // ----------------------------------------------------------------------
        ParticleRenderer(
            particles = lineClearAnimState.particles,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
fun AnimatedBoardCell(
    cell: Coord,
    cellValue: Int?,
    isOccupied: Boolean,
    isClearing: Boolean,
    cellAnimState: CellAnimationState?,
    ghostColor: Color,
    cellDp: Dp,
    tintColor: Color,
    isPreviewTinted: Boolean,
    onCellClick: () -> Unit
) {
    // Minimal, robust cell renderer to avoid crashes from TODO
    val corner = 6.dp
    val padding = 0.dp

    val bgColor = if (isOccupied) Color(0xFF0F0C14) else Color.Transparent
    val alpha = if (isClearing) 0.4f else 1f

    Box(
        modifier = Modifier
            .size(cellDp)
            .padding(padding)
            .clip(RoundedCornerShape(corner))
            .background(bgColor)
            .safeClickable { onCellClick() },
        contentAlignment = Alignment.Center
    ) {
        // Draw block texture if present (cellValue holds drawable resource id)
        if (cellValue != null) {
            Image(
                painter = painterResource(id = cellValue),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .scale(com.betterblocks.BLOCK_TEXTURE_SCALE)
                    .graphicsLayer { this.alpha = alpha }
            )
        }

        // Preview tint overlay (for previewed clear rows/cols)
        if (isPreviewTinted) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(tintColor.copy(alpha = 0.14f))
            )
        }

        // Ghost outline
        if (ghostColor != Color.Transparent) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .border(1.dp, ghostColor.copy(alpha = 0.22f), RoundedCornerShape(corner))
            )
        }
    }
}


@Composable
private fun CellTintGlowLayer(
    tintColor: Color,
    animState: CellAnimationState?,
    modifier: Modifier = Modifier
) {
    if (animState == null) return
    val baseAlpha = animState.alpha.coerceIn(0f, 1f)
    if (baseAlpha <= 0.01f) return

    Canvas(modifier = modifier) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val maxRadius = minOf(size.width, size.height) * 0.6f * (animState.scale.takeIf { it > 0f } ?: 1f)

        val radii = listOf(0.45f, 0.75f, 1.0f)
        val alphas = listOf(0.50f, 0.35f, 0.28f)

        for (i in radii.indices) {
            val r = maxRadius * radii[i]
            val a = (alphas[i] * baseAlpha).coerceIn(0f, 1f)
            drawCircle(color = tintColor.copy(alpha = a), radius = r, center = Offset(cx, cy))
        }

        // soft outer ring
        drawCircle(
            color = tintColor.copy(alpha = (0.18f * baseAlpha)),
            radius = maxRadius * 1.2f,
            center = Offset(cx, cy),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = maxRadius * 0.04f)
        )
    }
}

// --------------------------------------------------------------------------
// NEW: CellFragmentBurstLayer
// --------------------------------------------------------------------------

@Composable
private fun CellFragmentBurstLayer(
    animState: CellAnimationState?,
    tintColor: Color,
    modifier: Modifier = Modifier
) {
    if (animState == null) return
    val visibleAlpha = animState.alpha.coerceIn(0f, 1f)
    if (visibleAlpha <= 0.01f) return

    // progress derived from alpha decay (0 at start -> 1 at end)
    val prog = (1f - animState.alpha).coerceIn(0f, 1f)
    val eased = androidx.compose.animation.core.FastOutSlowInEasing.transform(prog)

    // Stable fragment specs keyed on animState.hashCode()
    val specs = remember(animState.hashCode()) {
        val rnd = java.util.Random(animState.hashCode().toLong())
        val count = 5 + rnd.nextInt(8) // 5..12
        List(count) {
            val ox = (rnd.nextFloat() - 0.5f)
            val oy = (rnd.nextFloat() - 0.5f)
            FragmentSpec(
                offsetX = ox,
                offsetY = oy,
                rot = (rnd.nextFloat() - 0.5f) * 60f,
                baseSize = 6f + rnd.nextFloat() * 10f,
                delay = rnd.nextFloat() * 0.12f,
                speed = 0.7f + rnd.nextFloat() * 1.0f
            )
        }
    }

    Canvas(modifier = modifier) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val cellMax = minOf(size.width, size.height)
        val travelBase = cellMax * 0.9f * (animState.scale.takeIf { it > 0f } ?: 1f)

        for (spec in specs) {
            val p = ((eased - spec.delay) / (1f - spec.delay)).coerceIn(0f, 1f)
            if (p <= 0f) continue

            val move = spec.speed * travelBase * p
            val dx = spec.offsetX * move
            val dy = spec.offsetY * move

            val fragScale = lerp(1f, 1.4f, p)
            val fragRot = spec.rot * p
            val fragAlpha = ((1f - p) * 0.65f * visibleAlpha).coerceIn(0f, 1f)

            val w = spec.baseSize * fragScale
            val h = spec.baseSize * fragScale * (0.7f + (kotlin.math.abs(spec.offsetX) * 0.6f))

            // Translate to fragment center, rotate, then draw fragment centered at origin
            translate(left = cx + dx, top = cy + dy) {
                rotate(degrees = fragRot) {
                    drawRoundRect(
                        color = tintColor.copy(alpha = fragAlpha),
                        topLeft = Offset(-w / 2f, -h / 2f),
                        size = androidx.compose.ui.geometry.Size(w, h),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f, 2f)
                    )
                }
            }
        }
    }
}

private data class FragmentSpec(
    val offsetX: Float,
    val offsetY: Float,
    val rot: Float,
    val baseSize: Float,
    val delay: Float,
    val speed: Float
)

private fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t
