package app.ryadom.core.designsystem.component

import androidx.annotation.DrawableRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import app.ryadom.core.designsystem.theme.Ry

/** Поле ввода «Рядом»: подпись сверху, поверхность с рамкой, ошибка текстом под полем. */
@Composable
fun RyTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    error: String? = null,
    singleLine: Boolean = true,
    minLines: Int = 1,
    password: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    trailing: (@Composable () -> Unit)? = null,
) {
    val c = Ry.colors
    var focused by remember { mutableStateOf(false) }
    val borderColor by animateColorAsState(
        when {
            error != null -> c.error
            focused -> c.primaryDark
            else -> c.border
        }, tween(160), label = "border",
    )
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, style = Ry.type.caption, color = c.textMuted)
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = singleLine,
            minLines = minLines,
            textStyle = Ry.type.body.copy(color = c.text),
            cursorBrush = SolidColor(c.primaryDark),
            visualTransformation = if (password) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { focused = it.isFocused }
                .semantics {
                    contentDescription = label
                    if (error != null) error(error)
                },
            decorationBox = { inner ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 52.dp)
                        .clip(Ry.shapes.m)
                        .background(c.surface)
                        .border(if (focused) 1.5.dp else 1.dp, borderColor, Ry.shapes.m)
                        .padding(start = 16.dp, end = if (trailing != null) 4.dp else 16.dp, top = 14.dp, bottom = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.weight(1f)) {
                        if (value.isEmpty() && placeholder != null) Text(placeholder, style = Ry.type.body, color = c.textSecondary)
                        inner()
                    }
                    trailing?.invoke()
                }
            },
        )
        if (error != null) Text(error, style = Ry.type.caption, color = c.errorText)
    }
}

@Composable
fun RySwitch(checked: Boolean, onCheckedChange: ((Boolean) -> Unit)?, modifier: Modifier = Modifier) {
    val c = Ry.colors
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        colors = SwitchDefaults.colors(
            checkedThumbColor = c.surface, checkedTrackColor = c.primaryDark, checkedBorderColor = c.primaryDark,
            uncheckedThumbColor = c.textSecondary, uncheckedTrackColor = c.surfaceMuted, uncheckedBorderColor = c.border,
        ),
    )
}

/** Строка настройки с переключателем: касание по всей строке переключает значение. */
@Composable
fun RySwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    @DrawableRes icon: Int? = null,
) {
    Row(
        modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 56.dp)
            .clickable(role = Role.Switch) { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (icon != null) RyIcon(icon, null, tint = Ry.colors.textMuted)
        Column(Modifier.weight(1f)) {
            Text(title, style = Ry.type.body, color = Ry.colors.text)
            if (subtitle != null) Text(subtitle, style = Ry.type.bodySmall, color = Ry.colors.textMuted)
        }
        RySwitch(checked, null)
    }
}

enum class CheckState { Idle, Due, Done }

/**
 * Главная кнопка приложения: отметка приёма в одно касание.
 * Круг заливается, галочка прорисовывается линией. Состояние озвучивается TalkBack.
 */
@Composable
fun RyCheckButton(
    state: CheckState,
    onClick: () -> Unit,
    actionLabel: String,
    stateLabel: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val c = Ry.colors
    val done = state == CheckState.Done
    val fill by animateColorAsState(if (done) c.primary else c.surface.copy(alpha = 0f), tween(200), label = "fill")
    val ring by animateColorAsState(
        when (state) { CheckState.Done -> c.primary; CheckState.Due -> c.primaryDark; CheckState.Idle -> c.border },
        tween(200), label = "ring",
    )
    val progress by animateFloatAsState(if (done) 1f else 0f, tween(240, delayMillis = if (done) 60 else 0), label = "check")
    val haptics = rememberHaptics()
    Box(
        modifier
            .size(48.dp)
            .clip(CircleShape)
            .clickable(enabled = enabled, role = Role.Checkbox, onClickLabel = actionLabel) {
                if (!done) haptics.confirm()
                onClick()
            }
            .semantics { stateDescription = stateLabel },
        contentAlignment = Alignment.Center,
    ) {
        val strokeW = if (state == CheckState.Due) 2.dp else 1.75.dp
        val check = c.onPrimary
        Canvas(Modifier.size(30.dp)) {
            val r = size.minDimension / 2
            drawCircle(fill, r)
            drawCircle(ring, r - strokeW.toPx() / 2, style = Stroke(strokeW.toPx()))
            if (progress > 0f) {
                val p = Path().apply {
                    moveTo(size.width * 0.29f, size.height * 0.52f)
                    lineTo(size.width * 0.44f, size.height * 0.67f)
                    lineTo(size.width * 0.72f, size.height * 0.37f)
                }
                val m = PathMeasure().apply { setPath(p, false) }
                val seg = Path()
                m.getSegment(0f, m.length * progress, seg, true)
                drawPath(seg, check, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
        }
    }
}

/** Прогресс дня: по сегменту на приём. Заполненные — принято, тёплый — «сейчас». */
@Composable
fun SegmentedProgress(total: Int, done: Int, modifier: Modifier = Modifier, highlightNext: Boolean = true) {
    val c = Ry.colors
    Row(modifier.fillMaxWidth().height(6.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        if (total == 0) {
            Box(Modifier.weight(1f).height(6.dp).clip(Ry.shapes.pill).background(c.surfaceMuted))
        }
        repeat(total.coerceAtMost(24)) { i ->
            val target = when {
                i < done -> c.primary
                i == done && highlightNext -> c.warm
                else -> c.surfaceMuted
            }
            val color by animateColorAsState(target, tween(450), label = "seg")
            Box(Modifier.weight(1f).height(6.dp).clip(Ry.shapes.pill).background(color))
        }
    }
}

/** Полоса остатка: обычная — шалфей, мало — песочное предупреждение (без красного). */
@Composable
fun RyMeter(fraction: Float, warn: Boolean, modifier: Modifier = Modifier) {
    val c = Ry.colors
    val f by animateFloatAsState(fraction.coerceIn(0f, 1f), tween(450), label = "meter")
    Canvas(modifier.fillMaxWidth().height(6.dp)) {
        val h = size.height
        drawRoundRect(c.surfaceMuted, cornerRadius = androidx.compose.ui.geometry.CornerRadius(h / 2))
        if (f > 0f) drawRoundRect(
            if (warn) c.warning else c.primary,
            size = size.copy(width = (size.width * f).coerceAtLeast(h)),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(h / 2),
        )
    }
}

/** Степпер для количества: «−  2  +». */
@Composable
fun RyStepper(
    value: Double,
    onChange: (Double) -> Unit,
    decreaseLabel: String,
    increaseLabel: String,
    modifier: Modifier = Modifier,
    step: Double = 1.0,
    min: Double = step,
    format: (Double) -> String = { if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() },
) {
    val c = Ry.colors
    Row(
        modifier.clip(Ry.shapes.m).background(c.surface).border(1.dp, c.border, Ry.shapes.m),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RyIconButton(app.ryadom.core.designsystem.theme.RyIcons.ChevronLeft, decreaseLabel, { onChange((value - step).coerceAtLeast(min)) },
            enabled = value - step >= min - 1e-9, tint = c.textMuted)
        Text(format(value), style = Ry.type.title, color = c.text, modifier = Modifier.widthIn(min = 32.dp))
        RyIconButton(app.ryadom.core.designsystem.theme.RyIcons.ChevronRight, increaseLabel, { onChange(value + step) }, tint = c.textMuted)
    }
}

/** Точка-метка дня в календаре. */
@Composable
fun DayMark(kind: Int, modifier: Modifier = Modifier, onDark: Boolean = false) {
    // 0 — принято, 1 — впереди, 2 — пропуск
    val c = Ry.colors
    Canvas(modifier.size(5.dp)) {
        val r = size.minDimension / 2
        when (kind) {
            0 -> drawCircle(if (onDark) c.onPrimary else c.primary, r)
            1 -> drawCircle(if (onDark) c.onPrimary else c.textSecondary, r - 0.5.dp.toPx(), style = Stroke(1.dp.toPx()))
            else -> drawCircle(c.warm, r)
        }
    }
}

internal fun Offset.isUnspecifiedOrZero() = this == Offset.Zero
