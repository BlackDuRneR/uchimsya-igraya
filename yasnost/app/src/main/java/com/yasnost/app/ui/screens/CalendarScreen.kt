package com.yasnost.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yasnost.app.data.AppState
import com.yasnost.app.data.CheckIn
import com.yasnost.app.data.Repository
import com.yasnost.app.ui.components.Glass
import com.yasnost.app.ui.components.SectionLabel
import com.yasnost.app.ui.components.StatTile
import com.yasnost.app.ui.theme.Y
import com.yasnost.app.ui.theme.YType
import com.yasnost.app.ui.theme.accent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private val monthFmt = DateTimeFormatter.ofPattern("LLLL yyyy", Locale("ru"))
private val dayFmt = DateTimeFormatter.ofPattern("d MMMM, EEEE", Locale("ru"))

@Composable
fun CalendarScreen(s: AppState, repo: Repository, scope: CoroutineScope) {
    var month by remember { mutableStateOf(YearMonth.now()) }
    var editDay by remember { mutableStateOf<Long?>(null) }
    val today = LocalDate.now()

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(18.dp))
        Text("ДНЕВНИК", style = YType.Label, color = Y.Text3)
        Text("Отметки", style = YType.Big, color = Y.Text, modifier = Modifier.padding(top = 4.dp, bottom = 18.dp))

        Glass(Modifier.fillMaxWidth(), padding = 14.dp) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { month = month.minusMonths(1) }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, null, tint = Y.Text2) }
                Text(
                    monthFmt.format(month).replaceFirstChar { it.uppercase() },
                    style = YType.Sub, color = Y.Text, textAlign = TextAlign.Center, modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { month = month.plusMonths(1) }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Y.Text2) }
            }
            Row(Modifier.padding(top = 6.dp, bottom = 6.dp)) {
                listOf("пн", "вт", "ср", "чт", "пт", "сб", "вс").forEach {
                    Text(it, style = YType.Label, color = Y.Text3, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                }
            }
            val first = month.atDay(1)
            val offset = first.dayOfWeek.value - 1
            val total = month.lengthOfMonth()
            val cells = ((offset + total + 6) / 7) * 7
            for (week in 0 until cells / 7) {
                Row {
                    for (d in 0 until 7) {
                        val idx = week * 7 + d - offset + 1
                        Box(Modifier.weight(1f).aspectRatio(1f).padding(3.dp), contentAlignment = Alignment.Center) {
                            if (idx in 1..total) {
                                val date = month.atDay(idx)
                                DayCell(date, s, today) { if (!date.isAfter(today)) editDay = date.toEpochDay() }
                            }
                        }
                    }
                }
            }
            Row(Modifier.padding(top = 10.dp, start = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Legend(Brush.linearGradient(listOf(Y.Teal, Y.Mint)), "Чистый день")
                Legend(Brush.linearGradient(listOf(Y.Amber, Y.Amber)), "Частично")
                Legend(Brush.linearGradient(listOf(Y.Danger, Y.Danger)), "Срыв")
            }
        }

        SectionLabel("Статистика", Modifier.padding(top = 10.dp))
        val streak = checkInStreak(s, today.toEpochDay())
        val last7 = s.checkIns.filter { it.day > today.toEpochDay() - 7 }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile("серия отметок", "$streak", com.yasnost.app.domain.Fmt.days(streak.toLong()), Y.Teal, Modifier.weight(1f))
            StatTile(
                "настроение 7д",
                if (last7.isEmpty()) "—" else MOODS[(last7.map { it.mood }.average().toInt() - 1).coerceIn(0, 4)],
                "", Y.Violet, Modifier.weight(1f)
            )
        }
        SectionLabel("Тяга за 14 дней", Modifier.padding(top = 10.dp))
        Glass(Modifier.fillMaxWidth(), padding = 16.dp) {
            CravingChart(s, today.toEpochDay())
        }

        SectionLabel("Последние заметки", Modifier.padding(top = 10.dp))
        val notes = s.checkIns.filter { it.note.isNotBlank() }.sortedByDescending { it.day }.take(10)
        if (notes.isEmpty()) {
            Text("Здесь появятся твои заметки из ежедневных отметок.", style = YType.Body, color = Y.Text3, modifier = Modifier.padding(start = 4.dp))
        }
        notes.forEach { c ->
            Glass(Modifier.fillMaxWidth().padding(bottom = 8.dp), radius = 20.dp, padding = 14.dp, onClick = { editDay = c.day }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(MOODS[c.mood - 1], fontSize = 22.sp)
                    Column(Modifier.padding(start = 12.dp)) {
                        Text(dayFmt.format(LocalDate.ofEpochDay(c.day)), style = YType.Label, color = Y.Text3)
                        Text(c.note, style = YType.Body, color = Y.Text)
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }

    editDay?.let { d -> CheckInDialog(s, d, repo, scope) { editDay = null } }
}

private fun checkInStreak(s: AppState, today: Long): Int {
    val days = s.checkIns.map { it.day }.toSet()
    var d = if (today in days) today else today - 1
    var n = 0
    while (d in days) { n++; d-- }
    return n
}

@Composable
private fun Legend(b: Brush, text: String) {
    Box(Modifier.size(10.dp).clip(CircleShape).background(b))
    Text(" $text   ", style = YType.Body.copy(fontSize = 12.sp), color = Y.Text3)
}

@Composable
private fun DayCell(date: LocalDate, s: AppState, today: LocalDate, onClick: () -> Unit) {
    val day = date.toEpochDay()
    val c = s.checkIn(day)
    val relapsed = s.relapses.any { java.time.Instant.ofEpochMilli(it.atMillis).atZone(java.time.ZoneId.systemDefault()).toLocalDate() == date }
    val trackedHabits = s.habits.filter { java.time.Instant.ofEpochMilli(it.createdMillis.coerceAtMost(it.startMillis)).atZone(java.time.ZoneId.systemDefault()).toLocalDate() <= date }
    val bg: Brush? = when {
        relapsed -> Brush.linearGradient(listOf(Y.Danger.copy(alpha = 0.8f), Y.Coral.copy(alpha = 0.6f)))
        c != null && trackedHabits.isNotEmpty() && c.cleanHabits.containsAll(trackedHabits.map { it.id }) -> Brush.linearGradient(listOf(Y.Teal, Y.Mint))
        c != null && c.cleanHabits.isNotEmpty() -> Brush.linearGradient(listOf(Y.Amber, Y.Amber.copy(alpha = 0.7f)))
        c != null -> Brush.linearGradient(listOf(Color(0x40FFFFFF), Color(0x30FFFFFF)))
        else -> null
    }
    val isToday = date == today
    val future = date.isAfter(today)
    Box(
        Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(12.dp))
            .then(if (bg != null) Modifier.background(bg) else Modifier.background(Color(0x08FFFFFF)))
            .then(if (isToday) Modifier.border(1.5.dp, Y.Text, RoundedCornerShape(12.dp)) else Modifier)
            .clickable(enabled = !future, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "${date.dayOfMonth}",
            style = YType.Sub.copy(fontSize = 14.sp),
            color = when {
                bg != null && c != null -> Color(0xFF0B0C14)
                future -> Y.Text3.copy(alpha = 0.5f)
                else -> Y.Text2
            }
        )
    }
}

@Composable
private fun CravingChart(s: AppState, today: Long) {
    val days = (13 downTo 0).map { today - it }
    val values = days.map { d -> s.checkIn(d)?.craving }
    Canvas(Modifier.fillMaxWidth().height(110.dp)) {
        val n = days.size
        val gap = 6.dp.toPx()
        val bw = (size.width - gap * (n - 1)) / n
        values.forEachIndexed { i, v ->
            val x = i * (bw + gap)
            val h = if (v == null) 4.dp.toPx() else (size.height * (v.coerceAtLeast(0) / 10f)).coerceAtLeast(6.dp.toPx())
            val brush = if (v == null) Brush.verticalGradient(listOf(Color(0x14FFFFFF), Color(0x14FFFFFF)))
            else Brush.verticalGradient(listOf(Y.Coral, Y.Magenta.copy(alpha = 0.6f)), startY = size.height - h, endY = size.height)
            drawRoundRect(brush, Offset(x, size.height - h), Size(bw, h), CornerRadius(bw / 2, bw / 2))
        }
    }
    Row(Modifier.padding(top = 8.dp)) {
        Text(LocalDate.ofEpochDay(days.first()).dayOfMonth.toString() + " " + LocalDate.ofEpochDay(days.first()).month.getDisplayName(TextStyle.SHORT, Locale("ru")), style = YType.Label, color = Y.Text3, modifier = Modifier.weight(1f))
        Text("сегодня", style = YType.Label, color = Y.Text3)
    }
}

@Composable
fun CheckInDialog(s: AppState, day: Long, repo: Repository, scope: CoroutineScope, onClose: () -> Unit) {
    val existing = s.checkIn(day)
    var mood by remember { mutableIntStateOf(existing?.mood ?: 4) }
    var craving by remember { mutableFloatStateOf((existing?.craving ?: 2).toFloat()) }
    var clean by remember { mutableStateOf(existing?.cleanHabits?.toSet() ?: s.habits.map { it.id }.toSet()) }
    var note by remember { mutableStateOf(existing?.note ?: "") }

    AlertDialog(
        onDismissRequest = onClose,
        containerColor = Y.Surface2,
        shape = RoundedCornerShape(32.dp),
        title = {
            Column {
                Text(if (day == LocalDate.now().toEpochDay()) "Как прошёл день?" else dayFmt.format(LocalDate.ofEpochDay(day)).replaceFirstChar { it.uppercase() }, style = YType.Title, color = Y.Text)
            }
        },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text("НАСТРОЕНИЕ", style = YType.Label, color = Y.Text3)
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    MOODS.forEachIndexed { i, e ->
                        val sel = mood == i + 1
                        Box(
                            Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(if (sel) Y.Violet.copy(alpha = 0.35f) else Color(0x0FFFFFFF))
                                .border(if (sel) 1.5.dp else 0.dp, if (sel) Y.Violet else Color.Transparent, CircleShape)
                                .clickable { mood = i + 1 },
                            contentAlignment = Alignment.Center,
                        ) { Text(e, fontSize = 24.sp) }
                    }
                }
                Text(MOOD_NAMES[mood - 1], style = YType.Body, color = Y.Text2, modifier = Modifier.padding(top = 6.dp))

                Text("ТЯГА: ${craving.toInt()} / 10", style = YType.Label, color = Y.Text3, modifier = Modifier.padding(top = 18.dp))
                Slider(
                    craving, { craving = it }, valueRange = 0f..10f, steps = 9,
                    colors = SliderDefaults.colors(thumbColor = Y.Coral, activeTrackColor = Y.Coral, inactiveTrackColor = Color(0x22FFFFFF), activeTickColor = Color.Transparent, inactiveTickColor = Color.Transparent)
                )

                if (s.habits.isNotEmpty()) {
                    Text("ДЕНЬ ПРОШЁЛ ЧИСТО", style = YType.Label, color = Y.Text3, modifier = Modifier.padding(top = 10.dp, bottom = 4.dp))
                    s.habits.forEach { h ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                            Text(h.title, style = YType.Body, color = Y.Text, modifier = Modifier.weight(1f))
                            Switch(
                                checked = h.id in clean,
                                onCheckedChange = { clean = if (it) clean + h.id else clean - h.id },
                                colors = SwitchDefaults.colors(checkedTrackColor = h.type.accent().a, checkedThumbColor = Color.White)
                            )
                        }
                    }
                }
                OutlinedTextField(
                    note, { note = it },
                    placeholder = { Text("Заметка: что помогло, что было трудно…", color = Y.Text3) },
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Y.Violet, unfocusedBorderColor = Y.Stroke, focusedTextColor = Y.Text, unfocusedTextColor = Y.Text),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                scope.launch { repo.saveCheckIn(CheckIn(day, mood, craving.toInt(), clean.toList(), note.trim())) }
                onClose()
            }) { Text("Сохранить", color = Y.Violet, style = YType.Sub) }
        },
        dismissButton = {
            TextButton(onClick = onClose) { Text("Отмена", color = Y.Text2) }
        },
    )
}
