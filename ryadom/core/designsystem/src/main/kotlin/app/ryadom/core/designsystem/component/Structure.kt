package app.ryadom.core.designsystem.component

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.ryadom.core.designsystem.theme.Illustration
import app.ryadom.core.designsystem.theme.Ry
import app.ryadom.core.designsystem.theme.RyIcons

/** Верхняя панель внутренних экранов: назад/закрыть, заголовок по центру, действие справа. */
@Composable
fun RyTopBar(
    title: String,
    modifier: Modifier = Modifier,
    @DrawableRes navIcon: Int? = RyIcons.ArrowLeft,
    navLabel: String = "",
    onNav: (() -> Unit)? = null,
    actions: @Composable () -> Unit = {},
) {
    Row(
        modifier.fillMaxWidth().height(56.dp).padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (navIcon != null && onNav != null) RyIconButton(navIcon, navLabel, onNav) else Spacer(Modifier.width(48.dp))
        Text(
            title, style = Ry.type.subtitle.copy(fontSize = Ry.type.title.fontSize * 0.95f), color = Ry.colors.text,
            textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).semantics { heading() },
        )
        Box(Modifier.defaultMinSize(minWidth = 48.dp), contentAlignment = Alignment.CenterEnd) { actions() }
    }
}

data class BottomItem(@DrawableRes val icon: Int, val label: String)

/** Нижняя навигация: 4 вкладки и кнопка «+» в центре — видна всегда и не перекрывает список. */
@Composable
fun RyBottomBar(
    items: List<BottomItem>,
    selected: Int,
    onSelect: (Int) -> Unit,
    addLabel: String,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = Ry.colors
    Column(modifier.fillMaxWidth().background(c.surface)) {
        RyDivider()
        Row(
            Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.navigationBars).padding(horizontal = 6.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items.forEachIndexed { i, item ->
                if (i == items.size / 2) {
                    Box(Modifier.width(64.dp), contentAlignment = Alignment.Center) {
                        Box(
                            Modifier.size(48.dp).clip(RoundedCornerShape(16.dp)).background(c.primaryDark)
                                .clickable(role = Role.Button, onClickLabel = addLabel, onClick = onAdd),
                            contentAlignment = Alignment.Center,
                        ) { RyIcon(RyIcons.Plus, addLabel, tint = c.onPrimary) }
                    }
                }
                val on = i == selected
                Column(
                    Modifier.weight(1f).clip(Ry.shapes.m).clickable(role = Role.Tab) { onSelect(i) }.padding(vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Box(
                        Modifier.width(56.dp).height(30.dp).clip(Ry.shapes.pill).background(if (on) c.primaryContainer else c.surface),
                        contentAlignment = Alignment.Center,
                    ) { RyIcon(item.icon, null, tint = if (on) c.primaryDark else c.textMuted, size = 22.dp) }
                    Text(item.label, style = Ry.type.caption.copy(fontSize = Ry.type.caption.fontSize * 0.92f),
                        color = if (on) c.text else c.textMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

/** Пустое состояние: иллюстрация, одна фраза, настоящее действие. */
@Composable
fun EmptyState(
    illustration: Illustration,
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    primary: Pair<String, () -> Unit>? = null,
    secondary: Pair<String, () -> Unit>? = null,
    @DrawableRes primaryIcon: Int? = null,
) {
    Column(
        modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        RyIllustration(illustration, Modifier.widthIn(max = 260.dp).fillMaxWidth())
        Text(title, style = Ry.type.headline, color = Ry.colors.text, textAlign = TextAlign.Center, modifier = Modifier.semantics { heading() })
        Text(body, style = Ry.type.bodySmall, color = Ry.colors.textMuted, textAlign = TextAlign.Center, modifier = Modifier.widthIn(max = 320.dp))
        if (primary != null) RyButton(primary.first, primary.second, Modifier.padding(top = 4.dp), icon = primaryIcon)
        if (secondary != null) RyButton(secondary.first, secondary.second, style = ButtonStyle.Text)
    }
}

/** Ошибка: что случилось и что сделать. Без извинений и без красного экрана. */
@Composable
fun ErrorState(title: String, body: String, retryLabel: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    EmptyState(Illustration.Missed, title, body, modifier, primary = retryLabel to onRetry, primaryIcon = RyIcons.Repeat)
}

/** Плашка «нет сети»: показывается сверху, данные при этом доступны. */
@Composable
fun NoticeBanner(text: String, @DrawableRes icon: Int, modifier: Modifier = Modifier, warn: Boolean = false, action: Pair<String, () -> Unit>? = null) {
    val c = Ry.colors
    Row(
        modifier.fillMaxWidth().clip(Ry.shapes.m).background(if (warn) c.warningContainer else c.surfaceMuted)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        RyIcon(icon, null, tint = if (warn) c.warningText else c.textMuted, size = 20.dp)
        Text(text, style = Ry.type.bodySmall, color = if (warn) c.warningText else c.text, modifier = Modifier.weight(1f))
        if (action != null) RyButton(action.first, action.second, style = ButtonStyle.Text, small = true)
    }
}

/** Скелетон той же формы, что и строки списка. */
@Composable
fun SkeletonList(rows: Int = 4, modifier: Modifier = Modifier) {
    val t = rememberInfiniteTransition(label = "skeleton")
    val a by t.animateFloat(0.5f, 1f, infiniteRepeatable(tween(800), RepeatMode.Reverse), label = "a")
    val c = Ry.colors
    Column(modifier.fillMaxWidth().alpha(a), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Box(Modifier.width(220.dp).height(30.dp).clip(Ry.shapes.s).background(c.surfaceMuted))
        Box(Modifier.fillMaxWidth().height(6.dp).clip(Ry.shapes.pill).background(c.surfaceMuted))
        RyGroup {
            repeat(rows) { i ->
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.width(40.dp).height(14.dp).clip(Ry.shapes.s).background(c.surfaceMuted))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(Modifier.fillMaxWidth(0.6f).height(14.dp).clip(Ry.shapes.s).background(c.surfaceMuted))
                        Box(Modifier.fillMaxWidth(0.4f).height(12.dp).clip(Ry.shapes.s).background(c.surfaceMuted))
                    }
                    Box(Modifier.size(30.dp).clip(Ry.shapes.pill).background(c.surfaceMuted))
                }
                if (i < rows - 1) RyDivider(start = 70.dp)
            }
        }
    }
}

@Composable
fun RyConfirmDialog(
    title: String,
    text: String,
    confirmLabel: String,
    dismissLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    destructive: Boolean = false,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Ry.colors.surface,
        shape = Ry.shapes.l,
        title = { Text(title, style = Ry.type.headline, color = Ry.colors.text) },
        text = { Text(text, style = Ry.type.body, color = Ry.colors.textMuted) },
        confirmButton = { RyButton(confirmLabel, onConfirm, style = if (destructive) ButtonStyle.Danger else ButtonStyle.Primary, small = true) },
        dismissButton = { RyButton(dismissLabel, onDismiss, style = ButtonStyle.Text, small = true) },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RySheet(
    onDismiss: () -> Unit,
    title: String? = null,
    state: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    content: @Composable () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = state,
        containerColor = Ry.colors.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = { Box(Modifier.padding(top = 10.dp).width(36.dp).height(4.dp).clip(Ry.shapes.pill).background(Ry.colors.border)) },
    ) {
        Column(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (title != null) Text(title, style = Ry.type.headline, color = Ry.colors.text, modifier = Modifier.padding(top = 8.dp).semantics { heading() })
            content()
        }
    }
}

/** Плитка-сводка (остатки, статистика). */
@Composable
fun StatTile(value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier.clip(Ry.shapes.m).background(Ry.colors.surface).border(1.dp, Ry.colors.border, Ry.shapes.m).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(value, style = Ry.type.headline, color = Ry.colors.text)
        Text(label, style = Ry.type.caption, color = Ry.colors.textMuted)
    }
}

/** Ограничение ширины контента на планшетах и раскладушках. */
fun Modifier.contentWidth(max: Dp = 640.dp) = this.fillMaxWidth().widthIn(max = max)

@Composable
fun CenteredContent(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Box(Modifier.widthIn(max = 640.dp).fillMaxWidth()) { content() }
    }
}
