package com.yasnost.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.SweepGradient
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.util.SizeF
import android.view.View
import android.widget.RemoteViews
import com.yasnost.app.MainActivity
import com.yasnost.app.R
import com.yasnost.app.data.AppState
import com.yasnost.app.data.Habit
import com.yasnost.app.data.HabitType
import com.yasnost.app.data.Repository
import com.yasnost.app.domain.Achievements
import com.yasnost.app.domain.DAY
import com.yasnost.app.domain.Fmt
import com.yasnost.app.domain.stats
import com.yasnost.app.domain.verb
import com.yasnost.app.work.Ticker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

private val widgetScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

/** Счётчик одной привычки: кольцо прогресса + живой таймер. */
class CounterWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) = async { WidgetUpdater.renderCounters(context, ids) }
    override fun onAppWidgetOptionsChanged(context: Context, manager: AppWidgetManager, id: Int, newOptions: Bundle?) =
        async { WidgetUpdater.renderCounters(context, intArrayOf(id)) }
    override fun onDeleted(context: Context, ids: IntArray) {
        val p = WidgetUpdater.prefs(context).edit()
        ids.forEach { p.remove("w_$it") }
        p.apply()
    }
    private fun async(block: suspend () -> Unit) {
        val pending = goAsync()
        widgetScope.launch { try { block() } finally { pending.finish() } }
    }
}

/** Сводка: все привычки сразу. */
class SummaryWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) = async { WidgetUpdater.renderSummaries(context, ids) }
    override fun onAppWidgetOptionsChanged(context: Context, manager: AppWidgetManager, id: Int, newOptions: Bundle?) =
        async { WidgetUpdater.renderSummaries(context, intArrayOf(id)) }
    private fun async(block: suspend () -> Unit) {
        val pending = goAsync()
        widgetScope.launch { try { block() } finally { pending.finish() } }
    }
}

object WidgetUpdater {
    fun prefs(ctx: Context) = ctx.getSharedPreferences("widgets", Context.MODE_PRIVATE)

    suspend fun updateAll(ctx: Context) {
        val m = AppWidgetManager.getInstance(ctx)
        renderCounters(ctx, m.getAppWidgetIds(ComponentName(ctx, CounterWidget::class.java)))
        renderSummaries(ctx, m.getAppWidgetIds(ComponentName(ctx, SummaryWidget::class.java)))
        Ticker.scheduleBoundary(ctx)
    }

    suspend fun renderCounters(ctx: Context, ids: IntArray) {
        if (ids.isEmpty()) return
        val s = Repository.get(ctx).current()
        val m = AppWidgetManager.getInstance(ctx)
        val now = System.currentTimeMillis()
        ids.forEach { id ->
            val habitId = prefs(ctx).getString("w_$id", null)
            val h = s.habit(habitId) ?: s.habits.firstOrNull()
            m.updateAppWidget(id, counterViews(ctx, id, h, s, now))
        }
    }

    suspend fun renderSummaries(ctx: Context, ids: IntArray) {
        if (ids.isEmpty()) return
        val s = Repository.get(ctx).current()
        val m = AppWidgetManager.getInstance(ctx)
        val now = System.currentTimeMillis()
        ids.forEach { id -> m.updateAppWidget(id, summaryViews(ctx, id, s, now)) }
    }

    // ---------- Счётчик ----------

    private fun counterViews(ctx: Context, id: Int, h: Habit?, s: AppState, now: Long): RemoteViews {
        if (h == null) return emptyViews(ctx)
        val small = counterLayout(ctx, R.layout.widget_counter_small, id, h, s, now)
        val wide = counterLayout(ctx, R.layout.widget_counter_wide, id, h, s, now)
        return if (Build.VERSION.SDK_INT >= 31) {
            RemoteViews(mapOf(SizeF(100f, 100f) to small, SizeF(220f, 100f) to wide))
        } else {
            val w = AppWidgetManager.getInstance(ctx).getAppWidgetOptions(id).getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
            if (w >= 220) wide else small
        }
    }

    private fun counterLayout(ctx: Context, layout: Int, id: Int, h: Habit, s: AppState, now: Long): RemoteViews {
        val st = h.stats(now)
        val v = RemoteViews(ctx.packageName, layout)
        v.setInt(R.id.root, "setBackgroundResource", bgFor(h.type))
        v.setImageViewBitmap(R.id.ring, ring(Achievements.progress(st.elapsed), h.type))
        v.setTextViewText(R.id.days, Fmt.intNum(st.days))
        v.setTextViewText(R.id.daysLabel, Fmt.days(st.days))
        v.setChronometer(R.id.timer, SystemClock.elapsedRealtime() - (st.elapsed % DAY), null, true)
        v.setTextColor(R.id.daysLabel, colorB(h.type))
        if (layout == R.layout.widget_counter_wide) {
            v.setTextViewText(R.id.title, h.title)
            v.setTextViewText(R.id.verb, h.type.verb.uppercase())
            v.setTextColor(R.id.verb, colorB(h.type))
            v.setTextViewText(R.id.money, "${Fmt.money(st.money)} ${s.settings.currency}")
            val next = Achievements.next(st.elapsed)
            v.setTextViewText(R.id.next, if (next != null) "до «${next.title}» ${Fmt.duration(next.millis - st.elapsed)}" else "все награды получены")
        }
        v.setOnClickPendingIntent(R.id.root, openApp(ctx, id, h.id))
        return v
    }

    // ---------- Сводка ----------

    private val rowIds = listOf(
        intArrayOf(R.id.row1, R.id.badge1, R.id.icon1, R.id.title1, R.id.days1, R.id.timer1, R.id.money1),
        intArrayOf(R.id.row2, R.id.badge2, R.id.icon2, R.id.title2, R.id.days2, R.id.timer2, R.id.money2),
        intArrayOf(R.id.row3, R.id.badge3, R.id.icon3, R.id.title3, R.id.days3, R.id.timer3, R.id.money3),
    )

    private fun summaryViews(ctx: Context, id: Int, s: AppState, now: Long): RemoteViews {
        if (s.habits.isEmpty()) return emptyViews(ctx)
        val v = RemoteViews(ctx.packageName, R.layout.widget_summary)
        val total = s.habits.sumOf { it.stats(now).money }
        v.setTextViewText(R.id.total, "${Fmt.money(total)} ${s.settings.currency} сохранено")
        rowIds.forEachIndexed { i, r ->
            val h = s.habits.getOrNull(i)
            if (h == null) {
                v.setViewVisibility(r[0], View.GONE)
                return@forEachIndexed
            }
            val st = h.stats(now)
            v.setViewVisibility(r[0], View.VISIBLE)
            v.setInt(r[1], "setBackgroundResource", badgeFor(h.type))
            v.setImageViewResource(r[2], iconFor(h.type))
            v.setTextViewText(r[3], h.title)
            v.setTextViewText(r[4], "${Fmt.intNum(st.days)} ${Fmt.days(st.days)}")
            v.setTextColor(r[4], colorB(h.type))
            v.setChronometer(r[5], SystemClock.elapsedRealtime() - (st.elapsed % DAY), null, true)
            v.setTextViewText(r[6], "${Fmt.money(st.money)} ${s.settings.currency}")
            v.setOnClickPendingIntent(r[0], openApp(ctx, id * 10 + i, h.id))
        }
        v.setOnClickPendingIntent(R.id.root, openApp(ctx, id * 10 + 9, null))
        return v
    }

    private fun emptyViews(ctx: Context): RemoteViews {
        val v = RemoteViews(ctx.packageName, R.layout.widget_empty)
        v.setOnClickPendingIntent(R.id.root, openApp(ctx, 0, null))
        return v
    }

    private fun openApp(ctx: Context, req: Int, habitId: String?): PendingIntent {
        val i = Intent(ctx, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            if (habitId != null) putExtra(MainActivity.EXTRA_HABIT, habitId)
            data = android.net.Uri.parse("yasnost://open/$req/${habitId ?: "home"}")
        }
        return PendingIntent.getActivity(ctx, req, i, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    // ---------- Графика ----------

    private fun colorA(t: HabitType) = when (t) {
        HabitType.ALCOHOL -> 0xFF8B7CFF.toInt(); HabitType.SMOKING -> 0xFF2DE3C5.toInt(); HabitType.CUSTOM -> 0xFFFFB547.toInt()
    }
    private fun colorB(t: HabitType) = when (t) {
        HabitType.ALCOHOL -> 0xFFE07CFF.toInt(); HabitType.SMOKING -> 0xFF8CF5A8.toInt(); HabitType.CUSTOM -> 0xFFFF6B6B.toInt()
    }
    private fun bgFor(t: HabitType) = when (t) {
        HabitType.ALCOHOL -> R.drawable.widget_bg_alcohol; HabitType.SMOKING -> R.drawable.widget_bg_smoking; HabitType.CUSTOM -> R.drawable.widget_bg_custom
    }
    private fun badgeFor(t: HabitType) = when (t) {
        HabitType.ALCOHOL -> R.drawable.badge_alcohol; HabitType.SMOKING -> R.drawable.badge_smoking; HabitType.CUSTOM -> R.drawable.badge_custom
    }
    fun iconFor(t: HabitType) = when (t) {
        HabitType.ALCOHOL -> R.drawable.ic_wine; HabitType.SMOKING -> R.drawable.ic_cigarette; HabitType.CUSTOM -> R.drawable.ic_spark
    }

    /** Кольцо прогресса до следующей награды. */
    private fun ring(progress: Float, t: HabitType): Bitmap {
        val size = 360
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val sw = 26f
        val pad = sw / 2 + 14f
        val r = RectF(pad, pad, size - pad, size - pad)
        val track = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = sw; color = 0x1AFFFFFF }
        c.drawArc(r, 0f, 360f, false, track)
        val a = colorA(t); val b = colorB(t)
        val sweep = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE; strokeWidth = sw; strokeCap = Paint.Cap.ROUND
            shader = SweepGradient(size / 2f, size / 2f, intArrayOf(a, b, a), floatArrayOf(0f, 0.5f, 1f)).apply {
                val m = android.graphics.Matrix(); m.setRotate(-90f, size / 2f, size / 2f); setLocalMatrix(m)
            }
        }
        val p = progress.coerceIn(0.01f, 1f)
        c.drawArc(r, -90f, 360f * p, false, sweep)
        val ang = Math.toRadians(360.0 * p - 90.0)
        val rad = r.width() / 2
        val cx = size / 2f + (rad * Math.cos(ang)).toFloat()
        val cy = size / 2f + (rad * Math.sin(ang)).toFloat()
        val glow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(cx, cy, sw * 1.8f, (b and 0x00FFFFFF) or (0xB0 shl 24), 0x00000000, Shader.TileMode.CLAMP)
        }
        c.drawCircle(cx, cy, sw * 1.8f, glow)
        c.drawCircle(cx, cy, sw * 0.3f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFFFFF.toInt() })
        return bmp
    }
}
