package com.yatai.widget

import android.graphics.Bitmap

object PixelFont {
    const val HEIGHT = 5

    private val GLYPHS: Map<Char, List<String>> = mapOf(
        'A' to listOf(".#.", "#.#", "###", "#.#", "#.#"),
        'B' to listOf("##.", "#.#", "##.", "#.#", "##."),
        'C' to listOf(".##", "#..", "#..", "#..", ".##"),
        'D' to listOf("##.", "#.#", "#.#", "#.#", "##."),
        'E' to listOf("###", "#..", "##.", "#..", "###"),
        'F' to listOf("###", "#..", "##.", "#..", "#.."),
        'G' to listOf(".##", "#..", "#.#", "#.#", ".##"),
        'H' to listOf("#.#", "#.#", "###", "#.#", "#.#"),
        'I' to listOf("###", ".#.", ".#.", ".#.", "###"),
        'J' to listOf("..#", "..#", "..#", "#.#", ".#."),
        'K' to listOf("#..#", "#.#.", "##..", "#.#.", "#..#"),
        'L' to listOf("#..", "#..", "#..", "#..", "###"),
        'M' to listOf("#...#", "##.##", "#.#.#", "#...#", "#...#"),
        'N' to listOf("#..#", "##.#", "#.##", "#..#", "#..#"),
        'O' to listOf(".#.", "#.#", "#.#", "#.#", ".#."),
        'P' to listOf("##.", "#.#", "##.", "#..", "#.."),
        'Q' to listOf(".#.", "#.#", "#.#", "##.", ".##"),
        'R' to listOf("##.", "#.#", "##.", "#.#", "#.#"),
        'S' to listOf(".##", "#..", ".#.", "..#", "##."),
        'T' to listOf("###", ".#.", ".#.", ".#.", ".#."),
        'U' to listOf("#.#", "#.#", "#.#", "#.#", "###"),
        'V' to listOf("#.#", "#.#", "#.#", "#.#", ".#."),
        'W' to listOf("#...#", "#...#", "#.#.#", "##.##", "#...#"),
        'X' to listOf("#.#", "#.#", ".#.", "#.#", "#.#"),
        'Y' to listOf("#.#", "#.#", ".#.", ".#.", ".#."),
        'Z' to listOf("###", "..#", ".#.", "#..", "###"),
        '0' to listOf("###", "#.#", "#.#", "#.#", "###"),
        '1' to listOf(".#.", "##.", ".#.", ".#.", "###"),
        '2' to listOf("##.", "..#", ".#.", "#..", "###"),
        '3' to listOf("##.", "..#", ".#.", "..#", "##."),
        '4' to listOf("#.#", "#.#", "###", "..#", "..#"),
        '5' to listOf("###", "#..", "##.", "..#", "##."),
        '6' to listOf(".##", "#..", "###", "#.#", "###"),
        '7' to listOf("###", "..#", ".#.", ".#.", ".#."),
        '8' to listOf("###", "#.#", "###", "#.#", "###"),
        '9' to listOf("###", "#.#", "###", "..#", "##."),
        ' ' to listOf("...", "...", "...", "...", "..."),
        '!' to listOf(".#.", ".#.", ".#.", "...", ".#."),
        '?' to listOf("##.", "..#", ".#.", "...", ".#."),
        '-' to listOf("...", "...", "###", "...", "..."),
        '.' to listOf("...", "...", "...", "...", ".#."),
        ':' to listOf("...", ".#.", "...", ".#.", "..."),
        '/' to listOf("..#", "..#", ".#.", "#..", "#.."),
        '+' to listOf("...", ".#.", "###", ".#.", "..."),
        '_' to listOf("...", "...", "...", "...", "###"),
        ',' to listOf("..", "..", "..", ".#", "#."),
        '\'' to listOf("#", "#", ".", ".", "."),
        '(' to listOf(".#", "#.", "#.", "#.", ".#"),
        ')' to listOf("#.", ".#", ".#", ".#", "#."),
        '~' to listOf("....", "....", ".#.#", "#.#.", "...."),
        '%' to listOf("#.#", "..#", ".#.", "#..", "#.#"),
        '=' to listOf("...", "###", "...", "###", "..."),
        '<' to listOf("..#", ".#.", "#..", ".#.", "..#"),
        '>' to listOf("#..", ".#.", "..#", ".#.", "#.."),
    )

    fun supports(ch: Char) = GLYPHS.containsKey(ch.uppercaseChar())

    private fun advance(ch: Char) = (GLYPHS[ch]?.first()?.length ?: 3) + 1

    fun width(text: String) = text.uppercase().sumOf { advance(it) }.minus(1).coerceAtLeast(0)

    fun forEachPixel(text: String, block: (Int, Int) -> Unit) {
        var cursor = 0
        text.uppercase().forEach { ch ->
            GLYPHS[ch]?.forEachIndexed { y, row ->
                row.forEachIndexed { x, c -> if (c == '#') block(cursor + x, y) }
            }
            cursor += advance(ch)
        }
    }

    fun bitmap(text: String, color: Int, shadow: Int? = null): Bitmap {
        val bmp = Bitmap.createBitmap(maxOf(width(text) + 1, 1), HEIGHT + 1, Bitmap.Config.ARGB_8888)
        val c = PixelCanvas(bmp)
        if (shadow != null) forEachPixel(text) { x, y -> c.px(x + 1, y + 1, shadow) }
        forEachPixel(text) { x, y -> c.px(x, y, color) }
        return bmp
    }
}
