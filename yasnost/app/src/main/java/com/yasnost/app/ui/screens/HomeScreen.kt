package com.yasnost.app.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yasnost.app.Nav
import com.yasnost.app.Route
import com.yasnost.app.data.AppState
import com.yasnost.app.data.Habit
import com.yasnost.app.data.HabitType
import com.yasnost.app.data.Repository
import com.yasnost.app.domain.Achievements
import com.yasnost.app.domain.Fmt
import com.yasnost.app.domain.Quotes
import com.yasnost.app.domain.stats
import com.yasnost.app.domain.unitsLabel
import com.yasnost.app.domain.verb
import com.yasnost.app.ui.components.Glass
import com.yasnost.app.ui.components.GradientButton
import com.yasnost.app.ui.components.HabitBadge
import com.yasnost.app.ui.components.SectionLabel
import com.yasnost.app.ui.components.rememberNow
import com.yasnost.app.ui.theme.Accent
import com.yasnost.app.ui.theme.Y
import com.yasnost.app.ui.theme.YType
import com.yasnost.app.ui.theme.accent
import kotlinx.coroutines.CoroutineScope
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

val ruDate: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy, HH:mm", Locale("ru"))
val ruDay: DateTimeFormatter = DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale("ru"))

fun Long.fmtDate(): String = ruDate.format(Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()))

@Composable
fun HomeScreen(s: AppState, nav: Nav, repo: Repository, scope: CoroutineScope) {
    val now = rememberNow()
    val today = LocalDate.now()
    var showCheckIn by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(18.dp))
        Text(ruDay.format(today).replaceFirstChar { it.uppercase() }, style = YType.Label, color = Y.Text3)
        Text("Ясность", style = YType.Big.copy(fontSize = 40.sp), color = Y.Text, modifier = Modifier.padding(top = 4.dp))
        Text(Quotes.today(today.toEpochDay()), style = YType.Body, color = Y.Text2, modifier = Modifier.padding(top = 6.dp, bottom = 20.dp))

        if (s.habits.isEmpty()) {
            Onboarding(nav)
        } else {
            s.habits.forEach { h ->
                HabitCard(h, now, s.settings.currency) { nav.go(Route.Habit(h.id)) }
                Spacer(Modifier.height(14.dp))
            }
            CheckInPrompt(s, today.toEpochDay()) { showCheckIn = true }
            Spacer(Modifier.height(14.dp))
            Totals(s, now)
            Spacer(Modifier.height(14.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .clickable { nav.go(Route.Editor(null)) }
                    .padding(16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.Add, null, tint = Y.Text2)
                Text("  Добавить привычку", style = YType.Sub, color = Y.Text2)
            }
        }
        Spacer(Modifier.height(24.dp))
    }

    if (showCheckIn) CheckInDialog(s, today.toEpochDay(), repo, scope) { showCheckIn = false }
}

@Composable
private fun Onboarding(nav: Nav) {
    Glass(Modifier.fillMaxWidth()) {
        Text("С чего начнём?", style = YType.Title, color = Y.Text)
        Text(
            "Отметь, от чего ты отказался, и укажи, когда. Можно добавить несколько — например, алкоголь и курение.",
            style = YType.Body, color = Y.Text2, modifier = Modifier.padding(top = 6.dp, bottom = 18.dp)
        )
        GradientButton("Я бросил пить", HabitType.ALCOHOL.accent(), Modifier.fillMaxWidth()) { nav.go(Route.Editor(null, HabitType.ALCOHOL.name)) }
        Spacer(Modifier.height(10.dp))
        GradientButton("Я бросил курить", HabitType.SMOKING.accent(), Modifier.fillMaxWidth()) { nav.go(Route.Editor(null, HabitType.SMOKING.name)) }
        Spacer(Modifier.height(10.dp))
        GradientButton("Другая привычка", HabitType.CUSTOM.accent(), Modifier.fillMaxWidth()) { nav.go(Route.Editor(null, HabitType.CUSTOM.name)) }
    }
}

@Composable
fun HabitCard(h: Habit, now: Long, currency: String, onClick: () -> Unit) {
    val st = h.stats(now)
    val acc = h.type.accent()
    val next = Achievements.next(st.elapsed)
    Glass(Modifier.fillMaxWidth(), radius = 30.dp, onClick = onClick, tint = acc.a.copy(alpha = 0.05f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            HabitBadge(h.type, 44.dp)
            Column(Modifier.padding(start = 12.dp).weight(1f)) {
                Text(h.title, style = YType.Sub, color = Y.Text)
                Text("с ${h.startMillis.fmtDate()}", style = YType.Body.copy(fontSize = 13.sp), color = Y.Text3)
            }
        }
        Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(top = 14.dp)) {
            Text(Fmt.intNum(st.days), style = YType.Hero, color = Y.Text)
            Column(Modifier.padding(start = 10.dp, bottom = 12.dp)) {
                Text(Fmt.days(st.days), style = YType.Sub, color = acc.b)
                Text(Fmt.hms(st.elapsed), style = YType.Mono.copy(fontSize = 16.sp), color = Y.Text2)
            }
        }
        // прогресс до следующей награды
        if (next != null) {
            val p = Achievements.progress(st.elapsed)
            Box(
                Modifier
                    .padding(top = 12.dp)
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color(0x14FFFFFF))
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(p.coerceAtLeast(0.02f))
                        .height(8.dp)
                        .clip(RoundedCornerShape(50))
                        .background(acc.brush)
                )
            }
            Text(
                "До «${next.title}» — ${Fmt.duration(next.millis - st.elapsed)}",
                style = YType.Body.copy(fontSize = 13.sp), color = Y.Text3, modifier = Modifier.padding(top = 8.dp)
            )
        }
        Row(Modifier.padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MiniStat("сэкономлено", "${Fmt.money(st.money)} $currency", acc, Modifier.weight(1f))
            if (h.unitsPerDay > 0) MiniStat(h.type.unitsLabel, "${Fmt.num(st.units)} ${h.unitName}", acc, Modifier.weight(1f))
        }
    }
}

@Composable
private fun MiniStat(label: String, value: String, acc: Accent, modifier: Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(18.dp))
            .background(acc.soft)
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Text(label.uppercase(), style = YType.Label.copy(fontSize = 10.sp), color = Y.Text3, maxLines = 1)
        Text(value, style = YType.Sub.copy(fontWeight = FontWeight.Bold), color = Y.Text, maxLines = 1, modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
private fun CheckInPrompt(s: AppState, today: Long, onClick: () -> Unit) {
    val c = s.checkIn(today)
    Glass(Modifier.fillMaxWidth(), radius = 26.dp, onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(if (c == null) "✍️" else MOODS[c.mood - 1], fontSize = 30.sp)
            Column(Modifier.padding(start = 14.dp).weight(1f)) {
                Text(if (c == null) "Отметка за сегодня" else "Сегодня отмечено", style = YType.Sub, color = Y.Text)
                Text(
                    if (c == null) "Настроение, тяга, заметка"
                    else "Тяга ${c.craving}/10" + if (c.note.isNotBlank()) " · ${c.note}" else "",
                    style = YType.Body.copy(fontSize = 13.sp), color = Y.Text2, maxLines = 1
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(if (c == null) "Отметить" else "Изменить", style = YType.Sub.copy(fontSize = 14.sp), color = Y.Violet)
        }
    }
}

@Composable
private fun Totals(s: AppState, now: Long) {
    val money = s.habits.sumOf { it.stats(now).money }
    val checks = s.checkIns.size
    SectionLabel("Всего")
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        com.yasnost.app.ui.components.StatTile("сохранено", Fmt.money(money), s.settings.currency, Y.Mint, Modifier.weight(1.4f))
        com.yasnost.app.ui.components.StatTile("тяг побеждено", "${s.cravingsBeaten}", "", Y.Coral, Modifier.weight(1f))
    }
    Spacer(Modifier.height(10.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        com.yasnost.app.ui.components.StatTile("отметок", "$checks", Fmt.plural(checks.toLong(), "день", "дня", "дней"), Y.Sky, Modifier.weight(1f))
        val bestDays = s.habits.maxOfOrNull { s.bestStreak(it.id, now) / com.yasnost.app.domain.DAY } ?: 0
        com.yasnost.app.ui.components.StatTile("лучшая серия", "$bestDays", Fmt.days(bestDays), Y.Amber, Modifier.weight(1f))
    }
}

val MOODS = listOf("😣", "😕", "😐", "🙂", "😄")
val MOOD_NAMES = listOf("Тяжело", "Так себе", "Нормально", "Хорошо", "Отлично")
