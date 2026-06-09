package com.betterblocks

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.betterblocks.ads.AdManager
import com.betterblocks.ui.theme.BetterBlocksTheme

class GameActivity : ComponentActivity() {
    override fun onDestroy() {
        AdManager.cleanup()
        super.onDestroy()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Ensure SFX are loaded even when GameActivity is launched directly,
        // restored by Android, or opened without passing through MainActivity.
        SoundManager.init(this)

        // Set navigation bar color and icon style
        window.decorView.post {
            window.navigationBarColor = 0xFF0F0C15.toInt() // Midnight Void
            WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightNavigationBars = false // Light icons
        }

        setContent {
            BetterBlocksTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val viewModel: GameViewModel = viewModel()
                    val uiState by viewModel.uiState.collectAsState()

                    // Note: interstitials are intentionally not auto-shown here.
                    // Ads are gated and shown from the GameScreen when the user taps Play Again (every Nth game).

                    GameScreen(
                        uiState = uiState,
                        onGridCellClicked = { row, col ->
                            viewModel.placeBlock(row, col)
                        },
                        onSelectBlock = { block ->
                            viewModel.selectBlock(block)
                        },
                        onRotateBlock = viewModel::rotateSelectedBlock,
                        onSelectRainbow = { viewModel.selectRainbowBlock() },
                        onReset = viewModel::restartGame,
                        onGoToMenu = { finish() },
                        // --- NEW REQUIRED PARAMETERS ---
                        onLastChanceUsed = viewModel::onLastChanceUsed,
                        onLastChanceDeclined = viewModel::onLastChanceDeclined,
                        onToggleSound = viewModel::toggleSound,
                        onToggleMusic = viewModel::toggleMusic,
                        onUseRainbowImmediately = viewModel::useRainbowWipeImmediately,
                        onColorWipeSpinResult = viewModel::onColorWipeSpinResult,
                        onDismissTierPromotion = { viewModel.dismissTierPromotion() },
                        onDismissRainbowEarned = viewModel::dismissRainbowEarnedDialog,
                        onDismissFirstGameOver = viewModel::dismissFirstGameOverDialog,
                        onDismissPurchaseSuccess = viewModel::dismissPurchaseSuccessDialog,
                        onClearCoinAnimation = viewModel::clearCoinEarnedAnimation,
                        onDismissShopBubble = viewModel::dismissShopPurchaseBubble,
                        onWatchAd = {
                            val activity = this@GameActivity
                            if (AdManager.isRewardedLoaded.value) {
                                AdManager.showRewarded(
                                    activity,
                                    onRewardEarned = {
                                        viewModel.addCoins(com.betterblocks.ads.AdManager.REWARDED_COINS)
                                        viewModel.dismissZeroCoinsDialog()
                                    },
                                    onFailed = {
                                        AdManager.preloadRewarded(activity)
                                    }
                                )
                            } else {
                                AdManager.preloadRewarded(activity)
                            }
                        },
                        onGoToShop = {
                            startActivity(Intent(this@GameActivity, ShopActivity::class.java))
                            viewModel.dismissZeroCoinsDialog()
                        },
                        onDismissZeroCoins = { viewModel.dismissZeroCoinsDialog() },
                        onClearAnimationFinished = viewModel::onClearAnimationFinished
                    )
                }
            }
        }
    }
}
