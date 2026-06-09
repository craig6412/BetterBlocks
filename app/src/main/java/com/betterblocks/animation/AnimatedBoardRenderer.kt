package com.betterblocks.animation

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.ui.zIndex
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.betterblocks.*
import com.betterblocks.BoardBackground
import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext

import androidx.compose.foundation.shape.RoundedCornerShape
import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.sqrt
import kotlin.math.roundToInt
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.res.painterResource
import kotlin.div
import kotlin.math.cos
import kotlin.math.sin

// New imports for fallback drawable->bitmap painter
import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter

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

        // Safety guard: only render sweep glow when progress is > 0
        if (progress <= 0f) return@Canvas

        // Draw a subtle, wide outer glow behind the sweep to make it feel cinematic.
        // This glow uses a smoothed, slightly desaturated rainbow and should be constrained to the cleared band.
        try {
            val glowThickness = cellSizePx * 0.55f // confined glow: keeps a single-line clear on the cleared row/column
            if (isRow) {
                val centerY = (lineIndex + 0.5f) * cellSizePx
                val top = (centerY - glowThickness).coerceAtLeast(0f)
                val bandHeight = ((glowThickness * 2f).coerceAtMost(size.height - top)).coerceAtLeast(0f)

                // Rainbow runs along X (sweep axis); brush provided by SweepEngine is horizontal when isRow=true
                val outerBrush = SweepEngine.createSoftOuterGlowBrush(
                    sweepProgress = progress,
                    isRow = true,
                    totalPixels = totalSize,
                    cellPixelSize = cellSizePx
                )

                // Draw the rainbow band (Screen to be subtle)
                drawRect(
                    brush = outerBrush,
                    topLeft = Offset(0f, top),
                    size = androidx.compose.ui.geometry.Size(size.width, bandHeight),
                    blendMode = BlendMode.Screen,
                    alpha = 0.45f
                )

                // Perpendicular alpha feather: strongest in center, transparent at edges
                val maskBrush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0f),
                        Color.Black.copy(alpha = 1f),
                        Color.Black.copy(alpha = 0f)
                    ),
                    startY = top,
                    endY = top + bandHeight
                )
                // Use DstIn so this mask multiplies the existing rainbow band's alpha
                drawRect(
                    brush = maskBrush,
                    topLeft = Offset(0f, top),
                    size = androidx.compose.ui.geometry.Size(size.width, bandHeight),
                    blendMode = BlendMode.DstIn
                )

            } else {
                val centerX = (lineIndex + 0.5f) * cellSizePx
                val left = (centerX - glowThickness).coerceAtLeast(0f)
                val bandWidth = ((glowThickness * 2f).coerceAtMost(size.width - left)).coerceAtLeast(0f)

                // Rainbow runs along Y for columns (SweepEngine handles vertical gradients when isRow=false)
                val outerBrush = SweepEngine.createSoftOuterGlowBrush(
                    sweepProgress = progress,
                    isRow = false,
                    totalPixels = totalSize,
                    cellPixelSize = cellSizePx
                )

                drawRect(
                    brush = outerBrush,
                    topLeft = Offset(left, 0f),
                    size = androidx.compose.ui.geometry.Size(bandWidth, size.height),
                    blendMode = BlendMode.Screen,
                    alpha = 0.45f
                )

                val maskBrush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0f),
                        Color.Black.copy(alpha = 1f),
                        Color.Black.copy(alpha = 0f)
                    ),
                    startX = left,
                    endX = left + bandWidth
                )
                drawRect(
                    brush = maskBrush,
                    topLeft = Offset(left, 0f),
                    size = androidx.compose.ui.geometry.Size(bandWidth, size.height),
                    blendMode = BlendMode.DstIn
                )
            }
        } catch (_: Exception) {
            // no-op; keep stable if helper isn't available
        }

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
                radius = cellSizePx * 0.85f
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
                        Color(0xFF00B0FF).copy(alpha = 0.5f),
                        Color(0xFF00E5FF).copy(alpha = 0.6f),
                        Color(0xFFFFFFFF).copy(alpha = 0.7f)
                    ),
                    startX = trailStart - cellSizePx * 1.1f,
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
                    radius = cellSizePx * 0.75f
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
                radius = cellSizePx * 0.85f
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
                        Color(0xFF00B0FF).copy(alpha = 0.5f),
                        Color(0xFF00E5FF).copy(alpha = 0.6f),
                        Color(0xFFFFFFFF).copy(alpha = 0.7f)
                    ),
                    startY = trailStart - cellSizePx * 1.1f,
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
                    radius = cellSizePx * 0.75f
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

private data class PreviewClearLines(
    val rows: Set<Int> = emptySet(),
    val cols: Set<Int> = emptySet()
) {
    val hasAny: Boolean get() = rows.isNotEmpty() || cols.isNotEmpty()
}

private fun hueShiftPreviewColor(cellValue: Int?, fallback: Color): Color {
    if (cellValue == null || cellValue <= 0) return fallback

    val palette = listOf(
        Color(0xFFFF4FD8), // blue -> neon pink feel
        Color(0xFFB45CFF), // orange -> purple feel
        Color(0xFF5DFF8B), // purple -> green feel
        Color(0xFFFFD166), // green -> gold feel
        Color(0xFF00E5FF), // red/pink -> cyan feel
        Color(0xFFFF6B6B)  // cyan/yellow -> coral feel
    )

    val index = kotlin.math.abs(cellValue) % palette.size
    return palette[index]
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

    // (Removed) border-derived visual inset. The grid content fills from (0,0);
    // the board border is drawn over it and does not shift the grid, so there is
    // no inset to compensate for in the placement math.

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

    // Always derive preview lines from the ghost placement so preview appears while dragging.
    // Return BOTH rows and columns so cross-clears preview correctly before the player drops the block.
    val previewClearLines = remember(board, ghostBlock, ghostOrigin) {
        computePreviewClearLinesFromGhost(
            board = board,
            gridSize = gridSize,
            ghostBlock = ghostBlock,
            ghostOrigin = ghostOrigin
        )
    }
    val previewTintColor = pickTintColor(uiState.moveNumber)

    val infinite = rememberInfiniteTransition()
    val pulseAlpha by infinite.animateFloat(
        initialValue = 0.20f,
        targetValue = 0.30f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )


    var boardWindowPos by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = Modifier
            .size(totalBoardSize)
            .offset(y = 10.dp)
            // capture ROOT coords into local state during layout; apply to controller from LaunchedEffect.
            // Root space is used because the finger position is delivered in root space (fingerPosRoot).
            .onGloballyPositioned { coords ->
                boardWindowPos = coords.positionInRoot()
                // Diagnostic: root vs window differ by the window top inset (status bar / cutout).
                // If finger lands ~one cell off in Y, compare these two in Logcat.
                Log.d(
                    "BoardRender",
                    "boardOrigin root=${coords.positionInRoot()} window=${coords.positionInWindow()}"
                )
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
                    // The LazyVerticalGrid fills from (0,0) of this Box; the 1.dp border is
                    // drawn on top and does NOT inset content. So there is no visual offset.
                    visualOffset = Offset.Zero
                )
                Log.d("BoardRender", "setGridMetrics (deferred) topLeftRoot=$boardWindowPos totalBoardPx=$totalBoardPx cellSizePx=$cellSizePx visualOffset=ZERO")
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

                val isPreviewLineCell = (r in previewClearLines.rows) || (c in previewClearLines.cols)
                // Option 1 drag truth: do not paint the dragged block into fixed grid cells here.
                // The live dragged piece is rendered once in the soft magnetic preview layer below.
                val isPreviewGhostCell = false
                val isPreviewTinted = isGhostValid && isPreviewLineCell

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
                    isPreviewGhostCell = isPreviewGhostCell,
                    previewPulseAlpha = pulseAlpha,
                    previewDrawableResId = previewDrawableForCell(
                        row = r,
                        col = c,
                        moveNumber = uiState.moveNumber,
                        existingCellValue = cellValue,
                        previewRows = previewClearLines.rows,
                        previewCols = previewClearLines.cols
                    ),
                    onCellClick = { onCellClick(r, c) }
                )
            }
        }

        // ----------------------------------------------------------------------
        // LAYER 1.25 — (removed) board-local snapped preview piece
        // ----------------------------------------------------------------------
        // The dragged piece is now drawn ONCE as a single free-floating overlay in
        // GameScreen, in root coordinates, so the visual always matches the snap
        // candidate. ghostBlock/ghostOrigin are still consumed above ONLY to drive
        // the cell-confined line-clear tint preview (computePreviewClearLinesFromGhost),
        // never to draw the piece. This removes the stiff cell-locked second preview.

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
        // LAYER 1.75 — PREVIEW LINE BACKDROP
        // ----------------------------------------------------------------------
        // Intentionally empty. The pre-clear preview is now cell-confined only:
        // each affected cell renders with a solid block drawable preview color.
        // This avoids full-row/column beams and glow bleed into neighboring cells.

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
    isPreviewGhostCell: Boolean = false,
    previewPulseAlpha: Float = 0f,
    previewDrawableResId: Int? = null,
    onCellClick: () -> Unit
) {
    // Minimal, robust cell renderer to avoid crashes from TODO
    val corner = 6.dp
    val padding = 0.dp

    val bgColor = if (isOccupied) Color(0xFF0F0C14) else Color.Transparent

    // During a clear, the CellAnimationEngine now hits cells left-to-right.
    // Use that per-cell state to make the actual block texture shrink/fade as it explodes.
    val clearAlpha = if (isClearing) {
        (cellAnimState?.alpha ?: 1f).coerceIn(0f, 1f)
    } else {
        1f
    }
    val clearScale = if (isClearing) {
        (cellAnimState?.scale ?: 1f).coerceIn(0.72f, 1.18f)
    } else {
        1f
    }

    Box(
        modifier = Modifier
            .size(cellDp)
            .padding(padding)
            .clip(RoundedCornerShape(corner))
            .background(bgColor)
            .safeClickable { onCellClick() },
        contentAlignment = Alignment.Center
    ) {

        val displayCellValue = if ((isPreviewTinted || isPreviewGhostCell) && previewDrawableResId != null) {
            previewDrawableResId
        } else {
            cellValue
        }

        if (displayCellValue != null && displayCellValue > 0) {
            val context = LocalContext.current
            val resType = remember(displayCellValue) {
                try {
                    context.resources.getResourceTypeName(displayCellValue)
                } catch (_: Exception) {
                    null
                }
            }

            if (resType == "drawable") {
                // Allow only raster or vector images; if painterResource fails (unsupported XML types), fall back to Drawable->Bitmap
                val painterFallback = remember(displayCellValue) {
                    // Attempt to load a Drawable and convert to Bitmap for compose when needed
                    try {
                        val dr: Drawable? = AppCompatResources.getDrawable(context, displayCellValue)
                        if (dr == null) return@remember null

                        // If it's already a BitmapDrawable use its bitmap else render to bitmap
                        when (dr) {
                            is BitmapDrawable -> dr.bitmap
                            else -> drawableToBitmap(dr)
                        }
                    } catch (t: Throwable) {
                        Log.w("BoardCell", "drawable fallback failed for id=$displayCellValue", t)
                        null
                    }
                }

                // First try painterResource; it will throw for unsupported XML drawable types
                // var painter: Painter? = null
                // try {
                //     painter = painterResource(id = cellValue)
                // } catch (iae: IllegalArgumentException) {
                //     // fallback to bitmap painter if we were able to produce one
                //     painter = painterFallback?.let { bmp -> BitmapPainter(bmp.asImageBitmap()) }
                //     Log.w("BoardCell", "painterResource unsupported for id=$displayCellValue -> falling back to BitmapPainter")
                // }

                // Compose doesn't allow try/catch around composable invocations like painterResource.
                // Instead, load the Drawable via AppCompatResources (non-composable) and rasterize it to a BitmapPainter.
                val painter: Painter? = painterFallback?.let { bmp -> BitmapPainter(bmp.asImageBitmap()) }

                if (painter != null) {
                    Image(
                        painter = painter,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .scale(BLOCK_TEXTURE_SCALE * clearScale)
                            .alpha(clearAlpha)
                    )
                } else {
                    Log.e(
                        "BoardCellError",
                        "Invalid drawable used as block texture: ${runCatching {
                            context.resources.getResourceEntryName(displayCellValue)
                        }.getOrNull()}"
                    )
                }
            } else {
                Log.e(
                    "BoardCellError",
                    "Invalid drawable used as block texture: ${runCatching {
                        context.resources.getResourceEntryName(displayCellValue)
                    }.getOrNull()}"
                )
            }
        }

        if (isClearing && cellAnimState != null) {
            CellTintGlowLayer(
                tintColor = tintColor,
                animState = cellAnimState,
                modifier = Modifier.matchParentSize()
            )
            CellFragmentBurstLayer(
                animState = cellAnimState,
                tintColor = tintColor,
                modifier = Modifier.matchParentSize()
            )
        }



        // Cell-confined pre-clear preview accent.
        // The preview color comes from one shared block drawable per clearing line.
        // This adds a small pulsing glow without drawing full-row/column beams.
        if (isPreviewTinted || isPreviewGhostCell) {
            val pulseBoost = ((previewPulseAlpha - 0.20f) / 0.10f).coerceIn(0f, 1f)
            val accentColor = hueShiftPreviewColor(previewDrawableResId, tintColor)
            val glowAlpha = 0.12f + pulseBoost * 0.10f
            val borderAlpha = 0.30f + pulseBoost * 0.18f

            Canvas(modifier = Modifier.matchParentSize()) {
                val cornerPx = size.minDimension * 0.16f

                // Small pulsing glow, clipped to this cell only.
                drawRoundRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            accentColor.copy(alpha = glowAlpha),
                            accentColor.copy(alpha = glowAlpha * 0.55f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.5f, size.height * 0.5f),
                        radius = size.minDimension * 0.58f
                    ),
                    topLeft = Offset.Zero,
                    size = size,
                    cornerRadius = CornerRadius(cornerPx, cornerPx),
                    blendMode = BlendMode.Screen
                )

                // Tight border for the exact cells that will clear.
                drawRoundRect(
                    color = Color.White.copy(alpha = borderAlpha),
                    topLeft = Offset.Zero,
                    size = size,
                    cornerRadius = CornerRadius(cornerPx, cornerPx),
                    style = Stroke(width = size.minDimension * 0.024f),
                    blendMode = BlendMode.Screen
                )
            }
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
        val maxRadius = minOf(size.width, size.height) * 0.42f * (animState.scale.takeIf { it > 0f } ?: 1f)

        val radii = listOf(0.45f, 0.75f, 1.0f)
        val alphas = listOf(0.45f, 0.28f, 0.18f)

        for (i in radii.indices) {
            val r = maxRadius * radii[i]
            val a = (alphas[i] * baseAlpha).coerceIn(0f, 1f)
            drawCircle(color = tintColor.copy(alpha = a), radius = r, center = Offset(cx, cy))
        }

        // soft outer ring
        drawCircle(
            color = tintColor.copy(alpha = (0.18f * baseAlpha)),
            radius = maxRadius * 0.95f,
            center = Offset(cx, cy),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = maxRadius * 0.04f)
        )
    }
}

// Helper: convert any Drawable into a Bitmap (uses intrinsic size when available, falls back to 1x1)
private fun drawableToBitmap(drawable: Drawable): Bitmap {
    val width = (drawable.intrinsicWidth.takeIf { it > 0 } ?: 1)
    val height = (drawable.intrinsicHeight.takeIf { it > 0 } ?: 1)
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = AndroidCanvas(bitmap)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)
    return bitmap
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
        val count = 8 + rnd.nextInt(7) // 8..14 shards: richer but still cheap
        List(count) {
            // Bias shards forward so each cell feels like it pops in the sweep direction.
            val ox = 0.20f + rnd.nextFloat() * 0.95f
            val oy = (rnd.nextFloat() - 0.5f) * 0.85f
            FragmentSpec(
                offsetX = ox,
                offsetY = oy,
                rot = (rnd.nextFloat() - 0.5f) * 110f,
                baseSize = 4f + rnd.nextFloat() * 8f,
                delay = rnd.nextFloat() * 0.08f,
                speed = 0.65f + rnd.nextFloat() * 0.85f
            )
        }
    }

    Canvas(modifier = modifier) {
        clipRect(left = 0f, top = 0f, right = size.width, bottom = size.height) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val cellMax = minOf(size.width, size.height)
            val travelBase = cellMax * 0.62f * (animState.scale.takeIf { it > 0f } ?: 1f)

            for (spec in specs) {
                val p = ((eased - spec.delay) / (1f - spec.delay)).coerceIn(0f, 1f)
                if (p <= 0f) continue

                val move = spec.speed * travelBase * p
                val dx = spec.offsetX * move
                val dy = spec.offsetY * move

                val fragScale = lerp(1f, 1.4f, p)
                val fragRot = spec.rot * p
                val fragAlpha = ((1f - p) * 0.82f * visibleAlpha).coerceIn(0f, 1f)

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




private fun isGhostCell(
    row: Int,
    col: Int,
    ghostBlock: Block?,
    ghostOrigin: Pair<Int, Int>?
): Boolean {
    if (ghostBlock == null || ghostOrigin == null) return false

    for (shapeCell in ghostBlock.shape) {
        val r = ghostOrigin.first + shapeCell.row
        val c = ghostOrigin.second + shapeCell.col
        if (row == r && col == c) return true
    }

    return false
}

private fun previewDrawableForLine(
    lineIndex: Int,
    isRow: Boolean,
    moveNumber: Int
): Int? {
    val choices = COLOR_WIPE_DRAWABLES
    if (choices.isEmpty()) return null

    // Stable line-level random color. Every cell in the same row/column gets the same drawable.
    val seed = if (isRow) {
        (moveNumber * 37) + (lineIndex * 101) + 19
    } else {
        (moveNumber * 41) + (lineIndex * 103) + 29
    }

    return choices[kotlin.math.abs(seed) % choices.size]
}

private fun previewDrawableForCell(
    row: Int,
    col: Int,
    moveNumber: Int,
    existingCellValue: Int?,
    previewRows: Set<Int>,
    previewCols: Set<Int>
): Int? {
    // Row wins at intersections so a horizontal clear reads as one uninterrupted color.
    val rowDrawable = if (row in previewRows) {
        previewDrawableForLine(row, isRow = true, moveNumber = moveNumber)
    } else {
        null
    }

    val colDrawable = if (col in previewCols) {
        previewDrawableForLine(col, isRow = false, moveNumber = moveNumber)
    } else {
        null
    }

    val picked = rowDrawable ?: colDrawable ?: existingCellValue

    // Do not force a different color per cell. Same-line consistency is more important
    // than avoiding a same-color match on one existing block.
    return picked
}

private fun computePreviewClearLinesFromGhost(
    board: GameGrid,
    gridSize: Int,
    ghostBlock: Block?,
    ghostOrigin: Pair<Int, Int>?
): PreviewClearLines {
    if (ghostBlock == null || ghostOrigin == null) return PreviewClearLines()

    val flat = board.flatten()

    // Snapshot current board into a nullable Int grid.
    val snapshot = Array(gridSize) { r ->
        MutableList<Int?>(gridSize) { c ->
            flat.getOrNull(r * gridSize + c)
        }
    }

    // Only preview a line clear if the held block could actually be dropped there.
    // This prevents invalid/overlapping ghost positions from lighting up rows or columns.
    for (shapeCell in ghostBlock.shape) {
        val r = ghostOrigin.first + shapeCell.row
        val c = ghostOrigin.second + shapeCell.col

        if (r !in 0 until gridSize || c !in 0 until gridSize) {
            return PreviewClearLines()
        }

        if (snapshot[r][c] != null) {
            return PreviewClearLines()
        }
    }

    // Stamp ghost block only after the whole placement is known valid.
    for (shapeCell in ghostBlock.shape) {
        val r = ghostOrigin.first + shapeCell.row
        val c = ghostOrigin.second + shapeCell.col
        snapshot[r][c] = -1
    }

    val rows = mutableSetOf<Int>()
    val cols = mutableSetOf<Int>()

    for (r in 0 until gridSize) {
        if (snapshot[r].all { it != null }) rows.add(r)
    }

    for (c in 0 until gridSize) {
        var full = true
        for (r in 0 until gridSize) {
            if (snapshot[r][c] == null) {
                full = false
                break
            }
        }
        if (full) cols.add(c)
    }

    return PreviewClearLines(rows = rows, cols = cols)
}

