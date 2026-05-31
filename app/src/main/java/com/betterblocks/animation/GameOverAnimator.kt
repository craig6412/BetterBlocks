package com.betterblocks.animation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import kotlinx.coroutines.coroutineScope
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

@Immutable
data class GameOverAnimationState(
    val progress: Float = 0f,
    val dimAlpha: Float = 0f,
    val glowAlpha: Float = 0f,
    val impactAlpha: Float = 0f,
    val swirlAlpha: Float = 0f,
    val isRunning: Boolean = false
)

@Stable
class GameOverAnimator(private val durationMs: Int = 1250) {
    suspend fun run(
        update: (GameOverAnimationState) -> Unit,
        onComplete: () -> Unit
    ) = coroutineScope {
        val anim = Animatable(0f)

        // Initial state: visible immediately so the player gets feedback before the dialog.
        update(
            GameOverAnimationState(
                impactAlpha = 1f,
                swirlAlpha = 0f,
                isRunning = true
            )
        )

        anim.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = durationMs,
                easing = com.betterblocks.animation.SweepEngine.BlockBlastSweepEasing
            )
        ) {
            val p = value
            val impact = if (p < 0.22f) (1f - (p / 0.22f)).coerceIn(0f, 1f) else 0f
            val swirl = ((p - 0.12f) / 0.88f).coerceIn(0f, 1f)

            update(
                GameOverAnimationState(
                    progress = p,
                    dimAlpha = (0.12f + p * 0.58f).coerceIn(0f, 0.70f),
                    glowAlpha = (1f - abs(p - 0.45f) * 2f).coerceIn(0f, 1f),
                    impactAlpha = impact,
                    swirlAlpha = swirl,
                    isRunning = true
                )
            )
        }

        // Keep the board dimmed beneath the summary dialog after the animation completes.
        update(
            GameOverAnimationState(
                progress = 1f,
                dimAlpha = 0.70f,
                glowAlpha = 0f,
                impactAlpha = 0f,
                swirlAlpha = 1f,
                isRunning = false
            )
        )
        onComplete()
    }
}

@Composable
fun GameOverOverlay(
    state: GameOverAnimationState,
    cellDp: Dp,
    gridSize: Int,
    modifier: Modifier = Modifier
) {
    if (!state.isRunning && state.progress <= 0f && state.dimAlpha <= 0f) return

    Canvas(modifier = modifier) {
        val cellPx = cellDp.toPx()
        val total = cellPx * gridSize
        val center = Offset(size.width / 2f, size.height / 2f)
        val maxRadius = size.minDimension * 0.62f

        // Darken the whole screen so failure reads as a clear state change.
        if (state.dimAlpha > 0f) {
            drawRect(color = Color.Black.copy(alpha = state.dimAlpha))
        }

        // Impact flash / freeze-frame pulse at the start of the fail sequence.
        if (state.impactAlpha > 0f) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.18f * state.impactAlpha),
                        Color(0xFF7C4DFF).copy(alpha = 0.14f * state.impactAlpha),
                        Color.Transparent
                    ),
                    center = center,
                    radius = maxRadius
                ),
                radius = maxRadius,
                center = center,
                blendMode = BlendMode.Screen
            )

            drawRect(
                color = Color.White.copy(alpha = 0.08f * state.impactAlpha),
                blendMode = BlendMode.Screen
            )
        }

        // Soft rainbow/blue sweep using the existing SweepEngine so we keep your current visual language.
        try {
            val glowBrush = com.betterblocks.animation.SweepEngine.createSoftOuterGlowBrush(
                sweepProgress = state.progress,
                isRow = true,
                totalPixels = total,
                cellPixelSize = cellPx
            )

            drawRect(
                brush = glowBrush,
                topLeft = Offset.Zero,
                size = Size(size.width, size.height),
                blendMode = BlendMode.Screen,
                alpha = 0.40f * state.glowAlpha
            )
        } catch (_: Exception) {
            if (state.glowAlpha > 0f) {
                drawRect(
                    color = Color(0xFF081426).copy(alpha = 0.12f * state.glowAlpha),
                    blendMode = BlendMode.Screen
                )
            }
        }

        // Swirl collapse: render-only, no board mutation. This gives the player a visible fail moment
        // before GameOverSummaryDialog appears.
        if (state.swirlAlpha > 0f) {
            val p = state.swirlAlpha.coerceIn(0f, 1f)
            val spin = 70f * p
            val armCount = 4
            val pointsPerArm = 44

            rotate(degrees = spin, pivot = center) {
                repeat(armCount) { arm ->
                    val path = Path()
                    for (i in 0 until pointsPerArm) {
                        val t = i / (pointsPerArm - 1f)
                        val radius = maxRadius * (1f - t * 0.86f) * (1f - p * 0.18f)
                        val angle = (arm * (2.0 * PI / armCount)) + (t * 3.8 * PI) + (p * 1.2 * PI)
                        val x = center.x + cos(angle).toFloat() * radius
                        val y = center.y + sin(angle).toFloat() * radius
                        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }

                    drawPath(
                        path = path,
                        color = Color(0xFF7C4DFF).copy(alpha = 0.28f * p),
                        style = Stroke(width = cellPx * 0.10f),
                        blendMode = BlendMode.Screen
                    )
                    drawPath(
                        path = path,
                        color = Color(0xFF00E5FF).copy(alpha = 0.18f * p),
                        style = Stroke(width = cellPx * 0.045f),
                        blendMode = BlendMode.Screen
                    )
                }
            }

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF00E5FF).copy(alpha = 0.30f * p),
                        Color(0xFF7C4DFF).copy(alpha = 0.18f * p),
                        Color.Transparent
                    ),
                    center = center,
                    radius = maxRadius * 0.28f
                ),
                radius = maxRadius * 0.28f,
                center = center,
                blendMode = BlendMode.Screen
            )

            // Closing vignette makes the summary feel like it arrives after a collapse, not instantly.
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.Black.copy(alpha = 0.48f * p)
                    ),
                    center = center,
                    radius = size.maxDimension * 0.72f
                )
            )
        }
    }
}
