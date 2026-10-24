package com.rumahku.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

data class SceneState(val mood: Mood, val phase: DayPhase)

object SceneRenderer {
    const val SIZE = 96
    const val FRAMES = 16
    const val FRAME_MS = 125

    private const val STREET_Y = 82
    private val OPENING = intArrayOf(16, 34, 79, 63)

    fun renderFrames(state: SceneState): List<Bitmap> = List(FRAMES) { render(state, it) }

    fun upscale(frame: Bitmap, scale: Int): Bitmap =
        Bitmap.createScaledBitmap(frame, SIZE * scale, SIZE * scale, false)

    fun render(state: SceneState, frame: Int): Bitmap {
        val bmp = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888)
        val c = PixelCanvas(bmp)
        val mood = state.mood
        val open = mood != Mood.SLEEPING
        val dark = state.phase == DayPhase.NIGHT || state.phase == DayPhase.EVENING

        drawSky(c, state, frame)
        drawCity(c, dark, frame)
        drawFacade(c, dark)
        drawWires(c)
        drawSign(c, mood, frame)
        drawInterior(c, mood, frame)
        drawNoren(c, frame)
        drawAwning(c)
        drawLanterns(c, open, frame)
        drawCounterFront(c)
        drawStools(c)
        if (mood == Mood.HAPPY) drawCustomer(c, frame)
        if (mood != Mood.LONELY && mood != Mood.SLEEPING) drawCat(c, mood == Mood.HAPPY, frame)
        drawVendingMachine(c, frame)
        drawStreet(c, mood, dark, frame)
        drawRain(c, mood, frame)
        drawDrips(c, frame)
        drawEffects(c, mood, frame)
        return bmp
    }

    private fun wave(frame: Int, cycles: Int = 1, offset: Double = 0.0): Double =
        sin(2 * PI * (frame.toDouble() * cycles / FRAMES + offset))

    private fun sway(frame: Int, amount: Double, cycles: Int = 1, offset: Double = 0.0): Int =
        (wave(frame, cycles, offset) * amount).roundToInt()

    private fun shimmer(x: Int, y: Int, frame: Int) = y % 2 == 1 && (x / 3 + y + frame / 4) % 4 != 0

    private class Sky(val top: Int, val bottom: Int)

    private fun skyOf(state: SceneState): Sky {
        val base = when (state.phase) {
            DayPhase.MORNING -> Sky(0x6F7C92, 0xA8A6B4)
            DayPhase.DAY -> Sky(0x5E6B80, 0x8C97A8)
            DayPhase.EVENING -> Sky(0x2B2350, 0x6A4A6E)
            DayPhase.NIGHT -> Sky(0x0E1226, 0x2E2446)
        }
        if (state.mood != Mood.LONELY) return base
        return Sky(mix(base.top, 0x3A4050, 0.4f), mix(base.bottom, 0x5A6070, 0.4f))
    }

    private fun drawSky(c: PixelCanvas, state: SceneState, frame: Int) {
        val sky = skyOf(state)
        for (y in 0 until STREET_Y) {
            val band = (y * 6 / STREET_Y).coerceIn(0, 5)
            c.rect(0, y, SIZE, 1, mix(sky.top, sky.bottom, band / 5f))
        }
        val cloud = mix(sky.top, 0x000000, 0.25f)
        listOf(8 to 2, 30 to 1, 60 to 3, 86 to 1).forEachIndexed { i, (x, y) ->
            val dx = sway(frame, 2.0, offset = i * 0.3)
            c.disk(x + dx, y, 5, cloud)
            c.disk(x + dx + 5, y + 1, 4, cloud)
        }
    }

    private fun drawCity(c: PixelCanvas, dark: Boolean, frame: Int) {
        val wall = if (dark) 0x1E1A2E else 0x4A4A5C
        val lit = if (dark) 0xFFD98A else 0x9AA2B2
        val off = if (dark) 0x2E2A40 else 0x5E6272

        c.rect(0, 18, 10, 64, wall)
        for (y in 21..46 step 4) for (x in 2..8 step 3) {
            c.rect(x, y, 2, 2, if ((x * 3 + y * 5) % 7 < 4) lit else off)
        }
        c.rect(84, 12, 12, 70, mix(wall, 0x0A0A14, 0.2f))
        for (y in 16..70 step 4) for (x in 85..90 step 3) {
            c.rect(x, y, 2, 2, if ((x * 5 + y * 3) % 7 < 3) lit else off)
        }
        c.rect(91, 20, 4, 22, 0x14101C)
        for (i in 0 until 5) {
            val on = (i + frame / 3) % 5 != 0
            c.rect(92, 22 + i * 4, 2, 3, if (on) (if (i % 2 == 0) 0x5AD0FF else 0xFF6AC8) else 0x2A3040)
        }

        c.rect(86, 0, 3, 82, 0x5A5258)
        c.rect(87, 0, 1, 82, 0x6E666C)
        c.rect(82, 6, 11, 1, 0x4A4248)
        c.rect(84, 9, 5, 5, 0x7A7E86)
        c.rect(84, 9, 5, 1, 0x9A9EA6)
    }

    private fun drawFacade(c: PixelCanvas, dark: Boolean) {
        c.rect(9, 5, 78, 1, 0x2A2230)
        c.rect(10, 6, 76, 25, 0x4A3B48)
        for (y in 9..29 step 4) c.rect(10, y, 76, 1, 0x423441)

        c.rect(15, 8, 14, 10, 0x2A2230)
        c.rect(16, 9, 12, 8, if (dark) 0xFFD98A else 0xB8C0CC)
        c.rect(16, 9, 5, 8, if (dark) 0xE8A86A else 0x9AA2B2)
        c.rect(16, 12, 12, 1, 0x2A2230)

        c.rect(32, 10, 8, 6, 0xB8BCC4)
        for (x in 33..38 step 2) c.rect(x, 11, 1, 4, 0x8A8E96)

        c.rect(65, 8, 14, 10, 0x2A2230)
        c.rect(66, 9, 12, 8, 0x3A4660)
        for (y in 10..16 step 2) c.rect(66, y, 12, 1, 0x4A5878)

        c.rect(0, 78, SIZE, 4, if (dark) 0x34343F else 0x5A5A64)
        c.rect(0, 78, SIZE, 1, if (dark) 0x44444F else 0x6A6A74)
        c.rect(10, 34, 4, 48, 0x3A2418)
        c.rect(11, 34, 1, 48, 0x553826)
        c.rect(82, 34, 4, 48, 0x3A2418)
        c.rect(83, 34, 1, 48, 0x553826)
    }

    private fun drawWires(c: PixelCanvas) {
        for (x in 0..86) {
            val sag = x * (86 - x) / 900
            c.px(x, 6 + (86 - x) * 8 / 86 + sag, 0x141420)
            c.px(x, 9 + (86 - x) * 6 / 86 + sag, 0x141420)
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

    private val CUSTOMER = listOf(
        "..hhhhh..",
        ".hhhhhhh.",
        ".hhhhhhh.",
        ".hhhhhhh.",
        "..hhhhh..",
        "...sss...",
        ".JJJJJJJ.",
        "JJJJJJJJJ",
        "JJJJJJJJJ",
        "JJJjJjJJJ",
        "JJJJJJJJJ",
        ".JJJJJJJ.",
    )

    private fun drawCustomer(c: PixelCanvas, frame: Int) {
        val slurp = if (frame in 4..7 || frame in 12..15) 1 else 0
        c.sprite(CUSTOMER.take(6), 18, 60 + slurp, mapOf('h' to 0x3A2A22, 's' to SKIN))
        c.sprite(CUSTOMER.drop(6), 18, 66, mapOf('J' to 0xF2C94C, 'j' to 0xD9A82A))
        for (i in 0..9) c.px(31 + i / 4, 80 - i, 0x8A8E96)
        c.rect(32, 66, 3, 7, argb(140, 0xE6F0FA))
        c.px(33, 65, argb(160, 0xE6F0FA))
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

    private fun drawCat(c: PixelCanvas, awakeMood: Boolean, frame: Int) {
        val x = 66
        val y = 65
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

    private fun drawStreet(c: PixelCanvas, mood: Mood, dark: Boolean, frame: Int) {
        val asphalt = if (dark) 0x24242E else 0x45454F
        c.rect(0, STREET_Y, SIZE, SIZE - STREET_Y, asphalt)
        c.rect(0, STREET_Y, SIZE, 1, 0x5A5A66)
        for (y in STREET_Y + 1 until SIZE) for (x in 0 until SIZE) {
            if ((x * 7 + y * 11) % 17 == 0) c.px(x, y, mix(asphalt, 0x000000, 0.25f))
        }

        val open = mood != Mood.SLEEPING
        for (y in 83..95) for (x in 0 until SIZE) {
            if (!shimmer(x, y, frame)) continue
            if (open && y <= 87 && x in 14..81) c.px(x, y, argb(45, 0xFFC870))
            if (open && x in 37..59 && y in 85..93) c.px(x, y, argb(70 - (y - 85) * 6, 0xFF4F7A))
            if (open && (x in 11..15 || x in 80..84) && y in 85..95) c.px(x, y, argb(100 - (y - 85) * 6, 0xFF6A4A))
            if (x in 1..9 && y in 83..91) c.px(x, y, argb(70, 0xBFE4FF))
        }

        c.disk(64, 90, 3) { _, _ -> mix(asphalt, 0x3A3A55, 0.6f) }
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

    private fun drawRain(c: PixelCanvas, mood: Mood, frame: Int) {
        val count = when (mood) {
            Mood.LONELY -> 85
            Mood.HAPPY -> 40
            else -> 55
        }
        val color = if (mood == Mood.LONELY) 0xB8C8E0 else 0xC8D6EA
        for (i in 0 until count) {
            val x0 = (i * 37 + i * i * 3) % SIZE
            val y0 = (i * 53) % SIZE
            val y = (y0 + frame * SIZE * 2 / FRAMES) % SIZE
            val x = Math.floorMod(x0 - frame * SIZE / FRAMES, SIZE)
            for (p in 0..3) {
                val px = x - p / 2
                val py = y + p
                if (py >= SIZE) continue
                if (px in 11..84 && py in 31..65) continue
                c.px(px, py, argb(if (p == 0) 70 else 120, color))
            }
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

    private fun argb(alpha: Int, rgb: Int) = (alpha.coerceIn(0, 255) shl 24) or (rgb and 0xFFFFFF)

    private fun mix(a: Int, b: Int, t: Float): Int {
        fun ch(shift: Int) = (((a shr shift) and 0xFF) * (1 - t) + ((b shr shift) and 0xFF) * t).toInt()
        return (ch(16) shl 16) or (ch(8) shl 8) or ch(0)
    }

    private class PixelCanvas(bmp: Bitmap) {
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
}
