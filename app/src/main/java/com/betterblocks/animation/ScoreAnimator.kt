package com.betterblocks.animation

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class ScoreAnimator(private val scope: CoroutineScope) {

    private var animationJob: Job? = null

    fun start(
        baseScore: Int,
        comboCount: Int,
        onUpdate: (Int) -> Unit,
        onFinished: () -> Unit,
        onScoreIncrementSound: () -> Unit,
        onComboSound: () -> Unit
    ) {
        animationJob?.cancel()

        val multiplier = when {
            comboCount >= 5 -> 1.50f
            comboCount == 4 -> 1.10f
            comboCount == 3 -> 0.70f
            comboCount == 2 -> 0.40f
            else -> 0f
        }

        if (comboCount >= 2) {
            onComboSound()
        }

        val totalScore = baseScore + (baseScore * multiplier).roundToInt()

        animationJob = scope.launch {
            val easing = CubicBezierEasing(0.12f, 0.95f, 0.20f, 1.0f)
            val duration = 1200L
            var currentTime = 0L

            while (currentTime < duration) {
                val progress = easing.transform(currentTime.toFloat() / duration)
                val currentAnimatedScore = (totalScore * progress).roundToInt()
                onUpdate(currentAnimatedScore)
                if (progress > 0.1f) onScoreIncrementSound() // Play sound after a short delay
                delay(16) // Corresponds to ~60fps
                currentTime += 16
            }

            onUpdate(totalScore) // Ensure final score is set
            onFinished()
        }
    }
}


