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

/**
 * Main activity for the Better Blocks game.
 * It sets up the Compose environment and connects the UI to the ViewModel.
 */
class GameActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // This will now resolve to the BetterBlocksTheme composable
            // defined in your Theme.kt file (in the same package)
            BetterBlocksTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Initialize the ViewModel instance
                    val viewModel: GameViewModel = viewModel()

                    // Collect the StateFlow as Compose state
                    val uiState by viewModel.uiState.collectAsState()

                    // Display the main game screen, passing the state and event handlers
                    GameScreen(
                        // Pass the collected 'uiState' value
                        uiState = uiState,

                        // Use parameter names that match GameScreen's signature
                        onGridCellClicked = viewModel::onGridCellClicked,
                        onSelectBlock = viewModel::selectBlock,
                        onRotateBlock = viewModel::rotateSelectedBlock,
                        onReset = viewModel::resetGame
                    )
                }
            }
        }
    }
}