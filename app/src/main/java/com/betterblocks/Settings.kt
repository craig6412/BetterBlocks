package com.betterblocks

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_placeholder)
        findViewById<TextView>(R.id.placeholderText).text = "Settings: Volume, Controls, etc."
        title = "Settings"
    }
}