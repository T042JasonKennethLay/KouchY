package com.yatai.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint

internal fun argb(alpha: Int, rgb: Int) = (alpha.coerceIn(0, 255) shl 24) or (rgb and 0xFFFFFF)

internal fun mix(a: Int, b: Int, t: Float): Int {
    fun ch(shift: Int) = (((a shr shift) and 0xFF) * (1 - t) + ((b shr shift) and 0xFF) * t).toInt()
    return (ch(16) shl 16) or (ch(8) shl 8) or ch(0)
}

internal class PixelCanvas(bmp: Bitmap) {
    private val canvas = Canvas(bmp)
    private val paint = Paint().apply { isAntiAlias = false; isDither = false }

    fun rect(x: Int, y: Int, w: Int, h: Int, color: Int) {
        paint.color = if (color ushr 24 == 0) color or 0xFF000000.toInt() else color
        canvas.drawRect(x.toFloat(), y.toFloat(), (x + w).toFloat(), (y + h).toFloat(), paint)
    }

    fun px(x: Int, y: Int, color: Int) = rect(x, y, 1, 1, color)

    fun sprite(rows: List<String>, x: Int, y: Int, palette: Map<Char, Int>) {
        rows.forEachIndexed { dy, row ->
            row.forEachIndexed { dx, ch -> palette[ch]?.let { px(x + dx, y + dy, it) } }
        }
    }

    fun disk(cx: Int, cy: Int, r: Int, color: Int, clip: IntArray? = null) =
        disk(cx, cy, r, clip) { _, _ -> color }

    fun disk(cx: Int, cy: Int, r: Int, clip: IntArray? = null, colorAt: (Int, Int) -> Int) {
        for (dy in -r..r) for (dx in -r..r) {
            if (dx * dx + dy * dy > r * r + r) continue
            val x = cx + dx
            val y = cy + dy
            if (clip != null && (x < clip[0] || y < clip[1] || x > clip[2] || y > clip[3])) continue
            px(x, y, colorAt(x, y))
        }
    }
}
