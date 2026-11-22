package app.ryadom.feature.medication

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import app.ryadom.core.data.repository.FamilyRepository
import app.ryadom.core.data.repository.MedicationRepository
import app.ryadom.core.data.repository.PlanRepository
import app.ryadom.core.data.util.Clock
import app.ryadom.core.designsystem.component.BadgeKind
import app.ryadom.core.designsystem.component.ButtonStyle
import app.ryadom.core.designsystem.component.EmptyState
import app.ryadom.core.designsystem.component.RyBadge
import app.ryadom.core.designsystem.component.RyButton
import app.ryadom.core.designsystem.component.RyConfirmDialog
import app.ryadom.core.designsystem.component.RyDivider
import app.ryadom.core.designsystem.component.RyGroup
import app.ryadom.core.designsystem.component.RyIcon
import app.ryadom.core.designsystem.component.RyIconButton
import app.ryadom.core.designsystem.component.RyListRow
import app.ryadom.core.designsystem.component.RyMeter
import app.ryadom.core.designsystem.component.RySheet
import app.ryadom.core.designsystem.component.RyTextField
import app.ryadom.core.designsystem.component.RyTopBar
import app.ryadom.core.designsystem.component.SectionTitle
import app.ryadom.core.designsystem.component.SkeletonList
import app.ryadom.core.designsystem.theme.Illustration
import app.ryadom.core.designsystem.theme.Ry
import app.ryadom.core.designsystem.theme.RyIcons
import app.ryadom.core.l10n.R
import app.ryadom.core.model.FamilyGroup
import app.ryadom.core.model.IntakeStatus
import app.ryadom.core.model.Medication
import app.ryadom.core.model.RestockEntry
import app.ryadom.core.model.ScheduleEngine
import app.ryadom.core.ui.PersonAvatar
import app.ryadom.core.ui.clean
import app.ryadom.core.ui.dayMonth
import app.ryadom.core.ui.dayMonthShort
import app.ryadom.core.ui.hhmm
import app.ryadom.core.ui.icon
import app.ryadom.core.ui.label
import app.ryadom.core.ui.text
import app.ryadom.core.ui.unitCount
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import java.time.LocalDate
import javax.inject.Inject

@Serializable data class MedicationDetailRoute(val id: String)

data class RecentIntake(val date: LocalDate, val time: java.time.LocalTime, val status: IntakeStatus, val by: String?)

data class DetailState(
    val loading: Boolean = true,
    val med: Medication? = null,
    val family: FamilyGroup? = null,
    val today: LocalDate = LocalDate.now(),
    val recent: List<RecentIntake> = emptyList(),
    val restocks: List<RestockEntry> = emptyList(),
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MedicationDetailViewModel @Inject constructor(
    handle: SavedStateHandle,
    private val meds: MedicationRepository,
    family: FamilyRepository,
    plan: PlanRepository,
    private val clock: Clock,
) : ViewModel() {
    val id = handle.toRoute<MedicationDetailRoute>().id

    val state: StateFlow<DetailState> = combine(
        meds.medication(id), family.family, meds.restocks(id),
        plan.today().flatMapLatest { t -> plan.range(t.minusDays(6), t) },
    ) { m, f, r, days ->
        val today = clock.today()
        val recent = days.flatMap { d -> d.intakes.filter { it.medication.id == id } }
            .filter { it.status != IntakeStatus.UPCOMING }
            .sortedByDescending { it.scheduledAt }
            .map { RecentIntake(it.scheduledAt.toLocalDate(), it.scheduledAt.toLocalTime(), it.status, f?.person(it.event?.byPersonId)?.name) }
        DetailState(false, m, f, today, recent, r)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DetailState())

    fun restock(amount: Double) = viewModelScope.launch { meds.restock(id, amount) }
    fun setRemaining(v: Double) = viewModelScope.launch { meds.setRemaining(id, v) }
    fun archive(a: Boolean) = viewModelScope.launch { meds.archive(id, a) }
    fun delete() = viewModelScope.launch { meds.delete(id) }
}

@Composable
fun MedicationDetailScreen(
    onBack: () -> Unit,
    onEdit: (String) -> Unit,
    onFinishCourse: (String) -> Unit,
    vm: MedicationDetailViewModel = hiltViewModel(),
) {
    val st by vm.state.collectAsStateWithLifecycle()
    var deleteFlow by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var fixStock by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().background(Ry.colors.bg)) {
        RyTopBar("", navLabel = stringResource(R.string.common_back), onNav = onBack, actions = {
            if (st.med != null) RyIconButton(RyIcons.Edit, stringResource(R.string.common_edit), { onEdit(vm.id) })
        })
        val m = st.med
        val family = st.family
        when {
            st.loading -> Box(Modifier.padding(16.dp)) { SkeletonList() }
            m == null || family == null -> EmptyState(
                Illustration.Package, stringResource(R.string.med_not_found_title), stringResource(R.string.med_not_found_body),
                primary = stringResource(R.string.common_back) to onBack,
            )
            else -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                Column(
                    Modifier.widthIn(max = 640.dp).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    DetailHeader(m, family)
                    Info(m, st.today)
                    if (ScheduleEngine.courseEndsOn(m, st.today) || (ScheduleEngine.isCourseFinished(m, st.today) && m.course.finishedAt == null)) {
                        RyButton(stringResource(R.string.course_finish_cta), { onFinishCourse(m.id) }, Modifier.fillMaxWidth(), style = ButtonStyle.Tonal, icon = RyIcons.Flag)
                    }
                    Stock(m, st.restocks, onRestock = { vm.restock(it) }, onFix = { fixStock = true }, family = family)
                    Recent(st.recent)
                    if (m.notes.isNotBlank()) {
                        SectionTitle(stringResource(R.string.med_field_notes))
                        Text(m.notes, style = Ry.type.body, color = Ry.colors.text)
                    }
                    RyGroup {
                        RyListRow(
                            stringResource(if (m.archived) R.string.med_restore else R.string.med_archive),
                            subtitle = stringResource(if (m.archived) R.string.med_restore_hint else R.string.med_archive_hint),
                            icon = RyIcons.Archive, trailing = null, onClick = { vm.archive(!m.archived) },
                        )
                        RyDivider(start = 52.dp)
                        RyListRow(stringResource(R.string.med_delete), icon = RyIcons.Trash, titleColor = Ry.colors.errorText,
                            trailing = null, onClick = { deleteFlow = true })
                    }
                    Text(stringResource(R.string.med_dose_disclaimer), style = Ry.type.caption, color = Ry.colors.textMuted,
                        modifier = Modifier.padding(bottom = 32.dp))
                }
            }
        }
    }
    val m = st.med
    if (deleteFlow && m != null) {
        RySheet(onDismiss = { deleteFlow = false }, title = stringResource(R.string.med_delete_title, m.name)) {
            Text(stringResource(R.string.med_delete_body), style = Ry.type.body, color = Ry.colors.textMuted)
            if (!m.archived) RyButton(stringResource(R.string.med_archive_instead), { vm.archive(true); deleteFlow = false }, Modifier.fillMaxWidth(), style = ButtonStyle.Tonal, icon = RyIcons.Archive)
            RyButton(stringResource(R.string.med_delete), { deleteFlow = false; confirmDelete = true }, Modifier.fillMaxWidth(), style = ButtonStyle.Danger, icon = RyIcons.Trash)
            RyButton(stringResource(R.string.common_cancel), { deleteFlow = false }, Modifier.fillMaxWidth(), style = ButtonStyle.Text)
        }
    }
    if (confirmDelete && m != null) {
        RyConfirmDialog(
            stringResource(R.string.med_delete_confirm_title), stringResource(R.string.med_delete_confirm_body, m.name),
            stringResource(R.string.med_delete), stringResource(R.string.common_cancel),
            onConfirm = { confirmDelete = false; vm.delete(); onBack() }, onDismiss = { confirmDelete = false }, destructive = true,
        )
    }
    if (fixStock && m != null) {
        var v by remember { mutableStateOf(m.inventory?.remaining?.clean() ?: "") }
        RySheet(onDismiss = { fixStock = false }, title = stringResource(R.string.stock_fix_title)) {
            RyTextField(v, { s -> v = s.filter { c -> c.isDigit() || c == '.' || c == ',' }.take(7) }, stringResource(R.string.med_stock_now),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
            RyButton(stringResource(R.string.common_save), {
                v.replace(',', '.').toDoubleOrNull()?.let { vm.setRemaining(it) }; fixStock = false
            }, Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun DetailHeader(m: Medication, family: FamilyGroup) {
    val person = family.person(m.personId)
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
        if (m.photoUri != null) UriImage(m.photoUri!!, Modifier.size(64.dp).clip(Ry.shapes.m))
        else Box(Modifier.size(64.dp).clip(Ry.shapes.m).background(Ry.colors.warmContainer), contentAlignment = Alignment.Center) {
            RyIcon(m.dosage.unit.icon, null, size = 28.dp)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(m.name, style = Ry.type.display, color = Ry.colors.text)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (person != null) { PersonAvatar(person, size = 22.dp); Text(person.name, style = Ry.type.bodySmall, color = Ry.colors.textMuted) }
                Text("· ${m.category.label()}", style = Ry.type.bodySmall, color = Ry.colors.textMuted)
            }
            if (m.archived) RyBadge(stringResource(R.string.med_archived), BadgeKind.Neutral, icon = RyIcons.Archive)
        }
    }
}

@Composable
private fun Info(m: Medication, today: LocalDate) {
    RyGroup {
        RyListRow(stringResource(R.string.med_field_dosage), subtitle = m.dosage.text(), icon = m.dosage.unit.icon, trailing = null)
        RyDivider(start = 52.dp)
        RyListRow(
            stringResource(R.string.med_field_schedule),
            subtitle = "${m.schedule.days.text()} · ${m.schedule.times.joinToString(", ") { it.hhmm() }}",
            icon = RyIcons.Clock, trailing = null,
        )
        RyDivider(start = 52.dp)
        val progress = ScheduleEngine.courseProgress(m, today)
        val courseText = when {
            m.course.finishedAt != null -> stringResource(R.string.course_finished_on, m.course.finishedAt!!.dayMonth())
            progress != null -> stringResource(R.string.course_progress, progress.first, progress.second, m.course.end!!.dayMonth())
            else -> stringResource(R.string.course_open_since, m.course.start.dayMonth())
        }
        RyListRow(stringResource(R.string.med_field_course), subtitle = courseText, icon = RyIcons.Flag, trailing = null)
        if (progress != null && m.course.finishedAt == null) {
            Box(Modifier.padding(start = 52.dp, end = 16.dp, bottom = 14.dp)) { RyMeter(progress.first.toFloat() / progress.second, false) }
        }
        RyDivider(start = 52.dp)
        val r = m.reminder
        RyListRow(
            stringResource(R.string.med_field_reminders),
            subtitle = if (!r.enabled) stringResource(R.string.med_reminders_off) else buildList {
                add(if (r.advanceMinutes == 0) stringResource(R.string.med_remind_on_time) else stringResource(R.string.med_remind_before, r.advanceMinutes))
                r.repeatAfterMinutes?.let { add(stringResource(R.string.med_remind_repeat_after, it)) }
                r.familyNudgeAfterMinutes?.let { add(stringResource(R.string.med_remind_family_after, it)) }
            }.joinToString(" · "),
            icon = if (r.enabled) RyIcons.Bell else RyIcons.BellOff, trailing = null,
        )
    }
}

@Composable
private fun Stock(m: Medication, restocks: List<RestockEntry>, onRestock: (Double) -> Unit, onFix: () -> Unit, family: FamilyGroup) {
    val inv = m.inventory ?: return
    SectionTitle(stringResource(R.string.med_field_stock))
    RyGroup {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            val days = ScheduleEngine.daysLeft(m)
            val low = ScheduleEngine.isLowStock(m)
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(unitCount(m.dosage.unit, inv.remaining, m.dosage.customUnit), style = Ry.type.headline, color = Ry.colors.text)
            }
            val full = inv.packageSize ?: inv.remaining.coerceAtLeast(1.0)
            RyMeter((inv.remaining / full).toFloat(), low)
            if (days != null) Text(
                if (low) stringResource(R.string.stock_low_days, days) else stringResource(R.string.stock_days, days),
                style = Ry.type.bodySmall, color = if (low) Ry.colors.warningText else Ry.colors.textMuted,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (inv.packageSize != null) RyButton(stringResource(R.string.stock_bought), { onRestock(inv.packageSize!!) }, small = true, style = ButtonStyle.Tonal, icon = RyIcons.Bag)
                RyButton(stringResource(R.string.stock_fix), onFix, small = true, style = ButtonStyle.Ghost, icon = RyIcons.Edit)
            }
        }
        restocks.take(5).forEach { r ->
            RyDivider(start = 16.dp)
            RyListRow(
                stringResource(R.string.stock_restocked, unitCount(m.dosage.unit, r.amount, m.dosage.customUnit)),
                subtitle = "${r.at.atZone(java.time.ZoneId.systemDefault()).toLocalDate().dayMonth()} · ${family.person(r.byPersonId)?.name.orEmpty()}",
                trailing = null,
            )
        }
    }
}

@Composable
private fun Recent(recent: List<RecentIntake>) {
    SectionTitle(stringResource(R.string.med_recent))
    if (recent.isEmpty()) {
        Text(stringResource(R.string.med_recent_empty), style = Ry.type.bodySmall, color = Ry.colors.textMuted)
        return
    }
    RyGroup {
        recent.take(10).forEachIndexed { i, r ->
            if (i > 0) RyDivider(start = 16.dp)
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("${r.date.dayMonthShort()}, ${r.time.hhmm()}", style = Ry.type.number, color = Ry.colors.text, modifier = Modifier.weight(1f))
                val (text, kind) = when (r.status) {
                    IntakeStatus.TAKEN -> (r.by?.let { stringResource(R.string.recent_taken_by, it) } ?: stringResource(R.string.status_taken)) to BadgeKind.Ok
                    IntakeStatus.SKIPPED -> stringResource(R.string.status_skipped) to BadgeKind.Neutral
                    IntakeStatus.CANCELLED -> stringResource(R.string.status_cancelled) to BadgeKind.Neutral
                    IntakeStatus.SNOOZED -> stringResource(R.string.status_snoozed) to BadgeKind.Warn
                    else -> stringResource(R.string.status_due) to BadgeKind.Ok
                }
                RyBadge(text, kind)
            }
        }
    }
}
