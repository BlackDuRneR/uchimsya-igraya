package com.yasnost.app.ui.screens

import com.yasnost.app.ui.components.Glyph
import com.yasnost.app.ui.components.GlyphTile
import com.yasnost.app.ui.components.MoodIcon
import com.yasnost.app.ui.components.MoodChoice
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
import androidx.compose.ui.graphics.Brush
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
import com.yasnost.app.ui.components.g
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
        Text(
            ruDay.format(today).replaceFirstChar { it.uppercase() } + if (s.profile.name.isNotBlank()) " · привет, ${s.profile.name}" else "",
            style = YType.Label, color = Y.Text3
        )
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
            Text("Ясность", style = YType.Big.copy(fontSize = 40.sp), color = Y.Text, modifier = Modifier.weight(1f))
            if (s.habits.isNotEmpty()) {
                val lvl = com.yasnost.app.domain.Levels.of(s, now)
                Row(
                    Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Brush.linearGradient(listOf(Y.Amber, Y.Magenta)))
                        .clickable { nav.tab(Route.Awards) }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Ур. ${lvl.n} · ${lvl.title}", style = YType.Sub.copy(fontSize = 13.sp), color = Color(0xFF0B0C14))
                }
            }
        }
        Text(if (s.settings.anyHabits) Quotes.today(today.toEpochDay()) else Quotes.general(today.toEpochDay()), style = YType.Body, color = Y.Text2, modifier = Modifier.padding(top = 6.dp, bottom = 20.dp))

        if (!s.settings.anyHabits) {
            WellnessHome(s, nav, repo, scope, today.toEpochDay()) { showCheckIn = true }
        } else if (s.habits.isEmpty()) {
            Onboarding(nav, s)
            if (s.cycleSettings.enabled) {
                Spacer(Modifier.height(14.dp))
                CycleMini(s) { CalendarTab.tab = "cycle"; nav.tab(Route.Calendar) }
            }
        } else {
            s.habits.forEach { h ->
                HabitCard(h, now, s.settings.currency) { nav.go(Route.Habit(h.id)) }
                Spacer(Modifier.height(14.dp))
            }
            CheckInPrompt(s, today.toEpochDay()) { showCheckIn = true }
            Spacer(Modifier.height(14.dp))
            if (s.cycleSettings.enabled) {
                CycleMini(s) { CalendarTab.tab = "cycle"; nav.tab(Route.Calendar) }
                Spacer(Modifier.height(14.dp))
            }
            FactOfDay(s, today.toEpochDay())
            Spacer(Modifier.height(14.dp))
            DreamPreview(s, now) { nav.go(Route.Money) }
            Totals(s, now) { nav.go(Route.Money) }
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
private fun WellnessHome(s: AppState, nav: Nav, repo: Repository, scope: CoroutineScope, today: Long, onCheckIn: () -> Unit) {
    if (s.cycleSettings.enabled) {
        CycleHomeCard(s, repo, scope) { CalendarTab.tab = "cycle"; nav.tab(Route.Calendar) }
        Spacer(Modifier.height(14.dp))
    }
    CheckInPrompt(s, today, onCheckIn)
    Spacer(Modifier.height(14.dp))
    if (s.settings.intimacyEnabled) {
        val e = s.intimacyOn(today)
        Glass(Modifier.fillMaxWidth(), radius = 24.dp, padding = 16.dp, onClick = { CalendarTab.tab = "intimacy"; nav.tab(Route.Calendar) }, tint = Y.Rose.copy(alpha = 0.05f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                GlyphTile("❤", Accent(Y.Rose, Y.Magenta), 44.dp)
                Column(Modifier.padding(start = 14.dp).weight(1f)) {
                    Text("Близость", style = YType.Sub, color = Y.Text)
                    Text(if (e == null) "Отметить сегодняшний день" else "Сегодня отмечено", style = YType.Body.copy(fontSize = 13.sp), color = Y.Text2)
                }
                Text("→", style = YType.Title, color = Y.Rose)
            }
        }
        Spacer(Modifier.height(14.dp))
    }
    FactOfDay(s, today)
    if (!s.cycleSettings.enabled && !s.settings.intimacyEnabled) {
        Spacer(Modifier.height(14.dp))
        Glass(Modifier.fillMaxWidth(), radius = 24.dp, padding = 16.dp, onClick = { nav.tab(Route.Settings) }) {
            Text("Все разделы выключены", style = YType.Sub, color = Y.Text)
            Text("Включи нужное в «Ещё» → «Что отслеживаю»: трезвость, курение, цикл, близость.", style = YType.Body.copy(fontSize = 13.sp), color = Y.Text2, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

@Composable
private fun Onboarding(nav: Nav, s: AppState) {
    Glass(Modifier.fillMaxWidth()) {
        Text("С чего начнём?", style = YType.Title, color = Y.Text)
        Text(
            g("Отметь, от чего ты отказался, и укажи, когда.", "Отметь, от чего ты отказалась, и укажи, когда.") + " Можно добавить несколько — например, алкоголь и курение.",
            style = YType.Body, color = Y.Text2, modifier = Modifier.padding(top = 6.dp, bottom = 18.dp)
        )
        if (s.settings.trackAlcohol) {
            GradientButton(g("Я бросил пить", "Я бросила пить"), HabitType.ALCOHOL.accent(), Modifier.fillMaxWidth()) { nav.go(Route.Editor(null, HabitType.ALCOHOL.name)) }
            Spacer(Modifier.height(10.dp))
        }
        if (s.settings.trackSmoking) {
            GradientButton(g("Я бросил курить", "Я бросила курить"), HabitType.SMOKING.accent(), Modifier.fillMaxWidth()) { nav.go(Route.Editor(null, HabitType.SMOKING.name)) }
            Spacer(Modifier.height(10.dp))
        }
        if (s.settings.trackOther) {
            GradientButton("Другая привычка", HabitType.CUSTOM.accent(), Modifier.fillMaxWidth()) { nav.go(Route.Editor(null, HabitType.CUSTOM.name)) }
        }
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
            if (c == null) GlyphTile("✍", Accent(Y.Violet, Y.Sky), 44.dp) else MoodIcon(c.mood, 40.dp)
            Column(Modifier.padding(start = 14.dp).weight(1f)) {
                Text(if (c == null) (if (s.habits.isEmpty()) "Как ты сегодня?" else "Отметка за сегодня") else "Сегодня отмечено", style = YType.Sub, color = Y.Text)
                Text(
                    if (c == null) (if (s.habits.isEmpty()) "Настроение и заметка" else "Настроение, тяга, заметка")
                    else if (s.habits.isEmpty()) MOOD_NAMES[c.mood - 1] + if (c.note.isNotBlank()) " · ${c.note}" else ""
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
private fun Totals(s: AppState, now: Long, onMoney: () -> Unit) {
    val money = s.habits.sumOf { it.stats(now).money }
    val checks = s.checkIns.size
    SectionLabel("Всего")
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        com.yasnost.app.ui.components.StatTile("сохранено", Fmt.money(money), s.settings.currency, Y.Mint, Modifier.weight(1.4f).clickable(onClick = onMoney))
        com.yasnost.app.ui.components.StatTile("тяг побеждено", "${s.cravingsBeaten}", "", Y.Coral, Modifier.weight(1f))
    }
    Spacer(Modifier.height(10.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        com.yasnost.app.ui.components.StatTile("отметок", "$checks", Fmt.plural(checks.toLong(), "день", "дня", "дней"), Y.Sky, Modifier.weight(1f))
        val bestDays = s.habits.maxOfOrNull { s.bestStreak(it.id, now) / com.yasnost.app.domain.DAY } ?: 0
        com.yasnost.app.ui.components.StatTile("лучшая серия", "$bestDays", Fmt.days(bestDays), Y.Amber, Modifier.weight(1f))
    }
}

@Composable
private fun FactOfDay(s: AppState, day: Long) {
    val f = com.yasnost.app.domain.Facts.today(day, s.habits.map { it.type }.toSet(), s.profile.female)
    Glass(Modifier.fillMaxWidth(), radius = 26.dp, padding = 18.dp, tint = Y.Sky.copy(alpha = 0.05f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Glyph("🔬", Y.Sky, 16.dp)
            Text("  ФАКТ ДНЯ", style = YType.Label, color = Y.Sky)
        }
        Text(f.text, style = YType.Body, color = Y.Text, modifier = Modifier.padding(top = 8.dp))
        Text(f.source, style = YType.Body.copy(fontSize = 11.sp), color = Y.Text3, modifier = Modifier.padding(top = 6.dp))
    }
}

@Composable
private fun DreamPreview(s: AppState, now: Long, onClick: () -> Unit) {
    val goals = com.yasnost.app.domain.Money.goals(s, now)
    val g = goals.firstOrNull { !it.done } ?: goals.lastOrNull()
    if (g != null) {
        SectionLabel("Копилка мечты")
        Box(Modifier.clickable(onClick = onClick)) { GoalCard(g, s.settings.currency) }
    } else if (s.habits.any { it.costPerDay > 0 }) {
        Glass(Modifier.fillMaxWidth().padding(bottom = 4.dp), radius = 24.dp, padding = 16.dp, onClick = onClick) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                GlyphTile("🎯", Accent(Y.Amber, Y.Coral), 44.dp)
                Column(Modifier.padding(start = 14.dp).weight(1f)) {
                    Text("Поставь цель", style = YType.Sub, color = Y.Text)
                    Text("Копи сэкономленные деньги на мечту", style = YType.Body.copy(fontSize = 13.sp), color = Y.Text2)
                }
                Text("→", style = YType.Title, color = Y.Amber)
            }
        }
    }
}

val MOODS = listOf("1", "2", "3", "4", "5")
val MOOD_NAMES = listOf("Тяжело", "Так себе", "Нормально", "Хорошо", "Отлично")
