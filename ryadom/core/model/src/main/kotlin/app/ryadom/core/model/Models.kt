@file:UseSerializers(
    LocalDateSerializer::class,
    LocalTimeSerializer::class,
    LocalDateTimeSerializer::class,
    InstantSerializer::class,
)

package app.ryadom.core.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

// ---------- Люди и семья ----------

@Serializable
enum class PersonKind { ADULT, CHILD }

/** Роль в семейной группе. У профиля ребёнка нет аккаунта. */
@Serializable
enum class FamilyRole { OWNER, ADMIN, MEMBER, CHILD_PROFILE }

/** Цветовой акцент человека. Конкретные цвета задаёт дизайн-система. */
@Serializable
enum class PersonAccent { CLAY, MOSS, SAND, SAGE, ROSE, SLATE }

@Serializable
data class Person(
    val id: String,
    val name: String,
    val kind: PersonKind,
    val accent: PersonAccent,
    val role: FamilyRole,
    val birthDate: LocalDate? = null,
    /** ID аккаунта; null у профиля ребёнка. */
    val userId: String? = null,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    val initial: String get() = name.trim().firstOrNull()?.uppercase() ?: "?"
    val canManageFamily: Boolean get() = role == FamilyRole.OWNER || role == FamilyRole.ADMIN
}

@Serializable
data class FamilyInvitation(
    val code: String,
    val createdBy: String,
    val createdAt: Instant,
    val expiresAt: Instant,
    val revoked: Boolean = false,
) {
    fun isActive(now: Instant) = !revoked && now.isBefore(expiresAt)
}

@Serializable
data class FamilyGroup(
    val id: String,
    val name: String,
    val members: List<Person>,
    val invitation: FamilyInvitation? = null,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    val adults: List<Person> get() = members.filter { it.kind == PersonKind.ADULT }
    val children: List<Person> get() = members.filter { it.kind == PersonKind.CHILD }
    fun person(id: String?): Person? = members.firstOrNull { it.id == id }
}

/** Текущий пользователь этого телефона. */
@Serializable
data class User(
    val id: String,
    val name: String,
    val email: String,
    val createdAt: Instant,
)

// ---------- Препараты ----------

@Serializable
enum class MedicationCategory { VITAMIN, MINERAL, MEDICINE, SUPPLEMENT, OTHER }

@Serializable
enum class DoseUnit { TABLET, CAPSULE, ML, DROP, SACHET, OTHER }

/** Дозировку всегда вводит пользователь. Приложение её не рассчитывает. */
@Serializable
data class Dosage(
    val amount: Double,
    val unit: DoseUnit,
    val customUnit: String? = null,
)

@Serializable
sealed interface DaysRule {
    @Serializable
    data object Daily : DaysRule

    @Serializable
    data class Weekdays(val days: Set<DayOfWeek>) : DaysRule

    /** Раз в N дней, считая от [anchor]. */
    @Serializable
    data class Interval(val everyDays: Int, val anchor: LocalDate) : DaysRule
}

@Serializable
data class MedicationSchedule(
    val times: List<LocalTime>,
    val days: DaysRule = DaysRule.Daily,
)

/** Курс приёма. end == null — без даты окончания. */
@Serializable
data class MedicationCourse(
    val start: LocalDate,
    val end: LocalDate? = null,
    val notifyOnEnd: Boolean = true,
    val finishedAt: LocalDate? = null,
)


@Serializable
data class ReminderSettings(
    val enabled: Boolean = true,
    /** За сколько минут напомнить заранее; 0 — только в момент приёма. */
    val advanceMinutes: Int = 0,
    /** Повтор, если приём не отмечен; null — без повтора. */
    val repeatAfterMinutes: Int? = 15,
    /** Сообщить второму взрослому, если не отмечено через N минут; null — не сообщать. */
    val familyNudgeAfterMinutes: Int? = null,
)

@Serializable
data class Inventory(
    /** Сколько единиц (таблеток, мл, капель…) осталось сейчас. */
    val remaining: Double,
    /** Сколько в одной упаковке — для отметки «купили новую». */
    val packageSize: Double? = null,
    /** Предупредить, когда останется меньше N дней. */
    val warnDays: Int = 7,
)

@Serializable
data class Medication(
    val id: String,
    val name: String,
    val category: MedicationCategory,
    val personId: String,
    val dosage: Dosage,
    val schedule: MedicationSchedule,
    val course: MedicationCourse,
    /** Кто из взрослых следит за приёмом. Пусто — любой член семьи. */
    val caretakerIds: Set<String> = emptySet(),
    val reminder: ReminderSettings = ReminderSettings(),
    val inventory: Inventory? = null,
    val description: String = "",
    val notes: String = "",
    val photoUri: String? = null,
    /** Важный приём: семейные уведомления в режиме «только важные». */
    val important: Boolean = false,
    /** Архив: препарат не в расписании, но виден в списке «Архив». */
    val archived: Boolean = false,
    /** Мягкое удаление: препарат скрыт везде, история приёмов остаётся. */
    val deletedAt: Instant? = null,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    val isVisible: Boolean get() = deletedAt == null
}

@Serializable
data class RestockEntry(
    val id: String,
    val medicationId: String,
    val amount: Double,
    val byPersonId: String,
    val at: Instant,
)

// ---------- Приёмы ----------

/** Что записано о конкретном плановом приёме. Запись неизменяемая; правка — новая версия с тем же ID. */
@Serializable
enum class IntakeAction { TAKEN, SKIPPED, SNOOZED, CANCELLED }

@Serializable
data class IntakeEvent(
    /** Детерминированный ID: один плановый приём — одна запись, даже если отметили двое одновременно. */
    val id: String,
    val medicationId: String,
    val personId: String,
    val scheduledAt: LocalDateTime,
    val action: IntakeAction,
    val byPersonId: String,
    val at: Instant,
    val snoozedUntil: LocalDateTime? = null,
    /** Сколько единиц списано с остатка. */
    val amount: Double = 0.0,
) {
    companion object {
        fun idFor(medicationId: String, scheduledAt: LocalDateTime) = "$medicationId@$scheduledAt"
    }
}

/** Состояние приёма для интерфейса. */
enum class IntakeStatus {
    /** Ожидается. */
    UPCOMING,
    /** Пора принять. */
    DUE,
    TAKEN,
    /** Отложено «Позже». */
    SNOOZED,
    /** Пропущено: время прошло, отметки нет, или пользователь пропустил сам. */
    SKIPPED,
    CANCELLED,
}

enum class DayPart { MORNING, DAY, EVENING;

    companion object {
        fun of(time: LocalTime): DayPart = when {
            time.hour < 12 -> MORNING
            time.hour < 18 -> DAY
            else -> EVENING
        }
    }
}

/** Плановый приём на конкретный день, собранный из расписания и записей. */
data class Intake(
    val medication: Medication,
    val person: Person,
    val scheduledAt: LocalDateTime,
    val status: IntakeStatus,
    val event: IntakeEvent?,
) {
    val id: String get() = IntakeEvent.idFor(medication.id, scheduledAt)
    /** Время, которое показываем: перенесённое, если было «Позже». */
    val effectiveAt: LocalDateTime get() = event?.snoozedUntil?.takeIf { status == IntakeStatus.SNOOZED } ?: scheduledAt
    val dayPart: DayPart get() = DayPart.of(scheduledAt.toLocalTime())
    val isDone: Boolean get() = status == IntakeStatus.TAKEN
    val isOpen: Boolean get() = status == IntakeStatus.UPCOMING || status == IntakeStatus.DUE || status == IntakeStatus.SNOOZED
}

// ---------- Сообщения ----------

@Serializable
enum class QuickMessage { GAVE_IT, ALREADY_TAKEN, PLEASE_REMIND, DONT_FORGET_EVENING, BUY_PACKAGE, MY_TURN_TODAY, THANKS }

@Serializable
data class FamilyMessage(
    val id: String,
    val fromId: String,
    /** null — всей семье. */
    val toId: String? = null,
    val quick: QuickMessage? = null,
    val text: String? = null,
    val medicationId: String? = null,
    val at: Instant,
    val readBy: Set<String> = emptySet(),
)

// ---------- Привычки (отдельный тип, не смешивается с лекарствами) ----------

@Serializable
data class Habit(
    val id: String,
    val personId: String,
    val title: String,
    val schedule: MedicationSchedule,
    val archived: Boolean = false,
    val createdAt: Instant,
    val updatedAt: Instant,
)

@Serializable
data class HabitCompletion(
    val id: String,
    val habitId: String,
    val date: LocalDate,
    val at: Instant,
)

// ---------- Настройки ----------

@Serializable
enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** Когда беспокоить второго взрослого. */
@Serializable
enum class FamilyNudgeScope { ALL, CHILDREN_ONLY, IMPORTANT_ONLY, NONE }

@Serializable
data class NotificationSettings(
    val sound: Boolean = true,
    val vibration: Boolean = true,
    val quietFrom: LocalTime? = LocalTime.of(22, 30),
    val quietTo: LocalTime? = LocalTime.of(7, 0),
    val familyScope: FamilyNudgeScope = FamilyNudgeScope.CHILDREN_ONLY,
    val restockReminders: Boolean = true,
    val courseEndReminders: Boolean = true,
    val dayCompleteScreen: Boolean = true,
)

@Serializable
data class UserSettings(
    val theme: ThemeMode = ThemeMode.SYSTEM,
    /** null — язык системы. */
    val language: String? = null,
    val notifications: NotificationSettings = NotificationSettings(),
)

/** Состояние синхронизации для индикатора в шапке. */
enum class SyncState { SYNCED, SYNCING, OFFLINE, ERROR, LOCAL_ONLY }
