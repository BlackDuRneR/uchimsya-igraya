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

data class HealthStep(
    val afterMillis: Long, val title: String, val text: String, val source: String = "",
    val femaleText: String? = null, val femaleSource: String? = null,
) {
    fun forFemale(f: Boolean) = if (f && femaleText != null) copy(text = femaleText, source = femaleSource ?: source) else this
}

private const val WHO = "ВОЗ, «Польза отказа от курения»"
private const val CDC = "CDC, Benefits of Quitting Smoking"
private const val ACS = "American Cancer Society"

object Health {
    val smoking = listOf(
        HealthStep(20 * MIN, "20 минут", "Пульс и давление снижаются.", WHO),
        HealthStep(12 * HOUR, "12 часов", "Уровень угарного газа в крови возвращается к норме — органы получают больше кислорода.", WHO),
        HealthStep(1 * DAY, "1 день", "Никотина в крови больше нет. У мужчин уже через сутки улучшаются ночные эрекции.", "$CDC; Guay et al., Endocr Pract 1998",
            "Никотина в крови больше нет. Сосуды начинают расслабляться, кровоснабжение органов улучшается.", CDC),
        HealthStep(2 * DAY, "2 дня", "Нервные окончания восстанавливаются — возвращаются вкус и обоняние.", ACS),
        HealthStep(3 * DAY, "3 дня", "Пик синдрома отмены позади: обычно хуже всего первые 3 дня. Отдельная волна тяги длится минуты.", "NCI, Nicotine withdrawal fact sheet; HSE"),
        HealthStep(14 * DAY, "2 недели", "Улучшаются кровообращение и функция лёгких (процесс идёт до 3 месяцев).", WHO),
        HealthStep(30 * DAY, "1 месяц", "Уменьшаются кашель и одышка (этот период длится до 9 месяцев).", WHO),
        HealthStep(66 * DAY, "66 дней", "В среднем столько нужно, чтобы новое поведение стало автоматическим (разброс — от 18 до 254 дней).", "Lally et al., Eur J Soc Psychol 2010"),
        HealthStep(90 * DAY, "3 месяца", "Эректильная функция у бросивших заметно лучше: средний балл IIEF вырос с 54,8 до 60,4.", "Sahin et al., 2020 (n=181)",
            "Кровообращение и дыхание заметно лучше. Курение мешает забеременеть и приближает менопаузу — отказ снижает эти риски.", "CDC, Cigarettes and Reproductive Health"),
        HealthStep(270 * DAY, "9 месяцев", "Реснички в лёгких восстанавливаются и снова очищают дыхательные пути, меньше инфекций.", ACS),
        HealthStep(365 * DAY, "1 год", "Риск ишемической болезни сердца примерно вдвое ниже, чем у курящего. У четверти бросивших с проблемами эрекции — улучшение.", "$WHO; Pourmand et al., BJU Int 2004",
            "Риск ишемической болезни сердца примерно вдвое ниже, чем у курящей.", WHO),
        HealthStep(5 * 365 * DAY, "5 лет", "Риск инсульта снижается до уровня некурящего (за 5–15 лет). Риск рака рта, горла и пищевода вдвое ниже.", "$WHO; $ACS"),
        HealthStep(10 * 365 * DAY, "10 лет", "Риск рака лёгких примерно вдвое ниже, чем у курящего.", WHO),
        HealthStep(15 * 365 * DAY, "15 лет", "Риск болезней сердца — как у человека, который никогда не курил.", WHO,
            "Риск болезней сердца — как у никогда не курившей. Дополнительный риск рака шейки матки снижается примерно вдвое.", "$WHO; CDC"),
    )

    val alcohol = listOf(
        HealthStep(12 * HOUR, "12 часов", "Если пил много и долго — могут появиться тревога, потливость, дрожь. Это отмена. При сильных симптомах, судорогах или галлюцинациях — сразу к врачу.", "NIAAA"),
        HealthStep(1 * DAY, "1 день", "Алкоголь выведен из организма, печень переключается на обычную работу.", "NIAAA"),
        HealthStep(3 * DAY, "3 дня", "Острая фаза отмены обычно позади: её пик приходится на 24–72 часа.", "NIAAA"),
        HealthStep(7 * DAY, "1 неделя", "Сон начинает выравниваться. Многие замечают разницу за 1–2 недели; после долгого стажа сну нужно больше времени.", "Обзор: Alcohol Res Health, PMC2778757"),
        HealthStep(30 * DAY, "1 месяц", "Исследование месячного отказа: инсулинорезистентность −26%, давление −6%, вес −1,5%, снизились факторы роста, связанные с раком.", "Mehta et al., BMJ Open 2018 (n=94)"),
        HealthStep(35 * DAY, "5 недель", "Жира в печени становится заметно меньше: в эксперименте UCL — минус 15% и более.", "UCL / New Scientist, 2013"),
        HealthStep(66 * DAY, "66 дней", "В среднем столько нужно, чтобы новая привычка стала автоматической (от 18 до 254 дней).", "Lally et al., Eur J Soc Psychol 2010"),
        HealthStep(90 * DAY, "3 месяца", "У 88,5% мужчин с проблемами эрекции на фоне алкоголя через 3 месяца трезвости наступило улучшение.", "Проспективное исследование, 2022 (n=104), PubMed 36606722",
            "Алкоголь повышает риск рака груди: каждые 10 г спирта в день (маленький бокал вина) — плюс 5–9%. Трезвость убирает этот вклад.", "Метаанализ McTiernan et al.; WCRF/AICR"),
        HealthStep(180 * DAY, "6 месяцев", "Уровень тестостерона постепенно восстанавливается — после тяжёлого употребления на это уходят месяцы.", "Клинические обзоры; Cleveland Clinic",
            "Гормональный фон выравнивается: алкоголь влияет на эстроген и регулярность цикла. Печень и кожа выглядят заметно лучше.", "NIAAA, обзоры"),
        HealthStep(365 * DAY, "1 год", "Алкоголь — доказанная причина как минимум 7 видов рака. Каждый трезвый год снижает накопленный риск.", "ВОЗ, IARC"),
        HealthStep(5 * 365 * DAY, "5 лет", "Риск рака рта, горла и пищевода продолжает снижаться и с годами приближается к уровню непьющих.", "IARC, обзоры"),
    )

    val custom = listOf(
        HealthStep(1 * DAY, "1 день", "Первые сутки — самые важные. Ты начал.", ""),
        HealthStep(7 * DAY, "1 неделя", "Мозг начинает ослаблять старые связи «сигнал → действие».", ""),
        HealthStep(30 * DAY, "1 месяц", "Пропуск одного дня почти не мешает формированию привычки — важна общая регулярность.", "Lally et al., 2010"),
        HealthStep(66 * DAY, "66 дней", "В среднем столько нужно, чтобы поведение стало автоматическим.", "Lally et al., Eur J Soc Psychol 2010"),
        HealthStep(365 * DAY, "1 год", "Целый год свободы.", ""),
    )

    fun forType(t: HabitType, female: Boolean = false) = when (t) {
        HabitType.SMOKING -> smoking
        HabitType.ALCOHOL -> alcohol
        HabitType.CUSTOM -> custom
    }.map { it.forFemale(female) }
}

data class Achievement(val millis: Long, val title: String, val short: String)

object Achievements {
    val list = listOf(
        Achievement(1 * DAY, "Первый шаг", "1 д"),
        Achievement(3 * DAY, "Три дня", "3 д"),
        Achievement(7 * DAY, "Неделя силы", "7 д"),
        Achievement(14 * DAY, "Две недели", "14 д"),
        Achievement(21 * DAY, "Три недели", "21 д"),
        Achievement(30 * DAY, "Месяц свободы", "30 д"),
        Achievement(45 * DAY, "Полтора месяца", "45 д"),
        Achievement(60 * DAY, "Два месяца", "60 д"),
        Achievement(66 * DAY, "Новый автомат", "66 д"),
        Achievement(90 * DAY, "Квартал", "90 д"),
        Achievement(100 * DAY, "Сотня", "100 д"),
        Achievement(180 * DAY, "Полгода", "½ г"),
        Achievement(270 * DAY, "Девять месяцев", "270"),
        Achievement(365 * DAY, "Год ясности", "1 г"),
        Achievement(500 * DAY, "Пятьсот", "500"),
        Achievement(730 * DAY, "Два года", "2 г"),
        Achievement(1000 * DAY, "Тысяча дней", "1000"),
        Achievement(1095 * DAY, "Три года", "3 г"),
        Achievement(1825 * DAY, "Пять лет", "5 л"),
        Achievement(3650 * DAY, "Десятилетие", "10 л"),
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

val TRIGGERS = listOf(
    "Стресс", "Компания", "Скука", "Усталость", "Праздник", "Конфликт",
    "Одиночество", "Тревога", "Злость", "Выходные", "После еды", "Кофе",
)

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
        "Каждый трезвый день — кирпич в доме, который ты строишь для себя.",
        "Тяга длится минуты. Свобода — годы.",
        "Ты не отказываешься от чего-то. Ты возвращаешь себе себя.",
        "Не нужно бросать навсегда. Нужно не начать сегодня.",
        "Сила — это не отсутствие желания, а выбор, который ты делаешь снова и снова.",
        "Голова ясная, утро доброе, деньги целы.",
        "Ты уже прошёл самое трудное — начало.",
        "Маленькие шаги каждый день дают большие перемены.",
        "Завтрашний ты скажет спасибо сегодняшнему.",
        "Дыши. Это пройдёт. Ты останешься.",
        "Срыв — это событие, а не приговор. Важно, что ты делаешь дальше.",
        "Тебе не нужно чувствовать себя готовым. Нужно просто не сдаваться ещё один час.",
        "Каждое «нет» тяге делает следующее «нет» легче.",
        "Свобода пахнет свежим воздухом и чистой одеждой.",
        "Мозг меняется от повторения. Сегодня ты снова его тренируешь.",
        "Ты не теряешь удовольствие. Ты учишься получать его по-настоящему.",
        "Хорошие дни бывают и без допинга. Сегодня — проверь.",
        "Ты сильнее любой привычки, потому что умеешь замечать её.",
        "Утро без похмелья — лучший подарок самому себе.",
        "Деньги, которые не ушли в дым, работают на твою мечту.",
        "Самое долгое путешествие начинается с одного трезвого дня.",
        "Желание — это волна. Ты — серфер, а не утопающий.",
        "Ясная голова принимает лучшие решения.",
        "Ты меняешь не только привычку. Ты меняешь историю своей жизни.",
        "Не сравнивай свой путь с чужим. Сравнивай себя с собой вчерашним.",
        "Каждый день без — это день, когда ты выбрал себя.",
        "Тело уже благодарит тебя. Прислушайся.",
        "Ты можешь всё. Даже скучать без этого.",
        "Не бывает слишком поздно стать здоровее.",
        "Гордись. Сегодня ты сделал то, что многим не по силам.",
    )
    fun today(epochDay: Long) = list[(epochDay % list.size).toInt()]
}

/** male: null — для всех, true — только для мужчин, false — только для женщин */
data class Fact(val text: String, val source: String, val type: HabitType?, val male: Boolean? = null) {
    fun fits(female: Boolean) = male == null || male == !female
}

/** Короткие научные факты для «Факта дня». */
object Facts {
    val list = listOf(
        Fact("Месяц без алкоголя у умеренно-сильно пьющих снизил инсулинорезистентность на 26%, давление — на 6%, вес — на 1,5%.", "Mehta et al., BMJ Open 2018", HabitType.ALCOHOL),
        Fact("У 88,5% мужчин с нарушением эрекции на фоне алкоголя через 3 месяца трезвости эрекция улучшилась.", "PubMed 36606722, 2022", HabitType.ALCOHOL, male = true),
        Fact("Каждые 10 г алкоголя в день повышают риск рака груди на 5–9%. Безопасной дозы для этого риска не найдено.", "McTiernan et al.; WCRF/AICR", HabitType.ALCOHOL, male = false),
        Fact("Курение затрудняет беременность и может приблизить менопаузу на 1–2 года. Отказ — лучший подарок будущему здоровью.", "CDC, Reproductive Health", HabitType.SMOKING, male = false),
        Fact("Регулярные упражнения могут заметно снизить менструальную боль — в обзоре Cochrane эффект был большим.", "Armour et al., Cochrane 2019", null, male = false),
        Fact("Кофеин, соль и алкоголь во второй половине цикла усиливают симптомы ПМС.", "womenshealth.gov", null, male = false),
        Fact("Алкоголь в анализе 50 исследований связан с более редким использованием презервативов и большим числом случайных партнёров.", "Cho & Yang, Int J Public Health 2023", HabitType.ALCOHOL),
        Fact("После 5 недель без алкоголя у добровольцев жира в печени стало меньше на 15% и более.", "UCL / New Scientist, 2013", HabitType.ALCOHOL),
        Fact("Алкоголь — канцероген 1-й группы: он причинно связан минимум с 7 видами рака.", "ВОЗ / IARC", HabitType.ALCOHOL),
        Fact("Через 12 часов без сигарет уровень угарного газа в крови возвращается к норме.", "ВОЗ", HabitType.SMOKING),
        Fact("Отдельная волна тяги к сигарете обычно проходит за 3–5 минут. Переждать её — реально.", "HSE (Ирландия)", HabitType.SMOKING),
        Fact("Отказ от курения в 30 лет добавляет почти 10 лет ожидаемой жизни.", "ВОЗ; CDC", HabitType.SMOKING),
        Fact("Уже через 24 часа без курения у мужчин измеримо улучшаются ночные эрекции.", "Guay et al., Endocr Pract 1998", HabitType.SMOKING, male = true),
        Fact("Через год после отказа риск ишемической болезни сердца примерно вдвое ниже, чем у курящего.", "ВОЗ", HabitType.SMOKING),
        Fact("Синдром отмены никотина сильнее всего в первые 3 дня и заметно слабеет к концу первой недели.", "NCI", HabitType.SMOKING),
        Fact("Привычка в среднем становится автоматической за 66 дней, а пропуск одного дня почти не влияет на результат.", "Lally et al., 2010", null),
        Fact("Счастье от секса растёт примерно до частоты «раз в неделю», дальше — выходит на плато.", "Muise et al., SPPS 2016 (n=30 645)", null),
        Fact("Мужчины с частотой эякуляций 21+ в месяц имели на 19–22% меньший риск рака простаты, чем при 4–7 в месяц.", "Rider et al., European Urology 2016", null, male = true),
        Fact("Презерватив защищает от беременности на 98% при идеальном и около 87% при обычном использовании.", "Contraceptive Technology / Guttmacher", null),
        Fact("Отложить действие на 5 минут, потом ещё на 5 — простой и рабочий способ пережить тягу.", "GoodRx; техника «urge surfing»", null),
        Fact("Срыв чаще превращается в полный возврат из-за чувства вины («всё пропало»). Доброта к себе после срыва помогает быстрее вернуться.", "Marlatt, модель профилактики рецидивов", null),
        Fact("Даже 10–15 минут физической нагрузки заметно ослабляют тягу к сигарете.", "Taylor, Ussher, Faulkner, Addiction 2007 (систематический обзор)", HabitType.SMOKING),
    )

    fun today(epochDay: Long, types: Set<HabitType>, female: Boolean = false): Fact {
        val pool = list.filter { (it.type == null || it.type in types) && it.fits(female) }
        return pool[(epochDay % pool.size).toInt()]
    }
}
