package app.ryadom.feature.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import app.ryadom.core.data.repository.DayPlan
import app.ryadom.core.data.repository.DaySummary
import app.ryadom.core.data.repository.FamilyRepository
import app.ryadom.core.data.repository.IntakeCommands
import app.ryadom.core.data.repository.MedicationRepository
import app.ryadom.core.data.repository.PlanRepository
import app.ryadom.core.data.util.Clock
import app.ryadom.core.designsystem.component.DayMark
import app.ryadom.core.designsystem.component.EmptyState
import app.ryadom.core.designsystem.component.GroupHeader
import app.ryadom.core.designsystem.component.RyChip
import app.ryadom.core.designsystem.component.RyDivider
import app.ryadom.core.designsystem.component.RyGroup
import app.ryadom.core.designsystem.component.RyIconButton
import app.ryadom.core.designsystem.component.RyListRow
import app.ryadom.core.designsystem.component.RyMeter
import app.ryadom.core.designsystem.component.RySegmented
import app.ryadom.core.designsystem.component.ScreenTitle
import app.ryadom.core.designsystem.component.SectionTitle
import app.ryadom.core.designsystem.component.SkeletonList
import app.ryadom.core.designsystem.theme.Illustration
import app.ryadom.core.designsystem.theme.Ry
import app.ryadom.core.designsystem.theme.RyIcons
import app.ryadom.core.l10n.R
import app.ryadom.core.model.FamilyGroup
import app.ryadom.core.model.Intake
import app.ryadom.core.model.IntakeStatus
import app.ryadom.core.model.Medication
import app.ryadom.core.model.Person
import app.ryadom.core.model.QuickMessage
import app.ryadom.core.model.ScheduleEngine
import app.ryadom.core.ui.IntakeActions
import app.ryadom.core.ui.IntakeActionsSheet
import app.ryadom.core.ui.IntakeRow
import app.ryadom.core.ui.PersonAvatar
import app.ryadom.core.ui.currentLocale
import app.ryadom.core.ui.dayMonth
import app.ryadom.core.ui.icon
import app.ryadom.core.ui.label
import app.ryadom.core.ui.relativeTitle
import app.ryadom.core.ui.short
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject

@Serializable data object ScheduleRoute

enum class ScheduleMode { DAY, WEEK, MONTH }

data class ScheduleState(
    val loading: Boolean = true,
    val mode: ScheduleMode = ScheduleMode.WEEK,
    val selected: LocalDate = LocalDate.now(),
    val today: LocalDate = LocalDate.now(),
    val personId: String? = null,
    val family: FamilyGroup? = null,
    val meId: String? = null,
    val day: DayPlan? = null,
    val marks: Map<LocalDate, DaySummary> = emptyMap(),
    val courses: List<Medication> = emptyList(),
)

private data class Query(val mode: ScheduleMode, val selected: LocalDate, val personId: String?)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ScheduleViewModel @Inject constructor(
    plan: PlanRepository,
    family: FamilyRepository,
    meds: MedicationRepository,
    private val commands: IntakeCommands,
    clock: Clock,
) : ViewModel(), IntakeActions {
    private val query = MutableStateFlow(Query(ScheduleMode.WEEK, clock.today(), null))

    val state: StateFlow<ScheduleState> = query.flatMapLatest { q ->
        val (from, to) = when (q.mode) {
            ScheduleMode.DAY -> q.selected to q.selected
            ScheduleMode.WEEK -> q.selected.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).let { it to it.plusDays(6) }
            ScheduleMode.MONTH -> YearMonth.from(q.selected).let { it.atDay(1) to it.atEndOfMonth() }
        }
        combine(
            plan.day(q.selected, q.personId), plan.range(from, to, q.personId), family.family, family.me, meds.medications(),
        ) { day, range, f, me, m ->
            ScheduleState(
                loading = false, mode = q.mode, selected = q.selected, today = clock.today(), personId = q.personId,
                family = f, meId = me?.id, day = day, marks = range.associateBy { it.date },
                courses = m.filter { it.course.end != null && it.course.finishedAt == null && (q.personId == null || it.personId == q.personId) },
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ScheduleState())

    fun setMode(m: ScheduleMode) { query.value = query.value.copy(mode = m) }
    fun select(d: LocalDate) { query.value = query.value.copy(selected = d) }
    fun shift(step: Long) {
        val q = query.value
        query.value = q.copy(selected = when (q.mode) {
            ScheduleMode.DAY -> q.selected.plusDays(step)
            ScheduleMode.WEEK -> q.selected.plusWeeks(step)
            ScheduleMode.MONTH -> q.selected.plusMonths(step)
        })
    }
    fun filter(personId: String?) { query.value = query.value.copy(personId = personId) }

    private fun launch(block: suspend () -> Unit) = viewModelScope.launch { runCatching { block() } }
    fun toggle(i: Intake) = launch { commands.toggle(i) }
    override fun taken(intake: Intake) { launch { commands.taken(intake) } }
    override fun undo(intake: Intake) { launch { commands.undo(intake) } }
    override fun snooze(intake: Intake, until: LocalDateTime) { launch { commands.snooze(intake, until) } }
    override fun skip(intake: Intake) { launch { commands.skip(intake) } }
    override fun cancel(intake: Intake) { launch { commands.cancel(intake) } }
    override fun message(intake: Intake, quick: QuickMessage, toId: String?) { launch { commands.message(intake, quick, toId) } }
    /** Переход к препарату делает экран: у ViewModel нет навигации. */
    override fun open(intake: Intake) = Unit
}

@Composable
fun ScheduleScreen(onOpenMedication: (String) -> Unit, onReminders: () -> Unit, vm: ScheduleViewModel = hiltViewModel()) {
    val actions = androidx.compose.runtime.remember(vm, onOpenMedication) {
        object : IntakeActions by vm { override fun open(intake: Intake) = onOpenMedication(intake.medication.id) }
    }
    val st by vm.state.collectAsStateWithLifecycle()
    var sheetFor by rememberSaveable { mutableStateOf<String?>(null) }
    val family = st.family
    Box(Modifier.fillMaxSize().background(Ry.colors.bg), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            Modifier.widthIn(max = 640.dp).fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ScreenTitle(stringResource(R.string.schedule_title), Modifier.weight(1f))
                    RyIconButton(RyIcons.Bell, stringResource(R.string.reminders_title), onReminders)
                }
            }
            item {
                RySegmented(
                    listOf(stringResource(R.string.schedule_day), stringResource(R.string.schedule_week), stringResource(R.string.schedule_month)),
                    st.mode.ordinal, { vm.setMode(ScheduleMode.entries[it]) },
                )
            }
            if (st.loading || family == null) { item { SkeletonList() }; return@LazyColumn }
            item { PeriodHeader(st, vm) }
            when (st.mode) {
                ScheduleMode.WEEK -> item { WeekStrip(st, vm::select) }
                ScheduleMode.MONTH -> item { MonthGrid(st, vm::select) }
                ScheduleMode.DAY -> Unit
            }
            if (family.members.size > 1) item { PersonFilter(family.members, st.personId, vm::filter) }
            val day = st.day
            if (st.mode != ScheduleMode.DAY) item { SectionTitle(st.selected.relativeTitle(st.today)) }
            if (day == null || day.intakes.isEmpty()) {
                item { EmptyState(Illustration.Calendar, stringResource(R.string.schedule_empty_title), stringResource(R.string.schedule_empty_body)) }
            } else {
                day.byPart.forEach { (part, list) ->
                    item(key = "${st.selected}-$part") {
                        RyGroup(header = { GroupHeader(part.icon, part.label()) }) {
                            list.forEachIndexed { i, intake ->
                                if (i > 0) RyDivider(start = 66.dp)
                                IntakeRow(intake, family, onCheck = { vm.toggle(intake) }, onOpen = { sheetFor = intake.id })
                            }
                        }
                    }
                }
            }
            if (st.courses.isNotEmpty()) {
                item { SectionTitle(stringResource(R.string.schedule_courses)) }
                item { Courses(st.courses, family, st.today, onOpenMedication) }
            }
        }
    }
    val intake = st.day?.intakes?.firstOrNull { it.id == sheetFor }
    if (intake != null && family != null) {
        IntakeActionsSheet(intake, family, st.meId, st.day!!.now, actions, onDismiss = { sheetFor = null })
    }
}

@Composable
private fun PeriodHeader(st: ScheduleState, vm: ScheduleViewModel) {
    val locale = currentLocale()
    val title = when (st.mode) {
        ScheduleMode.DAY -> st.selected.relativeTitle(st.today)
        ScheduleMode.WEEK -> {
            val mon = st.selected.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            "${mon.dayMonth()} — ${mon.plusDays(6).dayMonth()}"
        }
        ScheduleMode.MONTH -> YearMonth.from(st.selected).let {
            "${it.month.getDisplayName(TextStyle.FULL_STANDALONE, locale).replaceFirstChar { c -> c.titlecase(locale) }} ${it.year}"
        }
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        RyIconButton(RyIcons.ChevronLeft, stringResource(R.string.a11y_previous), { vm.shift(-1) })
        Text(title, style = Ry.type.subtitle, color = Ry.colors.text, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        RyIconButton(RyIcons.ChevronRight, stringResource(R.string.a11y_next), { vm.shift(1) })
    }
    if (st.selected != st.today) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            RyChip(stringResource(R.string.date_today), false, { vm.select(st.today) })
        }
    }
}

/** До трёх меток на день: принято, пропуск, впереди. Пустые дни без меток. */
private fun marksFor(s: DaySummary?, date: LocalDate, today: LocalDate): List<Int> {
    if (s == null || s.total == 0) return emptyList()
    val out = mutableListOf<Int>()
    if (s.taken > 0) out += 0
    if (s.skipped > 0) out += 2
    if (!date.isBefore(today) && s.taken + s.skipped < s.total) out += 1
    return out
}

@Composable
private fun WeekStrip(st: ScheduleState, onSelect: (LocalDate) -> Unit) {
    val mon = st.selected.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        (0L..6L).map { mon.plusDays(it) }.forEach { d ->
            DayCell(d, st, Modifier.weight(1f), onSelect, showWeekday = true)
        }
    }
}

@Composable
private fun DayCell(d: LocalDate, st: ScheduleState, modifier: Modifier, onSelect: (LocalDate) -> Unit, showWeekday: Boolean, faded: Boolean = false) {
    val c = Ry.colors
    val on = d == st.selected
    val isToday = d == st.today
    val summary = st.marks[d]
    val desc = summary?.let { stringResource(R.string.a11y_day_summary, it.taken, it.total) } ?: ""
    Column(
        modifier.clip(Ry.shapes.m)
            .background(if (on) c.primaryDark else androidx.compose.ui.graphics.Color.Transparent)
            .clickable(role = Role.Button) { onSelect(d) }
            .semantics { contentDescription = "${d.dayOfMonth} ${desc}" }
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (showWeekday) Text(d.dayOfWeek.short(), style = Ry.type.caption, color = if (on) c.onPrimary else c.textMuted)
        Text(
            d.dayOfMonth.toString(), style = Ry.type.subtitle,
            color = when { on -> c.onPrimary; faded -> c.textSecondary; isToday -> c.primaryDark; else -> c.text },
        )
        Row(Modifier.height(6.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            marksFor(summary, d, st.today).forEach { DayMark(it, onDark = on) }
        }
    }
}

@Composable
private fun MonthGrid(st: ScheduleState, onSelect: (LocalDate) -> Unit) {
    val ym = YearMonth.from(st.selected)
    val first = ym.atDay(1).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(Modifier.fillMaxWidth()) {
            DayOfWeek.entries.forEach { Text(it.short(), style = Ry.type.caption, color = Ry.colors.textMuted, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center) }
        }
        var d = first
        while (!d.isAfter(ym.atEndOfMonth())) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                repeat(7) {
                    val day = d
                    DayCell(day, st, Modifier.weight(1f), onSelect, showWeekday = false, faded = day.month != ym.month)
                    d = d.plusDays(1)
                }
            }
        }
    }
}

@Composable
private fun PersonFilter(people: List<Person>, selected: String?, onSelect: (String?) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        item { RyChip(stringResource(R.string.filter_all), selected == null, { onSelect(null) }) }
        items(people, key = { it.id }) { p ->
            RyChip(p.name, selected == p.id, { onSelect(p.id) }, leading = { PersonAvatar(p, size = 24.dp) })
        }
    }
}

@Composable
private fun Courses(courses: List<Medication>, family: FamilyGroup, today: LocalDate, onOpen: (String) -> Unit) {
    RyGroup {
        courses.forEachIndexed { i, m ->
            if (i > 0) RyDivider(start = 16.dp)
            val p = ScheduleEngine.courseProgress(m, today)
            Column(Modifier.fillMaxWidth().clickable { onOpen(m.id) }.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    family.person(m.personId)?.let { PersonAvatar(it, size = 22.dp) }
                    Text(m.name, style = Ry.type.subtitle, color = Ry.colors.text, modifier = Modifier.weight(1f))
                    if (p != null) Text(stringResource(R.string.count_slash, p.first, p.second), style = Ry.type.caption, color = Ry.colors.textMuted)
                }
                if (p != null) RyMeter(p.first.toFloat() / p.second, false)
                Text(stringResource(R.string.course_until, m.course.end!!.dayMonth()), style = Ry.type.caption, color = Ry.colors.textMuted)
            }
        }
    }
}

