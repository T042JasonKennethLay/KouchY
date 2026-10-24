package com.rumahku.widget

enum class Mood(
    val label: String,
    val title: String,
    val line: String,
    val lampOn: Boolean,
) {
    HAPPY("Senang", "Senang! 🍜", "Kedai rame, makasih udah mampir", true),
    LONELY("Kesepian", "Kesepian…", "Kedainya sepi, kamu ke mana aja?", false),
    TIRED("Capek", "Pusing… 😵", "Kebanyakan main HP nih, istirahat yuk", true),
    SLEEPY("Ngantuk", "Ngantuk… 🥱", "Udah larut, tidur yuk", true),
    SLEEPING("Tidur", "Zzz… 😴", "Kedai tutup dulu, lagi tidur", false),
    NO_PERMISSION("Belum diizinkan", "Halo! 👋", "Ketuk aku buat kasih izin ya", true);

    fun bubble(phase: DayPhase, weather: Weather): String = when (this) {
        HAPPY -> when {
            weather == Weather.SNOW -> "yuki da~! ❄️"
            weather == Weather.AUTUMN -> "momiji~ 🍁"
            weather == Weather.SUMMER && (phase == DayPhase.NIGHT || phase == DayPhase.EVENING) -> "hanabi~! 🎆"
            weather == Weather.SUMMER -> "atsui~ 🍧"
            weather == Weather.CLEAR && phase == DayPhase.NIGHT -> "irasshai~! 🌃"
            phase == DayPhase.MORNING -> "ohayō~ ☀️"
            phase == DayPhase.DAY -> "itadakimasu~ 🍜"
            else -> "irasshai~! 🏮"
        }
        LONELY -> if (weather == Weather.SNOW) "dingin… sepi… ❄️" else "sepi ya… 🌧️"
        TIRED -> "pusiiing… 😵"
        SLEEPY -> "hoaam… 🥱"
        SLEEPING -> "tutup dulu… 💤"
        NO_PERMISSION -> "tap aku dong! 👋"
    }
}

object MoodEngine {
    private const val TIRED_TODAY_MINUTES = 300
    private const val TIRED_SESSION_MINUTES = 90
    private const val LONELY_IDLE_MINUTES = 180
    private const val LONELY_TODAY_MINUTES = 15
    private const val SLEEP_IDLE_MINUTES = 120

    fun decide(stats: PhoneStats, hour: Int): Mood {
        if (!stats.hasPermission) return Mood.NO_PERMISSION

        val lateNight = hour >= 23 || hour < 5
        val sleepHours = lateNight || hour < 8
        return when {
            sleepHours && stats.idleMinutes >= SLEEP_IDLE_MINUTES && stats.sessionMinutes < 5 -> Mood.SLEEPING
            lateNight -> Mood.SLEEPY
            stats.screenMinutes >= TIRED_TODAY_MINUTES || stats.sessionMinutes >= TIRED_SESSION_MINUTES -> Mood.TIRED
            stats.idleMinutes >= LONELY_IDLE_MINUTES && stats.sessionMinutes < 10 -> Mood.LONELY
            stats.screenMinutes < LONELY_TODAY_MINUTES && hour >= 10 -> Mood.LONELY
            else -> Mood.HAPPY
        }
    }
}
