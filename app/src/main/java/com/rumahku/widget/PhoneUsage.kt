package com.rumahku.widget

import android.Manifest
import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Process
import java.util.Calendar

data class PhoneStats(
    val hasPermission: Boolean,
    val screenMinutes: Int = 0,
    val unlocks: Int = 0,
    val idleMinutes: Int = 0,
    val sessionMinutes: Int = 0,
)

object PhoneUsage {
    private const val DAY_MS = 24 * 60 * 60 * 1000L
    private const val NO_DATA_MINUTES = 24 * 60

    fun hasPermission(ctx: Context): Boolean {
        val ops = ctx.getSystemService(AppOpsManager::class.java)
        val mode = ops.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), ctx.packageName
        )
        if (mode == AppOpsManager.MODE_DEFAULT) {
            return ctx.checkSelfPermission(Manifest.permission.PACKAGE_USAGE_STATS) ==
                PackageManager.PERMISSION_GRANTED
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun read(ctx: Context, now: Long = System.currentTimeMillis()): PhoneStats {
        if (!hasPermission(ctx)) return PhoneStats(hasPermission = false)

        val usm = ctx.getSystemService(UsageStatsManager::class.java)
        val dayStart = startOfDay(now)
        val events = usm.queryEvents(dayStart - DAY_MS, now)
        val e = UsageEvents.Event()

        var screenMs = 0L
        var unlocks = 0
        var onSince: Long? = null
        var lastOff = 0L
        var idleBeforeSession = 0L

        while (events.hasNextEvent()) {
            events.getNextEvent(e)
            val t = e.timeStamp
            when (e.eventType) {
                UsageEvents.Event.SCREEN_INTERACTIVE -> if (onSince == null) {
                    idleBeforeSession = if (lastOff > 0) t - lastOff else DAY_MS
                    onSince = t
                }
                UsageEvents.Event.SCREEN_NON_INTERACTIVE -> {
                    onSince?.let { screenMs += overlap(it, t, dayStart, now) }
                    onSince = null
                    lastOff = t
                }
                UsageEvents.Event.KEYGUARD_HIDDEN -> if (t >= dayStart) unlocks++
            }
        }

        val sessionMs: Long
        val idleMs: Long
        val current = onSince
        if (current != null) {
            screenMs += overlap(current, now, dayStart, now)
            sessionMs = now - current
            idleMs = idleBeforeSession
        } else {
            sessionMs = 0
            idleMs = if (lastOff > 0) now - lastOff else NO_DATA_MINUTES * 60_000L
        }

        return PhoneStats(
            hasPermission = true,
            screenMinutes = (screenMs / 60_000).toInt(),
            unlocks = unlocks,
            idleMinutes = (idleMs / 60_000).toInt(),
            sessionMinutes = (sessionMs / 60_000).toInt(),
        )
    }

    fun formatStats(stats: PhoneStats): String {
        if (!stats.hasPermission) return "belum ada izin screen time"
        val h = stats.screenMinutes / 60
        val m = stats.screenMinutes % 60
        val time = if (h > 0) "${h}j ${m}m" else "${m}m"
        return "📱 $time hari ini · ${stats.unlocks}x buka"
    }

    fun currentHour(): Int = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)

    private fun startOfDay(now: Long): Long = Calendar.getInstance().apply {
        timeInMillis = now
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private fun overlap(start: Long, end: Long, from: Long, to: Long): Long =
        (minOf(end, to) - maxOf(start, from)).coerceAtLeast(0)
}
