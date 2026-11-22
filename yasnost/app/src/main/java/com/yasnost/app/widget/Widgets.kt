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
import com.yasnost.app.domain.unitsLabel
import com.yasnost.app.domain.Levels
import com.yasnost.app.domain.Health
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

/** Мини 1×1: только число дней. */
class MiniWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) = async { WidgetUpdater.renderMinis(context, ids) }
    override fun onAppWidgetOptionsChanged(context: Context, manager: AppWidgetManager, id: Int, newOptions: Bundle?) =
        async { WidgetUpdater.renderMinis(context, intArrayOf(id)) }
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

/** Ярлыки 1×1 без настройки. */
open class IconWidget(private val kind: String) : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val pending = goAsync()
        widgetScope.launch { try { WidgetUpdater.renderIcons(context, ids, kind) } finally { pending.finish() } }
    }
    override fun onAppWidgetOptionsChanged(context: Context, manager: AppWidgetManager, id: Int, newOptions: Bundle?) {
        val pending = goAsync()
        widgetScope.launch { try { WidgetUpdater.renderIcons(context, intArrayOf(id), kind) } finally { pending.finish() } }
    }
}
class MiniAlcoholWidget : IconWidget("alcohol")
class MiniSmokingWidget : IconWidget("smoking")
class MiniCycleWidget : IconWidget("cycle")

object WidgetUpdater {
    fun prefs(ctx: Context) = ctx.getSharedPreferences("widgets", Context.MODE_PRIVATE)

    suspend fun updateAll(ctx: Context) {
        val m = AppWidgetManager.getInstance(ctx)
        renderCounters(ctx, m.getAppWidgetIds(ComponentName(ctx, CounterWidget::class.java)))
        renderSummaries(ctx, m.getAppWidgetIds(ComponentName(ctx, SummaryWidget::class.java)))
        renderMinis(ctx, m.getAppWidgetIds(ComponentName(ctx, MiniWidget::class.java)))
        renderIcons(ctx, m.getAppWidgetIds(ComponentName(ctx, MiniAlcoholWidget::class.java)), "alcohol")
        renderIcons(ctx, m.getAppWidgetIds(ComponentName(ctx, MiniSmokingWidget::class.java)), "smoking")
        renderIcons(ctx, m.getAppWidgetIds(ComponentName(ctx, MiniCycleWidget::class.java)), "cycle")
        Ticker.scheduleBoundary(ctx)
    }

    /** Ярлык с числом. kind: alcohol / smoking / cycle */
    suspend fun renderIcons(ctx: Context, ids: IntArray, kind: String) {
        if (ids.isEmpty()) return
        val s = Repository.get(ctx).current()
        val m = AppWidgetManager.getInstance(ctx)
        val now = System.currentTimeMillis()
        ids.forEach { id ->
            val v = when (kind) {
                "cycle" -> cycleIcon(ctx, id, s)
                else -> {
                    val type = if (kind == "smoking") HabitType.SMOKING else HabitType.ALCOHOL
                    habitIcon(ctx, id, s.habits.firstOrNull { it.type == type }, type, now, s)
                }
            }
            m.updateAppWidget(id, v)
        }
    }

    private fun tileFor(t: HabitType) = when (t) {
        HabitType.ALCOHOL -> R.drawable.tile_bg_alcohol; HabitType.SMOKING -> R.drawable.tile_bg_smoking; HabitType.CUSTOM -> R.drawable.tile_bg_custom
    }

    /** Подгоняем плитку под размер иконок конкретного лаунчера. */
    fun iconTileDp(ctx: Context, id: Int, s: AppState): Int {
        if (s.settings.iconSize > 0) return s.settings.iconSize
        val o = AppWidgetManager.getInstance(ctx).getAppWidgetOptions(id)
        val w = o.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
        val h = o.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT)
        if (w <= 0) return 56
        var t = (w * 0.62f).toInt()
        if (h > 0) t = minOf(t, h - 24)
        return t.coerceIn(40, 68)
    }

    private fun applyIconGeometry(ctx: Context, v: RemoteViews, id: Int, s: AppState) {
        val tile = iconTileDp(ctx, id, s)
        if (Build.VERSION.SDK_INT >= 31) {
            v.setViewLayoutWidth(R.id.tile, tile.toFloat(), android.util.TypedValue.COMPLEX_UNIT_DIP)
            v.setViewLayoutHeight(R.id.tile, tile.toFloat(), android.util.TypedValue.COMPLEX_UNIT_DIP)
            v.setViewLayoutWidth(R.id.glyph, tile * 0.54f, android.util.TypedValue.COMPLEX_UNIT_DIP)
            v.setViewLayoutHeight(R.id.glyph, tile * 0.54f, android.util.TypedValue.COMPLEX_UNIT_DIP)
        }
        val d = ctx.resources.displayMetrics.density
        val off = (s.settings.iconOffset * 2 * d).toInt()
        v.setViewPadding(R.id.root, 0, if (off > 0) off else 0, 0, if (off < 0) -off else 0)
    }

    private fun habitIcon(ctx: Context, id: Int, h: Habit?, type: HabitType, now: Long, s: AppState? = null): RemoteViews {
        val v = RemoteViews(ctx.packageName, R.layout.widget_icon)
        if (s != null) applyIconGeometry(ctx, v, id, s)
        h?.let { v.setTextViewText(R.id.caption, Fmt.days(it.stats(now).days)) } ?: v.setTextViewText(R.id.caption, "")
        v.setInt(R.id.tile, "setBackgroundResource", tileFor(type))
        v.setImageViewResource(R.id.glyph, iconFor(type))
        v.setTextViewText(R.id.days, h?.let { Fmt.intNum(it.stats(now).days) } ?: "—")
        v.setTextViewText(R.id.label, if (h == null) "Ясность" else when (type) {
            HabitType.ALCOHOL -> "Не пью"; HabitType.SMOKING -> "Не курю"; HabitType.CUSTOM -> h.title
        })
        v.setOnClickPendingIntent(R.id.root, openApp(ctx, id, h?.id))
        return v
    }

    private fun cycleIcon(ctx: Context, id: Int, s: AppState): RemoteViews {
        val v = RemoteViews(ctx.packageName, R.layout.widget_icon)
        applyIconGeometry(ctx, v, id, s)
        v.setInt(R.id.tile, "setBackgroundResource", R.drawable.tile_bg_cycle)
        v.setImageViewResource(R.id.glyph, R.drawable.ic_drop)
        val ci = if (s.cycleSettings.enabled) com.yasnost.app.domain.CycleLogic.info(s, java.time.LocalDate.now().toEpochDay()) else null
        val name = if (s.profile.female) null else s.cyclePartner()?.name
        v.setTextViewText(R.id.label, name ?: "Месячные")
        when {
            ci == null || !ci.hasData -> { v.setTextViewText(R.id.days, "—"); v.setTextViewText(R.id.caption, "нет данных") }
            ci.inPeriodNow -> { v.setTextViewText(R.id.days, "${ci.today - ci.lastStart!! + 1}"); v.setTextViewText(R.id.caption, "день") }
            ci.lateDays > 0 -> { v.setTextViewText(R.id.days, "+${ci.lateDays}"); v.setTextViewText(R.id.caption, "задержка") }
            else -> { val n = ci.nextStart!! - ci.today; v.setTextViewText(R.id.days, "$n"); v.setTextViewText(R.id.caption, Fmt.days(n) + " до") }
        }
        v.setOnClickPendingIntent(R.id.root, openApp(ctx, id + 50000, null))
        return v
    }

    /** Обновить один виджет любого типа (после настройки). */
    suspend fun renderById(ctx: Context, id: Int) {
        val cls = AppWidgetManager.getInstance(ctx).getAppWidgetInfo(id)?.provider?.className
        when (cls) {
            MiniWidget::class.java.name -> renderMinis(ctx, intArrayOf(id))
            SummaryWidget::class.java.name -> renderSummaries(ctx, intArrayOf(id))
            else -> renderCounters(ctx, intArrayOf(id))
        }
    }

    suspend fun renderMinis(ctx: Context, ids: IntArray) {
        if (ids.isEmpty()) return
        val s = Repository.get(ctx).current()
        val m = AppWidgetManager.getInstance(ctx)
        val now = System.currentTimeMillis()
        ids.forEach { id ->
            val h = s.habit(prefs(ctx).getString("w_$id", null)) ?: s.habits.firstOrNull()
            m.updateAppWidget(id, if (h == null) cycleIcon(ctx, id, s) else habitIcon(ctx, id, h, h.type, now, s))
        }
    }

    suspend fun renderCounters(ctx: Context, ids: IntArray) {
        if (ids.isEmpty()) return
        val s = Repository.get(ctx).current()
        val m = AppWidgetManager.getInstance(ctx)
        val now = System.currentTimeMillis()
        ids.forEach { id ->
            val habitId = prefs(ctx).getString("w_$id", null)
            val h = s.habit(habitId) ?: s.habits.firstOrNull()
            m.updateAppWidget(id, if (h == null) cycleOrEmpty(ctx, id, s) else counterViews(ctx, id, h, s, now))
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
        val large = counterLayout(ctx, R.layout.widget_counter_large, id, h, s, now)
        return if (Build.VERSION.SDK_INT >= 31) {
            RemoteViews(mapOf(SizeF(100f, 100f) to small, SizeF(220f, 100f) to wide, SizeF(220f, 280f) to large))
        } else {
            val o = AppWidgetManager.getInstance(ctx).getAppWidgetOptions(id)
            val w = o.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
            val hh = o.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT)
            if (w >= 220 && hh >= 280) large else if (w >= 220) wide else small
        }
    }

    private fun counterLayout(ctx: Context, layout: Int, id: Int, h: Habit, s: AppState, now: Long): RemoteViews {
        val st = h.stats(now)
        val cur = s.settings.currency
        val v = RemoteViews(ctx.packageName, layout)
        v.setInt(R.id.root, "setBackgroundResource", bgFor(h.type))
        v.setInt(R.id.badge, "setBackgroundResource", badgeFor(h.type))
        v.setImageViewResource(R.id.icon, iconFor(h.type))
        v.setTextViewText(R.id.verb, h.type.verb.uppercase())
        v.setTextColor(R.id.verb, colorB(h.type))
        v.setImageViewBitmap(R.id.ring, ring(Achievements.progress(st.elapsed), h.type))
        v.setTextViewText(R.id.days, Fmt.intNum(st.days))
        v.setTextViewText(R.id.daysLabel, Fmt.days(st.days))
        v.setTextColor(R.id.daysLabel, colorB(h.type))
        v.setChronometer(R.id.timer, SystemClock.elapsedRealtime() - (st.elapsed % DAY), null, true)
        val next = Achievements.next(st.elapsed)
        val nextText = if (next != null) "до «${next.title}» · ${Fmt.duration(next.millis - st.elapsed)}" else "все награды за время получены"
        if (layout == R.layout.widget_counter_small) {
            v.setTextViewText(R.id.money, "${Fmt.money(st.money)} $cur")
        } else {
            v.setTextViewText(R.id.next, nextText)
            v.setImageViewBitmap(R.id.bar, bar(Achievements.progress(st.elapsed), h.type))
            v.setTextViewText(R.id.stat1, "${Fmt.money(st.money)} $cur")
            if (h.unitsPerDay > 0) {
                v.setTextViewText(R.id.stat2Label, h.type.unitsLabel.uppercase())
                v.setTextViewText(R.id.stat2, "${Fmt.num(st.units)} ${h.unitName}")
            } else {
                val best = s.bestStreak(h.id, now) / DAY
                v.setTextViewText(R.id.stat2Label, "РЕКОРД")
                v.setTextViewText(R.id.stat2, "$best ${Fmt.days(best)}")
            }
        }
        if (layout == R.layout.widget_counter_large) {
            val lvl = Levels.of(s, now)
            v.setViewVisibility(R.id.level, View.VISIBLE)
            v.setTextViewText(R.id.level, "Ур. ${lvl.n} · ${lvl.title}")
            val best = s.bestStreak(h.id, now) / DAY
            v.setTextViewText(R.id.stat3, "$best ${Fmt.days(best)}")
            val steps = Health.forType(h.type, s.profile.female)
            val step = steps.firstOrNull { it.afterMillis > st.elapsed }
            if (step != null) {
                v.setTextViewText(R.id.healthTitle, "СЛЕДУЮЩИЙ ЭТАП · ${step.title.uppercase()} · ЧЕРЕЗ ${Fmt.duration(step.afterMillis - st.elapsed).uppercase()}")
                v.setTextViewText(R.id.healthText, step.text)
            } else {
                v.setTextViewText(R.id.healthTitle, "ВСЕ ЭТАПЫ ПРОЙДЕНЫ")
                v.setTextViewText(R.id.healthText, steps.last().text)
            }
            v.setTextColor(R.id.healthTitle, colorB(h.type))
        }
        v.setOnClickPendingIntent(R.id.root, openApp(ctx, id, h.id))
        return v
    }

    // ---------- Сводка ----------

    private val rowIds = listOf(
        intArrayOf(R.id.row1, R.id.badge1, R.id.icon1, R.id.title1, R.id.days1, R.id.timer1, R.id.money1, R.id.bar1, R.id.next1),
        intArrayOf(R.id.row2, R.id.badge2, R.id.icon2, R.id.title2, R.id.days2, R.id.timer2, R.id.money2, R.id.bar2, R.id.next2),
        intArrayOf(R.id.row3, R.id.badge3, R.id.icon3, R.id.title3, R.id.days3, R.id.timer3, R.id.money3, R.id.bar3, R.id.next3),
    )

    private fun summaryViews(ctx: Context, id: Int, s: AppState, now: Long): RemoteViews {
        if (s.habits.isEmpty()) return cycleOrEmpty(ctx, id, s)
        val compact = summaryLayout(ctx, id, s, now, tall = false)
        val tall = summaryLayout(ctx, id, s, now, tall = true)
        return if (Build.VERSION.SDK_INT >= 31) RemoteViews(mapOf(SizeF(200f, 100f) to compact, SizeF(200f, 230f) to tall))
        else {
            val hh = AppWidgetManager.getInstance(ctx).getAppWidgetOptions(id).getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT)
            if (hh >= 230) tall else compact
        }
    }

    private fun summaryLayout(ctx: Context, id: Int, s: AppState, now: Long, tall: Boolean): RemoteViews {
        val v = RemoteViews(ctx.packageName, R.layout.widget_summary)
        val cur = s.settings.currency
        val total = s.habits.sumOf { it.stats(now).money }
        v.setTextViewText(R.id.total, "${Fmt.money(total)} $cur")
        val lvl = Levels.of(s, now)
        v.setTextViewText(R.id.level, "Ур. ${lvl.n} · ${lvl.title}")
        val shown = s.habits.take(if (tall) 3 else 2)
        if (shown.size == 1) return singleLayout(ctx, id, s, shown[0], now, tall)
        rowIds.forEachIndexed { i, r ->
            val h = shown.getOrNull(i)
            if (h == null) { v.setViewVisibility(r[0], View.GONE); return@forEachIndexed }
            val st = h.stats(now)
            v.setViewVisibility(r[0], View.VISIBLE)
            v.setInt(r[1], "setBackgroundResource", badgeFor(h.type))
            v.setImageViewResource(r[2], iconFor(h.type))
            v.setTextViewText(r[3], "${Fmt.days(st.days)} · ${h.type.verb.lowercase()}")
            v.setTextViewText(r[4], Fmt.intNum(st.days))
            v.setTextColor(r[4], colorB(h.type))
            v.setChronometer(r[5], SystemClock.elapsedRealtime() - (st.elapsed % DAY), null, true)
            v.setTextViewText(r[6], "${Fmt.money(st.money)} $cur")
            v.setImageViewBitmap(r[7], bar(Achievements.progress(st.elapsed), h.type))
            val next = Achievements.next(st.elapsed)
            v.setTextViewText(r[8], if (next != null) "до «${next.title}» · ${Fmt.duration(next.millis - st.elapsed)}" else "все награды получены")
            v.setOnClickPendingIntent(r[0], openApp(ctx, id * 10 + i, h.id))
        }
        // нижняя часть: цикл и цитата, если есть место
        val roomy = tall || shown.size == 1
        val today = java.time.LocalDate.now().toEpochDay()
        val ci = if (s.cycleSettings.enabled) com.yasnost.app.domain.CycleLogic.info(s, today) else null
        if (ci != null && ci.hasData && roomy) {
            val who = if (s.profile.female) "Мой цикл" else (s.cyclePartner()?.let { "Цикл · ${it.name}" } ?: "Цикл девушки")
            v.setViewVisibility(R.id.cycle, View.VISIBLE)
            v.setTextViewText(R.id.cycle, "🌸 $who: ${com.yasnost.app.ui.screens.cycleStatus(ci)}")
        } else v.setViewVisibility(R.id.cycle, View.GONE)
        if (tall) {
            val ctxA = com.yasnost.app.domain.AwardCtx(s, now)
            val got = com.yasnost.app.domain.Awards.unlocked(ctxA).size + s.habits.sumOf { h -> Achievements.list.count { now - h.startMillis >= it.millis } }
            v.setViewVisibility(R.id.extra, View.VISIBLE)
            v.setTextViewText(R.id.sx1, "${s.cravingsBeaten}")
            v.setTextViewText(R.id.sx2, "${s.checkIns.size}")
            v.setTextViewText(R.id.sx3, "$got 🏅")
        }
        if (roomy) {
            v.setViewVisibility(R.id.quote, View.VISIBLE)
            v.setTextViewText(R.id.quote,
                if (tall) "🔬 " + com.yasnost.app.domain.Facts.today(today, s.habits.map { it.type }.toSet(), s.profile.female).text
                else "«${com.yasnost.app.domain.Quotes.today(today)}»")
            v.setInt(R.id.quote, "setMaxLines", if (tall) 3 else if (ci?.hasData == true) 1 else 2)
        } else v.setViewVisibility(R.id.quote, View.GONE)
        v.setOnClickPendingIntent(R.id.root, openApp(ctx, id * 10 + 9, null))
        return v
    }

    /** Сводка для одной привычки: крупный «герой» на весь виджет. */
    private fun singleLayout(ctx: Context, id: Int, s: AppState, h: Habit, now: Long, tall: Boolean): RemoteViews {
        val v = RemoteViews(ctx.packageName, R.layout.widget_summary_single)
        val st = h.stats(now)
        val cur = s.settings.currency
        v.setInt(R.id.root, "setBackgroundResource", bgFor(h.type))
        v.setTextViewText(R.id.total, "${Fmt.money(st.money)} $cur")
        val lvl = Levels.of(s, now)
        v.setTextViewText(R.id.level, "Ур. ${lvl.n} · ${lvl.title}")
        v.setInt(R.id.badge, "setBackgroundResource", badgeFor(h.type))
        v.setImageViewResource(R.id.icon, iconFor(h.type))
        v.setTextViewText(R.id.days, Fmt.intNum(st.days))
        v.setTextColor(R.id.days, colorB(h.type))
        v.setTextViewText(R.id.daysLabel, Fmt.days(st.days))
        v.setTextColor(R.id.daysLabel, colorB(h.type))
        val since = java.time.format.DateTimeFormatter.ofPattern("d MMMM", java.util.Locale.forLanguageTag("ru"))
            .format(java.time.Instant.ofEpochMilli(h.startMillis).atZone(java.time.ZoneId.systemDefault()))
        v.setTextViewText(R.id.title, "${h.type.verb.lowercase()} · с $since")
        v.setChronometer(R.id.timer, SystemClock.elapsedRealtime() - (st.elapsed % DAY), null, true)
        v.setTextViewText(R.id.timerLabel, "Ч · МИН · СЕК")
        val p = Achievements.progress(st.elapsed)
        v.setImageViewBitmap(R.id.bar, bar(p, h.type))
        val next = Achievements.next(st.elapsed)
        v.setTextViewText(R.id.next, if (next != null) "до «${next.title}» · ${Fmt.duration(next.millis - st.elapsed)}" else "все награды за время получены")
        v.setTextViewText(R.id.percent, "${(p * 100).toInt()}%")
        v.setTextColor(R.id.percent, colorB(h.type))

        val today = java.time.LocalDate.now().toEpochDay()
        val ci = if (s.cycleSettings.enabled) com.yasnost.app.domain.CycleLogic.info(s, today) else null
        val hasCycle = ci != null && ci.hasData
        if (hasCycle) {
            val who = if (s.profile.female) "Мой цикл" else (s.cyclePartner()?.let { "Цикл · ${it.name}" } ?: "Цикл девушки")
            v.setViewVisibility(R.id.cycle, View.VISIBLE)
            v.setTextViewText(R.id.cycle, "🌸 $who: ${com.yasnost.app.ui.screens.cycleStatus(ci!!)}")
        }
        if (tall) {
            v.setViewVisibility(R.id.extra, View.VISIBLE)
            v.setTextViewText(R.id.sx1, "${Fmt.money(st.money)} $cur")
            if (h.unitsPerDay > 0) {
                v.setTextViewText(R.id.sx2Label, h.type.unitsLabel.uppercase())
                v.setTextViewText(R.id.sx2, "${Fmt.num(st.units)} ${h.unitName}")
            } else {
                v.setTextViewText(R.id.sx2Label, "ТЯГ ПОБЕЖДЕНО")
                v.setTextViewText(R.id.sx2, "${s.cravingsBeaten}")
            }
            val best = s.bestStreak(h.id, now) / DAY
            v.setTextViewText(R.id.sx3, "$best ${Fmt.days(best)}")
            v.setViewVisibility(R.id.quote, View.VISIBLE)
            v.setTextViewText(R.id.quote, "🔬 " + com.yasnost.app.domain.Facts.today(today, setOf(h.type), s.profile.female).text)
            v.setInt(R.id.quote, "setMaxLines", 3)
        } else if (!hasCycle) {
            v.setViewVisibility(R.id.quote, View.VISIBLE)
            v.setTextViewText(R.id.quote, "«${com.yasnost.app.domain.Quotes.today(today)}»")
        }
        v.setOnClickPendingIntent(R.id.root, openApp(ctx, id * 10 + 9, h.id))
        return v
    }

    // ---------- Цикл (когда привычки не отслеживаются) ----------

    private fun cycleOrEmpty(ctx: Context, id: Int, s: AppState): RemoteViews {
        if (!s.cycleSettings.enabled) return emptyViews(ctx)
        val ci = com.yasnost.app.domain.CycleLogic.info(s, java.time.LocalDate.now().toEpochDay())
        if (!ci.hasData) return emptyViews(ctx)
        val compact = cycleLayout(ctx, id, s, ci, false)
        val tall = cycleLayout(ctx, id, s, ci, true)
        return if (Build.VERSION.SDK_INT >= 31) RemoteViews(mapOf(SizeF(100f, 100f) to compact, SizeF(200f, 230f) to tall)) else compact
    }

    private fun cycleRight(ci: com.yasnost.app.domain.CycleInfo): Pair<String, String> {
        val t = ci.today
        val ns = ci.nextStart!!
        return when {
            ci.inPeriodNow -> "день ${t - ci.lastStart!! + 1}" to "МЕСЯЧНЫЕ"
            ci.lateDays > 0 -> "+${ci.lateDays} дн" to "ЗАДЕРЖКА"
            else -> "${ns - t} дн" to "ДО МЕСЯЧНЫХ"
        }
    }

    private fun cycleLayout(ctx: Context, id: Int, s: AppState, ci: com.yasnost.app.domain.CycleInfo, tall: Boolean): RemoteViews {
        val v = RemoteViews(ctx.packageName, R.layout.widget_summary_single)
        val t = ci.today
        val ru = java.util.Locale.forLanguageTag("ru")
        val df = java.time.format.DateTimeFormatter.ofPattern("d MMMM", ru)
        fun d(day: Long) = df.format(java.time.LocalDate.ofEpochDay(day))
        v.setInt(R.id.root, "setBackgroundResource", R.drawable.widget_bg_cycle)
        v.setInt(R.id.badge, "setBackgroundResource", R.drawable.badge_cycle)
        v.setImageViewResource(R.id.icon, R.drawable.ic_drop)
        val lvl = Levels.of(s, System.currentTimeMillis())
        v.setTextViewText(R.id.level, "Ур. ${lvl.n} · ${lvl.title}")
        val who = if (s.profile.female) "МОЙ ЦИКЛ" else (s.cyclePartner()?.let { "ЦИКЛ · ${it.name.uppercase()}" } ?: "ЦИКЛ ДЕВУШКИ")
        v.setTextViewText(R.id.total, who)
        v.setTextColor(R.id.total, 0xFFFF7AB6.toInt())
        v.setTextViewTextSize(R.id.total, android.util.TypedValue.COMPLEX_UNIT_SP, 13f)
        val doc = ci.dayOfCycle ?: 1
        v.setTextViewText(R.id.days, "$doc")
        v.setTextColor(R.id.days, 0xFFFF7AB6.toInt())
        v.setTextViewText(R.id.daysLabel, "день цикла")
        v.setTextColor(R.id.daysLabel, 0xFFFF7AB6.toInt())
        v.setTextViewText(R.id.title, ci.phase()?.title?.lowercase() ?: "")
        v.setViewVisibility(R.id.timer, View.GONE)
        val (big, small) = cycleRight(ci)
        v.setViewVisibility(R.id.rightBig, View.VISIBLE)
        v.setTextViewText(R.id.rightBig, big)
        v.setTextViewText(R.id.timerLabel, small)
        v.setImageViewBitmap(R.id.bar, bar((doc.toFloat() / ci.avgCycle).coerceIn(0f, 1f), null))
        val ns = ci.nextStart!!
        v.setTextViewText(R.id.next, if (ci.ovulation != null && ci.ovulation!! >= t) "овуляция ≈ ${d(ci.ovulation!!)} · месячные ≈ ${d(ns)}" else "месячные ≈ ${d(ns)}")
        v.setTextViewText(R.id.percent, "$doc/${ci.avgCycle}")
        v.setTextColor(R.id.percent, 0xFFFF7AB6.toInt())
        if (tall) {
            v.setViewVisibility(R.id.extra, View.VISIBLE)
            v.setTextViewText(R.id.sx1Label, "СРЕДНИЙ ЦИКЛ"); v.setTextViewText(R.id.sx1, "${ci.avgCycle} дн")
            v.setTextViewText(R.id.sx2Label, "МЕСЯЧНЫЕ"); v.setTextViewText(R.id.sx2, "${ci.avgPeriod} дн")
            v.setTextViewText(R.id.sx3Label, "ПМС С"); v.setTextViewText(R.id.sx3, d(ns - 5))
        }
        val phase = ci.phase()
        v.setViewVisibility(R.id.quote, View.VISIBLE)
        if (phase != null) {
            val pt = com.yasnost.app.domain.CycleLogic.phaseText(phase)
            val tips = if (s.profile.female) pt.tips else pt.partner
            v.setTextViewText(R.id.quote, "💡 " + tips[(t % tips.size).toInt()])
            v.setInt(R.id.quote, "setMaxLines", if (tall) 3 else 2)
        }
        v.setOnClickPendingIntent(R.id.root, openApp(ctx, id * 10 + 8, null))
        return v
    }

    private fun cycleMini(ctx: Context, id: Int, s: AppState): RemoteViews? {
        if (!s.cycleSettings.enabled) return null
        val ci = com.yasnost.app.domain.CycleLogic.info(s, java.time.LocalDate.now().toEpochDay())
        if (!ci.hasData) return null
        val v = RemoteViews(ctx.packageName, R.layout.widget_mini)
        v.setInt(R.id.root, "setBackgroundResource", R.drawable.mini_bg_cycle)
        val t = ci.today
        when {
            ci.inPeriodNow -> { v.setTextViewText(R.id.days, "${t - ci.lastStart!! + 1}"); v.setTextViewText(R.id.label, "день мес.") }
            ci.lateDays > 0 -> { v.setTextViewText(R.id.days, "+${ci.lateDays}"); v.setTextViewText(R.id.label, "задержка") }
            else -> { v.setTextViewText(R.id.days, "${ci.nextStart!! - t}"); v.setTextViewText(R.id.label, "до мес.") }
        }
        v.setOnClickPendingIntent(R.id.root, openApp(ctx, id, null))
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

    /** Полоска прогресса (растягивается по ширине). */
    private fun bar(progress: Float, t: HabitType?): Bitmap {
        val w = 600; val h = 14
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val r = h / 2f
        c.drawRoundRect(RectF(0f, 0f, w.toFloat(), h.toFloat()), r, r, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x1FFFFFFF })
        val pw = (w * progress.coerceIn(0.03f, 1f))
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = android.graphics.LinearGradient(0f, 0f, pw, 0f, t?.let { colorA(it) } ?: 0xFFFF4D6D.toInt(), t?.let { colorB(it) } ?: 0xFFFF7AB6.toInt(), Shader.TileMode.CLAMP)
        }
        c.drawRoundRect(RectF(0f, 0f, pw, h.toFloat()), r, r, p)
        return bmp
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
