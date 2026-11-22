package com.yasnost.app.work

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.yasnost.app.MainActivity
import com.yasnost.app.R
import com.yasnost.app.data.Repository
import com.yasnost.app.domain.Achievements
import com.yasnost.app.domain.DAY
import com.yasnost.app.domain.Health
import com.yasnost.app.domain.verb
import com.yasnost.app.widget.WidgetUpdater
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.util.concurrent.TimeUnit

object Ticker {
    fun schedule(ctx: Context) {
        val req = PeriodicWorkRequestBuilder<TickWorker>(15, TimeUnit.MINUTES).build()
        WorkManager.getInstance(ctx).enqueueUniquePeriodicWork("tick", ExistingPeriodicWorkPolicy.KEEP, req)
    }

    /** Обновить виджеты ровно в момент смены суток счётчика. */
    suspend fun scheduleBoundary(ctx: Context) {
        val s = Repository.get(ctx).current()
        if (s.habits.isEmpty()) return
        val now = System.currentTimeMillis()
        val delay = s.habits.minOf { DAY - ((now - it.startMillis).coerceAtLeast(0) % DAY) } + 3_000
        val req = OneTimeWorkRequestBuilder<BoundaryWorker>().setInitialDelay(delay, TimeUnit.MILLISECONDS).build()
        WorkManager.getInstance(ctx).enqueueUniqueWork("boundary", ExistingWorkPolicy.REPLACE, req)
    }
}

class BoundaryWorker(ctx: Context, p: WorkerParameters) : CoroutineWorker(ctx, p) {
    override suspend fun doWork(): Result {
        val m = android.appwidget.AppWidgetManager.getInstance(applicationContext)
        WidgetUpdater.renderCounters(applicationContext, m.getAppWidgetIds(android.content.ComponentName(applicationContext, com.yasnost.app.widget.CounterWidget::class.java)))
        WidgetUpdater.renderSummaries(applicationContext, m.getAppWidgetIds(android.content.ComponentName(applicationContext, com.yasnost.app.widget.SummaryWidget::class.java)))
        WidgetUpdater.renderMinis(applicationContext, m.getAppWidgetIds(android.content.ComponentName(applicationContext, com.yasnost.app.widget.MiniWidget::class.java)))
        val c = applicationContext
        WidgetUpdater.renderIcons(c, m.getAppWidgetIds(android.content.ComponentName(c, com.yasnost.app.widget.MiniAlcoholWidget::class.java)), "alcohol")
        WidgetUpdater.renderIcons(c, m.getAppWidgetIds(android.content.ComponentName(c, com.yasnost.app.widget.MiniSmokingWidget::class.java)), "smoking")
        WidgetUpdater.renderIcons(c, m.getAppWidgetIds(android.content.ComponentName(c, com.yasnost.app.widget.MiniCycleWidget::class.java)), "cycle")
        return Result.success()
    }
}

class TickWorker(ctx: Context, p: WorkerParameters) : CoroutineWorker(ctx, p) {
    override suspend fun doWork(): Result {
        val ctx = applicationContext
        WidgetUpdater.updateAll(ctx)
        Notifier.ensureChannels(ctx)
        val repo = Repository.get(ctx)
        val s = repo.current()
        val now = System.currentTimeMillis()

        // Вехи
        val notified = s.notified.toMutableMap()
        var changed = false
        s.habits.forEach { h ->
            val elapsed = now - h.startMillis
            val events = Health.forType(h.type, s.profile.female).map { it.afterMillis to "${h.title}: ${it.title}" to it.text } +
                Achievements.list.map { it.millis to "Награда «${it.title}»" to "${h.type.verb} уже ${it.short.replace(" д", " дн.")}. Так держать!" }
            val passed = events.filter { it.first.first <= elapsed }.maxByOrNull { it.first.first }
            val last = passed?.first?.first ?: 0L
            val prev = notified[h.id]
            if (prev == null) {
                notified[h.id] = last; changed = true
            } else if (last > prev) {
                notified[h.id] = last; changed = true
                if (s.settings.milestoneNotifications && passed != null) {
                    Notifier.show(ctx, h.id.hashCode(), passed.first.second, passed.second, Notifier.CH_MILESTONES, h.id)
                }
            }
        }

        // Особые награды
        val got = com.yasnost.app.domain.Awards.unlocked(com.yasnost.app.domain.AwardCtx(s, now))
        var unlocked = s.unlocked
        var init = s.unlockedInit
        val fresh = got.filter { it.id !in s.unlocked }
        if (fresh.isNotEmpty() || !init) {
            if (init && s.settings.milestoneNotifications) {
                fresh.take(3).forEach { a ->
                    Notifier.show(ctx, a.id.hashCode(), "${a.emoji} Новая награда: «${a.title}»", a.desc + ". Загляни в раздел «Награды».", Notifier.CH_MILESTONES, null)
                }
            }
            unlocked = (s.unlocked + got.map { it.id }).distinct()
            init = true
            changed = true
        }

        // Цикл
        var cycleKeys = s.cycleNotified
        val hour = LocalTime.now().hour
        if (s.cycleSettings.enabled && hour in 9..21) {
            val today = LocalDate.now().toEpochDay()
            val female = s.profile.female
            val owners: List<Pair<String, String>> = if (female) listOf("" to "")
                else s.partners.map { com.yasnost.app.data.ownerKey(it.id) to it.name }.ifEmpty { listOf("" to "Девушка") }
            for ((owner, her) in owners) {
            val ci = com.yasnost.app.domain.CycleLogic.info(s, today, owner)
            val ns = ci.nextStart
            val ok = if (owner.isEmpty()) "" else "${owner}_"
            fun once(key: String, title: String, text: String) {
                if (key !in cycleKeys) {
                    Notifier.show(ctx, key.hashCode(), title, text, Notifier.CH_REMINDERS, null)
                    cycleKeys = cycleKeys + key
                    changed = true
                }
            }
            if (ns != null && !ci.inPeriodNow) {
                val left = ns - today
                val rd = s.cycleSettings.remindDaysBefore
                if (rd > 0 && left in 1..rd) {
                    val dn = "$left ${com.yasnost.app.domain.Fmt.days(left)}"
                    if (female) once("pre_$ok$ns", "Через $dn ожидаются месячные", "Положи в сумку средства гигиены и обезболивающее. НПВП лучше начать при первых признаках.")
                    else once("pre_$ok$ns", "$her: через $dn могут начаться месячные", "Будь внимательнее и терпеливее — и запасись её любимым шоколадом 🍫")
                }
                if (ci.lateDays >= 3) {
                    once("late_$ok$ns", if (female) "Задержка ${ci.lateDays} ${com.yasnost.app.domain.Fmt.days(ci.lateDays.toLong())}" else "$her: задержка ${ci.lateDays} ${com.yasnost.app.domain.Fmt.days(ci.lateDays.toLong())}",
                        "Частые причины — стресс, болезнь, перелёт. Если был незащищённый контакт — стоит сделать тест на беременность.")
                }
                if (s.cycleSettings.fertileReminder && ci.fertile?.first == today) {
                    once("fert_$ok$ns", if (female) "Начинаются фертильные дни" else "$her: начинаются фертильные дни", "Ближайшие ~7 дней шанс беременности самый высокий. Календарь — не контрацепция.")
                }
            }
            }
        }

        // Девушкам: мягкие напоминания о паре
        if (s.profile.female && s.settings.partnerTips && s.settings.intimacyEnabled && hour in 18..21) {
            val today = LocalDate.now().toEpochDay()
            val last = s.intimacy.filter { it.sex != com.yasnost.app.data.SexStatus.NONE }.maxOfOrNull { it.day }
            val gap = last?.let { today - it }
            if (gap != null && gap >= 10 && "gap_$last" !in cycleKeys) {
                Notifier.show(ctx, 778, "Время друг для друга 🤍",
                    "Близости не было $gap ${com.yasnost.app.domain.Fmt.days(gap)}. Не обязательно секс — объятия, свидание или разговор «как ты?» тоже сближают.",
                    Notifier.CH_REMINDERS, null)
                cycleKeys = cycleKeys + "gap_$last"; changed = true
            }
            val dow = LocalDate.now().dayOfWeek.value
            val week = today / 7
            if (dow == 7 && "tip_$week" !in cycleKeys) {
                val c = com.yasnost.app.domain.PartnerInsights.today(today)
                Notifier.show(ctx, 779, "${c.emoji} ${c.title}", c.text, Notifier.CH_REMINDERS, null)
                cycleKeys = cycleKeys + "tip_$week"; changed = true
            }
        }

        // Вечернее напоминание
        val today = LocalDate.now().toEpochDay()
        var remindDay = s.lastReminderDay
        if (s.settings.reminderEnabled && s.habits.isNotEmpty() && s.checkIn(today) == null &&
            LocalTime.now().hour >= s.settings.reminderHour && s.lastReminderDay != today
        ) {
            Notifier.show(ctx, 777, "Как прошёл день?", "Отметь настроение и тягу — это займёт 10 секунд.", Notifier.CH_REMINDERS, null)
            remindDay = today; changed = true
        }
        if (changed) repo.update(refreshWidgets = false) {
            it.copy(notified = it.notified + notified, lastReminderDay = remindDay, unlocked = (it.unlocked + unlocked).distinct(), unlockedInit = init, cycleNotified = (it.cycleNotified + cycleKeys).distinct().takeLast(60))
        }
        return Result.success()
    }
}

object Notifier {
    const val CH_MILESTONES = "milestones"
    const val CH_REMINDERS = "reminders"

    fun ensureChannels(ctx: Context) {
        val nm = ctx.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CH_MILESTONES, "Этапы и награды", NotificationManager.IMPORTANCE_DEFAULT))
        nm.createNotificationChannel(NotificationChannel(CH_REMINDERS, "Вечерняя отметка", NotificationManager.IMPORTANCE_DEFAULT))
    }

    fun show(ctx: Context, id: Int, title: String, text: String, channel: String, habitId: String?) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        val intent = Intent(ctx, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            if (habitId != null) putExtra(MainActivity.EXTRA_HABIT, habitId)
        }
        val pi = PendingIntent.getActivity(ctx, id, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val n = NotificationCompat.Builder(ctx, channel)
            .setSmallIcon(R.drawable.ic_spark)
            .setColor(0xFF8B7CFF.toInt())
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(pi)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(ctx).notify(id, n)
    }
}

/** После перезагрузки и смены времени пересчитываем таймеры виджетов. */
class TimeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                Ticker.schedule(context)
                WidgetUpdater.updateAll(context)
            } finally { pending.finish() }
        }
    }
}
