package com.betterblocks

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.betterblocks.ui.sdp
import com.betterblocks.ui.ssp



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
