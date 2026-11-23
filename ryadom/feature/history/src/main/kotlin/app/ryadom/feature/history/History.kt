package app.ryadom.feature.history

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import app.ryadom.core.data.repository.DaySummary
import app.ryadom.core.data.repository.FamilyRepository
import app.ryadom.core.data.repository.MedicationRepository
import app.ryadom.core.data.repository.PlanRepository
import app.ryadom.core.data.util.Clock
import app.ryadom.core.designsystem.component.BadgeKind
import app.ryadom.core.designsystem.component.ButtonStyle
import app.ryadom.core.designsystem.component.EmptyState
import app.ryadom.core.designsystem.component.RyBadge
import app.ryadom.core.designsystem.component.RyButton
import app.ryadom.core.designsystem.component.RyChip
import app.ryadom.core.designsystem.component.RyDivider
import app.ryadom.core.designsystem.component.RyGroup
import app.ryadom.core.designsystem.component.RyListRow
import app.ryadom.core.designsystem.component.RyMeter
import app.ryadom.core.designsystem.component.RySegmented
import app.ryadom.core.designsystem.component.RyTopBar
import app.ryadom.core.designsystem.component.ScreenTitle
import app.ryadom.core.designsystem.component.SectionTitle
import app.ryadom.core.designsystem.component.SkeletonList
import app.ryadom.core.designsystem.component.StatTile
import app.ryadom.core.designsystem.theme.Illustration
import app.ryadom.core.designsystem.theme.Ry
import app.ryadom.core.designsystem.theme.RyIcons
import app.ryadom.core.l10n.R
import app.ryadom.core.model.FamilyGroup
import app.ryadom.core.model.IntakeAction
import app.ryadom.core.model.IntakeStatus
import app.ryadom.core.model.Medication
import app.ryadom.core.ui.PersonAvatar
import app.ryadom.core.ui.PersonDot
import app.ryadom.core.ui.RyDatePicker
import app.ryadom.core.ui.dayMonth
import app.ryadom.core.ui.dayMonthShort
import app.ryadom.core.ui.hhmm
import app.ryadom.core.ui.label
import app.ryadom.core.ui.longDate
import app.ryadom.core.ui.relativeTitle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import kotlin.math.roundToInt

@Serializable data object HistoryRoute
@Serializable data object StatsRoute
@Serializable data class HistoryDayRoute(val date: String)

/** Период: неделя, 2 недели, месяц или выбранный вручную. */
data class Period(val days: Long = 14, val from: LocalDate? = null, val to: LocalDate? = null)

data class HistoryState(
    val loading: Boolean = true,
    val today: LocalDate = LocalDate.now(),
    val period: Period = Period(),
    val personId: String? = null,
    val family: FamilyGroup? = null,
    val days: List<DaySummary> = emptyList(),
    val finishedCourses: List<Medication> = emptyList(),
) {
    /** Только прошедшие и сегодняшние приёмы, будущие не считаем. */
    val past: List<DaySummary> get() = days.filter { !it.date.isAfter(today) }
    val total: Int get() = past.sumOf { it.total - it.intakes.count { i -> i.isOpen } }
    val taken: Int get() = past.sumOf { it.taken }
    val skipped: Int get() = past.sumOf { it.skipped }
    val snoozed: Int get() = past.sumOf { it.snoozed }
    val regularity: Int? get() = if (total == 0) null else (taken * 100.0 / total).roundToInt()
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HistoryViewModel @Inject constructor(
    plan: PlanRepository,
    family: FamilyRepository,
    meds: MedicationRepository,
    clock: Clock,
) : ViewModel() {
    private val query = MutableStateFlow(Period() to (null as String?))

    val state: StateFlow<HistoryState> = combine(query, plan.today(), ::Pair).flatMapLatest { (q, today) ->
        val (period, person) = q
        val to = period.to ?: today
        val from = period.from ?: to.minusDays(period.days - 1)
        combine(plan.range(from, to, person), family.family, meds.medications(includeArchived = true)) { days, f, m ->
            HistoryState(false, today, period, person, f, days.reversed(),
                m.filter { it.course.finishedAt != null && (person == null || it.personId == person) })
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryState(today = clock.today()))

    fun setPeriod(p: Period) { query.value = p to query.value.second }
    fun setPerson(id: String?) { query.value = query.value.first to id }
}

// ---------- История ----------

@Composable
fun HistoryScreen(onStats: () -> Unit, onDay: (LocalDate) -> Unit, onMedication: (String) -> Unit, vm: HistoryViewModel = hiltViewModel()) {
    val st by vm.state.collectAsStateWithLifecycle()
    val f = st.family
    Box(Modifier.fillMaxSize().background(Ry.colors.bg), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            Modifier.widthIn(max = 640.dp).fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item { ScreenTitle(stringResource(R.string.history_title)) }
            item { PeriodSwitch(st.period, vm::setPeriod, allowCustom = false) }
            if (st.loading || f == null) { item { SkeletonList() }; return@LazyColumn }
            if (f.members.size > 1) item { PersonFilter(f, st.personId, vm::setPerson) }
            if (st.total == 0) {
                item { EmptyState(Illustration.History, stringResource(R.string.history_empty_title), stringResource(R.string.history_empty_body)) }
                return@LazyColumn
            }
            item { SummaryCard(st, onStats) }
            item {
                RyGroup {
                    st.past.forEachIndexed { i, d ->
                        if (i > 0) RyDivider(start = 16.dp)
                        RyListRow(
                            d.date.relativeTitle(st.today),
                            subtitle = daySubtitle(d),
                            onClick = { onDay(d.date) },
                            trailing = {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    if (d.total > 0) Text(stringResource(R.string.count_slash, d.taken, d.total), style = Ry.type.number, color = Ry.colors.textMuted)
                                    app.ryadom.core.designsystem.component.RyIcon(RyIcons.ChevronRight, null, tint = Ry.colors.textSecondary, size = 20.dp)
                                }
                            },
                        )
                    }
                }
            }
            if (st.finishedCourses.isNotEmpty()) {
                item { SectionTitle(stringResource(R.string.history_finished_courses)) }
                item {
                    RyGroup {
                        st.finishedCourses.forEachIndexed { i, m ->
                            if (i > 0) RyDivider(start = 16.dp)
                            RyListRow(m.name, subtitle = stringResource(R.string.course_finished_on, m.course.finishedAt!!.dayMonth()),
                                leading = { f.person(m.personId)?.let { PersonAvatar(it, size = 28.dp) } }, onClick = { onMedication(m.id) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun daySubtitle(d: DaySummary): String {
    if (d.total == 0) return stringResource(R.string.history_no_intakes)
    val parts = mutableListOf<String>()
    if (d.markedBy.isNotEmpty()) parts += stringResource(R.string.history_marked_by, d.markedBy.joinToString(", ") { it.name })
    if (d.skipped > 0) parts += stringResource(R.string.history_skipped_n, d.skipped)
    if (d.snoozed > 0) parts += stringResource(R.string.history_snoozed_n, d.snoozed)
    return parts.joinToString(" · ").ifEmpty { stringResource(R.string.history_in_progress) }
}

@Composable
private fun PeriodSwitch(p: Period, onChange: (Period) -> Unit, allowCustom: Boolean) {
    var pickFrom by remember { mutableStateOf(false) }
    var pickTo by remember { mutableStateOf(false) }
    val options = buildList {
        add(stringResource(R.string.period_week)); add(stringResource(R.string.period_two_weeks)); add(stringResource(R.string.period_month))
        if (allowCustom) add(stringResource(R.string.period_custom))
    }
    val sel = when {
        p.from != null -> 3
        p.days == 7L -> 0
        p.days == 14L -> 1
        else -> 2
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        RySegmented(options, sel, { i ->
            when (i) {
                0 -> onChange(Period(7)); 1 -> onChange(Period(14)); 2 -> onChange(Period(30))
                else -> pickFrom = true
            }
        })
        if (p.from != null && p.to != null) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RyChip(stringResource(R.string.period_from, p.from.dayMonth()), true, { pickFrom = true })
                RyChip(stringResource(R.string.period_to, p.to.dayMonth()), true, { pickTo = true })
            }
        }
    }
    if (pickFrom) RyDatePicker(p.from ?: LocalDate.now().minusDays(13), { pickFrom = false }) { d ->
        pickFrom = false; onChange(Period(0, d, maxOf(p.to ?: LocalDate.now(), d)))
    }
    if (pickTo) RyDatePicker(p.to ?: LocalDate.now(), { pickTo = false }) { d ->
        pickTo = false; onChange(Period(0, minOf(p.from ?: d, d), d))
    }
}

@Composable
private fun PersonFilter(f: FamilyGroup, selected: String?, onSelect: (String?) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        item { RyChip(stringResource(R.string.filter_all), selected == null, { onSelect(null) }) }
        items(f.members, key = { it.id }) { p -> RyChip(p.name, selected == p.id, { onSelect(p.id) }, leading = { PersonAvatar(p, size = 24.dp) }) }
    }
}

@Composable
private fun SummaryCard(st: HistoryState, onStats: (() -> Unit)?) {
    RyGroup {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("${st.regularity ?: 0}%", style = Ry.type.display.copy(fontSize = Ry.type.display.fontSize * 1.4f), color = Ry.colors.text)
                Text(stringResource(R.string.history_regularity_caption), style = Ry.type.caption, color = Ry.colors.textMuted, modifier = Modifier.padding(bottom = 6.dp))
            }
            Bars(st.past.reversed())
            Row {
                st.past.lastOrNull()?.let { Text(it.date.dayMonthShort(), style = Ry.type.caption, color = Ry.colors.textMuted, modifier = Modifier.weight(1f)) }
                st.past.firstOrNull()?.let { Text(it.date.dayMonthShort(), style = Ry.type.caption, color = Ry.colors.textMuted) }
            }
            if (onStats != null) RyButton(stringResource(R.string.stats_title), onStats, style = ButtonStyle.Ghost, small = true, icon = RyIcons.Chart)
        }
    }
}

/** Столбики по дням: доля отмеченных. Полный день — шалфей, неполный — светлее. Подписи — первая и последняя даты. */
@Composable
private fun Bars(days: List<DaySummary>) {
    val c = Ry.colors
    val desc = stringResource(R.string.a11y_bars, days.count { it.total > 0 && it.taken == it.total }, days.count { it.total > 0 })
    Canvas(Modifier.fillMaxWidth().height(96.dp).semantics { contentDescription = desc }) {
        if (days.isEmpty()) return@Canvas
        val gap = 4.dp.toPx()
        val w = ((size.width - gap * (days.size - 1)) / days.size).coerceAtLeast(2f)
        days.forEachIndexed { i, d ->
            val x = i * (w + gap)
            drawRoundRect(c.surfaceMuted, Offset(x, 0f), Size(w, size.height), CornerRadius(4.dp.toPx()))
            if (d.total > 0) {
                val share = d.taken.toFloat() / d.total
                val h = (size.height * share).coerceAtLeast(if (d.taken > 0) 4.dp.toPx() else 0f)
                drawRoundRect(if (share >= 1f) c.primary else c.accent, Offset(x, size.height - h), Size(w, h), CornerRadius(4.dp.toPx()))
            }
        }
    }
}

// ---------- Статистика ----------

@Composable
fun StatsScreen(onBack: () -> Unit, vm: HistoryViewModel = hiltViewModel()) {
    val st by vm.state.collectAsStateWithLifecycle()
    val f = st.family
    Column(Modifier.fillMaxSize().background(Ry.colors.bg)) {
        RyTopBar(stringResource(R.string.stats_title), navLabel = stringResource(R.string.common_back), onNav = onBack)
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            LazyColumn(
                Modifier.widthIn(max = 640.dp).fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item { PeriodSwitch(st.period, vm::setPeriod, allowCustom = true) }
                if (st.loading || f == null) { item { SkeletonList() }; return@LazyColumn }
                item { SummaryCard(st, null) }
                item {
                    RyGroup {
                        listOf(
                            Triple(RyIcons.CheckCircle, R.string.stats_taken, st.taken),
                            Triple(RyIcons.Later, R.string.stats_snoozed, st.snoozed),
                            Triple(RyIcons.Skip, R.string.stats_skipped, st.skipped),
                        ).forEachIndexed { i, (icon, label, value) ->
                            if (i > 0) RyDivider(start = 52.dp)
                            RyListRow(stringResource(label).replaceFirstChar { it.uppercase() }, icon = icon,
                                trailing = { Text(value.toString(), style = Ry.type.title, color = Ry.colors.text) })
                        }
                    }
                }
                // По людям: у каждого своя регулярность, без рейтинга и сравнения.
                val byPerson = f.members.mapNotNull { p ->
                    val items = st.past.flatMap { it.intakes }.filter { it.person.id == p.id && !it.isOpen && it.status != IntakeStatus.CANCELLED }
                    if (items.isEmpty()) null else Triple(p, items.count { it.isDone }, items.size)
                }.sortedBy { it.first.name }
                if (byPerson.isNotEmpty()) {
                    item { SectionTitle(stringResource(R.string.stats_by_person)) }
                    item {
                        RyGroup {
                            byPerson.forEachIndexed { i, (p, done, total) ->
                                if (i > 0) RyDivider(start = 16.dp)
                                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        PersonAvatar(p, size = 24.dp)
                                        Text(p.name, style = Ry.type.subtitle, color = Ry.colors.text, modifier = Modifier.weight(1f))
                                        Text(stringResource(R.string.count_of, done, total), style = Ry.type.caption, color = Ry.colors.textMuted)
                                    }
                                    RyMeter(done.toFloat() / total, false)
                                }
                            }
                        }
                    }
                }
                val byMed = st.past.flatMap { it.intakes }.filter { !it.isOpen && it.status != IntakeStatus.CANCELLED }
                    .groupBy { it.medication.id }.map { (_, l) -> Triple(l.first().medication, l.count { it.isDone }, l.size) }
                    .sortedBy { it.first.name.lowercase() }
                if (byMed.isNotEmpty()) {
                    item { SectionTitle(stringResource(R.string.stats_by_medication)) }
                    item {
                        RyGroup {
                            byMed.forEachIndexed { i, (m, done, total) ->
                                if (i > 0) RyDivider(start = 16.dp)
                                RyListRow(m.name, subtitle = stringResource(R.string.count_of, done, total) + " · " + (f.person(m.personId)?.name ?: ""), trailing = null)
                            }
                        }
                    }
                }
                item { Text(stringResource(R.string.stats_note), style = Ry.type.caption, color = Ry.colors.textMuted) }
            }
        }
    }
}

// ---------- День в истории ----------

@HiltViewModel
class HistoryDayViewModel @Inject constructor(handle: SavedStateHandle, plan: PlanRepository, family: FamilyRepository) : ViewModel() {
    val date: LocalDate = LocalDate.parse(handle.toRoute<HistoryDayRoute>().date)
    val state = combine(plan.range(date, date), family.family) { d, f -> d.firstOrNull() to f }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null to null)
}

@Composable
fun HistoryDayScreen(onBack: () -> Unit, onMedication: (String) -> Unit, vm: HistoryDayViewModel = hiltViewModel()) {
    val (day, f) = vm.state.collectAsStateWithLifecycle().value
    Column(Modifier.fillMaxSize().background(Ry.colors.bg)) {
        RyTopBar(vm.date.longDate(), navLabel = stringResource(R.string.common_back), onNav = onBack)
        if (day == null || f == null) return@Column
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            LazyColumn(Modifier.widthIn(max = 640.dp).fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                if (day.intakes.isEmpty()) {
                    item { EmptyState(Illustration.Calendar, stringResource(R.string.history_no_intakes), "") }
                    return@LazyColumn
                }
                item {
                    RyGroup {
                        day.intakes.forEachIndexed { i, it ->
                            if (i > 0) RyDivider(start = 16.dp)
                            val ev = it.event
                            val by = f.person(ev?.byPersonId)
                            val (text, kind) = when (it.status) {
                                IntakeStatus.TAKEN -> stringResource(R.string.status_taken) to BadgeKind.Ok
                                IntakeStatus.SKIPPED -> stringResource(R.string.status_skipped) to BadgeKind.Neutral
                                IntakeStatus.CANCELLED -> stringResource(R.string.status_cancelled) to BadgeKind.Neutral
                                IntakeStatus.SNOOZED -> stringResource(R.string.status_snoozed) to BadgeKind.Warn
                                IntakeStatus.DUE -> stringResource(R.string.status_due) to BadgeKind.Ok
                                IntakeStatus.UPCOMING -> stringResource(R.string.status_upcoming) to BadgeKind.Neutral
                            }
                            RyListRow(
                                "${it.scheduledAt.toLocalTime().hhmm()}  ${it.medication.name}",
                                subtitle = buildList {
                                    add(it.person.name)
                                    if (ev?.action == IntakeAction.TAKEN) add(stringResource(R.string.meta_taken_by, ev.at.atZone(ZoneId.systemDefault()).toLocalTime().hhmm(), by?.name ?: ""))
                                    ev?.snoozedUntil?.let { s -> add(stringResource(R.string.meta_moved_to, s.toLocalTime().hhmm())) }
                                }.joinToString(" · "),
                                leading = { PersonDot(it.person) },
                                trailing = { RyBadge(text, kind) },
                                onClick = { onMedication(it.medication.id) },
                            )
                        }
                    }
                }
            }
        }
    }
}

data class HistoryNav(val back: () -> Unit, val openMedication: (String) -> Unit, val openStats: () -> Unit, val openDay: (LocalDate) -> Unit)

fun NavGraphBuilder.historyGraph(nav: HistoryNav) {
    composable<HistoryRoute> { HistoryScreen(onStats = nav.openStats, onDay = nav.openDay, onMedication = nav.openMedication) }
    composable<StatsRoute> { StatsScreen(onBack = nav.back) }
    composable<HistoryDayRoute> { HistoryDayScreen(onBack = nav.back, onMedication = nav.openMedication) }
}
