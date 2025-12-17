package com.betterblocks.animation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.Canvas
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.ui.unit.Dp
import kotlinx.coroutines.coroutineScope
import kotlin.math.abs

@Immutable
data class GameOverAnimationState(
    val progress: Float = 0f,
    val dimAlpha: Float = 0f,
    val glowAlpha: Float = 0f,
    val isRunning: Boolean = false
)

@Stable
class GameOverAnimator(private val durationMs: Int = 850) {
    suspend fun run(
        update: (GameOverAnimationState) -> Unit,
        onComplete: () -> Unit
    ) = coroutineScope {
        val anim = Animatable(0f)

        // initial state
        update(GameOverAnimationState(isRunning = true))

        anim.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = durationMs,
                easing = com.betterblocks.animation.SweepEngine.BlockBlastSweepEasing
            )
        ) {
            val p = value
            update(
                GameOverAnimationState(
                    progress = p,
                    dimAlpha = (p * 0.65f).coerceIn(0f, 0.65f),
                    glowAlpha = (1f - abs(p - 0.5f) * 2f).coerceIn(0f, 1f),
                    isRunning = true
                )
            )
        }

        // finalize
        update(GameOverAnimationState(progress = 1f, dimAlpha = 0.65f, glowAlpha = 0f, isRunning = false))
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
    if (!state.isRunning && state.progress <= 0f) return

    Canvas(modifier = modifier) {
        val cellPx = cellDp.toPx()
        val total = cellPx * gridSize

        // Board dim
        if (state.dimAlpha > 0f) {
            drawRect(color = Color.Black.copy(alpha = state.dimAlpha))
        }

        // Soft rainbow/blue sweep using SweepEngine
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
                size = androidx.compose.ui.geometry.Size(size.width, size.height),
                blendMode = BlendMode.Screen,
                alpha = 0.45f * state.glowAlpha
            )
        } catch (_: Exception) {
            // fallback subtle blue wash
            if (state.glowAlpha > 0f) drawRect(color = Color(0xFF081426).copy(alpha = 0.12f * state.glowAlpha))
        }
    }
}
