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
import androidx.compose.foundation.layout.size
import androidx.compose.ui.draw.clip
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
import com.yasnost.app.ui.components.g
import com.yasnost.app.ui.components.HabitBadge
import com.yasnost.app.ui.components.SectionLabel
import com.yasnost.app.ui.screens.Stepper
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

        SectionLabel("Профиль")
        var pname by remember(s.profile.name) { mutableStateOf(s.profile.name) }
        var partner by remember { mutableStateOf("") }
        val fc = OutlinedTextFieldDefaults.colors(focusedBorderColor = Y.Violet, unfocusedBorderColor = Y.Stroke, focusedTextColor = Y.Text, unfocusedTextColor = Y.Text, focusedLabelColor = Y.Violet, unfocusedLabelColor = Y.Text3)
        Glass(Modifier.fillMaxWidth(), radius = 24.dp, padding = 14.dp) {
            OutlinedTextField(pname, { pname = it.take(24) }, Modifier.fillMaxWidth(), label = { Text("Имя") }, singleLine = true, shape = RoundedCornerShape(16.dp), colors = fc)
            Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip("♂  Мужской", !s.profile.female, Accent(Y.Sky, Y.Violet)) {
                    scope.launch { repo.update(false) { it.copy(profile = it.profile.copy(gender = com.yasnost.app.data.Gender.MALE)) } }
                }
                Chip("♀  Женский", s.profile.female, Accent(Y.Rose, Y.Magenta)) {
                    scope.launch { repo.update(false) { it.copy(profile = it.profile.copy(gender = com.yasnost.app.data.Gender.FEMALE)) } }
                }
            }

            if (s.profile.female) {
                Box(Modifier.fillMaxWidth().padding(vertical = 10.dp).height(1.dp).background(Y.Stroke))
                ToggleRow("Советы для пары", "Раздел «Понять его» и мягкие напоминания о близости", s.settings.partnerTips) { v -> setSettings { it.copy(partnerTips = v) } }
            }
            if (pname != s.profile.name) {
                Text("Сохранить", style = YType.Sub, color = Y.Violet, modifier = Modifier.padding(top = 10.dp).clickable {
                    scope.launch { repo.update(false) { it.copy(profile = it.profile.copy(name = pname.trim())) } }
                })
            }
            Box(Modifier.fillMaxWidth().padding(vertical = 12.dp).height(1.dp).background(Y.Stroke))
            Text("ПАРТНЁРЫ", style = YType.Label, color = Y.Text3)
            Text(
                if (s.profile.female) "Для календаря близости: с кем был контакт." else "Для близости и календаря цикла: у каждой партнёрши свой цикл.",
                style = YType.Body.copy(fontSize = 12.sp), color = Y.Text3, modifier = Modifier.padding(top = 2.dp, bottom = 4.dp)
            )
            s.partners.forEach { p ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("•  ${p.name}", style = YType.Body, color = Y.Text, modifier = Modifier.weight(1f))
                    IconButton(onClick = { scope.launch { repo.removePartner(p.id) } }) { Icon(Icons.Filled.Close, null, tint = Y.Text3) }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(partner, { partner = it.take(24) }, Modifier.weight(1f), placeholder = { Text("Имя партнёра", color = Y.Text3) },
                    singleLine = true, shape = RoundedCornerShape(16.dp), colors = fc)
                IconButton(onClick = {
                    val n = partner.trim()
                    if (n.isNotEmpty()) scope.launch { repo.addPartner(n) }
                    partner = ""
                }) { Icon(Icons.Filled.Add, null, tint = Y.Violet) }
            }
        }

        SectionLabel("Что отслеживаю", Modifier.padding(top = 14.dp))
        Glass(Modifier.fillMaxWidth(), radius = 24.dp, padding = 16.dp) {
            ToggleRow("🍷  Алкоголь", if (s.settings.trackAlcohol) g("Бросаю / бросил", "Бросаю / бросила") else "Не пью вообще — раздел скрыт", s.settings.trackAlcohol) { v -> setSettings { it.copy(trackAlcohol = v) } }
            Box(Modifier.fillMaxWidth().padding(vertical = 10.dp).height(1.dp).background(Y.Stroke))
            ToggleRow("🚬  Курение", if (s.settings.trackSmoking) g("Бросаю / бросил", "Бросаю / бросила") else "Не курю вообще — раздел скрыт", s.settings.trackSmoking) { v -> setSettings { it.copy(trackSmoking = v) } }
            Box(Modifier.fillMaxWidth().padding(vertical = 10.dp).height(1.dp).background(Y.Stroke))
            ToggleRow("✨  Другие привычки", "Сладкое, соцсети, игры…", s.settings.trackOther) { v -> setSettings { it.copy(trackOther = v) } }
            Box(Modifier.fillMaxWidth().padding(vertical = 10.dp).height(1.dp).background(Y.Stroke))
            ToggleRow("💞  Близость", "Календарь интимной жизни", s.settings.intimacyEnabled) { v -> setSettings { it.copy(intimacyEnabled = v) } }
            Box(Modifier.fillMaxWidth().padding(vertical = 10.dp).height(1.dp).background(Y.Stroke))
            ToggleRow("🌸  Цикл", if (s.profile.female) "Месячные, фертильные дни, ПМС" else "Цикл девушки", s.cycleSettings.enabled) { v -> scope.launch { repo.update(false) { it.copy(cycleSettings = it.cycleSettings.copy(enabled = v)) } } }
            Text(
                "Если выключить раздел, его данные не удаляются — включишь снова, и всё вернётся.",
                style = YType.Body.copy(fontSize = 12.sp), color = Y.Text3, modifier = Modifier.padding(top = 10.dp)
            )
        }

        SectionLabel(if (s.profile.female) "Мой цикл" else "Цикл девушки", Modifier.padding(top = 14.dp))
        fun setCycle(f: (com.yasnost.app.data.CycleSettings) -> com.yasnost.app.data.CycleSettings) = scope.launch { repo.update(false) { it.copy(cycleSettings = f(it.cycleSettings)) } }
        Glass(Modifier.fillMaxWidth(), radius = 24.dp, padding = 16.dp) {
            if (!s.cycleSettings.enabled) Text("Цикл выключен — включи его в «Что отслеживаю».", style = YType.Body.copy(fontSize = 13.sp), color = Y.Text3)
            if (s.cycleSettings.enabled) {
                Stepper("Длина цикла (пока нет данных)", s.cycleSettings.cycleLength, 20..45, "дн") { v -> setCycle { it.copy(cycleLength = v) } }
                Stepper("Длительность месячных", s.cycleSettings.periodLength, 2..10, "дн") { v -> setCycle { it.copy(periodLength = v) } }
                Text("НАПОМИНАТЬ ДО МЕСЯЧНЫХ", style = YType.Label, color = Y.Text3, modifier = Modifier.padding(top = 10.dp, bottom = 8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(0 to "Нет", 1 to "1 день", 2 to "2 дня", 3 to "3 дня").forEach { (d, l) ->
                        Chip(l, s.cycleSettings.remindDaysBefore == d, Accent(Y.Rose, Y.Magenta)) { setCycle { it.copy(remindDaysBefore = d) } }
                    }
                }
                Box(Modifier.fillMaxWidth().padding(vertical = 10.dp).height(1.dp).background(Y.Stroke))
                ToggleRow("Фертильные дни", "Напоминать о начале фертильного окна", s.cycleSettings.fertileReminder) { v -> setCycle { it.copy(fertileReminder = v) } }
            }
        }

        if (s.settings.anyHabits) {
        SectionLabel("Мои привычки", Modifier.padding(top = 14.dp))
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

        SectionLabel(g("Почему я бросил", "Почему я бросила"), Modifier.padding(top = 14.dp))
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

        SectionLabel("Письмо себе", Modifier.padding(top = 14.dp))
        var letter by remember(s.letter) { mutableStateOf(s.letter) }
        Glass(Modifier.fillMaxWidth(), radius = 24.dp, padding = 14.dp) {
            Text(
                "Напиши себе — сегодняшнему, решительному — в тот момент, когда будет тяжело. Письмо появится на экране SOS.",
                style = YType.Body.copy(fontSize = 13.sp), color = Y.Text3, modifier = Modifier.padding(bottom = 8.dp)
            )
            OutlinedTextField(
                letter, { letter = it }, Modifier.fillMaxWidth(),
                placeholder = { Text("Привет. Если ты читаешь это, значит, сейчас трудно. Вспомни, как…", color = Y.Text3) },
                shape = RoundedCornerShape(16.dp), minLines = 3,
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Y.Violet, unfocusedBorderColor = Y.Stroke, focusedTextColor = Y.Text, unfocusedTextColor = Y.Text),
            )
            if (letter != s.letter) {
                Text(
                    "Сохранить", style = YType.Sub, color = Y.Violet,
                    modifier = Modifier.padding(top = 10.dp).clickable { scope.launch { repo.update(false) { it.copy(letter = letter.trim()) } } }
                )
            }
        }

        SectionLabel("Деньги", Modifier.padding(top = 14.dp))
        Glass(Modifier.fillMaxWidth(), radius = 24.dp, padding = 8.dp) {
            Row(
                Modifier.fillMaxWidth().clickable { nav.go(Route.Money) }.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("💰", fontSize = 22.sp)
                Text("Деньги и копилка мечты", style = YType.Sub, color = Y.Text, modifier = Modifier.padding(start = 12.dp).weight(1f))
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Y.Text3)
            }
        }

        SectionLabel("Валюта", Modifier.padding(top = 14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("₸", "₽", "$", "€", "сом").forEach { c ->
                Chip(c, s.settings.currency == c, acc) { setSettings { it.copy(currency = c) } }
            }
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

        SectionLabel("Приватность", Modifier.padding(top = 14.dp))
        Glass(Modifier.fillMaxWidth(), radius = 24.dp, padding = 16.dp) {
            if (s.settings.intimacyEnabled) {
                ToggleRow("Защитить отпечатком", "Открывать «Близость» по отпечатку, лицу или PIN", s.settings.intimacyLock) { v ->
                    setSettings { it.copy(intimacyLock = v) }
                }
                Box(Modifier.fillMaxWidth().padding(vertical = 10.dp).height(1.dp).background(Y.Stroke))
            }
            ToggleRow("Запретить скриншоты", "И скрыть содержимое в списке недавних приложений", s.settings.secureScreen) { v ->
                setSettings { it.copy(secureScreen = v) }
            }
        }

        SectionLabel("Виджеты", Modifier.padding(top = 14.dp))
        Glass(Modifier.fillMaxWidth(), radius = 24.dp, padding = 16.dp) {
            Text(
                "Долгое нажатие на пустом месте рабочего стола → «Виджеты» → «Ясность». Есть ярлыки 1×1 («Не пью», «Не курю», «Месячные») — только число; «Счётчик» с кольцом для одной привычки (растяни до 4×3 — появятся рекорд и этап восстановления); «Сводка» со всеми привычками, циклом и фактом дня.",
                style = YType.Body, color = Y.Text2,
            )
        }

        SectionLabel("Ярлыки 1×1 на рабочем столе", Modifier.padding(top = 14.dp))
        Glass(Modifier.fillMaxWidth(), radius = 24.dp, padding = 16.dp) {
            Text(
                "У разных телефонов (Samsung, Xiaomi, Pixel…) иконки разного размера. «Авто» подстраивает ярлык под сетку лаунчера; если не совпадает — выбери размер и сдвиг вручную.",
                style = YType.Body.copy(fontSize = 13.sp), color = Y.Text3
            )
            val sizeNow = s.settings.iconSize
            Row(Modifier.fillMaxWidth().padding(vertical = 14.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.Bottom) {
                listOf(Y.Violet to Y.Magenta, Y.Teal to Y.Mint, androidx.compose.ui.graphics.Color(0xFFFF4D6D) to Y.Rose).forEachIndexed { i, (c1, c2) ->
                    val tile = if (sizeNow == 0) 56 else sizeNow
                    Column(Modifier.padding(horizontal = 10.dp).padding(top = (if (s.settings.iconOffset > 0) s.settings.iconOffset * 2 else 0).dp, bottom = (if (s.settings.iconOffset < 0) -s.settings.iconOffset * 2 else 0).dp),
                        horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            Modifier.size(tile.dp).clip(RoundedCornerShape((tile * 0.34f).dp))
                                .background(androidx.compose.ui.graphics.Brush.linearGradient(listOf(c1, c2))),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(listOf("98", "41", "8")[i], style = YType.Title.copy(fontSize = (tile * 0.4f).sp), color = androidx.compose.ui.graphics.Color(0xFF0B0C14))
                                Text(listOf("дней", "день", "дней до")[i], fontSize = 8.sp, color = androidx.compose.ui.graphics.Color(0xB30B0C14))
                            }
                        }
                        Text(listOf("Не пью", "Не курю", "Месячные")[i], fontSize = 12.sp, color = Y.Text, modifier = Modifier.padding(top = 5.dp))
                    }
                }
            }
            Text("РАЗМЕР", style = YType.Label, color = Y.Text3, modifier = Modifier.padding(bottom = 8.dp))
            com.yasnost.app.ui.components.ChipsMulti(listOf("Авто", "48", "52", "56", "60", "64"), setOf(if (sizeNow == 0) "Авто" else "$sizeNow"), acc) { v ->
                setSettings { it.copy(iconSize = v.toIntOrNull() ?: 0) }
            }
            Text("ПОЛОЖЕНИЕ", style = YType.Label, color = Y.Text3, modifier = Modifier.padding(top = 14.dp))
            Stepper(
                when { s.settings.iconOffset < 0 -> "Выше на ${-s.settings.iconOffset}"; s.settings.iconOffset > 0 -> "Ниже на ${s.settings.iconOffset}"; else -> "По центру" },
                s.settings.iconOffset, -8..8, "",
            ) { v -> setSettings { it.copy(iconOffset = v) } }
            Text("Изменения применяются к ярлыкам на рабочем столе сразу.", style = YType.Body.copy(fontSize = 12.sp), color = Y.Text3, modifier = Modifier.padding(top = 6.dp))
        }

        SectionLabel("О приложении", Modifier.padding(top = 14.dp))
        Glass(Modifier.fillMaxWidth(), radius = 24.dp, padding = 16.dp) {
            Text("Ясность 3.5", style = YType.Sub, color = Y.Text)
            Text(
                "Все данные хранятся только на этом телефоне. Никаких аккаунтов, рекламы и слежки. Информация о здоровье носит справочный характер и не заменяет консультацию врача.",
                style = YType.Body.copy(fontSize = 13.sp), color = Y.Text3, modifier = Modifier.padding(top = 6.dp),
            )
        }

        SectionLabel("Научные источники", Modifier.padding(top = 14.dp))
        val uri = androidx.compose.ui.platform.LocalUriHandler.current
        Glass(Modifier.fillMaxWidth(), radius = 24.dp, padding = 8.dp) {
            SOURCES.forEach { (title, url) ->
                Text(
                    title, style = YType.Body.copy(fontSize = 13.sp), color = Y.Text2,
                    modifier = Modifier.fillMaxWidth().clickable { runCatching { uri.openUri(url) } }.padding(horizontal = 10.dp, vertical = 8.dp)
                )
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

val SOURCES = listOf(
    "ВОЗ — польза отказа от курения" to "https://www.who.int/news-room/q-a-detail/health-benefits-of-smoking-cessation",
    "CDC — Benefits of Quitting Smoking" to "https://www.cdc.gov/tobacco/about/benefits-of-quitting.html",
    "American Cancer Society — польза отказа со временем" to "https://www.cancer.org/healthy/stay-away-from-tobacco/benefits-of-quitting-smoking-over-time.html",
    "NCI — синдром отмены никотина" to "https://www.cancer.gov/about-cancer/causes-prevention/risk/tobacco/withdrawal-fact-sheet",
    "Mehta et al., BMJ Open 2018 — месяц без алкоголя" to "https://pubmed.ncbi.nlm.nih.gov/29730627/",
    "Эрекция после 3 мес. трезвости, 2022" to "https://pubmed.ncbi.nlm.nih.gov/36606722/",
    "Pourmand et al., BJU Int 2004 — курение и ЭД" to "https://pubmed.ncbi.nlm.nih.gov/15610111/",
    "Sahin et al., 2020 — отказ от курения и сексуальная функция" to "https://pmc.ncbi.nlm.nih.gov/articles/PMC7239286/",
    "Lally et al., 2010 — формирование привычки" to "https://www.ucl.ac.uk/news/2009/aug/how-long-does-it-take-form-habit",
    "Rider et al., European Urology 2016 — эякуляция и рак простаты" to "https://pubmed.ncbi.nlm.nih.gov/27033442/",
    "Muise et al., 2016 — частота секса и благополучие" to "https://journals.sagepub.com/doi/abs/10.1177/1948550615616462",
    "Hall et al., 2010 — сексуальная активность и сердце" to "https://pubmed.ncbi.nlm.nih.gov/20102917/",
    "Cho & Yang, 2023 — алкоголь и рискованное поведение" to "https://www.ncbi.nlm.nih.gov/pmc/articles/PMC10154531/",
    "Frontiers in Psychology 2021 — эякуляция и здоровье" to "https://www.frontiersin.org/articles/10.3389/fpsyg.2021.693121/full",
    "CDC — постконтактная профилактика ВИЧ" to "https://www.cdc.gov/hivnexus/hcp/pep/index.html",
    "NHS — инфекции, передающиеся половым путём" to "https://www.nhs.uk/conditions/sexually-transmitted-infections/",
    "Guttmacher — эффективность контрацепции" to "https://www.guttmacher.org/fact-sheet/contraceptive-effectiveness-united-states",
)

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
