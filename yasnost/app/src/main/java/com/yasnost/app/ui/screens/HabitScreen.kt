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
import androidx.compose.foundation.layout.fillMaxHeight
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
import com.yasnost.app.ui.components.g
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
                MoneyHighlights(com.yasnost.app.domain.Rates.toKzt(st.money, s.settings.currency), s.settings.currency, h.costPerDay) { nav.go(Route.Money) }
            }

            // Здоровье
            SectionLabel("Восстановление организма", Modifier.padding(top = 18.dp))
            HealthTimeline(h, st.elapsed, acc, com.yasnost.app.ui.components.LocalFemale.current)
            Text(
                "Сроки ориентировочные, по данным ВОЗ, CDC, NIAAA и рецензируемых исследований. У каждого организма свой темп.",
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
                                if (r.triggers.isNotEmpty()) Text(r.triggers.joinToString(" · "), style = YType.Body.copy(fontSize = 12.sp), color = Y.Coral)
                            }
                            Text(Fmt.duration(r.streakMillis), style = YType.Sub, color = Y.Text)
                        }
                        if (i != history.lastIndex) Box(Modifier.fillMaxWidth().height(1.dp).background(Y.Stroke))
                    }
                }
            }

            Spacer(Modifier.height(22.dp))
            GhostButton(g("Я сорвался", "Я сорвалась"), Modifier.fillMaxWidth(), color = Y.Danger) { relapseDialog = true }
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
        var trig by remember { mutableStateOf(setOf<String>()) }
        AlertDialog(
            onDismissRequest = { relapseDialog = false },
            containerColor = Y.Surface2,
            shape = RoundedCornerShape(32.dp),
            title = { Text("Срыв — это не конец", style = YType.Title, color = Y.Text) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        g("Ты продержался ", "Ты продержалась ") + "${Fmt.duration(st.elapsed)} — это никуда не денется, серия останется в истории. " +
                            "Психологи называют ловушкой мысль «всё пропало, теперь можно». Один эпизод — это урок, а не приговор. " +
                            "Большинство людей бросают окончательно не с первой попытки.",
                        style = YType.Body, color = Y.Text2
                    )
                    Text("ЧТО ПОДТОЛКНУЛО", style = YType.Label, color = Y.Text3, modifier = Modifier.padding(top = 14.dp, bottom = 8.dp))
                    com.yasnost.app.ui.components.ChipsMulti(com.yasnost.app.domain.TRIGGERS, trig, Accent(Y.Coral, Y.Magenta)) { t -> trig = if (t in trig) trig - t else trig + t }
                    OutlinedTextField(
                        note, { note = it }, placeholder = { Text("Что было бы полезно сделать в следующий раз?", color = Y.Text3) },
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp), shape = RoundedCornerShape(18.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Y.Danger, unfocusedBorderColor = Y.Stroke, focusedTextColor = Y.Text, unfocusedTextColor = Y.Text),
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { scope.launch { repo.relapse(h.id, note.trim(), trig.toList()) }; relapseDialog = false }) {
                    Text("Начать заново", color = Y.Danger, style = YType.Sub)
                }
            },
            dismissButton = { TextButton(onClick = { relapseDialog = false }) { Text("Я держусь", color = Y.Text) } },
        )
    }
}

@Composable
fun MoneyHighlights(kzt: Double, cur: String, perDay: Double, onClick: () -> Unit) {
    val hl = com.yasnost.app.domain.Catalog.highlights(kzt, java.time.LocalDate.now().toEpochDay())
    val next = com.yasnost.app.domain.Catalog.nextTargets(kzt, 1).firstOrNull()
    Glass(Modifier.fillMaxWidth(), radius = 22.dp, padding = 16.dp, onClick = onClick) {
        Text("ТВОИ ДЕНЬГИ — ЭТО", style = YType.Label, color = Y.Text3)
        if (hl.isEmpty()) Text(com.yasnost.app.domain.Money.idea(kzt), style = YType.Body, color = Y.Text, modifier = Modifier.padding(top = 6.dp))
        hl.forEach { e ->
            Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(e.thing.emoji, fontSize = 18.sp)
                Text(e.thing.title, style = YType.Body, color = Y.Text, modifier = Modifier.padding(start = 10.dp).weight(1f))
                Text("× ${e.count}", style = YType.Sub, color = Y.Mint)
            }
        }
        if (next != null && perDay > 0) {
            val days = kotlin.math.ceil(com.yasnost.app.domain.Rates.fromKzt(next.priceKzt - kzt, cur) / perDay).toLong()
            Text(
                "${next.emoji} ${next.title} — ещё $days ${Fmt.days(days)} без срывов",
                style = YType.Body.copy(fontSize = 13.sp), color = Y.Amber, modifier = Modifier.padding(top = 10.dp)
            )
        }
        Text("Подробнее и копилка мечты →", style = YType.Body.copy(fontSize = 12.sp), color = Y.Text3, modifier = Modifier.padding(top = 8.dp))
    }
}

@Composable
private fun HealthTimeline(h: Habit, elapsed: Long, acc: Accent, female: Boolean) {
    val steps = Health.forType(h.type, female)
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
                    if (step.source.isNotEmpty() && (done || active)) Text(step.source, style = YType.Body.copy(fontSize = 11.sp), color = Y.Text3, modifier = Modifier.padding(top = 2.dp))
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
        Row(Modifier.fillMaxWidth().height(androidx.compose.foundation.layout.IntrinsicSize.Min).padding(bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            row.forEach { a ->
                val got = elapsed >= a.millis
                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
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

