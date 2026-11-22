package com.yasnost.app.domain

import com.yasnost.app.data.Habit
import com.yasnost.app.data.HabitType
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.roundToLong

const val MIN = 60_000L
const val HOUR = 60 * MIN
const val DAY = 24 * HOUR

data class HealthStep(val afterMillis: Long, val title: String, val text: String)

object Health {
    val smoking = listOf(
        HealthStep(20 * MIN, "20 минут", "Пульс и давление начинают возвращаться к норме."),
        HealthStep(8 * HOUR, "8 часов", "Угарного газа в крови вдвое меньше, кислород приходит в норму."),
        HealthStep(1 * DAY, "1 день", "Угарный газ выведен. Сердцу работать легче."),
        HealthStep(2 * DAY, "2 дня", "Никотин выведен. Начинают возвращаться вкус и обоняние."),
        HealthStep(3 * DAY, "3 дня", "Бронхи расслабляются — дышать заметно легче. Пик ломки позади."),
        HealthStep(14 * DAY, "2 недели", "Кровообращение улучшается, ходить и подниматься проще."),
        HealthStep(30 * DAY, "1 месяц", "Меньше кашля и одышки, кожа выглядит свежее."),
        HealthStep(90 * DAY, "3 месяца", "Объём лёгких растёт, выносливость заметно выше."),
        HealthStep(270 * DAY, "9 месяцев", "Реснички в лёгких восстановились и чистят дыхательные пути."),
        HealthStep(365 * DAY, "1 год", "Риск ишемической болезни сердца вдвое ниже, чем у курящего."),
        HealthStep(5 * 365 * DAY, "5 лет", "Риск инсульта приближается к уровню некурящего."),
        HealthStep(10 * 365 * DAY, "10 лет", "Риск рака лёгких примерно вдвое ниже."),
        HealthStep(15 * 365 * DAY, "15 лет", "Риск болезней сердца — как у человека, который никогда не курил."),
    )

    val alcohol = listOf(
        HealthStep(12 * HOUR, "12 часов", "Уровень сахара в крови выравнивается."),
        HealthStep(1 * DAY, "1 день", "Алкоголь выведен из организма."),
        HealthStep(3 * DAY, "3 дня", "Самое тяжёлое позади, нервная система успокаивается."),
        HealthStep(7 * DAY, "1 неделя", "Сон глубже и полноценнее, кожа лучше увлажнена."),
        HealthStep(14 * DAY, "2 недели", "Восстанавливается слизистая желудка, снижается давление."),
        HealthStep(30 * DAY, "1 месяц", "Жира в печени становится меньше, ясность мышления растёт."),
        HealthStep(90 * DAY, "3 месяца", "Кровь обновилась, больше энергии, крепче иммунитет."),
        HealthStep(180 * DAY, "6 месяцев", "Печень в значительной мере восстановилась, вес стабилизируется."),
        HealthStep(365 * DAY, "1 год", "Ниже риск болезней сердца, печени и ряда видов рака."),
        HealthStep(5 * 365 * DAY, "5 лет", "Риск многих связанных с алкоголем болезней заметно снижен."),
    )

    val custom = listOf(
        HealthStep(1 * DAY, "1 день", "Первые сутки — самые важные. Ты начал."),
        HealthStep(7 * DAY, "1 неделя", "Мозг начинает перестраивать привычные связи."),
        HealthStep(21 * DAY, "3 недели", "Новая привычка закрепляется."),
        HealthStep(66 * DAY, "66 дней", "В среднем столько нужно, чтобы поведение стало автоматическим."),
        HealthStep(365 * DAY, "1 год", "Целый год свободы."),
    )

    fun forType(t: HabitType) = when (t) {
        HabitType.SMOKING -> smoking
        HabitType.ALCOHOL -> alcohol
        HabitType.CUSTOM -> custom
    }
}

data class Achievement(val millis: Long, val title: String, val short: String)

object Achievements {
    val list = listOf(
        Achievement(1 * DAY, "Первый шаг", "1 д"),
        Achievement(3 * DAY, "Три дня", "3 д"),
        Achievement(7 * DAY, "Неделя силы", "7 д"),
        Achievement(14 * DAY, "Две недели", "14 д"),
        Achievement(30 * DAY, "Месяц свободы", "30 д"),
        Achievement(60 * DAY, "Два месяца", "60 д"),
        Achievement(100 * DAY, "Сотня", "100 д"),
        Achievement(180 * DAY, "Полгода", "½ г"),
        Achievement(365 * DAY, "Год ясности", "1 г"),
        Achievement(500 * DAY, "Пятьсот", "500"),
        Achievement(730 * DAY, "Два года", "2 г"),
        Achievement(1000 * DAY, "Тысяча дней", "1000"),
        Achievement(1825 * DAY, "Пять лет", "5 л"),
    )

    fun next(elapsed: Long) = list.firstOrNull { it.millis > elapsed }
    fun last(elapsed: Long) = list.lastOrNull { it.millis <= elapsed }

    /** Прогресс 0..1 от предыдущей вехи к следующей. */
    fun progress(elapsed: Long): Float {
        val n = next(elapsed) ?: return 1f
        val prev = last(elapsed)?.millis ?: 0L
        return ((elapsed - prev).toFloat() / (n.millis - prev)).coerceIn(0f, 1f)
    }
}

object Fmt {
    private val sym = DecimalFormatSymbols(Locale("ru")).apply { groupingSeparator = ' '; decimalSeparator = ',' }
    private val int = DecimalFormat("#,##0", sym)
    private val one = DecimalFormat("#,##0.#", sym)
    private val two = DecimalFormat("#,##0.##", sym)

    fun money(v: Double) = int.format(v.roundToLong())
    fun num(v: Double) = if (v >= 100) int.format(v) else if (v >= 10) one.format(v) else two.format(v)
    fun intNum(v: Long) = int.format(v)

    fun plural(n: Long, one: String, few: String, many: String): String {
        val a = n % 100
        val b = n % 10
        return when {
            a in 11..14 -> many
            b == 1L -> one
            b in 2..4 -> few
            else -> many
        }
    }

    fun days(n: Long) = plural(n, "день", "дня", "дней")
    fun hours(n: Long) = plural(n, "час", "часа", "часов")

    fun hms(ms: Long): String {
        val s = (ms / 1000) % 60
        val m = (ms / MIN) % 60
        val h = (ms / HOUR) % 24
        return "%02d:%02d:%02d".format(h, m, s)
    }

    /** «3 г 2 мес», «12 дн 4 ч», «5 ч 12 мин» */
    fun duration(ms: Long): String {
        val d = ms / DAY
        val h = (ms / HOUR) % 24
        val m = (ms / MIN) % 60
        return when {
            d >= 365 -> "${d / 365} г ${(d % 365) / 30} мес"
            d >= 1 -> "$d дн $h ч"
            h >= 1 -> "$h ч $m мин"
            else -> "$m мин"
        }
    }

    fun lifeMinutes(min: Long): String {
        val d = min / (60 * 24)
        val h = (min / 60) % 24
        return when {
            d > 0 -> "$d дн $h ч"
            h > 0 -> "$h ч ${min % 60} мин"
            else -> "$min мин"
        }
    }
}

data class Stats(
    val elapsed: Long,
    val days: Long,
    val money: Double,
    val units: Double,
    /** Для курения: минуты жизни, ~11 минут на сигарету. */
    val lifeMinutes: Long,
)

fun Habit.stats(now: Long): Stats {
    val e = (now - startMillis).coerceAtLeast(0)
    val d = e.toDouble() / DAY
    val units = unitsPerDay * d
    return Stats(
        elapsed = e,
        days = e / DAY,
        money = costPerDay * d,
        units = units,
        lifeMinutes = if (type == HabitType.SMOKING) (units * 11).toLong() else 0L,
    )
}

val HabitType.verb: String get() = when (this) {
    HabitType.ALCOHOL -> "Не пью"
    HabitType.SMOKING -> "Не курю"
    HabitType.CUSTOM -> "Свободен"
}

val HabitType.unitsLabel: String get() = when (this) {
    HabitType.ALCOHOL -> "не выпито"
    HabitType.SMOKING -> "не выкурено"
    HabitType.CUSTOM -> "не потрачено"
}

object Quotes {
    val list = listOf(
        "Каждый трезвый день — это кирпич в доме, который ты строишь для себя.",
        "Тяга длится минуты. Свобода — годы.",
        "Ты не отказываешься от чего-то. Ты возвращаешь себе себя.",
        "Не нужно бросать навсегда. Нужно не начать сегодня.",
        "Сила — это не отсутствие желания, а выбор, который ты делаешь снова и снова.",
        "Голова ясная, утро доброе, деньги целы.",
        "Ты уже прошёл самое трудное — начало.",
        "Маленькие шаги каждый день дают большие перемены.",
        "Завтрашний ты скажет спасибо сегодняшнему.",
        "Дыши. Это пройдёт. Ты останешься.",
    )
    fun today(epochDay: Long) = list[(epochDay % list.size).toInt()]
}
