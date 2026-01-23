package com.betterblocks.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.betterblocks.ads.AdManager

@Composable
fun ZeroCoinsDialog(
    onDismiss: () -> Unit,
    onWatchAd: () -> Unit,
    onGoToShop: () -> Unit
) {
    val isLoaded = AdManager.isRewardedLoaded.value

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MonetizationOn,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp)
                )
                Text(text = "Out of Coins", fontSize = 22.sp)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Watch an ad to get ${AdManager.REWARDED_COINS} coins.",
                    style = MaterialTheme.typography.bodyMedium
                )

                if (!isLoaded) {
                    Text(
                        text = "Loading ad…",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onWatchAd,
                enabled = isLoaded
            ) {
                Text(text = "WATCH AD (${AdManager.REWARDED_COINS} COINS)")
            }
        },
        dismissButton = {
            Button(
                onClick = onGoToShop,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Text(text = "Go to Shop")
            }
        }
    )
}
