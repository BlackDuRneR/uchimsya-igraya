package com.yasnost.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yasnost.app.Nav
import com.yasnost.app.R
import com.yasnost.app.Route
import com.yasnost.app.data.AppState
import com.yasnost.app.data.Habit
import com.yasnost.app.data.HabitType
import com.yasnost.app.data.Repository
import com.yasnost.app.domain.Achievements
import com.yasnost.app.domain.DAY
import com.yasnost.app.domain.Fmt
import com.yasnost.app.domain.Health
import com.yasnost.app.domain.stats
import com.yasnost.app.domain.unitsLabel
import com.yasnost.app.domain.verb
import com.yasnost.app.ui.components.Glass
import com.yasnost.app.ui.components.GhostButton
import com.yasnost.app.ui.components.HabitBadge
import com.yasnost.app.ui.components.ProgressRing
import com.yasnost.app.ui.components.SectionLabel
import com.yasnost.app.ui.components.StatTile
import com.yasnost.app.ui.components.rememberNow
import com.yasnost.app.ui.theme.Accent
import com.yasnost.app.ui.theme.Y
import com.yasnost.app.ui.theme.YType
import com.yasnost.app.ui.theme.accent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun TopBar(title: String, onBack: () -> Unit, action: (@Composable () -> Unit)? = null) {
    Row(
        Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Y.Text) }
        Text(title, style = YType.Sub, color = Y.Text, modifier = Modifier.weight(1f))
        action?.invoke()
    }
}

@Composable
fun HabitScreen(id: String, s: AppState, nav: Nav, repo: Repository, scope: CoroutineScope) {
    val h = s.habit(id) ?: return
    val now = rememberNow()
    val st = h.stats(now)
    val acc = h.type.accent()
    var relapseDialog by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        TopBar(h.title, { nav.back() }) {
            IconButton(onClick = { nav.go(Route.Editor(h.id)) }) { Icon(Icons.Filled.Edit, null, tint = Y.Text2) }
        }
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            // Главное кольцо
            ProgressRing(
                Achievements.progress(st.elapsed), acc,
                Modifier.padding(horizontal = 24.dp).fillMaxWidth().aspectRatio(1f), stroke = 16.dp
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(h.type.verb.uppercase(), style = YType.Label, color = acc.b)
                    Text(Fmt.intNum(st.days), style = YType.Hero.copy(fontSize = 88.sp), color = Y.Text)
                    Text(Fmt.days(st.days), style = YType.Sub, color = Y.Text2)
                    Text(Fmt.hms(st.elapsed), style = YType.Mono, color = Y.Text, modifier = Modifier.padding(top = 8.dp))
                    Achievements.next(st.elapsed)?.let {
                        Text("до «${it.title}»: ${Fmt.duration(it.millis - st.elapsed)}", style = YType.Body.copy(fontSize = 12.sp), color = Y.Text3, modifier = Modifier.padding(top = 6.dp))
                    }
                }
            }
            Text("с ${h.startMillis.fmtDate()}", style = YType.Body, color = Y.Text3, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(bottom = 18.dp))

            // Статистика
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile("сэкономлено", Fmt.money(st.money), s.settings.currency, acc.b, Modifier.weight(1f))
                if (h.unitsPerDay > 0) StatTile(h.type.unitsLabel, Fmt.num(st.units), h.unitName, acc.b, Modifier.weight(1f))
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                val best = s.bestStreak(h.id, now) / DAY
                StatTile("лучшая серия", "$best", Fmt.days(best), acc.b, Modifier.weight(1f))
                if (h.type == HabitType.SMOKING) StatTile("жизни вернул", Fmt.lifeMinutes(st.lifeMinutes), "", acc.b, Modifier.weight(1f))
                else StatTile("в год", Fmt.money(h.costPerDay * 365), s.settings.currency, acc.b, Modifier.weight(1f))
            }
            if (h.costPerDay > 0) {
                Spacer(Modifier.height(10.dp))
                Glass(Modifier.fillMaxWidth(), radius = 22.dp, padding = 16.dp) {
                    Text("НА ЭТИ ДЕНЬГИ МОЖНО", style = YType.Label, color = Y.Text3)
                    Text(moneyIdea(st.money, s.settings.currency), style = YType.Body, color = Y.Text, modifier = Modifier.padding(top = 6.dp))
                }
            }

            // Здоровье
            SectionLabel("Восстановление организма", Modifier.padding(top = 18.dp))
            HealthTimeline(h, st.elapsed, acc)
            Text(
                "Сроки ориентировочные, по данным открытых медицинских источников. У каждого организма свой темп.",
                style = YType.Body.copy(fontSize = 12.sp), color = Y.Text3, modifier = Modifier.padding(top = 4.dp, start = 4.dp)
            )

            // Награды
            SectionLabel("Награды", Modifier.padding(top = 18.dp))
            MedalsGrid(st.elapsed, acc)

            // История
            val history = s.relapses.filter { it.habitId == h.id }.sortedByDescending { it.atMillis }
            if (history.isNotEmpty()) {
                SectionLabel("История попыток", Modifier.padding(top = 18.dp))
                Glass(Modifier.fillMaxWidth(), radius = 22.dp, padding = 16.dp) {
                    history.forEachIndexed { i, r ->
                        Row(Modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(r.atMillis.fmtDate(), style = YType.Body, color = Y.Text2)
                                if (r.note.isNotBlank()) Text(r.note, style = YType.Body.copy(fontSize = 13.sp), color = Y.Text3)
                            }
                            Text(Fmt.duration(r.streakMillis), style = YType.Sub, color = Y.Text)
                        }
                        if (i != history.lastIndex) Box(Modifier.fillMaxWidth().height(1.dp).background(Y.Stroke))
                    }
                }
            }

            Spacer(Modifier.height(22.dp))
            GhostButton("Я сорвался", Modifier.fillMaxWidth(), color = Y.Danger) { relapseDialog = true }
            Text(
                "Срыв — не конец пути. Счётчик начнётся заново, а прошлая серия сохранится в истории.",
                style = YType.Body.copy(fontSize = 12.sp), color = Y.Text3, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            )
            Spacer(Modifier.height(28.dp))
            Spacer(Modifier.navigationBarsPadding())
        }
    }

    if (relapseDialog) {
        var note by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { relapseDialog = false },
            containerColor = Y.Surface2,
            shape = RoundedCornerShape(32.dp),
            title = { Text("Начать заново?", style = YType.Title, color = Y.Text) },
            text = {
                Column {
                    Text(
                        "Ты продержался ${Fmt.duration(st.elapsed)}. Это не пропадёт — серия останется в истории. Что стало причиной? Это поможет в следующий раз.",
                        style = YType.Body, color = Y.Text2
                    )
                    OutlinedTextField(
                        note, { note = it }, placeholder = { Text("Например: стресс на работе", color = Y.Text3) },
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp), shape = RoundedCornerShape(18.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Y.Danger, unfocusedBorderColor = Y.Stroke, focusedTextColor = Y.Text, unfocusedTextColor = Y.Text),
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { scope.launch { repo.relapse(h.id, note.trim()) }; relapseDialog = false }) {
                    Text("Начать заново", color = Y.Danger, style = YType.Sub)
                }
            },
            dismissButton = { TextButton(onClick = { relapseDialog = false }) { Text("Я держусь", color = Y.Text) } },
        )
    }
}

private fun moneyIdea(money: Double, cur: String): String {
    val m = money
    return when {
        m < 5_000 -> "Пока немного — но это уже чашка хорошего кофе каждый день вместо вреда."
        m < 30_000 -> "Новые наушники, кроссовки или хороший ужин в ресторане."
        m < 100_000 -> "Абонемент в спортзал на полгода или поездка на выходные."
        m < 300_000 -> "Новый смартфон среднего класса или отпуск у моря."
        m < 1_000_000 -> "Хороший отпуск за границей или крупная покупка для дома."
        else -> "Первый взнос на машину или серьёзная подушка безопасности. ${Fmt.money(m)} $cur — это твоя свобода."
    }
}

@Composable
private fun HealthTimeline(h: Habit, elapsed: Long, acc: Accent) {
    val steps = Health.forType(h.type)
    Glass(Modifier.fillMaxWidth(), radius = 24.dp, padding = 16.dp) {
        var prev = 0L
        steps.forEachIndexed { i, step ->
            val done = elapsed >= step.afterMillis
            val active = !done && elapsed >= prev
            val p = if (active) ((elapsed - prev).toFloat() / (step.afterMillis - prev)).coerceIn(0f, 1f) else 0f
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp).alpha(if (done || active) 1f else 0.45f)) {
                Box(
                    Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .then(if (done) Modifier.background(acc.brush) else Modifier.border(1.5.dp, if (active) acc.a else Y.Stroke, CircleShape)),
                    contentAlignment = Alignment.Center,
                ) {
                    if (done) Icon(Icons.Filled.Check, null, tint = Color(0xFF0B0C14), modifier = Modifier.size(16.dp))
                    else if (active) Text("${(p * 100).toInt()}", fontSize = 9.sp, color = acc.a)
                }
                Column(Modifier.padding(start = 14.dp).weight(1f)) {
                    Text(step.title, style = YType.Sub, color = if (done) acc.b else Y.Text)
                    Text(step.text, style = YType.Body.copy(fontSize = 14.sp), color = Y.Text2)
                    if (active) {
                        Box(Modifier.padding(top = 8.dp).fillMaxWidth().height(5.dp).clip(RoundedCornerShape(50)).background(Color(0x14FFFFFF))) {
                            Box(Modifier.fillMaxWidth(p.coerceAtLeast(0.02f)).height(5.dp).clip(RoundedCornerShape(50)).background(acc.brush))
                        }
                        Text("осталось ${Fmt.duration(step.afterMillis - elapsed)}", style = YType.Body.copy(fontSize = 12.sp), color = Y.Text3, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }
            prev = step.afterMillis
        }
    }
}

@Composable
fun MedalsGrid(elapsed: Long, acc: Accent) {
    val list = Achievements.list
    list.chunked(4).forEach { row ->
        Row(Modifier.fillMaxWidth().padding(bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            row.forEach { a ->
                val got = elapsed >= a.millis
                Column(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (got) acc.soft else Color(0x08FFFFFF))
                        .border(1.dp, if (got) acc.a.copy(alpha = 0.5f) else Y.Stroke, RoundedCornerShape(20.dp))
                        .padding(vertical = 12.dp, horizontal = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .then(if (got) Modifier.background(acc.brush) else Modifier.background(Color(0x10FFFFFF))),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (got) Icon(painterResource(R.drawable.ic_medal), null, tint = Color(0xFF0B0C14), modifier = Modifier.size(22.dp))
                        else Icon(Icons.Filled.Lock, null, tint = Y.Text3, modifier = Modifier.size(18.dp))
                    }
                    Text(a.short, style = YType.Sub.copy(fontSize = 14.sp), color = if (got) Y.Text else Y.Text3, modifier = Modifier.padding(top = 6.dp))
                    Text(a.title, style = YType.Body.copy(fontSize = 10.sp, lineHeight = 12.sp), color = Y.Text3, textAlign = TextAlign.Center, maxLines = 2)
                }
            }
            repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
        }
    }
}

@Composable
fun AwardsScreen(s: AppState, nav: Nav) {
    val now = rememberNow(60_000)
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(18.dp))
        Text("ДОСТИЖЕНИЯ", style = YType.Label, color = Y.Text3)
        Text("Награды", style = YType.Big, color = Y.Text, modifier = Modifier.padding(top = 4.dp, bottom = 8.dp))
        val total = s.habits.sumOf { h -> Achievements.list.count { h.stats(now).elapsed >= it.millis } }
        Text("Получено $total из ${Achievements.list.size * s.habits.size.coerceAtLeast(1)}", style = YType.Body, color = Y.Text2, modifier = Modifier.padding(bottom = 10.dp))
        if (s.habits.isEmpty()) Text("Добавь привычку на главной, и здесь появятся награды.", style = YType.Body, color = Y.Text3)
        s.habits.forEach { h ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 14.dp, bottom = 12.dp)) {
                HabitBadge(h.type, 34.dp)
                Text(h.title, style = YType.Sub, color = Y.Text, modifier = Modifier.padding(start = 10.dp))
            }
            MedalsGrid(h.stats(now).elapsed, h.type.accent())
        }
        Spacer(Modifier.height(24.dp))
    }
}
