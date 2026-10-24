package com.rumahku.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.SystemClock
import android.widget.RemoteViews

class RumahWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        WidgetUpdater.updateAll(context)
        UpdateScheduler.schedule(context)
    }

    override fun onEnabled(context: Context) = UpdateScheduler.schedule(context)

    override fun onDisabled(context: Context) = UpdateScheduler.cancel(context)

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH) WidgetUpdater.updateAll(context)
    }

    companion object {
        const val ACTION_REFRESH = "com.rumahku.widget.REFRESH"

        fun refreshIntent(ctx: Context) =
            Intent(ctx, RumahWidget::class.java).setAction(ACTION_REFRESH)
    }
}

object WidgetUpdater {
    private val FRAME_IDS = intArrayOf(R.id.frame0, R.id.frame1, R.id.frame2, R.id.frame3)

    fun updateAll(ctx: Context) {
        val manager = AppWidgetManager.getInstance(ctx)
        val ids = manager.getAppWidgetIds(ComponentName(ctx, RumahWidget::class.java))
        if (ids.isEmpty()) return

        val stats = PhoneUsage.read(ctx)
        val hour = PhoneUsage.currentHour()
        val mood = MoodEngine.decide(stats, hour)
        val phase = DayPhase.of(hour)
        val frames = SceneRenderer.renderFrames(SceneState(mood, phase))

        for (scale in intArrayOf(4, 3, 2)) {
            try {
                manager.updateAppWidget(ids, buildViews(ctx, frames, scale, mood.bubble(phase), stats))
                return
            } catch (e: RuntimeException) {
                continue
            }
        }
    }

    private fun buildViews(
        ctx: Context,
        frames: List<Bitmap>,
        scale: Int,
        bubble: String,
        stats: PhoneStats,
    ): RemoteViews {
        val views = RemoteViews(ctx.packageName, R.layout.widget_rumah)
        frames.forEachIndexed { i, frame ->
            views.setImageViewBitmap(FRAME_IDS[i], SceneRenderer.upscale(frame, scale))
        }
        views.setTextViewText(R.id.bubble, bubble)

        val click = if (stats.hasPermission) {
            PendingIntent.getBroadcast(
                ctx, 1, RumahWidget.refreshIntent(ctx),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        } else {
            PendingIntent.getActivity(
                ctx, 2, Intent(ctx, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }
        views.setOnClickPendingIntent(R.id.widget_root, click)
        return views
    }
}

object UpdateScheduler {
    fun schedule(ctx: Context) {
        val interval = AlarmManager.INTERVAL_FIFTEEN_MINUTES
        ctx.getSystemService(AlarmManager::class.java).setInexactRepeating(
            AlarmManager.ELAPSED_REALTIME,
            SystemClock.elapsedRealtime() + interval,
            interval,
            pending(ctx),
        )
    }

    fun cancel(ctx: Context) = ctx.getSystemService(AlarmManager::class.java).cancel(pending(ctx))

    private fun pending(ctx: Context) = PendingIntent.getBroadcast(
        ctx, 0, RumahWidget.refreshIntent(ctx),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
}
