package app.ryadom.feature.family

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import app.ryadom.core.data.repository.DaySummary
import app.ryadom.core.data.repository.FamilyRepository
import app.ryadom.core.data.repository.MedicationRepository
import app.ryadom.core.data.repository.MessageRepository
import app.ryadom.core.data.repository.PlanRepository
import app.ryadom.core.data.repository.SettingsRepository
import app.ryadom.core.data.util.Clock
import app.ryadom.core.model.FamilyGroup
import app.ryadom.core.model.FamilyMessage
import app.ryadom.core.model.FamilyNudgeScope
import app.ryadom.core.model.FamilyRole
import app.ryadom.core.model.Medication
import app.ryadom.core.model.Person
import app.ryadom.core.model.PersonAccent
import app.ryadom.core.model.PersonKind
import app.ryadom.core.model.QuickMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import java.time.LocalDate
import javax.inject.Inject

/** Вкладка «Семья». invite = true — сразу открыть приглашение. */
@Serializable data class FamilyRoute(val invite: Boolean = false)
@Serializable data class MemberRoute(val id: String)
@Serializable data class MemberEditRoute(val id: String? = null, val child: Boolean = true)
@Serializable data object MessagesRoute

// ---------- Семья ----------

data class FamilyState(
    val family: FamilyGroup? = null,
    val me: Person? = null,
    val unread: Int = 0,
    val now: java.time.Instant = java.time.Instant.now(),
)

@HiltViewModel
class FamilyViewModel @Inject constructor(
    private val repo: FamilyRepository,
    messages: MessageRepository,
    private val clock: Clock,
) : ViewModel() {
    private val errors = Channel<Throwable>(Channel.BUFFERED)
    val errorEvents = errors.receiveAsFlow()

    val state: StateFlow<FamilyState> = combine(repo.family, repo.me, messages.messages) { f, me, msgs ->
        FamilyState(f, me, msgs.count { me != null && it.fromId != me.id && me.id !in it.readBy && (it.toId == null || it.toId == me.id) }, clock.instant())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FamilyState())

    private fun run(block: suspend () -> Unit) = viewModelScope.launch { runCatching { block() }.onFailure { errors.send(it) } }
    fun createInvite() = run { repo.createInvitation() }
    fun revokeInvite() = run { repo.revokeInvitation() }
    fun rename(name: String) = run { repo.renameFamily(name) }
}

// ---------- Профиль члена семьи ----------

data class MemberState(
    val loading: Boolean = true,
    val person: Person? = null,
    val me: Person? = null,
    val family: FamilyGroup? = null,
    val meds: List<Medication> = emptyList(),
    val week: List<DaySummary> = emptyList(),
    val scope: FamilyNudgeScope = FamilyNudgeScope.CHILDREN_ONLY,
    val today: LocalDate = LocalDate.now(),
    val removed: Boolean = false,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MemberViewModel @Inject constructor(
    handle: SavedStateHandle,
    private val repo: FamilyRepository,
    meds: MedicationRepository,
    plan: PlanRepository,
    settings: SettingsRepository,
    private val clock: Clock,
) : ViewModel() {
    val id = handle.toRoute<MemberRoute>().id
    private val removed = MutableStateFlow(false)
    private val errors = Channel<Throwable>(Channel.BUFFERED)
    val errorEvents = errors.receiveAsFlow()

    val state: StateFlow<MemberState> = combine(
        combine(repo.family, repo.me, ::Pair),
        meds.medications(includeArchived = true),
        plan.today().flatMapLatest { t -> plan.range(t.minusDays(6), t, id) },
        settings.settings,
        removed,
    ) { (f, me), m, week, prefs, gone ->
        MemberState(false, f?.person(id), me, f, m.filter { it.personId == id }, week, prefs.notifications.familyScope, clock.today(), gone)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MemberState())

    private fun run(block: suspend () -> Unit) = viewModelScope.launch { runCatching { block() }.onFailure { errors.send(it) } }
    fun remove() = run { repo.removeMember(id); removed.value = true }
    fun makeAdmin(on: Boolean) = run { repo.setRole(id, if (on) FamilyRole.ADMIN else FamilyRole.MEMBER) }
    fun transferOwnership() = run { repo.transferOwnership(id) }
}

// ---------- Добавление / редактирование ----------

data class MemberDraft(
    val name: String = "",
    val kind: PersonKind = PersonKind.CHILD,
    val accent: PersonAccent = PersonAccent.SAND,
    val birthDate: LocalDate? = null,
)

data class MemberEditState(
    val loading: Boolean = true,
    val isNew: Boolean = true,
    val draft: MemberDraft = MemberDraft(),
    val original: Person? = null,
    val nameError: Boolean = false,
    val done: Boolean = false,
    val error: Throwable? = null,
)

@HiltViewModel
class MemberEditViewModel @Inject constructor(
    handle: SavedStateHandle,
    private val repo: FamilyRepository,
) : ViewModel() {
    private val route = handle.toRoute<MemberEditRoute>()
    private val _state = MutableStateFlow(MemberEditState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val f = repo.family.filterNotNull().first()
            val p = route.id?.let { f.person(it) }
            val used = f.members.map { it.accent }.toSet()
            val freeAccent = PersonAccent.entries.firstOrNull { it !in used } ?: PersonAccent.SAGE
            _state.value = if (p != null) MemberEditState(false, false, MemberDraft(p.name, p.kind, p.accent, p.birthDate), p)
            else MemberEditState(false, true, MemberDraft(kind = if (route.child) PersonKind.CHILD else PersonKind.ADULT, accent = freeAccent))
        }
    }

    fun edit(block: (MemberDraft) -> MemberDraft) = _state.update { it.copy(draft = block(it.draft), nameError = false) }

    fun save() {
        val s = _state.value
        if (s.draft.name.isBlank()) { _state.update { it.copy(nameError = true) }; return }
        viewModelScope.launch {
            runCatching {
                val o = s.original
                if (o == null) repo.addMember(s.draft.name, s.draft.kind, s.draft.accent, s.draft.birthDate)
                else repo.updateMember(o.copy(name = s.draft.name.trim(), accent = s.draft.accent, birthDate = s.draft.birthDate))
            }.onSuccess { _state.update { it.copy(done = true) } }
                .onFailure { e -> _state.update { it.copy(error = e) } }
        }
    }
}

// ---------- Сообщения ----------

data class MessagesState(val family: FamilyGroup? = null, val me: Person? = null, val messages: List<FamilyMessage> = emptyList(), val meds: Map<String, Medication> = emptyMap())

@HiltViewModel
class MessagesViewModel @Inject constructor(
    private val repo: MessageRepository,
    family: FamilyRepository,
    meds: MedicationRepository,
) : ViewModel() {
    val state: StateFlow<MessagesState> = combine(family.family, family.me, repo.messages, meds.medications(true)) { f, me, msgs, m ->
        MessagesState(f, me, msgs.sortedBy { it.at }, m.associateBy { it.id })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MessagesState())

    init { viewModelScope.launch { repo.markAllRead() } }

    fun send(quick: QuickMessage?, text: String?, toId: String?) = viewModelScope.launch {
        runCatching { repo.send(quick, text, toId) }
        repo.markAllRead()
    }
}
