package com.betterblocks

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.betterblocks.ui.DarkBackground
import com.betterblocks.ui.LightText
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp



@Composable
fun SettingsScreen(
    initialSoundEnabled: Boolean,
    initialHapticEnabled: Boolean,
    initialDarkTheme: Boolean,
    onToggleSound: () -> Unit,
    onToggleHaptic: () -> Unit,
    onToggleTheme: () -> Unit,
    onBack: () -> Unit
) {
    var soundEnabled by remember { mutableStateOf(initialSoundEnabled) }
    var hapticEnabled by remember { mutableStateOf(initialHapticEnabled) }
    var darkTheme by remember { mutableStateOf(initialDarkTheme) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = DarkBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start
        ) {

            // TITLE
            Text(
                text = "Settings",
                color = LightText,
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(modifier = Modifier.height(30.dp))

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

            Spacer(modifier = Modifier.height(20.dp))

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

            Spacer(modifier = Modifier.height(20.dp))

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

            Spacer(modifier = Modifier.height(40.dp))

            Button(
                onClick = onBack,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text("Back")
            }
        }
    }
}
