package com.betterblocks

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.betterblocks.ads.AdManager
import com.betterblocks.GameScreen
import com.betterblocks.ui.theme.BetterBlocksTheme

class GameActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
                                AdManager.showDoubleRewarded(
                                    activity,
                                    onCompletedBoth = {
                                        // Grant coins and dismiss the zero-coins dialog when the user finishes both ads
                                        viewModel.addCoins(com.betterblocks.ads.AdManager.DOUBLE_REWARD_COINS)
                                        viewModel.dismissZeroCoinsDialog()
                                    },
                                    onFailed = {
                                        // If ad failed, try to preload again and keep dialog open (user can choose shop)
                                        AdManager.preloadDoubleRewarded(activity)
                                    }
                                )
                            } else {
                                // Not ready — kick off preload and keep the dialog open so user can still choose the shop
                                AdManager.preloadDoubleRewarded(activity)
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
