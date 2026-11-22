package com.yasnost.app.ui.screens

import android.widget.Toast
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yasnost.app.Nav
import com.yasnost.app.Route
import com.yasnost.app.data.AppState
import com.yasnost.app.data.Repository
import com.yasnost.app.domain.Fmt
import com.yasnost.app.domain.stats
import com.yasnost.app.ui.components.Dot
import com.yasnost.app.ui.components.Glass
import com.yasnost.app.ui.components.GhostButton
import com.yasnost.app.ui.components.GradientButton
import com.yasnost.app.ui.components.SectionLabel
import com.yasnost.app.ui.components.rememberNow
import com.yasnost.app.ui.theme.Accent
import com.yasnost.app.ui.theme.Y
import com.yasnost.app.ui.theme.YType
import com.yasnost.app.ui.theme.accent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

private data class Phase(val name: String, val seconds: Int, val target: Float)
private val PHASES = listOf(
    Phase("Вдох", 4, 1f),
    Phase("Задержка", 4, 1f),
    Phase("Выдох", 6, 0f),
    Phase("Пауза", 2, 0f),
)

@Composable
fun SosScreen(s: AppState, nav: Nav, repo: Repository, scope: CoroutineScope) {
    val ctx = LocalContext.current
    val started = remember { System.currentTimeMillis() }
    val now = rememberNow()
    val left = (10 * 60_000L - (now - started)).coerceAtLeast(0)
    val acc = Accent(Y.Coral, Y.Magenta)

    var phaseIdx by remember { mutableStateOf(0) }
    val scale = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        var i = 0
        while (true) {
            val p = PHASES[i % PHASES.size]
            phaseIdx = i % PHASES.size
            scale.animateTo(p.target, tween(p.seconds * 1000, easing = LinearEasing))
            i++
        }
    }

    Column(Modifier.fillMaxSize()) {
        TopBar("Тяга? Давай переждём", { nav.back() })
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Text(
                "Желание накатывает волной и обычно спадает за 5–10 минут. Просто подыши вместе с кругом.",
                style = YType.Body, color = Y.Text2, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()
            )
            Box(Modifier.fillMaxWidth().height(300.dp), contentAlignment = Alignment.Center) {
                val v = scale.value
                Canvas(Modifier.size(280.dp)) {
                    val maxR = size.minDimension / 2
                    val r = maxR * (0.42f + 0.5f * v)
                    drawCircle(Brush.radialGradient(listOf(Y.Magenta.copy(alpha = 0.35f), Color.Transparent), radius = maxR), maxR)
                    drawCircle(Brush.linearGradient(listOf(Y.Coral, Y.Magenta, Y.Violet)), r)
                    drawCircle(Color.White.copy(alpha = 0.25f), maxR * 0.92f, style = Stroke(1.dp.toPx()))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(PHASES[phaseIdx].name, style = YType.Title, color = Color(0xFF0B0C14))
                    Text("${PHASES[phaseIdx].seconds} сек", style = YType.Body, color = Color(0xCC0B0C14))
                }
            }
            Text(
                if (left > 0) "Волна пройдёт через ${"%d:%02d".format(left / 60_000, (left / 1000) % 60)}" else "10 минут позади. Ты сильнее, чем думаешь.",
                style = YType.Mono.copy(fontSize = 18.sp), color = Y.Text, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()
            )

            if (s.habits.isNotEmpty()) {
                SectionLabel("Не обнуляй это", Modifier.padding(top = 20.dp))
                Glass(Modifier.fillMaxWidth(), radius = 24.dp, padding = 16.dp) {
                    s.habits.forEach { h ->
                        val st = h.stats(now)
                        Row(Modifier.padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                            Dot(h.type.accent().a, 10.dp)
                            Text(
                                "  ${h.title}: ${st.days} ${Fmt.days(st.days)}, ${Fmt.money(st.money)} ${s.settings.currency}",
                                style = YType.Body, color = Y.Text
                            )
                        }
                    }
                }
            }

            if (s.letter.isNotBlank()) {
                SectionLabel("Письмо от тебя", Modifier.padding(top = 20.dp))
                Glass(Modifier.fillMaxWidth(), radius = 24.dp, padding = 18.dp, tint = Y.Violet.copy(alpha = 0.06f)) {
                    Text("✉️", fontSize = 22.sp)
                    Text(s.letter, style = YType.Body.copy(fontSize = 16.sp, lineHeight = 23.sp), color = Y.Text, modifier = Modifier.padding(top = 8.dp))
                }
            }

            SectionLabel("Мои причины", Modifier.padding(top = 20.dp))
            if (s.reasons.isEmpty()) {
                Glass(Modifier.fillMaxWidth(), radius = 24.dp, padding = 16.dp, onClick = { nav.go(Route.Settings) }) {
                    Text("Добавь свои причины в настройках — в трудную минуту они помогают лучше всего.", style = YType.Body, color = Y.Text2)
                }
            } else {
                Glass(Modifier.fillMaxWidth(), radius = 24.dp, padding = 16.dp) {
                    s.reasons.forEach {
                        Row(Modifier.padding(vertical = 5.dp)) {
                            Text("✦ ", color = Y.Magenta, style = YType.Body)
                            Text(it, style = YType.Body, color = Y.Text)
                        }
                    }
                }
            }

            SectionLabel("Что ещё помогает", Modifier.padding(top = 20.dp))
            Glass(Modifier.fillMaxWidth(), radius = 24.dp, padding = 16.dp) {
                listOf(
                    "Выпей стакан холодной воды",
                    "Выйди на улицу на 5 минут",
                    "Съешь что-нибудь: яблоко, орехи, жвачка",
                    "Позвони или напиши кому-нибудь",
                    "Скажи себе: «отложу на 5 минут» — потом ещё на 5",
                    "Почисти зубы — вкус мяты сбивает тягу",
                    "Сделай 20 приседаний",
                    "Прими душ",
                ).forEach { Text("•  $it", style = YType.Body, color = Y.Text2, modifier = Modifier.padding(vertical = 3.dp)) }
            }

            Spacer(Modifier.height(20.dp))
            GradientButton("Я справился 💪", acc, Modifier.fillMaxWidth()) {
                scope.launch { repo.update(refreshWidgets = false) { it.copy(cravingsBeaten = it.cravingsBeaten + 1) } }
                Toast.makeText(ctx, "Отлично! Ещё одна победа.", Toast.LENGTH_SHORT).show()
                nav.back()
            }
            Spacer(Modifier.height(10.dp))
            GhostButton("Закрыть", Modifier.fillMaxWidth(), color = Y.Text2) { nav.back() }
            Spacer(Modifier.height(24.dp))
            Spacer(Modifier.navigationBarsPadding())
        }
    }
}
