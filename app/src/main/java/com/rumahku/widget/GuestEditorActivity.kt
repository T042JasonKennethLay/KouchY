package com.rumahku.widget

import android.app.Activity
import android.graphics.drawable.AnimationDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout

class GuestEditorActivity : Activity() {
    private lateinit var draft: Guest
    private var editing = false
    private val handler = Handler(Looper.getMainLooper())
    private val renderPreview = Runnable { renderScene() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_guest_editor)
        findViewById<View>(R.id.root).background = PixelUi.backdrop(this)

        val existing = intent.getStringExtra(EXTRA_ID)?.let { GuestStore.find(this, it) }
        editing = existing != null
        draft = existing ?: Guest(
            hairColor = Guest.HAIR_COLORS.random(),
            outfitColor = Guest.OUTFIT_COLORS.random(),
            hairStyle = HairStyle.entries.random(),
        )

        PixelUi.setText(findViewById(R.id.header_title), if (editing) "EDIT TAMU" else "TAMU BARU", PixelUi.PINK, 5f, 0x5A1A6E)

        val input = findViewById<EditText>(R.id.name_input)
        input.setText(draft.name)
        input.setOnFocusChangeListener { _, _ -> showName() }
        input.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                draft = draft.copy(name = s?.toString().orEmpty())
                findViewById<View>(R.id.error).visibility = View.GONE
                refresh(debounce = true)
            }
        })

        findViewById<PixelButton>(R.id.btn_save).apply {
            text = if (editing) "Simpan perubahan" else "Ajak duduk"
            setOnClickListener { saveGuest() }
        }
        findViewById<PixelButton>(R.id.btn_delete).apply {
            visibility = if (editing) View.VISIBLE else View.GONE
            setOnClickListener { confirmDelete() }
        }

        refresh(debounce = false)
    }

    override fun onDestroy() {
        handler.removeCallbacks(renderPreview)
        super.onDestroy()
    }

    private fun showName() {
        val display = findViewById<PixelTextView>(R.id.name_display)
        val focused = findViewById<EditText>(R.id.name_input).hasFocus()
        if (draft.name.isEmpty()) {
            display.text = if (focused) "_" else "Ketik nama"
            display.color = PixelUi.MUTED
        } else {
            display.text = draft.name + if (focused) "_" else ""
            display.color = PixelUi.CREAM
        }
    }

    private fun refresh(debounce: Boolean) {
        showName()
        PixelUi.setBitmap(findViewById(R.id.portrait), GuestArt.portrait(draft), 4f)
        val tag = draft.tag
        findViewById<PixelTextView>(R.id.tag_hint).text =
            if (tag.isEmpty()) "Papan nama: -" else "Papan nama: $tag"
        buildOptions()
        handler.removeCallbacks(renderPreview)
        if (debounce) handler.postDelayed(renderPreview, 250) else renderScene()
    }

    private fun buildOptions() {
        val styles = findViewById<LinearLayout>(R.id.hair_style_row)
        styles.removeAllViews()
        HairStyle.entries.forEach { style ->
            styles.addView(PixelUi.chip(this, style.label) { update(draft.copy(hairStyle = style)) }.apply {
                isSelected = style == draft.hairStyle
            })
        }

        val hair = findViewById<LinearLayout>(R.id.hair_color_row)
        hair.removeAllViews()
        Guest.HAIR_COLORS.forEach { color ->
            hair.addView(PixelUi.swatch(this, color, color == draft.hairColor) { update(draft.copy(hairColor = color)) })
        }

        val outfit = findViewById<LinearLayout>(R.id.outfit_color_row)
        outfit.removeAllViews()
        Guest.OUTFIT_COLORS.forEach { color ->
            outfit.addView(PixelUi.swatch(this, color, color == draft.outfitColor) { update(draft.copy(outfitColor = color)) })
        }

        val activities = findViewById<LinearLayout>(R.id.activity_row)
        activities.removeAllViews()
        GuestActivity.entries.forEach { activity ->
            activities.addView(PixelUi.chip(this, activity.label) { update(draft.copy(activity = activity)) }.apply {
                isSelected = activity == draft.activity
            })
        }
    }

    private fun update(next: Guest) {
        draft = next
        refresh(debounce = false)
    }

    private fun renderScene() {
        val others = GuestStore.load(this).toMutableList()
        val index = others.indexOfFirst { it.id == draft.id }.let { if (it >= 0) it else others.size }
        others.removeAll { it.id == draft.id }
        others.add(index.coerceAtMost(others.size), draft)

        val hour = PhoneUsage.currentHour()
        val state = SceneState(Mood.HAPPY, DayPhase.of(hour), Weather.current(), others.take(GuestStore.MAX))
        val anim = AnimationDrawable().apply {
            isOneShot = false
            SceneRenderer.renderFrames(state).forEach { addFrame(PixelUi.drawable(this@GuestEditorActivity, it), SceneRenderer.FRAME_MS) }
            isFilterBitmap = false
        }
        findViewById<ImageView>(R.id.preview).setImageDrawable(anim)
        anim.start()
    }

    private fun saveGuest() {
        if (draft.name.isBlank()) {
            findViewById<View>(R.id.error).visibility = View.VISIBLE
            return
        }
        GuestStore.upsert(this, draft.copy(name = draft.name.trim()))
        WidgetUpdater.updateAll(this)
        finish()
    }

    private fun confirmDelete() {
        PixelDialog.show(
            this,
            title = "Pulangkan ${draft.name}?",
            message = "${draft.name} bakal pulang dari kedai.",
            confirm = "Pulangkan",
            cancel = "Batal",
        ) {
            GuestStore.delete(this, draft.id)
            WidgetUpdater.updateAll(this)
            finish()
        }
    }

    companion object {
        const val EXTRA_ID = "guest_id"
    }
}
