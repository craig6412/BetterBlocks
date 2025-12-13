package com.betterblocks.ui

//calling Game REady for closed testing

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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropUp
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
import androidx.compose.ui.platform.LocalInspectionMode
import com.betterblocks.ui.sw
import com.betterblocks.ui.sh
import com.betterblocks.ui.sdp
import com.betterblocks.ui.ssp
import kotlinx.coroutines.launch
import androidx.compose.ui.res.painterResource
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
import com.betterblocks.PreviewGameViewModel
import com.betterblocks.SCREEN_HORIZONTAL_PADDING
import com.betterblocks.isValidPlacement
import com.betterblocks.model.TrophyTier
import com.betterblocks.trophyColorForTier
import kotlin.times


//song lyrics because I am getting somewhere finally

/* Called you, never heard back
After everything we been through, the good and the bad
You're 'bout to throw it all away and overreact
Because I overreacted, karma, I guess
My temper gets the best of me, a part of me that
I wish I knew how to get rid of, it's a issue I have​
My M.O. say I need you and I love you to death
Then turn around and go and treat you like you nothing but trash, I know
I know I messed that part up, but */

enum class DeviceClass {
    Phone,
    Tablet,
    Foldable
}
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
    forcePreviewFold: Boolean = false,
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
    // Haptic feedback: use a safe no-op so builds/previews don't fail if platform API is unavailable.
    val performHaptic: (HapticFeedbackType) -> Unit = { /* no-op (device may still have haptics) */ }

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
    var showBuyColorWipeDialog by remember { mutableStateOf(false) }

    // How far above finger block appears
    val liftPx = with(density) { sh(0.12f).toPx() }

    // Ghost info (controller output)
    val ghostPosition = drag.ghostPosition  // ✅ Correct property name

    // Ghost validity (placement check)
    val isGhostValid = remember(ghostPosition, drag.draggedBlock?.id, uiState.board) {
        val block = drag.draggedBlock
        ghostPosition != null && block != null &&
                isValidPlacement(uiState.board, block, ghostPosition)
    }
    // ✅ Capture during composition
    val screenW = sw(1f)
    val screenH = sh(1f)
    val densityValue = LocalDensity.current.density

    LaunchedEffect(Unit) {
        Log.e(
            "FOLD_DEBUG",
            """
        SCREEN METRICS
        sw(1f) = $screenW
        sh(1f) = $screenH
        density = $densityValue
        """.trimIndent()
        )
    }
    @Composable
    fun maxBoardHeight(): Dp {
        return sh(1f) * 0.55f
    }
    fun classifyDevice(
        widthDp: Dp,
        heightDp: Dp
    ): DeviceClass {
        val smallest = minOf(widthDp, heightDp)
        val largest = maxOf(widthDp, heightDp)
        val aspect = largest / smallest

        return when {
            // ✅ REAL unfolded foldables (Galaxy Z Fold, Pixel Fold)
            smallest >= 600.dp && aspect < 1.35f -> DeviceClass.Foldable

            // Tablets (wide + not square-ish)
            smallest >= 600.dp -> DeviceClass.Tablet

            else -> DeviceClass.Phone
        }
    }

    @Composable
    fun deviceCategory(forcePreviewFold: Boolean = false): DeviceClass {
        if (forcePreviewFold) {
            Log.e("FOLD_DEBUG", "FORCED PREVIEW FOLD")
            return DeviceClass.Foldable
        }

        val width = sw(1f)
        val height = sh(1f)

        val category = classifyDevice(width, height)

        Log.e(
            "FOLD_DEBUG",
            """
        deviceCategory()
        widthDp = $width
        heightDp = $height
        RESULT = $category
        """.trimIndent()
        )

        return category
    }

    @Composable
    fun boardSize(forcePreviewFold: Boolean = false): Dp {
        val category = deviceCategory(forcePreviewFold)
        val smallest = minOf(sw(1f), sh(1f))

        val rawSize = when (category) {
            DeviceClass.Phone -> smallest * 0.95f
            DeviceClass.Tablet -> smallest * 0.80f
            DeviceClass.Foldable -> smallest * 0.50f
        }

        val finalSize =
            if (category == DeviceClass.Foldable)
                minOf(rawSize, sh(1f) * 0.55f) // 👈 the FIX
            else
                rawSize

        Log.e(
            "FOLD_DEBUG",
            """
        boardSize()
        category = $category
        rawSize = $rawSize
        finalSize = $finalSize
        """.trimIndent()
        )

        return finalSize
    }
    @Composable
    fun gridVerticalOffset(forcePreviewFold: Boolean = false): Dp =
        when (deviceCategory(forcePreviewFold)) {
            DeviceClass.Phone -> sh(-0.07f)
            DeviceClass.Tablet -> sh(-0.04f)
            DeviceClass.Foldable -> sh(-0.07f)
        }

    @Composable
    fun availableBlocksOffset(forcePreviewFold: Boolean = false): Dp =
        when (deviceCategory(forcePreviewFold)) {
            DeviceClass.Phone -> sh(-0.05f)
            DeviceClass.Tablet -> sh(-0.08f)
            DeviceClass.Foldable -> sh(-0.05f)
        }
    @Composable
    fun cellSize(): Dp = boardSize() / 9

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

                        // GRID (90% of screen width as requested)
                        val boardWidth = boardSize(forcePreviewFold)
                        Box(
                            modifier = Modifier
                                .padding(horizontal = sdp(0.02f))
                                .size(boardWidth)
                                .offset(
                                    x = GameSettings.gridOffsetX.value.dp,
                                    y = gridVerticalOffset()
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
                                    // compute an even integer pixel cell size so rendering and drag math align
                                    val rawCellPx = measuredGridSizePx / 9f
                                    var cellPxInt = rawCellPx.toInt().coerceAtLeast(1)
                                    if (cellPxInt % 2 != 0) cellPxInt -= 1 // make even

                                    drag.gridTopLeft = measuredGridTopLeftWindow
                                    drag.gridSizePx = measuredGridSizePx
                                    drag.cellSizePx = cellPxInt.toFloat()
                                    Log.d("GameScreen", "Deferred drag metrics set: topLeftWindow=$measuredGridTopLeftWindow sizePx=$measuredGridSizePx cellPx=$cellPxInt")
                                }
                            }

                            // Visual border drawn slightly larger than the grid so there is a ~5.dp gap
                            val borderGap = sdp(0.005f)
                             if (measuredGridSizePx > 0f) {
                                 val gridDp = with(density) { measuredGridSizePx.toDp() }
                                 val borderSize = gridDp + (borderGap * 2)

                                 Box(
                                     modifier = Modifier
                                         .size(borderSize)
                                         .offset (
                                             x = 0.dp,
                                             y = sdp(0.01f)

                                         )
                                         .align(Alignment.Center)
                                         .border(
                                             width = sdp(0.005f),
                                             color = LightText.copy(alpha = 0.08f),
                                             shape = RoundedCornerShape(sdp(0.01f))
                                         )
                                         .zIndex(0f)
                                 )
                             }

                            // The actual game board (measurements used by drag controller remain tied to this Box)
                            // Compute an effective cell Dp that matches the even pixel cell size used by the drag controller
                            val effectiveCellDp = if (measuredGridSizePx > 0f) {
                                val rawCellPx = measuredGridSizePx / 9f
                                var cellPxInt = rawCellPx.toInt().coerceAtLeast(1)
                                if (cellPxInt % 2 != 0) cellPxInt -= 1
                                with(density) { cellPxInt.toDp() }
                            } else {
                                cellSize()
                            }


                            Box(modifier = Modifier.align(Alignment.Center).zIndex(1f)) {
                                com.betterblocks.animation.AnimatedGameBoard(
                                    board = uiState.board,
                                    gridSize = 9,
                                    cellDp = effectiveCellDp,
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

                        Spacer(modifier = Modifier.height(sdp(0.01f)))

                        // =====================
                        // AVAILABLE BLOCKS ROW
                        // =====================
                        // Defensive clamp: ensure height is non-negative (developer knobs can set negatives)
                        @Composable
                        fun availableBlocksRowHeight(forcePreviewFold: Boolean = false): Dp {
                            val category = deviceCategory(forcePreviewFold)
                            val base = GameSettings.availableBlocksRowHeight.value.dp

                            return when (category) {
                                DeviceClass.Foldable -> base * 0.85f   // 👈 shrink ONLY on fold
                                else -> base
                            }
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(
                                    GameSettings.availableBlocksRowHeight.value
                                        .coerceAtLeast(0f)
                                        .dp
                                )
                                .padding(horizontal = SCREEN_HORIZONTAL_PADDING)
                                .offset(y = availableBlocksOffset()),
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
                                        performHaptic(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
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
                if (!LocalInspectionMode.current && showMenuDialog) {
                    GameMenuDialog(
                        uiState = uiState,
                        onDismiss = { showMenuDialog = false },
                        onRestart = onReset,
                        onGoToMenu = onGoToMenu,
                        onToggleSound = onToggleSound,
                        onToggleMusic = onToggleMusic
                    )
                }

                Spacer(modifier = Modifier.height(sdp(0.02f)))

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
                        Log.d("GameScreen", "BottomBar.onColorWipeClick -> checking color wipe inventory")
                        if (uiState.colorWipeCount <= 0) {
                            // Prompt user to buy color wipes from the shop
                            showBuyColorWipeDialog = true
                        } else {
                            showColorWheelDialog = true
                        }
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
                            performHaptic(HapticFeedbackType.TextHandleMove)
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
        if (!LocalInspectionMode.current && showColorWheelDialog) {
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

        // Buy / Purchase prompt when user has zero color wipes
        if (!LocalInspectionMode.current && showBuyColorWipeDialog) {
            AlertDialog(
                onDismissRequest = { showBuyColorWipeDialog = false },
                title = { Text(text = "No Color Wipes", color = Pink_Jackie, fontFamily = Oswald, fontWeight = FontWeight.ExtraBold) },
                text = {
                    Column {
                        Text(text = "You don't have any Color Wipes.")
                        Spacer(modifier = Modifier.height(sdp(0.006f)))
                        Text(text = "Would you like to purchase some from the shop?", color = LightText.copy(alpha = 0.9f))
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        showBuyColorWipeDialog = false
                        onGoToShop()
                    }) { Text("Go to Shop") }
                },
                dismissButton = {
                    TextButton(onClick = { showBuyColorWipeDialog = false }) { Text("Cancel") }
                },
                properties = androidx.compose.ui.window.DialogProperties(dismissOnClickOutside = true, dismissOnBackPress = true)
            )
        }

        // --- Rainbow Earned Dialog (earned by filling special meter) ---
        if (!LocalInspectionMode.current && uiState.showRainbowEarnedDialog) {
            AlertDialog(
                onDismissRequest = { onDismissRainbowEarned() },
                title = { Text(text = "CONGRATULATIONS!", color = Pink_Jackie, fontFamily = Oswald, fontWeight = FontWeight.ExtraBold) },
                text = {
                    Column {
                        Text(text = "You just earned a free Rainbow Wipe.", color = LightText)
                        Spacer(modifier = Modifier.height(sdp(0.006f)))
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
        if (!LocalInspectionMode.current && uiState.showFirstGameOverDialog) {
            AlertDialog(
                onDismissRequest = { onDismissFirstGameOver() },
                title = { Text(text = "WELCOME!", color = Pink_Jackie, fontFamily = Oswald, fontWeight = FontWeight.ExtraBold) },
                text = {
                    Column {
                        Text(text = "This is your first game over — congratulations!", color = LightText)
                        Spacer(modifier = Modifier.height(sdp(0.006f)))
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

        // --- Last‑chance dialog (offer a rainbow wipe once per game) ---
        if (!LocalInspectionMode.current && uiState.isLastChance) {
            Log.d("GameScreen", "uiState.isLastChance == true -> showing LastChanceDialog")
            LastChanceDialog(
                onUseRainbow = { onLastChanceUsed() },
                onGameOver = { onLastChanceDeclined() }
            )
        }

        // Show Game Over summary dialog when ViewModel marks the game over OR explicitly requests the summary
        if (!LocalInspectionMode.current && (uiState.isGameOver || uiState.showGameSummaryDialog)) {
            Log.d("GameScreen", "Showing GameOverSummaryDialog -> isGameOver=${uiState.isGameOver} showGameSummaryDialog=${uiState.showGameSummaryDialog}")
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
        if (!LocalInspectionMode.current && uiState.showZeroCoinsDialog) {
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

// Color wheel dialog implementation with spinning animation
@Composable
private fun ColorWheelDialog(
    onDismiss: () -> Unit,
    onSpinFinished: (Int) -> Unit
) {
    // 8-segment wheel
    val segments = 8
    val segmentColors = listOf(
        Color(0xFF2196F3), // Blue
        Color(0xFF4CAF50), // Green
        Color(0xFFE91E63), // Pink
        Color(0xFFFF9800), // Orange
        Color(0xFF9C27B0), // Purple
        Color(0xFFF44336), // Red
        Color(0xFFFFEB3B), // Yellow
        Color(0xFF00BCD4)  // Teal
    )

    var spinning by remember { mutableStateOf(false) }
    val rotation = remember { androidx.compose.animation.core.Animatable(0f) }
    val random = java.util.Random()
    val coroutineScope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = { if (!spinning) onDismiss() },
        title = { Text(text = "Color Wheel", color = Pink_Jackie, fontFamily = Oswald, fontWeight = FontWeight.ExtraBold) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(modifier = Modifier.size(sw(0.35f)), contentAlignment = Alignment.Center) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val radius = size.minDimension / 2f
                        val center = center
                        val sweep = 360f / segments
                        var start = rotation.value % 360f
                        // Draw segments
                        for (i in 0 until segments) {
                            drawArc(
                                color = segmentColors[i % segmentColors.size],
                                startAngle = -start + i * sweep,
                                sweepAngle = sweep,
                                useCenter = true,
                                topLeft = center - androidx.compose.ui.geometry.Offset(radius, radius),
                                size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2)
                            )
                        }
                        // Draw center circle
                        drawCircle(color = DarkBackground, radius = radius * 0.28f, center = center)
                    }
                    // Pointer at top
                    Box(modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = sdp(-0.012f))) {
                        Icon(
                            imageVector = Icons.Default.ArrowDropUp,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(sdp(0.03f))
                        )
                    }
                }
                Spacer(modifier = Modifier.height(sdp(0.01f)))
                Text(text = if (spinning) "Spinning..." else "Tap Spin to try your luck!", color = LightText)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (spinning) return@TextButton
                spinning = true
                // Spin asynchronously using coroutineScope
                val extraRotations = 5 + random.nextInt(6) // between 5..10 full rotations
                val targetDegrees = extraRotations * 360f + random.nextFloat() * 360f
                coroutineScope.launch {
                    rotation.animateTo(targetDegrees, animationSpec = androidx.compose.animation.core.tween(durationMillis = 2400))
                    // Compute final index — pointer at top (0 degrees) maps to segment index
                    val finalAngle = (rotation.value % 360f + 360f) % 360f
                    val sweep = 360f / segments
                    val index = (((360f - finalAngle + sweep / 2f) / sweep).toInt()).mod(segments)
                    spinning = false
                    onSpinFinished(index)
                }
            }) {
                Text(if (spinning) "Spinning..." else "Spin")
            }
        },
        dismissButton = {
            TextButton(onClick = { if (!spinning) onDismiss() }) { Text("Cancel") }
        },
        properties = androidx.compose.ui.window.DialogProperties(dismissOnClickOutside = !spinning, dismissOnBackPress = !spinning)
    )
}


@Preview(
    name = "Galaxy Z Fold 5 (Unfolded)",
    device = "spec:width=930dp,height=775dp,dpi=374",
    showSystemUi = true,
    showBackground = true
)
@Composable
fun GameScreenPreview_Fold() {
    val vm = PreviewGameViewModel()
    GameScreen(
        uiState = vm.uiState.collectAsState().value,
        forcePreviewFold = true,
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
        onDismissFirstGameOver = {},
        onDismissPurchaseSuccess = {},
        onClearCoinAnimation = {},
        onDismissShopBubble = {},
        onWatchAd = {},
        onGoToShop = {},
        onDismissZeroCoins = {},
        onClearAnimationFinished = {}
    )
}
