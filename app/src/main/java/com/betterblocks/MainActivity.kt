package com.betterblocks

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.betterblocks.ui.theme.BetterBlocksTheme // Corrected/Confirmed Import for the Theme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // --- FIX: Start the GameActivity instead of showing "Hello Android" ---
        // We launch the GameActivity immediately to start the game screen.
        val intent = Intent(this, GameActivity::class.java) // FIX: Correctly reference GameActivity
        startActivity(intent)

        // Finish this main activity so the user doesn't return to it when pressing back
        finish()
        // --- END FIX ---

        setContent {
            // FIX: Ensure BetterBlocksTheme is imported and used
            BetterBlocksTheme {
                // This content is never seen because we immediately jump to GameActivity,
                // but we keep the structure for compliance.
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Greeting("Android")
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    // FIX: Ensure BetterBlocksTheme is imported and used
    BetterBlocksTheme {
        Greeting("Android")
    }
}