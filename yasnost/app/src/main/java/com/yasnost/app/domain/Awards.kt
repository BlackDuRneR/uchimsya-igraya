package com.yasnost.app.domain

import com.yasnost.app.data.AppState
import com.yasnost.app.data.SexStatus
import java.time.LocalDate

enum class AwardCat(val title: String) {
    MONEY("Деньги"), DIARY("Дневник"), SOS("Тяга"), HEALTH("Здоровье"),
    COMEBACK("Возвращение"), COMBO("Комбо"), INTIMACY("Близость"), CYCLE("Цикл"), DREAMS("Мечты"), MIND("Опора")
}

/** Общее достижение: прогресс value/target. */
data class Award(val id: String, val cat: AwardCat, val emoji: String, val title: String, val desc: String, val target: Double, val value: (AwardCtx) -> Double)

class AwardCtx(val s: AppState, val now: Long) {
    val today = LocalDate.now().toEpochDay()
    val moneyKzt by lazy { s.habits.sumOf { Rates.toKzt(it.stats(now).money, s.settings.currency) } }
    val checkCount get() = s.checkIns.size.toDouble()
    val checkStreakBest by lazy { bestRun(s.checkIns.map { it.day }) }
    val healthSteps by lazy { s.habits.sumOf { h -> Health.forType(h.type).count { now - h.startMillis >= it.afterMillis } }.toDouble() }
    fun comeback(days: Int) = if (s.habits.any { h -> s.relapses.any { it.habitId == h.id } && now - h.startMillis >= days * DAY }) 1.0 else 0.0
    fun combo(days: Int) = s.habits.count { now - it.startMillis >= days * DAY }.toDouble()
    val intimacyDays get() = s.intimacy.size.toDouble()
    val protectedCount get() = s.intimacy.filter { it.sex == SexStatus.PROTECTED }.sumOf { it.sexCount }.toDouble()
    val periods by lazy { CycleLogic.info(s, today).periods.size.toDouble() }
    val goalsDone by lazy { Money.goals(s, now).count { it.done }.toDouble() }

    private fun bestRun(days: List<Long>): Double {
        val set = days.toSortedSet()
        var best = 0; var cur = 0; var prev: Long? = null
        for (d in set) { cur = if (prev != null && d == prev + 1) cur + 1 else 1; best = maxOf(best, cur); prev = d }
        return best.toDouble()
    }
}

object Awards {
    val list: List<Award> = buildList {
        listOf(
            Triple(10_000.0, "Первые сбережения", "💵"), Triple(50_000.0, "Копилка", "🐷"), Triple(100_000.0, "Сто тысяч", "💰"),
            Triple(250_000.0, "Четверть миллиона", "💎"), Triple(500_000.0, "Полмиллиона", "🏦"), Triple(1_000_000.0, "Миллион свободы", "👑"),
            Triple(2_000_000.0, "Два миллиона", "🚀"),
        ).forEach { (t, title, e) -> add(Award("money_${t.toLong()}", AwardCat.MONEY, e, title, "Сэкономить ${Fmt.money(t)} ₸", t) { it.moneyKzt }) }

        listOf(1 to "Первая запись", 7 to "Неделя записей", 30 to "Летописец", 100 to "Сотня записей", 365 to "Год в дневнике").forEach { (n, t) ->
            add(Award("checks_$n", AwardCat.DIARY, "📝", t, "Сделать $n ${Fmt.plural(n.toLong(), "отметку", "отметки", "отметок")}", n.toDouble()) { it.checkCount })
        }
        listOf(7 to "Серия 7", 30 to "Серия 30", 100 to "Серия 100").forEach { (n, t) ->
            add(Award("streak_$n", AwardCat.DIARY, "🔥", t, "Отмечаться $n ${Fmt.days(n.toLong())} подряд", n.toDouble()) { it.checkStreakBest })
        }

        listOf(1 to "Устоял", 10 to "Сёрфер волн", 25 to "Стальные нервы", 50 to "Несгибаемый", 100 to "Мастер тяги").forEach { (n, t) ->
            add(Award("sos_$n", AwardCat.SOS, "🌊", t, "Пережить тягу через SOS $n ${Fmt.plural(n.toLong(), "раз", "раза", "раз")}", n.toDouble()) { it.s.cravingsBeaten.toDouble() })
        }

        listOf(3 to "Перезагрузка", 10 to "Обновление", 20 to "Новое тело").forEach { (n, t) ->
            add(Award("health_$n", AwardCat.HEALTH, "❤️", t, "Пройти $n ${Fmt.plural(n.toLong(), "этап", "этапа", "этапов")} восстановления", n.toDouble()) { it.healthSteps })
        }

        add(Award("phoenix", AwardCat.COMEBACK, "🐦‍🔥", "Феникс", "После срыва продержаться 7 дней", 1.0) { it.comeback(7) })
        add(Award("second_wind", AwardCat.COMEBACK, "🌬️", "Второе дыхание", "После срыва продержаться 30 дней", 1.0) { it.comeback(30) })
        add(Award("comeback_100", AwardCat.COMEBACK, "🏔️", "Вершина", "После срыва продержаться 100 дней", 1.0) { it.comeback(100) })

        add(Award("combo_7", AwardCat.COMBO, "⚡", "Двойной удар", "Две привычки — по 7 дней", 2.0) { it.combo(7) })
        add(Award("combo_30", AwardCat.COMBO, "🌗", "Двойная свобода", "Две привычки — по 30 дней", 2.0) { it.combo(30) })
        add(Award("combo_100", AwardCat.COMBO, "🌕", "Двойная сотня", "Две привычки — по 100 дней", 2.0) { it.combo(100) })
        add(Award("combo_365", AwardCat.COMBO, "🌌", "Двойной год", "Две привычки — по году", 2.0) { it.combo(365) })

        add(Award("int_7", AwardCat.INTIMACY, "💞", "Внимание к себе", "Заполнить календарь близости 7 дней", 7.0) { it.intimacyDays })
        add(Award("int_30", AwardCat.INTIMACY, "🧘", "Осознанность", "Заполнить календарь близости 30 дней", 30.0) { it.intimacyDays })
        add(Award("int_protect", AwardCat.INTIMACY, "🛡️", "Ответственный", "10 защищённых контактов", 10.0) { it.protectedCount })
        add(Award("int_test", AwardCat.INTIMACY, "🩺", "Проверился", "Отметить сданные анализы на ИППП", 1.0) { if (it.s.lastStiTestDay >= 0) 1.0 else 0.0 })

        add(Award("cycle_2", AwardCat.CYCLE, "🌸", "Первый цикл", "Отметить два периода подряд", 2.0) { it.periods })
        add(Award("cycle_4", AwardCat.CYCLE, "🌙", "Ритм", "Отметить 4 периода", 4.0) { it.periods })
        add(Award("cycle_7", AwardCat.CYCLE, "🔮", "Полгода наблюдений", "Отметить 7 периодов", 7.0) { it.periods })
        add(Award("cycle_log", AwardCat.CYCLE, "🗓️", "Внимание к деталям", "Записать самочувствие в 30 дней цикла", 30.0) { it.s.cycle.size.toDouble() })

        add(Award("goal_set", AwardCat.DREAMS, "🎯", "Есть цель", "Поставить цель в копилке мечты", 1.0) { if (it.s.goals.isNotEmpty()) 1.0 else 0.0 })
        add(Award("goal_1", AwardCat.DREAMS, "🎁", "Мечта сбылась", "Накопить на первую цель", 1.0) { it.goalsDone })
        add(Award("goal_3", AwardCat.DREAMS, "🌠", "Мечтатель-реалист", "Накопить на три цели", 3.0) { it.goalsDone })

        add(Award("reasons", AwardCat.MIND, "🧭", "Знаю зачем", "Записать 3 причины бросить", 3.0) { it.s.reasons.size.toDouble() })
        add(Award("letter", AwardCat.MIND, "✉️", "Письмо себе", "Написать письмо себе на трудный момент", 1.0) { if (it.s.letter.isNotBlank()) 1.0 else 0.0 })
    }

    fun unlocked(ctx: AwardCtx) = list.filter { it.value(ctx) >= it.target }
}

data class Level(val n: Int, val title: String, val xp: Long, val from: Long, val to: Long?) {
    val progress get() = if (to == null) 1f else ((xp - from).toFloat() / (to - from)).coerceIn(0f, 1f)
}

object Levels {
    private val names = listOf("Новичок", "Решившийся", "Упорный", "Боец", "Стойкий", "Воин", "Мастер", "Наставник", "Мудрец", "Легенда", "Ясный")
    private val thresholds = listOf(0L, 300, 800, 1500, 2500, 4000, 6000, 9000, 13000, 18000, 25000)

    fun xp(s: AppState, now: Long): Long {
        val ctx = AwardCtx(s, now)
        val global = Awards.unlocked(ctx).size * 100L
        val time = s.habits.sumOf { h -> Achievements.list.count { now - h.startMillis >= it.millis } } * 100L
        val days = s.habits.sumOf { it.stats(now).days } * 5L
        return global + time + days + s.checkIns.size * 10L + s.cravingsBeaten * 20L
    }

    fun of(s: AppState, now: Long): Level {
        val xp = xp(s, now)
        val i = thresholds.indexOfLast { xp >= it }
        return Level(i + 1, names[i], xp, thresholds[i], thresholds.getOrNull(i + 1))
    }
}
