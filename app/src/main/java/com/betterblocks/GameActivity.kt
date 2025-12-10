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
import com.betterblocks.ui.GameScreen
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

                    // React to game over transitions and attempt to show interstitials from the Activity
                    androidx.compose.runtime.LaunchedEffect(uiState.isGameOver) {
                        if (uiState.isGameOver) {
                            // Try to show an interstitial on game over (AdManager handles load/show and counters)
                            AdManager.tryShowInterstitial(this@GameActivity)
                        }
                    }

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
                                AdManager.showDoubleRewarded(activity, onCompletedBoth = { viewModel.addCoins(50) }, onFailed = { AdManager.preloadDoubleRewarded(activity) })
                            } else {
                                AdManager.preloadDoubleRewarded(activity)
                            }
                            viewModel.dismissZeroCoinsDialog()
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
