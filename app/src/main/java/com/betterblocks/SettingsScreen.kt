package com.betterblocks

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.Preview
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
    onApplyCoupon: (String) -> CouponManager.CouponResult,
    onBack: () -> Unit
) {
    var soundEnabled by remember { mutableStateOf(initialSoundEnabled) }
    var hapticEnabled by remember { mutableStateOf(initialHapticEnabled) }
    var darkTheme by remember { mutableStateOf(initialDarkTheme) }
    var highscoreNotifications by remember { mutableStateOf(initialHighscoreNotifications) }

    var couponCode by remember { mutableStateOf("") }
    var couponStatus by remember { mutableStateOf<String?>(null) }
    var isApplyingCoupon by remember { mutableStateOf(false) }

    val isInPreview = LocalInspectionMode.current

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

            Spacer(modifier = Modifier.height(sdp(0.04f)))

            // COUPON CODE
            Text(
                text = "Coupon Code",
                color = LightText,
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(sdp(0.01f)))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = couponCode,
                    onValueChange = {
                        couponCode = it
                        couponStatus = null
                    },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    enabled = !isApplyingCoupon,
                    placeholder = { Text("Enter code") }
                )

                Spacer(modifier = Modifier.width(sdp(0.02f)))

                Button(
                    onClick = {
                        if (isInPreview) return@Button
                        isApplyingCoupon = true
                        val result = onApplyCoupon(couponCode)
                        couponStatus = when (result) {
                            is CouponManager.CouponResult.Success -> {
                                couponCode = ""
                                "Coupon applied! +${result.coinsGranted} coins"
                            }

                            CouponManager.CouponResult.Invalid -> "Invalid code"
                            CouponManager.CouponResult.AlreadyUsed -> "Code already used"
                        }
                        isApplyingCoupon = false
                    },
                    enabled = !isApplyingCoupon && couponCode.trim().isNotEmpty()
                ) {
                    Text("Apply")
                }
            }

            if (couponStatus != null) {
                Spacer(modifier = Modifier.height(sdp(0.01f)))
                Text(
                    text = couponStatus!!,
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
        onApplyCoupon = { CouponManager.CouponResult.Invalid },
        onBack = {}
    )
}
