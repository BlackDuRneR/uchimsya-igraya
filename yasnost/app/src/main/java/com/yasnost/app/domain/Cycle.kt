package com.yasnost.app.domain

import com.yasnost.app.data.AppState
import com.yasnost.app.data.Flow
import kotlin.math.roundToInt

enum class Phase(val title: String) {
    MENSTRUAL("Менструация"),
    FOLLICULAR("Фолликулярная фаза"),
    FERTILE("Фертильное окно"),
    LUTEAL("Лютеиновая фаза"),
    PMS("Предменструальные дни"),
    LATE("Задержка"),
}

data class PeriodRun(val start: Long, val end: Long) { val length get() = (end - start + 1).toInt() }

data class CycleInfo(
    val periods: List<PeriodRun>,
    val avgCycle: Int,
    val avgPeriod: Int,
    val cycleLengths: List<Int>,
    val today: Long,
    /** Последний период ещё не закрыт кнопкой «Закончились» */
    val open: Boolean,
) {
    val last get() = periods.lastOrNull()
    val lastStart get() = last?.start
    val hasData get() = last != null
    val dayOfCycle get() = lastStart?.let { (today - it + 1).toInt() }

    /** Конец последнего периода с учётом того, что он ещё может идти. */
    val lastEffectiveEnd: Long? get() = last?.let { r ->
        if (open && today - r.end <= 2) maxOf(r.end, minOf(r.start + avgPeriod - 1, today)) else r.end
    }
    val inPeriodNow get() = last != null && today in last!!.start..lastEffectiveEnd!! && (open || today <= last!!.end)

    val nextStart: Long? get() = lastStart?.let { it + avgCycle }
    val lateDays get() = nextStart?.let { (today - it).toInt() }?.takeIf { it > 0 && !inPeriodNow } ?: 0
    val ovulation get() = nextStart?.let { it - 14 }
    val fertile get() = ovulation?.let { (it - 5)..(it + 1) }
    val regular get() = cycleLengths.size < 3 || (cycleLengths.max() - cycleLengths.min()) <= 7

    fun phase(): Phase? {
        val ns = nextStart ?: return null
        return when {
            inPeriodNow -> Phase.MENSTRUAL
            today >= ns -> Phase.LATE
            fertile != null && today in fertile!! -> Phase.FERTILE
            today >= ns - 5 -> Phase.PMS
            today > ns - 14 -> Phase.LUTEAL
            else -> Phase.FOLLICULAR
        }
    }

    /** Ожидаемые начала месячных: ближайшее и ещё несколько. */
    fun futureStarts(n: Int = 4): List<Long> {
        val ns = nextStart ?: return emptyList()
        return (0 until n).map { ns + it * avgCycle }
    }

    fun isPredictedPeriod(day: Long) = day > (lastEffectiveEnd ?: Long.MIN_VALUE) && futureStarts().any { day in it until it + avgPeriod }
    fun isPredictedFertile(day: Long) = futureStarts().any { s -> day in (s - 19)..(s - 13) }
    fun isOvulation(day: Long) = futureStarts().any { day == it - 14 }
    /** День внутри текущего периода, ещё не отмеченный вручную. */
    fun isAssumedPeriod(day: Long) = last != null && day > last!!.end && day <= (lastEffectiveEnd ?: 0) && day <= today
}

object CycleLogic {
    fun info(s: AppState, today: Long): CycleInfo {
        val bleeding = s.cycle.filter { it.flow != null && it.flow != Flow.SPOTTING }.map { it.day }.sorted()
        val runs = mutableListOf<PeriodRun>()
        for (d in bleeding) {
            val last = runs.lastOrNull()
            if (last != null && d - last.end <= 2) runs[runs.lastIndex] = last.copy(end = d)
            else runs += PeriodRun(d, d)
        }
        val lengths = runs.zipWithNext { a, b -> (b.start - a.start).toInt() }.filter { it in 15..60 }.takeLast(6)
        val avgCycle = if (lengths.isEmpty()) s.cycleSettings.cycleLength else lengths.average().roundToInt()
        val open = runs.isNotEmpty() && s.periodClosedStart != runs.last().start
        // для средней длительности берём только завершённые и полностью отмеченные периоды
        val closed = runs.dropLast(if (open) 1 else 0).takeLast(6).map { it.length }.filter { it in 2..12 }
        val avgPeriod = if (closed.isEmpty()) s.cycleSettings.periodLength else closed.average().roundToInt()
        return CycleInfo(runs, avgCycle, avgPeriod, lengths, today, open)
    }

    val symptoms = listOf(
        "Спазмы", "Боль в пояснице", "Головная боль", "Вздутие", "Грудь чувствительна", "Акне",
        "Усталость", "Тошнота", "Перепады настроения", "Раздражительность", "Тревога",
        "Тяга к сладкому", "Бессонница", "Диарея", "Высокое либидо",
    )
    val meds = listOf("Ибупрофен", "Напроксен", "Парацетамол", "Грелка", "Прогулка / спорт", "Другое")

    data class PhaseText(val body: String, val tips: List<String>, val partner: List<String>)

    fun phaseText(p: Phase): PhaseText = when (p) {
        Phase.MENSTRUAL -> PhaseText(
            "Слизистая матки обновляется. Эстроген и прогестерон на минимуме — отсюда усталость, спазмы и желание тепла.",
            listOf("Тепло на низ живота — грелка или пластырь", "Лёгкое движение: прогулка, йога, растяжка", "Обезболивающее из группы НПВП — лучше в начале боли (см. гид ниже)", "Еда с железом: мясо, гречка, бобовые, зелень", "Больше сна и воды"),
            listOf("Спроси, чем помочь, — не угадывай", "Предложи грелку, чай, плед и любимую еду", "Возьми на себя часть бытовых дел", "Не обесценивай боль фразами «да ладно, у всех так»"),
        )
        Phase.FOLLICULAR -> PhaseText(
            "Растёт эстроген — у многих прибавляется энергии, настроение лучше, легче даются тренировки и новые дела.",
            listOf("Хорошее время для интенсивных тренировок", "Планируй важные задачи и встречи", "Пробуй новое — мотивация обычно выше"),
            listOf("Хорошее время для активных планов вдвоём", "Свидания, спорт, поездки — энергии обычно больше"),
        )
        Phase.FERTILE -> PhaseText(
            "Около овуляции шанс забеременеть самый высокий: сперматозоиды живут до 5 дней, яйцеклетка — около суток. Многие отмечают пик либидо.",
            listOf("Если беременность не планируется — только надёжная контрацепция", "Календарь не защищает от беременности: при обычном использовании ошибается у 8–25 из 100 женщин в год", "Возможны лёгкие тянущие боли сбоку внизу живота — это нормально"),
            listOf("Если вы не планируете ребёнка — обязательно предохраняйтесь", "Календарный прогноз — не контрацепция"),
        )
        Phase.LUTEAL -> PhaseText(
            "После овуляции растёт прогестерон: может хотеться больше есть и спать, повышается температура тела.",
            listOf("Сложные углеводы и белок помогают с тягой к сладкому", "Умеренные тренировки, больше сна", "Начни снижать кофеин и соль к концу фазы"),
            listOf("Будь терпеливее — силы у неё может быть меньше", "Хорошее время для спокойных вечеров дома"),
        )
        Phase.PMS -> PhaseText(
            "Гормоны падают перед месячными. Возможны раздражительность, вздутие, чувствительность груди, тревога — это физиология, а не «характер».",
            listOf("Аэробная нагрузка 30 минут большую часть дней уменьшает симптомы ПМС", "Меньше кофеина, соли, сахара и алкоголя", "Кальций 1000–1200 мг в сутки (из еды или добавок) в исследованиях уменьшал симптомы", "Если ПМС сильно мешает жизни — это повод обсудить лечение с гинекологом"),
            listOf("Не принимай раздражительность на свой счёт", "Не говори «это у тебя ПМС» во время спора", "Отложи серьёзные разговоры и решения на пару дней", "Шоколадка, объятия (если она хочет) и тишина работают лучше советов"),
        )
        Phase.LATE -> PhaseText(
            "Месячные не начались в ожидаемый день. Задержка до 7 дней бывает из-за стресса, болезни, перелётов, резкой смены веса или нагрузок.",
            listOf("Если был незащищённый контакт — сделай тест на беременность (с первого дня задержки или через 21 день после контакта)", "Если месячных нет 3 месяца и ты не беременна — обратись к врачу"),
            listOf("Сохраняй спокойствие и будь рядом", "Если был незащищённый секс — предложи вместе купить тест"),
        )
    }

    data class Guide(val emoji: String, val title: String, val text: String, val source: String)

    val painGuide = listOf(
        Guide("💊", "Обезболивающие (НПВП)",
            "Препараты первой линии при менструальной боли — нестероидные противовоспалительные: ибупрофен, напроксен. Они снижают выработку простагландинов, которые вызывают спазмы, и работают лучше плацебо и парацетамола. Лучше всего — начать при первых признаках месячных (или за день, если цикл предсказуем) и принимать регулярно первые 2–3 дня, а не ждать сильной боли.\\n\\nБез рецепта ибупрофен обычно принимают по 200–400 мг, не чаще чем раз в 4–6 часов и не больше 1200 мг в сутки. Принимай после еды и строго по инструкции на упаковке.\\n\\nНельзя без врача: язва или кровотечения ЖКТ, болезни почек, астма от аспирина, нарушения свёртываемости, беременность, приём других НПВП или антикоагулянтов.",
            "AAFP, Dysmenorrhea 2021; NHS: Ibuprofen for adults"),
        Guide("🌡️", "Парацетамол",
            "Подходит, если НПВП нельзя. Действует слабее при спазмах, но безопаснее для желудка. Не превышай дозу с упаковки и не сочетай с другими лекарствами, содержащими парацетамол.",
            "AAFP 2021; NHS"),
        Guide("🔥", "Тепло",
            "Грелка или тёплый пластырь на низ живота. В рандомизированном исследовании постоянное мягкое тепло (≈40 °C) помогало примерно как ибупрофен, а вместе с ним боль уходила быстрее.",
            "Akin et al., Obstet Gynecol 2001"),
        Guide("🏃‍♀️", "Движение",
            "Регулярные упражнения — от йоги до аэробики — по обзору Cochrane могут заметно снижать силу менструальной боли. Лучший эффект — при занятиях 3+ раза в неделю по 30+ минут на протяжении 8 недель и дольше.",
            "Armour et al., Cochrane 2019; Frontiers in Medicine 2025"),
        Guide("🌿", "Что ещё может помочь",
            "Имбирь в исследованиях уменьшал боль сильнее плацебо и сопоставимо с НПВП (данные невысокого качества). Магний и витамин B1 — небольшие исследования с положительным результатом. ТЭНС (электростимуляция высокой частоты) снижает боль. Гормональная контрацепция — тоже лечение первой линии, но её подбирает врач.",
            "J Integr Complement Med 2024; Cochrane 2016; AAFP 2021"),
        Guide("🩺", "Когда срочно к врачу",
            "• Боль не проходит от обезболивающих или мешает учёбе и работе — проверь эндометриоз (в среднем его диагностируют только через 8–9 лет)\\n• Прокладки или тампона хватает меньше чем на час, или месячные дольше 7 дней\\n• Кровь между месячными или после секса\\n• Цикл короче 21 или длиннее 35 дней, или месячных нет 3 месяца\\n• Резкая боль с температурой, обмороком или рвотой — это неотложное состояние",
            "NHS: Period problems; womenshealth.gov; Endometriosis UK"),
    )

    val pmsGuide = Guide("🌧️", "ПМС: что работает",
        "Аэробные упражнения около 30 минут большую часть дней. Кальций 1000–1200 мг в сутки. За 2 недели до месячных — меньше кофеина, соли, сахара и алкоголя. Сон по режиму. Если настроение падает настолько, что мешает жизни (ПМДР), — к врачу: это лечится.",
        "womenshealth.gov; ACOG через UIHC; AAFP 2003")

    val contraceptionNote = "Прогноз овуляции — оценка. Даже у женщин с регулярным циклом фертильные дни часто сдвигаются: в исследовании BMJ лишь у 30% женщин они целиком попадали в «классические» 10–17 дни. Календарный метод при обычном использовании ведёт к 8–25 беременностям на 100 женщин в год. Не используй прогноз как контрацепцию."
}
