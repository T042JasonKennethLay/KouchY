package com.yatai.widget

import java.util.TimeZone
import java.util.concurrent.TimeUnit
import kotlin.random.Random

enum class Weather(val label: String) {
    RAIN("Hujan"), SNOW("Salju"), AUTUMN("Gugur"), SUMMER("Musim panas"), CLEAR("Cerah");

    companion object {
        private const val HOURS_PER_WEATHER = 3

        private fun localHours(now: Long) =
            TimeUnit.MILLISECONDS.toHours(now + TimeZone.getDefault().getOffset(now))

        fun current(now: Long = System.currentTimeMillis()): Weather {
            val slot = localHours(now) / HOURS_PER_WEATHER
            return entries[Random(slot).nextInt(entries.size)]
        }

        fun nextChangeHour(now: Long = System.currentTimeMillis()): Int =
            ((localHours(now) / HOURS_PER_WEATHER + 1) * HOURS_PER_WEATHER % 24).toInt()
    }
}
