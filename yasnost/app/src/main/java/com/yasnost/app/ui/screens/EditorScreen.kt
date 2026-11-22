package com.yasnost.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yasnost.app.Nav
import com.yasnost.app.data.AppState
import com.yasnost.app.data.Habit
import com.yasnost.app.data.HabitType
import com.yasnost.app.data.Repository
import com.yasnost.app.ui.components.Chip
import com.yasnost.app.ui.components.Glass
import com.yasnost.app.ui.components.g
import com.yasnost.app.ui.components.GhostButton
import com.yasnost.app.ui.components.GradientButton
import com.yasnost.app.ui.components.SectionLabel
import com.yasnost.app.ui.theme.Y
import com.yasnost.app.ui.theme.YType
import com.yasnost.app.ui.theme.accent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

private fun defaults(t: HabitType) = when (t) {
    HabitType.ALCOHOL -> Triple("Не пью", 0.1, "л")
    HabitType.SMOKING -> Triple("Не курю", 20.0, "сиг.")
    HabitType.CUSTOM -> Triple("Без сладкого", 0.0, "")
}

private fun Double.clean(): String = if (this == 0.0) "" else if (this % 1.0 == 0.0) this.toLong().toString() else this.toString()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(id: String?, preset: String?, s: AppState, nav: Nav, repo: Repository, scope: CoroutineScope) {
    val existing = s.habit(id)
    val zone = ZoneId.systemDefault()
    var type by remember { mutableStateOf(existing?.type ?: preset?.let { HabitType.valueOf(it) } ?: HabitType.entries.firstOrNull { s.settings.tracks(it) } ?: HabitType.CUSTOM) }
    var title by remember { mutableStateOf(existing?.title ?: defaults(type).first) }
    var start by remember { mutableStateOf(LocalDateTime.ofInstant(Instant.ofEpochMilli(existing?.startMillis ?: System.currentTimeMillis()), zone).withSecond(0).withNano(0)) }
    var cost by remember { mutableStateOf(existing?.costPerDay?.clean() ?: if (type == HabitType.SMOKING) "1200" else if (type == HabitType.ALCOHOL) "1700" else "") }
    var units by remember { mutableStateOf(existing?.unitsPerDay?.clean() ?: defaults(type).second.clean()) }
    var unitName by remember { mutableStateOf(existing?.unitName ?: defaults(type).third) }
    var pickDate by remember { mutableStateOf(false) }
    var pickTime by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val acc = type.accent()

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = acc.a, unfocusedBorderColor = Y.Stroke, focusedTextColor = Y.Text, unfocusedTextColor = Y.Text,
        focusedLabelColor = acc.a, unfocusedLabelColor = Y.Text3, cursorColor = acc.a,
    )

    Column(Modifier.fillMaxSize().imePadding()) {
        TopBar(if (existing == null) "Новая привычка" else "Редактировать", { nav.back() })
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            SectionLabel("От чего отказываюсь")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(HabitType.ALCOHOL to "Алкоголь", HabitType.SMOKING to "Курение", HabitType.CUSTOM to "Другое")
                    .filter { (t, _) -> s.settings.tracks(t) || t == existing?.type }.forEach { (t, label) ->
                    Chip(label, type == t, t.accent()) {
                        if (existing == null && type != t) {
                            type = t
                            val d = defaults(t)
                            title = d.first; units = d.second.clean(); unitName = d.third
                            cost = when (t) { HabitType.SMOKING -> "1200"; HabitType.ALCOHOL -> "1700"; else -> "" }
                        } else type = t
                    }
                }
            }

            SectionLabel("Название", Modifier.padding(top = 14.dp))
            OutlinedTextField(title, { title = it }, Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(18.dp), colors = fieldColors)

            SectionLabel(g("Когда бросил", "Когда бросила"), Modifier.padding(top = 14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Glass(Modifier.weight(1.4f), radius = 18.dp, padding = 16.dp, onClick = { pickDate = true }) {
                    Text("ДАТА", style = YType.Label, color = Y.Text3)
                    Text(DateTimeFormatter.ofPattern("d MMM yyyy", Locale("ru")).format(start), style = YType.Sub, color = Y.Text)
                }
                Glass(Modifier.weight(1f), radius = 18.dp, padding = 16.dp, onClick = { pickTime = true }) {
                    Text("ВРЕМЯ", style = YType.Label, color = Y.Text3)
                    Text(DateTimeFormatter.ofPattern("HH:mm").format(start), style = YType.Sub, color = Y.Text)
                }
            }

            SectionLabel("Сколько уходило в день", Modifier.padding(top = 14.dp))
            OutlinedTextField(
                cost, { cost = it.filter { c -> c.isDigit() || c == '.' || c == ',' } }, Modifier.fillMaxWidth(),
                label = { Text("Денег в день, ${s.settings.currency}") }, singleLine = true, shape = RoundedCornerShape(18.dp), colors = fieldColors,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    units, { units = it.filter { c -> c.isDigit() || c == '.' || c == ',' } }, Modifier.weight(1.4f),
                    label = { Text("Количество в день") }, singleLine = true, shape = RoundedCornerShape(18.dp), colors = fieldColors,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
                OutlinedTextField(
                    unitName, { unitName = it.take(8) }, Modifier.weight(1f),
                    label = { Text("Ед.") }, singleLine = true, shape = RoundedCornerShape(18.dp), colors = fieldColors,
                )
            }
            Text(
                when (type) {
                    HabitType.ALCOHOL -> "Например: 0,1 л в день ≈ бутылка пива раз в 5 дней. Считай по среднему."
                    HabitType.SMOKING -> "Например: 20 сиг. — пачка в день."
                    HabitType.CUSTOM -> "Необязательно. Можно оставить пустым."
                },
                style = YType.Body.copy(fontSize = 13.sp), color = Y.Text3, modifier = Modifier.padding(top = 8.dp, start = 4.dp)
            )

            Spacer(Modifier.height(24.dp))
            GradientButton("Сохранить", acc, Modifier.fillMaxWidth()) {
                val h = Habit(
                    id = existing?.id ?: Repository.newId(),
                    type = type,
                    title = title.ifBlank { defaults(type).first },
                    startMillis = start.atZone(zone).toInstant().toEpochMilli().coerceAtMost(System.currentTimeMillis()),
                    costPerDay = cost.replace(',', '.').toDoubleOrNull() ?: 0.0,
                    unitsPerDay = units.replace(',', '.').toDoubleOrNull() ?: 0.0,
                    unitName = unitName.trim(),
                    createdMillis = existing?.createdMillis ?: System.currentTimeMillis(),
                )
                scope.launch { repo.upsertHabit(h) }
                nav.back()
            }
            if (existing != null) {
                Spacer(Modifier.height(10.dp))
                GhostButton("Удалить привычку", Modifier.fillMaxWidth(), color = Y.Danger) { confirmDelete = true }
            }
            Spacer(Modifier.height(28.dp))
            Spacer(Modifier.navigationBarsPadding())
        }
    }

    if (pickDate) {
        val st = rememberDatePickerState(
            initialSelectedDateMillis = start.toLocalDate().atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli(),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis <= System.currentTimeMillis()
            },
        )
        DatePickerDialog(
            onDismissRequest = { pickDate = false },
            confirmButton = {
                TextButton(onClick = {
                    st.selectedDateMillis?.let {
                        val d = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
                        start = start.with(d)
                    }
                    pickDate = false
                }) { Text("OK", color = acc.a) }
            },
            dismissButton = { TextButton(onClick = { pickDate = false }) { Text("Отмена", color = Y.Text2) } },
            colors = DatePickerDefaults.colors(containerColor = Y.Surface2),
        ) {
            DatePicker(
                st, showModeToggle = true,
                colors = DatePickerDefaults.colors(
                    containerColor = Y.Surface2, selectedDayContainerColor = acc.a, todayDateBorderColor = acc.a, todayContentColor = acc.a,
                ),
            )
        }
    }
    if (pickTime) {
        val st = rememberTimePickerState(start.hour, start.minute, is24Hour = true)
        AlertDialog(
            onDismissRequest = { pickTime = false },
            containerColor = Y.Surface2,
            confirmButton = { TextButton(onClick = { start = start.withHour(st.hour).withMinute(st.minute); pickTime = false }) { Text("OK", color = acc.a) } },
            dismissButton = { TextButton(onClick = { pickTime = false }) { Text("Отмена", color = Y.Text2) } },
            text = {
                TimePicker(
                    st,
                    colors = TimePickerDefaults.colors(
                        clockDialColor = Y.Surface, selectorColor = acc.a, timeSelectorSelectedContainerColor = acc.a.copy(alpha = 0.3f),
                        timeSelectorUnselectedContainerColor = Y.Surface, timeSelectorSelectedContentColor = Y.Text,
                    ),
                )
            },
        )
    }
    if (confirmDelete && existing != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            containerColor = Y.Surface2,
            title = { Text("Удалить «${existing.title}»?", color = Y.Text) },
            text = { Text("Счётчик и история попыток будут удалены без возможности восстановления.", color = Y.Text2) },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch { repo.deleteHabit(existing.id) }
                    confirmDelete = false
                    nav.tab(com.yasnost.app.Route.Home)
                }) { Text("Удалить", color = Y.Danger) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Отмена", color = Y.Text2) } },
        )
    }
}

