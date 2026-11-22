package app.ryadom.feature.medication

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
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
import app.ryadom.core.designsystem.component.RyGroup
import app.ryadom.core.designsystem.component.RyIllustration
import app.ryadom.core.designsystem.component.RyMeter
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
import app.ryadom.core.model.ScheduleEngine
import app.ryadom.core.ui.PersonAvatar
import app.ryadom.core.ui.unitCount
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import java.time.temporal.ChronoUnit
import javax.inject.Inject

@Serializable data object InventoryRoute
@Serializable data class CourseCompleteRoute(val id: String)

// ---------- Остатки ----------

data class InventoryState(val loading: Boolean = true, val meds: List<Medication> = emptyList(), val family: FamilyGroup? = null)

@HiltViewModel
class InventoryViewModel @Inject constructor(private val meds: MedicationRepository, family: FamilyRepository) : ViewModel() {
    val state: StateFlow<InventoryState> = combine(meds.medications(), family.family) { m, f ->
        // Сначала то, что заканчивается, затем по алфавиту
        InventoryState(false, m.sortedWith(compareBy({ it.inventory == null }, { ScheduleEngine.daysLeft(it) ?: Int.MAX_VALUE })), f)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InventoryState())

    fun restock(m: Medication) = viewModelScope.launch { m.inventory?.packageSize?.let { meds.restock(m.id, it) } }
}

@Composable
fun InventoryScreen(onBack: () -> Unit, onOpen: (String) -> Unit, vm: InventoryViewModel = hiltViewModel()) {
    val st by vm.state.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize().background(Ry.colors.bg)) {
        RyTopBar(stringResource(R.string.inventory_title), navLabel = stringResource(R.string.common_back), onNav = onBack)
        val family = st.family
        when {
            st.loading || family == null -> Box(Modifier.padding(16.dp)) { SkeletonList() }
            st.meds.none { it.inventory != null } -> EmptyState(
                Illustration.Package, stringResource(R.string.inventory_empty_title), stringResource(R.string.inventory_empty_body),
            )
            else -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                LazyColumn(
                    Modifier.widthIn(max = 640.dp).fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    val tracked = st.meds.filter { it.inventory != null }
                    val untracked = st.meds.filter { it.inventory == null }
                    items(tracked, key = { it.id }) { m -> StockCard(m, family, { onOpen(m.id) }, { vm.restock(m) }) }
                    if (untracked.isNotEmpty()) {
                        item { SectionTitle(stringResource(R.string.inventory_untracked)) }
                        item {
                            Text(untracked.joinToString(", ") { it.name }, style = Ry.type.bodySmall, color = Ry.colors.textMuted)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StockCard(m: Medication, family: FamilyGroup, onOpen: () -> Unit, onRestock: () -> Unit) {
    val inv = m.inventory ?: return
    val low = ScheduleEngine.isLowStock(m)
    val days = ScheduleEngine.daysLeft(m)
    RyGroup(Modifier.clickable(onClick = onOpen)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                family.person(m.personId)?.let { PersonAvatar(it, size = 24.dp) }
                Text(m.name, style = Ry.type.subtitle, color = Ry.colors.text, modifier = Modifier.weight(1f))
                if (low) RyBadge(stringResource(R.string.stock_time_to_buy), BadgeKind.Warn, icon = RyIcons.Bag)
            }
            RyMeter((inv.remaining / (inv.packageSize ?: inv.remaining.coerceAtLeast(1.0))).toFloat(), low)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    buildString {
                        append(unitCount(m.dosage.unit, inv.remaining, m.dosage.customUnit))
                        if (days != null) append(" · ").append(stringResource(R.string.stock_days, days))
                    },
                    style = Ry.type.bodySmall, color = if (low) Ry.colors.warningText else Ry.colors.textMuted, modifier = Modifier.weight(1f),
                )
                if (inv.packageSize != null) RyButton(stringResource(R.string.stock_bought), onRestock, small = true, style = ButtonStyle.Text)
            }
        }
    }
}

// ---------- Завершение курса ----------

data class CourseState(val med: Medication? = null, val taken: Int = 0, val planned: Int = 0, val days: Int = 0)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CourseCompleteViewModel @Inject constructor(
    handle: SavedStateHandle,
    private val meds: MedicationRepository,
    plan: PlanRepository,
    private val clock: Clock,
) : ViewModel() {
    private val id = handle.toRoute<CourseCompleteRoute>().id
    val state: StateFlow<CourseState> = meds.medication(id).flatMapLatest { m ->
        if (m == null) return@flatMapLatest flowOf(CourseState())
        val end = minOf(m.course.end ?: clock.today(), clock.today())
        plan.range(m.course.start, end).combine(flowOf(m)) { days, med ->
            val list = days.flatMap { d -> d.intakes.filter { it.medication.id == med.id && it.status != IntakeStatus.CANCELLED } }
            CourseState(med, list.count { it.isDone }, list.size, ChronoUnit.DAYS.between(med.course.start, end).toInt() + 1)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CourseState())

    fun finish() = viewModelScope.launch { meds.finishCourse(id, clock.today()) }
}

@Composable
fun CourseCompleteDialog(onDismiss: () -> Unit, onExtend: (String) -> Unit, vm: CourseCompleteViewModel = hiltViewModel()) {
    val st by vm.state.collectAsStateWithLifecycle()
    val m = st.med ?: return
    Column(
        Modifier.widthIn(max = 420.dp).fillMaxWidth().padding(16.dp)
            .background(Ry.colors.surface, Ry.shapes.l).padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        RyIllustration(Illustration.CourseComplete, Modifier.widthIn(max = 240.dp).fillMaxWidth())
        Text(stringResource(R.string.course_complete_title, m.name), style = Ry.type.headline, color = Ry.colors.text, textAlign = TextAlign.Center)
        Text(stringResource(R.string.course_complete_body, st.days, st.taken, st.planned), style = Ry.type.bodySmall,
            color = Ry.colors.textMuted, textAlign = TextAlign.Center)
        RyButton(stringResource(R.string.course_finish_cta), { vm.finish(); onDismiss() }, Modifier.fillMaxWidth(), icon = RyIcons.Flag)
        RyButton(stringResource(R.string.course_extend), { onDismiss(); onExtend(m.id) }, Modifier.fillMaxWidth(), style = ButtonStyle.Ghost)
        RyButton(stringResource(R.string.common_later), onDismiss, style = ButtonStyle.Text)
    }
}

// ---------- Навигация фичи ----------

data class MedicationNav(
    val back: () -> Unit,
    val edit: (id: String?, personId: String?) -> Unit,
    val open: (String) -> Unit,
    val finishCourse: (String) -> Unit,
)

fun NavGraphBuilder.medicationGraph(nav: MedicationNav) {
    composable<MedicationEditRoute> { MedicationEditScreen(onClose = nav.back) }
    composable<MedicationDetailRoute> {
        MedicationDetailScreen(onBack = nav.back, onEdit = { nav.edit(it, null) }, onFinishCourse = nav.finishCourse)
    }
    composable<InventoryRoute> { InventoryScreen(onBack = nav.back, onOpen = nav.open) }
    dialog<CourseCompleteRoute>(dialogProperties = DialogProperties(usePlatformDefaultWidth = false)) {
        CourseCompleteDialog(onDismiss = nav.back, onExtend = { nav.edit(it, null) })
    }
}
