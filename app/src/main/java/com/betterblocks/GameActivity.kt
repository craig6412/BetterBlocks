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
                        // Fix: We must pass a value for the onReset parameter, even if it's an empty function,
                        // because the GameScreen composable still requires it.
                        onReset = { /* Do nothing */ },
                        onGoToMenu = {
                            finish() // Closes GameActivity -> Returns to MainMenu
                        }
                    )
                }
            }
        }
    }
}