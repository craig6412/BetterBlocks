package com.betterblocks

//calling Game REady for closed testing

import android.util.Log
import com.betterblocks.BuildConfig
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import android.app.Activity
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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.betterblocks.BLOCK_MANAGER
import com.betterblocks.BLOCK_DRAWABLES
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
import com.betterblocks.model.getPlayerTier
import com.betterblocks.trophyColorForTier
import kotlin.times
import kotlin.math.roundToInt

// Add the missing UI helper imports and TextStyle/Shadow here near the top
import com.betterblocks.ui.AvailableBlocks
import com.betterblocks.ui.GameOverSummaryDialog
import com.betterblocks.ui.shareGameResults
import com.betterblocks.ui.ZeroCoinsDialog
import com.betterblocks.ui.TierUnlockDialog
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.graphics.Shadow
import com.betterblocks.ads.AdManager

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
    onDismissShopBubble: () -> Unit,
    onWatchAd: () -> Unit,
    onGoToShop: () -> Unit,
    onDismissZeroCoins: () -> Unit = {},
    onClearAnimationFinished: () -> Unit
) {
    val context = LocalContext.current
    val activity = remember { context as? Activity }
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

    // Root-space origin of the floating-preview overlay container (see below).
    var overlayOriginRoot by remember { mutableStateOf(Offset.Zero) }

    var showMenuDialog by remember { mutableStateOf(false) }
    var showColorWheelDialog by remember { mutableStateOf(false) }
    var showBuyColorWipeDialog by remember { mutableStateOf(false) }
    var showGameOverDialogLocal by remember { mutableStateOf(false) }
    var deferredTierUnlock by remember { mutableStateOf<TrophyTier?>(null) }

    fun clearLocalDialogs() {
        showMenuDialog = false
        showColorWheelDialog = false
        showBuyColorWipeDialog = false
    }

    // Trophy unlock popup tracking.
    // This is UI-only and watches the live score crossing into a newly earned tier.
    val trophyPrefs = remember(context) {
        context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
    }
    val currentEarnedTier = remember(uiState.score, uiState.coins, uiState.highScore) {
        getPlayerTier(
            bestScore = uiState.score.coerceAtLeast(uiState.highScore),
            coins = trophyPrefs.getInt(KEY_LIFETIME_COINS, 0),
            prefs = trophyPrefs
        )
    }
    var lastObservedTierName by rememberSaveable {
        mutableStateOf(currentEarnedTier.name)
    }
    var pendingTierUnlock by remember { mutableStateOf<TrophyTier?>(null) }

    val gameOverSummaryReady =
        (uiState.isGameOver || uiState.showGameSummaryDialog) && showGameOverDialogLocal

    val highPriorityDialogShowing =
        uiState.isLastChance || gameOverSummaryReady

    val gameDialogWaitingOrShowing =
        uiState.isLastChance ||
                uiState.isGameOver ||
                uiState.showGameSummaryDialog ||
                showGameOverDialogLocal

    val localDialogShowing =
        showMenuDialog || showColorWheelDialog || showBuyColorWipeDialog

    val vmNonGameDialogShowing =
        uiState.showRainbowEarnedDialog ||
                uiState.showZeroCoinsDialog

    val anyDialogShowing =
        gameDialogWaitingOrShowing ||
                localDialogShowing ||
                vmNonGameDialogShowing ||
                pendingTierUnlock != null

    LaunchedEffect(uiState.isGameOver, uiState.isLastChance, uiState.showGameSummaryDialog) {
        if (uiState.isGameOver || uiState.isLastChance || uiState.showGameSummaryDialog) {
            clearLocalDialogs()
        }
    }

    LaunchedEffect(currentEarnedTier, uiState.isGameOver, uiState.isLastChance, uiState.showGameSummaryDialog, anyDialogShowing) {
        val lastObservedTier = runCatching {
            TrophyTier.valueOf(lastObservedTierName)
        }.getOrDefault(TrophyTier.UNRANKED)

        if (
            !uiState.isGameOver &&
            !uiState.isLastChance &&
            currentEarnedTier != TrophyTier.UNRANKED &&
            currentEarnedTier.ordinal > lastObservedTier.ordinal
        ) {
            if (gameDialogWaitingOrShowing || anyDialogShowing) {
                deferredTierUnlock = currentEarnedTier
            } else {
                pendingTierUnlock = currentEarnedTier
            }
            lastObservedTierName = currentEarnedTier.name
        } else if (currentEarnedTier.ordinal < lastObservedTier.ordinal) {
            lastObservedTierName = currentEarnedTier.name
        }
    }

    // How far above finger block appears
    val liftPx = with(density) { sh(0.12f).toPx() }

    // Truth placement info (kept as ghostPosition for compatibility)
    val ghostPosition = drag.ghostPosition  // ✅ Correct property name

    // Truth-preview validity (same row/col used for final placement)
    val isGhostValid = remember(ghostPosition, drag.draggedBlock, uiState.board) {
        val block = drag.draggedBlock
        ghostPosition != null && block != null &&
                isValidPlacement(uiState.board, block, ghostPosition)
    }
    // ✅ Capture during composition
    val screenW = sw(1f)
    val screenH = sh(1f)
    val densityValue = LocalDensity.current.density

    // --- Double-ad countdown ticker for testing ---
    // Countdown ticking is owned by AdManager (main-thread handler). Compose must not mutate countdown state.
    // (Any previous UI-driven ticker has been removed.)

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

    // Compute device category once per composition — screen size doesn't change mid-session.
    // screenW/screenH already captured above; pass as regular values into the remember lambda
    // (composable functions can't be called inside a remember { } lambda).
    val cachedDeviceCategory: DeviceClass = if (forcePreviewFold) {
        DeviceClass.Foldable
    } else {
        remember(screenW, screenH) { classifyDevice(screenW, screenH) }
    }

    if (BuildConfig.DEBUG) {
        LaunchedEffect(Unit) {
            Log.d("FOLD_DEBUG", "SCREEN METRICS sw=$screenW sh=$screenH density=$densityValue category=$cachedDeviceCategory")
        }
    }

    @Composable
    fun deviceCategory(forcePreviewFold: Boolean = false): DeviceClass = cachedDeviceCategory

    @Composable
    fun boardSize(forcePreviewFold: Boolean = false): Dp {
        val category = cachedDeviceCategory
        val smallest = minOf(sw(1f), sh(1f))

        val rawSize = when (category) {
            DeviceClass.Phone -> smallest * 0.95f
            DeviceClass.Tablet -> smallest * 0.80f
            DeviceClass.Foldable -> smallest * 0.50f
        }

        return if (category == DeviceClass.Foldable)
            minOf(rawSize, sh(1f) * 0.55f)
        else
            rawSize
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
            DeviceClass.Tablet -> sh(-0.025f)
            DeviceClass.Foldable -> sh(-0.05f)
        }

    @Composable
    fun availableBlocksRowHeight(forcePreviewFold: Boolean = false): Dp {
        return when (deviceCategory(forcePreviewFold)) {
            DeviceClass.Phone -> GameSettings.availableBlocksRowHeight.value.coerceAtLeast(0f).dp
            DeviceClass.Tablet -> 118.dp
            DeviceClass.Foldable -> GameSettings.availableBlocksRowHeight.value.coerceAtLeast(0f).dp * 0.85f
        }
    }

    @Composable
    fun previewCardSize(forcePreviewFold: Boolean = false): Dp {
        return when (deviceCategory(forcePreviewFold)) {
            DeviceClass.Phone -> sw(0.22f)
            DeviceClass.Tablet -> minOf(sw(0.17f), 118.dp)
            DeviceClass.Foldable -> sw(0.20f)
        }
    }

    @Composable
    fun previewCellSize(forcePreviewFold: Boolean = false): Dp {
        return when (deviceCategory(forcePreviewFold)) {
            DeviceClass.Phone -> sdp(0.03f)
            DeviceClass.Tablet -> minOf(sdp(0.022f), 18.dp)
            DeviceClass.Foldable -> sdp(0.026f)
        }
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
            modifier = Modifier
                .fillMaxSize()
                // Capture this Box's own origin in ROOT coords. The floating drag preview
                // is positioned with .offset {} relative to THIS Box, but getBlockTopLeft()
                // is in root coords, so we subtract this origin. This is what fixes the
                // "preview ~one cell too low" caused by the safeDrawing inset.
                .onGloballyPositioned { overlayOriginRoot = it.positionInRoot() }
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
                    onMenuClicked = {
                        if (!anyDialogShowing) {
                            clearLocalDialogs()
                            showMenuDialog = true
                        }
                    }
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
                                    // use root coords to match AnimatedGameBoard and the drag overlay
                                    measuredGridTopLeftWindow = coordinates.positionInRoot()
                                    measuredGridSizePx = coordinates.size.width.toFloat()
                                }
                        ) {
                            // Drag metrics are now set by AnimatedGameBoard only.
                            // That keeps placement math tied to the actual rendered board.

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
                                    // Keep placement data available for clear-line preview math.
                                    // The separate ghost block is disabled; the dragged block overlay is the visible truth.
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
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(availableBlocksRowHeight(forcePreviewFold))
                                .padding(horizontal = SCREEN_HORIZONTAL_PADDING)
                                .offset(y = availableBlocksOffset(forcePreviewFold)),
                            contentAlignment = Alignment.Center
                        ) {
                            AvailableBlocks(
                                uiState = uiState,
                                cardSize = previewCardSize(forcePreviewFold),
                                previewCellSize = previewCellSize(forcePreviewFold),
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
                                    if (BuildConfig.DEBUG) Log.d("GameScreen", "onDragStart (AvailableBlocks): block=${block.id} fingerPosRoot=$fingerPosRoot")
                                    // record recent drag here as well (defensive in case DRAG_START path differs)
                                    recentlyDraggedBlockId = block.id
                                    recentDragTimeMs = System.currentTimeMillis()
                                    // Mark selection explicitly when drag begins so UI keeps selection after finger-up
                                    onSelectBlock(block)
                                    drag.startDrag(block, fingerPosRoot, liftPx)
                                },
                                onDrag = { newFingerPos ->
                                    if (BuildConfig.DEBUG) Log.d("GameScreen", "onDrag (AvailableBlocks): pos=$newFingerPos")
                                    drag.updatePosition(newFingerPos)

                                },
                                onDragEnd = {
                                    if (BuildConfig.DEBUG) Log.d("GameScreen", "onDragEnd (AvailableBlocks) called")
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
                if (!LocalInspectionMode.current && showMenuDialog && !highPriorityDialogShowing) {
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
                        if (!anyDialogShowing) {
                            clearLocalDialogs()
                            if (uiState.colorWipeCount <= 0) {
                                // Prompt user to buy color wipes from the shop
                                showBuyColorWipeDialog = true
                            } else {
                                showColorWheelDialog = true
                            }
                        }
                    },
                    onDragStart = { block, previewOffset ->
                        if (BuildConfig.DEBUG) Log.d("GameScreen", "onDragStart (BottomBar): block=${block.id} previewOffset=$previewOffset")
                        // record recent drag (special block drag starts too)
                        recentlyDraggedBlockId = block.id
                        recentDragTimeMs = System.currentTimeMillis()
                        // Mark selection explicitly when drag begins from bottom bar
                        onSelectBlock(block)
                        // Preview card gives window coords for finger start — pass into controller
                        drag.startDrag(block, previewOffset, liftPx)
                    },
                    onDrag = { newFingerPos ->
                        if (BuildConfig.DEBUG) Log.d("GameScreen", "onDrag (BottomBar): pos=$newFingerPos")
                        drag.updatePosition(newFingerPos)
                    },
                    onDragEnd = {
                        if (BuildConfig.DEBUG) Log.d("GameScreen", "onDragEnd (BottomBar) called")
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
            // DRAGGED BLOCK PREVIEW — single free-floating piece, the visible truth
            // =====================
            // Rendered for the WHOLE drag (over the board AND over the tray). It follows
            // the finger freely and is never grid-locked or clamped. The snap candidate
            // (drag.ghostPosition) is derived from this exact same top-left, so the
            // landing position always matches what the player sees.
            if (drag.isDragging && drag.draggedBlock != null) {
                val block = drag.draggedBlock!!
                val draggedCellDp = if (drag.cellSizePx > 0f) {
                    with(density) { drag.cellSizePx.toDp() }
                } else {
                    cellSize()
                }

                // previewTopLeft is in ROOT coords; convert into this overlay Box's local space.
                val topLeftRoot = drag.getBlockTopLeft()
                val drawX = topLeftRoot.x - overlayOriginRoot.x
                val drawY = topLeftRoot.y - overlayOriginRoot.y

                val previewShape = RoundedCornerShape(draggedCellDp * 0.18f)
                val invalidOverlayColor = Color(0xFFFF1744).copy(alpha = 0.22f)
                // Invalid only when the piece is over the board but the cell is taken / out of range.
                val showInvalid = drag.ghostPosition != null && !isGhostValid

                Box(
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                x = drawX.roundToInt(),
                                y = drawY.roundToInt()
                            )
                        }
                        .size(
                            width = draggedCellDp * block.boundingBoxWidth.toFloat(),
                            height = draggedCellDp * block.boundingBoxHeight.toFloat()
                        )
                        .zIndex(50f)
                ) {
                    BlockGrid(
                        block = block,
                        cellSize = draggedCellDp,
                        modifier = Modifier.fillMaxSize()
                    )

                    if (showInvalid) {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(invalidOverlayColor, previewShape)
                        )
                    }
                }
            }

            // =====================
            // HAPTIC FEEDBACK TRIGGER (for testing)
            // =====================
            // HAPTIC: explicit haptics are called on drop; removed snapshot-based trigger.
        }

        // =====================
        // DEBUG: Log composition
        // =====================
        // Color wheel dialog rendering (debug logs)
        if (!LocalInspectionMode.current && showColorWheelDialog && !highPriorityDialogShowing) {
            Log.d("GameScreen", "showColorWheelDialog == true -> rendering ColorWheelDialog")
            ColorWheelDialog(
                board = uiState.board,
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
        if (!LocalInspectionMode.current && showBuyColorWipeDialog && !highPriorityDialogShowing && !showColorWheelDialog) {
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
        if (!LocalInspectionMode.current && uiState.showRainbowEarnedDialog && !highPriorityDialogShowing && !localDialogShowing) {
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

        // --- Trophy Unlock Dialog (shown immediately when score reaches a new trophy tier) ---
        pendingTierUnlock?.let { unlockedTier ->
            if (
                !LocalInspectionMode.current &&
                !highPriorityDialogShowing &&
                !localDialogShowing &&
                !uiState.showRainbowEarnedDialog &&
                !uiState.showZeroCoinsDialog
            ) {
                TierUnlockDialog(
                    tier = unlockedTier,
                    onDismiss = { pendingTierUnlock = null }
                )
            }
        }

        // --- Last‑chance dialog (offer a rainbow wipe once per game) ---
        if (!LocalInspectionMode.current && uiState.isLastChance) {
            Log.d("GameScreen", "uiState.isLastChance == true -> showing LastChanceDialog")
            LastChanceDialog(
                onUseRainbow = { onLastChanceUsed() },
                onGameOver = { onLastChanceDeclined() }
            )
        }

        // ----------------------------------------------------------------------
        // Game Over flow: run animator, then show summary dialog (root GameScreen)
        // ----------------------------------------------------------------------
        val gameOverAnimator = remember { com.betterblocks.animation.GameOverAnimator() }
        var gameOverAnimState by remember { mutableStateOf(com.betterblocks.animation.GameOverAnimationState()) }

        // Persisted counter for games played to gate interstitials
        var gamesPlayed by rememberSaveable { mutableStateOf(0) }

        LaunchedEffect(uiState.isGameOver, uiState.showGameSummaryDialog) {
            showGameOverDialogLocal = false
            if (uiState.isGameOver || uiState.showGameSummaryDialog) {
                // Run animation; update local state and show dialog only after completion
                gameOverAnimator.run(
                    update = { st -> gameOverAnimState = st },
                    onComplete = { showGameOverDialogLocal = true }
                )
            } else {
                gameOverAnimState = com.betterblocks.animation.GameOverAnimationState()
            }
        }

        // Render overlay above board but beneath dialogs
        if (gameOverAnimState.isRunning || gameOverAnimState.progress > 0f || gameOverAnimState.dimAlpha > 0f || gameOverAnimState.glowAlpha > 0f) {
            com.betterblocks.animation.GameOverOverlay(
                state = gameOverAnimState,
                cellDp = cellDp,
                gridSize = 9,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Show Game Over summary dialog only after the animation finished
        if (!LocalInspectionMode.current && (uiState.isGameOver || uiState.showGameSummaryDialog) && showGameOverDialogLocal) {
            // Player name onboarding has been moved here: end of the first game over, before the summary dialog.
            val prefs = context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
            val savedPlayerName = prefs.getString(KEY_PLAYER_NAME, null)
            val prompted = prefs.getBoolean(KEY_PLAYER_NAME_PROMPTED, false)

            var showPlayerNameDialog by rememberSaveable(uiState.isGameOver, uiState.showGameSummaryDialog) { mutableStateOf(false) }

            LaunchedEffect(uiState.isGameOver, uiState.showGameSummaryDialog, showGameOverDialogLocal) {
                if ((uiState.isGameOver || uiState.showGameSummaryDialog) && showGameOverDialogLocal) {
                    if (!prompted && savedPlayerName.isNullOrBlank()) {
                        showPlayerNameDialog = true
                    }
                }
            }

            if (!LocalInspectionMode.current && showPlayerNameDialog) {
                PlayerNameDialog(
                    currentName = null,
                    onSave = { chosenName ->
                        prefs.edit().putString(KEY_PLAYER_NAME, chosenName).putBoolean(KEY_PLAYER_NAME_PROMPTED, true).apply()
                        showPlayerNameDialog = false

                        // Optionally attempt leaderboard update if a user id exists.
                        val userId = prefs.getString(KEY_FIREBASE_USER_ID, null)
                        val currentScore = prefs.getInt(KEY_HIGH_SCORE, 0)
                        if (!userId.isNullOrBlank()) {
                            try {
                                com.betterblocks.FirestoreManager.updateLeaderboard(
                                    userId = userId,
                                    score = currentScore,
                                    tier = com.betterblocks.model.getPlayerTier(
                                        currentScore,
                                        prefs.getInt(KEY_LIFETIME_COINS, 0),
                                        prefs
                                    ),
                                    playerNameOverride = chosenName
                                )
                            } catch (_: Throwable) {
                            }
                        }
                    },
                    onCancel = {
                        // Mark as prompted so we don't show again.
                        prefs.edit().putBoolean(KEY_PLAYER_NAME_PROMPTED, true).apply()
                        showPlayerNameDialog = false
                    }
                )
            } else {
                Log.d("GameScreen", "Showing GameOverSummaryDialog -> isGameOver=${uiState.isGameOver} showGameSummaryDialog=${uiState.showGameSummaryDialog}")
                GameOverSummaryDialog(
                    finalScore = uiState.score,
                    highScore = uiState.highScore,
                    totalLinesCleared = uiState.linesClearedThisGame,
                    coinsEarned = uiState.coinsEarnedThisGame,
                    trophyTier = uiState.trophyTier,
                    isNewHighScore = uiState.showHighScoreAnim,
                    onPlayAgain = {
                        // Play again handler — increment games counter and show interstitial every 3rd completed game
                        gamesPlayed += 1
                        val shouldShowAd = (gamesPlayed % 3 == 0)
                        if (shouldShowAd && activity != null) {
                            // Defer to AdManager to show interstitial; ensure it doesn't run during animation
                            try {
                                com.betterblocks.ads.AdManager.tryShowInterstitial(activity)
                            } catch (t: Throwable) {
                                Log.w("GameScreen", "Failed to show interstitial: ${t.message}")
                            }
                        }
                        // Reset game via provided callback
                        onReset()
                    },
                    onMainMenu = { onGoToMenu() },
                    onShare = { shareGameResults(context, uiState.score, uiState.trophyTier) }
                )
            }
        }

        LaunchedEffect(anyDialogShowing, pendingTierUnlock, deferredTierUnlock) {
            if (!anyDialogShowing && pendingTierUnlock == null && deferredTierUnlock != null) {
                pendingTierUnlock = deferredTierUnlock
                deferredTierUnlock = null
            }
        }

        // --- Zero Coins Dialog ---
        if (!LocalInspectionMode.current && uiState.showZeroCoinsDialog && !highPriorityDialogShowing && !localDialogShowing && !uiState.showRainbowEarnedDialog && pendingTierUnlock == null) {
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

    // Top-right countdown overlay for testing the rewarded-ad flow
    if (!LocalInspectionMode.current) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(10f)
        ) {
            val overlaySeconds = AdManager.rewardedSecondsLeft.value
            if (overlaySeconds > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 12.dp, end = 12.dp)
                ) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "AD • $overlaySeconds s",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            color = Color.White,
                            style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }
    }
}

// Analyze the board and return the wheel indices (0..segments-1) that correspond to colors present on the board.
// This does not change visuals — it only constrains which segment the wheel can land on.
private fun allowedWheelIndicesFromBoard(board: GameGrid, segments: Int): List<Int> {
    if (segments <= 0) return emptyList()

    // Map drawableResId -> wheel index for quick lookup.
    val resIdToWheelIndex: Map<Int, Int> = COLOR_WIPE_DRAWABLES
        .take(segments)
        .mapIndexed { index, resId -> resId to index }
        .toMap()

    val allowed = LinkedHashSet<Int>()

    // GameGrid is 9x9 (Array<Array<Int?>>). Each occupied cell already stores the drawable resId.
    for (r in 0 until 9) {
        for (c in 0 until 9) {
            val drawableResId = board[r][c]
            if (drawableResId != null) {
                val idx = resIdToWheelIndex[drawableResId]
                if (idx != null) allowed.add(idx)
            }
        }
    }

    // If for some reason we can't detect any colors, fall back to allowing all segments.
    return if (allowed.isNotEmpty()) allowed.toList() else (0 until segments).toList()
}

// Color wheel dialog implementation with spinning animation
@Composable
private fun ColorWheelDialog(
    board: GameGrid,
    onDismiss: () -> Unit,
    onSpinFinished: (Int) -> Unit
) {
    // Use dedicated COLOR_WIPE_DRAWABLES (single source-of-truth) and force 7 segments
    val segments = COLOR_WIPE_DRAWABLES.size.coerceAtMost(7)

    // Map the drawable IDs to approximate display colors for the wheel visuals.
    // IMPORTANT: This list must match the order of COLOR_WIPE_DRAWABLES exactly (index -> drawable id).
    val wheelColors = COLOR_WIPE_DRAWABLES.map { drawableRes ->
        when (drawableRes) {
            R.drawable.blue -> Color(0xFF1976D2)
            R.drawable.green -> Color(0xFF43A047)
            R.drawable.pink -> Color(0xFFE91E63)
            R.drawable.pumpkin_orange -> Color(0xFFFF6D00)
            R.drawable.purple -> Color(0xFF8E24AA)
            R.drawable.red -> Color(0xFFE53935)
            R.drawable.yellow -> Color(0xFFFFEE58)
            else -> Color(0xFF111217)
        }
    }

    // UI state
    var spinning by remember { mutableStateOf(false) }
    var winningIndex by remember { mutableStateOf<Int?>(null) }
    val rotation = remember { androidx.compose.animation.core.Animatable(0f) }
    val random = java.util.Random()
    val coroutineScope = rememberCoroutineScope()

    // Compute allowed landing indices based on which colors are currently present on the board.
    // Keep in sync with the current board (background-only logic).
    val allowedIndices = remember(board, segments) { allowedWheelIndicesFromBoard(board, segments) }

    // Visual constants
    val sweep = 360f / segments
    val neon = Color(0xFF00E5FF)
    val panelBg = Color(0xFF0E0F14).copy(alpha = 0.82f)

    // Use Dialog + Surface for perfect centering and proper overlay semantics
    androidx.compose.ui.window.Dialog(onDismissRequest = { if (!spinning) onDismiss() }) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = panelBg,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .padding(16.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
                // Title
                Text(
                    text = "Color Wheel",
                    color = neon,
                    fontFamily = Oswald,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Wheel container — use Box with fixed aspect ratio to ensure perfect centering.
                Box(modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f), contentAlignment = Alignment.Center) {

                    // Glow rim behind wheel
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val r = size.minDimension / 2f
                        drawCircle(
                            brush = Brush.radialGradient(listOf(neon.copy(alpha = 0.26f), Color.Transparent)),
                            radius = r * 1.1f,
                            center = center
                        )
                    }

                    // Wheel drawing — rotation measured in degrees; 0deg as-drawn corresponds to 3 o'clock.
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val radius = size.minDimension / 2f
                        val cx = center.x
                        val cy = center.y

                        // Current rotation normalized to 0..360
                        val rot = rotation.value % 360f

                        // Draw exactly `segments` arcs in the order of COLOR_WIPE_DRAWABLES.
                        // Note: drawArc startAngle is measured from 3 o'clock and positive is clockwise.
                        for (i in 0 until segments) {
                            val startAngle = -rot + i * sweep
                            drawArc(
                                color = wheelColors[i % wheelColors.size],
                                startAngle = startAngle,
                                sweepAngle = sweep,
                                useCenter = true,
                                topLeft = androidx.compose.ui.geometry.Offset(cx - radius, cy - radius),
                                size = androidx.compose.ui.geometry.Size(radius * 2f, radius * 2f)
                            )
                        }

                        // Draw inner glass hole
                        drawCircle(color = DarkBackground, radius = radius * 0.28f, center = center)

                        // Rim stroke glow
                        drawCircle(
                            color = neon.copy(alpha = 0.22f),
                            radius = radius * 0.98f,
                            style = Stroke(width = radius * 0.06f)
                        )

                        // If a winning segment is set, softly highlight it
                        winningIndex?.let { win ->
                            val highlightStart = -rot + win * sweep
                            drawArc(
                                color = neon.copy(alpha = 0.14f),
                                startAngle = highlightStart,
                                sweepAngle = sweep,
                                useCenter = true,
                                topLeft = androidx.compose.ui.geometry.Offset(cx - radius, cy - radius),
                                size = androidx.compose.ui.geometry.Size(radius * 2f, radius * 2f)
                            )
                        }
                    }

                    // Fixed pointer at 12 o'clock (top) — this is the selection reference (0 degrees)
                    Box(modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = (-6).dp)) {
                        Canvas(modifier = Modifier.size(28.dp)) {
                            val w = size.width
                            val h = size.height
                            val path = androidx.compose.ui.graphics.Path().apply {
                                moveTo(w / 2f, 0f)
                                lineTo(w, h * 0.75f)
                                lineTo(0f, h * 0.75f)
                                close()
                            }
                            drawPath(path, color = Color.White.copy(alpha = 0.92f))
                            drawPath(path, color = neon.copy(alpha = 0.4f), style = Stroke(width = 2f))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(text = if (spinning) "Spinning..." else "Tap Spin to pick a color", color = Color(0xFFBFC5D0))

                Spacer(modifier = Modifier.height(12.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { if (!spinning) onDismiss() }) {
                        Text("Cancel", color = Color(0xFFBFC5D0))
                    }

                    Button(onClick = {
                        if (spinning) return@Button
                        spinning = true
                        winningIndex = null

                        // Choose random target index ONLY from colors present on the board
                        val targetIndex = allowedIndices[random.nextInt(allowedIndices.size)]

                        // Math explanation (center-pointer alignment):
                        // - Each segment i occupies angular interval [i*sweep, i*sweep + sweep) measured from 3 o'clock
                        // - Segment center (unrotated) = i*sweep + sweep/2
                        // - We draw arcs with startAngle = -rotation + ... so after rotating by r degrees,
                        //   the visual center of segment i becomes (i*sweep + sweep/2) - r (measured from 3 o'clock)
                        // - Pointer at top corresponds to angle -90 degrees (12 o'clock). For the segment center to match pointer:
                        //     (i*sweep + sweep/2) - r == -90  =>  r == i*sweep + sweep/2 + 90
                        // - So target rotation r should land at r = centerOfTarget + 90 (plus whole rotations for spin effect).

                        val centerOfTarget = targetIndex * sweep + sweep / 2f
                        val extraRotations = 6 + random.nextInt(6) // 6..11 full spins for pleasing length
                        val targetDegrees = extraRotations * 360f + centerOfTarget + 90f

                        coroutineScope.launch {
                            rotation.animateTo(targetDegrees, animationSpec = androidx.compose.animation.core.tween(durationMillis = 2400))
                            val finalIndex = targetIndex % segments
                            Log.d("ColorWheel", "targetDegrees=$targetDegrees finalIndex=$finalIndex")

                            // Show winning highlight immediately, keep dialog visible for a short pause
                            winningIndex = finalIndex

                            // Pause so the user can visually register the winning segment (1 second)
                            kotlinx.coroutines.delay(1000L)

                            // Finalize spin state and notify the caller with direct index mapping to COLOR_WIPE_DRAWABLES
                            spinning = false

                            onSpinFinished(finalIndex)
                        }
                    }, enabled = !spinning) {
                        Text(text = if (spinning) "Spinning..." else "Spin", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
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
