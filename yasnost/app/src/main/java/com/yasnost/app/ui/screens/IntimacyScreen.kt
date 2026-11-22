package com.yasnost.app.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.yasnost.app.data.AppState
import com.yasnost.app.data.HabitType
import com.yasnost.app.data.IntimacyEntry
import com.yasnost.app.data.Repository
import com.yasnost.app.data.SexStatus
import com.yasnost.app.domain.Fmt
import com.yasnost.app.domain.IntimacyLogic
import com.yasnost.app.ui.components.Chip
import com.yasnost.app.ui.components.Glass
import com.yasnost.app.ui.components.g
import com.yasnost.app.ui.components.GradientButton
import com.yasnost.app.ui.components.MonthCalendar
import com.yasnost.app.ui.components.SectionLabel
import com.yasnost.app.ui.components.Session
import com.yasnost.app.ui.components.StatTile
import com.yasnost.app.ui.components.authenticate
import com.yasnost.app.ui.theme.Accent
import com.yasnost.app.ui.theme.Y
import com.yasnost.app.ui.theme.YType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

private val ROSE = Accent(Y.Rose, Y.Magenta)
private val DESIRE = listOf("Нет", "Слабое", "Ниже среднего", "Обычное", "Сильное", "Очень сильное")

@Composable
fun IntimacyContent(s: AppState, repo: Repository, scope: CoroutineScope) {
    var unlocked by remember { mutableStateOf(!s.settings.intimacyLock || Session.intimacyUnlocked) }
    val ctx = LocalContext.current
    if (!unlocked) {
        Glass(Modifier.fillMaxWidth(), padding = 24.dp) {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(64.dp).clip(CircleShape).background(ROSE.brush), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Lock, null, tint = Color(0xFF0B0C14), modifier = Modifier.size(28.dp))
                }
                Text("Раздел защищён", style = YType.Title, color = Y.Text, modifier = Modifier.padding(top = 14.dp))
                Text("Открывается по отпечатку, лицу или PIN-коду телефона.", style = YType.Body, color = Y.Text2, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 6.dp, bottom = 18.dp))
                GradientButton("Открыть", ROSE, Modifier.fillMaxWidth()) {
                    (ctx as? FragmentActivity)?.let { a -> authenticate(a, "Близость") { Session.intimacyUnlocked = true; unlocked = true } }
                }
            }
        }
        return
    }

    var month by remember { mutableStateOf(YearMonth.now()) }
    var editDay by remember { mutableStateOf<Long?>(null) }
    val today = LocalDate.now()
    val t = today.toEpochDay()
    val st = IntimacyLogic.stats(s, t)

    // Быстрая отметка сегодняшнего дня
    val todayEntry = s.intimacyOn(t)
    Glass(Modifier.fillMaxWidth(), radius = 26.dp, onClick = { editDay = t }, tint = Y.Rose.copy(alpha = 0.05f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).clip(RoundedCornerShape(15.dp)).background(ROSE.brush), contentAlignment = Alignment.Center) {
                Icon(if (todayEntry == null) Icons.Filled.Add else Icons.Filled.Check, null, tint = Color(0xFF0B0C14))
            }
            Column(Modifier.padding(start = 14.dp).weight(1f)) {
                Text(if (todayEntry == null) "Отметить сегодня" else "Сегодня отмечено", style = YType.Sub, color = Y.Text)
                Text(todayEntry?.let { summary(it) } ?: "Близость, мастурбация, желание", style = YType.Body.copy(fontSize = 13.sp), color = Y.Text2, maxLines = 1)
            }
        }
    }
    Spacer(Modifier.height(14.dp))

    MonthCalendar(month, { month = it }, footer = {
        Column(Modifier.padding(top = 10.dp, start = 6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Legend(Brush.linearGradient(listOf(Y.Teal, Y.Mint)), "Защищённый")
                Legend(Brush.linearGradient(listOf(Y.Coral, Y.Rose)), "Незащищённый")
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                Legend(Brush.linearGradient(listOf(Y.Violet, Y.Violet)), "Мастурбация (точка)")
                Legend(Brush.linearGradient(listOf(Color(0x40FFFFFF), Color(0x40FFFFFF))), "Ничего")
            }
        }
    }) { date ->
        val e = s.intimacyOn(date.toEpochDay())
        val bg = when (e?.sex) {
            SexStatus.PROTECTED -> Brush.linearGradient(listOf(Y.Teal, Y.Mint))
            SexStatus.UNPROTECTED -> Brush.linearGradient(listOf(Y.Coral, Y.Rose))
            SexStatus.NONE -> Brush.linearGradient(listOf(Color(0x30FFFFFF), Color(0x22FFFFFF)))
            null -> null
        }
        CalendarDay(date, today, bg, darkText = e != null && e.sex != SexStatus.NONE, dot = if (e?.masturbation == true) (if (e.sex == SexStatus.NONE) Y.Violet else Color(0xFF0B0C14)) else null) {
            editDay = date.toEpochDay()
        }
    }

    // Обследование после незащищённого контакта
    val lu = st.lastUnprotectedDay
    if (lu != null && t - lu <= 90 && s.lastStiTestDay < lu + 14) {
        SectionLabel("Здоровье после незащищённого контакта", Modifier.padding(top = 14.dp))
        Glass(Modifier.fillMaxWidth(), radius = 24.dp, padding = 16.dp, tint = Y.Coral.copy(alpha = 0.06f)) {
            Text("Контакт ${shortDayFmt.format(LocalDate.ofEpochDay(lu))} · ${t - lu} ${Fmt.days(t - lu)} назад", style = YType.Label, color = Y.Coral)
            IntimacyLogic.testPlan.forEachIndexed { i, step ->
                val date = LocalDate.ofEpochDay(lu + step.dayOffset)
                val passed = if (i == 0) t - lu > 3 else t >= lu + step.dayOffset
                val current = if (i == 0) t - lu <= 3 else passed && (IntimacyLogic.testPlan.getOrNull(i + 1)?.let { t < lu + it.dayOffset } ?: true)
                if (i == 0 && t - lu > 3) return@forEachIndexed
                Row(Modifier.padding(top = 12.dp)) {
                    Box(
                        Modifier.size(24.dp).clip(CircleShape)
                            .then(if (current) Modifier.background(Brush.linearGradient(listOf(Y.Coral, Y.Rose))) else Modifier.border(1.5.dp, if (passed) Y.Coral else Y.Stroke, CircleShape)),
                        contentAlignment = Alignment.Center,
                    ) { if (current) Box(Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF0B0C14))) }
                    Column(Modifier.padding(start = 12.dp)) {
                        Text(if (i == 0) step.title else "${step.title} · с ${shortDayFmt.format(date)}", style = YType.Sub.copy(fontSize = 15.sp), color = if (current) Y.Text else Y.Text2)
                        Text(step.text, style = YType.Body.copy(fontSize = 13.sp), color = Y.Text3)
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            GradientButton(g("Я сдал анализы", "Я сдала анализы"), Accent(Y.Coral, Y.Rose), Modifier.fillMaxWidth()) {
                scope.launch { repo.update(false) { it.copy(lastStiTestDay = t) } }
            }
            Text("Источники: NHS, CDC (nPEP 2025), ВОЗ. Это ориентир, а не диагноз — точный план подскажет врач.", style = YType.Body.copy(fontSize = 11.sp), color = Y.Text3, modifier = Modifier.padding(top = 8.dp))
        }
    }

    SectionLabel("Последние 30 дней", Modifier.padding(top = 14.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        StatTile("контакты", "${st.contacts30}", "", Y.Rose, Modifier.weight(1f))
        StatTile(
            "защищённых",
            if (st.contacts30 == 0) "—" else "${st.protected30 * 100 / st.contacts30}", if (st.contacts30 == 0) "" else "%",
            Y.Teal, Modifier.weight(1f)
        )
    }
    Spacer(Modifier.height(10.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        StatTile("мастурбация", "${st.masturbationDays30}", Fmt.days(st.masturbationDays30.toLong()), Y.Violet, Modifier.weight(1f))
        StatTile("желание", st.avgDesire30?.let { "%.1f".format(it).replace('.', ',') } ?: "—", if (st.avgDesire30 != null) "из 5" else "", Y.Magenta, Modifier.weight(1f))
    }
    Spacer(Modifier.height(10.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        StatTile("без мастурбации", st.noMasturbationStreak?.toString() ?: "—", st.noMasturbationStreak?.let { Fmt.days(it) } ?: "", Y.Violet, Modifier.weight(1f))
        StatTile("рекорд", "${st.bestNoMasturbationStreak}", Fmt.days(st.bestNoMasturbationStreak), Y.Violet, Modifier.weight(1f))
    }

    if (st.loggedDays >= 3) {
        SectionLabel("12 недель", Modifier.padding(top = 14.dp))
        Glass(Modifier.fillMaxWidth(), padding = 16.dp) { WeeksChart(s, t) }
        SectionLabel("Желание за 30 дней", Modifier.padding(top = 14.dp))
        Glass(Modifier.fillMaxWidth(), padding = 16.dp) { DesireChart(s, t) }
    }

    // Связь с трезвостью
    val alcoholHabit = s.habits.firstOrNull { it.type == HabitType.ALCOHOL }
    if (st.alcoholContacts > 0 || alcoholHabit != null) {
        SectionLabel("Трезвость и близость", Modifier.padding(top = 14.dp))
        Glass(Modifier.fillMaxWidth(), radius = 24.dp, padding = 16.dp) {
            if (st.alcoholContacts > 0) {
                val sober = st.totalContacts - st.alcoholContacts
                val soberUnprot = (st.totalContacts - st.totalProtected) - st.alcoholUnprotected
                Text(
                    "После алкоголя без защиты: ${st.alcoholUnprotected * 100 / st.alcoholContacts}% контактов" +
                        if (sober > 0) ", на трезвую голову — ${soberUnprot.coerceAtLeast(0) * 100 / sober}%." else ".",
                    style = YType.Body, color = Y.Text,
                )
            }
            Text(
                "Алкоголь связан с более редким использованием презервативов (анализ 50 исследований), а трезвость за 3 месяца улучшает эрекцию у большинства мужчин с проблемами на фоне алкоголя.",
                style = YType.Body.copy(fontSize = 13.sp), color = Y.Text2, modifier = Modifier.padding(top = if (st.alcoholContacts > 0) 8.dp else 0.dp)
            )
        }
    }

    SectionLabel("Что говорит наука", Modifier.padding(top = 14.dp))
    IntimacyLogic.facts.filter { it.fits(com.yasnost.app.ui.components.LocalFemale.current) }.forEach { f -> FactCard(f.text, f.source, ROSE) }
    Text(
        "Здесь нет «правильной» нормы. Календарь нужен, чтобы лучше понимать себя, беречь здоровье и замечать связь с трезвостью. Все записи хранятся только на этом телефоне.",
        style = YType.Body.copy(fontSize = 12.sp), color = Y.Text3, modifier = Modifier.padding(top = 6.dp, start = 4.dp)
    )

    editDay?.let { d -> IntimacyDialog(s, d, repo, scope) { editDay = null } }
}

private fun summary(e: IntimacyEntry): String = buildList {
    when (e.sex) {
        SexStatus.NONE -> add("без близости")
        SexStatus.PROTECTED -> add("защищённый" + if (e.sexCount > 1) " ×${e.sexCount}" else "")
        SexStatus.UNPROTECTED -> add("незащищённый" + if (e.sexCount > 1) " ×${e.sexCount}" else "")
    }
    if (e.masturbation) add("мастурбация")
    add("желание ${e.desire}/5")
}.joinToString(" · ")

@Composable
fun FactCard(text: String, source: String, acc: Accent) {
    var open by remember { mutableStateOf(false) }
    Glass(Modifier.fillMaxWidth().padding(bottom = 8.dp).animateContentSize(), radius = 20.dp, padding = 14.dp, onClick = { open = !open }) {
        Row {
            Box(Modifier.padding(top = 7.dp).size(6.dp).clip(CircleShape).background(acc.brush))
            Text(text, style = YType.Body.copy(fontSize = 14.sp), color = Y.Text, maxLines = if (open) Int.MAX_VALUE else 3, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis, modifier = Modifier.padding(start = 10.dp))
        }
        if (open) Text("Источник: $source", style = YType.Body.copy(fontSize = 12.sp), color = acc.a, modifier = Modifier.padding(top = 8.dp, start = 16.dp))
    }
}

@Composable
private fun WeeksChart(s: AppState, today: Long) {
    // неделя заканчивается сегодня
    val weeks = (11 downTo 0).map { w -> (today - w * 7 - 6)..(today - w * 7) }
    val data = weeks.map { r ->
        val es = s.intimacy.filter { it.day in r }
        Triple(
            es.filter { it.sex == SexStatus.PROTECTED }.sumOf { it.sexCount },
            es.filter { it.sex == SexStatus.UNPROTECTED }.sumOf { it.sexCount },
            es.count { it.masturbation },
        )
    }
    val max = data.maxOf { maxOf(it.first + it.second, it.third) }.coerceAtLeast(3)
    Canvas(Modifier.fillMaxWidth().height(120.dp)) {
        val n = weeks.size
        val gap = 8.dp.toPx()
        val gw = (size.width - gap * (n - 1)) / n
        val bw = gw / 2 - 1.dp.toPx()
        data.forEachIndexed { i, (p, u, m) ->
            val x = i * (gw + gap)
            val hp = size.height * p / max
            val hu = size.height * u / max
            val hm = size.height * m / max
            val r = CornerRadius(bw / 2, bw / 2)
            drawRoundRect(Color(0x12FFFFFF), Offset(x, size.height - 3.dp.toPx()), Size(gw, 3.dp.toPx()), r)
            if (hp > 0) drawRoundRect(Brush.verticalGradient(listOf(Y.Mint, Y.Teal)), Offset(x, size.height - hp), Size(bw, hp), r)
            if (hu > 0) drawRoundRect(Brush.verticalGradient(listOf(Y.Rose, Y.Coral)), Offset(x, size.height - hp - hu), Size(bw, hu), r)
            if (hm > 0) drawRoundRect(Brush.verticalGradient(listOf(Y.Violet, Y.Violet.copy(alpha = 0.5f))), Offset(x + bw + 2.dp.toPx(), size.height - hm), Size(bw, hm), r)
        }
    }
    Row(Modifier.padding(top = 8.dp)) {
        Text("12 нед. назад", style = YType.Label, color = Y.Text3, modifier = Modifier.weight(1f))
        Text("эта неделя", style = YType.Label, color = Y.Text3)
    }
    Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Legend(Brush.linearGradient(listOf(Y.Teal, Y.Mint)), "Защищ.")
        Legend(Brush.linearGradient(listOf(Y.Coral, Y.Rose)), "Незащ.")
        Legend(Brush.linearGradient(listOf(Y.Violet, Y.Violet)), "Мастурб.")
    }
}

@Composable
private fun DesireChart(s: AppState, today: Long) {
    val pts = (29 downTo 0).map { today - it }.mapIndexedNotNull { i, d -> s.intimacyOn(d)?.let { i to it.desire } }
    Canvas(Modifier.fillMaxWidth().height(90.dp)) {
        val w = size.width; val h = size.height
        for (lvl in 0..5) drawLine(Color(0x0DFFFFFF), Offset(0f, h - h * lvl / 5f), Offset(w, h - h * lvl / 5f))
        if (pts.isEmpty()) return@Canvas
        val path = Path()
        pts.forEachIndexed { k, (i, v) ->
            val x = w * i / 29f; val y = h - h * v / 5f
            if (k == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, Brush.horizontalGradient(listOf(Y.Rose, Y.Magenta)), style = Stroke(3.dp.toPx(), cap = StrokeCap.Round))
        pts.forEach { (i, v) -> drawCircle(Y.Magenta, 3.5.dp.toPx(), Offset(w * i / 29f, h - h * v / 5f)) }
    }
    Row(Modifier.padding(top = 8.dp)) {
        Text("30 дней назад", style = YType.Label, color = Y.Text3, modifier = Modifier.weight(1f))
        Text("сегодня", style = YType.Label, color = Y.Text3)
    }
}

@Composable
private fun IntimacyDialog(s: AppState, day: Long, repo: Repository, scope: CoroutineScope, onClose: () -> Unit) {
    val e = s.intimacyOn(day)
    var sex by remember { mutableStateOf(e?.sex ?: SexStatus.NONE) }
    var count by remember { mutableIntStateOf(e?.sexCount ?: 1) }
    var mast by remember { mutableStateOf(e?.masturbation ?: false) }
    var desire by remember { mutableFloatStateOf((e?.desire ?: 3).toFloat()) }
    var alcohol by remember { mutableStateOf(e?.alcohol ?: false) }
    var note by remember { mutableStateOf(e?.note ?: "") }
    val switchColors = SwitchDefaults.colors(checkedTrackColor = Y.Rose, checkedThumbColor = Color.White)

    AlertDialog(
        onDismissRequest = onClose,
        containerColor = Y.Surface2,
        shape = RoundedCornerShape(32.dp),
        title = { Text(dayFmt.format(LocalDate.ofEpochDay(day)).replaceFirstChar { it.uppercase() }, style = YType.Title, color = Y.Text) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text("ПОЛОВОЙ КОНТАКТ", style = YType.Label, color = Y.Text3)
                Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Chip("Не было", sex == SexStatus.NONE, Accent(Y.Text2, Y.Text3)) { sex = SexStatus.NONE }
                    Chip("Защищ.", sex == SexStatus.PROTECTED, Accent(Y.Teal, Y.Mint)) { sex = SexStatus.PROTECTED }
                    Chip("Без защ.", sex == SexStatus.UNPROTECTED, Accent(Y.Coral, Y.Rose)) { sex = SexStatus.UNPROTECTED }
                }
                if (sex != SexStatus.NONE) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 10.dp)) {
                        Text("Сколько раз", style = YType.Body, color = Y.Text, modifier = Modifier.weight(1f))
                        IconButton(onClick = { if (count > 1) count-- }) { Text("−", style = YType.Title, color = Y.Text2) }
                        Text("$count", style = YType.Title, color = Y.Text)
                        IconButton(onClick = { if (count < 9) count++ }) { Text("+", style = YType.Title, color = Y.Text2) }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 12.dp)) {
                    Text("Мастурбация", style = YType.Body, color = Y.Text, modifier = Modifier.weight(1f))
                    Switch(mast, { mast = it }, colors = switchColors)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Был алкоголь", style = YType.Body, color = Y.Text)
                        Text("в этот день", style = YType.Body.copy(fontSize = 12.sp), color = Y.Text3)
                    }
                    Switch(alcohol, { alcohol = it }, colors = switchColors)
                }
                Text("ЖЕЛАНИЕ: ${DESIRE[desire.toInt()]}", style = YType.Label, color = Y.Text3, modifier = Modifier.padding(top = 14.dp))
                Slider(
                    desire, { desire = it }, valueRange = 0f..5f, steps = 4,
                    colors = SliderDefaults.colors(thumbColor = Y.Rose, activeTrackColor = Y.Rose, inactiveTrackColor = Color(0x22FFFFFF), activeTickColor = Color.Transparent, inactiveTickColor = Color.Transparent)
                )
                if (sex == SexStatus.UNPROTECTED) {
                    Text(
                        "Если партнёр новый или его статус неизвестен — посмотри план обследования под календарём. ПКП от ВИЧ эффективна только в первые 72 часа.",
                        style = YType.Body.copy(fontSize = 12.sp), color = Y.Coral, modifier = Modifier.padding(top = 4.dp)
                    )
                }
                OutlinedTextField(
                    note, { note = it }, placeholder = { Text("Заметка (видна только тебе)", color = Y.Text3) },
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp), shape = RoundedCornerShape(18.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Y.Rose, unfocusedBorderColor = Y.Stroke, focusedTextColor = Y.Text, unfocusedTextColor = Y.Text),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                scope.launch { repo.saveIntimacy(IntimacyEntry(day, sex, if (sex == SexStatus.NONE) 0 else count, mast, desire.toInt(), alcohol, note.trim())) }
                onClose()
            }) { Text("Сохранить", color = Y.Rose, style = YType.Sub) }
        },
        dismissButton = {
            Row {
                if (e != null) TextButton(onClick = { scope.launch { repo.deleteIntimacy(day) }; onClose() }) { Text("Удалить", color = Y.Danger) }
                TextButton(onClick = onClose) { Text("Отмена", color = Y.Text2) }
            }
        },
    )
}
