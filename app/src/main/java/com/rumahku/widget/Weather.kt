package com.rumahku.widget

import java.util.TimeZone
import java.util.concurrent.TimeUnit
import kotlin.random.Random

enum class Weather(val label: String) {
    RAIN("Hujan"), SNOW("Salju"), AUTUMN("Gugur"), SUMMER("Musim panas"), CLEAR("Cerah");

    companion object {
        private const val HOURS_PER_WEATHER = 3

        fun current(now: Long = System.currentTimeMillis()): Weather {
            val localHours = TimeUnit.MILLISECONDS.toHours(now + TimeZone.getDefault().getOffset(now))
            val slot = localHours / HOURS_PER_WEATHER
            return entries[Random(slot).nextInt(entries.size)]
        }
    }
}
