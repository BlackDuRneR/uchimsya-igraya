package com.yasnost.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.yasnost.app.data.HabitType
import com.yasnost.app.ui.theme.Accent
import com.yasnost.app.ui.theme.Y
import com.yasnost.app.ui.theme.YType
import com.yasnost.app.ui.theme.accent
import com.yasnost.app.ui.theme.icon
import kotlinx.coroutines.delay

/** Текущее время, тикающее раз в секунду. */
@Composable
fun rememberNow(periodMs: Long = 1000L): Long {
    val now = remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(periodMs) {
        while (true) {
            now.longValue = System.currentTimeMillis()
            delay(periodMs - System.currentTimeMillis() % periodMs)
        }
    }
    return now.longValue
}

/** Фон «северное сияние»: медленно плывущие цветные пятна на глубоком чёрном. */
@Composable
fun AuroraBackground(accentA: Color = Y.Violet, accentB: Color = Y.Teal, content: @Composable BoxScope.() -> Unit) {
    val t = rememberInfiniteTransition(label = "aurora")
    val p by t.animateFloat(0f, 1f, infiniteRepeatable(tween(18000, easing = LinearEasing), RepeatMode.Reverse), label = "p")
    Box(
        Modifier
            .fillMaxSize()
            .background(Y.Bg)
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            drawCircle(
                Brush.radialGradient(listOf(accentA.copy(alpha = 0.28f), Color.Transparent), center = Offset(w * (0.1f + 0.3f * p), h * 0.08f), radius = w * 0.9f),
                radius = w * 0.9f, center = Offset(w * (0.1f + 0.3f * p), h * 0.08f)
            )
            drawCircle(
                Brush.radialGradient(listOf(accentB.copy(alpha = 0.18f), Color.Transparent), center = Offset(w * (0.95f - 0.25f * p), h * 0.32f), radius = w * 0.8f),
                radius = w * 0.8f, center = Offset(w * (0.95f - 0.25f * p), h * 0.32f)
            )
            drawCircle(
                Brush.radialGradient(listOf(Y.Sky.copy(alpha = 0.08f), Color.Transparent), center = Offset(w * 0.3f, h * (0.85f - 0.1f * p)), radius = w),
                radius = w, center = Offset(w * 0.3f, h * (0.85f - 0.1f * p))
            )
        }
        content()
    }
}

/** Стеклянная карточка. */
@Composable
fun Glass(
    modifier: Modifier = Modifier,
    radius: Dp = 28.dp,
    tint: Color = Color.Transparent,
    onClick: (() -> Unit)? = null,
    padding: Dp = 20.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(radius)
    Column(
        modifier
            .clip(shape)
            .background(Brush.verticalGradient(listOf(Color(0x1AFFFFFF), Color(0x0AFFFFFF))))
            .background(tint)
            .border(1.dp, Brush.verticalGradient(listOf(Color(0x33FFFFFF), Color(0x08FFFFFF))), shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(padding),
        content = content,
    )
}

/** Кольцо прогресса с градиентом и светящимся концом. */
@Composable
fun ProgressRing(
    progress: Float,
    accent: Accent,
    modifier: Modifier = Modifier,
    stroke: Dp = 14.dp,
    content: @Composable BoxScope.() -> Unit = {},
) {
    val anim by animateFloatAsState(progress, tween(900), label = "ring")
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val sw = stroke.toPx()
            val inset = sw / 2 + 6.dp.toPx()
            val arcSize = Size(size.width - inset * 2, size.height - inset * 2)
            val tl = Offset(inset, inset)
            drawArc(Color(0x14FFFFFF), 0f, 360f, false, tl, arcSize, style = Stroke(sw))
            rotate(-90f) {
                val sweep = Brush.sweepGradient(listOf(accent.a, accent.b, accent.a))
                drawArc(sweep, 0f, 360f * anim.coerceAtLeast(0.005f), false, tl, arcSize, style = Stroke(sw, cap = StrokeCap.Round))
            }
            // светящаяся точка на конце
            val ang = Math.toRadians((360.0 * anim) - 90.0)
            val r = arcSize.width / 2
            val c = Offset(size.width / 2 + (r * Math.cos(ang)).toFloat(), size.height / 2 + (r * Math.sin(ang)).toFloat())
            drawCircle(Brush.radialGradient(listOf(accent.b.copy(alpha = 0.7f), Color.Transparent), c, sw * 1.6f), sw * 1.6f, c)
            drawCircle(Color.White, sw * 0.28f, c)
        }
        content()
    }
}

@Composable
fun HabitBadge(type: HabitType, size: Dp = 48.dp) {
    val a = type.accent()
    Box(
        Modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.34f))
            .background(a.brush),
        contentAlignment = Alignment.Center,
    ) {
        Icon(painterResource(type.icon()), null, tint = Color(0xFF0B0C14), modifier = Modifier.size(size * 0.52f))
    }
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(text.uppercase(), style = YType.Label, color = Y.Text3, modifier = modifier.padding(start = 4.dp, top = 8.dp, bottom = 10.dp))
}

@Composable
fun StatTile(label: String, value: String, unit: String, accent: Color, modifier: Modifier = Modifier) {
    Glass(modifier, radius = 22.dp, padding = 16.dp) {
        Text(label.uppercase(), style = YType.Label, color = Y.Text3, maxLines = 1)
        Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(top = 8.dp)) {
            Text(value, style = YType.Title, color = Y.Text, maxLines = 1)
            if (unit.isNotEmpty()) Text(" $unit", style = YType.Sub, color = accent, modifier = Modifier.padding(bottom = 1.dp))
        }
    }
}

@Composable
fun GradientButton(text: String, accent: Accent, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .clip(RoundedCornerShape(20.dp))
            .background(accent.brush)
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp, horizontal = 20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = YType.Sub, color = Color(0xFF0B0C14), textAlign = TextAlign.Center)
    }
}

@Composable
fun GhostButton(text: String, modifier: Modifier = Modifier, color: Color = Y.Text, onClick: () -> Unit) {
    Box(
        modifier
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, Y.Stroke, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 15.dp, horizontal = 20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = YType.Sub, color = color, textAlign = TextAlign.Center)
    }
}

@Composable
fun Chip(text: String, selected: Boolean, accent: Accent, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(50)
    Box(
        modifier
            .clip(shape)
            .then(if (selected) Modifier.background(accent.brush) else Modifier.background(Color(0x10FFFFFF)))
            .border(1.dp, if (selected) Color.Transparent else Y.Stroke, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = YType.Sub.copy(fontSize = YType.Body.fontSize), color = if (selected) Color(0xFF0B0C14) else Y.Text2)
    }
}

@Composable
fun Dot(color: Color, size: Dp = 8.dp) {
    Box(Modifier.size(size).clip(CircleShape).background(color))
}

@Composable
fun Fill(modifier: Modifier = Modifier) = Box(modifier.fillMaxWidth())
