package com.betterblocks.animation.blockblast

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.runtime.withFrameNanos
import com.betterblocks.Coord
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

// Global duration scale for line clear animations.
// 1.0f = current speed, >1.0f = slower animation, <1.0f = faster.
var LINE_CLEAR_DURATION_SCALE: Float = 0.5f

class BlockBlastLineClearState {
    // Per-tile pop/flash states
    val tileStates: SnapshotStateMap<Coord, TilePopState> = mutableStateMapOf()

    // Active sweep bar (null when idle)
    var activeSweep = mutableStateOf<SweepBarState?>(null)
        private set

    // Particle pool rendered by ParticleRenderer
    val particles = mutableStateListOf<ParticleState>()

    // Previous clearing request to prevent restart spam
    private var currentRequest: ClearingRequest? = null

    fun applyClears(request: ClearingRequest) {
        if (request == currentRequest) return
        currentRequest = request
        launchSweep(request)
    }

    private fun launchSweep(request: ClearingRequest) {
        val sweep = SweepTimeline(
            isRow = request.isRow,
            index = request.lineIndex,
            gridSize = request.gridSize,
            cellSize = request.cellSizePx
        )
        activeSweep.value = sweep.state
        sweep.onTileImpact = { coord ->
            val state = tileStates.getOrPut(coord) { TilePopState() }
            state.restart()
            particles += ParticleState.fromTile(coord, request.cellSizePx)
        }
        sweep.onFinished = { activeSweep.value = null }
        sweep.start()
    }
}

data class ClearingRequest(
    val lineIndex: Int,
    val isRow: Boolean,
    val gridSize: Int,
    val cellSizePx: Float
)

class TilePopState {
    private var elapsedMs = 0f
    private var flashElapsed = 0f
    private var running = false

    val scale: Float
        get() = when {
            elapsedMs <= 45f -> 1f + (elapsedMs / 45f) * 0.22f
            elapsedMs <= 90f -> 1.22f - ((elapsedMs - 45f) / 45f) * 0.12f
            elapsedMs <= 180f -> 1.10f - ((elapsedMs - 90f) / 90f) * 1.10f
            else -> 0f
        }

    val rotation: Float
        get() = if (elapsedMs <= 90f) {
            (sin(elapsedMs / 90f * PI) * 13f).toFloat()
        } else 0f

    val alpha: Float
        get() = when {
            elapsedMs <= 120f -> 1f
            elapsedMs <= 180f -> (1f - (elapsedMs - 120f) / 60f).coerceIn(0f, 1f)
            else -> 0f
        }

    val flashAlpha: Float
        get() = (1f - flashElapsed / 70f).coerceIn(0f, 1f)

    fun restart() {
        elapsedMs = 0f
        flashElapsed = 0f
        running = true
    }

    fun tick(deltaMs: Float) {
        if (!running) return
        val scaledDelta = deltaMs * (1f / LINE_CLEAR_DURATION_SCALE.coerceAtLeast(0.1f))
        elapsedMs += scaledDelta
        flashElapsed += scaledDelta
        if (elapsedMs >= 180f && flashAlpha <= 0f) {
            running = false
        }
    }
}

class ParticleState private constructor(
    var positionX: Float,
    var positionY: Float,
    private var velX: Float,
    private var velY: Float,
    val sizePx: Float,
    private val lifeMs: Float
) {
    private var ageMs = 0f

    val alpha: Float
        get() = (1f - ageMs / lifeMs).coerceIn(0f, 1f)

    companion object {
        fun fromTile(coord: Coord, cellSize: Float): ParticleState {
            val centerX = coord.col * cellSize + cellSize / 2f
            val centerY = coord.row * cellSize + cellSize / 2f
            val speed = Random.nextFloat() * 180f + 340f
            val angle = Random.nextFloat() * 2f * PI.toFloat()
            return ParticleState(
                centerX,
                centerY,
                velX = (kotlin.math.cos(angle) * speed),
                velY = (kotlin.math.sin(angle) * speed),
                sizePx = Random.nextFloat() * 6f + 6f,
                lifeMs = Random.nextFloat() * 200f + 450f
            )
        }
    }

    fun tick(deltaMs: Float) {
        // Apply the same global duration scaling as tile pop so particles
        // slow down / speed up together with the line clear animation.
        val scaledDelta = deltaMs * (1f / LINE_CLEAR_DURATION_SCALE.coerceAtLeast(0.1f))

        ageMs += scaledDelta
        if (ageMs >= lifeMs) return

        val drag = 0.88f
        val gravity = 820f
        val dtSeconds = scaledDelta / 1000f

        positionX += velX * dtSeconds
        positionY += velY * dtSeconds
        velY += gravity * dtSeconds
        velX *= drag
        velY *= drag
    }
}

private class SweepTimeline(
    private val isRow: Boolean,
    private val index: Int,
    private val gridSize: Int,
    private val cellSize: Float
) {
    val state = SweepBarState(
        isRow = isRow,
        lineIndex = index,
        position = 0f,
        alpha = 1f,
        floatingSquares = emptyList(),
        dustParticles = emptyList()
    )

    var onTileImpact: ((Coord) -> Unit)? = null
    var onFinished: (() -> Unit)? = null

    fun start() {
        onFinished?.invoke()
    }
}

@Composable
fun rememberBlockBlastLineClearState(
    clearingCells: Set<Coord>,
    gridSize: Int,
    cellSizePx: Float
): BlockBlastLineClearState {
    val state = remember { BlockBlastLineClearState() }

    LaunchedEffect(clearingCells) {
        if (clearingCells.isNotEmpty()) {
            val rows = clearingCells.map { it.row }.toSet()
            rows.forEach { row ->
                state.applyClears(
                    ClearingRequest(
                        lineIndex = row,
                        isRow = true,
                        gridSize = gridSize,
                        cellSizePx = cellSizePx
                    )
                )
            }
        }
    }

    LaunchedEffect(Unit) {
        var lastFrame = 0L
        while (true) {
            withFrameNanos { frameTime ->
                if (lastFrame == 0L) {
                    lastFrame = frameTime
                    return@withFrameNanos
                }
                val delta = (frameTime - lastFrame) / 1_000_000f
                lastFrame = frameTime
                state.tileStates.values.forEach { it.tick(delta) }
                val iterator = state.particles.iterator()
                while (iterator.hasNext()) {
                    val particle = iterator.next()
                    particle.tick(delta)
                    if (particle.alpha <= 0f) iterator.remove()
                }
            }
        }
    }

    return state
}
