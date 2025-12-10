// ...existing code...
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
+    onDismissFirstGameOver: () -> Unit = {},
    onDismissPurchaseSuccess: () -> Unit = {},
    onClearCoinAnimation: () -> Unit = {},
    onDismissShopBubble: () -> Unit = {},
    onClearAnimationFinished: () -> Unit
) {
// ...existing code...
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

+        // --- Rainbow Earned Dialog (earned by filling special meter) ---
+        if (uiState.showRainbowEarnedDialog) {
+            AlertDialog(
+                onDismissRequest = { onDismissRainbowEarned() },
+                title = { Text(text = "CONGRATULATIONS!", color = Pink_Jackie, fontFamily = Oswald, fontWeight = FontWeight.ExtraBold) },
+                text = {
+                    Column {
+                        Text(text = "You just earned a free Rainbow Wipe.", color = LightText)
+                        Spacer(modifier = Modifier.height(6.dp))
+                        Text(text = "Use it when you're stuck to clear the board.", color = LightText.copy(alpha = 0.9f))
+                    }
+                },
+                confirmButton = {
+                    TextButton(onClick = {
+                        // Use immediately then dismiss
+                        onUseRainbowImmediately()
+                        onDismissRainbowEarned()
+                    }) { Text("USE NOW") }
+                },
+                dismissButton = {
+                    TextButton(onClick = { onDismissRainbowEarned() }) { Text("OK") }
+                },
+                properties = DialogProperties(dismissOnClickOutside = true, dismissOnBackPress = true)
+            )
+        }
+
+        // --- First-Time Game Over Reward Dialog (awards 3 rainbows on first-ever game over) ---
+        if (uiState.showFirstGameOverDialog) {
+            AlertDialog(
+                onDismissRequest = { onDismissFirstGameOver() },
+                title = { Text(text = "WELCOME!", color = Pink_Jackie, fontFamily = Oswald, fontWeight = FontWeight.ExtraBold) },
+                text = {
+                    Column {
+                        Text(text = "This is your first game over — congratulations!", color = LightText)
+                        Spacer(modifier = Modifier.height(6.dp))
+                        Text(text = "We've awarded you 3 free Rainbow Wipes to help you get back in the game.", color = LightText.copy(alpha = 0.9f))
+                    }
+                },
+                confirmButton = {
+                    TextButton(onClick = {
+                        // Let the user use one immediately if they want
+                        onUseRainbowImmediately()
+                        onDismissFirstGameOver()
+                    }) { Text("USE ONE NOW") }
+                },
+                dismissButton = {
+                    TextButton(onClick = { onDismissFirstGameOver() }) { Text("GOT IT") }
+                },
+                properties = DialogProperties(dismissOnClickOutside = true, dismissOnBackPress = true)
+            )
+        }
+
        // Show Game Over summary dialog when ViewModel marks the game over
        if (uiState.isGameOver) {
// ...existing code...

