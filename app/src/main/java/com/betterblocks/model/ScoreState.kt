package com.betterblocks.model

data class ScorePopupState(
    val currentScore: Int = 0,
    val isAnimating: Boolean = false,
    val comboCount: Int = 0,
    val floatPopups: List<FloatingScorePopup> = emptyList()
)

data class FloatingScorePopup(
    val id: Long,
    val amount: Int,
    val progress: Float
)

