package com.betterblocks

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool

/**
 * ENHANCED SOUND MANAGER
 * Now includes event deduplication to prevent duplicate SFX
 */
object SoundManager {

    private lateinit var soundPool: SoundPool

    private var blockPlace = 0
    private var blockBadPlace = 0
    private var lineClear = 0
    private var rainbowClear = 0
    private var wheelSpin = 0

    private var wheelStreamId = 0

    // Event deduplication: Track last play time for each sound
    private val lastPlayTimes = mutableMapOf<String, Long>()
    private const val DEDUPE_WINDOW_MS = 150L  // Prevent same sound within 150ms

    fun init(context: Context) {
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(8)
            .setAudioAttributes(attrs)
            .build()

        blockPlace = soundPool.load(context, R.raw.block_placement, 1)
        blockBadPlace = soundPool.load(context, R.raw.block_bad_placement, 1)
        lineClear = soundPool.load(context, R.raw.line_clear, 1)
        rainbowClear = soundPool.load(context, R.raw.rainbow_block_clear, 1)
        wheelSpin = soundPool.load(context, R.raw.color_wheel_spin, 1)
    }

    /**
     * Helper function: Play sound only if not recently played (deduplication)
     */
    private fun playWithDedupe(soundId: Int, eventKey: String, volume: Float = 1f) {
        val now = System.currentTimeMillis()
        val lastPlayTime = lastPlayTimes[eventKey] ?: 0L

        if (now - lastPlayTime > DEDUPE_WINDOW_MS) {
            soundPool.play(soundId, volume, volume, 1, 0, 1f)
            lastPlayTimes[eventKey] = now
        } else {
            // Duplicate detected - skip this play
            android.util.Log.d("SoundManager", "⚠️ Duplicate $eventKey blocked (within ${now - lastPlayTime}ms)")
        }
    }

    fun playBlockPlace() {
        playWithDedupe(blockPlace, "block_place")
    }

    fun playBadPlacement() {
        playWithDedupe(blockBadPlace, "bad_place")
    }

    fun playLineClear() {
        playWithDedupe(lineClear, "line_clear")
    }

    fun playRainbowClear() {
        playWithDedupe(rainbowClear, "rainbow_clear")
    }

    fun startWheelSpinLoop() {
        wheelStreamId = soundPool.play(wheelSpin, 0.8f, 0.8f, 1, -1, 1f)
    }

    fun stopWheelSpinLoop() {
        if (wheelStreamId != 0) {
            soundPool.stop(wheelStreamId)
            wheelStreamId = 0
        }
    }

    /**
     * Manual reset for testing or edge cases
     */
    fun resetDedupeTimers() {
        lastPlayTimes.clear()
    }
}
