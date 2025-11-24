package com.betterblocks

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool

object SoundManager {

    private lateinit var soundPool: SoundPool

    private var blockPlace = 0
    private var blockBadPlace = 0
    private var lineClear = 0
    private var rainbowClear = 0
    private var wheelSpin = 0

    private var wheelStreamId = 0

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

    fun playBlockPlace() {
        soundPool.play(blockPlace, 1f, 1f, 1, 0, 1f)
    }

    fun playBadPlacement() {
        soundPool.play(blockBadPlace, 1f, 1f, 1, 0, 1f)
    }

    fun playLineClear() {
        soundPool.play(lineClear, 1f, 1f, 1, 0, 1f)
    }

    fun playRainbowClear() {
        soundPool.play(rainbowClear, 1f, 1f, 1, 0, 1f)
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
}
