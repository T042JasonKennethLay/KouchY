package com.rumahku.widget

import android.app.Activity
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.View
import android.view.ViewGroup
import android.view.Window
import kotlin.math.roundToInt

object PixelDialog {
    fun show(
        activity: Activity,
        title: String,
        message: String,
        confirm: String,
        cancel: String? = null,
        onConfirm: () -> Unit = {},
    ) {
        val dialog = Dialog(activity)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.pixel_dialog)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        val width = activity.resources.displayMetrics.widthPixels - PixelUi.dp(activity, 48f).roundToInt()
        dialog.window?.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)

        dialog.findViewById<PixelTextView>(R.id.dialog_title).text = title
        dialog.findViewById<PixelTextView>(R.id.dialog_message).text = message
        dialog.findViewById<PixelButton>(R.id.dialog_confirm).apply {
            text = confirm
            setOnClickListener {
                dialog.dismiss()
                onConfirm()
            }
        }
        dialog.findViewById<PixelButton>(R.id.dialog_cancel).apply {
            if (cancel == null) {
                visibility = View.GONE
            } else {
                text = cancel
                normalColor = PixelUi.CREAM
                setOnClickListener { dialog.dismiss() }
            }
        }
        dialog.show()
    }
}
