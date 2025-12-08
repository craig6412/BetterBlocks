package com.betterblocks.ui

import android.util.Log
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.betterblocks.BLOCK_MANAGER
import com.betterblocks.Block
import com.betterblocks.GameSettings
import com.betterblocks.GameUiState
import com.betterblocks.InteractionType
import com.betterblocks.R
import com.betterblocks.model.TrophyTier
import com.betterblocks.trophyColorForTier


// --- DRAG STATE ---
data class DragState(
    val isDragging: Boolean = false,
    val draggedBlock: Block? = null,
    val fingerPosition: Offset = Offset.Zero  // Raw finger position in root coordinates
)

// Drag offset constants used to position the dragged preview relative to the finger
private val DRAG_OFFSET_Y: Dp = 100.dp
private val DRAG_OFFSET_X: Dp = 20.dp

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
    onDismissPurchaseSuccess: () -> Unit = {},
    onClearCoinAnimation: () -> Unit = {},
    onDismissShopBubble: () -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var dragState by remember { mutableStateOf(DragState()) }
    var gridTopLeft by remember { mutableStateOf(Offset.Zero) }
    var gridSizePx by remember { mutableStateOf(0f) }
    var showMenuDialog by remember { mutableStateOf(false) }
    var showColorWheelDialog by remember { mutableStateOf(false) }
    var rootTopLeft by remember { mutableStateOf(Offset.Zero) }

    val density = LocalDensity.current
    val dragOffsetYPx = with(density) { DRAG_OFFSET_Y.toPx() }
    val dragOffsetXPx = with(density) { DRAG_OFFSET_X.toPx() }

    val ghostPosition =
        remember(dragState.fingerPosition, gridTopLeft, gridSizePx, dragState.draggedBlock) {
            if (!dragState.isDragging || gridSizePx == 0f || dragState.draggedBlock == null) null
            else {
                // Offset the ghost position to align perfectly under the preview block
                // Preview is 150dp above finger, ghost should align with it (100dp up, 20dp right)
                val ghostOffsetY = with(density) { 100.dp.toPx() }
                val ghostOffsetX = with(density) { 20.dp.toPx() }
                val adjustedFingerPos = dragState.fingerPosition.copy(
                    x = dragState.fingerPosition.x + ghostOffsetX,
                    y = dragState.fingerPosition.y - ghostOffsetY
                )

                calculateGridPosition(
                    dragPos = adjustedFingerPos,
                    gridTopLeft = gridTopLeft,
                    gridSizePx = gridSizePx,
                    gridSize = 9,
                    visualOffsetX = 0f,
                    visualOffsetY = 0f
                )
            }
        }

    val isGhostValid = remember(ghostPosition, dragState.draggedBlock, uiState.board) {
        if (ghostPosition == null || dragState.draggedBlock == null) false
        else isValidPlacement(uiState.board, dragState.draggedBlock!!, ghostPosition)
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing),   // <-- this is the ONLY inset padding you need
        color = DarkBackground
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .onGloballyPositioned { coordinates ->
                    rootTopLeft = coordinates.localToWindow(Offset.Zero)
                }
        ) {

            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header expects onReset and onMenuClicked in your current definition
                Header(
                    uiState = uiState,
                    onMenuClicked = { showMenuDialog = true }
                )

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
                                .onGloballyPositioned { coordinates ->
                                    gridTopLeft = coordinates.localToWindow(Offset.Zero)
                                    gridSizePx = coordinates.size.width.toFloat()
                                }
                        ) {
                            com.betterblocks.animation.AnimatedGameBoard(
                                board = uiState.board,
                                gridSize = 9,
                                cellDp = cellDp,
                                ghostBlock = if (dragState.isDragging) dragState.draggedBlock else null,
                                ghostOrigin = ghostPosition,
                                isGhostValid = isGhostValid,
                                onCellClick = onGridCellClicked,
                                uiState = uiState,
                                effectCells = uiState.effectCells
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // AVAILABLE BLOCKS
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(GameSettings.availableBlocksRowHeight.value.dp)
                                .padding(horizontal = SCREEN_HORIZONTAL_PADDING),

                            contentAlignment = Alignment.Center
                        ) {
                            AvailableBlocks(
                                uiState = uiState,
                                onBlockInteraction = { block, interactionType ->
                                    when (interactionType) {
                                        InteractionType.TAP -> onSelectBlock(block)
                                        InteractionType.DRAG_START -> onSelectBlock(block)
                                    }
                                },
                                onDragStart = { block, previewCardOffset ->
                                    Log.d("DRAG", "START block=${block.name} at window pos=$previewCardOffset")

                                    // Compute drag offset so the block is centered under the finger regardless of rotation
                                    val cellSizePx = with(density) { cellDp.toPx() }
                                    val dragOffset = calculateDragOffset(block, cellSizePx)

                                    dragState = DragState(
                                        isDragging = true,
                                        draggedBlock = block,
                                        // Store finger position already adjusted so overlay/ghost can use it directly
                                        fingerPosition = previewCardOffset + dragOffset
                                    )
                                },
                                onDrag = { dragAmount ->
                                    // Update finger position as user drags
                                    val newFingerPos = dragState.fingerPosition + dragAmount
                                    dragState = dragState.copy(
                                        fingerPosition = newFingerPos
                                    )
                                },
                                onDragEnd = {
                                    Log.d("DRAG", "END at finger pos=${dragState.fingerPosition}")

                                    // Offset drop position to match ghost/preview alignment (100dp up, 20dp right)
                                    val ghostOffsetY = with(density) { 100.dp.toPx() }
                                    val ghostOffsetX = with(density) { 20.dp.toPx() }
                                    val adjustedFingerPos = dragState.fingerPosition.copy(
                                        x = dragState.fingerPosition.x + ghostOffsetX,
                                        y = dragState.fingerPosition.y - ghostOffsetY
                                    )

                                    val dropTarget = calculateGridPosition(
                                        dragPos = adjustedFingerPos,
                                        gridTopLeft = gridTopLeft,
                                        gridSizePx = gridSizePx,
                                        gridSize = 9,
                                        visualOffsetX = 0f,
                                        visualOffsetY = 0f
                                    )

                                    Log.d("DRAG", " dropTarget=$dropTarget")

                                    if (dropTarget != null && dragState.draggedBlock != null) {
                                        Log.d("DRAG", " VALID drop → placing ${dragState.draggedBlock!!.name}")
                                        onGridCellClicked(dropTarget.first, dropTarget.second)
                                    } else {
                                        Log.d("DRAG", " INVALID drop → ignoring")
                                    }
                                    dragState = DragState()
                                }
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    BottomBar(
                        uiState = uiState,
                        onRotateBlock = onRotateBlock,
                        onSelectRainbow = onSelectRainbow,
                        onUseRainbowImmediately = onUseRainbowImmediately,
                        onColorWipeClick = { showColorWheelDialog = true },
                        onDragStart = { block, previewCardOffset ->
                            val cellSizePx = with(density) { cellDp.toPx() }
                            val dragOffset = calculateDragOffset(block, cellSizePx)
                            dragState = DragState(
                                isDragging = true,
                                draggedBlock = block,
                                fingerPosition = previewCardOffset + dragOffset
                            )
                        },
                        onDrag = { dragAmount ->
                            val newFingerPos = dragState.fingerPosition + dragAmount
                            dragState = dragState.copy(
                                fingerPosition = newFingerPos
                            )
                        },
                        onDragEnd = {
                            // Offset drop position to match ghost/preview alignment (100dp up, 20dp right)
                            val ghostOffsetY = with(density) { DRAG_OFFSET_Y.toPx() }
                            val ghostOffsetX = with(density) { DRAG_OFFSET_X.toPx() }
                            val adjustedFingerPos = dragState.fingerPosition.copy(
                                x = dragState.fingerPosition.x + ghostOffsetX,
                                y = dragState.fingerPosition.y - ghostOffsetY
                            )

                            val dropTarget = calculateGridPosition(
                                dragPos = adjustedFingerPos,
                                gridTopLeft = gridTopLeft,
                                gridSizePx = gridSizePx,
                                gridSize = 9,
                                visualOffsetX = 0f,
                                visualOffsetY = 0f
                            )
                            if (dropTarget != null && dragState.draggedBlock != null) {
                                onGridCellClicked(dropTarget.first, dropTarget.second)
                            }
                            dragState = DragState()
                        }
                    )

                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 80.dp)
                    ) {
                        androidx.compose.animation.AnimatedVisibility(
                            visible = uiState.showHighScoreAnim,
                            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut()
                        ) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Pink_Jackie),
                                elevation = CardDefaults.cardElevation(8.dp),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Filled.EmojiEvents,
                                        contentDescription = null,
                                        tint = androidx.compose.ui.graphics.Color.White,
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "NEW HIGH SCORE!",
                                        color = androidx.compose.ui.graphics.Color.White,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontFamily = Oswald
                                    )
                                 }
                            }
                        }
                    }
                }
            }

            // --- Drag Overlay ---
            // Dragged block appears 100dp above the user's finger for natural placement feel
            if (dragState.isDragging && dragState.draggedBlock != null) {
                val block = dragState.draggedBlock!!
                val previewCellSize = cellDp

                // Convert finger position from window to root coordinates
                val rootFingerPos = dragState.fingerPosition - rootTopLeft

                // Because dragState.fingerPosition is already the block center, just apply any fine-tuning
                val blockX = rootFingerPos.x + dragOffsetXPx
                val blockY = rootFingerPos.y - dragOffsetYPx

                Box(
                    modifier = Modifier
                        .graphicsLayer {
                            translationX = blockX
                            translationY = blockY
                        }
                        .zIndex(999f)
                        .scale(1.0f)
                ) {
                    // Important: use the same composable and spacing as the real board
                    BlockGrid(
                        block = block,
                        cellSize = previewCellSize,

                        )

                }
            }

            if (uiState.isLastChance) {
                LastChanceDialog(
                    onUseRainbow = { dragState = DragState(); onLastChanceUsed() },
                    onGameOver = { onLastChanceDeclined() }
                )
            }

            // Game Over Summary Dialog - New Enhanced Version
            if (uiState.showGameSummaryDialog) {
                GameOverSummaryDialog(
                    finalScore = uiState.score,
                    highScore = uiState.highScore,
                    totalLinesCleared = uiState.linesClearedThisGame,
                    coinsEarned = uiState.coinsEarnedThisGame,
                    trophyTier = uiState.trophyTier,
                    isNewHighScore = uiState.score >= uiState.highScore,
                    onPlayAgain = onReset,
                    onMainMenu = onGoToMenu,
                    onShare = {
                        shareGameResults(context, uiState.score, uiState.trophyTier)
                    }
                )
            } else if (uiState.isGameOver && !uiState.isLastChance) {
                // Fallback to old dialog if summary not shown
                GameOverDialog(
                    score = uiState.score,
                    highScore = uiState.highScore,
                    onRestart = onReset,
                    onMenu = onGoToMenu
                )
            }

            if (showMenuDialog) {
                GameMenuDialog(
                    uiState = uiState,
                    onDismiss = { showMenuDialog = false },
                    onRestart = {
                        showMenuDialog = false
                        onReset()
                    },
                    onGoToMenu = {
                        showMenuDialog = false
                        onGoToMenu()
                    },
                    onToggleSound = onToggleSound,
                    onToggleMusic = onToggleMusic
                )
            }

            if (showColorWheelDialog) {
                ColorWheelDialog(
                    onDismiss = { showColorWheelDialog = false },
                    onSpinFinished = { index ->
                        showColorWheelDialog = false
                        onColorWipeSpinResult(index)
                    }
                )
            }

            if (uiState.showZeroCoinsDialog) {
                ZeroCoinsDialog(
                    onDismiss = { /* TODO hook to VM */ },
                    onWatchAd = { /* TODO load ad */ },
                    onGoToShop = { /* TODO open shop */ }
                )
            }

            // Rainbow Meter Full Dialog
            if (uiState.showRainbowEarnedDialog) {
                RainbowEarnedDialog(
                    onDismiss = onDismissRainbowEarned
                )
            }

            // Tier Unlock Dialog
            if (uiState.showTierPromotionDialog && uiState.newlyUnlockedTier != null) {
                TierUnlockDialog(
                    tier = uiState.newlyUnlockedTier,
                    onDismiss = onDismissTierPromotion
                )
            }

            // Purchase Success Dialog
            if (uiState.showPurchaseSuccessDialog && uiState.purchaseCoinsAwarded > 0) {
                PurchaseSuccessDialog(
                    coinsAwarded = uiState.purchaseCoinsAwarded,
                    onDismiss = onDismissPurchaseSuccess
                )
            }

            // Floating Coin Reward Animation
            if (uiState.coinsEarnedThisUpdate > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = 60.dp)
                ) {
                    FloatingCoinReward(
                        amount = uiState.coinsEarnedThisUpdate,
                        onComplete = onClearCoinAnimation
                    )
                }
            }

            // Shop Purchase Bubble
            if (uiState.showShopPurchaseBubble) {
                Box(
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    ShopPurchaseBubble(
                        message = uiState.shopPurchaseMessage,
                        onComplete = onDismissShopBubble
                    )
                }
            }

            ScorePopupRenderer(scoreState = uiState.scoreState)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GameScreenPreview() {
    val sampleBoard =
        Array(9) { r -> Array<Int?>(9) { _ -> if (r == 8) R.drawable.blue else null } }
    val sampleBlocks = BLOCK_MANAGER.take(3)
    val sampleUi = GameUiState(
        board = sampleBoard,
        availableBlocks = sampleBlocks,
        score = 2500,
        highScore = 4000,
        coins = 9999,
        freeRotations = 1,
        lastRotatedBlockId = sampleBlocks[0].id,
        isGameOver = false,
        isLastChance = false,
        selectedBlock = sampleBlocks[0],
        rainbowBlockCount = 99,
        specialMeterValue = 2,
        isSoundEnabled = true,
        isMusicEnabled = false
    )
    MaterialTheme {
        GameScreen(
            uiState = sampleUi,
            cellDp = 38.dp,
            onGridCellClicked = { _, _ -> },
            onSelectBlock = {},
            onRotateBlock = {},
            onSelectRainbow = {},
            onReset = {},
            onGoToMenu = {},
            onLastChanceUsed = {},
            onLastChanceDeclined = {},
            onToggleSound = {},
            onToggleMusic = {},
            onUseRainbowImmediately = {},
            onColorWipeSpinResult = {},
            onDismissTierPromotion = {},
            onShareTier = {},
            onDismissRainbowEarned = {},
            onDismissPurchaseSuccess = {},
            onClearCoinAnimation = {},
            onDismissShopBubble = {}
        )
    }
}

@Composable
fun TierPromotionDialog(
    tier: TrophyTier,
    onDismiss: () -> Unit,
    onShare: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "New Rank Achieved!",
                fontFamily = Oswald,
                fontSize = 22.sp,
                color = LightText
            )
        },
        text = {
            Text(
                text = "You've unlocked the ${tier.name} tier!",
                fontFamily = Oswald,
                fontSize = 18.sp,
                color = trophyColorForTier(tier)
            )
        },
        confirmButton = {
            TextButton(onClick = onShare) {
                Text("Share", color = Pink_Jackie, fontFamily = Oswald)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = LightText, fontFamily = Oswald)
            }
        },
        containerColor = DeepBlue
    )
}
