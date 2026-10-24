package com.yatai.widget

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.widget.FrameLayout
import kotlin.math.roundToInt

class PixelButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : FrameLayout(context, attrs) {

    private val label = PixelTextView(context).apply {
        centered = true
        shadow = null
        pixelDp = 2.5f
    }

    var text: String
        get() = label.text
        set(value) {
            label.text = value
        }

    var normalColor: Int = DARK
        set(value) {
            field = value
            refreshLabel()
        }

    var selectedColor: Int = DARK
        set(value) {
            field = value
            refreshLabel()
        }

    init {
        isClickable = true
        isFocusable = true
        minimumHeight = dp(54f)
        setPadding(dp(14f), dp(7f), dp(14f), dp(11f))
        addView(label, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, Gravity.CENTER))
        if (attrs != null) {
            val a = context.obtainStyledAttributes(attrs, R.styleable.PixelTextView)
            try {
                label.text = a.getString(R.styleable.PixelTextView_pixelText).orEmpty()
                label.pixelDp = a.getFloat(R.styleable.PixelTextView_pixelSize, label.pixelDp)
                normalColor = a.getColor(R.styleable.PixelTextView_pixelColor, DARK)
                selectedColor = normalColor
            } finally {
                a.recycle()
            }
        }
        refreshLabel()
    }

    fun setPixelSize(value: Float) {
        label.pixelDp = value
    }

    override fun drawableStateChanged() {
        super.drawableStateChanged()
        refreshLabel()
    }

    private fun refreshLabel() {
        @Suppress("USELESS_CAST", "SENSELESS_COMPARISON")
        val label = (label as PixelTextView?) ?: return
        label.translationY = if (isPressed) dp(2f).toFloat() else 0f
        label.color = when {
            !isEnabled -> DISABLED
            isSelected -> selectedColor
            else -> normalColor
        }
    }

    private fun dp(value: Float) = PixelUi.dp(context, value).roundToInt()

    companion object {
        const val DARK = 0xFF1A1424.toInt()
        const val DISABLED = 0xFF8A8494.toInt()
    }
}
