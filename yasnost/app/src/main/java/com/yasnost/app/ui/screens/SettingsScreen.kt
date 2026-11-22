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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yasnost.app.Nav
import com.yasnost.app.Route
import com.yasnost.app.data.AppState
import com.yasnost.app.data.Repository
import com.yasnost.app.data.Settings
import com.yasnost.app.ui.components.Chip
import com.yasnost.app.ui.components.Glass
import com.yasnost.app.ui.components.HabitBadge
import com.yasnost.app.ui.components.SectionLabel
import com.yasnost.app.ui.theme.Accent
import com.yasnost.app.ui.theme.Y
import com.yasnost.app.ui.theme.YType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(s: AppState, nav: Nav, repo: Repository, scope: CoroutineScope) {
    val acc = Accent(Y.Violet, Y.Magenta)
    fun setSettings(f: (Settings) -> Settings) = scope.launch { repo.update { it.copy(settings = f(it.settings)) } }
    var newReason by remember { mutableStateOf("") }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(18.dp))
        Text("НАСТРОЙКИ", style = YType.Label, color = Y.Text3)
        Text("Ещё", style = YType.Big, color = Y.Text, modifier = Modifier.padding(top = 4.dp, bottom = 8.dp))

        SectionLabel("Мои привычки")
        Glass(Modifier.fillMaxWidth(), radius = 24.dp, padding = 8.dp) {
            s.habits.forEach { h ->
                Row(
                    Modifier.fillMaxWidth().clickable { nav.go(Route.Editor(h.id)) }.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    HabitBadge(h.type, 36.dp)
                    Text(h.title, style = YType.Sub, color = Y.Text, modifier = Modifier.padding(start = 12.dp).weight(1f))
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Y.Text3)
                }
            }
            Row(
                Modifier.fillMaxWidth().clickable { nav.go(Route.Editor(null)) }.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.Add, null, tint = Y.Violet)
                Text("  Добавить привычку", style = YType.Sub, color = Y.Violet)
            }
        }

        SectionLabel("Почему я бросил", Modifier.padding(top = 14.dp))
        Glass(Modifier.fillMaxWidth(), radius = 24.dp, padding = 14.dp) {
            Text("Эти причины покажутся на экране SOS, когда будет трудно.", style = YType.Body.copy(fontSize = 13.sp), color = Y.Text3, modifier = Modifier.padding(bottom = 6.dp))
            s.reasons.forEachIndexed { i, r ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("✦  $r", style = YType.Body, color = Y.Text, modifier = Modifier.weight(1f))
                    IconButton(onClick = { scope.launch { repo.update(false) { st -> st.copy(reasons = st.reasons.filterIndexed { j, _ -> j != i }) } } }) {
                        Icon(Icons.Filled.Close, null, tint = Y.Text3)
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    newReason, { newReason = it }, Modifier.weight(1f),
                    placeholder = { Text("Например: ясная голова по утрам", color = Y.Text3) },
                    shape = RoundedCornerShape(16.dp), singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Y.Violet, unfocusedBorderColor = Y.Stroke, focusedTextColor = Y.Text, unfocusedTextColor = Y.Text),
                )
                IconButton(onClick = {
                    val t = newReason.trim()
                    if (t.isNotEmpty()) scope.launch { repo.update(false) { it.copy(reasons = it.reasons + t) } }
                    newReason = ""
                }) { Icon(Icons.Filled.Add, null, tint = Y.Violet) }
            }
        }

        SectionLabel("Валюта", Modifier.padding(top = 14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("₸", "₽", "$", "€", "сом").forEach { c ->
                Chip(c, s.settings.currency == c, acc) { setSettings { it.copy(currency = c) } }
            }
        }

        SectionLabel("Уведомления", Modifier.padding(top = 14.dp))
        Glass(Modifier.fillMaxWidth(), radius = 24.dp, padding = 16.dp) {
            ToggleRow("Новые этапы и награды", "Сообщу, когда откроется новый этап восстановления", s.settings.milestoneNotifications) { v ->
                setSettings { it.copy(milestoneNotifications = v) }
            }
            Box(Modifier.fillMaxWidth().padding(vertical = 10.dp).height(1.dp).background(Y.Stroke))
            ToggleRow("Вечерняя отметка", "Напомню отметить день, если ещё не отмечен", s.settings.reminderEnabled) { v ->
                setSettings { it.copy(reminderEnabled = v) }
            }
            if (s.settings.reminderEnabled) {
                Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(19, 20, 21, 22, 23).forEach { h ->
                        Chip("$h:00", s.settings.reminderHour == h, acc) { setSettings { it.copy(reminderHour = h) } }
                    }
                }
            }
        }

        SectionLabel("Виджеты", Modifier.padding(top = 14.dp))
        Glass(Modifier.fillMaxWidth(), radius = 24.dp, padding = 16.dp) {
            Text(
                "Долгое нажатие на пустом месте рабочего стола → «Виджеты» → «Ясность». Есть два вида: «Счётчик» с кольцом для одной привычки и «Сводка» со всеми сразу. Размер можно менять.",
                style = YType.Body, color = Y.Text2,
            )
        }

        SectionLabel("О приложении", Modifier.padding(top = 14.dp))
        Glass(Modifier.fillMaxWidth(), radius = 24.dp, padding = 16.dp) {
            Text("Ясность 1.0", style = YType.Sub, color = Y.Text)
            Text(
                "Все данные хранятся только на этом телефоне. Никаких аккаунтов, рекламы и слежки. Информация о здоровье носит справочный характер и не заменяет консультацию врача.",
                style = YType.Body.copy(fontSize = 13.sp), color = Y.Text3, modifier = Modifier.padding(top = 6.dp),
            )
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun ToggleRow(title: String, sub: String, value: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = YType.Sub, color = Y.Text)
            Text(sub, style = YType.Body.copy(fontSize = 13.sp), color = Y.Text3)
        }
        Switch(value, onChange, colors = SwitchDefaults.colors(checkedTrackColor = Y.Violet, checkedThumbColor = androidx.compose.ui.graphics.Color.White))
    }
}
