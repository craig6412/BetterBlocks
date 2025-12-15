package com.betterblocks

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.betterblocks.ui.sdp
import com.betterblocks.ui.ssp
import kotlinx.coroutines.delay
import kotlin.random.Random

private val LOCAL_PLAYER_NAME_REGEX = Regex("^[A-Za-z0-9 ]+$")

/**
 * Themed Player Name dialog that matches the game's dark UI.
 * Use this from any screen (MainMenuScreen, HighScoreActivity, etc.).
 */
@Composable
fun PlayerNameDialog(
    currentName: String?,
    onSave: (String) -> Unit,
    onCancel: () -> Unit
) {
    var name by rememberSaveable(currentName) { mutableStateOf(currentName ?: "") }
    var errorText by remember { mutableStateOf<String?>(null) }
    val focusRequester = remember { FocusRequester() }
    val isInPreview = LocalInspectionMode.current

    fun attemptSave() {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) {
            errorText = null
            onSave(generateFallbackPlayerName())
            return
        }
        when {
            trimmed.length < 3 || trimmed.length > 16 -> {
                errorText = "Name must be 3–16 characters"
            }
            !LOCAL_PLAYER_NAME_REGEX.matches(trimmed) -> {
                errorText = "Only letters, numbers, spaces allowed"
            }
            else -> {
                errorText = null
                onSave(trimmed)
            }
        }
    }

    if (!isInPreview) {
        Dialog(onDismissRequest = onCancel) {
            Card(
                shape = RoundedCornerShape(sdp(0.02f)),
                colors = CardDefaults.cardColors(containerColor = DeepBlue),
                border = BorderStroke(sdp(0.0015f), Pink_Jackie.copy(alpha = 0.2f)),
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .wrapContentHeight()
            ) {
                Column(modifier = Modifier.padding(horizontal = sdp(0.03f), vertical = sdp(0.025f))) {
                    Text(
                        "Choose Display Name",
                        fontFamily = Oswald,
                        fontWeight = FontWeight.Bold,
                        color = LightText,
                        fontSize = ssp(0.028f)
                    )

                    Spacer(modifier = Modifier.height(sdp(0.015f)))

                    Text(
                        "This name will appear on leaderboards and other players will see it.",
                        color = LightText.copy(alpha = 0.8f),
                        fontSize = ssp(0.014f)
                    )

                    Spacer(modifier = Modifier.height(sdp(0.02f)))

                    OutlinedTextField(
                        value = name,
                        onValueChange = {
                            if (errorText != null) errorText = null
                            name = it
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester),
                        textStyle = TextStyle(fontFamily = Oswald, color = LightText),
                        singleLine = true,
                        placeholder = {
                            Text("Player Name", fontFamily = Oswald, color = LightText.copy(alpha = 0.5f))
                        },
                        isError = errorText != null
                    )

                    if (errorText != null) {
                        Text(
                            errorText!!,
                            color = MaterialTheme.colorScheme.error,
                            fontFamily = Oswald,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(sdp(0.02f)))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = onCancel) {
                            Text("Cancel", color = LightText.copy(alpha = 0.9f), fontFamily = Oswald)
                        }

                        Spacer(modifier = Modifier.width(sdp(0.02f)))

                        Button(onClick = { attemptSave() }, colors = ButtonDefaults.buttonColors(containerColor = Pink_Jackie)) {
                            Text("Save", color = Color.White, fontFamily = Oswald)
                        }
                    }
                }
            }
        }
    } else {
        // Preview-friendly fallback
        AlertDialog(
            onDismissRequest = onCancel,
            title = { Text("Choose Display Name", fontFamily = Oswald, fontWeight = FontWeight.Bold, color = LightText) },
            text = {
                Column {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = TextStyle(fontFamily = Oswald, color = LightText),
                        singleLine = true,
                        placeholder = { Text("Player Name", color = LightText.copy(alpha = 0.6f)) },
                        isError = errorText != null
                    )
                    if (errorText != null) Text(errorText!!, color = MaterialTheme.colorScheme.error, fontFamily = Oswald)
                }
            },
            confirmButton = { Button(onClick = { attemptSave() }) { Text("Save") } },
            dismissButton = { TextButton(onClick = onCancel) { Text("Cancel") } }
        )
    }

    LaunchedEffect(Unit) {
        if (isInPreview) return@LaunchedEffect
        repeat(6) {
            try {
                focusRequester.requestFocus()
                return@LaunchedEffect
            } catch (_: Throwable) {
                delay(50L)
            }
        }
        try { focusRequester.requestFocus() } catch (_: Throwable) {}
    }
}

fun generateFallbackPlayerName(): String {
    val digits = Random.nextInt(0, 10_000)
    return "Player%04d".format(digits)
}
