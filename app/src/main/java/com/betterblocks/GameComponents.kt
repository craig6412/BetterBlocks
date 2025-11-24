package com.betterblocks.ui

import android.R.attr.y
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import com.betterblocks.*
import kotlin.math.roundToInt
import kotlin.random.Random
import com.betterblocks.R
import androidx.compose.foundation.layout.Box
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorFilter.Companion.tint
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Shape


import kotlin.math.roundToInt

// --- CONSTANTS & COLORS ---
val DeepBlue = Color(0xFF282C6D)
val DarkBackground = Color(0xFF1E214A)
val BoardBackground = Color(0xFF2B2F5D)
val LightText = Color(0xFFFFFFFF)
val CoinGold = Color(0xFFFFD700)
val SpecialPurple = Color(0xFF9C27B0)
val SuccessGreen = Color(0xFF4CAF50)

val Magenta = Color(0xFFFF00FF)

val Oswald = FontFamily(Font(R.font.oswald_regular))

// --- VISUAL ADJUSTMENT KNOBS ---
var DEFAULT_CELL_SIZE: Dp = 38.dp
var SCREEN_HORIZONTAL_PADDING: Dp = 16.dp


// --- DETAILED TUNING KNOBS ---

// 1. MAIN BOARD SETTINGS
// Gap between blocks on the main grid (0.dp = touching)
var BOARD_CELL_PADDING: Dp = 0.3.dp
// Roundness of blocks on the main grid
var BOARD_CORNER_RADIUS: Dp = 4.dp
// Thickness of the faint grid lines (0.dp = no lines)
var BOARD_BORDER_WIDTH: Dp = 0.dp

// 2. TEXTURE SCALING
// Scales the PNG inside the cell to ensure no transparent edges are visible
var BLOCK_TEXTURE_SCALE: Float = 1.10f

// 3. PREVIEW / DRAG SETTINGS
var PREVIEW_CELL_PADDING: Dp = .5.dp
var PREVIEW_CORNER_RADIUS: Dp = 4.dp

// --- DRAG OFFSET KNOB ---
var DRAG_OFFSET_Y: Dp = (45).dp
var DRAG_OFFSET_X: Dp = 12.dp

// -------------------------------------------------------------
// --- TOP UI COMPONENTS (Header, Score, Meter)
// -------------------------------------------------------------

@Composable
fun Header(uiState: GameUiState, onReset: () -> Unit, onMenuClicked: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
            .background(DeepBlue)
            .padding(
                top = GameSettings.headerVerticalPadding.value.dp,
                bottom = GameSettings.headerVerticalPadding.value.dp,
                start = 16.dp,
                end = 16.dp
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Row: Menu Button | Spacer | Coins
        Box(modifier = Modifier.fillMaxWidth().padding(bottom = 2.dp)) {
            IconButton(
                onClick = onMenuClicked,
                modifier = Modifier.size(36.dp).align(Alignment.CenterStart)
            ) {
                Icon(
                    imageVector = Icons.Filled.Menu,
                    contentDescription = "Menu",
                    tint = LightText.copy(alpha = 0.8f)
                )
            }

            Surface(
                color = CoinGold.copy(alpha = 0.1f),
                shape = RoundedCornerShape(50),
                border = BorderStroke(1.dp, CoinGold),
                modifier = Modifier.align(Alignment.CenterEnd)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.MonetizationOn,
                        contentDescription = "Coins",
                        tint = CoinGold,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = uiState.coins.toString(),
                        color = LightText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = Oswald
                    )
                }
            }
        }

        // Special Meter
        SpecialMeterDisplay(currentValue = uiState.specialMeterValue, maxValue = 5)

        // Score Row
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 2.dp, bottom = 2.dp, start = 4.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            ScoreDisplay(score = uiState.score, label = "CURRENT", modifier = Modifier.weight(1f))
            ScoreDisplay(score = uiState.highScore, label = "HIGH SCORE", showTrophy = true, trophyTint = CoinGold, modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.width(36.dp)) // Balancing spacer
        }
    }
}

@Composable
fun Modifier.safeClickable(onClick: () -> Unit): Modifier =
    this.clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = rememberRipple(),
        onClick = onClick
    )

@Composable
fun ScoreDisplay(score: Int, label: String, modifier: Modifier = Modifier, showTrophy: Boolean = false, trophyTint: Color = LightText) {
    Column(
        modifier = modifier.padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (showTrophy) {
                Icon(
                    imageVector = Icons.Filled.EmojiEvents,
                    contentDescription = "Trophy",
                    tint = trophyTint,
                    modifier = Modifier
                        .size(16.dp)
                        .padding(end = 2.dp)
                )
            }
            Text(text = score.toString(), color = LightText, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, fontFamily = Oswald)
        }
        Text(text = label, color = LightText.copy(alpha = 0.7f), fontSize = 10.sp, fontFamily = Oswald)
    }
}

@Composable
fun SpecialMeterDisplay(currentValue: Int, maxValue: Int) {
    val progress = currentValue.toFloat() / maxValue.toFloat()
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 300),
        label = "MeterProgress"
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "SPECIAL CHARGE",
            color = LightText.copy(alpha = 0.9f),
            fontSize = 10.sp,
            fontFamily = Oswald,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 2.dp)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color.Black.copy(alpha = 0.5f))
                .border(1.dp, CoinGold.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animatedProgress)
                    .background(CoinGold.copy(alpha = 0.8f))
                    .clip(RoundedCornerShape(4.dp))
            )
        }
    }
}

// -------------------------------------------------------------
// --- GAME BOARD & ANIMATION COMPONENTS
// -------------------------------------------------------------

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
    val isFullBoardClearing = uiState.clearingCells.size == (GRID_SIZE * GRID_SIZE)

    Box(
        modifier = Modifier
            .width(totalBoardSize)
            .height(totalBoardSize)
            .clip(RoundedCornerShape(8.dp))
            .background(BoardBackground)
            .border(BorderStroke(3.dp, Color.White.copy(alpha = 0.15f)), RoundedCornerShape(8.dp))
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

                // --- CASCADING ANIMATION LOGIC ---
                val duration = if (isFullBoardClearing) 150 else 300
                val staggerDelay = if (isFullBoardClearing) {
                    (r * 70).toInt()
                } else if (isClearing) {
                    ((r + c) * 30).toInt()
                } else {
                    0
                }

                val targetScale = if (isClearing) 0.0f else if (isOccupied) 1.0f else 1.0f
                val animSpec = tween<Float>(durationMillis = duration, delayMillis = staggerDelay)
                val blockAlpha by animateFloatAsState(targetValue = if (isClearing) 0.0f else 1.0f, animationSpec = animSpec, label = "BlockAlpha")
                val blockScale by animateFloatAsState(targetValue = targetScale, animationSpec = animSpec, label = "BlockScale")
                val blockRotation by animateFloatAsState(targetValue = if (isClearing) 45f else 0f, animationSpec = animSpec, label = "BlockRotate")

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
                        .padding(BOARD_CELL_PADDING)
                        .clip(RoundedCornerShape(BOARD_CORNER_RADIUS))
                        .background(ghostColor)
                        .border(
                            BorderStroke(
                                BOARD_BORDER_WIDTH,
                                if (isOccupied) Color.Transparent else DarkBackground.copy(0.4f)
                            ),
                            RoundedCornerShape(BOARD_CORNER_RADIUS)
                        )
                        .safeClickable { onCellClick(r, c) }
                ) {
                    if (isOccupied && cellValue != null) {
                        Image(
                            painter = painterResource(id = cellValue),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    // UPDATED: Applies texture scale to overfill cell slightly
                                    val textureScale = BLOCK_TEXTURE_SCALE
                                    scaleX = blockScale * textureScale
                                    scaleY = blockScale * textureScale
                                    rotationZ = blockRotation
                                    alpha = blockAlpha
                                }
                                .scale(BLOCK_TEXTURE_SCALE)
                        )
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// --- CONTROL COMPONENTS (Blocks, Bottom Bar)
// -------------------------------------------------------------

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
                        cardAbsOffset = coordinates.localToWindow(Offset.Zero)
                    }
                    .pointerInput(block) {
                        detectDragGestures(

                            onDragStart = { onDragStart(block, cardAbsOffset) },
                            onDrag = { change, dragAmount -> change.consume(); onDrag(dragAmount) },
                            onDragEnd = { onDragEnd() }
                        )
                    }
                    .safeClickable { onSelectBlock(block) }
            ) {
                val scale = GameSettings.availableBlockScale.value
                BlockPreviewCard(block, isSelected, onSelectBlock, cellSize = 18.dp * scale)
            }
        }
    }
}

@Composable
fun BlockPreviewCard(block: Block, isSelected: Boolean, onClick: (Block) -> Unit, cellSize: Dp) {
    val borderColor = if (isSelected) Color.White else Color.Gray.copy(alpha = 0.3f)
    val borderWidth = if (isSelected) 3.dp else 1.dp

    Card(
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(borderWidth, borderColor),
        colors = CardDefaults.cardColors(containerColor = DeepBlue.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 8.dp else 4.dp),
        modifier = Modifier
            .wrapContentSize()
            .padding(4.dp)
            .safeClickable { onClick(block) }
    ) {
        BlockShapeDisplay(block, cellSize)
    }
}

@Composable
fun BlockShapeDisplay(block: Block, cellSize: Dp) {
    Column(modifier = Modifier.padding(6.dp)) {
        for (r in 0 until block.boundingBoxHeight) {
            Row {
                for (c in 0 until block.boundingBoxWidth) {
                    val isPresent = block.shape.any { it.row == r && it.col == c }
                    Box(
                        modifier = Modifier
                            .size(cellSize)
                            .padding(PREVIEW_CELL_PADDING)
                            .clip(RoundedCornerShape(PREVIEW_CORNER_RADIUS))
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
                            Spacer(modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Transparent))
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
    onUseRainbowImmediately: () -> Unit,
    onColorWipeClick: () -> Unit,
    onDragStart: (Block, previewCardOffset: Offset) -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit
) {

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
            .height(85.dp)
    ) {

        // -------------------------------------------------
        // BACKGROUND (this is the only thing that gets blurred)
        // -------------------------------------------------
        Box(
            modifier = Modifier
                .matchParentSize()
                .blur(20.dp)  // Blur ONLY this layer
                .background(DeepBlue.copy(alpha = 0.55f))
        )

        // -------------------------------------------------
        // CONTENT (this stays sharp and untouched)
        // -------------------------------------------------
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(85.dp),  // keep the same height
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {

            // 1. Rotate Button
            RotationButtonWithCost(
                uiState = uiState,
                onRotateBlock = onRotateBlock
            )

            // 2. Color Wipe Button
            ColorWipeButton(
                count = uiState.colorWipeCount,
                onClick = onColorWipeClick
            )

            // 3. Rainbow Special Block Button
            SpecialBlockButton(
                count = uiState.rainbowBlockCount,
                isSelected = uiState.selectedBlock?.id == RAINBOW_BLOCK.id,
                onClick = { onUseRainbowImmediately() },
                onDragStart = { offset ->
                    onSelectRainbow()
                    onDragStart(RAINBOW_BLOCK, offset)
                },
                onDrag = onDrag,
                onDragEnd = onDragEnd
            )
        }
    }
}
//new texture idea for colorwipe maybe it will work maybe not..

// --- NEW: Color Wipe Button Component ---
@Composable
fun ColorWipeButton(
    count: Int,
    onClick: () -> Unit
) {
    // Static button: Always visible even if count is 0

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
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
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = Oswald,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.5.dp)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Button
        Box(
            modifier = Modifier
                .size(GameSettings.bottomBarButtonSize.value.dp)
                .scale(GameSettings.bottomBarIconScale.value)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF7A2DF5)) // Purple BG
                .border(
                    width = 3.dp,
                    color = Color.White.copy(alpha = 0.35f),
                    shape = RoundedCornerShape(16.dp)
                )
                .safeClickable { onClick() },
            contentAlignment = Alignment.Center
        ) {

            // ⬇️ The new palette icon from drawable
            Image(
                painter = painterResource(id = R.drawable.ic_palette_colorwipe),
                contentDescription = "Color Wipe",
                modifier = Modifier
                    .fillMaxSize(0.75f)      // scale inside button
                    .aspectRatio(1f),
                contentScale = ContentScale.Fit
            )
        }
    }
}

// --- NEW: Color Wheel Dialog Component ---
@Composable
fun ColorWheelDialog(
    onDismiss: () -> Unit,
    onSpinFinished: (Int) -> Unit // Returns the BLOCK_DRAWABLE index (0-6)
) {
    // Map indices to approximate Colors for the wheel visual
    val segmentColors = listOf(
        Color(0xFF2196F3), // Blue
        Color(0xFF4CAF50), // Green
        Color(0xFFE91E63), // Pink
        Color(0xFFFF9800), // Orange
        Color(0xFF9C27B0), // Purple
        Color(0xFFF44336), // Red
        Color(0xFFFFEB3B)  // Yellow
    )

    var isSpinning by remember { mutableStateOf(false) }
    val rotation = remember { Animatable(0f) }

    // We pre-determine the result so we know where to stop the wheel
    val targetIndex = (0..6).random()

    LaunchedEffect(isSpinning) {
        if (isSpinning) {
            val segmentAngle = 360f / 7f
            // Target rotation logic: 5 spins + adjustment to land index at 270 deg (top)
            val targetRotation = 360f * 5 + (270f - (targetIndex * segmentAngle) - (segmentAngle / 2))

            rotation.animateTo(
                targetValue = targetRotation,
                animationSpec = tween(durationMillis = 3000, easing = FastOutSlowInEasing)
            )

            // Animation done, trigger the action
            kotlinx.coroutines.delay(500)
            onSpinFinished(targetIndex)
        }
    }

    Dialog(onDismissRequest = { if (!isSpinning) onDismiss() }) {
        Card(
            colors = CardDefaults.cardColors(containerColor = DeepBlue),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(2.dp, CoinGold)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "COLOR GAMBLE",
                    color = LightText,
                    fontSize = 24.sp,
                    fontFamily = Oswald,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Spin to destroy all blocks of one color!",
                    color = LightText.copy(0.7f),
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    fontFamily = Oswald
                )

                Spacer(modifier = Modifier.height(32.dp))

                // --- THE WHEEL ---
                Box(contentAlignment = Alignment.Center) {
                    Canvas(modifier = Modifier.size(200.dp)) {
                        val strokeWidth = 2.dp.toPx()

                        rotate(rotation.value) {
                            val anglePerSegment = 360f / 7f
                            segmentColors.forEachIndexed { index, color ->
                                drawArc(
                                    color = color,
                                    startAngle = index * anglePerSegment,
                                    sweepAngle = anglePerSegment,
                                    useCenter = true,
                                    size = size
                                )
                                // Draw segment borders
                                drawArc(
                                    color = DeepBlue,
                                    startAngle = index * anglePerSegment,
                                    sweepAngle = anglePerSegment,
                                    useCenter = true,
                                    style = Stroke(width = strokeWidth),
                                    size = size
                                )
                            }
                        }
                    }

                    // The "Center Pivot" Object
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = CoinGold,
                        border = BorderStroke(2.dp, Color.White),
                        modifier = Modifier.size(40.dp),
                        shadowElevation = 8.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("?", color = DeepBlue, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        }
                    }

                    // The "Clicker/Pointer" at the top - CHANGED TO BLACK ARROW
                    Icon(
                        imageVector = Icons.Filled.ArrowDropDown, // Arrow pointing down
                        contentDescription = null,
                        tint = Color.Black, // Black arrow
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .offset(y = (-12).dp)
                            .size(48.dp) // Slightly larger for visibility
                            .zIndex(2f)
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                if (!isSpinning) {
                    Button(
                        onClick = { isSpinning = true },
                        colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("SPIN (1 ITEM)", fontWeight = FontWeight.Bold, fontFamily = Oswald)
                    }
                } else {
                    Text(
                        "SPINNING...",
                        color = CoinGold,
                        fontSize = 18.sp,
                        fontFamily = Oswald,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RotationButtonWithCost(uiState: GameUiState, onRotateBlock: () -> Unit) {
    val selectedBlock = uiState.selectedBlock
    val isRotationEnabled = selectedBlock != null
    val buttonSize = 55.dp

    val rotationText = remember(selectedBlock, uiState.lastRotatedBlockId, uiState.freeRotations) {
        when {
            selectedBlock?.id == uiState.lastRotatedBlockId -> "FREE"
            uiState.freeRotations > 0 -> "${uiState.freeRotations} LEFT"
            else -> "10C"
        }
    }

    val rotationBadgeColor = when {
        selectedBlock?.id == uiState.lastRotatedBlockId -> SuccessGreen
        uiState.freeRotations > 0 -> SuccessGreen
        isRotationEnabled -> Color.Red
        else -> Color.Gray.copy(alpha = 0.3f)
    }

    val rotationIconTint = if (isRotationEnabled) LightText else LightText.copy(alpha = 0.4f)
    val buttonContainerColor = if (isRotationEnabled) DeepBlue else Color.Gray.copy(alpha = 0.3f)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Badge ABOVE button
        Surface(
            color = CoinGold,
            shape = RoundedCornerShape(50),
            modifier = Modifier.zIndex(2f)
        ) {
            Text(
                text = "x${uiState.freeRotations}",
                color = DarkBackground,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = Oswald,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.5.dp)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // The actual rotation button
        Box(
            modifier = Modifier
                .size(GameSettings.bottomBarButtonSize.value.dp)
                .scale(GameSettings.bottomBarIconScale.value)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF4C4E5A))
                .border(
                    width = 3.dp,
                    color = Color.White.copy(alpha = 0.35f),
                    shape = RoundedCornerShape(16.dp)
                )
                .safeClickable { if (isRotationEnabled) onRotateBlock() },
            contentAlignment = Alignment.Center
        ) {

            Image(
                painter = painterResource(id = R.drawable.rotate_right),
                contentDescription = "Rotate Block",
                modifier = Modifier
                    .fillMaxSize(0.75f)
                    .aspectRatio(1f),
                colorFilter = tint(rotationIconTint),
                contentScale = ContentScale.Fit
            )
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
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = Oswald,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.5.dp)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Box(
                modifier = Modifier
                    .size(GameSettings.bottomBarButtonSize.value.dp)
                    .scale(GameSettings.bottomBarIconScale.value)

                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isSelected) CoinGold else SpecialPurple)
                    .border(
                        3.dp,
                        if (isSelected) Color.White else Color.White.copy(0.3f),
                        RoundedCornerShape(16.dp)
                    )
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { onDragStart(buttonPos) },
                            onDrag = { change, dragAmount -> change.consume(); onDrag(dragAmount) },
                            onDragEnd = { onDragEnd() }
                        )
                    }
                    .safeClickable { onClick() },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier.scale(2.0f * GameSettings.bottomBarIconScale.value)
                ) {
                    BlockShapeDisplay(RAINBOW_BLOCK, 18.dp)


                }
            }
        }
    }

// -------------------------------------------------------------
// --- DIALOGS
// -------------------------------------------------------------

    @Composable
    fun GameMenuDialog(
        uiState: GameUiState,
        onDismiss: () -> Unit,
        onRestart: () -> Unit,
        onGoToMenu: () -> Unit,
        onToggleSound: () -> Unit,
        onToggleMusic: () -> Unit
    ) {
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = DeepBlue),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(8.dp),
                border = BorderStroke(2.dp, Color.White.copy(alpha = 0.1f))
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .width(IntrinsicSize.Max),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "GAME MENU",
                        color = LightText,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = Oswald
                    )
                    Spacer(modifier = Modifier.height(24.dp))

                    MenuToggleRow(
                        label = "SOUND EFFECTS",
                        checked = uiState.isSoundEnabled,
                        onClick = onToggleSound
                    )
                    MenuToggleRow(
                        label = "MUSIC",
                        checked = uiState.isMusicEnabled,
                        onClick = onToggleMusic
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = onRestart,
                        colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Filled.Refresh, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("RESTART GAME", fontWeight = FontWeight.Bold, fontFamily = Oswald)
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = onGoToMenu,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = LightText),
                        border = BorderStroke(1.dp, LightText.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Filled.Home, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("MAIN MENU", fontWeight = FontWeight.Bold, fontFamily = Oswald)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = onGoToMenu,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                        border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("QUIT GAME", fontWeight = FontWeight.Bold, fontFamily = Oswald)
                    }
                }
            }
        }
    }


    @Composable
    fun MenuToggleRow(label: String, checked: Boolean, onClick: () -> Unit) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .safeClickable(onClick = onClick)
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                color = LightText,
                fontSize = 16.sp,
                fontFamily = Oswald,
                fontWeight = FontWeight.Medium
            )
            Switch(
                checked = checked,
                onCheckedChange = { onClick() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = SuccessGreen,
                    checkedTrackColor = SuccessGreen.copy(alpha = 0.5f),
                    uncheckedThumbColor = Color.Gray,
                    uncheckedTrackColor = Color.Gray.copy(alpha = 0.5f)
                )
            )
        }
    }

    @Composable
    fun LastChanceDialog(
        onUseRainbow: () -> Unit,
        onGameOver: () -> Unit
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
                    Text(
                        "LAST CHANCE!",
                        color = CoinGold,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = Oswald
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "No more moves left. Use your Rainbow Wipe to clear the board and keep playing?",
                        color = LightText.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center,
                        fontSize = 18.sp,
                        fontFamily = Oswald
                    )
                    Spacer(modifier = Modifier.height(32.dp))

                    Button(
                        onClick = onUseRainbow,
                        colors = ButtonDefaults.buttonColors(containerColor = SpecialPurple),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            "USE RAINBOW WIPE (1)",
                            fontWeight = FontWeight.Bold,
                            fontFamily = Oswald
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = onGameOver,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = LightText),
                        border = BorderStroke(1.dp, LightText.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("END GAME", fontWeight = FontWeight.Bold, fontFamily = Oswald)
                    }
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
                    Text(
                        "GAME OVER",
                        color = LightText,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = Oswald
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        "Score",
                        color = LightText.copy(alpha = 0.7f),
                        fontSize = 14.sp,
                        fontFamily = Oswald
                    )
                    Text(
                        text = score.toString(),
                        color = LightText,
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = Oswald
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.EmojiEvents,
                            contentDescription = "High Score",
                            tint = CoinGold,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Best: $highScore",
                            color = CoinGold,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = Oswald
                        )
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                    Button(
                        onClick = onRestart,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Filled.Refresh, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("PLAY AGAIN", fontWeight = FontWeight.Bold, fontFamily = Oswald)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = onMenu,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = LightText),
                        border = BorderStroke(1.dp, LightText.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Filled.Home, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("MAIN MENU", fontWeight = FontWeight.Bold, fontFamily = Oswald)
                    }
                }
            }
        }
    }


// -------------------------------------------------------------
// --- HELPER FUNCTIONS
// -------------------------------------------------------------

fun calculateGridPosition(dragPos: Offset, gridTopLeft: Offset, gridSizePx: Float, gridSize: Int): Pair<Int, Int>? {
    val relativeX = dragPos.x - gridTopLeft.x
    val relativeY = dragPos.y - gridTopLeft.y

    val cellSize = gridSizePx / gridSize

    // Tolerance: Allows drop if roughly 80% within bounds
    val tolerance = cellSize * 0.8f

    if (relativeX < -tolerance || relativeX > gridSizePx + tolerance ||
        relativeY < -tolerance || relativeY > gridSizePx + tolerance) {
        return null
    }

    // Uses roundToInt() for that "magnetic" center snap feel
    val col = (relativeX / cellSize).roundToInt().coerceIn(0, gridSize - 1)
    val row = (relativeY / cellSize).roundToInt().coerceIn(0, gridSize - 1)
    return Pair(row, col)
}

fun isValidPlacement(board: GameGrid, block: Block, origin: Pair<Int, Int>): Boolean {
    val ignoreCollision = block.isSpecial
    if (origin.first + block.boundingBoxHeight > 9 || origin.second + block.boundingBoxWidth > 9) return false

    block.shape.forEach { coord ->
        val r = origin.first + coord.row
        val c = origin.second + coord.col
        if (r < 0 || r >= 9 || c < 0 || c >= 9) return false
        if (!ignoreCollision && board[r][c] != null) return false
    }
    return true
}