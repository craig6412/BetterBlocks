package com.betterblocks.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.betterblocks.model.ScorePopupState

@Composable
fun ScorePopupRenderer(scoreState: ScorePopupState) {
    // Debug logging
    if (scoreState.isAnimating) {
        android.util.Log.d("ScorePopup", "Rendering: currentScore=${scoreState.currentScore}, comboCount=${scoreState.comboCount}")
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(999f)
    ) {
        // Main Score Count-Up - positioned at top center
        if (scoreState.isAnimating && scoreState.currentScore > 0) {
            val scale by animateFloatAsState(
                targetValue = 1.35f,
                animationSpec = tween(durationMillis = 600), label = "score_scale"
            )

            Text(
                text = "+${scoreState.currentScore}",
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 32.dp)
                    .scale(scale)
                    .zIndex(1000f),
                style = TextStyle(
                    color = Color.White,
                    fontSize = 64.sp,
                    fontWeight = FontWeight.Bold,
                    shadow = Shadow(
                        color = Color(0xFF00FFFF).copy(alpha = 0.8f),
                        blurRadius = 28f
                    )
                )
            )
        }

        // Combo Counter
        if (scoreState.comboCount >= 2) {
            val comboPulse by animateFloatAsState(
                targetValue = 1.2f,
                animationSpec = tween(durationMillis = 300), label = "combo_pulse"
            )

            Text(
                text = "COMBO x${scoreState.comboCount}!",
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 120.dp)
                    .scale(comboPulse)
                    .zIndex(1000f),
                style = TextStyle(
                    color = Color.Yellow,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    shadow = Shadow(
                        color = Color.Red.copy(alpha = 0.6f),
                        blurRadius = 20f
                    )
                )
            )
        }

        // Floating "+X" Popups
        scoreState.floatPopups.forEach { popup ->
            val alpha by animateFloatAsState(
                targetValue = if (popup.progress < 0.5f) 1f else 0f,
                animationSpec = tween(durationMillis = 450), label = "popup_alpha_${popup.id}"
            )
            val offsetY by animateFloatAsState(
                targetValue = -48f * popup.progress,
                animationSpec = tween(durationMillis = 450), label = "popup_offset_${popup.id}"
            )

            Text(
                text = "+${popup.amount}",
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = offsetY.dp)
                    .alpha(alpha)
                    .padding(top = 180.dp)
                    .zIndex(1000f),
                style = TextStyle(
                    color = Color.Cyan,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    shadow = Shadow(
                        color = Color.Magenta.copy(alpha = 0.7f),
                        blurRadius = 16f
                    )
                )
            )
        }
    }
}

