package com.rumahku.widget

import android.graphics.Bitmap
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

object GuestArt {
    val SEATS = intArrayOf(22, 46, 70)

    private const val SKIN = 0xFBD8BE

    private val BODY = listOf(".JJJJJJJ.", "JJJJJJJJJ", "JJJJJJJJJ", "JJJjJjJJJ", "JJJJJJJJJ", ".JJJJJJJ.")
    private val LONG_BACK = listOf("..hhhhh..", "..hhhhh..", "...hhh...")

    private fun backHead(style: HairStyle) = when (style) {
        HairStyle.SHORT -> listOf("..hhhhh..", ".hHhhhhh.", ".hhhhhhh.", ".hhhhhhh.", "..hhhhh..", "...sss...")
        HairStyle.LONG -> listOf("..hhhhh..", ".hHhhhhh.", ".hhhhhhh.", ".hhhhhhh.", ".hhhhhhh.", ".hhhhhhh.")
        HairStyle.BUN -> listOf(
            "...hhh...", "...hHh...", "..hhhhh..", ".hHhhhhh.", ".hhhhhhh.", ".hhhhhhh.", "..hhhhh..", "...sss...",
        )
        HairStyle.CAP -> listOf("..ccccc..", ".cCCCCCc.", ".ccccccc.", ".hhhhhhh.", "..hhhhh..", "...sss...")
    }

    private fun palette(g: Guest) = mapOf(
        'h' to g.hairColor,
        'H' to mix(g.hairColor, 0xFFFFFF, 0.3f),
        's' to SKIN,
        'S' to SKIN,
        'C' to g.outfitColor,
        'c' to mix(g.outfitColor, 0x000000, 0.2f),
        'J' to g.outfitColor,
        'j' to mix(g.outfitColor, 0x000000, 0.2f),
        'T' to g.outfitColor,
        'V' to 0xF4F1EA,
        'E' to 0x2B1D2E,
        'w' to 0xFFFFFF,
        'M' to 0xC2505F,
        'B' to 0xF7A1A8,
    )

    private fun sway(frame: Int, amount: Double) =
        (sin(2 * PI * frame / SceneRenderer.FRAMES) * amount).roundToInt()

    internal fun drawSeated(c: PixelCanvas, guests: List<Guest>, frame: Int) {
        guests.take(SEATS.size).forEachIndexed { i, g -> drawBack(c, g, SEATS[i], i, frame) }
    }

    private fun drawBack(c: PixelCanvas, g: Guest, seat: Int, index: Int, frame: Int) {
        val x = seat - 4
        val f = (frame + index * 5) % SceneRenderer.FRAMES
        val pal = palette(g)
        val headDy = when (g.activity) {
            GuestActivity.RAMEN -> if (f in 4..7 || f in 12..15) 1 else 0
            GuestActivity.PHONE -> 1
            GuestActivity.TEA -> 0
        }

        if (g.activity == GuestActivity.PHONE) {
            c.disk(seat, 62, 4, argb(40 + sway(f, 15.0), 0x9AD8FF))
        }
        c.sprite(BODY, x, 66, pal)
        if (g.hairStyle == HairStyle.LONG) c.sprite(LONG_BACK, x, 66, pal)
        val head = backHead(g.hairStyle)
        c.sprite(head, x, 66 - head.size + headDy, pal)

        when (g.activity) {
            GuestActivity.RAMEN -> for (k in 0..1) {
                val s = (f + k * 8) % SceneRenderer.FRAMES
                c.px(seat - 3 + k * 6, 59 - s / 3, argb(160 - s * 10, 0xFFFFFF))
            }
            GuestActivity.TEA -> if (f in 8..12) {
                c.rect(x + 8, 64, 1, 3, g.outfitColor)
                c.rect(x + 8, 62, 2, 2, 0x6E9A5E)
                c.px(x + 9, 60 - (f - 8) / 2, argb(170, 0xFFFFFF))
            }
            GuestActivity.PHONE -> if ((f / 4) % 2 == 0) c.px(seat, 59, 0x9AD8FF)
        }

        val text = g.tag
        if (text.isEmpty()) return
        val w = PixelFont.width(text) + 4
        val left = seat - w / 2
        c.rect(left, 74, w, 7, argb(230, 0x1A1424))
        c.rect(left, 74, w, 1, g.outfitColor)
        PixelFont.forEachPixel(text) { px, py -> c.px(left + 2 + px, 75 + py, 0xFFF6EA) }
    }

    fun portrait(g: Guest): Bitmap {
        val rows = frontRows(g.hairStyle)
        val bmp = Bitmap.createBitmap(17, 20, Bitmap.Config.ARGB_8888)
        PixelCanvas(bmp).sprite(rows, 1, 20 - rows.size, palette(g).plus('h' to mix(g.hairColor, 0xFFFFFF, 0.3f)).plus('H' to g.hairColor))
        return bmp
    }

    private fun frontRows(style: HairStyle): List<String> {
        val rows = mutableListOf(
            "....HHHHHHH....",
            "..HHHHHHHHHHH..",
            ".HHHhHHHHHHHHH.",
            ".HHHHHHHHHHHHH.",
            "HHHHHHHHHHHHHHH",
            "HHHSHHSSSHHSHHH",
            "HHSSSSSSSSSSSHH",
            "HHSSwESSSwESSHH",
            "HHSSEESSSEESSHH",
            "HHSBBSSSSSBBSHH",
            ".HSSSSMSMSSSSH.",
            "..SSSSSMSSSSS..",
            "......SSS......",
            "...TTTVSVTTT...",
            "..TTTTTVTTTTT..",
            ".TTTTTTTTTTTTT.",
        )
        when (style) {
            HairStyle.SHORT -> Unit
            HairStyle.LONG -> for (r in 9 until rows.size) {
                val chars = rows[r].toCharArray()
                intArrayOf(0, 1, 13, 14).forEach { chars[it] = 'H' }
                rows[r] = String(chars)
            }
            HairStyle.BUN -> {
                rows.add(0, ".....HHHHH.....")
                rows.add(0, "......HHH......")
            }
            HairStyle.CAP -> {
                rows[0] = "....CCCCCCC...."
                rows[1] = "..CCCCCCCCCCC.."
                rows[2] = ".CCCCCCcCCCCCC."
                rows[3] = "ccccccccccccccc"
            }
        }
        return rows
    }
}
