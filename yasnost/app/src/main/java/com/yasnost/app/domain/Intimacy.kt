package com.yasnost.app.domain

import com.yasnost.app.data.AppState
import com.yasnost.app.data.IntimacyEntry
import com.yasnost.app.data.SexStatus

data class IntimacyStats(
    val contacts30: Int,
    val protected30: Int,
    val unprotected30: Int,
    val masturbationDays30: Int,
    val avgDesire30: Double?,
    val noMasturbationStreak: Long?,
    val bestNoMasturbationStreak: Long,
    val lastUnprotectedDay: Long?,
    val totalContacts: Int,
    val totalProtected: Int,
    val alcoholContacts: Int,
    val alcoholUnprotected: Int,
    val loggedDays: Int,
)

object IntimacyLogic {
    fun stats(s: AppState, today: Long): IntimacyStats {
        val all = s.intimacy.sortedBy { it.day }
        val last30 = all.filter { it.day > today - 30 && it.day <= today }
        fun List<IntimacyEntry>.contacts(st: SexStatus? = null) =
            filter { it.sex != SexStatus.NONE && (st == null || it.sex == st) }.sumOf { it.sexCount }

        val mDays = all.filter { it.masturbation }.map { it.day }
        val noMStreak = when {
            all.isEmpty() -> null
            mDays.isEmpty() -> today - all.first().day + 1
            else -> today - mDays.last()
        }
        var best = noMStreak ?: 0L
        val anchors = listOf(all.firstOrNull()?.day?.minus(1)).filterNotNull() + mDays
        anchors.zipWithNext().forEach { (a, b) -> best = maxOf(best, b - a - 1) }

        val withAlcohol = all.filter { it.alcohol && it.sex != SexStatus.NONE }
        return IntimacyStats(
            contacts30 = last30.contacts(),
            protected30 = last30.contacts(SexStatus.PROTECTED),
            unprotected30 = last30.contacts(SexStatus.UNPROTECTED),
            masturbationDays30 = last30.count { it.masturbation },
            avgDesire30 = last30.takeIf { it.isNotEmpty() }?.map { it.desire }?.average(),
            noMasturbationStreak = noMStreak,
            bestNoMasturbationStreak = best,
            lastUnprotectedDay = all.lastOrNull { it.sex == SexStatus.UNPROTECTED }?.day,
            totalContacts = all.contacts(),
            totalProtected = all.contacts(SexStatus.PROTECTED),
            alcoholContacts = withAlcohol.sumOf { it.sexCount },
            alcoholUnprotected = withAlcohol.filter { it.sex == SexStatus.UNPROTECTED }.sumOf { it.sexCount },
            loggedDays = all.size,
        )
    }

    data class TestStep(val dayOffset: Long, val title: String, val text: String)

    /** Рекомендуемые сроки после незащищённого контакта (NHS, CDC, ВОЗ). */
    val testPlan = listOf(
        TestStep(0, "До 72 часов — ПКП", "Если есть риск ВИЧ (партнёр с ВИЧ или статус неизвестен), постконтактную профилактику нужно начать как можно раньше — лучше в первые 24 часа, не позже 72. Обратись в СПИД-центр или к инфекционисту."),
        TestStep(14, "Через 2 недели", "ПЦР на хламидиоз и гонорею (и трихомониаз). Раньше тест может быть ложноотрицательным."),
        TestStep(49, "Через 7 недель", "Анализ крови на ВИЧ и сифилис. NHS советует ждать 7 недель, если нет симптомов."),
        TestStep(84, "Через 12 недель", "Контрольный анализ на сифилис и ВИЧ — для полной уверенности."),
    )

    val facts = listOf(
        Fact("Благополучие в паре растёт с частотой секса примерно до одного раза в неделю, дальше — плато. Больше — не обязательно лучше. Связь найдена только у людей в отношениях.", "Muise, Schimmack, Impett. Social Psychological and Personality Science, 2016 (n=30 645)", null),
        Fact("Мужчины, у которых было 21 и более эякуляций в месяц, имели на 19–22% меньший риск рака простаты, чем при 4–7 в месяц. Это наблюдательная связь, а не доказанная причина; на агрессивные формы рака она не распространялась.", "Rider et al., European Urology 2016 (n=31 925, 18 лет наблюдения)", null),
        Fact("Редкий секс (раз в месяц и реже) был связан с риском сердечно-сосудистых болезней на 45% выше, чем 2+ раза в неделю. Скорее всего, частота — маркер общего здоровья.", "Hall et al., American Journal of Cardiology 2010", null),
        Fact("Мастурбация — нормальная часть сексуальности, доказанного вреда у неё нет. Популярная идея «скачка тестостерона на 7-й день воздержания» основана на небольшом исследовании, и эффект был временным.", "Обзор: Frontiers in Psychology 2021; Jiang et al., 2003", null),
        Fact("Отказ от мастурбации (NoFap) — личный выбор. Научных доказательств заявленных «суперсил» нет, но если это помогает тебе чувствовать контроль — календарь покажет твою серию.", "Обзоры Healthline, Snopes / Medical Xpress 2021", null),
        Fact("Мужской презерватив защищает от беременности на 98% при идеальном и около 87% при обычном использовании. Он надёжно снижает риск ВИЧ и гонореи; от ВПЧ и герпеса защищает частично.", "Contraceptive Technology (Guttmacher); CDC; NIH, 2001", null),
        Fact("Алкоголь связан с более редким использованием презервативов (OR 1,23) и большим числом партнёров. Трезвость — это ещё и сексуальная безопасность.", "Cho & Yang, International Journal of Public Health 2023 (50 исследований)", null),
        Fact("Через 3 месяца трезвости у 88,5% мужчин с нарушением эрекции на фоне алкоголя эрекция улучшилась.", "Проспективное исследование 2022, n=104 (PubMed 36606722)", null),
        Fact("Курение повреждает сосуды полового члена. Уже через 24 часа без сигарет улучшаются ночные эрекции, а через год у четверти бросивших с ЭД — улучшение.", "Guay 1998; Pourmand et al., BJU International 2004", null),
        Fact("После оргазма выделяются пролактин и окситоцин — они расслабляют и могут помогать уснуть. Данные в основном опросные, но многие отмечают сон лучше.", "Обзоры сексологии; опросы Central Queensland University 2019", null),
        Fact("Анализы на ИППП: хламидиоз и гонорея — через 2 недели после контакта, ВИЧ и сифилис — через 7 недель, контроль — через 12. ПКП от ВИЧ — не позже 72 часов.", "NHS; CDC nPEP 2025; ВОЗ", null),
        Fact("Хорошее либидо — признак здоровья. Его снижают стресс, недосып, алкоголь и курение. Если желание пропало надолго — это повод сходить к врачу и проверить гормоны.", "Cleveland Clinic; клинические рекомендации", null),
    )
}
