// BlockTextures.kt
package com.betterblocks

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.drawable.BitmapDrawable
import android.util.Log
import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter

/**
 * ONLY drawables that may be rendered inside the board grid.
 * These must be PNG/JPG/WEBP or VectorDrawable.
 */
val BLOCK_TEXTURE_DRAWABLES = listOf(
    R.drawable.blue,
    R.drawable.green,
    R.drawable.red,
    R.drawable.yellow,
    R.drawable.purple,
    R.drawable.pumpkin_orange
)

/**
 * Pre-decoded block textures. Call [init] once at app start (e.g. Application.onCreate).
 * After that, [getPainter] returns an immediately available [BitmapPainter] with zero
 * disk I/O or Bitmap allocation — critical for the board cell hot path during drag.
 */
object BlockTextureCache {
    private val cache = HashMap<Int, ImageBitmap>(16)

    fun init(context: Context) {
        // Decode every texture that can appear on the game board.
        // BLOCK_DRAWABLES covers all 8 playable colours + rainbow.
        val ids = BLOCK_DRAWABLES.distinct()
        for (id in ids) {
            try {
                val dr = AppCompatResources.getDrawable(context, id) ?: continue
                val bmp: Bitmap = when (dr) {
                    is BitmapDrawable -> dr.bitmap
                    else -> {
                        val w = dr.intrinsicWidth.coerceAtLeast(1)
                        val h = dr.intrinsicHeight.coerceAtLeast(1)
                        val b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                        dr.setBounds(0, 0, w, h)
                        dr.draw(AndroidCanvas(b))
                        b
                    }
                }
                cache[id] = bmp.asImageBitmap()
            } catch (t: Throwable) {
                Log.w("BlockTextureCache", "Failed to pre-cache drawable id=$id: ${t.message}")
            }
        }
        Log.d("BlockTextureCache", "Pre-cached ${cache.size} block textures")
    }

    /** Returns a [BitmapPainter] for the given drawable resource ID, or null if not cached. */
    fun getPainter(resId: Int): Painter? = cache[resId]?.let { BitmapPainter(it) }

    /** Direct bitmap access (for callers that need ImageBitmap). */
    fun getBitmap(resId: Int): ImageBitmap? = cache[resId]
}