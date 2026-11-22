package app.ryadom.core.designsystem.component

import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.ryadom.core.designsystem.theme.Illustration
import app.ryadom.core.designsystem.theme.Ry

/** Иконка из набора «Рядом». Цвет — по роли, по умолчанию цвет текста. */
@Composable
fun RyIcon(
    @DrawableRes icon: Int,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = Ry.colors.text,
    size: Dp = 24.dp,
) = Icon(painterResource(icon), contentDescription, modifier.size(size), tint = tint)

@Composable
fun RyIllustration(illustration: Illustration, modifier: Modifier = Modifier, contentDescription: String? = null) =
    Image(painterResource(illustration.res), contentDescription, modifier, contentScale = ContentScale.Fit)

/** Тактильный отклик подтверждения: CONFIRM на Android 11+, иначе короткий клик. */
class Haptics(private val view: android.view.View) {
    fun confirm() {
        view.performHapticFeedback(if (Build.VERSION.SDK_INT >= 30) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.VIRTUAL_KEY)
    }
    fun tick() { view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK) }
}

@Composable
fun rememberHaptics(): Haptics {
    val view = LocalView.current
    return remember(view) { Haptics(view) }
}

enum class ButtonStyle { Primary, Tonal, Ghost, Text, Danger }

@Composable
fun RyButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: ButtonStyle = ButtonStyle.Primary,
    @DrawableRes icon: Int? = null,
    enabled: Boolean = true,
    small: Boolean = false,
) {
    val c = Ry.colors
    val (bg, fg, border) = when (style) {
        ButtonStyle.Primary -> Triple(c.primaryDark, c.onPrimary, null)
        ButtonStyle.Tonal -> Triple(c.primaryContainer, c.primaryDark, null)
        ButtonStyle.Ghost -> Triple(Color.Transparent, c.text, c.border)
        ButtonStyle.Text -> Triple(Color.Transparent, c.primaryDark, null)
        ButtonStyle.Danger -> Triple(c.errorContainer, c.errorText, null)
    }
    val alpha = if (enabled) 1f else 0.45f
    Row(
        modifier = modifier
            .defaultMinSize(minHeight = if (small) 40.dp else 48.dp)
            .clip(Ry.shapes.pill)
            .background(bg.copy(alpha = bg.alpha * alpha))
            .then(if (border != null) Modifier.border(1.dp, border, Ry.shapes.pill) else Modifier)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = if (style == ButtonStyle.Text) 12.dp else if (small) 16.dp else 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (icon != null) {
            RyIcon(icon, null, tint = fg.copy(alpha = alpha), size = 20.dp)
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = Ry.type.label.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold),
            color = fg.copy(alpha = alpha), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Круглая кнопка-иконка с зоной касания 48 dp. */
@Composable
fun RyIconButton(
    @DrawableRes icon: Int,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = Ry.colors.text,
    enabled: Boolean = true,
) {
    Box(
        modifier
            .size(48.dp)
            .clip(CircleShape)
            .clickable(enabled = enabled, role = Role.Button, onClickLabel = contentDescription, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { RyIcon(icon, contentDescription, tint = if (enabled) tint else tint.copy(alpha = 0.4f)) }
}

@Composable
fun RyChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    @DrawableRes icon: Int? = null,
    leading: (@Composable () -> Unit)? = null,
) {
    val c = Ry.colors
    Row(
        modifier
            .defaultMinSize(minHeight = 40.dp)
            .clip(Ry.shapes.pill)
            .background(if (selected) c.primaryContainer else c.surface)
            .then(if (selected) Modifier else Modifier.border(1.dp, c.border, Ry.shapes.pill))
            .clickable(role = Role.Checkbox, onClick = onClick)
            .semantics { this.selected = selected }
            .padding(start = if (leading != null) 6.dp else 14.dp, end = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        leading?.invoke()
        if (icon != null) RyIcon(icon, null, tint = if (selected) c.primaryDark else c.text, size = 18.dp)
        Text(text, style = Ry.type.label, color = if (selected) c.primaryDark else c.text, maxLines = 1)
    }
}

/** Сегментированный переключатель: день / неделя / месяц и подобное. */
@Composable
fun RySegmented(options: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val c = Ry.colors
    Row(
        modifier
            .fillMaxWidth()
            .clip(Ry.shapes.pill)
            .background(c.surfaceMuted)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        options.forEachIndexed { i, label ->
            val on = i == selected
            Box(
                Modifier
                    .weight(1f)
                    .defaultMinSize(minHeight = 40.dp)
                    .clip(Ry.shapes.pill)
                    .background(if (on) c.surface else Color.Transparent)
                    .clickable(role = Role.Tab) { onSelect(i) }
                    .semantics { this.selected = on }
                    .padding(horizontal = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(label, style = Ry.type.label, color = if (on) c.text else c.textMuted, maxLines = 1,
                    overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
            }
        }
    }
}

enum class BadgeKind { Ok, Warn, Error, Neutral }

@Composable
fun RyBadge(text: String, kind: BadgeKind, modifier: Modifier = Modifier, @DrawableRes icon: Int? = null) {
    val c = Ry.colors
    val (bg, fg) = when (kind) {
        BadgeKind.Ok -> c.primaryContainer to c.primaryDark
        BadgeKind.Warn -> c.warningContainer to c.warningText
        BadgeKind.Error -> c.errorContainer to c.errorText
        BadgeKind.Neutral -> c.surfaceMuted to c.textMuted
    }
    Row(
        modifier.clip(Ry.shapes.pill).background(bg).padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (icon != null) RyIcon(icon, null, tint = fg, size = 14.dp)
        Text(text, style = Ry.type.caption.copy(fontSize = Ry.type.caption.fontSize * 0.95f), color = fg, maxLines = 1)
    }
}

/** Аватар-инициал в цвете человека. Имя всегда подписано рядом, поэтому цвет — не единственный признак. */
@Composable
fun RyAvatar(initial: String, tone: Color, container: Color, modifier: Modifier = Modifier, size: Dp = 28.dp) {
    Box(modifier.size(size).clip(CircleShape).background(container), contentAlignment = Alignment.Center) {
        val style = when {
            size >= 64.dp -> Ry.type.display
            size >= 40.dp -> Ry.type.subtitle
            else -> Ry.type.caption.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
        }
        Text(initial, style = style, color = tone, maxLines = 1)
    }
}

/** Поверхность-группа: белый лист со скруглением 24 и тонкой рамкой. */
@Composable
fun RyGroup(
    modifier: Modifier = Modifier,
    header: (@Composable RowScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = Ry.colors
    Column(
        modifier
            .fillMaxWidth()
            .clip(Ry.shapes.l)
            .background(c.surface)
            .border(BorderStroke(1.dp, c.border), Ry.shapes.l),
    ) {
        if (header != null) {
            Row(
                Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                content = header,
            )
        }
        content()
    }
}

/** Заголовок группы: иконка + подпись капителью + счётчик справа. */
@Composable
fun RowScope.GroupHeader(@DrawableRes icon: Int?, title: String, trailing: String? = null) {
    val c = Ry.colors
    if (icon != null) RyIcon(icon, null, tint = c.textMuted, size = 18.dp)
    Text(title.uppercase(), style = Ry.type.overline, color = c.textMuted, modifier = Modifier.weight(1f).semantics { heading() })
    if (trailing != null) Text(trailing, style = Ry.type.caption, color = c.textMuted)
}

@Composable
fun RyDivider(modifier: Modifier = Modifier, start: Dp = 0.dp) {
    Box(modifier.padding(start = start).fillMaxWidth().height(1.dp).background(Ry.colors.border))
}

/** Строка списка: иконка, заголовок, подпись, хвост. Вся строка — одна зона касания. */
@Composable
fun RyListRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    @DrawableRes icon: Int? = null,
    titleColor: Color = Ry.colors.text,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = { RyIcon(app.ryadom.core.designsystem.theme.RyIcons.ChevronRight, null, tint = Ry.colors.textSecondary, size = 20.dp) },
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 56.dp)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        leading?.invoke()
        if (icon != null) RyIcon(icon, null, tint = Ry.colors.textMuted)
        Column(Modifier.weight(1f)) {
            Text(title, style = Ry.type.subtitle, color = titleColor)
            if (subtitle != null) Text(subtitle, style = Ry.type.bodySmall, color = Ry.colors.textMuted)
        }
        trailing?.invoke()
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier, action: (@Composable () -> Unit)? = null) {
    Row(modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(text, style = Ry.type.title, color = Ry.colors.text, modifier = Modifier.weight(1f).semantics { heading() })
        action?.invoke()
    }
}

@Composable
fun ScreenTitle(text: String, modifier: Modifier = Modifier, subtitle: String? = null) {
    Column(modifier.fillMaxWidth().widthIn(max = 640.dp)) {
        Text(text, style = Ry.type.display, color = Ry.colors.text, modifier = Modifier.semantics { heading() })
        if (subtitle != null) Text(subtitle, style = Ry.type.caption, color = Ry.colors.textMuted, modifier = Modifier.padding(top = 4.dp))
    }
}

val ScreenPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp)
