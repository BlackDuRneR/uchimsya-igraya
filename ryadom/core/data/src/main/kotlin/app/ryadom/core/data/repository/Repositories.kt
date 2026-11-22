package app.ryadom.core.data.repository

import app.ryadom.core.model.DayPart
import app.ryadom.core.model.FamilyGroup
import app.ryadom.core.model.FamilyMessage
import app.ryadom.core.model.FamilyRole
import app.ryadom.core.model.Intake
import app.ryadom.core.model.IntakeEvent
import app.ryadom.core.model.Medication
import app.ryadom.core.model.NotificationSettings
import app.ryadom.core.model.Person
import app.ryadom.core.model.PersonAccent
import app.ryadom.core.model.PersonKind
import app.ryadom.core.model.QuickMessage
import app.ryadom.core.model.RestockEntry
import app.ryadom.core.model.SyncState
import app.ryadom.core.model.ThemeMode
import app.ryadom.core.model.User
import app.ryadom.core.model.UserSettings
import kotlinx.coroutines.flow.Flow
import app.ryadom.core.model.RyadomException
import kotlinx.coroutines.flow.StateFlow
import java.time.LocalDate
import java.time.LocalDateTime

sealed interface Session {
    data object Loading : Session
    data object SignedOut : Session
    data class NoFamily(val user: User) : Session
    data class Ready(val user: User, val me: Person, val family: FamilyGroup, val isDemo: Boolean) : Session
}

interface SessionRepository {
    val session: Flow<Session>
    suspend fun register(name: String, email: String, password: String)
    suspend fun signIn(email: String, password: String)
    suspend fun signOut()
    /** Удаляет аккаунт и все данные этого телефона. */
    suspend fun deleteAccount()
    /** Загружает пример семьи, чтобы посмотреть приложение без регистрации. */
    suspend fun startDemo()
}

interface FamilyRepository {
    val family: Flow<FamilyGroup?>
    val me: Flow<Person?>
    suspend fun createFamily(familyName: String)
    suspend fun joinByCode(code: String)
    suspend fun renameFamily(name: String)
    suspend fun addMember(name: String, kind: PersonKind, accent: PersonAccent, birthDate: LocalDate?): Person
    suspend fun updateMember(person: Person)
    suspend fun removeMember(personId: String)
    suspend fun setRole(personId: String, role: FamilyRole)
    suspend fun transferOwnership(toPersonId: String)
    suspend fun createInvitation(): String
    suspend fun revokeInvitation()
    suspend fun leaveFamily()
    suspend fun deleteFamily()
}

interface MedicationRepository {
    fun medications(includeArchived: Boolean = false): Flow<List<Medication>>
    fun medication(id: String): Flow<Medication?>
    fun restocks(medicationId: String): Flow<List<RestockEntry>>
    suspend fun save(medication: Medication)
    suspend fun archive(id: String, archived: Boolean = true)
    /** Мягкое удаление: история приёмов сохраняется. */
    suspend fun delete(id: String)
    suspend fun finishCourse(id: String, date: LocalDate)
    suspend fun restock(id: String, amount: Double)
    suspend fun setRemaining(id: String, remaining: Double)
}

interface IntakeRepository {
    fun events(): Flow<Map<String, IntakeEvent>>
    suspend fun markTaken(intake: Intake)
    suspend fun skip(intake: Intake)
    suspend fun cancelForToday(intake: Intake)
    suspend fun snooze(intake: Intake, until: LocalDateTime)
    /** Снимает отметку; если приём был принят, возвращает единицы в остаток. */
    suspend fun undo(intake: Intake)
}

interface MessageRepository {
    val messages: Flow<List<FamilyMessage>>
    suspend fun send(quick: QuickMessage?, text: String?, toId: String?, medicationId: String? = null)
    suspend fun markAllRead()
}

data class PersonProgress(val person: Person, val done: Int, val total: Int)

data class DayPlan(
    val date: LocalDate,
    val now: LocalDateTime,
    val intakes: List<Intake>,
    val people: List<PersonProgress>,
) {
    val total: Int get() = intakes.count { it.status != app.ryadom.core.model.IntakeStatus.CANCELLED }
    val done: Int get() = intakes.count { it.isDone }
    val byPart: Map<DayPart, List<Intake>> get() = intakes.groupBy { it.dayPart }.toSortedMap()
    /** Приём для блока «Сейчас»: первое «пора», иначе ближайшее в течение часа. */
    val current: Intake?
        get() = intakes.firstOrNull { it.status == app.ryadom.core.model.IntakeStatus.DUE }
            ?: intakes.firstOrNull {
                it.status == app.ryadom.core.model.IntakeStatus.UPCOMING && !it.scheduledAt.isAfter(now.plusMinutes(60))
            }
    val allDone: Boolean get() = total > 0 && intakes.none { it.isOpen }
}

data class DaySummary(
    val date: LocalDate,
    val total: Int,
    val taken: Int,
    val skipped: Int,
    val snoozed: Int,
    val markedBy: List<Person>,
    val intakes: List<Intake>,
)

/** Готовые срезы для экранов: день, период. Пересчитываются раз в минуту и при любом изменении данных. */
interface PlanRepository {
    fun day(date: LocalDate? = null, personId: String? = null): Flow<DayPlan>
    fun range(from: LocalDate, to: LocalDate, personId: String? = null): Flow<List<DaySummary>>
    fun today(): Flow<LocalDate>
}

interface SettingsRepository {
    val settings: Flow<UserSettings>
    suspend fun setTheme(theme: ThemeMode)
    suspend fun setLanguage(tag: String?)
    suspend fun updateNotifications(transform: (NotificationSettings) -> NotificationSettings)
}

interface ConnectivityRepository {
    val online: StateFlow<Boolean>
    /** Состояние синхронизации для индикатора. До подключения облака — LOCAL_ONLY. */
    val sync: Flow<SyncState>
}
