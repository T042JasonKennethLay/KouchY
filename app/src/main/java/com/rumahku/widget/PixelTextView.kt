package com.rumahku.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import kotlin.math.roundToInt

class PixelTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {

    var text: String = ""
        set(value) {
            field = value
            requestLayout()
            invalidate()
        }

    var color: Int = PixelUi.CREAM
        set(value) {
            field = value
            invalidate()
        }

    var shadow: Int? = PixelUi.INK
        set(value) {
            field = value
            invalidate()
        }

    var pixelDp: Float = 2.5f
        set(value) {
            field = value
            requestLayout()
            invalidate()
        }

    var centered: Boolean = false
        set(value) {
            field = value
            invalidate()
        }

    private var lines: List<String> = emptyList()
    private val paint = Paint().apply { isAntiAlias = false }
    private val unit get() = PixelUi.dp(context, pixelDp).roundToInt().coerceAtLeast(1)

    init {
        if (attrs != null) {
            val a = context.obtainStyledAttributes(attrs, R.styleable.PixelTextView)
            try {
                text = a.getString(R.styleable.PixelTextView_pixelText).orEmpty()
                color = a.getColor(R.styleable.PixelTextView_pixelColor, color)
                pixelDp = a.getFloat(R.styleable.PixelTextView_pixelSize, pixelDp)
                centered = a.getBoolean(R.styleable.PixelTextView_pixelCentered, false)
                if (!a.getBoolean(R.styleable.PixelTextView_pixelShadow, true)) shadow = null
            } finally {
                a.recycle()
            }
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val u = unit
        val mode = MeasureSpec.getMode(widthMeasureSpec)
        val size = MeasureSpec.getSize(widthMeasureSpec)
        val horizontal = paddingLeft + paddingRight
        val maxUnits = if (mode == MeasureSpec.UNSPECIFIED) Int.MAX_VALUE else ((size - horizontal) / u - 1).coerceAtLeast(4)
        lines = wrap(text.uppercase(), maxUnits)

        val contentUnits = (lines.maxOfOrNull { PixelFont.width(it) } ?: 0) + 1
        val desired = contentUnits * u + horizontal
        val width = when (mode) {
            MeasureSpec.EXACTLY -> size
            MeasureSpec.AT_MOST -> minOf(desired, size)
            else -> desired
        }
        val heightUnits = if (lines.isEmpty()) 0 else lines.size * LINE - (LINE - PixelFont.HEIGHT) + 1
        setMeasuredDimension(width, resolveSize(heightUnits * u + paddingTop + paddingBottom, heightMeasureSpec))
    }

    override fun onDraw(canvas: Canvas) {
        val u = unit
        val inner = width - paddingLeft - paddingRight
        lines.forEachIndexed { i, line ->
            val lineWidth = (PixelFont.width(line) + 1) * u
            val x0 = paddingLeft + if (centered) (inner - lineWidth) / 2 else 0
            val y0 = paddingTop + i * LINE * u
            shadow?.let { s ->
                paint.color = s or 0xFF000000.toInt()
                PixelFont.forEachPixel(line) { x, y -> block(canvas, x0, y0, x + 1, y + 1, u) }
            }
            paint.color = color
            PixelFont.forEachPixel(line) { x, y -> block(canvas, x0, y0, x, y, u) }
        }
    }

    private fun block(canvas: Canvas, x0: Int, y0: Int, x: Int, y: Int, u: Int) {
        val left = (x0 + x * u).toFloat()
        val top = (y0 + y * u).toFloat()
        canvas.drawRect(left, top, left + u, top + u, paint)
    }

    private fun wrap(source: String, maxUnits: Int): List<String> {
        if (source.isEmpty()) return emptyList()
        val out = mutableListOf<String>()
        source.split('\n').forEach { paragraph ->
            var line = ""
            paragraph.split(' ').filter { it.isNotEmpty() }.forEach { word ->
                val candidate = if (line.isEmpty()) word else "$line $word"
                if (line.isEmpty() || PixelFont.width(candidate) <= maxUnits) {
                    line = candidate
                } else {
                    out += line
                    line = word
                }
            }
            out += line
        }
        return out
    }

    private companion object {
        const val LINE = 8
    }
}
