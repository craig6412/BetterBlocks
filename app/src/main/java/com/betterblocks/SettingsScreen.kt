package com.betterblocks

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import com.betterblocks.coupon.CouponLedger
import com.betterblocks.ui.sdp



@Composable
fun SettingsScreen(
    initialSoundEnabled: Boolean,
    initialHapticEnabled: Boolean,
    initialDarkTheme: Boolean,
    initialHighscoreNotifications: Boolean,
    onToggleSound: () -> Unit,
    onToggleHaptic: () -> Unit,
    onToggleTheme: () -> Unit,
    onToggleHighscoreNotifications: () -> Unit,
    onBack: () -> Unit
) {
    var soundEnabled by remember { mutableStateOf(initialSoundEnabled) }
    var hapticEnabled by remember { mutableStateOf(initialHapticEnabled) }
    var darkTheme by remember { mutableStateOf(initialDarkTheme) }
    var highscoreNotifications by remember { mutableStateOf(initialHighscoreNotifications) }

    val context = LocalContext.current

    // --- Coupon Code Center state ---
    var couponInput by remember { mutableStateOf("") }
    var couponStatus by remember { mutableStateOf<String?>(null) }
    var couponProcessing by remember { mutableStateOf(false) }

    val couponLedger = remember { CouponLedger.get(context) }

    fun applyCoupon() {
        if (couponProcessing) return
        couponProcessing = true
        couponStatus = null

        // Redeem synchronously; ledger is thread-safe and idempotent.
        val result = couponLedger.redeem(couponInput)
        couponStatus = when (result) {
            is CouponLedger.Result.Success -> "Success! 100,000 coins added."
            is CouponLedger.Result.Invalid -> "Invalid coupon code."
            is CouponLedger.Result.AlreadyRedeemed -> "This code has already been redeemed."
        }

        // If success, clear input for better UX (optional, UI-only)
        if (result is CouponLedger.Result.Success) {
            couponInput = ""
        }

        couponProcessing = false
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = DarkBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(sdp(0.03f)),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start
        ) {

            // TITLE
            Text(
                text = "Settings",
                color = LightText,
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(modifier = Modifier.height(sdp(0.03f)))

            // THEME TOGGLE
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (darkTheme) "Dark Theme" else "Light Theme",
                    color = LightText,
                    style = MaterialTheme.typography.titleMedium
                )

                Switch(
                    checked = darkTheme,
                    onCheckedChange = {
                        darkTheme = it
                        onToggleTheme()
                    }
                )
            }

            Spacer(modifier = Modifier.height(sdp(0.02f)))

            // SOUND TOGGLE
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {

                Text(
                    text = "Sound Effects",
                    color = LightText,
                    style = MaterialTheme.typography.titleMedium
                )

                Switch(
                    checked = soundEnabled,
                    onCheckedChange = {
                        soundEnabled = it
                        onToggleSound()
                    }
                )
            }

            Spacer(modifier = Modifier.height(sdp(0.02f)))

            // HAPTIC TOGGLE
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {

                Text(
                    text = "Haptic Feedback",
                    color = LightText,
                    style = MaterialTheme.typography.titleMedium
                )

                Switch(
                    checked = hapticEnabled,
                    onCheckedChange = {
                        hapticEnabled = it
                        onToggleHaptic()
                    }
                )
            }

            Spacer(modifier = Modifier.height(sdp(0.02f)))

            // HIGHSCORE NOTIFICATIONS TOGGLE
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Highscore Notifications",
                    color = LightText,
                    style = MaterialTheme.typography.titleMedium
                )

                Switch(
                    checked = highscoreNotifications,
                    onCheckedChange = {
                        highscoreNotifications = it
                        onToggleHighscoreNotifications()
                    }
                )
            }

            Spacer(modifier = Modifier.height(sdp(0.03f)))

            // -------------------------------
            // Coupon Code Center
            // -------------------------------
            Text(
                text = "Coupon Code Center",
                color = LightText,
                style = MaterialTheme.typography.titleLarge
            )

            Spacer(modifier = Modifier.height(sdp(0.015f)))

            OutlinedTextField(
                value = couponInput,
                onValueChange = { couponInput = it },
                label = { Text("Enter coupon code") },
                singleLine = true,
                enabled = !couponProcessing,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(sdp(0.015f)))

            Button(
                onClick = { applyCoupon() },
                enabled = !couponProcessing,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("APPLY")
            }

            couponStatus?.let { msg ->
                Spacer(modifier = Modifier.height(sdp(0.01f)))
                Text(
                    text = msg,
                    color = LightText,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.height(sdp(0.04f)))

            Button(
                onClick = onBack,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text("Back")
            }
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
fun SettingsScreenPreview() {
    val vm = PreviewSettingsViewModel()
    SettingsScreen(
        initialSoundEnabled = vm.isSoundEnabled,
        initialHapticEnabled = vm.isMusicEnabled,
        initialDarkTheme = true,
        initialHighscoreNotifications = true,
        onToggleSound = {},
        onToggleHaptic = {},
        onToggleTheme = {},
        onToggleHighscoreNotifications = {},
        onBack = {}
    )
}
