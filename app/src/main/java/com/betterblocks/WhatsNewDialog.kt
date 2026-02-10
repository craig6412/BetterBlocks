package com.betterblocks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Keep update text in one place so it's easy to edit each release.
private val WHATS_NEW_LINES = listOf(
    "✅ Pricing fixes: coin pack prices now match the correct packs",
    "🎨 Color Wheel update: animation improvements",
    "🏷️ Coupon codes: the coupon code section is now ready and functional",
    "🎯 Color Wheel improvement: it always lands on a color that exists on the board — no more gamble"
)

@Composable
fun WhatsNewDialog(onDismiss: () -> Unit) {
    val panelBg = Color(0xFF0E0F14).copy(alpha = 0.92f)

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = panelBg),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "What’s New",
                    color = Pink_Jackie,
                    fontFamily = Oswald,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 22.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                WHATS_NEW_LINES.forEach { line ->
                    Text(
                        text = line,
                        color = LightText,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(onClick = onDismiss) {
                        Text(text = "Got it", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

