package com.rumahku.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import kotlin.math.roundToInt

object PixelUi {
    const val PINK = 0xFFFF6AD5.toInt()
    const val CYAN = 0xFF6AE0FF.toInt()
    const val YELLOW = 0xFFFFE27A.toInt()
    const val INK = 0xFF0E0A1C.toInt()
    const val CREAM = 0xFFFFF3E0.toInt()
    const val MUTED = 0xFFA898C8.toInt()

    val ICON_BOWL = listOf("..w.w.w..", "...w.w...", "..w.w.w..", "YYYYYYYYY", "RRRRRRRRR", ".RRWRRRR.", "..RRRRR..")
    val ICON_PERSON = listOf("..HHH..", ".HHHHH.", ".HSSSH.", "..SSS..", ".PPPPP.", "PPPPPPP")
    val ICON_EYE = listOf("..WWW..", ".W...W.", "W..C..W", ".W...W.", "..WWW..")
    val ICON_STAR = listOf("...Y...", "..YYY..", "YYYYYYY", ".YYYYY.", ".Y...Y.")
    val ICON_HEART = listOf(".RR.RR.", "RRRRRRR", "RRRRRRR", ".RRRRR.", "..RRR..", "...R...")
    val ICON_PALETTE = mapOf(
        'w' to 0xFFFFFF, 'Y' to 0xF2D27A, 'R' to 0xD9483B, 'W' to 0xFFF3E0, 'H' to 0x2E2230,
        'S' to 0xFBD8BE, 'P' to 0xFF6AD5, 'C' to 0x6AE0FF,
    )

    fun dp(ctx: Context, value: Float) = value * ctx.resources.displayMetrics.density

    fun drawable(ctx: Context, bmp: Bitmap) =
        BitmapDrawable(ctx.resources, bmp).apply { isFilterBitmap = false }

    fun setBitmap(view: ImageView, bmp: Bitmap, pixelDp: Float) {
        view.setImageDrawable(drawable(view.context, bmp))
        view.scaleType = ImageView.ScaleType.FIT_XY
        val lp = view.layoutParams ?: ViewGroup.LayoutParams(0, 0)
        lp.width = (bmp.width * dp(view.context, pixelDp)).roundToInt()
        lp.height = (bmp.height * dp(view.context, pixelDp)).roundToInt()
        view.layoutParams = lp
    }

    fun setText(view: ImageView, text: String, color: Int, pixelDp: Float, shadow: Int = 0x0E0A1C) =
        setBitmap(view, PixelFont.bitmap(text, color, shadow), pixelDp)

    fun image(ctx: Context, bmp: Bitmap, pixelDp: Float) = ImageView(ctx).also { setBitmap(it, bmp, pixelDp) }

    fun icon(rows: List<String>): Bitmap {
        val bmp = Bitmap.createBitmap(rows.maxOf { it.length }, rows.size, Bitmap.Config.ARGB_8888)
        PixelCanvas(bmp).sprite(rows, 0, 0, ICON_PALETTE)
        return bmp
    }

    fun backdrop(ctx: Context): BitmapDrawable {
        val size = 48
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val c = PixelCanvas(bmp)
        c.rect(0, 0, size, size, 0x15112A)
        listOf(3 to 5, 17 to 2, 29 to 11, 41 to 6, 9 to 23, 35 to 27, 22 to 38, 44 to 41, 5 to 43, 26 to 19)
            .forEachIndexed { i, (x, y) -> c.px(x, y, intArrayOf(0x3A2E5A, 0x5A4A84, 0x6AE0FF, 0xFF6AD5)[i % 4]) }
        listOf(13 to 31, 39 to 16).forEach { (x, y) ->
            c.px(x, y, 0xFFF3E0)
            c.px(x - 1, y, 0x5A4A84); c.px(x + 1, y, 0x5A4A84)
            c.px(x, y - 1, 0x5A4A84); c.px(x, y + 1, 0x5A4A84)
        }
        val scale = dp(ctx, 3f).roundToInt().coerceAtLeast(1)
        val scaled = Bitmap.createScaledBitmap(bmp, size * scale, size * scale, false)
        return BitmapDrawable(ctx.resources, scaled).apply {
            isFilterBitmap = false
            setTileModeXY(Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
            setTargetDensity(scaled.density)
        }
    }

    fun fillHeader(container: LinearLayout, iconRows: List<String>, title: String, color: Int) {
        val ctx = container.context
        container.removeAllViews()
        container.orientation = LinearLayout.HORIZONTAL
        container.gravity = Gravity.CENTER_VERTICAL
        container.addView(image(ctx, icon(iconRows), 3f))
        val titleView = image(ctx, PixelFont.bitmap(title, color, 0x0E0A1C), 3f)
        titleView.layoutParams = LinearLayout.LayoutParams(titleView.layoutParams.width, titleView.layoutParams.height).apply {
            marginStart = dp(ctx, 10f).roundToInt()
        }
        container.addView(titleView)
    }

    fun chip(ctx: Context, text: String, onClick: () -> Unit) = Button(ctx).apply {
        this.text = text
        isAllCaps = false
        textSize = 12f
        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        setTextColor(ctx.getColorStateList(R.color.chip_text))
        background = ctx.getDrawable(R.drawable.btn_chip)
        stateListAnimator = null
        minHeight = dp(ctx, 44f).roundToInt()
        minimumHeight = minHeight
        minWidth = 0
        minimumWidth = 0
        val h = dp(ctx, 16f).roundToInt()
        setPadding(h, 0, h, dp(ctx, 4f).roundToInt())
        setOnClickListener { onClick() }
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT,
        ).apply { marginEnd = dp(ctx, 6f).roundToInt(); topMargin = dp(ctx, 6f).roundToInt() }
    }

    fun swatch(ctx: Context, color: Int, selected: Boolean, onClick: () -> Unit) = View(ctx).apply {
        val size = dp(ctx, 36f).roundToInt()
        background = GradientDrawable().apply {
            setColor(color or 0xFF000000.toInt())
            setStroke(dp(ctx, if (selected) 4f else 3f).roundToInt(), if (selected) YELLOW else INK)
        }
        setOnClickListener { onClick() }
        layoutParams = LinearLayout.LayoutParams(size, size).apply {
            marginEnd = dp(ctx, 8f).roundToInt()
            topMargin = dp(ctx, 6f).roundToInt()
        }
    }

    fun label(ctx: Context, text: String, color: Int = MUTED, sizeSp: Float = 12f) = TextView(ctx).apply {
        this.text = text
        setTextColor(color)
        textSize = sizeSp
        typeface = Typeface.MONOSPACE
    }
}
