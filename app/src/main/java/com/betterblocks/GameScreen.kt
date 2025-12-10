package com.betterblocks.ui

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.betterblocks.BLOCK_MANAGER
import com.betterblocks.Block
import com.betterblocks.BlockGrid
import com.betterblocks.BottomBar
import com.betterblocks.DEFAULT_CELL_SIZE
import com.betterblocks.DarkBackground
import com.betterblocks.DeepBlue
import com.betterblocks.GameSettings
import com.betterblocks.GameUiState
import com.betterblocks.InteractionType
import com.betterblocks.R
import com.betterblocks.SimpleDragController
import com.betterblocks.GameMenuDialog
import com.betterblocks.Header
import com.betterblocks.LastChanceDialog
import com.betterblocks.LightText
import com.betterblocks.Oswald
import com.betterblocks.Pink_Jackie
import com.betterblocks.SCREEN_HORIZONTAL_PADDING
import com.betterblocks.isValidPlacement
import com.betterblocks.model.TrophyTier
import com.betterblocks.trophyColorForTier

@Composable
fun GameScreen(
    uiState: GameUiState,
    cellDp: Dp = DEFAULT_CELL_SIZE,
    onGridCellClicked: (row: Int, col: Int) -> Unit,
    onSelectBlock: (Block) -> Unit,
    onRotateBlock: () -> Unit,
    onSelectRainbow: () -> Unit,
    onReset: () -> Unit,
    onGoToMenu: () -> Unit,
    onLastChanceUsed: () -> Unit,
    onLastChanceDeclined: () -> Unit,
    onToggleSound: () -> Unit,
    onToggleMusic: () -> Unit,
    onUseRainbowImmediately: () -> Unit,
    onColorWipeSpinResult: (Int) -> Unit = {},
    onDismissTierPromotion: () -> Unit = {},
    onShareTier: (TrophyTier) -> Unit = {},
    onDismissRainbowEarned: () -> Unit = {},
    onDismissFirstGameOver: () -> Unit = {},
    onDismissPurchaseSuccess: () -> Unit = {},
    onClearCoinAnimation: () -> Unit = {},
    onDismissShopBubble: () -> Unit = {},
    onWatchAd: () -> Unit,
    onGoToShop: () -> Unit,
    onDismissZeroCoins: () -> Unit = {},
    onClearAnimationFinished: () -> Unit
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current

    // Central drag controller (Block Blast style)
    val drag = remember { SimpleDragController() }

    // Guard to ignore a TAP immediately after a DRAG on the same preview -> prevents accidental deselect
    var recentlyDraggedBlockId by remember { mutableStateOf<Int?>(null) }
    var recentDragTimeMs by remember { mutableStateOf(0L) }
    val DRAG_TAP_IGNORE_MS = 350L

    // Local measured grid metrics captured in onGloballyPositioned and applied via LaunchedEffect
    var measuredGridTopLeftWindow by remember { mutableStateOf(Offset.Zero) }
    var measuredGridSizePx by remember { mutableStateOf(0f) }

    var showMenuDialog by remember { mutableStateOf(false) }
    var showColorWheelDialog by remember { mutableStateOf(false) }

    // How far above finger block appears
    val liftPx = with(density) { 100.dp.toPx() }

    // Ghost info (controller output)
    val ghostPosition = drag.ghostPosition  // ✅ Correct property name

    // Ghost validity (placement check)
    val isGhostValid = remember(ghostPosition, drag.draggedBlock?.id, uiState.board) {
        val block = drag.draggedBlock
        ghostPosition != null && block != null &&
                isValidPlacement(uiState.board, block, ghostPosition)
    }

    // Debug toggle - set to true to render guide overlay during drag
    val renderDebugOverlay = true

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing),
        color = DarkBackground
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {

            // =====================
            // UPPER GAME COLUMN
            // =====================
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) column@{

                Header(
                    uiState = uiState,
                    onMenuClicked = { showMenuDialog = true }
                )

                // =====================
                // MAIN BOARD AREA
                // =====================
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {

                        // GRID
                        Box(
                            modifier = Modifier
                                .padding(horizontal = SCREEN_HORIZONTAL_PADDING)
                                .aspectRatio(1f)
                                .fillMaxWidth()
                                .offset(
                                    x = GameSettings.gridOffsetX.value.dp,
                                    y = (-25).dp
                                )
                                // capture measured values during layout into local state, apply to drag in LaunchedEffect
                                .onGloballyPositioned { coordinates ->
                                    // use window coords to match AnimatedGameBoard which reports window coords
                                    measuredGridTopLeftWindow = coordinates.positionInWindow()
                                    measuredGridSizePx = coordinates.size.width.toFloat()
                                }
                        ) {
                            // apply captured metrics to the drag controller after layout to avoid snapshot writes during measure
                            LaunchedEffect(measuredGridTopLeftWindow, measuredGridSizePx) {
                                if (measuredGridTopLeftWindow != Offset.Zero && measuredGridSizePx > 0f) {
                                    drag.gridTopLeft = measuredGridTopLeftWindow
                                    drag.gridSizePx = measuredGridSizePx
                                    drag.cellSizePx = measuredGridSizePx / 9f
                                    Log.d("GameScreen", "Deferred drag metrics set: topLeftWindow=$measuredGridTopLeftWindow sizePx=$measuredGridSizePx cellPx=${measuredGridSizePx/9f}")
                                }
                            }

                            // Visual border drawn slightly larger than the grid so there is a ~5.dp gap
                            val borderGap = 8.dp
                             if (measuredGridSizePx > 0f) {
                                 val gridDp = with(density) { measuredGridSizePx.toDp() }
                                 val borderSize = gridDp + (borderGap * 2)

                                 Box(
                                     modifier = Modifier
                                         .size(borderSize)
                                         .offset (
                                             x = 0.dp,
                                             y = 10.dp

                                         )
                                         .align(Alignment.Center)
                                         .border(
                                             width = 4.5.dp,
                                             color = Color(0xFF3F51B5),
                                             shape = RoundedCornerShape(8.dp)
                                         )
                                         .zIndex(0f)
                                 )
                             }

                            // The actual game board (measurements used by drag controller remain tied to this Box)
                            Box(modifier = Modifier.align(Alignment.Center).zIndex(1f)) {
                                com.betterblocks.animation.AnimatedGameBoard(
                                    board = uiState.board,
                                    gridSize = 9,
                                    cellDp = cellDp,
                                    // Let the AnimatedGameBoard draw the ghost preview using the controller's dragged block and computed ghost origin
                                    ghostBlock = drag.draggedBlock,
                                    ghostOrigin = drag.ghostPosition,
                                    isGhostValid = isGhostValid,
                                    onCellClick = onGridCellClicked,
                                    uiState = uiState,
                                    effectCells = uiState.effectCells,
                                    onClearAnimationFinished = onClearAnimationFinished,
                                    controller = drag
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // =====================
                        // AVAILABLE BLOCKS ROW
                        // =====================
                        // Defensive clamp: ensure height is non-negative (developer knobs can set negatives)
                        val safeAvailableBlocksHeight = GameSettings.availableBlocksRowHeight.value.coerceAtLeast(0f).dp
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(safeAvailableBlocksHeight)
                                .padding(horizontal = SCREEN_HORIZONTAL_PADDING),
                            contentAlignment = Alignment.Center
                        ) {
                            AvailableBlocks(
                                uiState = uiState,
                                onBlockInteraction = { block, interactionType ->
                                    when (interactionType) {
                                        InteractionType.DRAG_START -> {
                                            // Select for drag start and record the drag timestamp so a following TAP is ignored.
                                            onSelectBlock(block)
                                            recentlyDraggedBlockId = block.id
                                            recentDragTimeMs = System.currentTimeMillis()
                                        }
                                        InteractionType.TAP -> {
                                            // Ignore tap if it immediately follows a drag on the same block (prevents toggle-off)
                                            val now = System.currentTimeMillis()
                                            val ignoreTap = recentlyDraggedBlockId == block.id && (now - recentDragTimeMs) < DRAG_TAP_IGNORE_MS
                                            if (!ignoreTap) {
                                                onSelectBlock(block)
                                            }
                                        }
                                    }
                                },
                                onDragStart = { block, fingerPosRoot ->
                                    Log.d("GameScreen", "onDragStart (AvailableBlocks): block=${block.id} fingerPosRoot=$fingerPosRoot")
                                    // record recent drag here as well (defensive in case DRAG_START path differs)
                                    recentlyDraggedBlockId = block.id
                                    recentDragTimeMs = System.currentTimeMillis()
                                    // Mark selection explicitly when drag begins so UI keeps selection after finger-up
                                    onSelectBlock(block)
                                    drag.startDrag(block, fingerPosRoot, liftPx)
                                },
                                onDrag = { newFingerPos ->
                                    Log.d("GameScreen", "onDrag (AvailableBlocks): pos=$newFingerPos")
                                    drag.updatePosition(newFingerPos)

                                },
                                onDragEnd = {
                                    Log.d("GameScreen", "onDragEnd (AvailableBlocks) called")
                                    val drop = drag.endDrag(uiState.board)
                                    // Clear the recent-drag guard on finger up (so later taps work normally)
                                    recentlyDraggedBlockId = null
                                    recentDragTimeMs = 0L

                                    if (drop != null) {
                                        haptic.performHapticFeedback(
                                            androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove
                                        )
                                        onGridCellClicked(drop.first, drop.second)
                                    }
                                }
                            )
                        }

                    }
                }

            }

            // =====================
            // LOWER GAME COLUMN (Menus, Ads, etc.)
            // =====================
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                // =====================
                // MENU / SETTINGS DIALOG
                // =====================
                if (showMenuDialog) {
                    GameMenuDialog(
                        uiState = uiState,
                        onDismiss = { showMenuDialog = false },
                        onRestart = onReset,
                        onGoToMenu = onGoToMenu,
                        onToggleSound = onToggleSound,
                        onToggleMusic = onToggleMusic
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // =====================
                // BOTTOM BAR (Coins, Drag targets, etc.)
                // =====================
                BottomBar(
                    uiState = uiState,
                    onRotateBlock = {
                        if (drag.isDragging) {
                            Log.d("GameScreen", "Rotate pressed while dragging -> rotateDraggedBlockClockwise")
                            drag.rotateDraggedBlockClockwise()
                        } else {
                            onRotateBlock()
                        }
                    },
                    onSelectRainbow = onSelectRainbow,
                    onUseRainbowImmediately = onUseRainbowImmediately,
                    onColorWipeClick = {
                        Log.d("GameScreen", "BottomBar.onColorWipeClick -> showColorWheelDialog = true")
                        showColorWheelDialog = true
                    },
                    onDragStart = { block, previewOffset ->
                        Log.d("GameScreen", "onDragStart (BottomBar): block=${block.id} previewOffset=$previewOffset")
                        // record recent drag (special block drag starts too)
                        recentlyDraggedBlockId = block.id
                        recentDragTimeMs = System.currentTimeMillis()
                        // Mark selection explicitly when drag begins from bottom bar
                        onSelectBlock(block)
                        // Preview card gives window coords for finger start — pass into controller
                        drag.startDrag(block, previewOffset, liftPx)
                    },
                    onDrag = { newFingerPos ->
                        Log.d("GameScreen", "onDrag (BottomBar): pos=$newFingerPos")
                        drag.updatePosition(newFingerPos)
                    },
                    onDragEnd = {
                        Log.d("GameScreen", "onDragEnd (BottomBar) called")
                        val drop = drag.endDrag(uiState.board)
                        // Clear guard on drag end
                        recentlyDraggedBlockId = null
                        recentDragTimeMs = 0L
                        if (drop != null) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onGridCellClicked(drop.first, drop.second)
                        }
                    }
                )
            }

            // =====================
            // GHOST BLOCK (Preview of dragged block)
            // =====================
            // Ghost preview rendering is provided by the AnimatedGameBoard overlay and drag overlay elsewhere.

            // =====================
            // HAPTIC FEEDBACK TRIGGER (for testing)
            // =====================
            // HAPTIC: explicit haptics are called on drop; removed snapshot-based trigger.
        }

        // =====================
        // DEBUG: Log composition
        // =====================
        // Color wheel dialog rendering (debug logs)
        if (showColorWheelDialog) {
            Log.d("GameScreen", "showColorWheelDialog == true -> rendering ColorWheelDialog")
            ColorWheelDialog(
                onDismiss = {
                    Log.d("GameScreen", "ColorWheelDialog.onDismiss called")
                    showColorWheelDialog = false
                },
                onSpinFinished = { index ->
                    Log.d("GameScreen", "ColorWheelDialog.onSpinFinished index=$index")
                    // Close dialog first, then forward the result
                    showColorWheelDialog = false
                    try {
                        onColorWipeSpinResult(index)
                    } catch (t: Throwable) {
                        Log.e("GameScreen", "onColorWipeSpinResult threw", t)
                    }
                }
            )
        }

        // --- Rainbow Earned Dialog (earned by filling special meter) ---
        if (uiState.showRainbowEarnedDialog) {
            AlertDialog(
                onDismissRequest = { onDismissRainbowEarned() },
                title = { Text(text = "CONGRATULATIONS!", color = Pink_Jackie, fontFamily = Oswald, fontWeight = FontWeight.ExtraBold) },
                text = {
                    Column {
                        Text(text = "You just earned a free Rainbow Wipe.", color = LightText)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = "Use it when you're stuck to clear the board.", color = LightText.copy(alpha = 0.9f))
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        // Use immediately then dismiss
                        onUseRainbowImmediately()
                        onDismissRainbowEarned()
                    }) { Text("USE NOW") }
                },
                dismissButton = {
                    TextButton(onClick = { onDismissRainbowEarned() }) { Text("OK") }
                },
                properties = androidx.compose.ui.window.DialogProperties(dismissOnClickOutside = true, dismissOnBackPress = true)
            )
        }

        // --- First-Time Game Over Reward Dialog (awards 3 rainbows on first-ever game over) ---
        if (uiState.showFirstGameOverDialog) {
            AlertDialog(
                onDismissRequest = { onDismissFirstGameOver() },
                title = { Text(text = "WELCOME!", color = Pink_Jackie, fontFamily = Oswald, fontWeight = FontWeight.ExtraBold) },
                text = {
                    Column {
                        Text(text = "This is your first game over — congratulations!", color = LightText)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = "We've awarded you 3 free Rainbow Wipes to help you get back in the game.", color = LightText.copy(alpha = 0.9f))
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        // Let the user use one immediately if they want
                        onUseRainbowImmediately()
                        onDismissFirstGameOver()
                    }) { Text("USE ONE NOW") }
                },
                dismissButton = {
                    TextButton(onClick = { onDismissFirstGameOver() }) { Text("GOT IT") }
                },
                properties = androidx.compose.ui.window.DialogProperties(dismissOnClickOutside = true, dismissOnBackPress = true)
            )
        }

        // Show Game Over summary dialog when ViewModel marks the game over
        if (uiState.isGameOver) {
            Log.d("GameScreen", "uiState.isGameOver == true -> showing GameOverSummaryDialog")
            GameOverSummaryDialog(
                finalScore = uiState.score,
                highScore = uiState.highScore,
                totalLinesCleared = uiState.linesClearedThisGame,
                coinsEarned = uiState.coinsEarnedThisGame,
                trophyTier = uiState.trophyTier,
                isNewHighScore = uiState.showHighScoreAnim,
                onPlayAgain = {
                    // Restart the game via provided callback
                    onReset()
                },
                onMainMenu = {
                    onGoToMenu()
                },
                onShare = {
                    // Use helper from GameOverSummaryDialog to create a share intent
                    shareGameResults(context, uiState.score, uiState.trophyTier)
                }
            )
        }

        // --- Zero Coins Dialog ---
        if (uiState.showZeroCoinsDialog) {
            ZeroCoinsDialog(
                onDismiss = onDismissZeroCoins,
                onWatchAd = {
                    onWatchAd()
                },
                onGoToShop = {
                    onGoToShop()
                }
            )
        }

        DisposableEffect(Unit) {
            Log.d("GameScreen", "GameScreen composed")
            onDispose {
                Log.d("GameScreen", "GameScreen disposed")
            }
        }
    }
}

// Minimal local fallback implementation for ColorWheelDialog to satisfy references during build.
@Composable
private fun ColorWheelDialog(
    onDismiss: () -> Unit,
    onSpinFinished: (Int) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Color Wheel") },
        text = { Text(text = "Spin the wheel to pick a color wipe result.") },
        confirmButton = {
            TextButton(onClick = {
                // Return a default spin result (zero) — actual logic can be provided elsewhere.
                onSpinFinished(0)
            }) {
                Text("Spin")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
