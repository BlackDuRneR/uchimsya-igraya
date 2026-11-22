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
fun partnerTitle(s: AppState) = if (s.profile.partnerName.isBlank()) "Цикл девушки" else "Цикл · ${s.profile.partnerName}"

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
        val lastUnprot = s.intimacy.filter { it.sex == SexStatus.UNPROTECTED && it.day >= (ci.lastStart ?: 0) }.maxByOrNull { it.day }
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

    editDay?.let { day -> CycleDayDialog(s, day, repo, scope) { editDay = null } }
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
    CalendarDay(date, today, bg, darkText = dark, dot = dot, onClick = onClick)
}

@Composable
private fun CycleDayDialog(s: AppState, day: Long, repo: Repository, scope: CoroutineScope, onClose: () -> Unit) {
    val e = s.cycleOn(day)
    val female = LocalFemale.current
    var flow by remember { mutableStateOf(e?.flow) }
    var pain by remember { mutableFloatStateOf((e?.pain ?: 0).toFloat()) }
    var symptoms by remember { mutableStateOf(e?.symptoms?.toSet() ?: emptySet()) }
    var meds by remember { mutableStateOf(e?.meds?.toSet() ?: emptySet()) }
    var mood by remember { mutableIntStateOf(e?.mood ?: 0) }
    var note by remember { mutableStateOf(e?.note ?: "") }

    AlertDialog(
        onDismissRequest = onClose,
        containerColor = Y.Surface2,
        shape = RoundedCornerShape(32.dp),
        title = { Text(dayFmt.format(LocalDate.ofEpochDay(day)).replaceFirstChar { it.uppercase() }, style = YType.Title, color = Y.Text) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(if (female) "ВЫДЕЛЕНИЯ" else "МЕСЯЧНЫЕ У НЕЁ", style = YType.Label, color = Y.Text3)
                ChipsMulti(
                    listOf("Нет", "Мажущие", "Слабые", "Средние", "Обильные"),
                    setOf(when (flow) { null -> "Нет"; Flow.SPOTTING -> "Мажущие"; Flow.LIGHT -> "Слабые"; Flow.MEDIUM -> "Средние"; Flow.HEAVY -> "Обильные" }),
                    CYCLE,
                ) { v -> flow = when (v) { "Мажущие" -> Flow.SPOTTING; "Слабые" -> Flow.LIGHT; "Средние" -> Flow.MEDIUM; "Обильные" -> Flow.HEAVY; else -> null } }
                Spacer(Modifier.height(4.dp))

                Text((if (female) "БОЛЬ" else "ЕЁ БОЛЬ") + ": ${pain.toInt()} / 10", style = YType.Label, color = Y.Text3, modifier = Modifier.padding(top = 14.dp))
                Slider(
                    pain, { pain = it }, valueRange = 0f..10f, steps = 9,
                    colors = SliderDefaults.colors(thumbColor = BLOOD, activeTrackColor = BLOOD, inactiveTrackColor = Color(0x22FFFFFF), activeTickColor = Color.Transparent, inactiveTickColor = Color.Transparent)
                )
                if (pain >= 7f) Text(
                    "Сильная боль: попробуй НПВП (ибупрофен) и тепло. Если не помогает — к врачу.",
                    style = YType.Body.copy(fontSize = 12.sp), color = Y.Coral
                )

                Text(if (female) "СИМПТОМЫ" else "ЧТО ЕЁ БЕСПОКОИТ", style = YType.Label, color = Y.Text3, modifier = Modifier.padding(top = 12.dp, bottom = 8.dp))
                ChipsMulti(CycleLogic.symptoms, symptoms, CYCLE) { v -> symptoms = if (v in symptoms) symptoms - v else symptoms + v }

                Text("НАСТРОЕНИЕ", style = YType.Label, color = Y.Text3, modifier = Modifier.padding(top = 14.dp))
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    MOODS.forEachIndexed { i, em ->
                        val sel = mood == i + 1
                        Box(
                            Modifier.size(42.dp).clip(CircleShape)
                                .background(if (sel) Y.Rose.copy(alpha = 0.3f) else Color(0x0FFFFFFF))
                                .border(if (sel) 1.5.dp else 0.dp, if (sel) Y.Rose else Color.Transparent, CircleShape)
                                .clickable { mood = if (sel) 0 else i + 1 },
                            contentAlignment = Alignment.Center,
                        ) { Text(em, fontSize = 22.sp) }
                    }
                }

                Text("ЧТО ПОМОГАЛО", style = YType.Label, color = Y.Text3, modifier = Modifier.padding(top = 14.dp, bottom = 8.dp))
                ChipsMulti(CycleLogic.meds, meds, Accent(Y.Violet, Y.Sky)) { v -> meds = if (v in meds) meds - v else meds + v }

                OutlinedTextField(
                    note, { note = it }, placeholder = { Text("Заметка", color = Y.Text3) },
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp), shape = RoundedCornerShape(18.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BLOOD, unfocusedBorderColor = Y.Stroke, focusedTextColor = Y.Text, unfocusedTextColor = Y.Text),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                scope.launch { repo.saveCycleDay(CycleDay(day, flow, pain.toInt(), symptoms.toList(), mood, meds.toList(), note.trim())) }
                onClose()
            }) { Text("Сохранить", color = BLOOD, style = YType.Sub) }
        },
        dismissButton = { TextButton(onClick = onClose) { Text("Отмена", color = Y.Text2) } },
    )
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
