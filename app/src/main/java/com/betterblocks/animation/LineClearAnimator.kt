package com.betterblocks.animation

import android.util.Log
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import com.betterblocks.Coord
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.*
import kotlin.random.Random

// -------------------------
// Core animation state models
// -------------------------

@Immutable
data class LineClearAnimationState(
    val sweepProgress: Float = 0f,
    val activeLine: LineInfo? = null,
    val affectedCells: Set<Coord> = emptySet(),
    val cellStates: Map<Coord, CellAnimationState> = emptyMap(),
    val particles: List<Particle> = emptyList()
)

@Immutable
data class LineInfo(
    val isRow: Boolean,
    val index: Int,
    val cells: List<Coord>,
    val fullBoardClear: Boolean = false
)

@Immutable
data class CellAnimationState(
    val scale: Float = 1f,
    val alpha: Float = 1f,
    val rotation: Float = 0f,
    val flashWhite: Float = 0f,
    val innerBloom: Float = 0f,
    val outerBloom: Float = 0f,
    val additiveGlow: Float = 0f,
    val rainbowTint: Color = Color.White,
    val sweepHit: Boolean = false
)

@Immutable
data class Particle(
    val position: Offset,
    val velocity: Offset,
    val color: Color,
    val size: Float,
    val alpha: Float,
    val lifetime: Float,
    val maxLifetime: Float,
    val rotation: Float,
    val angularVelocity: Float
)

// -------------------------
// SweepEngine
// -------------------------

object SweepEngine {
    val BlockBlastSweepEasing = CubicBezierEasing(0.05f, 0.90f, 0.25f, 1.00f)
    const val CELL_DP_BASE = 38f

    val RAINBOW_COLORS = listOf(
        Color(0xFFFF0000),
        Color(0xFFFF7F00),
        Color(0xFFFFFF00),
        Color(0xFF00FF00),
        Color(0xFF0000FF),
        Color(0xFF8B00FF)
    )

    fun rainbowColorAt(progress: Float): Color {
        val index = ((progress * RAINBOW_COLORS.size) % RAINBOW_COLORS.size)
        val i0 = index.toInt()
        val frac = index - i0
        val c1 = RAINBOW_COLORS[i0]
        val c2 = RAINBOW_COLORS[(i0 + 1) % RAINBOW_COLORS.size]
        return Color(
            red = c1.red + (c2.red - c1.red) * frac,
            green = c1.green + (c2.green - c1.green) * frac,
            blue = c1.blue + (c2.blue - c1.blue) * frac,
            alpha = 1f
        )
    }

    // New helper: produce a smoothed gradient by sampling rainbowColorAt across a small window
    fun smoothRainbowSamples(centerProgress: Float, spread: Float = 0.25f, samples: Int = 9, desaturate: Float = 0f): List<Color> {
        if (samples <= 1) return listOf(rainbowColorAt(centerProgress))
        val out = mutableListOf<Color>()
        val step = (spread * 2f) / (samples - 1)
        for (i in 0 until samples) {
            val p = (centerProgress - spread) + i * step
            // wrap progress to 0..1
            var wrapped = p
            while (wrapped < 0f) wrapped += 1f
            while (wrapped > 1f) wrapped -= 1f
            val c = rainbowColorAt(wrapped)
            out += if (desaturate > 0f) desaturateColor(c, desaturate) else c
        }
        return out
    }

    // Simple desaturation: lerp toward a neutral gray and slightly reduce saturation by mixing toward average
    private fun desaturateColor(c: Color, amount: Float): Color {
        val gray = (c.red + c.green + c.blue) / 3f
        val r = c.red + (gray - c.red) * amount
        val g = c.green + (gray - c.green) * amount
        val b = c.blue + (gray - c.blue) * amount
        // Slightly bias away from pure extremes to avoid neon spikes
        val blend = 0.92f
        return Color(
            red = (r * blend + 0.08f * 0.5f).coerceIn(0f, 1f),
            green = (g * blend + 0.08f * 0.5f).coerceIn(0f, 1f),
            blue = (b * blend + 0.08f * 0.5f).coerceIn(0f, 1f),
            alpha = c.alpha
        )
    }

    fun createSweepBrush(
        sweepProgress: Float,
        isRow: Boolean,
        totalPixels: Float
    ): Brush {
        val sweepLength = totalPixels * 0.33f
        return if (isRow) {
            Brush.linearGradient(
                colors = RAINBOW_COLORS,
                start = Offset(totalPixels * sweepProgress - sweepLength, 0f),
                end = Offset(totalPixels * sweepProgress + sweepLength, 0f)
            )
        } else {
            Brush.linearGradient(
                colors = RAINBOW_COLORS,
                start = Offset(0f, totalPixels * sweepProgress - sweepLength),
                end = Offset(0f, totalPixels * sweepProgress + sweepLength)
            )
        }
    }

    // New: create a soft, wide outer glow brush for the sweep that samples the rainbow smoothly
    fun createSoftOuterGlowBrush(
        sweepProgress: Float,
        isRow: Boolean,
        totalPixels: Float,
        cellPixelSize: Float
    ): Brush {
        // Make glow extend well beyond the board edges and be significantly wider than the sweep
        val glowExtent = cellPixelSize * 4f + totalPixels * 0.08f
        val centerPx = sweepProgress * totalPixels
        val startPx = centerPx - glowExtent - totalPixels * 0.25f
        val endPx = centerPx + glowExtent + totalPixels * 0.25f

        // Sample a set of colors around the sweep center to produce a smooth, blended gradient
        // Use slightly stronger desaturation and slightly fewer samples to avoid neon spikes
        val samples = smoothRainbowSamples(sweepProgress, spread = 0.4f, samples = 9, desaturate = 0.5f)
        return if (isRow) {
            Brush.linearGradient(
                colors = samples,
                start = Offset(startPx, 0f),
                end = Offset(endPx, 0f)
            )
        } else {
            Brush.linearGradient(
                colors = samples,
                start = Offset(0f, startPx),
                end = Offset(0f, endPx)
            )
        }
    }

    fun isCellHit(
        cell: Coord,
        sweepProgress: Float,
        isRow: Boolean,
        cellPixelSize: Float,
        gridSize: Int
    ): Boolean {
        val cellCenterPx = if (isRow) {
            (cell.col + 0.5f) * cellPixelSize
        } else {
            (cell.row + 0.5f) * cellPixelSize
        }
        val sweepPositionPx = sweepProgress * (gridSize * cellPixelSize)
        return cellCenterPx <= sweepPositionPx
    }

    fun isHitRadial(
        cell: Coord,
        sweepProgress: Float,
        gridSize: Int
    ): Boolean {
        val cx = gridSize / 2f
        val cy = gridSize / 2f
        val dx = (cell.col - cx)
        val dy = (cell.row - cy)
        val dist = sqrt(dx * dx + dy * dy)
        val maxDist = sqrt((cx * cx) + (cy * cy))
        val normalized = dist / maxDist
        return normalized <= sweepProgress
    }

    fun tintPositionForCell(cell: Coord, isRow: Boolean, gridSize: Int): Float {
        return if (isRow) cell.col.toFloat() / gridSize.toFloat() else cell.row.toFloat() / gridSize.toFloat()
    }
}

// -------------------------
// UltraGlowEngine
// -------------------------

object UltraGlowEngine {
    internal const val BASE_TILE_SIZE = 38f
    internal const val BLOOM_INNER_SCALE = 0.40f // As per request
    internal const val BLOOM_OUTER_SCALE = 0.95f // confined: prevents cleared cells from blooming into neighbors

    fun flashWhite(timeSinceHit: Float): Float {
        return when {
            timeSinceHit < 0.05f -> 1.0f // Intense flash at the start
            timeSinceHit < 0.15f -> 1f - ((timeSinceHit - 0.05f) * 10f) // Rapid decay
            else -> 0f
        }
    }

    fun innerBloom(timeSinceHit: Float): Float {
        if (timeSinceHit < 0f) return 0f
        val decay = max(0f, 1f - (timeSinceHit * 4f))
        return decay * decay * 1.2f
    }

    fun outerBloom(timeSinceHit: Float): Float {
        if (timeSinceHit < 0.05f) return 0.8f
        val t = max(0f, 1f - ((timeSinceHit - 0.05f) * 2.2f))
        return t * 0.9f
    }

    fun additiveGlow(timeSinceHit: Float): Float {
        val t = max(0f, 1f - (timeSinceHit * 3.5f))
        return t * t * 0.75f
    }

    fun tileScale(timeSinceHit: Float): Float {
        return when {
            timeSinceHit < 0.06f -> 1f + (timeSinceHit * 3f)
            timeSinceHit < 0.12f -> 1.18f - ((timeSinceHit - 0.06f) * 1.5f)
            timeSinceHit < 0.30f -> 1.07f - ((timeSinceHit - 0.12f) * 1.0f)
            else -> 0.85f - ((timeSinceHit - 0.30f) * 2f)
        }.coerceIn(0f, 1.18f)
    }

    fun alphaFade(timeSinceHit: Float): Float {
        if (timeSinceHit < 0.1f) return 1f
        val f = max(0f, 1f - (timeSinceHit * 2.2f))
        return f * f * f
    }

    fun tileRotation(timeSinceHit: Float): Float {
        val t = min(1f, timeSinceHit * 2f)
        return t * 25f
    }
}

// -------------------------
// ParticleEngine
// -------------------------

object ParticleEngine {
    private const val BASE_TILE_SIZE = 38f
    private const val PARTICLE_LIFETIME_MS = 550f

    fun generateBurst(
        centerX: Float,
        centerY: Float,
        cellPixelSize: Float,
        tintColor: Color,
        isRowClear: Boolean
    ): List<Particle> {
        val particles = mutableListOf<Particle>()

        // Short, premium-looking shard burst. No image resources needed:
        // tiny glowing rectangles travel with the same left-to-right / top-to-bottom sweep.
        val particleCount = (8 + (cellPixelSize / 12f)).toInt().coerceIn(8, 14)

        for (i in 0 until particleCount) {
            val forward = cellPixelSize * (1.25f + Random.nextFloat() * 1.35f)
            val sideways = (Random.nextFloat() - 0.5f) * cellPixelSize * 0.62f

            val vx = if (isRowClear) {
                forward
            } else {
                sideways
            }

            val vy = if (isRowClear) {
                sideways
            } else {
                forward
            }

            particles.add(
                Particle(
                    position = Offset(centerX, centerY),
                    velocity = Offset(vx, vy),
                    color = tintColor,
                    size = cellPixelSize * (0.045f + Random.nextFloat() * 0.075f),
                    alpha = 1f,
                    lifetime = 0f,
                    maxLifetime = PARTICLE_LIFETIME_MS,
                    rotation = Random.nextFloat() * 360f,
                    angularVelocity = (Random.nextFloat() - 0.5f) * 220f
                )
            )
        }

        return particles
    }

    fun updateParticles(
        particles: List<Particle>,
        deltaMs: Float
    ): List<Particle> {
        if (particles.isEmpty()) return emptyList()
        val dt = deltaMs / 1000f
        val gravity = BASE_TILE_SIZE * 2.5f * dt
        val drag = 0.90f
        return particles.mapNotNull { p ->
            val newLifetime = p.lifetime + deltaMs
            if (newLifetime >= p.maxLifetime) return@mapNotNull null
            val vx = p.velocity.x * drag
            val vy = (p.velocity.y * drag) + gravity
            val newRotation = p.rotation + p.angularVelocity * dt
             val newPos = Offset(
                 p.position.x + vx * dt,
                 p.position.y + vy * dt
             )
            val fadeProgress = newLifetime / p.maxLifetime
            val newAlpha = 1f - (fadeProgress * fadeProgress * fadeProgress)
            val shiftedColor = Color(
                red = (p.color.red + 0.1f).coerceIn(0f, 1f),
                green = (p.color.green + 0.05f).coerceIn(0f, 1f),
                blue = p.color.blue,
                alpha = 1f
            )
            p.copy(
                position = newPos,
                velocity = Offset(vx, vy),
                alpha = newAlpha,
                lifetime = newLifetime,
                color = shiftedColor,
                rotation = newRotation
             )
         }
     }
}

// -------------------------
// CellAnimationEngine
// -------------------------

object CellAnimationEngine {
    fun computeCellState(
        cell: Coord,
        sweepProgress: Float,
        lineInfo: LineInfo,
        cellPixelSize: Float,
        gridSize: Int
    ): CellAnimationState {
        // Propagation Delay
        // Single-line clears now travel like a premium block-blast: left -> right for rows,
        // top -> bottom for columns. Full-board/rainbow clears keep a soft radial feel.
        val order = if (lineInfo.fullBoardClear) {
            val center = (gridSize - 1) / 2f
            abs(cell.col - center) + abs(cell.row - center)
        } else if (lineInfo.isRow) {
            cell.col.toFloat()
        } else {
            cell.row.toFloat()
        }
        val delay = order * 0.045f
        val delayedProgress = (sweepProgress - delay).coerceIn(0f, 1f)

        if (delayedProgress <= 0f) return CellAnimationState()

        val timeSinceHit = delayedProgress // Use delayed progress for timing
        val tintColor = SweepEngine.rainbowColorAt(delayedProgress)

        return CellAnimationState(
            scale = UltraGlowEngine.tileScale(timeSinceHit),
            alpha = UltraGlowEngine.alphaFade(timeSinceHit),
            rotation = UltraGlowEngine.tileRotation(timeSinceHit),
            flashWhite = UltraGlowEngine.flashWhite(timeSinceHit),
            innerBloom = UltraGlowEngine.innerBloom(timeSinceHit),
            outerBloom = UltraGlowEngine.outerBloom(timeSinceHit),
            additiveGlow = UltraGlowEngine.additiveGlow(timeSinceHit),
            rainbowTint = tintColor,
            sweepHit = delayedProgress > 0
         )
     }
}

// -------------------------
// Easing extension
// -------------------------

private fun CubicBezierEasing.asEasing(): Easing =
    Easing { fraction -> this.transform(fraction) }

// -------------------------
// LineClearAnimator class
// -------------------------

@Stable
class LineClearAnimator {
    companion object {
        const val SWEEP_DURATION = 900 // Doubled duration for slower, more dramatic animation
        const val PARTICLE_FADE_DURATION = 1100 // Doubled to match slower sweep
        const val PARTICLE_FRAME_MS = 16
        const val CELL_PIXEL_SIZE = 38f * 3f
    }

    suspend fun runAnimation(
        clearingCells: Set<Coord>,
        isFullBoardClear: Boolean,
        gridSize: Int,
        animationSpeedMultiplier: Float = 1.0f,  // NEW: Speed control (2.0 = twice as slow)
        updateState: (LineClearAnimationState) -> Unit
    ) {
        if (clearingCells.isEmpty()) {
            updateState(LineClearAnimationState())
            return
        }

        // Snapshot the input set so we don't observe mutations while animating
        val targetCells = clearingCells.toSet()

        // Determine line info based on the snapshot; if it's not a single row/col,
        // treat it as a radial/fullBoard clear (prevents misplaced single-line sweeps)
        val lineInfo = determineLineInfo(targetCells, isFullBoardClear)
        val particles = mutableListOf<Particle>()
        val sweep = Animatable(0f)

        // Apply speed multiplier to duration (higher multiplier = slower animation)
        val adjustedSweepDuration = (SWEEP_DURATION * animationSpeedMultiplier).toInt()

        sweep.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = adjustedSweepDuration,
                easing = SweepEngine.BlockBlastSweepEasing.asEasing()
            )
        ) {
            val progress = value
            val cellStates = mutableMapOf<Coord, CellAnimationState>()

            targetCells.forEach { cell ->
                val st = CellAnimationEngine.computeCellState(
                    cell = cell,
                    sweepProgress = progress,
                    lineInfo = lineInfo,
                    cellPixelSize = CELL_PIXEL_SIZE,
                    gridSize = gridSize
                )
                cellStates[cell] = st
                if (st.flashWhite > 0.95f) {
                    val cx = (cell.col + 0.5f) * CELL_PIXEL_SIZE
                    val cy = (cell.row + 0.5f) * CELL_PIXEL_SIZE
                    particles += ParticleEngine.generateBurst(
                        cx,
                        cy,
                        CELL_PIXEL_SIZE,
                        st.rainbowTint,
                        lineInfo.isRow
                    )
                }
            }

            val updatedParticles = ParticleEngine.updateParticles(
                particles = particles,
                deltaMs = SWEEP_DURATION.toFloat() / 60f
            )
            particles.clear()
            particles += updatedParticles

            updateState(
                LineClearAnimationState(
                    sweepProgress = progress,
                    activeLine = lineInfo,
                    affectedCells = targetCells,
                    cellStates = cellStates,
                    particles = particles.toList()
                )
            )
        }

        // This now correctly waits for particles to fade out
        fadeOutParticles(particles, lineInfo, targetCells, updateState)
    }


    @Composable
    fun SweepGlowLayer(
        state: LineClearAnimationState,
        cellPixelSize: Float,
        gridSize: Int,
        modifier: Modifier = Modifier.fillMaxSize()
    ) {
        val line = state.activeLine ?: return
        if (state.sweepProgress <= 0f) return

        Canvas(modifier) {
            val totalSizePx = gridSize * cellPixelSize
            val sweepPos = state.sweepProgress * totalSizePx

            // Confined glow: stay close to the cleared row/column
            val glowThickness = cellPixelSize * 0.65f

            // Compute band bounds so the glow is constrained to the cleared row/column
            if (line.isRow) {
                val centerY = line.index * cellPixelSize + cellPixelSize / 2f
                val top = (centerY - glowThickness).coerceAtLeast(0f)
                val bandHeight = (glowThickness * 2f).coerceAtMost(size.height - top)

                // Outer soft glow (smoothed rainbow, desaturated)
                try {
                    val outerBrush = SweepEngine.createSoftOuterGlowBrush(
                        sweepProgress = state.sweepProgress,
                        isRow = true,
                        totalPixels = totalSizePx,
                        cellPixelSize = cellPixelSize
                    )
                    drawRect(
                        brush = outerBrush,
                        topLeft = Offset(0f, top),
                        size = androidx.compose.ui.geometry.Size(size.width, bandHeight),
                        blendMode = BlendMode.Screen,
                        alpha = 0.45f
                    )
                } catch (_: Exception) {
                }

                // Inner brighter sweep band (narrower) — keep additive feel
                val innerBrush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.White.copy(alpha = 0.16f),
                        Color.Transparent
                    ),
                    startY = sweepPos - glowThickness,
                    endY = sweepPos + glowThickness
                )
                drawRect(
                    brush = innerBrush,
                    topLeft = Offset(0f, top),
                    size = androidx.compose.ui.geometry.Size(size.width, bandHeight),
                    blendMode = BlendMode.Plus
                )

            } else {
                val centerX = line.index * cellPixelSize + cellPixelSize / 2f
                val left = (centerX - glowThickness).coerceAtLeast(0f)
                val bandWidth = (glowThickness * 2f).coerceAtMost(size.width - left)

                // Outer soft glow (smoothed rainbow, desaturated)
                try {
                    val outerBrush = SweepEngine.createSoftOuterGlowBrush(
                        sweepProgress = state.sweepProgress,
                        isRow = false,
                        totalPixels = totalSizePx,
                        cellPixelSize = cellPixelSize
                    )
                    drawRect(
                        brush = outerBrush,
                        topLeft = Offset(left, 0f),
                        size = androidx.compose.ui.geometry.Size(bandWidth, size.height),
                        blendMode = BlendMode.Screen,
                        alpha = 0.45f
                    )
                } catch (_: Exception) {
                }

                // Inner brighter sweep band (narrower) — keep additive feel
                val innerBrush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.White.copy(alpha = 0.16f),
                        Color.Transparent
                    ),
                    startX = sweepPos - glowThickness,
                    endX = sweepPos + glowThickness
                )
                drawRect(
                    brush = innerBrush,
                    topLeft = Offset(left, 0f),
                    size = androidx.compose.ui.geometry.Size(bandWidth, size.height),
                    blendMode = BlendMode.Plus
                )
            }
        }
    }
    private suspend fun fadeOutParticles(
        particles: MutableList<Particle>,
        lineInfo: LineInfo,
        clearingCells: Set<Coord>,
        updateState: (LineClearAnimationState) -> Unit
    ) {
        var elapsed = 0f
        // Use the dedicated particle fade duration
        while (elapsed < PARTICLE_FADE_DURATION && particles.isNotEmpty()) {
            val updated = ParticleEngine.updateParticles(particles, PARTICLE_FRAME_MS.toFloat())
            particles.clear()
            particles += updated
            // During fadeout, cellStates are empty but particles are still live
            updateState(
                LineClearAnimationState(
                    sweepProgress = 1f,
                    activeLine = lineInfo,
                    affectedCells = clearingCells,
                    cellStates = emptyMap(),
                    particles = particles.toList()
                )
            )
            elapsed += PARTICLE_FRAME_MS
            delay(PARTICLE_FRAME_MS.toLong())
        }
        // Final state reset
        updateState(LineClearAnimationState())
    }

    private fun determineLineInfo(cells: Set<Coord>, radial: Boolean): LineInfo {
        if (radial) {
            return LineInfo(
                isRow = true,
                index = 0,
                cells = cells.toList(),
                fullBoardClear = true
            )
        }

        // If the set isn't a single row or single column, treat it as a radial/full-board clear.
        // This covers actions like the rainbow wipe which selects arbitrary scattered cells.
        val first = cells.first()
        val allSameRow = cells.all { it.row == first.row }
        val allSameCol = cells.all { it.col == first.col }
        return when {
            allSameRow -> LineInfo(isRow = true, index = first.row, cells = cells.toList(), fullBoardClear = false)
            allSameCol -> LineInfo(isRow = false, index = first.col, cells = cells.toList(), fullBoardClear = false)
            else -> LineInfo(isRow = true, index = 0, cells = cells.toList(), fullBoardClear = true)
        }
    }
}

// -------------------------
// rememberLineClearAnimationState (top-level)
// -------------------------

@Composable
fun rememberLineClearAnimationState(
    clearingCells: Set<Coord>,
    isFullBoardClear: Boolean,
    gridSize: Int,
    animationSpeedMultiplier: Float,
    onAnimationComplete: () -> Unit
): LineClearAnimationState {
    val animator = remember { LineClearAnimator() }
    var animState by remember { mutableStateOf(LineClearAnimationState()) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(
        clearingCells.hashCode(),
        clearingCells.size,
        isFullBoardClear,
        animationSpeedMultiplier
    ) {
        if (clearingCells.isEmpty()) {
            animState = LineClearAnimationState()
            return@LaunchedEffect
        }
        Log.d(
            "ULTRA-ANIM",
            "Starting ULTRA line-clear animation for ${clearingCells.size} cells (fullBoard=$isFullBoardClear, speedMult=$animationSpeedMultiplier)"
        )
        scope.launch {
            animator.runAnimation(
                clearingCells = clearingCells,
                isFullBoardClear = isFullBoardClear,
                gridSize = gridSize,
                animationSpeedMultiplier = animationSpeedMultiplier
            ) { state ->
                animState = state
            }
            // Animation finished, notify the ViewModel
            Log.d("ULTRA-ANIM", "Animation complete, calling onAnimationComplete()")
            onAnimationComplete()
        }
    }

    return animState
}

// -------------------------
// Glow layer composables & ParticleRenderer (top-level)
// -------------------------

private val glowPaint = Paint()
private val additivePaint = Paint()
private val particlePaint = Paint()

@Composable
fun CellInnerGlowLayer(
    state: CellAnimationState?,
    modifier: Modifier = Modifier.fillMaxSize()
) {
    val st = state ?: return
    if (st.innerBloom <= 0f) return

    Canvas(modifier) {
        val radius = size.minDimension * UltraGlowEngine.BLOOM_INNER_SCALE
        val center = Offset(size.width / 2f, size.height / 2f)
        drawIntoCanvas { canvas ->
            glowPaint.color = st.rainbowTint.copy(alpha = 0.85f * st.innerBloom)
            canvas.drawCircle(center, radius, glowPaint)
        }
    }
}

@Composable
fun CellOuterGlowLayer(
    state: CellAnimationState?,
    modifier: Modifier = Modifier.fillMaxSize()
) {
    val st = state ?: return
    if (st.outerBloom <= 0f) return

    Canvas(modifier) {
        val radius = size.minDimension * UltraGlowEngine.BLOOM_OUTER_SCALE
        val center = Offset(size.width / 2f, size.height / 2f)
        drawIntoCanvas { canvas ->
            glowPaint.color = st.rainbowTint.copy(alpha = 0.65f * st.outerBloom)
            canvas.drawCircle(center, radius, glowPaint)
        }
    }
}

@Composable
fun CellFlashLayer(
    state: CellAnimationState?,
    modifier: Modifier = Modifier.fillMaxSize()
) {
    val st = state ?: return
    if (st.flashWhite <= 0f) return

    Canvas(modifier) {
        drawRect(
            color = Color.White,
            alpha = st.flashWhite.coerceIn(0f, 1f),
            blendMode = BlendMode.Plus
        )
    }
}

@Composable
fun CellAdditiveLayer(
    state: CellAnimationState?,
    modifier: Modifier = Modifier.fillMaxSize()
) {
    val st = state ?: return
    if (st.additiveGlow <= 0f) return

    Canvas(modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = size.minDimension * 0.45f
        drawIntoCanvas { canvas ->
            additivePaint.color = st.rainbowTint.copy(alpha = 0.95f * st.additiveGlow)
            additivePaint.blendMode = BlendMode.Plus
            canvas.drawCircle(center, radius, additivePaint)
        }
    }
}

@Composable
fun CellTintLayer(
    state: CellAnimationState?,
    modifier: Modifier = Modifier.fillMaxSize()
) {
    val st = state ?: return
    if (!st.sweepHit) return
    if (st.alpha <= 0f) return
    Canvas(modifier) {
        val tint = st.rainbowTint.copy(alpha = 0.35f * st.alpha)
        drawRect(color = tint)
    }
}

@Composable
fun ParticleRenderer(
    particles: List<Particle>,
    modifier: Modifier = Modifier.fillMaxSize()
) {
    if (particles.isEmpty()) return
    Canvas(modifier) {
        drawIntoCanvas { canvas ->
             particles.forEach { p ->
                 particlePaint.color = Color(
                     red = p.color.red,
                     green = p.color.green,
                     blue = p.color.blue,
                     alpha = p.alpha
                 )
                canvas.save()
                canvas.translate(p.position.x, p.position.y)
                canvas.rotate(p.rotation)
                canvas.drawRect(
                    left = -p.size / 2,
                    top = -p.size / 2,
                    right = p.size / 2,
                    bottom = p.size / 2,
                    paint = particlePaint
                )
                canvas.restore()
             }
         }
     }
}
