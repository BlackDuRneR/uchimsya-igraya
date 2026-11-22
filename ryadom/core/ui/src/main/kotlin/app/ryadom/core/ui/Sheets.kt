package app.ryadom.core.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.rememberDatePickerState
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TimeInput
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.ryadom.core.designsystem.component.ButtonStyle
import app.ryadom.core.designsystem.component.RyButton
import app.ryadom.core.designsystem.component.RyChip
import app.ryadom.core.designsystem.component.RyDivider
import app.ryadom.core.designsystem.component.RyListRow
import app.ryadom.core.designsystem.component.RySheet
import app.ryadom.core.designsystem.theme.Ry
import app.ryadom.core.designsystem.theme.RyIcons
import app.ryadom.core.l10n.R
import app.ryadom.core.model.FamilyGroup
import app.ryadom.core.model.Intake
import app.ryadom.core.model.IntakeStatus
import app.ryadom.core.model.QuickMessage
import java.time.LocalDateTime
import java.time.LocalTime

/** Что можно сделать с приёмом. Один набор действий для главной, расписания и уведомления. */
interface IntakeActions {
    fun taken(intake: Intake)
    fun undo(intake: Intake)
    fun snooze(intake: Intake, until: LocalDateTime)
    fun skip(intake: Intake)
    fun cancel(intake: Intake)
    fun message(intake: Intake, quick: QuickMessage, toId: String?)
    fun open(intake: Intake)
}

/**
 * Лист действий по приёму: отметить, позже (5/15/30/своё время), пропустить,
 * отменить на сегодня, быстрое сообщение второму взрослому, открыть препарат.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun IntakeActionsSheet(
    intake: Intake,
    family: FamilyGroup,
    meId: String?,
    now: LocalDateTime,
    actions: IntakeActions,
    onDismiss: () -> Unit,
) {
    var pickTime by remember { mutableStateOf(false) }
    val partner = family.adults.firstOrNull { it.id != meId && it.userId != null }
    RySheet(onDismiss, title = intake.medication.name) {
        Text(
            "${intake.person.name} · ${intake.medication.dosage.text()} · ${intake.scheduledAt.toLocalTime().hhmm()}",
            style = Ry.type.bodySmall, color = Ry.colors.textMuted,
        )
        val open = intake.status != IntakeStatus.TAKEN
        if (open) {
            RyButton(stringResource(R.string.intake_mark), { actions.taken(intake); onDismiss() }, Modifier.fillMaxWidth(), icon = RyIcons.Check)
            Text(stringResource(R.string.intake_later), style = Ry.type.overline, color = Ry.colors.textMuted, modifier = Modifier.padding(top = 4.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(5L, 15L, 30L).forEach { m ->
                    RyChip(stringResource(R.string.snooze_minutes, m), false, {
                        val base = if (now.isAfter(intake.scheduledAt)) now else intake.scheduledAt
                        actions.snooze(intake, base.plusMinutes(m).withSecond(0).withNano(0)); onDismiss()
                    })
                }
                RyChip(stringResource(R.string.snooze_pick_time), false, { pickTime = true }, icon = RyIcons.Clock)
            }
        } else {
            RyButton(stringResource(R.string.intake_undo), { actions.undo(intake); onDismiss() }, Modifier.fillMaxWidth(), style = ButtonStyle.Ghost)
        }
        Column(Modifier.fillMaxWidth()) {
            if (partner != null) {
                RyDivider()
                Text(stringResource(R.string.intake_message_to, partner.name), style = Ry.type.overline, color = Ry.colors.textMuted,
                    modifier = Modifier.padding(top = 12.dp, bottom = 8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val options = if (open) listOf(QuickMessage.PLEASE_REMIND, QuickMessage.MY_TURN_TODAY, QuickMessage.BUY_PACKAGE)
                    else listOf(QuickMessage.GAVE_IT, QuickMessage.ALREADY_TAKEN, QuickMessage.THANKS)
                    options.forEach { q -> RyChip(q.text(), false, { actions.message(intake, q, partner.id); onDismiss() }) }
                }
            }
            RyDivider(Modifier.padding(top = 12.dp))
            if (open && intake.status != IntakeStatus.SKIPPED) {
                RyListRow(stringResource(R.string.intake_skip), icon = RyIcons.Skip, trailing = null, onClick = { actions.skip(intake); onDismiss() })
            }
            if (open && intake.status != IntakeStatus.CANCELLED) {
                RyListRow(stringResource(R.string.intake_cancel_today), icon = RyIcons.Close, trailing = null, onClick = { actions.cancel(intake); onDismiss() })
            }
            if (intake.event != null && open) {
                RyListRow(stringResource(R.string.intake_reset), icon = RyIcons.Repeat, trailing = null, onClick = { actions.undo(intake); onDismiss() })
            }
            RyListRow(stringResource(R.string.intake_open_medication), icon = RyIcons.Capsule, onClick = { onDismiss(); actions.open(intake) })
        }
    }
    if (pickTime) {
        RyTimePickerDialog(
            initial = intake.scheduledAt.toLocalTime().plusMinutes(60),
            onDismiss = { pickTime = false },
            onPick = { t ->
                pickTime = false
                actions.snooze(intake, intake.scheduledAt.toLocalDate().atTime(t)); onDismiss()
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RyTimePickerDialog(initial: LocalTime, onDismiss: () -> Unit, onPick: (LocalTime) -> Unit) {
    val state = rememberTimePickerState(initial.hour, initial.minute, is24Hour = true)
    val c = Ry.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = c.surface,
        shape = Ry.shapes.l,
        title = { Text(stringResource(R.string.time_pick_title), style = Ry.type.headline, color = c.text) },
        text = {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                TimeInput(
                    state,
                    colors = TimePickerDefaults.colors(
                        timeSelectorSelectedContainerColor = c.primaryContainer,
                        timeSelectorSelectedContentColor = c.primaryDark,
                        timeSelectorUnselectedContainerColor = c.surfaceMuted,
                        timeSelectorUnselectedContentColor = c.text,
                    ),
                )
            }
        },
        confirmButton = { RyButton(stringResource(R.string.common_done), { onPick(LocalTime.of(state.hour, state.minute)) }, small = true) },
        dismissButton = { RyButton(stringResource(R.string.common_cancel), onDismiss, style = ButtonStyle.Text, small = true) },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RyDatePicker(initial: LocalDate, onDismiss: () -> Unit, onPick: (LocalDate) -> Unit) {
    val state = rememberDatePickerState(initialSelectedDateMillis = initial.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli())
    val c = Ry.colors
    DatePickerDialog(
        onDismissRequest = onDismiss,
        colors = DatePickerDefaults.colors(containerColor = c.surface),
        confirmButton = {
            RyButton(stringResource(R.string.common_done), {
                state.selectedDateMillis?.let { onPick(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) } ?: onDismiss()
            }, style = ButtonStyle.Text, small = true)
        },
        dismissButton = { RyButton(stringResource(R.string.common_cancel), onDismiss, style = ButtonStyle.Text, small = true) },
    ) {
        DatePicker(state, colors = DatePickerDefaults.colors(
            containerColor = c.surface, selectedDayContainerColor = c.primaryDark, selectedDayContentColor = c.onPrimary,
            todayDateBorderColor = c.primary, todayContentColor = c.primaryDark,
        ))
    }
}

