package com.yasnost.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yasnost.app.Nav
import com.yasnost.app.data.AppState
import com.yasnost.app.domain.Achievements
import com.yasnost.app.domain.Award
import com.yasnost.app.domain.AwardCat
import com.yasnost.app.domain.AwardCtx
import com.yasnost.app.domain.Awards
import com.yasnost.app.domain.Fmt
import com.yasnost.app.domain.Level
import com.yasnost.app.domain.Levels
import com.yasnost.app.ui.components.Glass
import com.yasnost.app.ui.components.HabitBadge
import com.yasnost.app.ui.components.ProgressRing
import com.yasnost.app.ui.components.SectionLabel
import com.yasnost.app.ui.components.Segmented
import com.yasnost.app.ui.components.rememberNow
import com.yasnost.app.ui.theme.Accent
import com.yasnost.app.ui.theme.Y
import com.yasnost.app.ui.theme.YType
import com.yasnost.app.ui.theme.accent

private val LEVEL = Accent(Y.Amber, Y.Magenta)

@Composable
fun LevelCard(level: Level, modifier: Modifier = Modifier) {
    Glass(modifier.fillMaxWidth(), radius = 28.dp, padding = 18.dp, tint = Y.Amber.copy(alpha = 0.05f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ProgressRing(level.progress, LEVEL, Modifier.size(84.dp), stroke = 8.dp) {
                Text("${level.n}", style = YType.Big, color = Y.Text)
            }
            Column(Modifier.padding(start = 16.dp)) {
                Text("УРОВЕНЬ ${level.n}", style = YType.Label, color = Y.Amber)
                Text(level.title, style = YType.Title, color = Y.Text)
                Text(
                    if (level.to != null) "${Fmt.intNum(level.xp)} / ${Fmt.intNum(level.to)} XP" else "${Fmt.intNum(level.xp)} XP · максимум",
                    style = YType.Body.copy(fontSize = 13.sp), color = Y.Text2
                )
            }
        }
        Text(
            "Опыт начисляется за каждый чистый день, награды, отметки в дневнике и побеждённую тягу.",
            style = YType.Body.copy(fontSize = 12.sp), color = Y.Text3, modifier = Modifier.padding(top = 10.dp)
        )
    }
}

@Composable
fun AwardsScreen(s: AppState, nav: Nav) {
    val now = rememberNow(60_000)
    var tab by remember { mutableIntStateOf(0) }
    val ctx = AwardCtx(s, now)
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(18.dp))
        Text("ДОСТИЖЕНИЯ", style = YType.Label, color = Y.Text3)
        Text("Награды", style = YType.Big, color = Y.Text, modifier = Modifier.padding(top = 4.dp, bottom = 14.dp))
        LevelCard(Levels.of(s, now))
        Spacer(Modifier.height(14.dp))

        val timeTotal = s.habits.sumOf { h -> Achievements.list.count { now - h.startMillis >= it.millis } }
        val globalGot = Awards.unlocked(ctx).size
        Segmented(listOf("За время  $timeTotal", "Особые  $globalGot/${Awards.list.size}"), tab, LEVEL) { tab = it }
        Spacer(Modifier.height(6.dp))

        if (tab == 0) {
            if (s.habits.isEmpty()) Text("Добавь привычку на главной, и здесь появятся награды.", style = YType.Body, color = Y.Text3, modifier = Modifier.padding(top = 12.dp))
            s.habits.forEach { h ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 14.dp, bottom = 12.dp)) {
                    HabitBadge(h.type, 34.dp)
                    Text(h.title, style = YType.Sub, color = Y.Text, modifier = Modifier.padding(start = 10.dp).weight(1f))
                    Text("${Achievements.list.count { now - h.startMillis >= it.millis }}/${Achievements.list.size}", style = YType.Sub, color = h.type.accent().b)
                }
                MedalsGrid(now - h.startMillis, h.type.accent())
            }
        } else {
            AwardCat.entries.forEach { cat ->
                val items = Awards.list.filter { it.cat == cat }
                if (cat == AwardCat.INTIMACY && !s.settings.intimacyEnabled) return@forEach
                SectionLabel(cat.title, Modifier.padding(top = 10.dp))
                items.chunked(3).forEach { row ->
                    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min).padding(bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        row.forEach { a -> AwardTile(a, ctx, Modifier.weight(1f).fillMaxHeight()) }
                        repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun AwardTile(a: Award, ctx: AwardCtx, modifier: Modifier) {
    val v = a.value(ctx)
    val got = v >= a.target
    val p = (v / a.target).toFloat().coerceIn(0f, 1f)
    Column(
        modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (got) LEVEL.soft else Color(0x08FFFFFF))
            .border(1.dp, if (got) Y.Amber.copy(alpha = 0.5f) else Y.Stroke, RoundedCornerShape(20.dp))
            .padding(vertical = 12.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.size(46.dp).clip(CircleShape).then(if (got) Modifier.background(LEVEL.brush) else Modifier.background(Color(0x10FFFFFF))),
            contentAlignment = Alignment.Center,
        ) { Text(a.emoji, fontSize = 22.sp, modifier = Modifier.alpha(if (got) 1f else 0.35f)) }
        Text(a.title, style = YType.Sub.copy(fontSize = 13.sp), color = if (got) Y.Text else Y.Text2, textAlign = TextAlign.Center, maxLines = 2, modifier = Modifier.padding(top = 6.dp))
        Text(a.desc, style = YType.Body.copy(fontSize = 10.sp, lineHeight = 12.sp), color = Y.Text3, textAlign = TextAlign.Center, maxLines = 3)
        Spacer(Modifier.weight(1f))
        if (!got) {
            Box(Modifier.padding(top = 8.dp).fillMaxWidth().height(4.dp).clip(RoundedCornerShape(50)).background(Color(0x14FFFFFF))) {
                Box(Modifier.fillMaxWidth(p.coerceAtLeast(0.03f)).height(4.dp).clip(RoundedCornerShape(50)).background(LEVEL.brush))
            }
        }
    }
}
