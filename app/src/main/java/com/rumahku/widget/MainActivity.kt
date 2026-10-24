package com.rumahku.widget

import android.app.Activity
import android.app.AlertDialog
import android.appwidget.AppWidgetManager
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.graphics.Typeface
import android.graphics.drawable.AnimationDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import kotlin.math.roundToInt

class MainActivity : Activity() {
    private var previewMood: Mood? = null
    private var previewPhase: DayPhase? = null
    private var previewWeather: Weather? = null

    private val moodChips = mutableListOf<Pair<Button, Mood?>>()
    private val phaseChips = mutableListOf<Pair<Button, DayPhase>>()
    private val weatherChips = mutableListOf<Pair<Button, Weather>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<View>(R.id.root).background = PixelUi.backdrop(this)
        PixelUi.setText(findViewById(R.id.header_title), "RUMAHKU", PixelUi.PINK, 7f, 0x5A1A6E)
        PixelUi.setText(findViewById(R.id.header_sub), "KEDAI RAMEN DI HP KAMU", PixelUi.CYAN, 2.5f)
        PixelUi.fillHeader(findViewById(R.id.header_mood), PixelUi.ICON_HEART, "MOOD HARI INI", PixelUi.YELLOW)
        PixelUi.fillHeader(findViewById(R.id.header_guests), PixelUi.ICON_PERSON, "TAMU KEDAI", PixelUi.PINK)
        PixelUi.fillHeader(findViewById(R.id.header_preview), PixelUi.ICON_EYE, "INTIP SUASANA", PixelUi.CYAN)
        PixelUi.fillHeader(findViewById(R.id.header_settings), PixelUi.ICON_STAR, "PENGATURAN", PixelUi.YELLOW)

        findViewById<Button>(R.id.btn_permission).setOnClickListener { openUsageSettings() }
        findViewById<Button>(R.id.btn_pin).setOnClickListener { pinWidget() }
        findViewById<Button>(R.id.btn_add_guest).setOnClickListener {
            startActivity(Intent(this, GuestEditorActivity::class.java))
        }

        val moodRow = findViewById<LinearLayout>(R.id.mood_row)
        val reset = PixelUi.chip(this, "✨ Asli") {
            previewMood = null; previewPhase = null; previewWeather = null; render()
        }
        moodRow.addView(reset)
        moodChips += reset to null
        Mood.entries.forEach { mood ->
            val chip = PixelUi.chip(this, mood.label) { previewMood = mood; render() }
            moodRow.addView(chip)
            moodChips += chip to mood
        }

        val phaseRow = findViewById<LinearLayout>(R.id.phase_row)
        DayPhase.entries.forEach { phase ->
            val chip = PixelUi.chip(this, phase.label) { previewPhase = phase; render() }
            phaseRow.addView(chip)
            phaseChips += chip to phase
        }

        val weatherRow = findViewById<LinearLayout>(R.id.weather_row)
        Weather.entries.forEach { weather ->
            val chip = PixelUi.chip(this, "${weather.emoji} ${weather.label}") { previewWeather = weather; render() }
            weatherRow.addView(chip)
            weatherChips += chip to weather
        }
    }

    override fun onResume() {
        super.onResume()
        render()
        renderGuests()
        WidgetUpdater.updateAll(this)
        UpdateScheduler.schedule(this)
    }

    private fun render() {
        val stats = PhoneUsage.read(this)
        val hour = PhoneUsage.currentHour()
        val mood = previewMood ?: MoodEngine.decide(stats, hour)
        val phase = previewPhase ?: DayPhase.of(hour)
        val weather = previewWeather ?: Weather.current()
        val guests = GuestStore.load(this)

        val anim = AnimationDrawable().apply {
            isOneShot = false
            SceneRenderer.renderFrames(SceneState(mood, phase, weather, guests)).forEach { frame ->
                addFrame(PixelUi.drawable(this@MainActivity, frame), SceneRenderer.FRAME_MS)
            }
            isFilterBitmap = false
        }
        findViewById<ImageView>(R.id.preview).setImageDrawable(anim)
        anim.start()

        findViewById<TextView>(R.id.preview_bubble).text = mood.bubble(phase, weather)
        findViewById<TextView>(R.id.mood_title).text = mood.title
        findViewById<TextView>(R.id.mood_line).text = mood.line
        renderStats(stats, weather)

        findViewById<Button>(R.id.btn_permission).visibility =
            if (stats.hasPermission) View.GONE else View.VISIBLE

        moodChips.forEach { (chip, m) -> chip.isSelected = m == previewMood }
        phaseChips.forEach { (chip, p) -> chip.isSelected = p == previewPhase }
        weatherChips.forEach { (chip, w) -> chip.isSelected = w == previewWeather }
    }

    private fun renderStats(stats: PhoneStats, weather: Weather) {
        val box = findViewById<LinearLayout>(R.id.stats_container)
        box.removeAllViews()
        if (!stats.hasPermission) {
            box.addView(PixelUi.label(this, "Izin screen time belum aktif, jadi mood belum bisa dibaca.", PixelUi.PINK))
        } else {
            val h = stats.screenMinutes / 60
            val m = stats.screenMinutes % 60
            box.addView(statRow("LAYAR", stats.screenMinutes / 360f, if (h > 0) "${h}j ${m}m" else "${m}m"))
            box.addView(statRow("BUKA", stats.unlocks / 80f, "${stats.unlocks}x"))
        }
        val next = "%02d:00".format(Weather.nextChangeHour())
        box.addView(statRow("CUACA", null, "${weather.emoji} ${weather.label} · ganti $next"))
    }

    private fun statRow(label: String, fill: Float?, value: String) = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT,
        ).apply { topMargin = dp(8) }

        val tag = PixelUi.image(context, PixelFont.bitmap(label, PixelUi.CYAN, 0x0E0A1C), 2.5f)
        addView(fixedWidth(tag, dp(64)))

        if (fill != null) {
            addView(segmentBar(fill.coerceIn(0f, 1f)))
            addView(PixelUi.label(context, value, PixelUi.CREAM, 13f).apply {
                typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
                layoutParams = LinearLayout.LayoutParams(dp(78), LinearLayout.LayoutParams.WRAP_CONTENT)
                gravity = Gravity.END
            })
        } else {
            addView(PixelUi.label(context, value, PixelUi.CREAM, 13f).apply {
                typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            })
        }
    }

    private fun fixedWidth(child: View, width: Int) = LinearLayout(this).apply {
        layoutParams = LinearLayout.LayoutParams(width, LinearLayout.LayoutParams.WRAP_CONTENT)
        gravity = Gravity.CENTER_VERTICAL
        addView(child)
    }

    private fun segmentBar(fill: Float) = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        layoutParams = LinearLayout.LayoutParams(0, dp(16), 1f)
        val count = 12
        val lit = (fill * count).roundToInt()
        repeat(count) { i ->
            val color = when {
                i >= lit -> 0xFF2E2448.toInt()
                i < 5 -> PixelUi.CYAN
                i < 9 -> PixelUi.PINK
                else -> 0xFFFF5A5A.toInt()
            }
            addView(View(context).apply {
                setBackgroundColor(color)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f).apply {
                    marginEnd = dp(2)
                }
            })
        }
    }

    private fun renderGuests() {
        val list = GuestStore.load(this)
        val box = findViewById<LinearLayout>(R.id.guest_list)
        box.removeAllViews()
        if (list.isEmpty()) {
            box.addView(LinearLayout(this).apply {
                background = getDrawable(R.drawable.pixel_panel)
                setPadding(dp(18), dp(16), dp(18), dp(16))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT,
                ).apply { topMargin = dp(10) }
                addView(PixelUi.label(context, "Kursinya masih kosong… 🍜\nTambah tamu pertamamu!", PixelUi.CREAM, 13f))
            })
        }
        list.forEachIndexed { i, guest -> box.addView(guestCard(guest, i)) }

        val add = findViewById<Button>(R.id.btn_add_guest)
        val full = list.size >= GuestStore.MAX
        add.isEnabled = !full
        add.text = if (full) "Kursi penuh (${list.size}/${GuestStore.MAX})" else "+ Tambah tamu (${list.size}/${GuestStore.MAX})"
    }

    private fun guestCard(guest: Guest, index: Int) = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        background = getDrawable(R.drawable.pixel_card)
        setPadding(dp(14), dp(12), dp(10), dp(12))
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT,
        ).apply { topMargin = dp(10) }
        setOnClickListener {
            startActivity(Intent(context, GuestEditorActivity::class.java).putExtra(GuestEditorActivity.EXTRA_ID, guest.id))
        }

        addView(PixelUi.image(context, GuestArt.portrait(guest), 3f))
        addView(LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginStart = dp(14)
            }
            addView(PixelUi.label(context, guest.name, PixelUi.CREAM, 17f).apply {
                typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            })
            addView(PixelUi.label(context, "${guest.activity.emoji} ${guest.activity.label}", PixelUi.CYAN, 12f))
            addView(PixelUi.label(context, "Kursi ${index + 1} · ketuk untuk edit", PixelUi.MUTED, 11f))
        })
        addView(PixelUi.chip(context, "✕") { confirmDelete(guest) })
    }

    private fun confirmDelete(guest: Guest) {
        AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle("Usir ${guest.name}?")
            .setMessage("${guest.name} bakal pulang dari kedai.")
            .setPositiveButton("Pulangkan") { _, _ ->
                GuestStore.delete(this, guest.id)
                renderGuests()
                render()
                WidgetUpdater.updateAll(this)
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun dp(value: Int) = PixelUi.dp(this, value.toFloat()).roundToInt()

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
