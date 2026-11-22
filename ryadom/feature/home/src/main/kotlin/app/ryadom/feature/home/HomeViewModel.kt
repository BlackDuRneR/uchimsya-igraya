package app.ryadom.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.ryadom.core.data.repository.ConnectivityRepository
import app.ryadom.core.data.repository.DayPlan
import app.ryadom.core.data.repository.IntakeCommands
import app.ryadom.core.data.repository.MedicationRepository
import app.ryadom.core.data.repository.MessageRepository
import app.ryadom.core.data.repository.PlanRepository
import app.ryadom.core.data.repository.Session
import app.ryadom.core.data.repository.SessionRepository
import app.ryadom.core.data.repository.SettingsRepository
import app.ryadom.core.model.FamilyGroup
import app.ryadom.core.model.FamilyMessage
import app.ryadom.core.model.Intake
import app.ryadom.core.model.Medication
import app.ryadom.core.model.Person
import app.ryadom.core.model.QuickMessage
import app.ryadom.core.model.ScheduleEngine
import app.ryadom.core.model.SyncState
import app.ryadom.core.ui.IntakeActions
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject

data class HomeState(
    val loading: Boolean = true,
    val me: Person? = null,
    val family: FamilyGroup? = null,
    val isDemo: Boolean = false,
    val plan: DayPlan? = null,
    val sync: SyncState = SyncState.LOCAL_ONLY,
    val message: FamilyMessage? = null,
    val lowStock: List<Medication> = emptyList(),
    val courseEnding: List<Medication> = emptyList(),
    val showDayComplete: Boolean = true,
)

sealed interface HomeEvent {
    data class OpenMedication(val id: String) : HomeEvent
    data class Error(val error: Throwable) : HomeEvent
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    session: SessionRepository,
    plan: PlanRepository,
    messages: MessageRepository,
    medications: MedicationRepository,
    connectivity: ConnectivityRepository,
    settings: SettingsRepository,
    private val commands: IntakeCommands,
    private val sessionRepo: SessionRepository,
    private val messageRepo: MessageRepository,
) : ViewModel(), IntakeActions {

    private val events = Channel<HomeEvent>(Channel.BUFFERED)
    val uiEvents = events.receiveAsFlow()

    val state: StateFlow<HomeState> = combine(
        session.session, plan.day(), messages.messages, medications.medications(), combine(connectivity.sync, settings.settings, ::Pair),
    ) { s, day, msgs, meds, (sync, prefs) ->
        val ready = s as? Session.Ready ?: return@combine HomeState(loading = s is Session.Loading)
        val incoming = msgs.firstOrNull { it.fromId != ready.me.id && (it.toId == null || it.toId == ready.me.id) && ready.me.id !in it.readBy }
        HomeState(
            loading = false,
            me = ready.me,
            family = ready.family,
            isDemo = ready.isDemo,
            plan = day,
            sync = sync,
            message = incoming,
            lowStock = meds.filter { ScheduleEngine.isLowStock(it) && !ScheduleEngine.isCourseFinished(it, day.date) },
            courseEnding = meds.filter { ScheduleEngine.courseEndsOn(it, day.date) && it.course.notifyOnEnd },
            showDayComplete = prefs.notifications.dayCompleteScreen,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeState())

    private fun launch(block: suspend () -> Unit) = viewModelScope.launch {
        runCatching { block() }.onFailure { events.send(HomeEvent.Error(it)) }
    }

    fun toggle(intake: Intake) = launch { commands.toggle(intake) }
    override fun taken(intake: Intake) { launch { commands.taken(intake) } }
    override fun undo(intake: Intake) { launch { commands.undo(intake) } }
    override fun snooze(intake: Intake, until: LocalDateTime) { launch { commands.snooze(intake, until) } }
    override fun skip(intake: Intake) { launch { commands.skip(intake) } }
    override fun cancel(intake: Intake) { launch { commands.cancel(intake) } }
    override fun message(intake: Intake, quick: QuickMessage, toId: String?) { launch { commands.message(intake, quick, toId) } }
    override fun open(intake: Intake) { viewModelScope.launch { events.send(HomeEvent.OpenMedication(intake.medication.id)) } }

    fun replyThanks(msg: FamilyMessage) = launch {
        messageRepo.send(QuickMessage.THANKS, null, msg.fromId, msg.medicationId)
        messageRepo.markAllRead()
    }

    fun markMessagesRead() = launch { messageRepo.markAllRead() }

    /** Выйти из примера и начать со своей семьи. */
    fun leaveDemo() = launch { sessionRepo.signOut() }
}
