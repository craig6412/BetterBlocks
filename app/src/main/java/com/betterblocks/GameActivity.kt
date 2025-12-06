package com.betterblocks

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.betterblocks.ui.GameScreen
import com.betterblocks.ui.theme.BetterBlocksTheme

class GameActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BetterBlocksTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val viewModel: GameViewModel = viewModel()
                    val uiState by viewModel.uiState.collectAsState()

                    GameScreen(
                        uiState = uiState,
                        onGridCellClicked = viewModel::onGridCellClicked,
                        onSelectBlock = viewModel::selectBlock,
                        onRotateBlock = viewModel::rotateSelectedBlock,
                        onSelectRainbow = viewModel::selectRainbowBlock,
                        onReset = viewModel::restartGame,
                        onGoToMenu = {
                            finish() // Closes GameActivity -> Returns to MainMenu
                        },
                        // --- NEW REQUIRED PARAMETERS ---
                        onLastChanceUsed = viewModel::onLastChanceUsed,
                        onLastChanceDeclined = viewModel::onLastChanceDeclined,
                        onToggleSound = viewModel::toggleSound,
                        onToggleMusic = viewModel::toggleMusic,
                        onUseRainbowImmediately = viewModel::useRainbowWipeImmediately,
                        onColorWipeSpinResult = viewModel::onColorWipeSpinResult,
                                                onDismissTierPromotion = viewModel::dismissTierPromotion,
                        onDismissRainbowEarned = viewModel::dismissRainbowEarnedDialog,
                        onDismissPurchaseSuccess = viewModel::dismissPurchaseSuccessDialog,
                        onClearCoinAnimation = viewModel::clearCoinEarnedAnimation,
                        onDismissShopBubble = viewModel::dismissShopPurchaseBubble,
                        // connect preview update callback
                        onUpdatePreviewClear = viewModel::updatePreviewClear
                    )
                }
            }
        }
    }
}