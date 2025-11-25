package com.betterblocks.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import com.betterblocks.R
import androidx.compose.ui.draw.scale
import android.util.Log



// --- DRAG STATE ---
data class DragState(
    val isDragging: Boolean = false,
    val draggedBlock: Block? = null,
    val currentDragOffset: Offset = Offset.Zero,
    val dragViewOffset: Offset = Offset.Zero,
    val dragPosition: Offset = Offset.Zero
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
    onColorWipeSpinResult: (Int) -> Unit = {}
) {
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
        remember(dragState.dragPosition, gridTopLeft, gridSizePx, uiState.selectedBlock) {
            if (!dragState.isDragging || gridSizePx == 0f || dragState.draggedBlock == null) null
            else {
                // Drag position is local to the root Box, but gridTopLeft is a window coordinate.
                // Convert dragPosition to window coordinates for calculation.
                val windowDragPosition = dragState.dragPosition + rootTopLeft
                calculateGridPosition(windowDragPosition, gridTopLeft, gridSizePx, 9)
            }
        }

    val isGhostValid = remember(ghostPosition, dragState.draggedBlock, uiState.board) {
        if (ghostPosition == null || dragState.draggedBlock == null) false
        else isValidPlacement(uiState.board, dragState.draggedBlock!!, ghostPosition)
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
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
                    onReset = onReset,
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
                            GameBoard(
                                board = uiState.board,
                                gridSize = 9,
                                cellDp = cellDp,
                                ghostBlock = if (dragState.isDragging) dragState.draggedBlock else null,
                                ghostOrigin = ghostPosition,
                                isGhostValid = isGhostValid,
                                onCellClick = onGridCellClicked,
                                uiState = uiState
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
                                onSelectBlock = onSelectBlock,
                                onDragStart = { block, previewCardOffset ->
                                    Log.d("DRAG", "START block=${block.name}  offset=$previewCardOffset")
                                    onSelectBlock(block)
                                    val initialDragViewOffset = previewCardOffset - rootTopLeft
                                    val initialPos = initialDragViewOffset + Offset(
                                        dragOffsetXPx,
                                        -dragOffsetYPx
                                    )
                                    dragState = DragState(
                                        isDragging = true,
                                        draggedBlock = block,
                                        currentDragOffset = Offset.Zero,
                                        dragViewOffset = initialDragViewOffset,
                                        dragPosition = initialPos
                                    )
                                },
                                onDrag = { dragAmount ->
                                    Log.d("DRAG", "DRAG amount=$dragAmount currentOffset=${dragState.currentDragOffset}")
                                    val newOffset = dragState.currentDragOffset + dragAmount

                                    dragState = dragState.copy(
                                        currentDragOffset = newOffset,
                                        dragPosition = dragState.dragViewOffset + newOffset +
                                                Offset(dragOffsetXPx, -dragOffsetYPx)
                                    )
                                },
                                onDragEnd = {
                                    Log.d("DRAG", "END at dragPos=${dragState.dragPosition}")

                                    val dropTarget = calculateGridPosition(
                                        dragState.dragPosition + rootTopLeft, // Convert to window coords
                                        gridTopLeft,
                                        gridSizePx,
                                        9


                                    )

                                    Log.d("DRAG", " dropTarget=$dropTarget")

                                    if (dropTarget != null && dragState.draggedBlock != null) {
                                        Log.d("DRAG", " VALID drop → placing ${dragState.draggedBlock!!.name}")
                                        onGridCellClicked(dropTarget.first, dropTarget.second)
                                    }
                                    else{

                                        Log.d("DRAG", " INVALID drop → ignoring")}
                                    Log.d("DRAG", " END dragState=$dragState")
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
                        onGridCellClicked = onGridCellClicked,
                        onUseRainbowImmediately = onUseRainbowImmediately,
                        onColorWipeClick = { showColorWheelDialog = true },
                        onDragStart = { block, previewCardOffset ->
                            val initialDragViewOffset = previewCardOffset - rootTopLeft
                            val initialPos = initialDragViewOffset + Offset(dragOffsetXPx, -dragOffsetYPx)
                            dragState = DragState(
                                isDragging = true,
                                draggedBlock = block,
                                currentDragOffset = Offset.Zero,
                                dragViewOffset = initialDragViewOffset,
                                dragPosition = initialPos
                            )
                        },
                        onDrag = { dragAmount ->
                            val newDragOffset = dragState.currentDragOffset + dragAmount
                            val logicalPos = dragState.dragViewOffset + newDragOffset + Offset(
                                dragOffsetXPx,
                                -dragOffsetYPx
                            )
                            dragState = dragState.copy(
                                currentDragOffset = newDragOffset,
                                dragPosition = logicalPos
                            )
                        },
                        onDragEnd = {
                            val dropTarget =
                                calculateGridPosition(
                                    dragState.dragPosition + rootTopLeft, // Convert to window coords
                                    gridTopLeft,
                                    gridSizePx,
                                    9
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

            // --- Drag Overlay ---
            // MOVED here to be a direct child of the root Box, so its coordinate system matches our calculations
            if (dragState.isDragging && dragState.draggedBlock != null) {
                val block = dragState.draggedBlock!!
                val previewCellSize = cellDp
                val offsetX = (dragState.dragViewOffset.x + dragState.currentDragOffset.x) + dragOffsetXPx
                val offsetY = (dragState.dragViewOffset.y + dragState.currentDragOffset.y) - dragOffsetYPx

                Box(
                    modifier = Modifier
                        .graphicsLayer {
                            translationX = offsetX
                            translationY = offsetY
                        }
                        .zIndex(100f)
                        .scale(1.0f)
                ) {
                    BlockShapeDisplay(block, previewCellSize)
                }
            }
            
            if (uiState.isLastChance) {
                LastChanceDialog(
                    onUseRainbow = { dragState = DragState(); onLastChanceUsed() },
                    onGameOver = { onLastChanceDeclined() }
                )
            }

            if (uiState.isGameOver && !uiState.isLastChance) {
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
            onUseRainbowImmediately = {}
        )
    }
}