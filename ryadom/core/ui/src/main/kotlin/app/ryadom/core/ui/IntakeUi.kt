package app.ryadom.core.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.ryadom.core.designsystem.component.BadgeKind
import app.ryadom.core.designsystem.component.CheckState
import app.ryadom.core.designsystem.component.RyAvatar
import app.ryadom.core.designsystem.component.RyBadge
import app.ryadom.core.designsystem.component.RyCheckButton
import app.ryadom.core.designsystem.component.RyIcon
import app.ryadom.core.designsystem.theme.Ry
import app.ryadom.core.designsystem.theme.RyIcons
import app.ryadom.core.l10n.R
import app.ryadom.core.model.FamilyGroup
import app.ryadom.core.model.Intake
import app.ryadom.core.model.IntakeStatus
import app.ryadom.core.model.Person
import app.ryadom.core.model.SyncState
import java.time.Duration
import java.time.ZoneId

@Composable
fun PersonAvatar(person: Person, modifier: Modifier = Modifier, size: Dp = 28.dp) {
    val p = person.colors()
    RyAvatar(person.initial, p.tone, p.container, modifier, size)
}

@Composable
fun PersonDot(person: Person, modifier: Modifier = Modifier) {
    Box(modifier.size(8.dp).clip(CircleShape).background(person.colors().tone))
}

@Composable
fun IntakeStatus.label(): String = stringResource(
    when (this) {
        IntakeStatus.UPCOMING -> R.string.status_upcoming
        IntakeStatus.DUE -> R.string.status_due
        IntakeStatus.TAKEN -> R.string.status_taken
        IntakeStatus.SNOOZED -> R.string.status_snoozed
        IntakeStatus.SKIPPED -> R.string.status_skipped
        IntakeStatus.CANCELLED -> R.string.status_cancelled
    },
)

/**
 * Строка приёма — главный элемент приложения.
 * Время слева, название и «кому · сколько» в центре, круг отметки справа (48 dp, одно касание).
 * Долгое нажатие или касание по строке открывает действия: позже, пропустить, сообщение.
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun IntakeRow(
    intake: Intake,
    family: FamilyGroup,
    onCheck: () -> Unit,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
    showPerson: Boolean = true,
    zone: ZoneId = ZoneId.systemDefault(),
) {
    val c = Ry.colors
    val status = intake.status
    val bg by animateColorAsState(
        if (status == IntakeStatus.DUE) c.primaryContainer.copy(alpha = 0.55f) else Color.Transparent, tween(200), label = "rowBg",
    )
    val dim = status == IntakeStatus.TAKEN || status == IntakeStatus.SKIPPED || status == IntakeStatus.CANCELLED
    Row(
        modifier
            .fillMaxWidth()
            .background(bg)
            .combinedClickable(role = Role.Button, onClickLabel = stringResource(R.string.intake_actions), onClick = onOpen, onLongClick = onOpen)
            .padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            intake.effectiveAt.toLocalTime().hhmm(),
            style = Ry.type.number,
            color = when (status) {
                IntakeStatus.DUE -> c.primaryDark
                IntakeStatus.SNOOZED -> c.warningText
                else -> c.text
            },
            modifier = Modifier.width(50.dp),
        )
        Column(Modifier.weight(1f).padding(end = 4.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    intake.medication.name,
                    style = Ry.type.subtitle,
                    color = if (dim) c.textMuted else c.text,
                    textDecoration = if (status == IntakeStatus.SKIPPED || status == IntakeStatus.CANCELLED) TextDecoration.LineThrough else null,
                    maxLines = 2, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.align(Alignment.CenterVertically),
                )
                StatusBadge(intake, Modifier.align(Alignment.CenterVertically))
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (showPerson) PersonDot(intake.person)
                Text(metaText(intake, family, showPerson, zone), style = Ry.type.bodySmall, color = c.textMuted,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
        if (status == IntakeStatus.CANCELLED) {
            Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) { RyIcon(RyIcons.Close, null, tint = c.textSecondary, size = 20.dp) }
        } else {
            RyCheckButton(
                state = when (status) {
                    IntakeStatus.TAKEN -> CheckState.Done
                    IntakeStatus.DUE -> CheckState.Due
                    else -> CheckState.Idle
                },
                onClick = onCheck,
                actionLabel = stringResource(if (status == IntakeStatus.TAKEN) R.string.intake_undo else R.string.intake_mark),
                stateLabel = status.label(),
            )
        }
    }
}

@Composable
private fun StatusBadge(intake: Intake, modifier: Modifier) {
    when (intake.status) {
        IntakeStatus.DUE -> RyBadge(stringResource(R.string.status_due), BadgeKind.Ok, modifier)
        IntakeStatus.SNOOZED -> {
            val mins = Duration.between(intake.scheduledAt, intake.effectiveAt).toMinutes()
            RyBadge(stringResource(R.string.badge_snoozed_minutes, mins), BadgeKind.Warn, modifier, RyIcons.Later)
        }
        IntakeStatus.SKIPPED -> RyBadge(stringResource(R.string.status_skipped), BadgeKind.Neutral, modifier, RyIcons.Skip)
        IntakeStatus.CANCELLED -> RyBadge(stringResource(R.string.status_cancelled), BadgeKind.Neutral, modifier)
        else -> Unit
    }
}

@Composable
private fun metaText(intake: Intake, family: FamilyGroup, showPerson: Boolean, zone: ZoneId): String {
    val parts = mutableListOf<String>()
    if (showPerson) parts += intake.person.name
    val ev = intake.event
    when (intake.status) {
        IntakeStatus.TAKEN -> {
            val at = ev?.at?.atZone(zone)?.toLocalTime()?.hhmm().orEmpty()
            val by = family.person(ev?.byPersonId)
            parts += if (by != null && by.id != intake.person.id) stringResource(R.string.meta_taken_by, at, by.name)
            else stringResource(R.string.meta_taken_at, at)
        }
        IntakeStatus.SNOOZED -> parts += stringResource(R.string.meta_moved_from, intake.scheduledAt.toLocalTime().hhmm())
        IntakeStatus.SKIPPED -> parts += stringResource(if (ev == null) R.string.meta_can_mark_later else R.string.meta_skipped_by_choice)
        IntakeStatus.CANCELLED -> parts += stringResource(R.string.meta_cancelled_today)
        else -> parts += intake.medication.dosage.text()
    }
    return parts.joinToString(" · ")
}

/** Индикатор синхронизации в шапке. Пока облако не подключено — «на этом телефоне». */
@Composable
fun SyncIndicator(state: SyncState, modifier: Modifier = Modifier) {
    val c = Ry.colors
    val (icon, text, tint) = when (state) {
        SyncState.SYNCED -> Triple(RyIcons.CloudCheck, R.string.sync_synced, c.primary)
        SyncState.SYNCING -> Triple(RyIcons.Sync, R.string.sync_syncing, c.textMuted)
        SyncState.OFFLINE -> Triple(RyIcons.CloudOff, R.string.sync_offline, c.warningText)
        SyncState.ERROR -> Triple(RyIcons.CloudOff, R.string.sync_error, c.errorText)
        SyncState.LOCAL_ONLY -> Triple(RyIcons.Lock, R.string.sync_local, c.textMuted)
    }
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        RyIcon(icon, null, tint = tint, size = 16.dp)
        Text(stringResource(text), style = Ry.type.caption.copy(fontSize = Ry.type.caption.fontSize * 0.92f), color = c.textMuted)
    }
}

/** Ошибка домена → понятный текст. */
@Composable
fun Throwable.userMessage(): String {
    val r = (this as? app.ryadom.core.model.RyadomException)?.reason
    return stringResource(
        when (r) {
            app.ryadom.core.model.RyadomException.Reason.INVALID_EMAIL -> R.string.err_invalid_email
            app.ryadom.core.model.RyadomException.Reason.WEAK_PASSWORD -> R.string.err_weak_password
            app.ryadom.core.model.RyadomException.Reason.EMPTY_NAME -> R.string.err_empty_name
            app.ryadom.core.model.RyadomException.Reason.ACCOUNT_NOT_FOUND -> R.string.err_account_not_found
            app.ryadom.core.model.RyadomException.Reason.EMAIL_TAKEN -> R.string.err_email_taken
            app.ryadom.core.model.RyadomException.Reason.INVITE_INVALID -> R.string.err_invite_invalid
            app.ryadom.core.model.RyadomException.Reason.NEEDS_CLOUD -> R.string.err_needs_cloud
            app.ryadom.core.model.RyadomException.Reason.NOT_ALLOWED -> R.string.err_not_allowed
            app.ryadom.core.model.RyadomException.Reason.LAST_OWNER -> R.string.err_last_owner
            app.ryadom.core.model.RyadomException.Reason.NOT_FOUND -> R.string.err_not_found
            null -> R.string.err_generic
        },
    )
}
