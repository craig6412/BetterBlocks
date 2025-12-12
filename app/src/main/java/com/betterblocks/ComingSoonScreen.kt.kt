package com.betterblocks.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.betterblocks.DarkBackground
import com.betterblocks.LightText
import com.betterblocks.Oswald
import com.betterblocks.Pink_Jackie
import androidx.compose.ui.tooling.preview.Preview
import com.betterblocks.ui.sdp
import com.betterblocks.ui.ssp
import com.betterblocks.ui.sw

@Composable
fun ComingSoonScreen(title: String, onBack: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = DarkBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(sdp(0.03f)),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Box(modifier = Modifier.fillMaxWidth()) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.align(Alignment.CenterStart)
                ) {
                    Icon(
                        imageVector = Icons.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = LightText
                    )
                }
                Text(
                    text = title,
                    color = LightText,
                    fontSize = ssp(0.035f),
                    fontFamily = Oswald,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Content
            Icon(
                imageVector = Icons.Filled.Build,
                contentDescription = null,
                tint = Pink_Jackie,
                modifier = Modifier.size(sw(0.12f))
            )
            Spacer(modifier = Modifier.height(sdp(0.03f)))
            Text(
                text = "COMING SOON",
                color = Pink_Jackie,
                fontSize = ssp(0.04f),
                fontFamily = Oswald,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(modifier = Modifier.height(sdp(0.01f)))
            Text(
                text = "We are building something awesome!",
                color = LightText.copy(alpha = 0.7f),
                fontSize = ssp(0.02f),
                fontFamily = Oswald
            )

            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Preview(
    name = "Tablet – Portrait",
    showBackground = true,
    showSystemUi = true,
    device = "spec:width=800dp,height=1280dp,dpi=480"
)
@Composable
fun ComingSoonScreenPreview() {
    ComingSoonScreen(title = "Coming Soon", onBack = {})
}
