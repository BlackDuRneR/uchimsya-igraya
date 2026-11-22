package com.yasnost.app.ui.screens

import com.yasnost.app.ui.components.Glyph
import com.yasnost.app.ui.components.GlyphTile
import com.yasnost.app.ui.components.MoodIcon
import com.yasnost.app.ui.components.MoodChoice
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yasnost.app.Nav
import com.yasnost.app.data.AppState
import com.yasnost.app.data.Goal
import com.yasnost.app.data.Repository
import com.yasnost.app.domain.Catalog
import com.yasnost.app.domain.Fmt
import com.yasnost.app.domain.GoalProgress
import com.yasnost.app.domain.Money
import com.yasnost.app.domain.Rates
import com.yasnost.app.domain.stats
import com.yasnost.app.ui.components.Glass
import com.yasnost.app.ui.components.GradientButton
import com.yasnost.app.ui.components.HabitBadge
import com.yasnost.app.ui.components.SectionLabel
import com.yasnost.app.ui.components.StatTile
import com.yasnost.app.ui.components.rememberNow
import com.yasnost.app.ui.theme.Accent
import com.yasnost.app.ui.theme.Y
import com.yasnost.app.ui.theme.YType
import com.yasnost.app.ui.theme.accent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.time.LocalDate

val MINT = Accent(Y.Teal, Y.Mint)
private val GOLD = Accent(Y.Amber, Y.Coral)

@Composable
fun MoneyScreen(s: AppState, nav: Nav, repo: Repository, scope: CoroutineScope) {
    val now = rememberNow()
    val cur = s.settings.currency
    val total = Money.total(s, now)
    val daily = Money.daily(s)
    val kzt = Rates.toKzt(total, cur)
    var addGoal by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        TopBar("Деньги и мечты", { nav.back() })
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            Text("УЖЕ СОХРАНЕНО", style = YType.Label, color = Y.Text3)
            Row(verticalAlignment = Alignment.Bottom) {
                Text(Fmt.money(total), style = YType.Hero.copy(fontSize = 54.sp), color = Y.Text)
                Text(" $cur", style = YType.Title, color = Y.Mint, modifier = Modifier.padding(bottom = 10.dp))
            }
            Text(Money.idea(kzt), style = YType.Body, color = Y.Text2, modifier = Modifier.padding(bottom = 14.dp))

            s.habits.filter { it.costPerDay > 0 }.forEach { h ->
                Row(Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    HabitBadge(h.type, 28.dp)
                    Text(h.title, style = YType.Body, color = Y.Text2, modifier = Modifier.padding(start = 10.dp).weight(1f))
                    Text("${Fmt.money(h.stats(now).money)} $cur", style = YType.Sub, color = h.type.accent().b)
                }
            }

            SectionLabel("Если продолжать так же", Modifier.padding(top = 14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile("в месяц", Fmt.money(daily * 30.44), cur, Y.Mint, Modifier.weight(1f))
                StatTile("в год", Fmt.money(daily * 365), cur, Y.Mint, Modifier.weight(1f))
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile("через 5 лет", Fmt.money(total + daily * 365 * 5), cur, Y.Amber, Modifier.weight(1f))
                StatTile("через 10 лет", Fmt.money(total + daily * 3652), cur, Y.Amber, Modifier.weight(1f))
            }
            Text(
                "Если откладывать эти деньги на депозит, за счёт процентов сумма будет ещё больше.",
                style = YType.Body.copy(fontSize = 12.sp), color = Y.Text3, modifier = Modifier.padding(top = 8.dp, start = 4.dp)
            )

            // Копилка мечты
            SectionLabel("Копилка мечты", Modifier.padding(top = 14.dp))
            val goals = Money.goals(s, now)
            if (goals.isEmpty()) {
                Glass(Modifier.fillMaxWidth(), radius = 24.dp, padding = 16.dp, onClick = { addGoal = true }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        GlyphTile("🎯", GOLD, 36.dp)
                        Text("  Поставь цель", style = YType.Sub, color = Y.Text)
                    }
                    Text(
                        "Например, «Отпуск в Турции — 500 000 ₸». Сэкономленные деньги будут копиться на неё, а приложение посчитает, через сколько дней мечта сбудется.",
                        style = YType.Body.copy(fontSize = 14.sp), color = Y.Text2, modifier = Modifier.padding(top = 6.dp)
                    )
                }
            }
            goals.forEach { gp -> GoalCard(gp, cur) { scope.launch { repo.update(false) { st -> st.copy(goals = st.goals.filterNot { it.id == gp.goal.id }) } } } }
            if (goals.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                GradientButton("+ Добавить цель", GOLD, Modifier.fillMaxWidth()) { addGoal = true }
            }

            // Эквиваленты
            val eq = Catalog.equivalents(kzt).reversed()
            if (eq.isNotEmpty()) {
                SectionLabel("Это уже", Modifier.padding(top = 14.dp))
                Glass(Modifier.fillMaxWidth(), radius = 24.dp, padding = 16.dp) {
                    eq.forEach { e ->
                        Row(Modifier.padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                            GlyphTile(e.thing.emoji, MINT, 34.dp)
                            Text(e.thing.title, style = YType.Body, color = Y.Text, modifier = Modifier.padding(start = 12.dp).weight(1f))
                            Text("× ${e.count}", style = YType.Sub, color = Y.Mint)
                        }
                    }
                }
            }
            val next = Catalog.nextTargets(kzt, 4)
            if (next.isNotEmpty() && daily > 0) {
                SectionLabel("Скоро будет хватать на", Modifier.padding(top = 14.dp))
                Glass(Modifier.fillMaxWidth(), radius = 24.dp, padding = 16.dp) {
                    next.forEach { th ->
                        val left = Rates.fromKzt(th.priceKzt - kzt, cur)
                        val days = kotlin.math.ceil(left / daily).toLong()
                        Row(Modifier.padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                            GlyphTile(th.emoji, GOLD, 34.dp)
                            Column(Modifier.padding(start = 12.dp).weight(1f)) {
                                Text(th.title, style = YType.Body, color = Y.Text)
                                Text("≈ ${Fmt.money(Rates.fromKzt(th.priceKzt, cur))} $cur", style = YType.Body.copy(fontSize = 12.sp), color = Y.Text3)
                            }
                            Text("через $days ${Fmt.days(days)}", style = YType.Sub.copy(fontSize = 14.sp), color = Y.Amber)
                        }
                    }
                }
            }
            Text(
                "Цены ориентировочные: Алматы, ноябрь 2026. Пересчёт валют по курсу Нацбанка РК.",
                style = YType.Body.copy(fontSize = 12.sp), color = Y.Text3, modifier = Modifier.padding(top = 8.dp, start = 4.dp)
            )
            Spacer(Modifier.height(28.dp))
            Spacer(Modifier.navigationBarsPadding())
        }
    }

    if (addGoal) {
        var title by remember { mutableStateOf("") }
        var price by remember { mutableStateOf("") }
        val colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Y.Amber, unfocusedBorderColor = Y.Stroke, focusedTextColor = Y.Text, unfocusedTextColor = Y.Text, focusedLabelColor = Y.Amber)
        AlertDialog(
            onDismissRequest = { addGoal = false },
            containerColor = Y.Surface2,
            shape = RoundedCornerShape(32.dp),
            title = { Text("Новая мечта", style = YType.Title, color = Y.Text) },
            text = {
                Column {
                    OutlinedTextField(title, { title = it }, label = { Text("Что хочу") }, singleLine = true, shape = RoundedCornerShape(16.dp), colors = colors, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        price, { price = it.filter { c -> c.isDigit() } }, label = { Text("Сколько стоит, $cur") }, singleLine = true,
                        shape = RoundedCornerShape(16.dp), colors = colors, modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    )
                    Text("Идеи: новый телефон, отпуск, курсы, подарок близким, первый взнос.", style = YType.Body.copy(fontSize = 12.sp), color = Y.Text3, modifier = Modifier.padding(top = 8.dp))
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val p = price.toDoubleOrNull()
                    if (title.isNotBlank() && p != null && p > 0) {
                        scope.launch { repo.update(false) { it.copy(goals = it.goals + Goal(Repository.newId(), title.trim(), p, System.currentTimeMillis())) } }
                    }
                    addGoal = false
                }) { Text("Добавить", color = Y.Amber, style = YType.Sub) }
            },
            dismissButton = { TextButton(onClick = { addGoal = false }) { Text("Отмена", color = Y.Text2) } },
        )
    }
}

@Composable
fun GoalCard(gp: GoalProgress, cur: String, onDelete: (() -> Unit)? = null) {
    Glass(Modifier.fillMaxWidth().padding(bottom = 10.dp), radius = 24.dp, padding = 16.dp, tint = if (gp.done) Y.Amber.copy(alpha = 0.08f) else Color.Transparent) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (gp.done) { Glyph("🎉", Y.Amber, 18.dp); Spacer(Modifier.width(6.dp)) }
                    Text(gp.goal.title, style = YType.Sub, color = Y.Text)
                }
                Text(
                    "${Fmt.money(gp.funded)} из ${Fmt.money(gp.goal.price)} $cur",
                    style = YType.Body.copy(fontSize = 13.sp), color = Y.Text2
                )
            }
            if (onDelete != null) IconButton(onClick = onDelete) { Icon(Icons.Filled.Close, null, tint = Y.Text3) }
        }
        Box(Modifier.padding(top = 10.dp).fillMaxWidth().height(10.dp).clip(RoundedCornerShape(50)).background(Color(0x14FFFFFF))) {
            Box(Modifier.fillMaxWidth(gp.fraction.coerceAtLeast(0.02f)).height(10.dp).clip(RoundedCornerShape(50)).background(GOLD.brush))
        }
        Text(
            when {
                gp.done -> "Цель достигнута! Можно исполнять мечту."
                gp.daysLeft != null -> "${(gp.fraction * 100).toInt()}% · осталось ${gp.daysLeft} ${Fmt.days(gp.daysLeft)} — примерно к ${shortDayFmt.format(LocalDate.now().plusDays(gp.daysLeft))}"
                else -> "${(gp.fraction * 100).toInt()}%"
            },
            style = YType.Body.copy(fontSize = 12.sp, fontWeight = FontWeight.Medium), color = if (gp.done) Y.Amber else Y.Text3, modifier = Modifier.padding(top = 8.dp)
        )
    }
}
