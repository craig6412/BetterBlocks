package com.betterblocks.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Refresh
// rotate_right is loaded via R.drawable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.betterblocks.Block
import com.betterblocks.GameGrid
import com.betterblocks.GameUiState
import com.betterblocks.R
import com.betterblocks.BLOCK_MANAGER
import kotlin.math.roundToInt
import kotlin.collections.flatten
import androidx.compose.ui.unit.sp
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api

// --- CONSTANTS & COLORS ---
val TROPHY_ICON_PLACEHOLDER = R.drawable.ic_launcher_foreground
val DeepBlue = Color(0xFF282C6D)
val DarkBackground = Color(0xFF1E214A)
val BoardBackground = Color(0xFF2B2F5D)
val LightText = Color(0xFFFFFFFF)
val CoinGold = Color(0xFFFFD700)

// Add Oswald font family
val Oswald = FontFamily(Font(R.font.oswald_regular))

// --- VISUAL ADJUSTMENT KNOBS ---
var DEFAULT_CELL_SIZE: Dp = 40.dp
var SCREEN_HORIZONTAL_PADDING: Dp = 10.dp
var SCREEN_VERTICAL_PADDING: Dp = 16.dp

// Controls the physical space between grid cells (0.dp = touching)
var BLOCK_PADDING: Dp = 0.dp

// Controls the Zoom level of the PNG texture to crop transparent borders
var BLOCK_TEXTURE_SCALE: Float = 1.13f

// --- DRAG OFFSET KNOBS (Adjusted for Visibility) ---
// Moves the floating block UP so it isn't covered by your finger
var DRAG_OFFSET_Y: Dp = 90.dp
// Moves the floating block RIGHT slightly for better visibility
var DRAG_OFFSET_X: Dp = 0.dp

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
    onReset: () -> Unit
) {
    var dragState by remember { mutableStateOf(DragState()) }
    var gridTopLeft by remember { mutableStateOf(Offset.Zero) }
    var gridSizePx by remember { mutableStateOf(0f) }

    val density = LocalDensity.current
    val dragOffsetYPx = with(density) { DRAG_OFFSET_Y.toPx() }
    val dragOffsetXPx = with(density) { DRAG_OFFSET_X.toPx() }

    // Calculate Ghost Position
    val ghostPosition = remember(dragState.dragPosition, gridTopLeft, gridSizePx, uiState.selectedBlock) {
        if (!dragState.isDragging || gridSizePx == 0f || dragState.draggedBlock == null) null
        else calculateGridPosition(dragState.dragPosition, gridTopLeft, gridSizePx, 9)
    }

    // Smart Placement Check
    val isGhostValid = remember(ghostPosition, dragState.draggedBlock, uiState.board) {
        if (ghostPosition == null || dragState.draggedBlock == null) false
        else isValidPlacement(uiState.board, dragState.draggedBlock!!, ghostPosition)
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = DarkBackground
    ) {
        Box(modifier = Modifier.fillMaxSize()) {

            // --- Main Layout ---
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = SCREEN_HORIZONTAL_PADDING, vertical = SCREEN_VERTICAL_PADDING),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Header(uiState = uiState, onReset = onReset)

                Spacer(modifier = Modifier.height(16.dp))

                // 2. Game Grid
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f)
                        .fillMaxWidth()
                        .onGloballyPositioned { coordinates ->
                            gridTopLeft = coordinates.positionInRoot()
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

                Spacer(modifier = Modifier.height(24.dp))

                // 3. Available Blocks
                AvailableBlocks(
                    uiState = uiState,
                    onSelectBlock = onSelectBlock,
                    onDragStart = { block, previewCardOffset ->
                        onSelectBlock(block)
                        // Calculate initial position with X and Y offsets
                        // Moves block UP (-Y) and RIGHT (+X)
                        val initialPos = previewCardOffset + Offset(dragOffsetXPx, -dragOffsetYPx)

                        dragState = DragState(
                            isDragging = true,
                            draggedBlock = block,
                            currentDragOffset = Offset.Zero,
                            dragViewOffset = previewCardOffset,
                            dragPosition = initialPos
                        )
                    },
                    onDrag = { dragAmount ->
                        val newDragOffset = dragState.currentDragOffset + dragAmount
                        // Apply offsets to logical position for ghost calculation
                        val logicalPos = dragState.dragViewOffset + newDragOffset + Offset(dragOffsetXPx, -dragOffsetYPx)

                        dragState = dragState.copy(
                            currentDragOffset = newDragOffset,
                            dragPosition = logicalPos
                        )
                    },
                    onDragEnd = {
                        val dropTarget = calculateGridPosition(dragState.dragPosition, gridTopLeft, gridSizePx, 9)
                        if (dropTarget != null && dragState.draggedBlock != null) {
                            onGridCellClicked(dropTarget.first, dropTarget.second)
                        }
                        dragState = DragState()
                    },
                    onRotateBlock = onRotateBlock
                )
            }

            // --- Drag Overlay (Floating Block) ---
            if (dragState.isDragging && dragState.draggedBlock != null) {
                val block = dragState.draggedBlock!!
                val previewCellSize = cellDp

                // Calculate visual position with X and Y offsets
                val offsetX = (dragState.dragViewOffset.x + dragState.currentDragOffset.x) + dragOffsetXPx
                val offsetY = (dragState.dragViewOffset.y + dragState.currentDragOffset.y) - dragOffsetYPx

                Box(
                    modifier = Modifier
                        .graphicsLayer {
                            translationX = offsetX
                            translationY = offsetY
                        }
                        .zIndex(100f)
                        .scale(1.0f) // Keep 1:1 scale so it looks exactly like it will land
                ) {
                    BlockShapeDisplay(block, previewCellSize)
                }
            }
        }
    }
}

// --- HELPER FUNCTIONS ---

/**
 * [NEW] Validates if the block can be placed at the specific origin (row, col).
 * Checks boundaries and existing blocks.
 */
fun isValidPlacement(board: GameGrid, block: Block, origin: Pair<Int, Int>): Boolean {
    if (origin.first + block.boundingBoxHeight > 9 || origin.second + block.boundingBoxWidth > 9) return false

    // 2. Check Overlaps
    for (coord in block.shape) {
        val r = origin.first + coord.row
        val c = origin.second + coord.col
        if (r < 0 || r >= 9 || c < 0 || c >= 9) return false
        if (board[r][c] != null) return false
    }
    return true
}

/**
 * [UPDATED] Uses 'roundToInt' for magnetic snapping.
 * This means if you are 51% into a cell, it snaps to that cell, rather than floor().
 */
fun calculateGridPosition(dragPos: Offset, gridTopLeft: Offset, gridSizePx: Float, gridSize: Int): Pair<Int, Int>? {
    val relativeX = dragPos.x - gridTopLeft.x
    val relativeY = dragPos.y - gridTopLeft.y

    val cellSize = gridSizePx / gridSize

    // Increase tolerance to allow drops slightly outside the visual grid boundary
    val tolerance = cellSize * 0.8f

    if (relativeX < -tolerance || relativeX > gridSizePx + tolerance ||
        relativeY < -tolerance || relativeY > gridSizePx + tolerance) {
        return null
    }

    // Use roundToInt() for magnetic snap-to-center feel
    val col = (relativeX / cellSize).roundToInt().coerceIn(0, gridSize - 1)
    val row = (relativeY / cellSize).roundToInt().coerceIn(0, gridSize - 1)
    return Pair(row, col)
}

// --- VISUAL COMPONENTS ---

@Composable
fun Header(uiState: GameUiState, onReset: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
            .background(DeepBlue)
            .padding(vertical = 16.dp, horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Better Blocks", color = LightText, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, fontFamily = Oswald)
            Surface(
                color = Color.Black.copy(alpha = 0.2f),
                shape = RoundedCornerShape(50),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.MonetizationOn,
                        contentDescription = "Coins",
                        tint = CoinGold,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = uiState.coins.toString(),
                        color = LightText,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = Oswald
                    )
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // CURRENT SCORE: Show trophy = false
            ScoreDisplay(
                score = uiState.score,
                label = "Current",
                showTrophy = false,
                modifier = Modifier.weight(1f)
            )

            // HIGH SCORE: Show trophy = true, Gold tint
            ScoreDisplay(
                score = uiState.highScore,
                label = "High Score",
                showTrophy = true,
                trophyTint = CoinGold,
                modifier = Modifier.weight(1f)
            )

            IconButton(onClick = onReset, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Default.Refresh, contentDescription = "Reset", tint = LightText)
            }
        }
    }
}

@Composable
fun ScoreDisplay(
    score: Int,
    label: String,
    modifier: Modifier = Modifier,
    showTrophy: Boolean = true,
    trophyTint: Color = LightText
) {
    Column(
        modifier = modifier.padding(horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (showTrophy) {
                Icon(
                    imageVector = Icons.Filled.EmojiEvents,
                    contentDescription = "Trophy",
                    tint = trophyTint,
                    modifier = Modifier.size(20.dp).padding(end = 4.dp)
                )
            }
            Text(text = score.toString(), color = LightText, fontSize = 20.sp, fontWeight = FontWeight.Bold, fontFamily = Oswald)
        }
        Text(text = label, color = LightText.copy(alpha = 0.7f), fontSize = 12.sp, fontFamily = Oswald)
    }
}

@Composable
fun GameBoard(
    board: GameGrid,
    gridSize: Int,
    cellDp: Dp,
    ghostBlock: Block?,
    ghostOrigin: Pair<Int, Int>?,
    isGhostValid: Boolean = false,
    onCellClick: (row: Int, col: Int) -> Unit,
    uiState: GameUiState
) {
    val totalBoardSize = with(LocalDensity.current) { (gridSize * cellDp.toPx()).toDp() }
    Box(
        modifier = Modifier
            .width(totalBoardSize)
            .height(totalBoardSize)
            .clip(RoundedCornerShape(8.dp))
            .background(BoardBackground)
            .border(BorderStroke(2.dp, Color.White.copy(alpha = 0.5f)), RoundedCornerShape(8.dp))
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(gridSize),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(0.dp)
        ) {
            itemsIndexed(board.flatten()) { index, cellValue ->
                val r = index / gridSize
                val c = index % gridSize
                val isOccupied = cellValue != null

                val isClearing = remember(uiState.clearingCells) {
                    uiState.clearingCells.any { it.row == r && it.col == c }
                }
                val staggerDelay = ((r + c) * 50).toInt()
                val targetScale = if (isClearing) 0.0f else if (isOccupied) 1.05f else 1.0f
                val animSpec = tween<Float>(durationMillis = 300, delayMillis = staggerDelay)
                val blockScale: Float by animateFloatAsState(targetValue = targetScale, animationSpec = animSpec, label = "BlockClearScale")

                // Ghost Logic
                var isGhostCell = false
                if (ghostBlock != null && ghostOrigin != null) {
                    val relativeR = r - ghostOrigin.first
                    val relativeC = c - ghostOrigin.second
                    isGhostCell = ghostBlock.shape.any { it.row == relativeR && it.col == relativeC }
                }

                val ghostColor = if (isGhostCell) {
                    if (isGhostValid) Color.Green.copy(alpha = 0.5f) else Color.Red.copy(alpha = 0.5f)
                } else {
                    BoardBackground
                }

                if (blockScale > 0.01f) {
                    Box(
                        modifier = Modifier
                            .size(cellDp)
                            .scale(blockScale)
                            .graphicsLayer(alpha = blockScale)
                            .padding(BLOCK_PADDING)
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (isOccupied) Color.Transparent else ghostColor)
                            .border(BorderStroke(1.dp, if (cellValue != null) Color.Black.copy(0.1f) else DarkBackground.copy(0.3f)), RoundedCornerShape(4.dp))
                            .clickable { onCellClick(r, c) }
                    ) {
                        if (isOccupied && cellValue != null) {
                            Image(
                                painter = painterResource(id = cellValue),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .scale(BLOCK_TEXTURE_SCALE)
                            )
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.size(cellDp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AvailableBlocks(
    uiState: GameUiState,
    onSelectBlock: (Block) -> Unit,
    onDragStart: (Block, previewCardOffset: Offset) -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    onRotateBlock: () -> Unit
) {
    val blocks = uiState.availableBlocks
    val selectedBlock = uiState.selectedBlock

    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            blocks.forEach { block ->
                val isSelected = block == selectedBlock
                var cardAbsOffset by remember(block) { mutableStateOf(Offset.Zero) }

                Box(
                    modifier = Modifier
                        .onGloballyPositioned { coordinates ->
                            cardAbsOffset = coordinates.positionInRoot()
                        }
                        .pointerInput(block) {
                            detectDragGestures(
                                onDragStart = {
                                    onDragStart(block, cardAbsOffset)
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    onDrag(dragAmount)
                                },
                                onDragEnd = { onDragEnd() }
                            )
                        }
                        .clickable { onSelectBlock(block) }
                ) {
                    BlockPreviewCard(block, isSelected, onSelectBlock)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        val isRotationEnabled = selectedBlock != null
        val buttonContainerColor = if (isRotationEnabled) DeepBlue else Color.Gray.copy(alpha = 0.3f)
        val buttonContentColor = if (isRotationEnabled) LightText else Color.White.copy(alpha = 0.5f)

        CompositionLocalProvider(LocalMinimumInteractiveComponentEnforcement provides false) {
            Button(
                onClick = onRotateBlock,
                enabled = isRotationEnabled,
                modifier = Modifier
                    .width(200.dp)
                    .height(40.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = buttonContainerColor,
                    contentColor = buttonContentColor,
                    disabledContainerColor = buttonContainerColor,
                    disabledContentColor = buttonContentColor
                )
            ) {
                val rotateIconTint = if (isRotationEnabled) LightText else LightText.copy(alpha = 0.4f)
                Icon(
                    painter = painterResource(R.drawable.rotate_right),
                    contentDescription = "Rotate Block",
                    modifier = Modifier.size(18.dp),
                    tint = rotateIconTint
                )
                Spacer(Modifier.width(8.dp))
                val rotationText = when {
                    selectedBlock == null -> ""
                    selectedBlock.id == uiState.lastRotatedBlockId -> " (Free)"
                    uiState.freeRotations > 0 -> " (${uiState.freeRotations} Left)"
                    else -> " (10 Coins)"
                }
                Text(
                    "Rotate$rotationText",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    fontFamily = Oswald
                )
            }
        }
    }
}

@Composable
fun BlockPreviewCard(block: Block, isSelected: Boolean, onClick: (Block) -> Unit) {
    val borderColor = if (isSelected) Color.White else Color.Gray.copy(alpha = 0.5f)
    val borderWidth = if (isSelected) 3.dp else 1.dp
    Card(
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(borderWidth, borderColor),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .wrapContentSize()
            .padding(4.dp)
            .clickable { onClick(block) }
    ) {
        BlockShapeDisplay(block, 18.dp)
    }
}

@Composable
fun BlockShapeDisplay(block: Block, cellSize: Dp) {
    val rows = block.boundingBoxHeight
    val cols = block.boundingBoxWidth

    Column(modifier = Modifier.padding(4.dp)) {
        for (r in 0 until rows) {
            Row {
                for (c in 0 until cols) {
                    val isPresent = block.shape.any { it.row == r && it.col == c }
                    Box(
                        modifier = Modifier
                            .size(cellSize)
                            .padding(BLOCK_PADDING)
                            .clip(RoundedCornerShape(2.dp))
                            .background(if (isPresent) Color.Transparent else Color.Transparent)
                            .border(
                                BorderStroke(1.dp, if (isPresent) Color.White.copy(0.3f) else Color.Transparent),
                                RoundedCornerShape(2.dp)
                            )
                    ) {
                        if (isPresent) {
                            Image(
                                painter = painterResource(id = block.colorResId),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .scale(BLOCK_TEXTURE_SCALE)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GameScreenPreview() {
    // Preview code
}