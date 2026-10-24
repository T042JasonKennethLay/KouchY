package com.rumahku.widget

enum class DayPhase(val label: String) {
    MORNING("Pagi"), DAY("Siang"), EVENING("Sore"), NIGHT("Malam");

    companion object {
        fun of(hour: Int) = when (hour) {
            in 5..8 -> MORNING
            in 9..15 -> DAY
            in 16..18 -> EVENING
            else -> NIGHT
        }
    }
}
