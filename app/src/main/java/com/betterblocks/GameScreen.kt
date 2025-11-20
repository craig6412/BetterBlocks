package com.betterblocks.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import com.betterblocks.*
import com.betterblocks.R
import kotlin.math.roundToInt
import kotlin.collections.flatten
import androidx.compose.ui.unit.sp
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api

// --- CONSTANTS & COLORS ---
val DeepBlue = Color(0xFF282C6D)
val DarkBackground = Color(0xFF1E214A)
val BoardBackground = Color(0xFF2B2F5D)
val LightText = Color(0xFFFFFFFF)
val CoinGold = Color(0xFFFFD700)
val SpecialPurple = Color(0xFF9C27B0)
val SuccessGreen = Color(0xFF4CAF50) // Used for free/valid actions

val Oswald = FontFamily(Font(R.font.oswald_regular))

// --- VISUAL ADJUSTMENT KNOBS ---
var DEFAULT_CELL_SIZE: Dp = 38.dp // Slightly smaller cells for more space
var SCREEN_HORIZONTAL_PADDING: Dp = 16.dp // Increased padding for cleaner look
var SCREEN_VERTICAL_PADDING: Dp = 0.dp // Removed vertical padding from surface

var BLOCK_PADDING: Dp = 0.dp
var BLOCK_TEXTURE_SCALE: Float = 1.13f

// --- DRAG OFFSET KNOB ---
var DRAG_OFFSET_Y: Dp = 80.dp
var DRAG_OFFSET_X: Dp = 12.dp

// --- PREVIEW BLOCK OFFSET (Removed usage of this knob) ---
var BLOCK_PREVIEW_OFFSET_Y: Dp = 0.dp

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
    onGoToMenu: () -> Unit
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
                    .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // 1. Header (Full Width)
                Header(uiState = uiState, onReset = onReset) // onReset is passed but the button is removed inside Header

                // 2. Game Grid (Takes up space based on aspect ratio/fillMaxWidth)
                Box(
                    modifier = Modifier
                        .padding(horizontal = SCREEN_HORIZONTAL_PADDING)
                        .aspectRatio(1f)
                        .fillMaxWidth()
                        .offset(
                            x = GameSettings.gridOffsetX.value.dp,
                            y = GameSettings.gridOffsetY.value.dp
                        )
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

                Spacer(modifier = Modifier.height(12.dp))

                // 3. Available Blocks (Positioned naturally by SpaceBetween and Spacer)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = SCREEN_HORIZONTAL_PADDING)
                ) {
                    AvailableBlocks(
                        uiState = uiState,
                        onSelectBlock = onSelectBlock,
                        onDragStart = { block, previewCardOffset ->
                            onSelectBlock(block)
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
                        }
                    )
                }

                // 4. Bottom Toolbar (Rotation and Special Block)
                BottomBar(
                    uiState = uiState,
                    onRotateBlock = onRotateBlock,
                    onSelectRainbow = onSelectRainbow,
                    onGridCellClicked = onGridCellClicked,
                    onDragStart = { block, previewCardOffset ->
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
                        val logicalPos = dragState.dragViewOffset + newDragOffset + Offset(dragOffsetXPx, -dragOffsetYPx)
                        dragState = dragState.copy(currentDragOffset = newDragOffset, dragPosition = logicalPos)
                    },
                    onDragEnd = {
                        val dropTarget = calculateGridPosition(dragState.dragPosition, gridTopLeft, gridSizePx, 9)
                        if (dropTarget != null && dragState.draggedBlock != null) {
                            onGridCellClicked(dropTarget.first, dropTarget.second)
                        }
                        dragState = DragState()
                    }
                )
            }

            // --- High Score Banner ---
            AnimatedVisibility(
                visible = uiState.showHighScoreAnim,
                enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 80.dp)
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
                        Icon(Icons.Filled.EmojiEvents, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "NEW HIGH SCORE!",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = Oswald
                        )
                    }
                }
            }


            // --- Drag Overlay ---
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
        }
    }
    // Game Over Dialog (kept original implementation)
    if (uiState.isGameOver) {
        GameOverDialog(
            score = uiState.score,
            highScore = uiState.highScore,
            onRestart = onReset,
            onMenu = onGoToMenu
        )
    }
}

@Composable
fun Header(uiState: GameUiState, onReset: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
            .background(DeepBlue)
            .padding(top = 16.dp, bottom = 12.dp, start = 16.dp, end = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Row: (Removed Title) Coins
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.End, // Aligned to end since title is removed
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Coin Display (Kept as requested)
            Surface(
                color = CoinGold.copy(alpha = 0.1f),
                shape = RoundedCornerShape(50),
                border = BorderStroke(1.dp, CoinGold)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
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
                        fontSize = 18.sp, // Larger coin count
                        fontWeight = FontWeight.Bold,
                        fontFamily = Oswald
                    )
                }
            }
        }

        // --- NEW: Special Meter Display ---
        SpecialMeterDisplay(currentValue = uiState.specialMeterValue, maxValue = 5)
        Spacer(modifier = Modifier.height(12.dp))

        // Score Row: Current Score | High Score
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            ScoreDisplay(score = uiState.score, label = "CURRENT", modifier = Modifier.weight(1f))
            ScoreDisplay(score = uiState.highScore, label = "HIGH SCORE", showTrophy = true, trophyTint = CoinGold, modifier = Modifier.weight(1f))

            // REMOVED Reset Button. Added Spacer to occupy the space where the button was (48.dp)
            Spacer(modifier = Modifier.width(48.dp))
        }
    }
}

@Composable
fun SpecialMeterDisplay(currentValue: Int, maxValue: Int) {
    val progress = currentValue.toFloat() / maxValue.toFloat()

    // Animate the progress bar fill
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 300),
        label = "MeterProgress"
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "SPECIAL CHARGE: COMBO x2+",
            color = LightText.copy(alpha = 0.9f),
            fontSize = 12.sp,
            fontFamily = Oswald,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth(0.9f) // Meter is slightly narrower than screen
                .height(16.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.Black.copy(alpha = 0.5f))
                .border(1.dp, CoinGold.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
        ) {
            // Progress Bar Fill
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animatedProgress)
                    .background(CoinGold.copy(alpha = 0.8f))
                    .clip(RoundedCornerShape(8.dp))
            )
            // Progress Text
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "$currentValue / $maxValue",
                    color = DarkBackground,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = Oswald
                )
            }
        }
    }
}
// -------------------------------------------------------------
// --- HELPER FUNCTIONS (Drag and Placement Logic)
// -------------------------------------------------------------

fun calculateGridPosition(dragPos: Offset, gridTopLeft: Offset, gridSizePx: Float, gridSize: Int): Pair<Int, Int>? {
    val relativeX = dragPos.x - gridTopLeft.x
    val relativeY = dragPos.y - gridTopLeft.y

    if (relativeX < 0 || relativeX > gridSizePx || relativeY < 0 || relativeY > gridSizePx) return null

    val cellSize = gridSizePx / gridSize
    val col = (relativeX / cellSize).toInt().coerceIn(0, gridSize - 1)
    val row = (relativeY / cellSize).toInt().coerceIn(0, gridSize - 1)
    return Pair(row, col)
}

fun isValidPlacement(board: GameGrid, block: Block, origin: Pair<Int, Int>): Boolean {
    // SPECIAL: If it's the Rainbow Block, it ignores collisions (only checks bounds)
    val ignoreCollision = block.isSpecial // Uses boolean flag from model

    if (origin.first + block.boundingBoxHeight > 9 || origin.second + block.boundingBoxWidth > 9) return false

    block.shape.forEach { coord ->
        val r = origin.first + coord.row
        val c = origin.second + coord.col
        if (r < 0 || r >= 9 || c < 0 || c >= 9) return false

        // If it's NOT special, check collision. If it IS special, skip this check.
        if (!ignoreCollision && board[r][c] != null) return false
    }
    return true
}


// -------------------------------------------------------------
// --- VISUAL COMPONENTS (Continued)
// -------------------------------------------------------------


@Composable
fun ScoreDisplay(score: Int, label: String, modifier: Modifier = Modifier, showTrophy: Boolean = false, trophyTint: Color = LightText) {
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
            Text(text = score.toString(), color = LightText, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, fontFamily = Oswald)
        }
        Text(text = label, color = LightText.copy(alpha = 0.7f), fontSize = 14.sp, fontFamily = Oswald)
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
            .clip(RoundedCornerShape(12.dp)) // Slightly rounder corners
            .background(BoardBackground)
            // Added subtle inner border for depth
            .border(BorderStroke(3.dp, Color.White.copy(alpha = 0.15f)), RoundedCornerShape(12.dp))
        // Optional: Add a shadow/glow effect here if desired (using elevation or custom draw modifier)
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
                val staggerDelay = ((r + c) * 30).toInt() // Reduced delay for faster animation
                val targetScale = if (isClearing) 0.0f else if (isOccupied) 1.0f else 1.0f // Simplified scale
                val animSpec = tween<Float>(durationMillis = 300, delayMillis = staggerDelay)
                val blockAlpha: Float by animateFloatAsState(targetValue = if (isClearing) 0.0f else 1.0f, animationSpec = animSpec, label = "BlockClearAlpha")

                var isGhostCell = false
                if (ghostBlock != null && ghostOrigin != null) {
                    val relativeR = r - ghostOrigin.first
                    val relativeC = c - ghostOrigin.second
                    isGhostCell = ghostBlock.shape.any { it.row == relativeR && it.col == relativeC }
                }

                val ghostColor = when {
                    isGhostCell -> if (isGhostValid) SuccessGreen.copy(alpha = 0.35f) else Color.Red.copy(alpha = 0.35f)
                    else -> BoardBackground
                }

                Box(
                    modifier = Modifier
                        .size(cellDp)
                        .padding(BLOCK_PADDING)
                        .clip(RoundedCornerShape(4.dp))
                        .background(ghostColor)
                        .border(BorderStroke(1.dp, DarkBackground.copy(0.4f)), RoundedCornerShape(4.dp)) // Grid lines
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
                                .graphicsLayer(alpha = blockAlpha) // Apply fade animation
                        )
                    }
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
    onDragEnd: () -> Unit
) {
    val blocks = uiState.availableBlocks
    val selectedBlock = uiState.selectedBlock

    Row(
        modifier = Modifier.fillMaxWidth(), // Removed padding(top=8.dp)
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
                // Pass new, scaled cell size (18dp * 1.5 = 27dp)
                BlockPreviewCard(block, isSelected, onSelectBlock, cellSize = 27.dp)
            }
        }
    }
}

@Composable
fun BlockPreviewCard(block: Block, isSelected: Boolean, onClick: (Block) -> Unit, cellSize: Dp) {
    val borderColor = if (isSelected) Color.White else Color.Gray.copy(alpha = 0.3f)
    val borderWidth = if (isSelected) 3.dp else 1.dp

    // Increased elevation and surface color for better contrast
    Card(
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(borderWidth, borderColor),
        colors = CardDefaults.cardColors(containerColor = DeepBlue.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 8.dp else 4.dp),
        modifier = Modifier
            .wrapContentSize()
            .padding(4.dp)
            .clickable { onClick(block) }
    ) {
        BlockShapeDisplay(block, cellSize)
    }
}

@Composable
fun BlockShapeDisplay(block: Block, cellSize: Dp) {
    val rows = block.boundingBoxHeight
    val cols = block.boundingBoxWidth

    Column(modifier = Modifier.padding(6.dp)) { // Slightly increased inner padding
        for (r in 0 until rows) {
            Row {
                for (c in 0 until cols) {
                    val isPresent = block.shape.any { it.row == r && it.col == c }
                    Box(
                        modifier = Modifier
                            .size(cellSize)
                            .padding(BLOCK_PADDING)
                            .clip(RoundedCornerShape(4.dp))
                        // Removed transparent background/border logic, relying only on texture
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
                        } else {
                            // Empty cells in the bounding box
                            Spacer(modifier = Modifier.fillMaxSize().background(Color.Transparent))
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomBar(
    uiState: GameUiState,
    onRotateBlock: () -> Unit,
    onSelectRainbow: () -> Unit,
    onGridCellClicked: (row: Int, col: Int) -> Unit,
    onDragStart: (Block, previewCardOffset: Offset) -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit
) {
    // Toolbar Container
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(DeepBlue.copy(alpha = 0.6f))
            .padding(vertical = 3.dp, horizontal = 24.dp), // Reduced vertical padding to 3.dp
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. Rotation Button
        RotationButtonWithCost(uiState = uiState, onRotateBlock = onRotateBlock)

        // 2. Rainbow Special Block Button (Centered in the bar)
        SpecialBlockButton(
            count = uiState.rainbowBlockCount,
            isSelected = uiState.selectedBlock?.id == RAINBOW_BLOCK.id,
            onClick = { onSelectRainbow() },
            onDragStart = { offset ->
                onSelectRainbow()
                onDragStart(RAINBOW_BLOCK, offset)
            },
            onDrag = onDrag,
            onDragEnd = onDragEnd
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RotationButtonWithCost(uiState: GameUiState, onRotateBlock: () -> Unit) {
    val selectedBlock = uiState.selectedBlock
    val isRotationEnabled = selectedBlock != null
    // Size is 75.dp to match SpecialBlockButton
    val buttonSize = 55.dp

    val rotationText = remember(selectedBlock, uiState.lastRotatedBlockId, uiState.freeRotations) {
        when {
            selectedBlock?.id == uiState.lastRotatedBlockId -> "FREE" // Already paid
            uiState.freeRotations > 0 -> "${uiState.freeRotations} LEFT"
            else -> "10C" // Cost to rotate
        }
    }

    val rotationBadgeColor = when {
        selectedBlock?.id == uiState.lastRotatedBlockId -> SuccessGreen
        uiState.freeRotations > 0 -> SuccessGreen
        isRotationEnabled -> Color.Red // If enabled but costs coins
        else -> Color.Gray.copy(alpha = 0.3f)
    }

    val rotationIconTint = if (isRotationEnabled) LightText else LightText.copy(alpha = 0.4f)
    val buttonContainerColor = if (isRotationEnabled) DeepBlue else Color.Gray.copy(alpha = 0.3f)

    CompositionLocalProvider(LocalMinimumInteractiveComponentEnforcement provides false) {
        Button(
            onClick = onRotateBlock,
            enabled = isRotationEnabled,
            modifier = Modifier
                .size(buttonSize) // Use 75.dp size
                .scale(GameSettings.rotateButtonScale.value),
            contentPadding = PaddingValues(0.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = buttonContainerColor,
                contentColor = LightText,
                disabledContainerColor = Color.Gray.copy(alpha = 0.3f),
                disabledContentColor = LightText.copy(alpha = 0.4f)
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(R.drawable.rotate_right), // Assuming this is a proper rotate icon
                    contentDescription = "Rotate Block",
                    modifier = Modifier.size(36.dp),
                    tint = rotationIconTint
                )

                // Dedicated Badge Overlay
                if (isRotationEnabled) {
                    Surface(
                        color = rotationBadgeColor,
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 6.dp, y = (-6).dp) // Moved further out
                            .zIndex(2f)
                    ) {
                        Text(
                            text = rotationText,
                            color = LightText,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = Oswald,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 0.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SpecialBlockButton(
    count: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    onDragStart: (Offset) -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit
) {
    if (count <= 0) return

    var buttonPos by remember { mutableStateOf(Offset.Zero) }

    // Increased size and prominence for a power-up button
    val buttonSize = 55.dp

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.onGloballyPositioned { buttonPos = it.positionInRoot() }
    ) {
        // Count Badge
        Surface(
            color = CoinGold,
            shape = RoundedCornerShape(50),
            modifier = Modifier.zIndex(2f)
        ) {
            Text(
                text = "x$count",
                color = DarkBackground,
                fontSize = 10.sp, // Larger badge font
                fontWeight = FontWeight.Bold,
                fontFamily = Oswald,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
            )
        }

        Spacer(modifier = Modifier.height(4.dp)) // Small space between badge and button

        Box(
            modifier = Modifier
                .size(buttonSize)
                .clip(RoundedCornerShape(16.dp)) // Rounder corners
                .background(if (isSelected) CoinGold else SpecialPurple)
                .border(3.dp, if (isSelected) Color.White else Color.White.copy(0.3f), RoundedCornerShape(16.dp)) // Thicker border
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { onDragStart(buttonPos) },
                        onDrag = { change, dragAmount -> change.consume(); onDrag(dragAmount) },
                        onDragEnd = { onDragEnd() }
                    )
                }
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            // Rainbow Block Preview (Reduced scale from 3.0f to 2.0f for better fit)
            Box(
                modifier = Modifier
                    .scale(2.0f)
            ) {
                BlockShapeDisplay(RAINBOW_BLOCK, 18.dp)
            }
        }
    }
}

@Composable
fun GameOverDialog(
    score: Int,
    highScore: Int,
    onRestart: () -> Unit,
    onMenu: () -> Unit
) {
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = DeepBlue),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(8.dp),
            border = BorderStroke(2.dp, Color.White.copy(alpha = 0.1f))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("GAME OVER", color = LightText, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, fontFamily = Oswald)
                Spacer(modifier = Modifier.height(24.dp))
                Text("Score", color = LightText.copy(alpha = 0.7f), fontSize = 14.sp, fontFamily = Oswald)
                Text(text = score.toString(), color = LightText, fontSize = 48.sp, fontWeight = FontWeight.Bold, fontFamily = Oswald)
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.EmojiEvents, contentDescription = "High Score", tint = CoinGold, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Best: $highScore", color = CoinGold, fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = Oswald)
                }
                Spacer(modifier = Modifier.height(32.dp))
                Button(onClick = onRestart, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)), modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(12.dp)) {
                    Icon(Icons.Filled.Refresh, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("PLAY AGAIN", fontWeight = FontWeight.Bold, fontFamily = Oswald)
                }
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(onClick = onMenu, colors = ButtonDefaults.outlinedButtonColors(contentColor = LightText), border = BorderStroke(1.dp, LightText.copy(alpha = 0.5f)), modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(12.dp)) {
                    Icon(Icons.Filled.Home, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("MAIN MENU", fontWeight = FontWeight.Bold, fontFamily = Oswald)
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GameScreenPreview() {
    val sampleBoard = Array(9) { r -> Array<Int?>(9) { c -> if (r == 8) R.drawable.blue else null } }
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
        selectedBlock = sampleBlocks[0],
        rainbowBlockCount = 3
    )
    MaterialTheme {
        GameScreen(
            uiState = sampleUi,
            cellDp = DEFAULT_CELL_SIZE,
            onGridCellClicked = {_,_ ->},
            onSelectBlock = {},
            onRotateBlock = {},
            onSelectRainbow = {},
            onReset = {},
            onGoToMenu = {}
        )
    }
}