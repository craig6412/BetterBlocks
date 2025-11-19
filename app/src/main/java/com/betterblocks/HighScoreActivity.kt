package com.betterblocks

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class HighscoresActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_placeholder)
        // Setup simple UI for now
        findViewById<TextView>(R.id.placeholderText).text = "Highscores Leaderboard Coming Soon!"
        title = "Highscores"
    }
}