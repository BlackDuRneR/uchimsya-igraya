package com.yasnost.app.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yasnost.app.data.AppState
import com.yasnost.app.data.CycleDay
import com.yasnost.app.data.Flow
import com.yasnost.app.data.Repository
import com.yasnost.app.data.SexStatus
import com.yasnost.app.data.ownerKey
import com.yasnost.app.domain.CycleInfo
import com.yasnost.app.domain.CycleLogic
import com.yasnost.app.domain.Fmt
import com.yasnost.app.domain.Phase
import com.yasnost.app.ui.components.ChipsMulti
import com.yasnost.app.ui.components.Chip
import com.yasnost.app.ui.components.Glass
import com.yasnost.app.ui.components.GhostButton
import com.yasnost.app.ui.components.GradientButton
import com.yasnost.app.ui.components.LocalFemale
import com.yasnost.app.ui.components.MonthCalendar
import com.yasnost.app.ui.components.ProgressRing
import com.yasnost.app.ui.components.SectionLabel
import com.yasnost.app.ui.components.StatTile
import com.yasnost.app.ui.components.g
import com.yasnost.app.ui.theme.Accent
import com.yasnost.app.ui.theme.Y
import com.yasnost.app.ui.theme.YType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset

val BLOOD = Color(0xFFFF4D6D)
val CYCLE = Accent(BLOOD, Y.Rose)
val FERTILE = Accent(Y.Teal, Y.Sky)

fun Phase.accent(): Accent = when (this) {
    Phase.MENSTRUAL -> CYCLE
    Phase.FOLLICULAR -> Accent(Y.Violet, Y.Sky)
    Phase.FERTILE -> FERTILE
    Phase.LUTEAL -> Accent(Y.Amber, Y.Coral)
    Phase.PMS -> Accent(Y.Magenta, Y.Violet)
    Phase.LATE -> Accent(Y.Coral, Y.Amber)
}

fun Phase.emoji() = when (this) {
    Phase.MENSTRUAL -> "🩸"; Phase.FOLLICULAR -> "🌱"; Phase.FERTILE -> "🌸"; Phase.LUTEAL -> "🌙"; Phase.PMS -> "🌧️"; Phase.LATE -> "⏳"
}

private fun d(day: Long) = shortDayFmt.format(LocalDate.ofEpochDay(day))
fun partnerTitle(s: AppState) = s.cyclePartner()?.let { "Цикл · ${it.name}" } ?: "Цикл девушки"

/** Партнёры с привязкой к текущему циклу (для фильтра записей близости). */
private fun AppState.cyclePartnerIds() = cyclePartner()?.let { setOf(it.id) } ?: emptySet()
private fun com.yasnost.app.data.IntimacyEntry.withCyclePerson(s: AppState) =
    s.profile.female || partnerIds.isEmpty() || s.cyclePartnerIds().isEmpty() || partnerIds.any { it in s.cyclePartnerIds() }

/** Короткая строка статуса для главной и уведомлений. */
fun cycleStatus(ci: CycleInfo): String {
    val ns = ci.nextStart ?: return "Нет данных"
    return when {
        ci.inPeriodNow -> "месячные · день ${ci.today - ci.lastStart!! + 1}"
        ci.lateDays > 0 -> "задержка ${ci.lateDays} ${Fmt.days(ci.lateDays.toLong())}"
        ns - ci.today == 1L -> "месячные завтра"
        else -> "день ${ci.dayOfCycle} · месячные через ${ns - ci.today} ${Fmt.days(ns - ci.today)}"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CycleContent(s: AppState, repo: Repository, scope: CoroutineScope) {
    val female = LocalFemale.current
    val today = LocalDate.now()
    val t = today.toEpochDay()
    val ci = CycleLogic.info(s, t)
    var month by remember { mutableStateOf(YearMonth.now()) }
    var editDay by remember { mutableStateOf<Long?>(null) }
    var pickStart by remember { mutableStateOf(false) }
    val phase = ci.phase()

    if (!female && s.partners.size > 1) {
        Row(Modifier.padding(bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            s.partners.forEach { p ->
                Chip(p.name, ownerKey(p.id) == s.cycleOwner, CYCLE) {
                    scope.launch { repo.update(refreshWidgets = true) { it.copy(cycleOwner = ownerKey(p.id)) } }
                }
            }
        }
    }

    if (!ci.hasData) {
        Glass(Modifier.fillMaxWidth(), padding = 22.dp, tint = BLOOD.copy(alpha = 0.05f)) {
            Text("🌸", fontSize = 34.sp)
            Text(g(partnerTitle(s), "Мой цикл"), style = YType.Title, color = Y.Text, modifier = Modifier.padding(top = 8.dp))
            Text(
                g("Отметь первый день её последних месячных — приложение посчитает следующие, фертильные дни и ПМС, и напомнит заранее.",
                    "Отметь первый день последних месячных — приложение посчитает следующие, фертильные дни и ПМС, подскажет, как облегчить боль, и напомнит заранее."),
                style = YType.Body, color = Y.Text2, modifier = Modifier.padding(top = 6.dp, bottom = 16.dp)
            )
            GradientButton("Отметить начало месячных", CYCLE, Modifier.fillMaxWidth()) { pickStart = true }
        }
    } else {
        // Главная карточка
        val acc = phase?.accent() ?: CYCLE
        Glass(Modifier.fillMaxWidth(), radius = 30.dp, padding = 18.dp, tint = acc.a.copy(alpha = 0.06f)) {
            Text(g(partnerTitle(s).uppercase(), "МОЙ ЦИКЛ"), style = YType.Label, color = acc.b)
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 10.dp)) {
                val doc = ci.dayOfCycle ?: 1
                ProgressRing((doc.toFloat() / ci.avgCycle).coerceIn(0f, 1f), acc, Modifier.size(130.dp), stroke = 11.dp) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("ДЕНЬ", style = YType.Label, color = Y.Text3)
                        Text("$doc", style = YType.Big.copy(fontSize = 40.sp), color = Y.Text)
                        Text("из ~${ci.avgCycle}", style = YType.Body.copy(fontSize = 12.sp), color = Y.Text3)
                    }
                }
                Column(Modifier.padding(start = 16.dp)) {
                    if (phase != null) Text("${phase.emoji()} ${phase.title}", style = YType.Sub, color = Y.Text)
                    val ns = ci.nextStart!!
                    Text(
                        when {
                            ci.inPeriodNow -> "День ${t - ci.lastStart!! + 1} из ~${ci.avgPeriod}"
                            ci.lateDays > 0 -> "Задержка ${ci.lateDays} ${Fmt.days(ci.lateDays.toLong())}"
                            else -> "Месячные через ${ns - t} ${Fmt.days(ns - t)}"
                        },
                        style = YType.Title.copy(fontSize = 19.sp), color = acc.b, modifier = Modifier.padding(top = 4.dp)
                    )
                    if (!ci.inPeriodNow && ci.lateDays == 0) Text("≈ ${d(ns)}", style = YType.Body, color = Y.Text2)
                    ci.ovulation?.takeIf { it >= t }?.let { Text("Овуляция ≈ ${d(it)}", style = YType.Body.copy(fontSize = 13.sp), color = Y.Text3, modifier = Modifier.padding(top = 4.dp)) }
                }
            }
            Row(Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (ci.inPeriodNow) {
                    GradientButton("Закончились", CYCLE, Modifier.weight(1f)) {
                        scope.launch { repo.markPeriod(ci.lastStart!!, t, close = true) }
                    }
                } else {
                    GradientButton("Начались сегодня", CYCLE, Modifier.weight(1f)) {
                        scope.launch { repo.markPeriod(t, t, close = false) }
                    }
                }
                GhostButton("Отметить день", Modifier.weight(1f)) { editDay = t }
            }
        }
    }
    Spacer(Modifier.height(14.dp))

    MonthCalendar(month, { month = it }, footer = {
        Column(Modifier.padding(top = 10.dp, start = 6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Legend(Brush.linearGradient(listOf(BLOOD, Y.Rose)), "Месячные")
                Legend(Brush.linearGradient(listOf(Y.Rose.copy(alpha = 0.35f), Y.Rose.copy(alpha = 0.35f))), "Прогноз")
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                Legend(Brush.linearGradient(listOf(Y.Teal.copy(alpha = 0.45f), Y.Teal.copy(alpha = 0.45f))), "Фертильные дни")
                Legend(Brush.linearGradient(listOf(Y.Teal, Y.Sky)), "Овуляция")
            }
        }
    }) { date -> CycleCell(date, today, s, ci) { if (!date.isAfter(today)) editDay = date.toEpochDay() } }

    if (pickStart) {
        val st = rememberDatePickerState(
            initialSelectedDateMillis = today.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli(),
            selectableDates = object : SelectableDates { override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis <= System.currentTimeMillis() },
        )
        DatePickerDialog(
            onDismissRequest = { pickStart = false },
            confirmButton = {
                TextButton(onClick = {
                    st.selectedDateMillis?.let {
                        val day = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().toEpochDay()
                        val end = minOf(day + s.cycleSettings.periodLength - 1, t)
                        scope.launch { repo.markPeriod(day, end, close = end < t) }
                    }
                    pickStart = false
                }) { Text("OK", color = BLOOD) }
            },
            dismissButton = { TextButton(onClick = { pickStart = false }) { Text("Отмена", color = Y.Text2) } },
            colors = DatePickerDefaults.colors(containerColor = Y.Surface2),
        ) {
            DatePicker(st, title = { Text("  Первый день месячных", color = Y.Text2, modifier = Modifier.padding(top = 16.dp, start = 12.dp)) },
                colors = DatePickerDefaults.colors(containerColor = Y.Surface2, selectedDayContainerColor = BLOOD, todayDateBorderColor = BLOOD, todayContentColor = BLOOD))
        }
    }

    if (ci.hasData && phase != null) {
        val pt = CycleLogic.phaseText(phase)
        SectionLabel("Сейчас: ${phase.title.lowercase()}", Modifier.padding(top = 14.dp))
        Glass(Modifier.fillMaxWidth(), radius = 24.dp, padding = 16.dp) {
            Text(pt.body, style = YType.Body, color = Y.Text)
            Text(if (female) "ЧТО ПОМОЖЕТ" else "КАК ПОДДЕРЖАТЬ ЕЁ", style = YType.Label, color = phase.accent().b, modifier = Modifier.padding(top = 14.dp, bottom = 4.dp))
            (if (female) pt.tips else pt.partner).forEach {
                Row(Modifier.padding(vertical = 3.dp)) {
                    Text("•  ", style = YType.Body, color = phase.accent().b)
                    Text(it, style = YType.Body.copy(fontSize = 14.sp), color = Y.Text2)
                }
            }
        }

        // Прогноз
        SectionLabel("Прогноз", Modifier.padding(top = 14.dp))
        Glass(Modifier.fillMaxWidth(), radius = 24.dp, padding = 16.dp) {
            val fs = ci.futureStarts(3)
            val ns = fs.first()
            ForecastRow("🩸", "Следующие месячные", "${d(ns)} – ${d(ns + ci.avgPeriod - 1)}")
            ForecastRow("🌸", "Фертильные дни", "${d(ns - 19)} – ${d(ns - 13)}")
            ForecastRow("🥚", "Овуляция", "≈ ${d(ns - 14)}")
            ForecastRow("🌧️", "Возможен ПМС", "с ${d(ns - 5)}")
            ForecastRow("📅", "Потом", fs.drop(1).joinToString(", ") { d(it) })
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile("средний цикл", "${ci.avgCycle}", Fmt.days(ci.avgCycle.toLong()), BLOOD, Modifier.weight(1f))
            StatTile("месячные", "${ci.avgPeriod}", Fmt.days(ci.avgPeriod.toLong()), BLOOD, Modifier.weight(1f))
        }
        if (ci.cycleLengths.isNotEmpty()) {
            SectionLabel("Последние циклы", Modifier.padding(top = 14.dp))
            Glass(Modifier.fillMaxWidth(), radius = 24.dp, padding = 16.dp) {
                val max = ci.cycleLengths.max().coerceAtLeast(35)
                ci.cycleLengths.forEach { len ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                        Box(Modifier.weight(1f).height(10.dp).clip(RoundedCornerShape(50)).background(Color(0x12FFFFFF))) {
                            Box(Modifier.fillMaxWidth(len.toFloat() / max).height(10.dp).clip(RoundedCornerShape(50)).background(
                                if (len in 21..35) CYCLE.brush else Brush.horizontalGradient(listOf(Y.Amber, Y.Coral))))
                        }
                        Text("  $len ${Fmt.days(len.toLong())}", style = YType.Sub.copy(fontSize = 13.sp), color = Y.Text2, modifier = Modifier.width(80.dp))
                    }
                }
                Text(
                    if (ci.regular) "Цикл регулярный: норма для взрослых — от 21 до 35 дней, разброс до 7 дней — это нормально."
                    else "Длина цикла заметно меняется (разброс больше 7 дней). Так бывает у 4 из 10 женщин, но прогноз будет менее точным. Если цикл короче 21 или длиннее 35 дней — стоит показаться гинекологу.",
                    style = YType.Body.copy(fontSize = 12.sp), color = Y.Text3, modifier = Modifier.padding(top = 8.dp)
                )
            }
        }

        // Риск беременности
        val lastUnprot = s.intimacy.filter { it.sex == SexStatus.UNPROTECTED && it.vaginal && it.day >= (ci.lastStart ?: 0) && it.withCyclePerson(s) }.maxByOrNull { it.day }
        if (lastUnprot != null) {
            val fertileNow = ci.fertile?.let { lastUnprot.day in it } == true
            SectionLabel("Незащищённый контакт", Modifier.padding(top = 14.dp))
            Glass(Modifier.fillMaxWidth(), radius = 24.dp, padding = 16.dp, tint = Y.Coral.copy(alpha = 0.06f)) {
                Text(
                    "${d(lastUnprot.day)} — " + if (fertileNow) "в прогнозируемые фертильные дни. Вероятность беременности повышена." else "вне прогнозируемых фертильных дней, но прогноз не гарантия.",
                    style = YType.Body, color = Y.Text
                )
                if (t - lastUnprot.day <= 5) Text(
                    "Экстренная контрацепция: таблетка левоноргестрела — до 72 часов, улипристала — до 120 часов, медная спираль — до 5 дней. Чем раньше, тем надёжнее. Обратись в аптеку или к гинекологу.",
                    style = YType.Body.copy(fontSize = 13.sp), color = Y.Coral, modifier = Modifier.padding(top = 8.dp)
                )
                Text(
                    "Тест на беременность — с первого дня задержки (${d(ci.nextStart!!)}) или через 21 день после контакта (${d(lastUnprot.day + 21)}).",
                    style = YType.Body.copy(fontSize = 13.sp), color = Y.Text2, modifier = Modifier.padding(top = 8.dp)
                )
                Text("Источник: NHS — Emergency contraception, Pregnancy tests", style = YType.Body.copy(fontSize = 11.sp), color = Y.Text3, modifier = Modifier.padding(top = 6.dp))
            }
        }

        // Симптомы
        val logged = s.cycle.filter { it.day > t - 90 }
        if (logged.isNotEmpty()) {
            val top = logged.flatMap { it.symptoms }.groupingBy { it }.eachCount().entries.sortedByDescending { it.value }.take(6)
            val painDays = logged.filter { it.flow != null && it.pain > 0 }
            if (top.isNotEmpty() || painDays.isNotEmpty()) {
                SectionLabel(g("Её симптомы за 3 месяца", "Мои симптомы за 3 месяца"), Modifier.padding(top = 14.dp))
                Glass(Modifier.fillMaxWidth(), radius = 24.dp, padding = 16.dp) {
                    if (painDays.isNotEmpty()) {
                        val avg = painDays.map { it.pain }.average()
                        Text("Средняя боль в дни месячных: ${"%.1f".format(avg).replace('.', ',')} из 10", style = YType.Sub, color = if (avg >= 7) Y.Coral else Y.Text)
                        if (avg >= 7) Text("Сильная боль каждый цикл — повод показаться гинекологу и исключить эндометриоз.", style = YType.Body.copy(fontSize = 13.sp), color = Y.Coral, modifier = Modifier.padding(top = 4.dp, bottom = 8.dp))
                    }
                    val max = top.firstOrNull()?.value?.toFloat() ?: 1f
                    top.forEach { (name, n) ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                            Text(name, style = YType.Body.copy(fontSize = 14.sp), color = Y.Text, modifier = Modifier.weight(0.45f))
                            Box(Modifier.weight(0.45f).height(8.dp).clip(RoundedCornerShape(50)).background(Color(0x14FFFFFF))) {
                                Box(Modifier.fillMaxWidth(n / max).height(8.dp).clip(RoundedCornerShape(50)).background(CYCLE.brush))
                            }
                            Text("  $n", style = YType.Sub.copy(fontSize = 13.sp), color = Y.Text2)
                        }
                    }
                }
            }
        }
    }

    SectionLabel(g("Как помочь ей при боли", "Как облегчить боль"), Modifier.padding(top = 14.dp))
    (CycleLogic.painGuide + CycleLogic.pmsGuide).forEach { GuideCard(it) }
    Glass(Modifier.fillMaxWidth().padding(top = 2.dp), radius = 20.dp, padding = 14.dp, tint = Y.Amber.copy(alpha = 0.05f)) {
        Text("⚠️  Это не контрацепция", style = YType.Sub.copy(fontSize = 15.sp), color = Y.Amber)
        Text(CycleLogic.contraceptionNote, style = YType.Body.copy(fontSize = 13.sp), color = Y.Text2, modifier = Modifier.padding(top = 6.dp))
        Text("Wilcox et al., BMJ 2000; KFF; Fehring et al. 2006", style = YType.Body.copy(fontSize = 11.sp), color = Y.Text3, modifier = Modifier.padding(top = 6.dp))
    }
    Text(
        "Информация справочная и не заменяет консультацию врача. Лекарства — только по инструкции и при отсутствии противопоказаний.",
        style = YType.Body.copy(fontSize = 12.sp), color = Y.Text3, modifier = Modifier.padding(top = 8.dp, start = 4.dp)
    )

    // Графики веса и температуры
    if (female) {
        val temps = s.cycle.filter { it.owner == "" && it.temp != null && it.day > t - 60 }.sortedBy { it.day }
        val weights = s.cycle.filter { it.owner == "" && it.weight != null && it.day > t - 120 }.sortedBy { it.day }
        if (temps.size >= 3) {
            SectionLabel("Базальная температура", Modifier.padding(top = 14.dp))
            Glass(Modifier.fillMaxWidth(), padding = 16.dp) {
                LineChart(temps.map { it.day to it.temp!! }, Accent(Y.Amber, Y.Coral), "°")
                Text("После овуляции температура держится выше. Подъём видно только задним числом, поэтому метод подтверждает овуляцию, но не предсказывает её.",
                    style = YType.Body.copy(fontSize = 12.sp), color = Y.Text3, modifier = Modifier.padding(top = 8.dp))
            }
        }
        if (weights.size >= 2) {
            SectionLabel("Вес", Modifier.padding(top = 14.dp))
            Glass(Modifier.fillMaxWidth(), padding = 16.dp) {
                LineChart(weights.map { it.day to it.weight!! }, Accent(Y.Sky, Y.Violet), " кг")
                Text("Перед месячными вес часто «прыгает» на 0,5–2 кг из-за задержки воды — это нормально.",
                    style = YType.Body.copy(fontSize = 12.sp), color = Y.Text3, modifier = Modifier.padding(top = 8.dp))
            }
        }
    }

    editDay?.let { day -> CycleDayEditor(s, day, repo, scope) { editDay = null } }
}

@Composable
private fun ForecastRow(emoji: String, title: String, value: String) {
    Row(Modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(emoji, fontSize = 18.sp)
        Text(title, style = YType.Body, color = Y.Text2, modifier = Modifier.padding(start = 12.dp).weight(1f))
        Text(value, style = YType.Sub.copy(fontSize = 14.sp), color = Y.Text, textAlign = TextAlign.End)
    }
}

@Composable
fun GuideCard(gd: CycleLogic.Guide) {
    var open by remember { mutableStateOf(false) }
    Glass(Modifier.fillMaxWidth().padding(bottom = 8.dp).animateContentSize(), radius = 20.dp, padding = 14.dp, onClick = { open = !open }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(gd.emoji, fontSize = 20.sp)
            Text(gd.title, style = YType.Sub.copy(fontSize = 15.sp), color = Y.Text, modifier = Modifier.padding(start = 12.dp).weight(1f))
            Text(if (open) "−" else "+", style = YType.Title, color = Y.Text3)
        }
        if (open) {
            Text(gd.text.replace("\\n", "\n"), style = YType.Body.copy(fontSize = 14.sp), color = Y.Text2, modifier = Modifier.padding(top = 10.dp))
            Text("Источник: ${gd.source}", style = YType.Body.copy(fontSize = 11.sp), color = Y.Rose, modifier = Modifier.padding(top = 8.dp))
        }
    }
}

@Composable
private fun CycleCell(date: LocalDate, today: LocalDate, s: AppState, ci: CycleInfo, onClick: () -> Unit) {
    val day = date.toEpochDay()
    val c = s.cycleOn(day)
    val flowAlpha = when (c?.flow) { Flow.HEAVY -> 1f; Flow.MEDIUM -> 0.85f; Flow.LIGHT -> 0.6f; else -> 0f }
    val bg: Brush? = when {
        c?.flow != null && c.flow != Flow.SPOTTING -> Brush.linearGradient(listOf(BLOOD.copy(alpha = flowAlpha), Y.Rose.copy(alpha = flowAlpha)))
        ci.isAssumedPeriod(day) -> Brush.linearGradient(listOf(BLOOD.copy(alpha = 0.4f), Y.Rose.copy(alpha = 0.4f)))
        ci.isPredictedPeriod(day) -> Brush.linearGradient(listOf(Y.Rose.copy(alpha = 0.22f), Y.Rose.copy(alpha = 0.22f)))
        ci.isOvulation(day) -> FERTILE.brush
        ci.isPredictedFertile(day) -> Brush.linearGradient(listOf(Y.Teal.copy(alpha = 0.28f), Y.Teal.copy(alpha = 0.28f)))
        else -> null
    }
    val dot = when {
        c?.flow == Flow.SPOTTING -> BLOOD
        c != null && (c.symptoms.isNotEmpty() || c.pain > 0) && c.flow == null -> Y.Amber
        else -> null
    }
    val dark = (c?.flow != null && c.flow != Flow.SPOTTING) || ci.isOvulation(day)
    val sex = s.settings.intimacyEnabled && s.intimacy.any { it.day == day && it.sex != SexStatus.NONE && it.withCyclePerson(s) }
    val marks = buildString {
        if (sex) append("❤️")
        if (c?.ovulationTest == 2) append("🥚")
    }
    CalendarDay(date, today, bg, darkText = dark, dot = dot, marks = marks, onClick = onClick)
}

@Composable
fun LineChart(points: List<Pair<Long, Double>>, acc: Accent, unit: String) {
    val min = points.minOf { it.second }; val max = points.maxOf { it.second }
    val span = (max - min).coerceAtLeast(0.1)
    val first = points.first().first; val last = points.last().first
    androidx.compose.foundation.Canvas(Modifier.fillMaxWidth().height(100.dp)) {
        val w = size.width; val h = size.height
        val path = androidx.compose.ui.graphics.Path()
        points.forEachIndexed { i, (d, v) ->
            val x = if (last == first) w / 2 else w * (d - first) / (last - first).toFloat()
            val y = h - 6.dp.toPx() - (h - 12.dp.toPx()) * ((v - min) / span).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            drawCircle(acc.b, 3.dp.toPx(), androidx.compose.ui.geometry.Offset(x, y))
        }
        drawPath(path, Brush.horizontalGradient(listOf(acc.a, acc.b)), style = androidx.compose.ui.graphics.drawscope.Stroke(2.5.dp.toPx()))
    }
    Row(Modifier.padding(top = 6.dp)) {
        Text("мин ${"%.1f".format(min).replace('.', ',')}$unit", style = YType.Label, color = Y.Text3, modifier = Modifier.weight(1f))
        Text("макс ${"%.1f".format(max).replace('.', ',')}$unit", style = YType.Label, color = Y.Text3)
    }
}

@Composable
private fun EditorRow(emoji: String, title: String, trailing: @Composable () -> Unit = {}, content: (@Composable () -> Unit)? = null) {
    Column(Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(emoji, fontSize = 20.sp)
            Text(title, style = YType.Sub, color = Y.Text, modifier = Modifier.padding(start = 12.dp).weight(1f))
            trailing()
        }
        if (content != null) Box(Modifier.padding(top = 10.dp)) { content() }
    }
    Box(Modifier.fillMaxWidth().height(1.dp).background(Y.Stroke))
}

/** Полноэкранный редактор дня цикла. */
@Composable
fun CycleDayEditor(s: AppState, startDay: Long, repo: Repository, scope: CoroutineScope, onClose: () -> Unit) {
    val female = LocalFemale.current
    val today = LocalDate.now().toEpochDay()
    var day by remember { mutableStateOf(startDay) }
    val owner = s.currentOwner()
    val ci = CycleLogic.info(s, today)

    androidx.compose.ui.window.Dialog(onDismissRequest = onClose, properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)) {
        androidx.compose.runtime.key(day) {
            val e = s.cycleOn(day, owner)
            val ie = s.intimacyOn(day)
            var flow by remember { mutableStateOf(e?.flow) }
            var pain by remember { mutableFloatStateOf((e?.pain ?: 0).toFloat()) }
            var symptoms by remember { mutableStateOf(e?.symptoms?.toSet() ?: emptySet()) }
            var meds by remember { mutableStateOf(e?.meds?.toSet() ?: emptySet()) }
            var mood by remember { mutableIntStateOf(e?.mood ?: 0) }
            var note by remember { mutableStateOf(e?.note ?: "") }
            var lh by remember { mutableIntStateOf(e?.ovulationTest ?: 0) }
            var weight by remember { mutableStateOf(e?.weight?.toString()?.replace('.', ',') ?: "") }
            var temp by remember { mutableStateOf(e?.temp?.toString()?.replace('.', ',') ?: "") }
            var water by remember { mutableIntStateOf(e?.water ?: 0) }
            val run = ci.periods.firstOrNull { day in it.start..it.end }
            var isStart by remember { mutableStateOf(run?.start == day) }
            var isEnd by remember { mutableStateOf(run != null && run.end == day && s.closedStart(owner) == run.start) }
            var sex by remember { mutableStateOf(ie?.sex ?: SexStatus.NONE) }
            var kinds by remember { mutableStateOf(ie?.kinds?.toSet()?.ifEmpty { setOf("Вагинальный") } ?: setOf("Вагинальный")) }
            val fieldColors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BLOOD, unfocusedBorderColor = Y.Stroke, focusedTextColor = Y.Text, unfocusedTextColor = Y.Text, focusedLabelColor = BLOOD)

            fun save() {
                val w = weight.replace(',', '.').toDoubleOrNull()
                val tp = temp.replace(',', '.').toDoubleOrNull()
                val f = flow ?: if (isStart) Flow.MEDIUM else null
                scope.launch {
                    repo.update(refreshWidgets = true) { st ->
                        var cyc = st.cycle.filterNot { it.day == day && it.owner == owner }
                        val c = CycleDay(day, owner, f, pain.toInt(), symptoms.toList(), mood, meds.toList(), note.trim(), lh, w, tp, water)
                        val empty = f == null && c.pain == 0 && c.symptoms.isEmpty() && mood == 0 && c.meds.isEmpty() && c.note.isBlank() && lh == 0 && w == null && tp == null && water == 0
                        if (!empty) cyc = cyc + c
                        var closed = st.periodClosed
                        if (isEnd && f != null) {
                            // закрыть период: заполнить дни от начала до сегодня
                            val startOfRun = run?.start ?: day
                            val fill = (startOfRun until day).filter { d -> cyc.none { it.day == d && it.owner == owner && it.flow != null } }
                                .map { d -> (cyc.firstOrNull { it.day == d && it.owner == owner } ?: CycleDay(d, owner)).copy(flow = Flow.LIGHT) }
                            cyc = cyc.filterNot { c2 -> c2.owner == owner && fill.any { it.day == c2.day } } + fill
                            closed = closed + (owner to startOfRun)
                        } else if (!isEnd && run != null && st.closedStart(owner) == run.start && run.end == day) {
                            closed = closed - owner
                        }
                        // близость
                        var intim = st.intimacy
                        if (st.settings.intimacyEnabled) {
                            val old = st.intimacyOn(day)
                            if (sex != SexStatus.NONE) {
                                val pid = if (!st.profile.female) st.cyclePartner()?.id else null
                                val base = old ?: com.yasnost.app.data.IntimacyEntry(day)
                                intim = intim.filterNot { it.day == day } + base.copy(
                                    sex = sex, sexCount = base.sexCount.coerceAtLeast(1), kinds = kinds.toList(),
                                    partnerIds = if (pid != null && pid !in base.partnerIds) base.partnerIds + pid else base.partnerIds,
                                )
                            } else if (old != null && old.sex != SexStatus.NONE) {
                                intim = intim.filterNot { it.day == day } + old.copy(sex = SexStatus.NONE, sexCount = 0)
                            }
                        }
                        st.copy(cycle = cyc, periodClosed = closed, intimacy = intim)
                    }
                }
            }

            Column(Modifier.fillMaxSize().background(Y.Bg)) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onClose) { Text("Закрыть", color = Y.Text2) }
                    Row(Modifier.weight(1f), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = { save(); day -= 1 }) { Text("‹", style = YType.Title, color = Y.Text2) }
                        Text(if (day == today) "Сегодня" else shortDayFmt.format(LocalDate.ofEpochDay(day)), style = YType.Sub, color = Y.Text)
                        TextButton(onClick = { if (day < today) { save(); day += 1 } }) { Text("›", style = YType.Title, color = if (day < today) Y.Text2 else Y.Text3) }
                    }
                    TextButton(onClick = { save(); onClose() }) { Text("Готово", color = BLOOD, style = YType.Sub) }
                }
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
                    if (!female) Text("Отметки для: ${s.cyclePartner()?.name ?: "девушки"}", style = YType.Body.copy(fontSize = 13.sp), color = Y.Text3, modifier = Modifier.padding(bottom = 4.dp))
                    Glass(Modifier.fillMaxWidth(), radius = 24.dp, padding = 16.dp) {
                        EditorRow("↪️", "Начало месячных в этот день", trailing = {
                            androidx.compose.material3.Checkbox(isStart, { isStart = it; if (it && flow == null) flow = Flow.MEDIUM; if (!it && run?.start == day) flow = null },
                                colors = androidx.compose.material3.CheckboxDefaults.colors(checkedColor = BLOOD))
                        })
                        EditorRow("🩸", "Месячные", content = {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                                listOf(Flow.SPOTTING to "Мажущие", Flow.LIGHT to "Слабые", Flow.MEDIUM to "Средние", Flow.HEAVY to "Обильные").forEachIndexed { i, (fl, label) ->
                                    val sel = flow == fl
                                    Column(
                                        Modifier.weight(1f).clip(RoundedCornerShape(14.dp))
                                            .background(if (sel) BLOOD.copy(alpha = 0.25f) else Color(0x0DFFFFFF))
                                            .border(1.dp, if (sel) BLOOD else Y.Stroke, RoundedCornerShape(14.dp))
                                            .clickable { flow = if (sel) null else fl }
                                            .padding(vertical = 8.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                    ) {
                                        Text("💧".repeat(i + 1), fontSize = 10.sp, maxLines = 1)
                                        Text(label, style = YType.Body.copy(fontSize = 11.sp), color = if (sel) Y.Text else Y.Text2)
                                    }
                                }
                            }
                        })
                        EditorRow("✅", "Конец месячных в этот день", trailing = {
                            androidx.compose.material3.Checkbox(isEnd, { isEnd = it; if (it && flow == null) flow = Flow.LIGHT },
                                colors = androidx.compose.material3.CheckboxDefaults.colors(checkedColor = BLOOD))
                        })
                        EditorRow("😖", (if (female) "Боль" else "Её боль") + ": ${pain.toInt()} / 10", content = {
                            Slider(pain, { pain = it }, valueRange = 0f..10f, steps = 9,
                                colors = SliderDefaults.colors(thumbColor = BLOOD, activeTrackColor = BLOOD, inactiveTrackColor = Color(0x22FFFFFF), activeTickColor = Color.Transparent, inactiveTickColor = Color.Transparent))
                        })
                        EditorRow("💊", if (female) "Лекарства и что помогло" else "Что ей помогало", content = {
                            ChipsMulti(CycleLogic.meds, meds, Accent(Y.Violet, Y.Sky)) { v -> meds = if (v in meds) meds - v else meds + v }
                        })
                        if (s.settings.intimacyEnabled) {
                            EditorRow("❤️", "Половой акт", trailing = {
                                androidx.compose.material3.Checkbox(sex != SexStatus.NONE, { sex = if (it) SexStatus.PROTECTED else SexStatus.NONE },
                                    colors = androidx.compose.material3.CheckboxDefaults.colors(checkedColor = Y.Rose))
                            }, content = if (sex == SexStatus.NONE) null else ({
                                Column {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Chip("Защищённый", sex == SexStatus.PROTECTED, Accent(Y.Teal, Y.Mint)) { sex = SexStatus.PROTECTED }
                                        Chip("Без защиты", sex == SexStatus.UNPROTECTED, Accent(Y.Coral, Y.Rose)) { sex = SexStatus.UNPROTECTED }
                                    }
                                    Spacer(Modifier.height(8.dp))
                                    ChipsMulti(com.yasnost.app.domain.IntimacyLogic.kinds, kinds, Accent(Y.Rose, Y.Magenta)) { v -> kinds = if (v in kinds) kinds - v else kinds + v }
                                }
                            }))
                        }
                        EditorRow("🩺", if (female) "Симптомы" else "Что её беспокоит", content = {
                            ChipsMulti(CycleLogic.symptoms, symptoms, CYCLE) { v -> symptoms = if (v in symptoms) symptoms - v else symptoms + v }
                        })
                        EditorRow("🙂", "Настроение", content = {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                MOODS.forEachIndexed { i, em ->
                                    val sel = mood == i + 1
                                    Box(
                                        Modifier.size(44.dp).clip(CircleShape)
                                            .background(if (sel) Y.Rose.copy(alpha = 0.3f) else Color(0x0FFFFFFF))
                                            .border(if (sel) 1.5.dp else 0.dp, if (sel) Y.Rose else Color.Transparent, CircleShape)
                                            .clickable { mood = if (sel) 0 else i + 1 },
                                        contentAlignment = Alignment.Center,
                                    ) { Text(em, fontSize = 22.sp) }
                                }
                            }
                        })
                        EditorRow("🥚", "Тест на овуляцию", content = {
                            Column {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Chip("Не делали", lh == 0, Accent(Y.Text2, Y.Text3)) { lh = 0 }
                                    Chip("−", lh == 1, Accent(Y.Sky, Y.Violet)) { lh = 1 }
                                    Chip("+ положительный", lh == 2, FERTILE) { lh = 2 }
                                }
                                if (lh == 2) Text("Овуляция обычно наступает через 12–36 часов после всплеска ЛГ — прогноз уточнён.",
                                    style = YType.Body.copy(fontSize = 12.sp), color = Y.Teal, modifier = Modifier.padding(top = 6.dp))
                            }
                        })
                        if (female) {
                            EditorRow("⚖️", "Вес", trailing = {
                                OutlinedTextField(weight, { weight = it.filter { c -> c.isDigit() || c == ',' || c == '.' }.take(5) }, Modifier.width(110.dp), singleLine = true,
                                    suffix = { Text("кг", color = Y.Text3) }, shape = RoundedCornerShape(14.dp), colors = fieldColors,
                                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal))
                            })
                            EditorRow("🌡️", "Температура (базальная)", trailing = {
                                OutlinedTextField(temp, { temp = it.filter { c -> c.isDigit() || c == ',' || c == '.' }.take(5) }, Modifier.width(110.dp), singleLine = true,
                                    suffix = { Text("°C", color = Y.Text3) }, shape = RoundedCornerShape(14.dp), colors = fieldColors,
                                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal))
                            })
                            EditorRow("🥛", "Вода: $water из 8 стаканов", trailing = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    TextButton(onClick = { if (water > 0) water-- }) { Text("−", style = YType.Title, color = Y.Text2) }
                                    TextButton(onClick = { if (water < 20) water++ }) { Text("+", style = YType.Title, color = Y.Sky) }
                                }
                            }, content = {
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    repeat(8) { i -> Text(if (i < water) "💧" else "○", fontSize = 16.sp, color = Y.Text3) }
                                }
                            })
                        }
                        Column(Modifier.padding(vertical = 10.dp)) {
                            Text("📝  Заметка", style = YType.Sub, color = Y.Text)
                            OutlinedTextField(note, { note = it }, Modifier.fillMaxWidth().padding(top = 8.dp), placeholder = { Text("Как самочувствие?", color = Y.Text3) },
                                shape = RoundedCornerShape(16.dp), colors = fieldColors)
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    GradientButton("Сохранить", CYCLE, Modifier.fillMaxWidth()) { save(); onClose() }
                    Spacer(Modifier.height(40.dp))
                }
            }
        }
    }
}

/** Мини-карточка цикла для главной. */
@Composable
fun CycleMini(s: AppState, onClick: () -> Unit) {
    val ci = CycleLogic.info(s, LocalDate.now().toEpochDay())
    val phase = ci.phase()
    val female = LocalFemale.current
    Glass(Modifier.fillMaxWidth(), radius = 24.dp, padding = 16.dp, onClick = onClick, tint = (phase?.accent()?.a ?: BLOOD).copy(alpha = 0.06f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).clip(RoundedCornerShape(15.dp)).background((phase?.accent() ?: CYCLE).brush), contentAlignment = Alignment.Center) {
                Text(phase?.emoji() ?: "🌸", fontSize = 20.sp)
            }
            Column(Modifier.padding(start = 14.dp).weight(1f)) {
                Text(if (female) "Мой цикл" else partnerTitle(s), style = YType.Sub, color = Y.Text)
                Text(
                    if (ci.hasData) cycleStatus(ci).replaceFirstChar { it.uppercase() } + (phase?.takeIf { it != Phase.MENSTRUAL && it != Phase.LATE }?.let { " · " + it.title.lowercase() } ?: "")
                    else "Отметь начало месячных",
                    style = YType.Body.copy(fontSize = 13.sp), color = Y.Text2, maxLines = 2
                )
            }
            Text("→", style = YType.Title, color = (phase?.accent() ?: CYCLE).b)
        }
    }
}

/** Крупная карточка цикла для главной (режим без привычек). */
@Composable
fun CycleHomeCard(s: AppState, repo: Repository, scope: CoroutineScope, onOpen: () -> Unit) {
    val t = LocalDate.now().toEpochDay()
    val ci = CycleLogic.info(s, t)
    val phase = ci.phase()
    val acc = phase?.accent() ?: CYCLE
    Glass(Modifier.fillMaxWidth(), radius = 30.dp, padding = 18.dp, tint = acc.a.copy(alpha = 0.06f), onClick = onOpen) {
        Text(g(partnerTitle(s).uppercase(), "МОЙ ЦИКЛ"), style = YType.Label, color = acc.b)
        if (!ci.hasData) {
            Text("Отметь первый день последних месячных — и увидишь прогноз, фертильные дни и ПМС.", style = YType.Body, color = Y.Text2, modifier = Modifier.padding(top = 8.dp, bottom = 14.dp))
            GradientButton("Открыть календарь", CYCLE, Modifier.fillMaxWidth(), onClick = onOpen)
            return@Glass
        }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 10.dp)) {
            val doc = ci.dayOfCycle ?: 1
            ProgressRing((doc.toFloat() / ci.avgCycle).coerceIn(0f, 1f), acc, Modifier.size(140.dp), stroke = 12.dp) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("ДЕНЬ", style = YType.Label, color = Y.Text3)
                    Text("$doc", style = YType.Big.copy(fontSize = 44.sp), color = Y.Text)
                    Text("из ~${ci.avgCycle}", style = YType.Body.copy(fontSize = 12.sp), color = Y.Text3)
                }
            }
            Column(Modifier.padding(start = 16.dp)) {
                if (phase != null) Text("${phase.emoji()} ${phase.title}", style = YType.Sub, color = Y.Text)
                val ns = ci.nextStart!!
                Text(
                    when {
                        ci.inPeriodNow -> "День ${t - ci.lastStart!! + 1} из ~${ci.avgPeriod}"
                        ci.lateDays > 0 -> "Задержка ${ci.lateDays} ${Fmt.days(ci.lateDays.toLong())}"
                        else -> "Месячные через ${ns - t} ${Fmt.days(ns - t)}"
                    },
                    style = YType.Title.copy(fontSize = 19.sp), color = acc.b, modifier = Modifier.padding(top = 4.dp)
                )
                if (!ci.inPeriodNow && ci.lateDays == 0) Text("≈ ${d(ns)}", style = YType.Body, color = Y.Text2)
                ci.ovulation?.takeIf { it >= t }?.let { Text("Овуляция ≈ ${d(it)}", style = YType.Body.copy(fontSize = 13.sp), color = Y.Text3, modifier = Modifier.padding(top = 4.dp)) }
            }
        }
        if (phase != null) {
            val pt = CycleLogic.phaseText(phase)
            val tip = (if (LocalFemale.current) pt.tips else pt.partner).let { it[(t % it.size).toInt()] }
            Text("💡 $tip", style = YType.Body.copy(fontSize = 13.sp), color = Y.Text2, modifier = Modifier.padding(top = 12.dp))
        }
        Row(Modifier.padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (ci.inPeriodNow) GradientButton("Закончились", CYCLE, Modifier.weight(1f)) { scope.launch { repo.markPeriod(ci.lastStart!!, t, close = true) } }
            else GradientButton("Начались сегодня", CYCLE, Modifier.weight(1f)) { scope.launch { repo.markPeriod(t, t, close = false) } }
            GhostButton("Календарь", Modifier.weight(1f), onClick = onOpen)
        }
    }
}
