package app.ryadom.core.data.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import app.ryadom.core.data.di.AppScope
import app.ryadom.core.model.RyadomException
import app.ryadom.core.model.RyadomException.Reason
import app.ryadom.core.data.store.DemoSeed
import app.ryadom.core.data.store.LocalStore
import app.ryadom.core.data.store.StoreState
import app.ryadom.core.data.util.Clock
import app.ryadom.core.data.util.newId
import app.ryadom.core.model.FamilyGroup
import app.ryadom.core.model.FamilyInvitation
import app.ryadom.core.model.FamilyMessage
import app.ryadom.core.model.FamilyRole
import app.ryadom.core.model.Intake
import app.ryadom.core.model.IntakeAction
import app.ryadom.core.model.IntakeEvent
import app.ryadom.core.model.IntakeStatus
import app.ryadom.core.model.Medication
import app.ryadom.core.model.NotificationSettings
import app.ryadom.core.model.Person
import app.ryadom.core.model.PersonAccent
import app.ryadom.core.model.PersonKind
import app.ryadom.core.model.QuickMessage
import app.ryadom.core.model.RestockEntry
import app.ryadom.core.model.ScheduleEngine
import app.ryadom.core.model.SyncState
import app.ryadom.core.model.ThemeMode
import app.ryadom.core.model.User
import app.ryadom.core.model.UserSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import java.security.SecureRandom
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

private val EMAIL = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")

@Singleton
class LocalSessionRepository @Inject constructor(
    private val store: LocalStore,
    private val clock: Clock,
) : SessionRepository {

    override val session: Flow<Session> = combine(store.ready, store.state) { ready, s ->
        when {
            !ready -> Session.Loading
            s.user == null -> Session.SignedOut
            s.family == null -> Session.NoFamily(s.user)
            else -> {
                val me = s.family.person(s.meId)
                if (me == null) Session.NoFamily(s.user) else Session.Ready(s.user, me, s.family, s.isDemo)
            }
        }
    }.distinctUntilChanged()

    override suspend fun register(name: String, email: String, password: String) {
        if (name.isBlank()) throw RyadomException(Reason.EMPTY_NAME)
        if (!EMAIL.matches(email.trim())) throw RyadomException(Reason.INVALID_EMAIL)
        if (password.length < 8) throw RyadomException(Reason.WEAK_PASSWORD)
        // Пароль не сохраняется: на этапе 5 его примет Firebase Authentication.
        store.update { StoreState(user = User(newId(), name.trim(), email.trim().lowercase(), clock.instant())) }
    }

    override suspend fun signIn(email: String, password: String) {
        if (!EMAIL.matches(email.trim())) throw RyadomException(Reason.INVALID_EMAIL)
        // До подключения облака аккаунты живут только на этом телефоне.
        throw RyadomException(Reason.NEEDS_CLOUD)
    }

    override suspend fun signOut() = store.reset()

    override suspend fun deleteAccount() = store.reset()

    override suspend fun startDemo() = store.update { DemoSeed.build(clock) }
}

@Singleton
class LocalFamilyRepository @Inject constructor(
    private val store: LocalStore,
    private val clock: Clock,
) : FamilyRepository {

    override val family: Flow<FamilyGroup?> = store.state.map { it.family }.distinctUntilChanged()
    override val me: Flow<Person?> = store.state.map { it.family?.person(it.meId) }.distinctUntilChanged()

    private fun StoreState.requireFamily() = family ?: throw RyadomException(Reason.NOT_FOUND)
    private fun StoreState.requireManager() {
        if (requireFamily().person(meId)?.canManageFamily != true) throw RyadomException(Reason.NOT_ALLOWED)
    }

    override suspend fun createFamily(familyName: String) {
        if (familyName.isBlank()) throw RyadomException(Reason.EMPTY_NAME)
        val now = clock.instant()
        store.update { s ->
            val user = s.user ?: throw RyadomException(Reason.ACCOUNT_NOT_FOUND)
            val me = Person(newId(), user.name, PersonKind.ADULT, PersonAccent.CLAY, FamilyRole.OWNER,
                userId = user.id, createdAt = now, updatedAt = now)
            s.copy(family = FamilyGroup(newId(), familyName.trim(), listOf(me), createdAt = now, updatedAt = now), meId = me.id)
        }
    }

    override suspend fun joinByCode(code: String) {
        if (code.trim().length < 6) throw RyadomException(Reason.INVITE_INVALID)
        // Код проверяет сервер, поэтому вход в чужую семью заработает вместе с облаком (этап 5).
        throw RyadomException(Reason.NEEDS_CLOUD)
    }

    override suspend fun renameFamily(name: String) {
        if (name.isBlank()) throw RyadomException(Reason.EMPTY_NAME)
        store.update { s ->
            s.requireManager()
            s.copy(family = s.requireFamily().copy(name = name.trim(), updatedAt = clock.instant()))
        }
    }

    override suspend fun addMember(name: String, kind: PersonKind, accent: PersonAccent, birthDate: LocalDate?): Person {
        if (name.isBlank()) throw RyadomException(Reason.EMPTY_NAME)
        val now = clock.instant()
        val p = Person(newId(), name.trim(), kind, accent,
            if (kind == PersonKind.CHILD) FamilyRole.CHILD_PROFILE else FamilyRole.MEMBER,
            birthDate = birthDate, createdAt = now, updatedAt = now)
        store.update { s ->
            s.requireManager()
            val f = s.requireFamily()
            s.copy(family = f.copy(members = f.members + p, updatedAt = now))
        }
        return p
    }

    override suspend fun updateMember(person: Person) {
        if (person.name.isBlank()) throw RyadomException(Reason.EMPTY_NAME)
        store.update { s ->
            val f = s.requireFamily()
            if (person.id != s.meId) s.requireManager()
            s.copy(family = f.copy(members = f.members.map { if (it.id == person.id) person.copy(updatedAt = clock.instant()) else it }))
        }
    }

    override suspend fun removeMember(personId: String) = store.update { s ->
        s.requireManager()
        val f = s.requireFamily()
        val target = f.person(personId) ?: throw RyadomException(Reason.NOT_FOUND)
        if (target.role == FamilyRole.OWNER) throw RyadomException(Reason.LAST_OWNER)
        // Препараты человека уходят в архив, история остаётся.
        s.copy(
            family = f.copy(members = f.members - target, updatedAt = clock.instant()),
            medications = s.medications.map { if (it.personId == personId) it.copy(archived = true, updatedAt = clock.instant()) else it },
        )
    }

    override suspend fun setRole(personId: String, role: FamilyRole) = store.update { s ->
        if (s.requireFamily().person(s.meId)?.role != FamilyRole.OWNER) throw RyadomException(Reason.NOT_ALLOWED)
        if (role == FamilyRole.OWNER || role == FamilyRole.CHILD_PROFILE) throw RyadomException(Reason.NOT_ALLOWED)
        val f = s.requireFamily()
        s.copy(family = f.copy(members = f.members.map { if (it.id == personId && it.kind == PersonKind.ADULT) it.copy(role = role) else it }))
    }

    override suspend fun transferOwnership(toPersonId: String) = store.update { s ->
        val f = s.requireFamily()
        if (f.person(s.meId)?.role != FamilyRole.OWNER) throw RyadomException(Reason.NOT_ALLOWED)
        val to = f.person(toPersonId) ?: throw RyadomException(Reason.NOT_FOUND)
        if (to.kind != PersonKind.ADULT || to.userId == null) throw RyadomException(Reason.NOT_ALLOWED)
        s.copy(family = f.copy(members = f.members.map {
            when (it.id) {
                toPersonId -> it.copy(role = FamilyRole.OWNER)
                s.meId -> it.copy(role = FamilyRole.ADMIN)
                else -> it
            }
        }))
    }

    override suspend fun createInvitation(): String {
        val code = inviteCode()
        val now = clock.instant()
        store.update { s ->
            s.requireManager()
            s.copy(family = s.requireFamily().copy(
                invitation = FamilyInvitation(code, s.meId!!, now, now.plus(Duration.ofDays(7)))))
        }
        return code
    }

    override suspend fun revokeInvitation() = store.update { s ->
        val f = s.requireFamily()
        s.copy(family = f.copy(invitation = f.invitation?.copy(revoked = true)))
    }

    override suspend fun leaveFamily() = store.update { s ->
        val f = s.requireFamily()
        val me = f.person(s.meId)
        if (me?.role == FamilyRole.OWNER && f.adults.any { it.id != me.id && it.userId != null }) {
            throw RyadomException(Reason.LAST_OWNER)
        }
        s.copy(family = null, meId = null, medications = emptyList(), events = emptyMap(), messages = emptyList(), restocks = emptyList(), isDemo = false)
    }

    override suspend fun deleteFamily() = store.update { s ->
        if (s.requireFamily().person(s.meId)?.role != FamilyRole.OWNER) throw RyadomException(Reason.NOT_ALLOWED)
        s.copy(family = null, meId = null, medications = emptyList(), events = emptyMap(), messages = emptyList(), restocks = emptyList(), isDemo = false)
    }

    private fun inviteCode(): String {
        val alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789" // без похожих символов 0/O и 1/I
        val rnd = SecureRandom()
        return (1..6).map { alphabet[rnd.nextInt(alphabet.length)] }.joinToString("")
    }
}

@Singleton
class LocalMedicationRepository @Inject constructor(
    private val store: LocalStore,
    private val clock: Clock,
) : MedicationRepository {

    override fun medications(includeArchived: Boolean): Flow<List<Medication>> = store.state.map { s ->
        s.medications.filter { it.isVisible && (includeArchived || !it.archived) }.sortedBy { it.name.lowercase() }
    }.distinctUntilChanged()

    override fun medication(id: String): Flow<Medication?> =
        store.state.map { s -> s.medications.firstOrNull { it.id == id && it.isVisible } }.distinctUntilChanged()

    override fun restocks(medicationId: String): Flow<List<RestockEntry>> =
        store.state.map { s -> s.restocks.filter { it.medicationId == medicationId }.sortedByDescending { it.at } }

    override suspend fun save(medication: Medication) {
        if (medication.name.isBlank()) throw RyadomException(Reason.EMPTY_NAME)
        store.update { s ->
            val now = clock.instant()
            val exists = s.medications.any { it.id == medication.id }
            val m = medication.copy(updatedAt = now)
            s.copy(medications = if (exists) s.medications.map { if (it.id == m.id) m else it } else s.medications + m.copy(createdAt = now))
        }
    }

    private suspend fun change(id: String, block: (Medication) -> Medication) = store.update { s ->
        s.copy(medications = s.medications.map { if (it.id == id) block(it).copy(updatedAt = clock.instant()) else it })
    }

    override suspend fun archive(id: String, archived: Boolean) = change(id) { it.copy(archived = archived) }
    override suspend fun delete(id: String) = change(id) { it.copy(deletedAt = clock.instant(), archived = true) }
    override suspend fun finishCourse(id: String, date: LocalDate) =
        change(id) { it.copy(course = it.course.copy(finishedAt = date, end = it.course.end ?: date)) }

    override suspend fun setRemaining(id: String, remaining: Double) = change(id) { m ->
        m.copy(inventory = (m.inventory ?: app.ryadom.core.model.Inventory(0.0)).copy(remaining = remaining.coerceAtLeast(0.0)))
    }

    override suspend fun restock(id: String, amount: Double) = store.update { s ->
        val now = clock.instant()
        val entry = RestockEntry(newId(), id, amount, s.meId ?: "", now)
        s.copy(
            medications = s.medications.map { m ->
                if (m.id != id) m else m.copy(
                    inventory = (m.inventory ?: app.ryadom.core.model.Inventory(0.0)).let { it.copy(remaining = it.remaining + amount) },
                    updatedAt = now,
                )
            },
            restocks = s.restocks + entry,
        )
    }
}

@Singleton
class LocalIntakeRepository @Inject constructor(
    private val store: LocalStore,
    private val clock: Clock,
) : IntakeRepository {

    override fun events(): Flow<Map<String, IntakeEvent>> = store.state.map { it.events }.distinctUntilChanged()

    private suspend fun record(intake: Intake, action: IntakeAction, until: LocalDateTime? = null) = store.update { s ->
        val prev = s.events[intake.id]
        val wasTaken = prev?.action == IntakeAction.TAKEN
        val nowTaken = action == IntakeAction.TAKEN
        val amount = intake.medication.dosage.amount
        val ev = IntakeEvent(
            id = intake.id, medicationId = intake.medication.id, personId = intake.person.id,
            scheduledAt = intake.scheduledAt, action = action, byPersonId = s.meId ?: "",
            at = clock.instant(), snoozedUntil = until, amount = if (nowTaken) amount else 0.0,
        )
        // Остаток меняется только при переходе в «принято» и обратно, повторная отметка не списывает дважды.
        val delta = when {
            nowTaken && !wasTaken -> -amount
            !nowTaken && wasTaken -> prev!!.amount
            else -> 0.0
        }
        s.copy(events = s.events + (ev.id to ev), medications = s.medications.adjust(intake.medication.id, delta))
    }

    override suspend fun markTaken(intake: Intake) = record(intake, IntakeAction.TAKEN)
    override suspend fun skip(intake: Intake) = record(intake, IntakeAction.SKIPPED)
    override suspend fun cancelForToday(intake: Intake) = record(intake, IntakeAction.CANCELLED)
    override suspend fun snooze(intake: Intake, until: LocalDateTime) = record(intake, IntakeAction.SNOOZED, until)

    override suspend fun undo(intake: Intake) = store.update { s ->
        val prev = s.events[intake.id] ?: return@update s
        val back = if (prev.action == IntakeAction.TAKEN) prev.amount else 0.0
        s.copy(events = s.events - intake.id, medications = s.medications.adjust(intake.medication.id, back))
    }

    private fun List<Medication>.adjust(id: String, delta: Double) = if (delta == 0.0) this else map { m ->
        val inv = m.inventory
        if (m.id != id || inv == null) m
        else m.copy(inventory = inv.copy(remaining = (inv.remaining + delta).coerceAtLeast(0.0)))
    }
}

@Singleton
class LocalMessageRepository @Inject constructor(
    private val store: LocalStore,
    private val clock: Clock,
) : MessageRepository {
    override val messages: Flow<List<FamilyMessage>> =
        store.state.map { s -> s.messages.sortedByDescending { it.at } }.distinctUntilChanged()

    override suspend fun send(quick: QuickMessage?, text: String?, toId: String?, medicationId: String?) {
        val clean = text?.trim()?.take(140)?.takeIf { it.isNotEmpty() }
        if (quick == null && clean == null) throw RyadomException(Reason.EMPTY_NAME)
        store.update { s ->
            val me = s.meId ?: throw RyadomException(Reason.ACCOUNT_NOT_FOUND)
            s.copy(messages = s.messages + FamilyMessage(newId(), me, toId, quick, clean, medicationId, clock.instant(), setOf(me)))
        }
    }

    override suspend fun markAllRead() = store.update { s ->
        val me = s.meId ?: return@update s
        s.copy(messages = s.messages.map { if (me in it.readBy) it else it.copy(readBy = it.readBy + me) })
    }
}

@Singleton
class LocalPlanRepository @Inject constructor(
    private val store: LocalStore,
    private val clock: Clock,
) : PlanRepository {

    override fun today(): Flow<LocalDate> = clock.minutes().map { it.toLocalDate() }.distinctUntilChanged()

    override fun day(date: LocalDate?, personId: String?): Flow<DayPlan> =
        combine(store.state, clock.minutes()) { s, now ->
            val d = date ?: now.toLocalDate()
            val fam = s.family ?: return@combine DayPlan(d, now, emptyList(), emptyList())
            val meds = s.medications.filter { it.isVisible && (personId == null || it.personId == personId) }
            val intakes = ScheduleEngine.intakes(d, meds, fam, s.events, now)
            val people = fam.members.mapNotNull { p ->
                val own = intakes.filter { it.person.id == p.id && it.status != IntakeStatus.CANCELLED }
                if (own.isEmpty()) null else PersonProgress(p, own.count { it.isDone }, own.size)
            }
            DayPlan(d, now, intakes, people)
        }.distinctUntilChanged()

    override fun range(from: LocalDate, to: LocalDate, personId: String?): Flow<List<DaySummary>> =
        combine(store.state, clock.minutes()) { s, now ->
            val fam = s.family ?: return@combine emptyList()
            // История берёт и архивные, и удалённые препараты: удаление не стирает прошлые приёмы.
            val meds = s.medications.filter { personId == null || it.personId == personId }
                .map { if (it.deletedAt != null || it.archived) it.copy(archived = false, deletedAt = null) else it }
            generateSequence(from) { it.plusDays(1) }.takeWhile { !it.isAfter(to) }.map { d ->
                val list = ScheduleEngine.intakes(d, meds.filter { m ->
                    val original = s.medications.first { it.id == m.id }
                    val hiddenSince = original.deletedAt?.atZone(clock.zone())?.toLocalDate()
                        ?: original.updatedAt.atZone(clock.zone()).toLocalDate().takeIf { original.archived }
                    hiddenSince == null || d.isBefore(hiddenSince) || s.events.values.any { it.medicationId == m.id && it.scheduledAt.toLocalDate() == d }
                }, fam, s.events, now)
                val counted = list.filter { it.status != IntakeStatus.CANCELLED }
                DaySummary(
                    date = d,
                    total = counted.size,
                    taken = counted.count { it.isDone },
                    skipped = counted.count { it.status == IntakeStatus.SKIPPED },
                    snoozed = list.count { it.event?.snoozedUntil != null },
                    markedBy = list.mapNotNull { it.event?.takeIf { e -> e.action == IntakeAction.TAKEN }?.byPersonId }
                        .distinct().mapNotNull { fam.person(it) },
                    intakes = list,
                )
            }.toList()
        }.distinctUntilChanged()
}

@Singleton
class DataStoreSettingsRepository @Inject constructor(
    private val prefs: DataStore<Preferences>,
) : SettingsRepository {
    private val key = stringPreferencesKey("settings")
    private val json = Json { ignoreUnknownKeys = true }

    override val settings: Flow<UserSettings> = prefs.data.map { p ->
        p[key]?.let { runCatching { json.decodeFromString(UserSettings.serializer(), it) }.getOrNull() } ?: UserSettings()
    }.distinctUntilChanged()

    private suspend fun edit(block: (UserSettings) -> UserSettings) {
        prefs.edit { p ->
            val cur = p[key]?.let { runCatching { json.decodeFromString(UserSettings.serializer(), it) }.getOrNull() } ?: UserSettings()
            p[key] = json.encodeToString(UserSettings.serializer(), block(cur))
        }
    }

    override suspend fun setTheme(theme: ThemeMode) = edit { it.copy(theme = theme) }
    override suspend fun setLanguage(tag: String?) = edit { it.copy(language = tag) }
    override suspend fun updateNotifications(transform: (NotificationSettings) -> NotificationSettings) =
        edit { it.copy(notifications = transform(it.notifications)) }
}

@Singleton
class DefaultConnectivityRepository @Inject constructor(
    @ApplicationContext context: Context,
    @AppScope scope: CoroutineScope,
) : ConnectivityRepository {
    private val cm = context.getSystemService(ConnectivityManager::class.java)
    private val _online = MutableStateFlow(isOnlineNow())
    override val online: StateFlow<Boolean> = _online.asStateFlow()

    init {
        val request = NetworkRequest.Builder().addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET).build()
        runCatching {
            cm?.registerNetworkCallback(request, object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) { _online.value = isOnlineNow() }
                override fun onLost(network: Network) { _online.value = isOnlineNow() }
                override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) { _online.value = isOnlineNow() }
            })
        }
    }

    private fun isOnlineNow(): Boolean {
        val caps = cm?.getNetworkCapabilities(cm.activeNetwork) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    // Облако подключается на этапе 5; до этого честно показываем «только на этом телефоне».
    override val sync: Flow<SyncState> = online.map { if (it) SyncState.LOCAL_ONLY else SyncState.OFFLINE }
}
