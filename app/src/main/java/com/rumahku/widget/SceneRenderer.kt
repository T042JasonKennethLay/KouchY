package com.rumahku.widget

import android.graphics.Bitmap
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

data class SceneState(
    val mood: Mood,
    val phase: DayPhase,
    val weather: Weather,
    val guests: List<Guest> = emptyList(),
)

object SceneRenderer {
    const val SIZE = 96
    const val FRAMES = 16
    const val FRAME_MS = 125

    private const val STREET_Y = 82
    private val OPENING = intArrayOf(16, 34, 79, 63)

    private enum class Precip { NONE, DRIZZLE, RAIN, HEAVY_RAIN, SNOW, HEAVY_SNOW, LEAVES }
    private enum class Ground { WET, SNOW, DRY }

    fun renderFrames(state: SceneState): List<Bitmap> = List(FRAMES) { render(state, it) }

    fun upscale(frame: Bitmap, scale: Int): Bitmap =
        Bitmap.createScaledBitmap(frame, SIZE * scale, SIZE * scale, false)

    fun render(state: SceneState, frame: Int): Bitmap {
        val bmp = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888)
        val c = PixelCanvas(bmp)
        val mood = state.mood
        val weather = state.weather
        val lonely = mood == Mood.LONELY
        val open = mood != Mood.SLEEPING
        val dark = state.phase == DayPhase.NIGHT || state.phase == DayPhase.EVENING
        val precip = precipOf(weather, lonely)
        val ground = when (precip) {
            Precip.SNOW, Precip.HEAVY_SNOW -> Ground.SNOW
            Precip.DRIZZLE, Precip.RAIN, Precip.HEAVY_RAIN -> Ground.WET
            else -> Ground.DRY
        }

        drawSky(c, state)
        drawCelestial(c, state, frame)
        drawSkyline(c, state, dark, frame)
        drawSideProps(c, frame)
        drawFacade(c, dark, ground)
        drawWires(c)
        drawSign(c, mood, frame)
        drawInterior(c, mood, frame)
        drawNoren(c, frame)
        drawAwning(c)
        if (weather == Weather.SUMMER) drawFurin(c, frame)
        drawLanterns(c, open, frame)
        drawCounterFront(c)
        drawStools(c)
        val guestsHere = open && !lonely && state.guests.isNotEmpty()
        if (guestsHere) GuestArt.drawSeated(c, state.guests, frame)
        else if (mood == Mood.HAPPY) drawCustomer(c, weather, ground == Ground.WET, frame)
        if (!lonely && open) {
            val fullHouse = guestsHere && state.guests.size >= GuestArt.SEATS.size
            if (fullHouse) drawCat(c, mood == Mood.HAPPY, frame, 1, 43)
            else drawCat(c, mood == Mood.HAPPY, frame, 66, 65)
        }
        drawVendingMachine(c, frame)
        drawStreet(c, open, dark, ground, weather, frame)
        if (ground == Ground.SNOW) drawSnowCaps(c, dark)
        drawPrecipitation(c, precip, frame)
        if (ground == Ground.WET) drawDrips(c, frame)
        drawEffects(c, mood, frame)
        drawBubble(c, mood.bubble(state.phase, weather), frame)
        return bmp
    }

    private fun drawBubble(c: PixelCanvas, text: String, frame: Int) {
        val ink = 0x2A1E36
        val fill = 0xFFFDF7
        val w = PixelFont.width(text) + 7
        val h = PixelFont.HEIGHT + 6
        val x = 9
        val y = 3 + if (wave(frame) > 0.6) -1 else 0
        c.rect(x + 1, y, w - 2, h, ink)
        c.rect(x, y + 1, w, h - 2, ink)
        c.rect(x + 1, y + 1, w - 2, h - 2, fill)
        val tx = x + w - 9
        c.rect(tx + 1, y + h - 1, 3, 1, fill)
        c.px(tx + 1, y + h, ink); c.rect(tx + 2, y + h, 2, 1, fill); c.px(tx + 4, y + h, ink)
        c.px(tx + 2, y + h + 1, ink); c.px(tx + 3, y + h + 1, fill); c.px(tx + 4, y + h + 1, ink)
        c.px(tx + 3, y + h + 2, ink)
        PixelFont.forEachPixel(text) { px, py -> c.px(x + 4 + px, y + 3 + py, 0x3B2A3A) }
    }

    private fun precipOf(weather: Weather, lonely: Boolean) = when (weather) {
        Weather.RAIN -> if (lonely) Precip.HEAVY_RAIN else Precip.RAIN
        Weather.SNOW -> if (lonely) Precip.HEAVY_SNOW else Precip.SNOW
        Weather.AUTUMN -> if (lonely) Precip.DRIZZLE else Precip.LEAVES
        else -> if (lonely) Precip.DRIZZLE else Precip.NONE
    }

    private fun wave(frame: Int, cycles: Int = 1, offset: Double = 0.0): Double =
        sin(2 * PI * (frame.toDouble() * cycles / FRAMES + offset))

    private fun sway(frame: Int, amount: Double, cycles: Int = 1, offset: Double = 0.0): Int =
        (wave(frame, cycles, offset) * amount).roundToInt()

    private fun shimmer(x: Int, y: Int, frame: Int) = y % 2 == 1 && (x / 3 + y + frame / 4) % 4 != 0

    private fun underAwning(x: Int, y: Int) = x in 11..84 && y in 31..65

    private fun particles(seeds: Int, copies: Int, frame: Int, block: (id: Int, x: Int, y: Int) -> Unit) {
        val step = SIZE / copies
        val fall = frame * step / FRAMES
        for (i in 0 until seeds) {
            val x0 = (i * 37 + i * i * 7) % SIZE
            val y0 = (i * 53) % step
            for (k in 0 until copies) block(i * copies + k, x0, (y0 + k * step + fall) % SIZE)
        }
    }

    private class Sky(val top: Int, val bottom: Int)

    private fun skyOf(state: SceneState): Sky {
        val w = state.weather
        val base = when (state.phase) {
            DayPhase.NIGHT -> when (w) {
                Weather.CLEAR -> Sky(0x0B0820, 0x3A1A5E)
                Weather.SUMMER -> Sky(0x0A1030, 0x2A2A5A)
                Weather.RAIN -> Sky(0x0E1226, 0x2E2446)
                Weather.SNOW -> Sky(0x161A36, 0x3A3E66)
                Weather.AUTUMN -> Sky(0x1A1028, 0x4A2438)
            }
            DayPhase.EVENING -> when (w) {
                Weather.CLEAR -> Sky(0x3A2A6E, 0xF29A7A)
                Weather.SUMMER -> Sky(0x4A3A8E, 0xFFB070)
                Weather.RAIN -> Sky(0x2B2350, 0x6A4A6E)
                Weather.SNOW -> Sky(0x5A6088, 0xC8B4C8)
                Weather.AUTUMN -> Sky(0x5A2A4A, 0xF2884A)
            }
            DayPhase.DAY -> when (w) {
                Weather.CLEAR -> Sky(0x5AA8E8, 0xC8E6F7)
                Weather.SUMMER -> Sky(0x3E9AE8, 0xBDE8FF)
                Weather.RAIN -> Sky(0x5E6B80, 0x8C97A8)
                Weather.SNOW -> Sky(0xA8B4C8, 0xDDE3EC)
                Weather.AUTUMN -> Sky(0x8AB4D8, 0xF2D2A8)
            }
            DayPhase.MORNING -> when (w) {
                Weather.CLEAR -> Sky(0x8EC5E8, 0xFCE1D4)
                Weather.SUMMER -> Sky(0x7CC3F0, 0xFFE8C8)
                Weather.RAIN -> Sky(0x6F7C92, 0xA8A6B4)
                Weather.SNOW -> Sky(0xB8C4D8, 0xE8E8F0)
                Weather.AUTUMN -> Sky(0x9CB8D8, 0xF7D0A8)
            }
        }
        if (state.mood != Mood.LONELY) return base
        return Sky(mix(base.top, 0x3A4050, 0.4f), mix(base.bottom, 0x5A6070, 0.4f))
    }

    private fun drawSky(c: PixelCanvas, state: SceneState) {
        val sky = skyOf(state)
        for (y in 0 until STREET_Y) {
            val band = (y * 6 / STREET_Y).coerceIn(0, 5)
            c.rect(0, y, SIZE, 1, mix(sky.top, sky.bottom, band / 5f))
        }
    }

    private val STARS = listOf(4 to 2, 14 to 1, 27 to 3, 42 to 1, 58 to 2, 66 to 9, 80 to 3, 93 to 1, 52 to 7)

    private fun drawCelestial(c: PixelCanvas, state: SceneState, frame: Int) {
        val sky = skyOf(state)
        val overcast = state.mood == Mood.LONELY || state.weather == Weather.RAIN || state.weather == Weather.SNOW
        if (overcast) {
            val cloud = mix(sky.top, if (state.weather == Weather.SNOW) 0xC8CCD8 else 0x000000, 0.25f)
            listOf(8 to 2, 30 to 1, 60 to 3, 86 to 1).forEachIndexed { i, (x, y) ->
                val dx = sway(frame, 2.0, offset = i * 0.3)
                c.disk(x + dx, y, 5, cloud)
                c.disk(x + dx + 5, y + 1, 4, cloud)
            }
            return
        }
        when (state.phase) {
            DayPhase.NIGHT, DayPhase.EVENING -> {
                if (state.phase == DayPhase.NIGHT) {
                    STARS.forEachIndexed { i, (x, y) ->
                        c.px(x, y, if ((frame / 2 + i * 3) % 8 == 0) 0x7A7FB0 else 0xFFF6D5)
                    }
                    if (state.weather == Weather.AUTUMN) {
                        c.disk(72, 6, 5, 0xFFD08A)
                        c.disk(71, 5, 2, 0xFFE4B0)
                    } else {
                        c.disk(72, 6, 4, 0xF6EFCF)
                        c.disk(74, 5, 4, mix(sky.top, sky.bottom, 0f))
                    }
                } else {
                    c.disk(72, 10, 5, 0xFFB070)
                    c.disk(72, 10, 3, 0xFFD08A)
                }
                if (state.weather == Weather.SUMMER) drawFireworks(c, frame)
            }
            else -> {
                c.disk(72, 6, 4, 0xFFE27A)
                c.disk(71, 5, 2, 0xFFF3B8)
                val big = state.weather == Weather.SUMMER
                cloud(c, 18 + sway(frame, 2.0), 3, big)
                cloud(c, 48 - sway(frame, 2.0), 6, big)
            }
        }
    }

    private fun cloud(c: PixelCanvas, x: Int, y: Int, big: Boolean) {
        if (big) {
            c.disk(x + 3, y + 1, 3, 0xFFFFFF)
            c.disk(x + 7, y, 4, 0xFFFFFF)
            c.rect(x, y + 2, 12, 2, 0xFFFFFF)
            c.rect(x + 1, y + 4, 10, 1, 0xE6F1FA)
        } else {
            c.rect(x, y, 9, 2, 0xFFFFFF)
            c.rect(x + 2, y - 1, 4, 1, 0xFFFFFF)
            c.rect(x + 1, y + 2, 7, 1, 0xE6F1FA)
        }
    }

    private fun drawFireworks(c: PixelCanvas, frame: Int) {
        listOf(Triple(64, 6, 0), Triple(34, 5, 8)).forEach { (cx, cy, offset) ->
            val t = (frame + offset) % FRAMES
            val first = if (offset == 0) 0xFF6AC8 else 0x6AE0FF
            val second = if (offset == 0) 0xFFE27A else 0xFFFFFF
            if (t < 4) {
                c.rect(cx, cy + 12 - t * 3, 1, 2, argb(220, 0xFFE8A8))
                return@forEach
            }
            val r = t - 4
            val radius = minOf(r + 1, 7)
            val alpha = if (r < 6) 255 else 255 - (r - 6) * 40
            for (k in 0 until 12) {
                val a = 2 * PI * k / 12
                c.px(
                    cx + (radius * cos(a)).roundToInt(),
                    cy + (radius * 0.9 * sin(a)).roundToInt(),
                    argb(alpha, if (k % 2 == 0) first else second),
                )
            }
            if (r < 3) c.disk(cx, cy, 1, argb(alpha, 0xFFFFFF))
        }
    }

    private val SKYLINE = listOf(
        Triple(0, 10, 8), Triple(9, 7, 3), Triple(16, 6, 10), Triple(22, 9, 1), Triple(31, 6, 7),
        Triple(37, 8, 12), Triple(45, 10, 4), Triple(55, 7, 9), Triple(62, 12, 13), Triple(74, 8, 12),
        Triple(82, 7, 6), Triple(89, 7, 2),
    )
    private val NEON = intArrayOf(0xB06AFF, 0xFF6AD5, 0x6AE0FF, 0xC77DFF)

    private fun drawSkyline(c: PixelCanvas, state: SceneState, dark: Boolean, frame: Int) {
        val clearSky = state.weather != Weather.RAIN && state.weather != Weather.SNOW
        val strength = when {
            !dark -> 0f
            clearSky && state.mood != Mood.LONELY -> 1f
            else -> 0.55f
        }
        if (dark && strength == 1f) {
            c.disk(48, 30, 46, argb(26, 0x9A4AFF))
            c.disk(48, 30, 30, argb(22, 0xC77DFF))
        }
        val snow = state.weather == Weather.SNOW
        SKYLINE.forEachIndexed { i, (x, w, top) ->
            val body = if (dark) mix(0x1A1430, 0x2A1E48, (i % 3) / 2f) else mix(0x8A90A8, 0xA0A6BC, (i % 3) / 2f)
            c.rect(x, top, w, STREET_Y - top, body)
            for (wy in top + 2 until STREET_Y step 3) for (wx in x + 1 until x + w - 1 step 2) {
                val h = (wx * 7 + wy * 13 + i * 5) % 11
                if (dark) {
                    val on = h < 4 || (h == 4 && (frame / 4 + wx + wy) % 3 == 0)
                    if (on) c.px(wx, wy, mix(NEON[(i + h) % 4], body, 1 - strength * 0.85f))
                } else if (h < 3) {
                    c.px(wx, wy, 0xC8D4E4)
                }
            }
            if (dark && i % 2 == 0) {
                c.rect(x, top, w, 1, mix(NEON[i % 4], body, 1 - strength))
                if (top > 0) c.rect(x, top - 1, w, 1, argb((70 * strength).toInt(), NEON[i % 4]))
            }
            if (dark && i % 3 == 1) {
                val sx = x + w - 2
                for (y in top + 2..minOf(top + 16, STREET_Y - 1)) {
                    val bright = (y + frame) % 6 == 0
                    c.px(sx, y, mix(if (bright) 0xFFFFFF else NEON[(i + 1) % 4], body, 1 - strength))
                }
            }
            if (snow) c.rect(x, top, w, 1, if (dark) 0xC8D0F0 else 0xF4F8FF)
        }
        c.rect(50, 0, 1, 4, 0x3A3448)
        if (dark && (frame / 4) % 2 == 0) c.px(50, 0, 0xFF3A3A)
    }

    private fun drawSideProps(c: PixelCanvas, frame: Int) {
        c.rect(91, 20, 4, 22, 0x14101C)
        for (i in 0 until 5) {
            val on = (i + frame / 3) % 5 != 0
            c.rect(92, 22 + i * 4, 2, 3, if (on) (if (i % 2 == 0) 0x5AD0FF else 0xFF6AC8) else 0x2A3040)
        }
        c.rect(86, 0, 3, 82, 0x5A5258)
        c.rect(87, 0, 1, 82, 0x6E666C)
        c.rect(82, 4, 11, 1, 0x4A4248)
        c.rect(84, 7, 5, 5, 0x7A7E86)
        c.rect(84, 7, 5, 1, 0x9A9EA6)
    }

    private fun drawFacade(c: PixelCanvas, dark: Boolean, ground: Ground) {
        c.rect(9, 15, 78, 1, 0x2A2230)
        c.rect(10, 16, 76, 15, 0x4A3B48)
        for (y in 19..29 step 4) c.rect(10, y, 76, 1, 0x423441)

        c.rect(14, 18, 13, 10, 0x2A2230)
        c.rect(15, 19, 11, 8, if (dark) 0xFFD98A else 0xB8C0CC)
        c.rect(15, 19, 4, 8, if (dark) 0xE8A86A else 0x9AA2B2)
        c.rect(15, 22, 11, 1, 0x2A2230)

        c.rect(70, 20, 9, 6, 0xB8BCC4)
        for (x in 71..77 step 2) c.rect(x, 21, 1, 4, 0x8A8E96)

        val walk = when (ground) {
            Ground.SNOW -> if (dark) 0x8A92B4 else 0xE4EAF2
            else -> if (dark) 0x34343F else 0x5A5A64
        }
        c.rect(0, 78, SIZE, 4, walk)
        c.rect(0, 78, SIZE, 1, mix(walk, 0xFFFFFF, 0.1f))
        c.rect(10, 34, 4, 48, 0x3A2418)
        c.rect(11, 34, 1, 48, 0x553826)
        c.rect(82, 34, 4, 48, 0x3A2418)
        c.rect(83, 34, 1, 48, 0x553826)
    }

    private fun drawWires(c: PixelCanvas) {
        for (x in 0..86) {
            val sag = x * (86 - x) / 900
            c.px(x, 4 + (86 - x) * 6 / 86 + sag, 0x141420)
            c.px(x, 7 + (86 - x) * 4 / 86 + sag, 0x141420)
        }
    }

    private val GLYPHS = listOf(
        listOf(".###.", ".....", "#####", "....#", "...#.", "..#..", "##..."),
        listOf(".....", ".....", ".....", "#####", ".....", ".....", "....."),
        listOf("....#", "#...#", ".#.#.", "..#..", ".#.#.", "#....", "....."),
        listOf("#....", ".#...", "....#", "...#.", "..#..", "##...", "....."),
    )

    private fun drawSign(c: PixelCanvas, mood: Mood, frame: Int) {
        val lit = mood != Mood.SLEEPING
        c.rect(30, 18, 37, 13, 0x2A1A14)
        c.rect(31, 19, 35, 11, 0x1A1016)
        if (lit) c.disk(48, 24, 22, argb((22 * (1 + 0.2 * wave(frame, 2))).toInt(), 0xFF4F7A))
        GLYPHS.forEachIndexed { g, rows ->
            val broken = mood == Mood.LONELY && g == 2 && frame in setOf(5, 6, 11)
            val on = lit && !broken
            val gx = 37 + g * 6
            rows.forEachIndexed { dy, row ->
                row.forEachIndexed { dx, ch ->
                    if (ch != '#') return@forEachIndexed
                    val x = gx + dx
                    val y = 21 + dy
                    if (on) {
                        c.px(x - 1, y, argb(60, 0xFF4F7A)); c.px(x + 1, y, argb(60, 0xFF4F7A))
                        c.px(x, y - 1, argb(60, 0xFF4F7A)); c.px(x, y + 1, argb(60, 0xFF4F7A))
                    }
                    c.px(x, y, if (on) 0xFFC2D2 else 0x5A3A44)
                }
            }
        }
    }

    private fun drawAwning(c: PixelCanvas) {
        c.rect(11, 31, 74, 2, 0x3E4A5E)
        for (x in 11..84 step 3) c.rect(x, 31, 1, 2, 0x2E3848)
        c.rect(10, 33, 76, 1, 0x1E2430)
    }

    private fun drawInterior(c: PixelCanvas, mood: Mood, frame: Int) {
        c.rect(16, 34, 64, 30, 0xE8C48A)
        c.rect(16, 34, 64, 9, 0xD9AE72)

        listOf(17, 21, 25, 29, 33, 59, 63, 67, 71, 75).forEachIndexed { i, x ->
            c.rect(x, 44, 3, 8, 0xFFF6E6)
            c.px(x + 1, 44, if (i % 3 == 0) 0xD9483B else 0x2A2A2A)
            for (y in 46..50 step 2) c.px(x + 1, y, 0x3A3A3A)
        }

        c.rect(18, 54, 14, 1, 0xD8DCE4)
        c.rect(19, 55, 12, 9, 0xB8BCC4)
        c.rect(19, 55, 2, 9, 0xD0D4DC)
        c.rect(29, 55, 2, 9, 0x9094A0)
        c.px(17, 57, 0x8A8E96); c.px(32, 57, 0x8A8E96)

        c.rect(58, 57, 21, 1, 0x8A5A3B)
        listOf(60 to 0x4F8A5A, 63 to 0x7A4A2A, 66 to 0xF4F1EA, 70 to 0xA8322A, 73 to 0x4F8A5A, 76 to 0xF2C94C)
            .forEach { (x, col) -> c.rect(x, 53, 2, 4, col); c.px(x, 52, col) }

        drawOwner(c, mood, frame)

        c.rect(12, 64, 72, 1, 0xC89A60)
        c.rect(12, 65, 72, 1, 0xA87A48)
        if (mood != Mood.SLEEPING) {
            c.rect(43, 63, 2, 1, SKIN)
            c.rect(52, 63, 2, 1, SKIN)
        }
        if (mood != Mood.LONELY) drawBowl(c, 34)
        drawBowl(c, 57)
        c.rect(66, 60, 2, 4, 0x3A2A2A)
        c.px(66, 59, 0xD9483B)

        if (mood.lampOn) {
            val flicker = 1 + 0.12 * wave(frame, 2)
            c.disk(48, 38, 30, argb((20 * flicker).toInt(), 0xFFD27A), OPENING)
            c.disk(48, 38, 18, argb((28 * flicker).toInt(), 0xFFE8A8), OPENING)
        } else {
            val alpha = if (mood == Mood.SLEEPING) 170 else 110
            c.rect(16, 34, 64, 30, argb(alpha, 0x141432))
        }

        if (mood != Mood.SLEEPING) {
            for (k in 0..2) {
                val s = (frame + k * 5) % FRAMES
                c.rect(24 + sway(s, 1.5), 52 - s * 2 / 3, 2, 2, argb(150 - s * 9, 0xFFFFFF))
            }
        }
        if (mood != Mood.LONELY && mood != Mood.SLEEPING) {
            listOf(36, 59).forEachIndexed { b, x ->
                for (k in 0..1) {
                    val s = (frame + k * 8 + b * 4) % FRAMES
                    c.px(x + sway(s, 1.0), 60 - s / 3, argb(170 - s * 10, 0xFFFFFF))
                }
            }
        }
    }

    private fun drawBowl(c: PixelCanvas, x: Int) {
        c.rect(x, 61, 6, 1, 0xF2D27A)
        c.px(x + 1, 61, 0xFFF6E0)
        c.px(x + 4, 61, 0x6E9A5E)
        c.rect(x, 62, 6, 1, 0xC8402A)
        c.rect(x + 1, 63, 4, 1, 0xA8322A)
        c.px(x + 2, 62, 0xFFFFFF)
    }

    private fun drawNoren(c: PixelCanvas, frame: Int) {
        c.rect(16, 34, 64, 1, 0x5A3A28)
        listOf(18, 33, 48, 63).forEachIndexed { i, x ->
            val dx = sway(frame, 1.0, offset = i * 0.2)
            for (row in 0..8) {
                val shift = if (row >= 6) dx else 0
                c.rect(x + shift, 35 + row, 13, 1, if (row == 8) 0x1E2D5A else 0x2B3F7A)
            }
            if (i == 1 || i == 2) {
                c.rect(x + 5, 37, 3, 1, 0xF4F1EA)
                c.rect(x + 4, 38, 1, 2, 0xF4F1EA)
                c.rect(x + 8, 38, 1, 2, 0xF4F1EA)
                c.rect(x + 5, 40, 3, 1, 0xF4F1EA)
                c.px(x + 6, 38, 0xF4F1EA)
            } else {
                c.rect(x + 6, 37, 1, 4, 0xF4F1EA)
                c.rect(x + 4, 38, 5, 1, 0xF4F1EA)
            }
        }
    }

    private fun drawFurin(c: PixelCanvas, frame: Int) {
        val dx = sway(frame, 1.0, 2)
        c.rect(23, 34, 1, 2, 0x8A8E96)
        c.rect(21, 36, 5, 1, argb(220, 0xDFF4FF))
        c.rect(20, 37, 7, 3, argb(200, 0xBFE8FF))
        c.px(22, 38, 0xFF6A8C); c.px(24, 38, 0xFF6A8C)
        c.px(23 + dx / 2, 40, 0xD9483B)
        c.rect(23 + dx, 41, 1, 1, 0x8A8E96)
        c.rect(22 + dx, 42, 3, 4, 0xF4F1EA)
        c.rect(22 + dx, 42, 3, 1, 0xFF8FB0)
    }

    private fun drawLanterns(c: PixelCanvas, lit: Boolean, frame: Int) {
        listOf(13, 82).forEachIndexed { k, base ->
            val cx = base + sway(frame, 1.0, offset = k * 0.5)
            c.rect(base, 34, 1, 2, 0x2B2B2B)
            if (lit) c.disk(cx, 40, 9, argb((45 * (1 + 0.15 * wave(frame, 2, k * 0.3))).toInt(), 0xFF8A5A))
            c.rect(cx - 2, 36, 5, 1, 0x2B2B2B)
            intArrayOf(5, 7, 7, 7, 7, 7, 7, 5).forEachIndexed { i, w ->
                val body = if (lit) 0xF2604A else 0x8A3A30
                c.rect(cx - w / 2, 37 + i, w, 1, if (i % 2 == 1) body else mix(body, 0x7A2020, 0.3f))
            }
            c.px(cx, 39, 0xFFF1D0); c.px(cx - 1, 40, 0xFFF1D0); c.px(cx + 1, 40, 0xFFF1D0); c.px(cx, 41, 0xFFF1D0)
            c.rect(cx - 2, 45, 5, 1, 0x2B2B2B)
            c.px(cx, 46, 0xFFC94A)
        }
    }

    private fun drawCounterFront(c: PixelCanvas) {
        c.rect(12, 66, 72, 12, 0x6B4226)
        c.rect(12, 66, 72, 1, 0x4A2E1E)
        for (x in 14..82 step 6) c.rect(x, 67, 1, 10, 0x5A3620)
        c.rect(12, 77, 72, 1, 0x3A2418)
    }

    private fun drawStools(c: PixelCanvas) {
        listOf(22, 46, 70).forEach { x ->
            c.rect(x, 74, 1, 6, 0xA8ACB4)
            c.rect(x - 2, 80, 5, 1, 0x6E7280)
            c.rect(x - 3, 72, 7, 2, 0xD9483B)
            c.rect(x - 3, 72, 7, 1, 0xF2604A)
        }
    }

    private val CUSTOMER_HEAD = listOf("..hhhhh..", ".hhhhhhh.", ".hhhhhhh.", ".hhhhhhh.", "..hhhhh..", "...sss...")
    private val CUSTOMER_BODY = listOf(".JJJJJJJ.", "JJJJJJJJJ", "JJJJJJJJJ", "JJJjJjJJJ", "JJJJJJJJJ", ".JJJJJJJ.")

    private fun drawCustomer(c: PixelCanvas, weather: Weather, wet: Boolean, frame: Int) {
        val coat = when (weather) {
            Weather.RAIN -> 0xF2C94C
            Weather.SNOW -> 0xD9483B
            Weather.AUTUMN -> 0x9A6A44
            Weather.SUMMER -> 0x8FD0F0
            Weather.CLEAR -> 0x8A5AC8
        }
        val slurp = if (frame in 4..7 || frame in 12..15) 1 else 0
        c.sprite(CUSTOMER_HEAD, 18, 60 + slurp, mapOf('h' to 0x3A2A22, 's' to SKIN))
        if (weather == Weather.SNOW) c.rect(19, 60 + slurp, 7, 2, 0xF4F1EA)
        c.sprite(CUSTOMER_BODY, 18, 66, mapOf('J' to coat, 'j' to mix(coat, 0x000000, 0.2f)))
        if (weather == Weather.SNOW) c.rect(19, 66, 7, 1, 0xF4F1EA)
        if (wet) {
            for (i in 0..9) c.px(31 + i / 4, 80 - i, 0x8A8E96)
            c.rect(32, 66, 3, 7, argb(140, 0xE6F0FA))
            c.px(33, 65, argb(160, 0xE6F0FA))
        }
    }

    private val CAT = listOf(
        "K.......K",
        "KK.....KK",
        "KKKKKKKKK",
        "KEEKKKEEK",
        "KKKwPwKKK",
        "KKKKKKKKK",
        ".KKKKKKK.",
    )
    private const val CAT_AWAKE_EYES = "KEKKKKKEK"
    private val CAT_TAIL = listOf(
        listOf(9 to 5, 10 to 4),
        listOf(9 to 5, 10 to 5),
        listOf(9 to 5, 10 to 4),
        listOf(9 to 4, 9 to 3),
    )

    private fun drawCat(c: PixelCanvas, awakeMood: Boolean, frame: Int, x: Int, y: Int) {
        val awake = awakeMood && frame != 6
        val rows = if (awake) CAT.toMutableList().also { it[3] = CAT_AWAKE_EYES } else CAT
        val ear = if (frame in 9..10) 1 else 0
        c.sprite(rows.drop(ear), x, y + ear, mapOf('K' to 0xF2A65A, 'E' to 0x3A2A2A, 'w' to 0xFFF6EA, 'P' to 0xF29BB5))
        for (sx in x + 1..x + 7 step 3) c.px(sx, y + 2, 0xD9822B)
        c.px(x + 8, y + 5, 0xF2A65A)
        CAT_TAIL[(frame / 4) % CAT_TAIL.size].forEach { (dx, dy) -> c.px(x + dx, y + dy, 0xF2A65A) }
    }

    private val DRINKS = listOf(
        listOf(0xD9483B, 0x3E8FD9, 0xF2C94C),
        listOf(0x4FB06A, 0xF29A2E, 0xD9483B),
        listOf(0x3E8FD9, 0xF4F1EA, 0x7A4A2A),
    )

    private fun drawVendingMachine(c: PixelCanvas, frame: Int) {
        c.disk(6, 62, 13, argb(28, 0xBFE4FF))
        c.rect(0, 50, 11, 32, 0xD8DEE8)
        c.rect(10, 50, 1, 32, 0xB8BEC8)
        c.rect(0, 50, 11, 3, 0x3E6FB5)
        c.rect(1, 54, 8, 13, 0xEAF4FF)
        DRINKS.forEachIndexed { r, row ->
            row.forEachIndexed { i, col ->
                val x = 2 + i * 3
                val y = 55 + r * 4
                c.rect(x, y, 2, 3, col)
                c.px(x, y + 3, if ((frame / 2 + r + i) % 7 == 0) 0x2A4A2A else 0x6AFF8A)
            }
        }
        c.rect(1, 69, 8, 5, 0xC4CAD4)
        c.px(7, 70, 0x3A3A3A)
        c.rect(2, 76, 6, 3, 0x2A2A35)
    }

    private val LEAF_COLORS = intArrayOf(0xE85A2A, 0xF29A2E, 0xD9483B, 0xF2C94C)

    private fun drawStreet(c: PixelCanvas, open: Boolean, dark: Boolean, ground: Ground, weather: Weather, frame: Int) {
        val asphalt = when (ground) {
            Ground.SNOW -> if (dark) 0x5A6286 else 0xD8E0EC
            else -> if (dark) 0x24242E else 0x45454F
        }
        c.rect(0, STREET_Y, SIZE, SIZE - STREET_Y, asphalt)
        c.rect(0, STREET_Y, SIZE, 1, mix(asphalt, 0xFFFFFF, 0.15f))
        for (y in STREET_Y + 1 until SIZE) for (x in 0 until SIZE) {
            if ((x * 7 + y * 11) % 17 == 0) c.px(x, y, mix(asphalt, 0x000000, if (ground == Ground.SNOW) 0.08f else 0.25f))
        }

        when (ground) {
            Ground.WET -> drawWetStreet(c, open, frame)
            Ground.SNOW -> {
                val track = mix(asphalt, 0x000000, 0.18f)
                c.rect(0, 88, SIZE, 1, track); c.rect(0, 92, SIZE, 1, track)
                for (x in 6 until SIZE step 9) {
                    c.px(x, 85 + (x / 9) % 2, track); c.px(x + 2, 86 - (x / 9) % 2, track)
                }
                if (open) for (y in 83..86) c.rect(14, y, 68, 1, argb(40 - (y - 83) * 8, 0xFFC870))
            }
            Ground.DRY -> {
                val line = if (dark) 0x6A6A74 else 0xC8C8CC
                for (x in 2 until SIZE step 12) c.rect(x, 89, 6, 1, line)
                if (open) for (y in 83..85) c.rect(14, y, 68, 1, argb(30 - (y - 83) * 8, 0xFFC870))
            }
        }

        if (weather == Weather.AUTUMN) {
            listOf(18 to 85, 57 to 93, 80 to 87, 36 to 80, 70 to 80).forEachIndexed { p, (cx, cy) ->
                for (k in 0 until 9) {
                    val x = cx + (k * 5 + p) % 7 - 3
                    val y = cy + (k * 3) % 3
                    c.px(x, y, LEAF_COLORS[(k + p) % 4])
                }
            }
        }
    }

    private fun drawWetStreet(c: PixelCanvas, open: Boolean, frame: Int) {
        for (y in 83..95) for (x in 0 until SIZE) {
            if (!shimmer(x, y, frame)) continue
            if (open && y <= 87 && x in 14..81) c.px(x, y, argb(45, 0xFFC870))
            if (open && x in 37..59 && y in 85..93) c.px(x, y, argb(70 - (y - 85) * 6, 0xFF4F7A))
            if (open && (x in 11..15 || x in 80..84) && y in 85..95) c.px(x, y, argb(100 - (y - 85) * 6, 0xFF6A4A))
            if (x in 1..9 && y in 83..91) c.px(x, y, argb(70, 0xBFE4FF))
        }

        c.disk(64, 90, 3) { _, _ -> 0x2E2E48 }
        for (k in 0..1) {
            val r = (frame + k * 8) % FRAMES / 2
            val alpha = 170 - r * 20
            for (deg in 0 until 360 step 20) {
                val a = deg * PI / 180
                c.px(64 + (r * 1.6 * cos(a)).roundToInt(), 90 + (r * 0.4 * sin(a)).roundToInt(), argb(alpha, 0xAAB8D0))
            }
        }

        for (i in 0 until 7) {
            val x = (i * 37 + frame * 13) % SIZE
            val y = 84 + (i * 5 + frame) % 11
            c.px(x, y, argb(200, 0xDDE6F2))
            if ((frame + i) % 2 == 0) {
                c.px(x - 1, y - 1, argb(150, 0xDDE6F2))
                c.px(x + 1, y - 1, argb(150, 0xDDE6F2))
            }
        }
    }

    private fun drawSnowCaps(c: PixelCanvas, dark: Boolean) {
        val snow = if (dark) 0xC8D0F0 else 0xF4F8FF
        c.rect(9, 14, 78, 1, snow)
        c.rect(30, 17, 37, 1, snow)
        c.rect(11, 30, 74, 1, snow)
        for (x in 13..83 step 7) c.px(x, 29, snow)
        c.rect(0, 49, 11, 1, snow)
        c.rect(82, 3, 11, 1, snow)
        c.rect(84, 6, 5, 1, snow)
        c.rect(14, 17, 13, 1, snow)
        c.rect(70, 19, 9, 1, snow)
        listOf(13, 82).forEach { c.rect(it - 2, 35, 5, 1, snow) }
        c.rect(43, 71, 7, 1, snow)
    }

    private fun drawPrecipitation(c: PixelCanvas, precip: Precip, frame: Int) {
        when (precip) {
            Precip.NONE -> Unit
            Precip.DRIZZLE -> drawRain(c, 25, 0xC8D6EA, frame)
            Precip.RAIN -> drawRain(c, 50, 0xC8D6EA, frame)
            Precip.HEAVY_RAIN -> drawRain(c, 85, 0xB8C8E0, frame)
            Precip.SNOW -> drawSnow(c, 16, frame)
            Precip.HEAVY_SNOW -> drawSnow(c, 28, frame)
            Precip.LEAVES -> drawLeaves(c, frame)
        }
    }

    private fun drawRain(c: PixelCanvas, count: Int, color: Int, frame: Int) {
        for (i in 0 until count) {
            val x0 = (i * 37 + i * i * 3) % SIZE
            val y0 = (i * 53) % SIZE
            val y = (y0 + frame * SIZE * 2 / FRAMES) % SIZE
            val x = Math.floorMod(x0 - frame * SIZE / FRAMES, SIZE)
            for (p in 0..3) {
                val px = x - p / 2
                val py = y + p
                if (py >= SIZE || underAwning(px, py)) continue
                c.px(px, py, argb(if (p == 0) 70 else 120, color))
            }
        }
    }

    private fun drawSnow(c: PixelCanvas, seeds: Int, frame: Int) {
        particles(seeds, 3, frame) { id, x0, y ->
            val x = Math.floorMod(x0 + sway(frame, 2.0, offset = id * 0.17), SIZE)
            if (underAwning(x, y)) return@particles
            if (id % 4 == 0) c.rect(x, y, 2, 2, argb(235, 0xFFFFFF)) else c.px(x, y, argb(220, 0xFFFFFF))
        }
    }

    private fun drawLeaves(c: PixelCanvas, frame: Int) {
        particles(9, 2, frame) { id, x0, y ->
            val x = Math.floorMod(x0 + sway(frame, 4.0, offset = id * 0.21), SIZE)
            if (underAwning(x, y)) return@particles
            val col = LEAF_COLORS[id % 4]
            c.px(x, y, col)
            if ((frame / 2 + id) % 2 == 0) c.px(x + 1, y, col) else c.px(x + 1, y + 1, col)
            c.px(x, y + 1, mix(col, 0x000000, 0.2f))
        }
    }

    private fun drawDrips(c: PixelCanvas, frame: Int) {
        listOf(10, 85).forEachIndexed { k, x ->
            val s = (frame + k * 8) % FRAMES
            c.rect(x, 34 + s * 3, 1, 2, argb(200, 0xDDE6F2))
        }
    }

    private enum class Eyes { OPEN, CLOSED, HAPPY, TIRED }
    private enum class Mouth { SMILE, NEUTRAL, FROWN, YAWN }
    private enum class Cheeks { PLAIN, BLUSH, BAGS }

    private const val CHAR_X = 41
    private const val CHAR_Y = 47
    private const val SKIN = 0xFBD8BE

    private val PALETTE = mapOf(
        'H' to 0x2E2230,
        'h' to 0x55446A,
        'W' to 0xF4F1EA,
        'S' to SKIN,
        'E' to 0x2B1D2E,
        'w' to 0xFFFFFF,
        'M' to 0xC2505F,
        'B' to 0xF7A1A8,
        'D' to 0xC9A0C0,
        'T' to 0x2E3A5C,
        'V' to 0xF4F1EA,
        'O' to 0xF4F1EA,
    )

    private fun person(eyes: Eyes, mouth: Mouth, cheeks: Cheeks, sad: Boolean = false): List<String> {
        val brows = if (sad) "HHSSSESSSESSSHH" else "HHSSSSSSSSSSSHH"
        val eyeRows = when (eyes) {
            Eyes.OPEN -> listOf("HHSSwESSSwESSHH", "HHSSEESSSEESSHH")
            Eyes.CLOSED -> listOf("HHSSSSSSSSSSSHH", "HHSSEESSSEESSHH")
            Eyes.HAPPY -> listOf("HHSSESSSSSESSHH", "HHSESESSSESESHH")
            Eyes.TIRED -> listOf("HHSEEESSSEEESHH", "HHSSEESSSEESSHH")
        }
        val cheekRow = when (cheeks) {
            Cheeks.PLAIN -> "HHSSSSSSSSSSSHH"
            Cheeks.BLUSH -> "HHSBBSSSSSBBSHH"
            Cheeks.BAGS -> "HHSSDDSSSDDSSHH"
        }
        val mouthRows = when (mouth) {
            Mouth.SMILE -> listOf(".HSSSSMSMSSSSH.", "..SSSSSMSSSSS..")
            Mouth.NEUTRAL -> listOf(".HSSSSSMSSSSSH.", "..SSSSSSSSSSS..")
            Mouth.FROWN -> listOf(".HSSSSSMSSSSSH.", "..SSSSMSMSSSS..")
            Mouth.YAWN -> listOf(".HSSSSMMMSSSSH.", "..SSSSMMMSSSS..")
        }
        return listOf(
            "....HHHHHHH....",
            "..HHHHHHHHHHH..",
            ".HHHhHHHHHHHHH.",
            ".WWWWWWWWWWWWWW",
            "HHHHHHHHHHHHHHHW",
            "HHHSHHSSSHHSHHH",
            brows,
        ) + eyeRows + cheekRow + mouthRows + listOf(
            "......SSS......",
            "...TTTVSVTTT...",
            "..TTTTTVTTTTT..",
            ".TTTTTTTTTTTTT.",
            ".TTOOOOOOOOOTT.",
        )
    }

    private fun drawOwner(c: PixelCanvas, mood: Mood, frame: Int) {
        if (mood == Mood.SLEEPING) {
            c.sprite(person(Eyes.CLOSED, Mouth.NEUTRAL, Cheeks.PLAIN).take(6), CHAR_X, 57 + sway(frame, 0.6), PALETTE)
            c.rect(CHAR_X - 1, 62, 17, 2, 0x2E3A5C)
            c.rect(CHAR_X + 2, 62, 3, 1, SKIN)
            c.rect(CHAR_X + 10, 62, 3, 1, SKIN)
            return
        }
        val blink = frame == FRAMES - 1
        val hop = frame in 2..5 || frame in 10..13
        val nod = frame in 4..11
        val sprite = when (mood) {
            Mood.HAPPY -> person(
                when { hop -> Eyes.HAPPY; blink -> Eyes.CLOSED; else -> Eyes.OPEN },
                Mouth.SMILE, Cheeks.BLUSH,
            )
            Mood.LONELY -> person(if (blink) Eyes.CLOSED else Eyes.OPEN, Mouth.FROWN, Cheeks.PLAIN, sad = true)
            Mood.TIRED -> person(Eyes.TIRED, Mouth.FROWN, Cheeks.BAGS)
            Mood.SLEEPY -> person(Eyes.CLOSED, if (frame in 8..13) Mouth.YAWN else Mouth.NEUTRAL, Cheeks.BLUSH)
            else -> person(if (blink) Eyes.CLOSED else Eyes.OPEN, Mouth.SMILE, Cheeks.BLUSH)
        }
        val dy = when (mood) {
            Mood.HAPPY -> if (hop) -1 else 0
            Mood.SLEEPY -> if (nod) 1 else 0
            else -> 0
        }
        c.sprite(sprite, CHAR_X, CHAR_Y + dy, PALETTE)
    }

    private val NOTE = listOf(".NN", ".N.", "NN.", "NN.")
    private val HEART = listOf("R.R", "RRR", ".R.")
    private val QUESTION = listOf("QQQ", "..Q", ".Q.", "...", ".Q.")
    private val ZED = listOf("ZZZZ", "..Z.", ".Z..", "ZZZZ")
    private val STAR = listOf(".Y.", "YYY", ".Y.")

    private fun fade(step: Int, start: Int = 10) = 255 - (step - start).coerceAtLeast(0) * 40

    private fun drawEffects(c: PixelCanvas, mood: Mood, frame: Int) {
        when (mood) {
            Mood.HAPPY -> {
                c.sprite(NOTE, 58 + sway(frame, 1.0, 2), 52 - frame / 2, mapOf('N' to argb(fade(frame), 0xFF8FB0)))
                val h = (frame + 8) % FRAMES
                c.sprite(HEART, 37 + sway(h, 1.0, 2), 54 - h / 2, mapOf('R' to argb(fade(h), 0xFF6F8F)))
            }
            Mood.LONELY -> {
                if (frame < 12) c.rect(CHAR_X + 4, CHAR_Y + 9 + frame / 3, 1, 2, 0x7FC4FF)
                val r = (frame + 8) % FRAMES
                if (r < 12) c.rect(CHAR_X + 9, CHAR_Y + 9 + r / 3, 1, 2, 0x7FC4FF)
            }
            Mood.TIRED -> {
                for (k in 0..1) {
                    val a = 2 * PI * frame / FRAMES + k * PI
                    val x = 48 + (9 * cos(a)).roundToInt()
                    val y = 45 + (2.5 * sin(a)).roundToInt()
                    c.sprite(STAR, x - 1, y - 1, mapOf('Y' to 0xFFE27A))
                }
                val drip = frame % 8
                c.rect(CHAR_X + 14, CHAR_Y + 6 + drip / 3, 1, 2, argb(255 - drip * 25, 0x7FC4FF))
            }
            Mood.SLEEPY -> if (frame < 12) {
                c.sprite(ZED, 58 + sway(frame, 1.0), 50 - frame / 3, mapOf('Z' to argb(fade(frame, 8), 0xE8E8FF)))
            }
            Mood.SLEEPING -> for (k in 0..1) {
                val s = (frame + k * 8) % FRAMES
                c.sprite(ZED, 57 + s / 4, 54 - s * 3 / 4, mapOf('Z' to argb(255 - s * 12, 0xE8E8FF)))
            }
            Mood.NO_PERMISSION -> c.sprite(QUESTION, 58, 46 + sway(frame, 1.0), mapOf('Q' to 0xFFE08A))
        }
    }

}
