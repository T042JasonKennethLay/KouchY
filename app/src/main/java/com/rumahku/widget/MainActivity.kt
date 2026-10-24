package com.rumahku.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.graphics.drawable.AnimationDrawable
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    private var previewMood: Mood? = null
    private var previewPhase: DayPhase? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<View>(R.id.preview_frame).clipToOutline = true
        findViewById<Button>(R.id.btn_permission).setOnClickListener { openUsageSettings() }
        findViewById<Button>(R.id.btn_pin).setOnClickListener { pinWidget() }

        val moodRow = findViewById<LinearLayout>(R.id.mood_row)
        moodRow.addView(chip("Asli") { previewMood = null; previewPhase = null; render() })
        Mood.entries.forEach { mood -> moodRow.addView(chip(mood.label) { previewMood = mood; render() }) }

        val phaseRow = findViewById<LinearLayout>(R.id.phase_row)
        DayPhase.entries.forEach { phase -> phaseRow.addView(chip(phase.label) { previewPhase = phase; render() }) }
    }

    override fun onResume() {
        super.onResume()
        render()
        WidgetUpdater.updateAll(this)
        UpdateScheduler.schedule(this)
    }

    private fun render() {
        val stats = PhoneUsage.read(this)
        val hour = PhoneUsage.currentHour()
        val mood = previewMood ?: MoodEngine.decide(stats, hour)
        val phase = previewPhase ?: DayPhase.of(hour)

        val anim = AnimationDrawable().apply {
            isOneShot = false
            SceneRenderer.renderFrames(SceneState(mood, phase)).forEach { frame ->
                addFrame(BitmapDrawable(resources, frame).apply { isFilterBitmap = false }, SceneRenderer.FRAME_MS)
            }
            isFilterBitmap = false
        }
        findViewById<ImageView>(R.id.preview).setImageDrawable(anim)
        anim.start()

        findViewById<TextView>(R.id.preview_bubble).text = mood.bubble(phase)
        findViewById<TextView>(R.id.mood_title).text = mood.title
        findViewById<TextView>(R.id.mood_line).text = mood.line
        findViewById<TextView>(R.id.stats).text = PhoneUsage.formatStats(stats)
        findViewById<Button>(R.id.btn_permission).visibility =
            if (stats.hasPermission) View.GONE else View.VISIBLE
    }

    private fun chip(text: String, onClick: () -> Unit) = Button(this).apply {
        this.text = text
        isAllCaps = false
        textSize = 12f
        setOnClickListener { onClick() }
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT,
        ).apply { marginEnd = 8 }
    }

    private fun openUsageSettings() {
        Toast.makeText(this, "Cari \"Rumahku\" lalu nyalakan izinnya ya", Toast.LENGTH_LONG).show()
        try {
            startActivity(
                Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS, Uri.parse("package:$packageName"))
            )
        } catch (e: ActivityNotFoundException) {
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        }
    }

    private fun pinWidget() {
        val manager = AppWidgetManager.getInstance(this)
        if (manager.isRequestPinAppWidgetSupported) {
            manager.requestPinAppWidget(ComponentName(this, RumahWidget::class.java), null, null)
        } else {
            Toast.makeText(
                this, "Tahan lama Home Screen → Widget → Rumahku", Toast.LENGTH_LONG,
            ).show()
        }
    }
}
