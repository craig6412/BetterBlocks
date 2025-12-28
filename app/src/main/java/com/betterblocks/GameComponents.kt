package com.betterblocks

import android.R.attr.shape
import com.betterblocks.ui.detectSimpleDragOrTap
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import android.content.Intent
import androidx.compose.ui.platform.LocalContext
import com.betterblocks.BuildConfig
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import com.betterblocks.ui.sw
import com.betterblocks.ui.sh
import com.betterblocks.ui.sdp
import com.betterblocks.ui.ssp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import androidx.compose.foundation.layout.Box
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.ColorFilter.Companion.tint
import android.util.Log
import androidx.compose.foundation.LocalIndication
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.positionInWindow
import kotlinx.coroutines.delay
import com.betterblocks.model.TrophyTier
import com.betterblocks.model.drawableRes

// --- CONSTANTS & COLORS ---
val DeepBlue = Color(0xFF0B0123)           // Main Background: Midnight Void
val DarkBackground = Color(0xFF000000)     // Game Grid / Empty Slots: Abyssal Purple
val BoardBackground = Color(0xFF08060B)    // Match grid background to Abyssal Purple
val LightText = Color(0xFFF2E7FE)          // All Text/Labels: Ice White
val Pink_Jackie = Color(0xFF673AB7)        // Special Charge / accent: Neon Cyber-Violet
val SpecialPurple = Color(0xFF311B92)      // Power-up Buttons: Deep Indigo
val SuccessGreen = Color(0xFF2C78B7)       // Keep semantics but align to text color for now

val GoldCoin = Color(0xFFF2E7FE)           // Coins text/icons now Ice White for contrast



val Oswald = FontFamily(Font(R.font.oswald_regular))

// --- VISUAL ADJUSTMENT KNOBS ---
var DEFAULT_CELL_SIZE: Dp = 38.dp
var SCREEN_HORIZONTAL_PADDING: Dp = 16.dp // kept as fallback; composables will use sdp/sw


// --- DETAILED TUNING KNOBS ---

// 1. MAIN BOARD SETTINGS
// Scales the PNG inside the cell to ensure no transparent edges are visible
var BLOCK_TEXTURE_SCALE: Float = 1.16f

// 2. PREVIEW / DRAG SETTINGS
var PREVIEW_CELL_PADDING: Dp = .5.dp
var PREVIEW_CORNER_RADIUS: Dp = 4.dp



// -------------------------------------------------------------
// --- TOP UI COMPONENTS (Header, Score, Meter)
// -------------------------------------------------------------

@Composable
fun Header(uiState: GameUiState, onMenuClicked: () -> Unit) {
    // Ensure header padding is non-negative to avoid crash if developer knobs set a negative value
    val safeHeaderPaddingDp = GameSettings.headerVerticalPadding.floatValue.coerceAtLeast(0f).dp
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = sdp(0.02f), bottomEnd = sdp(0.02f)))
            .background(DeepBlue)
            .padding(
                top = safeHeaderPaddingDp,
                bottom = safeHeaderPaddingDp,
                start = sdp(0.02f),
                end = sdp(0.02f)
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Row: Menu Button | Spacer | Coins
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = sdp(0.003f))
        ) {
            IconButton(
                onClick = onMenuClicked,
                modifier = Modifier
                    .size(sdp(0.045f))
                    .align(Alignment.CenterStart)
            ) {
                Icon(
                    imageVector = Icons.Filled.Menu,
                    contentDescription = "Menu",
                    tint = LightText.copy(alpha = 0.8f)
                )
            }

            Surface(
                color = Pink_Jackie.copy(alpha = 0.1f),
                shape = RoundedCornerShape(50),
                border = BorderStroke(sdp(0.0015f), Pink_Jackie),
                modifier = Modifier.align(Alignment.CenterEnd)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = sdp(0.02f), vertical = sdp(0.003f))
                ) {
                    Icon(
                        imageVector = Icons.Filled.MonetizationOn,
                        contentDescription = "Coins",
                        tint = Pink_Jackie,
                        modifier = Modifier.size(sw(0.04f))
                    )
                    Spacer(modifier = Modifier.width(sdp(0.005f)))
                    Text(
                        text = uiState.coins.toString(),
                        color = LightText,
                        fontSize = ssp(0.035f),
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
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = sdp(0.003f), bottom = sdp(0.003f), start = sdp(0.005f), end = sdp(0.005f)),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            ScoreDisplay(score = uiState.score, label = "CURRENT", modifier = Modifier.weight(1f))
            ScoreDisplay(score = uiState.highScore, label = "HIGH SCORE", showTrophy = true, trophyTier = uiState.trophyTier, modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.width(sw(0.09f))) // Balancing spacer
        }
    }
}

@Composable
fun Modifier.safeClickable(onClick: () -> Unit): Modifier =
    this.clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = LocalIndication.current,
        onClick = onClick
    )

@Composable
fun ScoreDisplay(score: Int, label: String, modifier: Modifier = Modifier, showTrophy: Boolean = false, trophyTier: com.betterblocks.model.TrophyTier? = null) {
    Column(
        modifier = modifier.padding(horizontal = sdp(0.005f)),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (showTrophy && trophyTier != null) {
                Image(
                    painter = painterResource(com.betterblocks.trophyRes(trophyTier)),
                    contentDescription = "Trophy",
                    modifier = Modifier
                        .size(sdp(0.03f)) // slightly larger trophy icon to match larger score text
                        .padding(end = sdp(0.0035f)),
                    contentScale = ContentScale.Fit
                )
            }
            // Make the main score text much larger for readability
            Text(text = score.toString(), color = LightText, fontSize = ssp(0.06f), fontWeight = FontWeight.ExtraBold, fontFamily = Oswald)
        }
        Text(text = label, color = LightText.copy(alpha = 0.7f), fontSize = ssp(0.02f), fontFamily = Oswald)
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
            fontSize = ssp(0.022f),
            fontFamily = Oswald,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = sdp(0.003f))
        )
        Box(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(sdp(0.01f))
                .clip(RoundedCornerShape(sdp(0.005f)))
                .background(Color(0xFF2A0E45)) // Charge Bar (Empty Track): Dark Track
                .border(sdp(0.0015f), Pink_Jackie.copy(alpha = 0.7f), RoundedCornerShape(sdp(0.005f)))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animatedProgress)
                    .background(Color(0xFFD500F9)) // Special Charge Bar (Active): Neon Cyber-Violet
                    .clip(RoundedCornerShape(sdp(0.005f)))
            )
        }
    }
}

@Composable
fun BlockPreviewCard(
    block: Block,
    isSelected: Boolean = false,
    onClick: () -> Unit,
    onDragStart: (fingerInWindow: Offset) -> Unit,
    onDrag: (currentFingerPos: Offset) -> Unit,
    onDragEnd: () -> Unit,
    modifier: Modifier = Modifier
) {
    // The card's top-left corner in WINDOW coordinates
    var cardWindowPos by remember(block.id) { mutableStateOf(Offset.Zero) }

    val onStartState = rememberUpdatedState(onDragStart)
    val onDragState = rememberUpdatedState(onDrag)
    val onEndState = rememberUpdatedState(onDragEnd)

    // Buffer the initial down position; only fire onDragStart when movement actually begins
    var pendingStartWindowPos by remember { mutableStateOf<Offset?>(null) }

    // Use Pink_Jackie when selected, otherwise keep the faint lavender border
    val effectiveBorderColor = if (isSelected) Pink_Jackie else Color(0x407F5AF0)
    val backgroundColor = if (isSelected) Pink_Jackie.copy(alpha = 0.08f) else Color(0x264A148C)

    Card(
        shape = RoundedCornerShape(sdp(0.01f)),
        colors = CardDefaults.cardColors(containerColor = DarkBackground),
        elevation = CardDefaults.cardElevation(sdp(0.006f)),
        modifier = modifier
            .size(sw(0.22f))
            .onGloballyPositioned { coords ->
                // store window coordinates to match grid/window coordinates
                cardWindowPos = coords.positionInWindow()
            }
            .pointerInput(block.id) {
                detectSimpleDragOrTap(
                    onDragStart = { localPos ->
                        // Buffer the down position; don't call parent onDragStart yet
                        pendingStartWindowPos = cardWindowPos + localPos
                    },
                    onDrag = { localPos ->
                        val windowPos = cardWindowPos + localPos
                        // If we have a buffered start, this is the first movement -> now start drag
                        pendingStartWindowPos?.let { startPos ->
                            onStartState.value(startPos)
                            pendingStartWindowPos = null
                        }
                        onDragState.value(windowPos)
                    },
                    onDragEnd = {
                        // Clear any pending start and forward end
                        pendingStartWindowPos = null
                        onEndState.value()
                    },
                    onTap = {
                        // Tap cancels any pending start and becomes a click
                        pendingStartWindowPos = null
                        onClick()
                    }
                )
            }
    ) {
        val shape = RoundedCornerShape(sdp(0.01f))

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor, shape)
                .border(if (isSelected) sdp(0.004f) else sdp(0.003f), effectiveBorderColor, shape)
                .padding(sdp(0.008f)),
             contentAlignment = Alignment.Center
         ) {
            BlockGrid(
                block = block,
                cellSize = sdp(0.03f),
                modifier = Modifier.graphicsLayer(
                    scaleX = 0.9f,
                    scaleY = 0.9f
                )
             )
         }
     }
}

// kotlin
// File: `app/src/main/java/com/betterblocks/GameComponents.kt`
// Modified: SpecialBlockButton - use positionInWindow()
@Composable
fun BlockGrid(
    block: Block,
    cellSize: Dp,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        for (r in 0 until block.boundingBoxHeight) {
            Row {
                for (c in 0 until block.boundingBoxWidth) {

                    val isPresent = block.shape.any { it.row == r && it.col == c }

                    Box(
                        modifier = Modifier
                            .size(cellSize)
                            .padding(sdp(0.0015f))
                            .clip(RoundedCornerShape(sdp(0.005f)))
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
                             Spacer(
                                 modifier = Modifier
                                     .fillMaxSize()
                                     .background(Color.Transparent)
                             )
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
    onUseRainbowImmediately: () -> Unit,
    onColorWipeClick: () -> Unit,
    onDragStart: (Block, previewCardOffset: Offset) -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit
) {

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = sdp(0.035f), topEnd = sdp(0.035f)))
            .height(sh(0.09f)) // increased bottom bar height
    ) {

        // -------------------------------------------------
        // BACKGROUND (this is the only thing that gets blurred)
        // -------------------------------------------------
        Box(
            modifier = Modifier
                .matchParentSize()
                .blur(sdp(0.03f))  // Blur ONLY this layer
                .background(DeepBlue.copy(alpha = 0.55f))
        )

        // -------------------------------------------------
        // CONTENT (this stays sharp and untouched)
        // -------------------------------------------------
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(sh(0.09f)),
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
                onDragStart = { block, offset ->  // ✅ Now receives Block
                    onSelectRainbow()
                    onDragStart(block, offset)  // ✅ Pass both to parent
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
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            color = Pink_Jackie,
            shape = RoundedCornerShape(50),
            modifier = Modifier.zIndex(2f)
        ) {
            Text(
                text = "x$count",
                color = DarkBackground,
                fontSize = ssp(0.02f),
                fontWeight = FontWeight.Bold,
                fontFamily = Oswald,
                modifier = Modifier.padding(horizontal = sdp(0.015f), vertical = sdp(0.003f))
            )
        }

        Spacer(modifier = Modifier.height(sdp(0.004f)))

        Box(
            modifier = Modifier
                .size(GameSettings.bottomBarButtonSize.value.coerceAtLeast(1).dp)
                .scale(GameSettings.bottomBarIconScale.value.coerceAtLeast(0.1f))
                .clip(RoundedCornerShape(sdp(0.02f)))
                .background(Color(0x4D4A148C)) // Piece Container / Holder: Purple Glass (30% opacity)
                .border(
                    width = sdp(0.003f),
                    color = Color(0x407F5AF0),   // Grid Lines / accent: Faint Lavender (25% opacity)
                    shape = RoundedCornerShape(sdp(0.02f))
                )
                .safeClickable { onClick() },
            contentAlignment = Alignment.Center
        ) {

            Image(
                painter = painterResource(id = R.drawable.ic_palette_colorwipe),
                contentDescription = "Color Wipe",
                modifier = Modifier
                    .fillMaxSize(0.75f)
                    .aspectRatio(1f),
                contentScale = ContentScale.Fit
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RotationButtonWithCost(
    uiState: GameUiState,
    onRotateBlock: () -> Unit
) {
    val selectedBlock = uiState.selectedBlock
    val isRotationEnabled = selectedBlock != null

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        // ---------- TOP BADGE ----------
        Surface(
            color = Pink_Jackie,
            shape = RoundedCornerShape(50),
            modifier = Modifier.zIndex(2f)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(
                    horizontal = sdp(0.015f),
                    vertical = sdp(0.003f)
                )
            ) {
                if (uiState.freeRotations > 0) {
                    Text(
                        text = "x${uiState.freeRotations}", // ✅ FIXED
                        color = DarkBackground,
                        fontSize = ssp(0.025f),
                        fontWeight = FontWeight.Bold,
                        fontFamily = Oswald
                    )
                } else {
                    Text(
                        text = ROTATION_COST.toString(),
                        color = DarkBackground,
                        fontSize = ssp(0.025f),
                        fontWeight = FontWeight.Bold,
                        fontFamily = Oswald
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Image(
                        painter = painterResource(id = R.drawable.shop_coins_small),
                        contentDescription = "Coins",
                        modifier = Modifier.size(14.dp),
                        contentScale = ContentScale.Fit
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(sdp(0.004f)))

        // ---------- BUTTON ----------
        Box(
            modifier = Modifier
                .size(GameSettings.bottomBarButtonSize.value.coerceAtLeast(1).dp)
                .scale(GameSettings.bottomBarIconScale.value.coerceAtLeast(0.1f))
                .clip(RoundedCornerShape(sdp(0.02f)))
                .background(
                    if (isRotationEnabled)
                        Color(0xFF311B92)
                    else
                        Color(0xFF311B92).copy(alpha = 0.4f) // ✅ Disabled clarity
                )
                .border(
                    width = sdp(0.003f),
                    color = Color(0x407F5AF0),
                    shape = RoundedCornerShape(sdp(0.02f))
                )
                .safeClickable {
                    if (isRotationEnabled) onRotateBlock()
                },
            contentAlignment = Alignment.Center
        ) {

            Image(
                painter = painterResource(id = R.drawable.rotate_right),
                contentDescription = "Rotate Block",
                modifier = Modifier
                    .fillMaxSize(0.75f)
                    .aspectRatio(1f),
                contentScale = ContentScale.Fit
                // ❌ NO tint — prevents washed-out PNG bug
            )
        }
    }
}

@Composable
fun SpecialBlockButton(
    count: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    onDragStart: (Block, Offset) -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit
) {
    if (count <= 0) return

    var buttonWindowPos by remember { mutableStateOf(Offset.Zero) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.onGloballyPositioned { buttonWindowPos = it.positionInWindow() }
    ) {
        Surface(
            color = Pink_Jackie,
            shape = RoundedCornerShape(50),
            modifier = Modifier.zIndex(2f)
        ) {
            Text(
                text = "x$count",
                color = DarkBackground,
                fontSize = ssp(0.02f),
                fontWeight = FontWeight.Bold,
                fontFamily = Oswald,
                modifier = Modifier.padding(horizontal = sdp(0.015f), vertical = sdp(0.003f))
            )
        }

        Spacer(modifier = Modifier.height(sdp(0.004f)))

        Box(
            modifier = Modifier
                .size(GameSettings.bottomBarButtonSize.value.coerceAtLeast(1).dp)
                .scale(GameSettings.bottomBarIconScale.value.coerceAtLeast(0.1f))
                .clip(RoundedCornerShape(sdp(0.02f)))
                .background(if (isSelected) Pink_Jackie else SpecialPurple)
                .border(
                    3.dp,
                    if (isSelected) Color.White else Color.White.copy(0.3f),
                    RoundedCornerShape(sdp(0.02f))
                )
                .pointerInput(Unit) {
                    detectSimpleDragOrTap(
                        onDragStart = { localPos ->
                            val windowPos = buttonWindowPos + localPos
                            Log.d("SpecialBlockButton", "onDragStart localPos=$localPos buttonWindowPos=$buttonWindowPos windowPos=$windowPos")
                            onDragStart(RAINBOW_BLOCK, windowPos)
                        },
                        onDrag = { localPos ->
                            val windowPos = buttonWindowPos + localPos
                            Log.d("SpecialBlockButton", "onDrag localPos=$localPos windowPos=$windowPos")
                            onDrag(windowPos)
                        },
                        onDragEnd = {
                            onDragEnd()
                        },
                        onTap = {
                            onClick()
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier.scale(2.0f * GameSettings.bottomBarIconScale.value.coerceAtLeast(0.1f))
             ) {
                // Use a single centered rainbow image for the special button (no tiling of 9 cells)
                Box(contentAlignment = Alignment.Center) {
                    Image(
                        painter = painterResource(id = R.drawable.rainbow),
                        contentDescription = "Rainbow Wipe",
                        modifier = Modifier
                            .size(sdp(0.04f))
                            .aspectRatio(1f),
                        contentScale = ContentScale.Fit
                    )
                }
             }
        }
    }
}

@Composable
fun BlockShapeDisplay(block: Block, cellSize: Dp) {
    Column {
        for (r in 0 until block.boundingBoxHeight) {
            Row {
                for (c in 0 until block.boundingBoxWidth) {
                    val isPresent = block.shape.any { it.row == r && it.col == c }
                    Box(
                        modifier = Modifier
                            .size(cellSize)
                            .padding(sdp(0.0015f))
                            .clip(RoundedCornerShape(sdp(0.005f)))
                    ) {
                        if (isPresent) {
                            Image(
                                painter = painterResource(id = block.colorResId),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
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
                    .padding(sdp(0.03f))
                    .width(IntrinsicSize.Max),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "GAME MENU",
                    color = LightText,
                    fontSize = ssp(0.035f),
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = Oswald
                )
                Spacer(modifier = Modifier.height(sdp(0.03f)))

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

                Spacer(modifier = Modifier.height(sdp(0.03f)))

                Button(
                    onClick = onRestart,
                    colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(sh(0.05f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Filled.Refresh, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("RESTART GAME", fontWeight = FontWeight.Bold, fontFamily = Oswald, fontSize = ssp(0.022f))
                }
                Spacer(modifier = Modifier.height(sdp(0.012f)))

                OutlinedButton(
                    onClick = onGoToMenu,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = LightText),
                    border = BorderStroke(1.dp, LightText.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(sh(0.05f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Filled.Home, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("MAIN MENU", fontWeight = FontWeight.Bold, fontFamily = Oswald, fontSize = ssp(0.022f))
                }

                Spacer(modifier = Modifier.height(sdp(0.012f)))

                OutlinedButton(
                    onClick = onGoToMenu,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                    border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(sh(0.05f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("QUIT GAME", fontWeight = FontWeight.Bold, fontFamily = Oswald, fontSize = ssp(0.022f))
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
            .height(sh(0.05f))
            .safeClickable(onClick = onClick)
            .padding(vertical = sdp(0.004f)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = LightText,
            fontSize = ssp(0.02f),
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
                modifier = Modifier.padding(sdp(0.03f)),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "LAST CHANCE!",
                    color = Pink_Jackie,
                    fontSize = ssp(0.05f),
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = Oswald
                )
                Spacer(modifier = Modifier.height(sdp(0.016f)))
                Text(
                    "No more moves left. Use your Rainbow Wipe to clear the board and keep playing?",
                    color = LightText.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                    fontSize = ssp(0.028f),
                    fontFamily = Oswald
                )
                Spacer(modifier = Modifier.height(sdp(0.03f)))

                Button(
                    onClick = onUseRainbow,
                    colors = ButtonDefaults.buttonColors(containerColor = SpecialPurple),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(sh(0.05f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        "USE RAINBOW WIPE (1)",
                        fontWeight = FontWeight.Bold,
                        fontFamily = Oswald
                    )
                }
                Spacer(modifier = Modifier.height(sdp(0.012f)))

                OutlinedButton(
                    onClick = onGameOver,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = LightText),
                    border = BorderStroke(1.dp, LightText.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(sh(0.05f)),
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
                modifier = Modifier.padding(sdp(0.03f)),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "GAME OVER",
                    color = LightText,
                    fontSize = ssp(0.05f),
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = Oswald
                )
                Spacer(modifier = Modifier.height(sdp(0.03f)))
                Text(
                    "Score",
                    color = LightText.copy(alpha = 0.7f),
                    fontSize = ssp(0.018f),
                    fontFamily = Oswald
                )
                Text(
                    text = score.toString(),
                    color = LightText,
                    fontSize = ssp(0.07f),
                    fontWeight = FontWeight.Bold,
                    fontFamily = Oswald
                )
                Spacer(modifier = Modifier.height(sdp(0.008f)))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(com.betterblocks.trophyRes(com.betterblocks.model.TrophyTier.GOLD)),
                        contentDescription = "High Score",
                        modifier = Modifier.size(sdp(0.02f)),
                        contentScale = ContentScale.Fit
                    )
                    Spacer(modifier = Modifier.width(sdp(0.005f)))
                    Text(
                        text = "Best: $highScore",
                        color = Pink_Jackie,
                        fontSize = ssp(0.025f),
                        fontWeight = FontWeight.Bold,
                        fontFamily = Oswald
                    )
                }
                Spacer(modifier = Modifier.height(sdp(0.03f)))
                Button(
                    onClick = onRestart,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(sh(0.05f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Filled.Refresh, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("PLAY AGAIN", fontWeight = FontWeight.Bold, fontFamily = Oswald, fontSize = ssp(0.022f))
                }
                Spacer(modifier = Modifier.height(sdp(0.012f)))
                OutlinedButton(
                    onClick = onMenu,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = LightText),
                    border = BorderStroke(1.dp, LightText.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(sh(0.05f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Filled.Home, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("MAIN MENU", fontWeight = FontWeight.Bold, fontFamily = Oswald, fontSize = ssp(0.022f))
                }
            }
        }
    }
}

/**
 * Computes an offset that, when applied to a touch point, visually centers the given block
 * under the user's finger regardless of its rotation (bounding box shape).
 *
 * @param block The block whose bounding box is used.
 * @param cellSizePx Size of a single grid cell in pixels.
 * @return Offset that can be added to the finger position so the block's center aligns there.
 */
fun calculateDragOffset(block: Block, cellSizePx: Float): Offset {
    // Block's bounding box dimensions in pixels
    val widthPx = block.boundingBoxWidth * cellSizePx
    val heightPx = block.boundingBoxHeight * cellSizePx

    // We want the block's center to be at the finger; Compose translation is from top-left,
    // so we shift left/up by half the width/height.
    return Offset(
        x = -widthPx / 2f,
        y = -heightPx / 2f
    )
}

// -------------------------------------------------------------
// --- HELPER FUNCTIONS
// -------------------------------------------------------------

/**
 * INDUSTRY-STANDARD GRID POSITION CALCULATION
 *
 * Best practices from Tetris, Woodoku, Block Puzzle:
 * 1. Use CENTER of block as anchor point (not top-left corner)
 * 2. Apply visual offset compensation BEFORE calculation
 * 3. Use floor-based snapping (toInt) for consistent cell targeting
 * 4. Generous boundary tolerance for better UX
 *
 * This ensures ghost preview and actual placement are ALWAYS identical.
 */




fun calculateGridPosition(
    blockCenter: Offset,
    gridTopLeft: Offset,
    gridSizePx: Float,
    gridSize: Int
): Pair<Int, Int>? {

    val cellSize = gridSizePx / gridSize

    val relX = blockCenter.x - gridTopLeft.x
    val relY = blockCenter.y - gridTopLeft.y

    if (relX < 0f || relY < 0f) return null
    if (relX >= gridSizePx || relY >= gridSizePx) return null

    val col = (relX / cellSize).toInt()
    val row = (relY / cellSize).toInt()

    return if (row in 0 until gridSize && col in 0 until gridSize)
        Pair(row, col)
    else null
}



fun isValidPlacement(board: GameGrid, block: Block, origin: Pair<Int, Int>): Boolean {
    val ignoreCollision = block.isSpecial

    // Check bounding box doesn't exceed grid
    if (origin.first + block.boundingBoxHeight > 9 || origin.second + block.boundingBoxWidth > 9) {
        Log.d(
            "PLACEMENT_CHECK",
            "Bounding box exceeds grid: origin=$origin boundingBox=${block.boundingBoxWidth}x${block.boundingBoxHeight}"
        )
        return false
    }

    // Check each cell of the block
    block.shape.forEach { coord ->
        val r = origin.first + coord.row
        val c = origin.second + coord.col

        // Bounds check
        if (r < 0 || r >= 9 || c < 0 || c >= 9) {
            Log.d("PLACEMENT_CHECK", "OUT OF BOUNDS: row=$r col=$c")
            return false
        }

        // Collision check (unless rainbow/special block)
        if (!ignoreCollision && board[r][c] != null) {
            Log.d("PLACEMENT_CHECK", "COLLISION: row=$r col=$c already occupied")
            return false
        }
    }

    Log.d("PLACEMENT_CHECK", "✅ VALID placement at origin=$origin")
    return true
}
