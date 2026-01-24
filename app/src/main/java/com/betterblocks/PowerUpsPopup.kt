package com.betterblocks.ui

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.betterblocks.KEY_POWERUP_POPUP_SHOWN_V1

/**
 * Returns whether the popup was already shown.
 */
fun hasShownPowerUpPopup(context: Context): Boolean {
    val prefs = context.getSharedPreferences("BetterBlocksPrefs", Context.MODE_PRIVATE)

    // Back-compat: if the legacy flag was already set, treat this as shown.
    val legacyShown = prefs.getBoolean("tutorialShown", false)
    if (legacyShown) return true

    return prefs.getBoolean(KEY_POWERUP_POPUP_SHOWN_V1, false)
}

/**
 * Sets the popup as shown.
 */
fun setPowerUpPopupShown(context: Context) {
    val prefs = context.getSharedPreferences("BetterBlocksPrefs", Context.MODE_PRIVATE)
    // Keep legacy flag set too so older versions won't re-show it.
    prefs.edit()
        .putBoolean(KEY_POWERUP_POPUP_SHOWN_V1, true)
        .putBoolean("tutorialShown", true)
        .apply()
}

/**
 * Dismissible popup explaining power-ups.
 */
@Composable
fun PowerUpsPopup(
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = { onDismiss() }) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.55f)),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Power-Ups Guide",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = "• Rainbow Wipe: Fill the Rainbow Meter by clearing lines. When full, you earn one wipe that clears the entire board.",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Text(
                        text = "• Color Wheel:  Clears all blocks of one chosen color. Earned during streaks or purchased in the Shop.",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Text(
                        text = "•Rotations: 3 free rotations per game, after though it is 10 coins per block.",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = { onDismiss() },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Got It")
                    }
                }
            }
        }
    }
}
