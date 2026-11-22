package com.yasnost.app.ui.screens

import com.yasnost.app.ui.components.Glyph
import com.yasnost.app.ui.components.GlyphTile
import com.yasnost.app.ui.components.MoodIcon
import com.yasnost.app.ui.components.MoodChoice
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.yasnost.app.data.Gender
import com.yasnost.app.data.Profile
import com.yasnost.app.data.Repository
import com.yasnost.app.ui.components.Glass
import com.yasnost.app.ui.components.GradientButton
import com.yasnost.app.ui.theme.Accent
import com.yasnost.app.ui.theme.Y
import com.yasnost.app.ui.theme.YType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

private val MAIN = Accent(Y.Violet, Y.Magenta)

@Composable
fun Stepper(label: String, value: Int, range: IntRange, suffix: String, onChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(label, style = YType.Body, color = Y.Text, modifier = Modifier.weight(1f))
        Box(Modifier.size(38.dp).clip(CircleShape).background(Color(0x14FFFFFF)).clickable { if (value > range.first) onChange(value - 1) }, contentAlignment = Alignment.Center) {
            Text("−", style = YType.Title, color = Y.Text)
        }
        Text("$value $suffix", style = YType.Sub, color = Y.Text, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 12.dp))
        Box(Modifier.size(38.dp).clip(CircleShape).background(Color(0x14FFFFFF)).clickable { if (value < range.last) onChange(value + 1) }, contentAlignment = Alignment.Center) {
            Text("+", style = YType.Title, color = Y.Text)
        }
    }
}

@Composable
fun GenderCard(title: String, symbol: String, selected: Boolean, acc: Accent, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(26.dp)
    Column(
        modifier
            .clip(shape)
            .then(if (selected) Modifier.background(acc.soft) else Modifier.background(Color(0x0DFFFFFF)))
            .border(if (selected) 2.dp else 1.dp, if (selected) acc.a else Y.Stroke, shape)
            .clickable(onClick = onClick)
            .padding(vertical = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.size(58.dp).clip(CircleShape).background(if (selected) acc.brush else Brush.linearGradient(listOf(Color(0x22FFFFFF), Color(0x11FFFFFF)))), contentAlignment = Alignment.Center) {
            Glyph(symbol, if (selected) Color(0xFF0B0C14) else Y.Text2, 32.dp)
        }
        Text(title, style = YType.Sub, color = if (selected) Y.Text else Y.Text2, modifier = Modifier.padding(top = 10.dp))
    }
}

@Composable
private fun HabitChoice(emoji: String, title: String, quitting: Boolean, yes: String, no: String, acc: Accent, onChange: (Boolean) -> Unit) {
    Glass(Modifier.fillMaxWidth(), radius = 24.dp, padding = 16.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            GlyphTile(emoji, acc, 36.dp)
            Text("   $title", style = YType.Sub, color = Y.Text)
        }
        Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            com.yasnost.app.ui.components.Chip(yes, quitting, acc, Modifier.weight(1f)) { onChange(true) }
            com.yasnost.app.ui.components.Chip(no, !quitting, Accent(Y.Text2, Y.Text3), Modifier.weight(1f)) { onChange(false) }
        }
    }
}

@Composable
private fun ModuleRow(emoji: String, title: String, sub: String, value: Boolean, color: Color, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        GlyphTile(emoji, Accent(color, color.copy(alpha = 0.7f)), 36.dp, dim = !value)
        Column(Modifier.padding(start = 12.dp).weight(1f)) {
            Text(title, style = YType.Sub, color = Y.Text)
            Text(sub, style = YType.Body.copy(fontSize = 12.sp), color = Y.Text3)
        }
        Switch(value, onChange, colors = SwitchDefaults.colors(checkedTrackColor = color, checkedThumbColor = Color.White))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WelcomeScreen(s: AppState, repo: Repository, scope: CoroutineScope) {
    val existing = s.profile.setupDone
    var step by remember { mutableIntStateOf(if (existing) 1 else 0) }
    var quitAlcohol by remember { mutableStateOf(s.settings.trackAlcohol) }
    var quitSmoking by remember { mutableStateOf(s.settings.trackSmoking) }
    var other by remember { mutableStateOf(s.settings.trackOther) }
    var intimacy by remember { mutableStateOf(s.settings.intimacyEnabled) }
    var name by remember { mutableStateOf(s.profile.name) }
    var gender by remember { mutableStateOf<Gender?>(if (s.profile.setupDone) s.profile.gender else null) }
    var partner by remember { mutableStateOf(s.profile.partnerName) }
    var track by remember { mutableStateOf(s.cycleSettings.enabled) }
    var lastStart by remember { mutableStateOf<Long?>(null) }
    var cycleLen by remember { mutableIntStateOf(s.cycleSettings.cycleLength) }
    var periodLen by remember { mutableIntStateOf(s.cycleSettings.periodLength) }
    var pickDate by remember { mutableStateOf(false) }
    val female = gender == Gender.FEMALE
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = Y.Violet, unfocusedBorderColor = Y.Stroke, focusedTextColor = Y.Text, unfocusedTextColor = Y.Text,
        focusedLabelColor = Y.Violet, unfocusedLabelColor = Y.Text3, cursorColor = Y.Violet,
    )

    fun finish() {
        val today = LocalDate.now().toEpochDay()
        scope.launch {
            repo.update(refreshWidgets = true) { st ->
                val days = lastStart?.let { ls -> (ls..minOf(ls + periodLen - 1, today)).map { CycleDay(it, flow = if (it - ls <= 1) Flow.MEDIUM else Flow.LIGHT) } } ?: emptyList()
                st.copy(
                    settings = st.settings.copy(trackAlcohol = quitAlcohol, trackSmoking = quitSmoking, trackOther = other, intimacyEnabled = intimacy, modulesChosen = true),
                    profile = Profile(name.trim(), gender ?: Gender.MALE, partner.trim(), setupDone = true),
                    partners = if (partner.isNotBlank() && st.partners.isEmpty()) listOf(com.yasnost.app.data.Partner(com.yasnost.app.data.MAIN_PARTNER, partner.trim(), System.currentTimeMillis())) else st.partners,
                    cycleSettings = st.cycleSettings.copy(enabled = track, cycleLength = cycleLen, periodLength = periodLen),
                    cycle = st.cycle.filterNot { c -> days.any { it.day == c.day } } + days,
                    periodClosedStart = lastStart?.takeIf { it + periodLen - 1 < today } ?: st.periodClosedStart,
                )
            }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(horizontal = 24.dp)
    ) {
        Spacer(Modifier.height(36.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(if (existing) 1 else 3) { i0 -> val i = if (existing) 1 else i0; Box(Modifier.height(4.dp).size(width = if (i == step) 28.dp else 12.dp, height = 4.dp).clip(RoundedCornerShape(50)).background(if (i <= step) Y.Violet else Y.Stroke)) }
        }
        Spacer(Modifier.height(24.dp))
        if (step == 0) {
            androidx.compose.foundation.Image(androidx.compose.ui.res.painterResource(com.yasnost.app.R.drawable.logo_harmony), null, modifier = Modifier.size(120.dp).padding(bottom = 12.dp))
            Text("Добро пожаловать\nв Harmony", style = YType.Big.copy(fontSize = 38.sp, lineHeight = 42.sp), color = Y.Text)
            Text(
                "Трезвость, свобода от сигарет, дневник, близость и цикл — всё в одном месте и только на твоём телефоне.",
                style = YType.Body, color = Y.Text2, modifier = Modifier.padding(top = 10.dp, bottom = 26.dp)
            )
            Text("КАК ТЕБЯ ЗОВУТ?", style = YType.Label, color = Y.Text3, modifier = Modifier.padding(bottom = 8.dp))
            OutlinedTextField(name, { name = it.take(24) }, Modifier.fillMaxWidth(), singleLine = true, placeholder = { Text("Имя", color = Y.Text3) }, shape = RoundedCornerShape(18.dp), colors = fieldColors)
            Text("ПОЛ", style = YType.Label, color = Y.Text3, modifier = Modifier.padding(top = 22.dp, bottom = 10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                GenderCard("Мужской", "♂", gender == Gender.MALE, Accent(Y.Sky, Y.Violet), Modifier.weight(1f)) { gender = Gender.MALE }
                GenderCard("Женский", "♀", gender == Gender.FEMALE, Accent(Y.Rose, Y.Magenta), Modifier.weight(1f)) { gender = Gender.FEMALE }
            }
            Text(
                "Пол нужен, чтобы правильно обращаться к тебе и показывать подходящие медицинские факты.",
                style = YType.Body.copy(fontSize = 12.sp), color = Y.Text3, modifier = Modifier.padding(top = 10.dp)
            )
            Spacer(Modifier.height(28.dp))
            GradientButton(if (gender == null) "Выбери пол" else "Далее", if (gender == null) Accent(Y.Text3, Y.Text3) else MAIN, Modifier.fillMaxWidth()) {
                if (gender != null) step = 1
            }
        } else if (step == 1) {
            Text(if (existing) "Что тебе актуально?" else "Что тебе нужно?", style = YType.Big, color = Y.Text)
            Text(
                "Выбери разделы — остальные скроем, чтобы ничего лишнего. Всё можно поменять в «Ещё».",
                style = YType.Body, color = Y.Text2, modifier = Modifier.padding(top = 10.dp, bottom = 18.dp)
            )
            HabitChoice("🍷", "Алкоголь", quitAlcohol, if (female) "Бросаю / бросила" else "Бросаю / бросил", "Не пью вообще", Accent(Y.Violet, Y.Magenta)) { quitAlcohol = it }
            Spacer(Modifier.height(10.dp))
            HabitChoice("🚬", "Курение", quitSmoking, if (female) "Бросаю / бросила" else "Бросаю / бросил", "Не курю вообще", Accent(Y.Teal, Y.Mint)) { quitSmoking = it }
            Spacer(Modifier.height(10.dp))
            Glass(Modifier.fillMaxWidth(), radius = 24.dp, padding = 16.dp) {
                ModuleRow("✨", "Другие привычки", "Сладкое, соцсети, игры — что угодно", other, Y.Amber) { other = it }
                ModuleRow("💞", "Календарь близости", "Отметки, статистика, здоровье", intimacy, Y.Rose) { intimacy = it }
                ModuleRow("🌸", if (female) "Мой цикл" else "Цикл девушки", if (female) "Месячные, фертильные дни, боль, ПМС" else "Когда у неё месячные и как поддержать", track, BLOOD) { track = it }
            }
            Spacer(Modifier.height(24.dp))
            GradientButton(if (existing || !track) "Готово" else "Далее", MAIN, Modifier.fillMaxWidth()) {
                if (existing) {
                    scope.launch {
                        repo.update(refreshWidgets = true) { st ->
                            st.copy(
                                settings = st.settings.copy(trackAlcohol = quitAlcohol, trackSmoking = quitSmoking, trackOther = other, intimacyEnabled = intimacy, modulesChosen = true),
                                cycleSettings = st.cycleSettings.copy(enabled = track),
                            )
                        }
                    }
                } else if (track) step = 2 else finish()
            }
            if (!existing) TextButton(onClick = { step = 0 }, modifier = Modifier.fillMaxWidth()) { Text("Назад", color = Y.Text2) }
        } else {
            Text(if (female) "Твой цикл" else "Цикл твоей девушки", style = YType.Big, color = Y.Text)
            Text(
                if (female) "Календарь покажет, когда ждать месячные, фертильные дни и ПМС, подскажет, как справиться с болью, и напомнит заранее."
                else "Узнаешь заранее, когда у неё начнутся месячные и ПМС, и получишь подсказки, как поддержать и помочь при боли.",
                style = YType.Body, color = Y.Text2, modifier = Modifier.padding(top = 10.dp, bottom = 20.dp)
            )
            Glass(Modifier.fillMaxWidth(), radius = 24.dp, padding = 16.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(if (female) "Отслеживать мой цикл" else "Отслеживать её цикл", style = YType.Sub, color = Y.Text, modifier = Modifier.weight(1f))
                    Switch(track, { track = it }, colors = SwitchDefaults.colors(checkedTrackColor = BLOOD, checkedThumbColor = Color.White))
                }
                if (female) {
                    OutlinedTextField(partner, { partner = it.take(24) }, Modifier.fillMaxWidth().padding(top = 12.dp), singleLine = true,
                        label = { Text("Имя парня (необязательно)") }, shape = RoundedCornerShape(16.dp), colors = fieldColors)
                }
                if (track) {
                    if (!female) {
                        OutlinedTextField(partner, { partner = it.take(24) }, Modifier.fillMaxWidth().padding(top = 12.dp), singleLine = true,
                            label = { Text("Её имя (необязательно)") }, shape = RoundedCornerShape(16.dp), colors = fieldColors)
                    }
                    Text("ПЕРВЫЙ ДЕНЬ ПОСЛЕДНИХ МЕСЯЧНЫХ", style = YType.Label, color = Y.Text3, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
                    Glass(Modifier.fillMaxWidth(), radius = 16.dp, padding = 14.dp, onClick = { pickDate = true }) {
                        Text(lastStart?.let { shortDayFmt.format(LocalDate.ofEpochDay(it)) } ?: "Выбрать дату (можно позже)", style = YType.Sub, color = if (lastStart != null) Y.Text else Y.Text2)
                    }
                    Spacer(Modifier.height(10.dp))
                    Stepper("Обычная длина цикла", cycleLen, 20..45, "дн") { cycleLen = it }
                    Stepper("Месячные длятся", periodLen, 2..10, "дн") { periodLen = it }
                    Text("Если не знаешь — оставь 28 и 5, приложение уточнит по твоим отметкам.", style = YType.Body.copy(fontSize = 12.sp), color = Y.Text3, modifier = Modifier.padding(top = 4.dp))
                }
            }
            Spacer(Modifier.height(24.dp))
            GradientButton("Начать", MAIN, Modifier.fillMaxWidth()) { finish() }
            TextButton(onClick = { step = 1 }, modifier = Modifier.fillMaxWidth()) { Text("Назад", color = Y.Text2) }
        }
        Spacer(Modifier.height(24.dp))
        Text(
            "Разработчик: " + androidx.compose.ui.res.stringResource(com.yasnost.app.R.string.developer),
            style = YType.Body.copy(fontSize = 12.sp), color = Y.Text3, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(16.dp))
        Spacer(Modifier.navigationBarsPadding())
    }

    if (pickDate) {
        val st = rememberDatePickerState(
            initialSelectedDateMillis = LocalDate.now().atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli(),
            selectableDates = object : SelectableDates { override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis <= System.currentTimeMillis() },
        )
        DatePickerDialog(
            onDismissRequest = { pickDate = false },
            confirmButton = {
                TextButton(onClick = {
                    st.selectedDateMillis?.let { lastStart = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().toEpochDay() }
                    pickDate = false
                }) { Text("OK", color = BLOOD) }
            },
            dismissButton = { TextButton(onClick = { pickDate = false }) { Text("Отмена", color = Y.Text2) } },
            colors = DatePickerDefaults.colors(containerColor = Y.Surface2),
        ) {
            DatePicker(st, colors = DatePickerDefaults.colors(containerColor = Y.Surface2, selectedDayContainerColor = BLOOD, todayDateBorderColor = BLOOD, todayContentColor = BLOOD))
        }
    }
}
