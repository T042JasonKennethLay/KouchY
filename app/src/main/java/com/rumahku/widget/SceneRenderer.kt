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

    private const val GROUND_Y = 84

    fun renderFrames(state: SceneState): List<Bitmap> = List(FRAMES) { render(state, it) }

    fun upscale(frame: Bitmap, scale: Int): Bitmap =
        Bitmap.createScaledBitmap(frame, SIZE * scale, SIZE * scale, false)

    fun render(state: SceneState, frame: Int): Bitmap {
        val bmp = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888)
        val c = PixelCanvas(bmp)
        val sky = skyOf(state)
        val shade = exteriorShade(state.phase)
        val evening = state.phase == DayPhase.EVENING || state.phase == DayPhase.NIGHT
        val lonely = state.mood == Mood.LONELY

        drawSky(c, state, sky, frame)
        if (lonely) drawRain(c, frame)
        drawGround(c, shade)
        drawHouse(c, shade)
        drawInterior(c, state, sky, frame)
        drawOutsideLantern(c, shade, lit = state.mood.lampOn && evening, frame)
        drawSakura(c, shade)
        if (!lonely) drawPetals(c, shade, frame)
        drawEffects(c, state.mood, frame)
        return bmp
    }

    private fun wave(frame: Int, cycles: Int = 1, offset: Double = 0.0): Double =
        sin(2 * PI * (frame.toDouble() * cycles / FRAMES + offset))

    private fun sway(frame: Int, amount: Double, cycles: Int = 1, offset: Double = 0.0): Int =
        (wave(frame, cycles, offset) * amount).roundToInt()

    private class Sky(val top: Int, val bottom: Int)

    private fun skyOf(state: SceneState): Sky {
        val base = when (state.phase) {
            DayPhase.MORNING -> Sky(0x9CCBEF, 0xFCE1E4)
            DayPhase.DAY -> Sky(0x7CC3F0, 0xD6EEFA)
            DayPhase.EVENING -> Sky(0x6B5BA8, 0xF7A880)
            DayPhase.NIGHT -> Sky(0x141A3A, 0x3A3570)
        }
        if (state.mood != Mood.LONELY) return base
        return Sky(mix(base.top, 0x6E7A8C, 0.5f), mix(base.bottom, 0x9AA3B0, 0.5f))
    }

    private fun exteriorShade(phase: DayPhase): (Int) -> Int = when (phase) {
        DayPhase.NIGHT -> { col -> mix(col, 0x1A1F40, 0.5f) }
        DayPhase.EVENING -> { col -> mix(col, 0x6A3A6A, 0.18f) }
        else -> { col -> col }
    }

    private fun skyAt(sky: Sky, y: Int): Int {
        val band = (y * 8 / GROUND_Y).coerceIn(0, 7)
        return mix(sky.top, sky.bottom, band / 7f)
    }

    private val STARS = listOf(6 to 4, 20 to 10, 34 to 3, 58 to 11, 4 to 22, 9 to 15, 26 to 6, 41 to 9)

    private fun drawSky(c: PixelCanvas, state: SceneState, sky: Sky, frame: Int) {
        for (y in 0 until GROUND_Y) c.rect(0, y, SIZE, 1, skyAt(sky, y))

        if (state.mood == Mood.LONELY) {
            listOf(Triple(12, 8, 6), Triple(24, 5, 5), Triple(40, 9, 6), Triple(56, 6, 5)).forEachIndexed { i, (x, y, r) ->
                val dx = sway(frame, 1.5, offset = i * 0.25)
                c.disk(x + dx, y, r, mix(sky.top, 0x5A6272, 0.4f))
                c.disk(x + dx, y - 1, r - 1, mix(sky.top, 0x8A93A3, 0.5f))
            }
            return
        }
        when (state.phase) {
            DayPhase.NIGHT -> {
                STARS.forEachIndexed { i, (x, y) ->
                    val dim = (frame / 2 + i * 3) % 8 == 0
                    c.px(x, y, if (dim) 0x7A7FB0 else 0xFFF6D5)
                }
                c.disk(48, 7, 4, 0xF6EFCF)
                c.disk(50, 6, 4, skyAt(sky, 6))
            }
            DayPhase.EVENING -> {
                c.disk(48, 11, 6, 0xFFB070)
                c.disk(48, 11, 4, 0xFFD08A)
            }
            else -> {
                c.disk(48, 7, 4, 0xFFE27A)
                c.disk(47, 6, 2, 0xFFF3B8)
                cloud(c, 14 + sway(frame, 2.0), 10)
                cloud(c, 30 - sway(frame, 2.0), 4)
            }
        }
    }

    private fun cloud(c: PixelCanvas, x: Int, y: Int) {
        c.rect(x, y, 9, 2, 0xFFFFFF)
        c.rect(x + 2, y - 1, 4, 1, 0xFFFFFF)
        c.rect(x + 1, y + 2, 7, 1, 0xE6F1FA)
    }

    private fun drawRain(c: PixelCanvas, frame: Int) {
        for (i in 0 until 34) {
            val x = (i * 29) % SIZE
            val y = (i * 17 + frame * 2 * GROUND_Y / FRAMES) % GROUND_Y
            c.rect(x, y, 1, 3, argb(150, 0xC8D6EA))
        }
    }

    private fun drawGround(c: PixelCanvas, shade: (Int) -> Int) {
        c.rect(0, GROUND_Y, SIZE, SIZE - GROUND_Y, shade(0x7DAE5A))
        c.rect(0, GROUND_Y, SIZE, 1, shade(0x95C46E))
        for (y in GROUND_Y + 1 until SIZE) for (x in 0 until SIZE) {
            if ((x * 5 + y * 7) % 13 == 0) c.px(x, y, shade(0x679A4A))
        }
        stone(c, shade, 40, 87, 9)
        stone(c, shade, 54, 91, 8)
        stone(c, shade, 30, 92, 7)
        listOf(6 to 0xF7B6C8, 20 to 0xFFFFFF, 70 to 0xF7B6C8, 80 to 0xFFE27A, 14 to 0xFFE27A).forEach { (x, col) ->
            c.px(x, 89, shade(col)); c.px(x, 90, shade(0x4F8A3F))
        }
        c.disk(4, 81, 5, shade(0x5E9A4A))
        c.disk(3, 79, 3, shade(0x76B35E))
    }

    private fun stone(c: PixelCanvas, shade: (Int) -> Int, x: Int, y: Int, w: Int) {
        c.rect(x + 1, y, w - 2, 1, shade(0xC4C4C4))
        c.rect(x, y + 1, w, 1, shade(0xA8A8A8))
        c.rect(x + 1, y + 2, w - 2, 1, shade(0x8A8A8A))
    }

    private fun drawHouse(c: PixelCanvas, shade: (Int) -> Int) {
        for (r in 0..12) {
            val y = 16 + r
            val left = 28 - r * 20 / 12
            val right = 67 + r * 20 / 12
            for (x in left..right) {
                val col = when {
                    r % 3 == 2 -> 0x3E4E6E
                    x % 3 == 0 -> 0x46587A
                    r % 3 == 0 -> 0x7085AA
                    else -> 0x5B6E92
                }
                c.px(x, y, shade(col))
            }
        }
        c.rect(30, 14, 36, 2, shade(0x34405A))
        c.rect(32, 13, 32, 1, shade(0x2B3348))
        c.rect(28, 12, 3, 4, shade(0x2B3348)); c.px(27, 11, shade(0x2B3348))
        c.rect(65, 12, 3, 4, shade(0x2B3348)); c.px(68, 11, shade(0x2B3348))
        c.rect(6, 29, 84, 2, shade(0x2B3348))
        c.px(5, 28, shade(0x2B3348)); c.px(4, 27, shade(0x2B3348))
        c.px(90, 28, shade(0x2B3348)); c.px(91, 27, shade(0x2B3348))
        c.rect(11, 31, 74, 2, shade(0x5A3A28))

        c.rect(11, 33, 4, 45, shade(0x4A2E1E))
        c.rect(81, 33, 4, 45, shade(0x4A2E1E))
        c.rect(12, 33, 1, 45, shade(0x6B4630))
        c.rect(82, 33, 1, 45, shade(0x6B4630))

        c.rect(8, 78, 80, 2, shade(0xA0714A))
        c.rect(8, 78, 80, 1, shade(0xB98658))
        for (x in 12..86 step 8) c.px(x, 79, shade(0x7A5236))
        c.rect(10, 80, 76, 4, shade(0x2E2420))
        listOf(16, 46, 76).forEach { c.rect(it, 80, 4, 4, shade(0x8C8C8C)) }
    }

    private fun drawOutsideLantern(c: PixelCanvas, shade: (Int) -> Int, lit: Boolean, frame: Int) {
        val cx = 7 + sway(frame, 1.0)
        c.rect(7, 31, 1, 3, shade(0x2B2B2B))
        if (lit) c.disk(cx, 39, 9, argb((40 * (1 + 0.15 * wave(frame, 2))).toInt(), 0xFFB070))
        c.rect(cx - 2, 34, 5, 1, shade(0x2B2B2B))
        val widths = intArrayOf(5, 7, 7, 7, 7, 7, 7, 5)
        widths.forEachIndexed { i, w ->
            val body = if (lit) 0xF2604A else 0xD9483B
            c.rect(cx - w / 2, 35 + i, w, 1, if (i % 2 == 1) body else mix(body, 0x7A2020, 0.3f))
        }
        c.px(cx, 37, 0xFFF1D0); c.px(cx - 1, 38, 0xFFF1D0); c.px(cx + 1, 38, 0xFFF1D0); c.px(cx, 39, 0xFFF1D0)
        c.rect(cx - 2, 43, 5, 1, shade(0x2B2B2B))
        c.px(cx + sway(frame, 1.0, offset = 0.15), 44, shade(0xFFC94A))
    }

    private fun drawSakura(c: PixelCanvas, shade: (Int) -> Int) {
        c.rect(88, 26, 4, 58, shade(0x6B4A4A))
        c.rect(89, 26, 1, 58, shade(0x866060))
        c.rect(91, 26, 1, 58, shade(0x523838))
        c.px(87, 83, shade(0x6B4A4A)); c.px(92, 83, shade(0x6B4A4A))
        for (i in 0..10) c.rect(87 - i, 22 - i / 2, 2, 2, shade(0x6B4A4A))

        listOf(
            Triple(95, 2, 7), Triple(66, 5, 5), Triple(80, 8, 9),
            Triple(91, 14, 8), Triple(72, 17, 6), Triple(86, 25, 6),
        ).forEach { (cx, cy, r) ->
            c.disk(cx, cy, r) { x, y ->
                val d = (x - cx) + (y - cy)
                val col = when {
                    (x * 7 + y * 13) % 29 == 0 -> 0xFFFFFF
                    d <= -r / 2 -> 0xFFD3E0
                    d >= r -> 0xE08BA8
                    else -> 0xF4B0C6
                }
                shade(col)
            }
        }
    }

    private val PETALS = listOf(60 to 20, 30 to 40, 10 to 60, 72 to 50, 45 to 70, 92 to 38, 20 to 12, 55 to 5)

    private fun drawPetals(c: PixelCanvas, shade: (Int) -> Int, frame: Int) {
        val fall = frame * (GROUND_Y / 2) / FRAMES
        PETALS.forEachIndexed { i, (x0, y0) ->
            for (twin in 0..1) {
                val y = (y0 + twin * GROUND_Y / 2 + fall) % GROUND_Y
                val x = Math.floorMod(x0 + sway(frame, 2.0, offset = i * 0.13 + twin * 0.5), SIZE)
                val col = shade(if ((i + twin) % 2 == 0) 0xFFD6E2 else 0xF4A6BE)
                val flat = (frame / 4 + i + twin) % 2 == 0
                c.px(x, y, col)
                c.px(if (flat) x + 1 else x, if (flat) y else y + 1, col)
            }
        }
    }

    private val INTERIOR = intArrayOf(15, 33, 80, 77)

    private fun drawInterior(c: PixelCanvas, state: SceneState, sky: Sky, frame: Int) {
        val mood = state.mood
        val sleeping = mood == Mood.SLEEPING

        c.rect(15, 33, 66, 37, 0xF5E6C8)
        c.rect(15, 33, 66, 7, 0xEAD6B0)
        c.rect(15, 40, 66, 1, 0x8A5A3B)
        c.rect(15, 41, 66, 1, 0xDCC49C)

        c.rect(15, 70, 66, 8, 0xC9CC86)
        for (y in 71..77 step 2) c.rect(15, y, 66, 1, 0xBDC07A)
        c.rect(15, 70, 66, 1, 0x5E6B3A)
        c.rect(37, 70, 1, 8, 0x5E6B3A)
        c.rect(59, 70, 1, 8, 0x5E6B3A)

        drawRoundWindow(c, state, sky, frame)
        drawScroll(c)
        drawLantern(c, mood.lampOn)
        drawCat(c, mood, frame)
        if (sleeping) drawFutonLaid(c, frame) else drawFutonStack(c)
        drawKotatsu(c)
        drawCharacter(c, mood, frame)
        drawTableTop(c, mood)

        if (mood.lampOn) {
            val flicker = 1 + 0.12 * wave(frame, 2)
            val boost = (if (state.phase == DayPhase.NIGHT) 1.5 else 1.0) * flicker
            c.disk(48, 41, 26, argb((18 * boost).toInt(), 0xFFD27A), INTERIOR)
            c.disk(48, 41, 16, argb((26 * boost).toInt(), 0xFFD27A), INTERIOR)
            c.disk(48, 41, 7, argb((40 * boost).toInt(), 0xFFE8A8), INTERIOR)
        } else {
            val alpha = when (state.phase) {
                DayPhase.NIGHT -> 150
                DayPhase.EVENING, DayPhase.MORNING -> 95
                DayPhase.DAY -> 60
            }
            c.rect(15, 33, 66, 45, argb(alpha, 0x141432))
            drawRoundWindow(c, state, sky, frame, glassOnly = true)
        }
    }

    private fun drawRoundWindow(c: PixelCanvas, state: SceneState, sky: Sky, frame: Int, glassOnly: Boolean = false) {
        val cx = 26
        val cy = 53
        for (dy in -9..9) for (dx in -9..9) {
            val d = dx * dx + dy * dy
            val x = cx + dx
            val y = cy + dy
            when {
                d <= 52 -> {
                    val bar = dx == -3 || dx == 3 || dy == 2
                    c.px(x, y, if (bar) 0x9A7A4A else skyAt(sky, (y - 44) * 8 + 4))
                }
                !glassOnly && d <= 88 -> c.px(x, y, if (dy < 0) 0x8A5A3B else 0x6B4226)
            }
        }
        if (state.mood == Mood.LONELY) {
            for (i in 0..2) c.rect(21 + i * 4, 47 + (i * 3 + frame * 18 / FRAMES) % 9, 1, 2, 0xB8CBE6)
        } else if (state.phase == DayPhase.NIGHT) {
            c.rect(28, 48, 2, 2, 0xF6EFCF)
            c.px(22, 50, if ((frame / 4) % 2 == 0) 0xFFF6D5 else 0x7A7FB0)
        } else {
            c.rect(19 + sway(frame, 1.0), 49, 3, 1, 0xFFFFFF)
        }
    }

    private fun drawScroll(c: PixelCanvas) {
        c.rect(71, 41, 1, 2, 0x5A3A28)
        c.rect(67, 43, 9, 1, 0x5A3A28)
        c.rect(68, 44, 7, 11, 0x7E9A6A)
        c.rect(69, 45, 5, 9, 0xF7F0DE)
        c.rect(70, 46, 3, 3, 0xD9483B)
        c.px(70, 46, 0xF7F0DE); c.px(72, 46, 0xF7F0DE)
        c.px(71, 50, 0x6E7F96)
        c.rect(70, 51, 3, 1, 0x6E7F96)
        c.rect(69, 52, 5, 1, 0x6E7F96)
        c.rect(67, 55, 9, 1, 0x5A3A28)
    }

    private fun drawLantern(c: PixelCanvas, on: Boolean) {
        val paper = if (on) 0xFFF1D0 else 0xD8CDB8
        val rib = if (on) 0xE8C88A else 0xB8AC98
        c.rect(48, 33, 1, 3, 0x3A2A2A)
        c.rect(46, 36, 5, 1, 0x2B2B2B)
        intArrayOf(7, 9, 9, 9, 9, 9, 9, 7).forEachIndexed { i, w ->
            c.rect(48 - w / 2, 37 + i, w, 1, if (i % 2 == 1) rib else paper)
        }
        c.px(48, 39, 0xD9483B); c.px(47, 40, 0xD9483B); c.px(49, 40, 0xD9483B); c.px(48, 41, 0xD9483B)
        c.px(48, 42, 0xD9483B)
        c.rect(46, 45, 5, 1, 0x2B2B2B)
        c.px(48, 46, 0xD9483B)
    }

    private fun drawKotatsu(c: PixelCanvas) {
        c.rect(34, 66, 29, 6, 0xD9604A)
        for (y in 66..71) for (x in 34..62) if ((x / 3 + y / 3) % 2 == 0) c.px(x, y, 0xE8805E)
        c.rect(34, 71, 29, 1, 0xB8463A)
        c.rect(33, 70, 1, 2, 0xB8463A)
        c.rect(63, 70, 1, 2, 0xB8463A)
    }

    private fun drawTableTop(c: PixelCanvas, mood: Mood) {
        c.rect(35, 64, 27, 1, 0xB5825A)
        c.rect(35, 65, 27, 1, 0x8A5A3B)
        if (mood != Mood.SLEEPING) {
            c.rect(43, 63, 2, 1, SKIN)
            c.rect(52, 63, 2, 1, SKIN)
        }
        c.rect(38, 61, 4, 3, 0x6E9A5E)
        c.rect(38, 61, 4, 1, 0x8FBA7A)
        c.rect(56, 62, 2, 2, 0xF29A2E); c.px(56, 62, 0xF7B45A)
        c.rect(59, 62, 2, 2, 0xF29A2E); c.px(59, 62, 0xF7B45A)
        c.px(57, 61, 0x4F9A4F)
    }

    private fun drawFutonStack(c: PixelCanvas) {
        c.rect(64, 66, 15, 4, 0xF4F1EA)
        c.rect(64, 69, 15, 1, 0xD8D2C4)
        c.rect(65, 63, 14, 3, 0xF2A7B8)
        for (x in 66..78 step 3) c.px(x, 64, 0xFFFFFF)
        c.rect(64, 60, 15, 3, 0x7E9BD0)
        c.rect(64, 60, 15, 1, 0x9DB4DE)
        c.rect(70, 57, 6, 3, 0xE8D9B5)
        c.rect(70, 59, 6, 1, 0xCDBB92)
    }

    private fun drawFutonLaid(c: PixelCanvas, frame: Int) {
        val breath = if (wave(frame) > 0.3) 1 else 0
        c.rect(63, 72, 18, 5, 0xF4F1EA)
        c.rect(63, 76, 18, 1, 0xD8D2C4)
        c.rect(70, 64, 10, 5, 0xE8D9B5)
        c.sprite(person(Eyes.CLOSED, Mouth.NEUTRAL, Cheeks.BLUSH).take(12), 66, 57, PALETTE)
        c.rect(63, 69 - breath, 16, 4 + breath, 0xF2A7B8)
        c.rect(63, 69 - breath, 16, 1, 0xF7C4D0)
        for (x in 65..77 step 3) c.px(x, 71, 0xFFFFFF)
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
        listOf(31 to 76, 32 to 75),
        listOf(31 to 76, 32 to 76),
        listOf(31 to 76, 32 to 75),
        listOf(31 to 75, 31 to 74),
    )

    private fun drawCat(c: PixelCanvas, mood: Mood, frame: Int) {
        val awake = mood == Mood.HAPPY && frame != 6
        val rows = if (awake) CAT.toMutableList().also { it[3] = CAT_AWAKE_EYES } else CAT
        val ear = if (frame in 9..10) 1 else 0
        c.sprite(rows.drop(ear), 22, 71 + ear, mapOf('K' to 0xF2A65A, 'E' to 0x3A2A2A, 'w' to 0xFFF6EA, 'P' to 0xF29BB5))
        for (x in 23..29 step 3) c.px(x, 73, 0xD9822B)
        c.px(30, 76, 0xF2A65A)
        CAT_TAIL[(frame / 4) % CAT_TAIL.size].forEach { (x, y) -> c.px(x, y, 0xF2A65A) }
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
        'R' to 0xE05A6A,
        'S' to SKIN,
        'E' to 0x2B1D2E,
        'w' to 0xFFFFFF,
        'M' to 0xC2505F,
        'B' to 0xF7A1A8,
        'D' to 0xC9A0C0,
        'T' to 0x4A6FB5,
        'V' to 0xF4F1EA,
        'O' to 0xE0703A,
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
            ".HHhHHHHHHHRRH.",
            "HHHHHHHHHHHHHHH",
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

    private fun drawCharacter(c: PixelCanvas, mood: Mood, frame: Int) {
        val blink = frame == FRAMES - 1
        val hop = frame in 2..5 || frame in 10..13
        val nod = frame in 4..11
        val sprite = when (mood) {
            Mood.SLEEPING -> return
            Mood.HAPPY -> person(
                when { hop -> Eyes.HAPPY; blink -> Eyes.CLOSED; else -> Eyes.OPEN },
                Mouth.SMILE, Cheeks.BLUSH,
            )
            Mood.LONELY -> person(if (blink) Eyes.CLOSED else Eyes.OPEN, Mouth.FROWN, Cheeks.PLAIN, sad = true)
            Mood.TIRED -> person(Eyes.TIRED, Mouth.FROWN, Cheeks.BAGS)
            Mood.SLEEPY -> person(Eyes.CLOSED, if (frame in 8..13) Mouth.YAWN else Mouth.NEUTRAL, Cheeks.BLUSH)
            Mood.NO_PERMISSION -> person(if (blink) Eyes.CLOSED else Eyes.OPEN, Mouth.SMILE, Cheeks.BLUSH)
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
                c.sprite(NOTE, 58 + sway(frame, 1.0, 2), 50 - frame / 2, mapOf('N' to argb(fade(frame), 0xFF8FB0)))
                val h = (frame + 8) % FRAMES
                c.sprite(HEART, 34 + sway(h, 1.0, 2), 52 - h / 2, mapOf('R' to argb(fade(h), 0xFF6F8F)))
                for (k in 0..1) {
                    val s = (frame + k * 8) % FRAMES
                    c.px(39 + sway(s, 1.0), 60 - s / 3, argb(200 - s * 12, 0xFFFFFF))
                }
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
                    val y = 44 + (2.5 * sin(a)).roundToInt()
                    c.sprite(STAR, x - 1, y - 1, mapOf('Y' to 0xFFE27A))
                }
                val drip = frame % 8
                c.rect(CHAR_X + 14, CHAR_Y + 4 + drip / 3, 1, 2, argb(255 - drip * 25, 0x7FC4FF))
            }
            Mood.SLEEPY -> if (frame < 12) {
                c.sprite(ZED, 58 + sway(frame, 1.0), 47 - frame / 3, mapOf('Z' to argb(fade(frame, 8), 0xE8E8FF)))
            }
            Mood.SLEEPING -> for (k in 0..1) {
                val s = (frame + k * 8) % FRAMES
                c.sprite(ZED, 62 - s / 4, 52 - s * 3 / 4, mapOf('Z' to argb(255 - s * 12, 0xE8E8FF)))
            }
            Mood.NO_PERMISSION -> c.sprite(QUESTION, 58, 44 + sway(frame, 1.0), mapOf('Q' to 0xFFE08A))
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
