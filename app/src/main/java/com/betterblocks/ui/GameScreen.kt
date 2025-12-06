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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.betterblocks.BLOCK_MANAGER
import com.betterblocks.Block
import com.betterblocks.GameSettings
import com.betterblocks.GameUiState
import com.betterblocks.GameViewModel
import com.betterblocks.GRID_SIZE
import com.betterblocks.R
import com.betterblocks.getRotatedBlock
import com.betterblocks.model.TrophyTier
import com.betterblocks.trophyColorForTier
import com.betterblocks.ui.AvailableBlocks


// NOTE: Drag offsets now controlled via GameSettings for developer tuning

/**
 * Calculates the grid row/col for a block based on its center point.
 *
 * @param blockCenterPoint The center coordinate of the dragged block visual.
 * @param gridTopLeft The top-left coordinate of the game grid.
 * @param gridSizePx The total size of the grid in pixels.
 * @param gridSize The number of cells in one dimension of the grid.
 * @return A Pair(row, col) if the center is within the grid, otherwise null.
 */
private fun calculateGridPosition(
    blockCenterPoint: Offset,
    gridTopLeft: Offset,
    gridSizePx: Float,
    gridSize: Int
): Pair<Int, Int>? {
    val relativePos = blockCenterPoint - gridTopLeft

    if (relativePos.x < 0 || relativePos.x > gridSizePx || relativePos.y < 0 || relativePos.y > gridSizePx) {
        return null
    }

    val cellSizePx = gridSizePx / gridSize
    val col = (relativePos.x / cellSizePx).toInt().coerceIn(0, gridSize - 1)
    val row = (relativePos.y / cellSizePx).toInt().coerceIn(0, gridSize - 1)

    return Pair(row, col)
}


// --- DRAG STATE ---
data class DragState(
    val isDragging: Boolean = false,
    val draggedBlock: Block? = null,
    val rawPosition: Offset = Offset.Zero, // Raw touch coordinates
    val snappedTarget: GridCoordinate? = null // The crucial addition
)

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
    onDismissRainbowEarned: () -> Unit = {},
    onDismissPurchaseSuccess: () -> Unit = {},
    onClearCoinAnimation: () -> Unit = {},
    onDismissShopBubble: () -> Unit = {},
    // Called by UI during ghost drag to update which rows/cols would clear
    onUpdatePreviewClear: (rowsOrCols: List<Int>, isRow: Boolean) -> Unit = { _, _ -> }
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var dragState by remember { mutableStateOf(DragState()) }
    var gridTopLeft by remember { mutableStateOf(Offset.Zero) }
    var gridSizePx by remember { mutableStateOf(0f) }
    var showMenuDialog by remember { mutableStateOf(false) }
    var showColorWheelDialog by remember { mutableStateOf(false) }
    var rootTopLeft by remember { mutableStateOf(Offset.Zero) }

    val density = LocalDensity.current

    // Visual offsets for rendering the dragged block (from GameSettings)
    val visualDragOffsetYPx = with(density) { GameSettings.visualDragOffsetY.floatValue.dp.toPx() }
    val visualDragOffsetXPx = with(density) { GameSettings.visualDragOffsetX.floatValue.dp.toPx() }

    // Matching offsets used for ghost/drop calculations (independent from visual preview)
    val matchingDragOffsetYPx = with(density) { GameSettings.matchingDragOffsetY.floatValue.dp.toPx() }
    val matchingDragOffsetXPx = with(density) { GameSettings.matchingDragOffsetX.floatValue.dp.toPx() }

    // Placement correction applied AFTER matching offset to fine-tune final placement
    val placementCorrectionXPx = with(density) { GameSettings.blockPlacementCorrectionX.floatValue.dp.toPx() }
    val placementCorrectionYPx = with(density) { GameSettings.blockPlacementCorrectionY.floatValue.dp.toPx() }

    val ghostPosition = remember(dragState.snappedTarget) {
        dragState.snappedTarget?.let {
            Pair(it.row, it.col)
        }
    }

    val isGhostValid = remember(ghostPosition) {
        ghostPosition != null
    }

    // Compute preview clear lines during drag and call back to ViewModel via onUpdatePreviewClear
    LaunchedEffect(ghostPosition, dragState.isDragging, dragState.draggedBlock, uiState.board) {
        if (!dragState.isDragging || dragState.draggedBlock == null) {
            onUpdatePreviewClear(emptyList(), true)
            return@LaunchedEffect
        }

        val gp = ghostPosition
        val block = dragState.draggedBlock
        if (gp == null || block == null) {
            onUpdatePreviewClear(emptyList(), true)
            return@LaunchedEffect
        }

        // Clone board
        val temp = uiState.board.map { it.clone() }.toTypedArray()

        // Try to place; if invalid placement, clear preview
        var outOfBounds = false
        for (coord in block.shape) {
            val r = gp.first + coord.row
            val c = gp.second + coord.col
            if (r !in 0 until GRID_SIZE || c !in 0 until GRID_SIZE) {
                outOfBounds = true
                break
            }
            if (temp[r][c] != null && !block.isSpecial) {
                outOfBounds = true
                break
            }
            temp[r][c] = block.colorResId
        }

        if (outOfBounds) {
            onUpdatePreviewClear(emptyList(), true)
            return@LaunchedEffect
        }

        val fullRows = mutableListOf<Int>()
        for (r in 0 until GRID_SIZE) {
            if (temp[r].all { it != null }) fullRows.add(r)
        }

        val fullCols = mutableListOf<Int>()
        for (c in 0 until GRID_SIZE) {
            var allFilled = true
            for (r in 0 until GRID_SIZE) {
                if (temp[r][c] == null) { allFilled = false; break }
            }
            if (allFilled) fullCols.add(c)
        }

        when {
            fullRows.isNotEmpty() -> onUpdatePreviewClear(fullRows, true)
            fullCols.isNotEmpty() -> onUpdatePreviewClear(fullCols, false)
            else -> onUpdatePreviewClear(emptyList(), true)
        }
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
                                    x = GameSettings.gridOffsetX.floatValue.dp,
                                    y = (-25).dp
                                )
                                .onGloballyPositioned { coordinates ->
                                    gridTopLeft = coordinates.localToWindow(Offset.Zero)
                                    gridSizePx = coordinates.size.width.toFloat()
                                }
                        ) {
                            com.betterblocks.animation.AnimatedGameBoard(
                                board = uiState.board,
                                gridSize = GRID_SIZE,
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
                                .height(GameSettings.availableBlocksRowHeight.floatValue.dp)
                                .padding(horizontal = SCREEN_HORIZONTAL_PADDING),

                            contentAlignment = Alignment.Center
                        ) {
                            AvailableBlocks(
                                uiState = uiState,
                                onSelectBlock = onSelectBlock,
                                onDragStart = { block: Block, previewCardOffset: Offset ->
                                    Log.d("🚀 START", "Drag starting: ${block.name}")
                                    onSelectBlock(block)
                                    // FIX: Use the selected block from uiState, which has the correct rotation
                                    val currentBlock = uiState.selectedBlock ?: block
                                    val rotatedBlock = getRotatedBlock(currentBlock, uiState.rotation)
                                    dragState = DragState(
                                        isDragging = true,
                                        draggedBlock = rotatedBlock,
                                        rawPosition = previewCardOffset
                                    )
                                },
                                onDrag = { dragAmount: Offset ->
                                    val newRawPosition = dragState.rawPosition + dragAmount
                                    // FIX: Ensure we use the rotated block for placement validation
                                    val block = dragState.draggedBlock
                                    var newSnappedTarget: GridCoordinate? = null

                                    if (block != null && gridSizePx > 0) {
                                        val adjustedDragPos = newRawPosition.copy(
                                            x = newRawPosition.x + matchingDragOffsetXPx + placementCorrectionXPx,
                                            // matching Y is applied as negative to position the block ABOVE the finger
                                            y = newRawPosition.y - matchingDragOffsetYPx + placementCorrectionYPx
                                        )

                                        val calculatedPos = calculateGridPosition(
                                            blockCenterPoint = adjustedDragPos,
                                            gridTopLeft = gridTopLeft,
                                            gridSizePx = gridSizePx,
                                            gridSize = GRID_SIZE
                                        )

                                        if (calculatedPos != null && isValidPlacement(uiState.board, block, calculatedPos)) {
                                            newSnappedTarget = GridCoordinate(calculatedPos.first, calculatedPos.second)
                                        }
                                    }

                                    dragState = dragState.copy(
                                        rawPosition = newRawPosition,
                                        snappedTarget = newSnappedTarget
                                    )
                                },
                                onDragEnd = {
                                    val target = dragState.snappedTarget
                                    // FIX: Use the rotated block from the drag state
                                    val block = dragState.draggedBlock
                                    if (target != null && block != null) {
                                        Log.d("🔍 DROP", "✅ Placing block at $target")
                                        // The onGridCellClicked will use the ViewModel's state, which should be correct
                                        onGridCellClicked(target.row, target.col)
                                    } else {
                                        Log.e("🔍 DROP", "❌ Drop failed. No valid target.")
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
                             onSelectRainbow()
                             // FIX: Use the rotated block for drag state
                             val rotatedBlock = getRotatedBlock(block, uiState.rotation)
                            dragState = DragState(
                                isDragging = true,
                                draggedBlock = rotatedBlock,
                                rawPosition = previewCardOffset
                            )
                        },
                        onDrag = { dragAmount ->
                            val newRawPosition = dragState.rawPosition + dragAmount
                            // FIX: Use the rotated block from drag state
                            val block = dragState.draggedBlock
                            var newSnappedTarget: GridCoordinate? = null

                            if (block != null && gridSizePx > 0) {
                                val adjustedDragPos = newRawPosition.copy(
                                    x = newRawPosition.x + matchingDragOffsetXPx + placementCorrectionXPx,
                                    y = newRawPosition.y - matchingDragOffsetYPx + placementCorrectionYPx
                                )
                                val calculatedPos = calculateGridPosition(
                                    blockCenterPoint = adjustedDragPos,
                                    gridTopLeft = gridTopLeft,
                                    gridSizePx = gridSizePx,
                                    gridSize = GRID_SIZE
                                )
                                if (calculatedPos != null && isValidPlacement(uiState.board, block, calculatedPos)) {
                                    newSnappedTarget = GridCoordinate(calculatedPos.first, calculatedPos.second)
                                }
                            }
                            dragState = dragState.copy(
                                rawPosition = newRawPosition,
                                snappedTarget = newSnappedTarget
                            )
                        },
                        onDragEnd = {
                            val target = dragState.snappedTarget
                             // FIX: Use the rotated block from drag state
                             val block = dragState.draggedBlock
                            if (target != null && block != null) {
                                onGridCellClicked(target.row, target.col)
                            } else {
                                Log.e("🔍 DROP [BottomBar]", "❌ NOT placing - conditions not met")
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
                                colors = CardDefaults.cardColors(containerColor = CoinGold),
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

            if (dragState.isDragging && dragState.draggedBlock!= null) {
    Box(
        modifier = Modifier
            // OPTIMIZATION: Use graphicsLayer instead of offset.
            //.offset() triggers a Layout Pass (expensive).
            //.graphicsLayer triggers only a Draw Pass (cheap, GPU accelerated).
            // This is essential for 120Hz smoothness and fixing the "Speed" issue.
           .graphicsLayer {
                // Visual preview uses visual offsets (block appears above finger)
                translationX = dragState.rawPosition.x + visualDragOffsetXPx
                translationY = dragState.rawPosition.y - visualDragOffsetYPx
                scaleX = GameSettings.draggedBlockScale.floatValue
                scaleY = GameSettings.draggedBlockScale.floatValue
            }
           .zIndex(10f) // Ensure it renders on top of everything
    ) {
        BlockGrid(
            block = dragState.draggedBlock!!,
            cellSize = cellDp
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
                Text("Share", color = CoinGold, fontFamily = Oswald)
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
