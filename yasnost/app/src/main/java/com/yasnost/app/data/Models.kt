package com.yasnost.app.data

import kotlinx.serialization.Serializable

@Serializable
enum class HabitType { ALCOHOL, SMOKING, CUSTOM }

@Serializable
data class Habit(
    val id: String,
    val type: HabitType,
    val title: String,
    val startMillis: Long,
    /** Сколько денег уходило в день. */
    val costPerDay: Double = 0.0,
    /** Сколько единиц (литров, сигарет…) уходило в день. */
    val unitsPerDay: Double = 0.0,
    val unitName: String = "",
    val createdMillis: Long = startMillis,
)

@Serializable
data class Relapse(
    val habitId: String,
    val atMillis: Long,
    val streakMillis: Long,
    val note: String = "",
    val triggers: List<String> = emptyList(),
)

@Serializable
data class CheckIn(
    /** LocalDate.toEpochDay() */
    val day: Long,
    /** 1..5 */
    val mood: Int = 3,
    /** 0..10 */
    val craving: Int = 0,
    /** id привычек, по которым день прошёл чисто */
    val cleanHabits: List<String> = emptyList(),
    val note: String = "",
    /** Что вызывало тягу сегодня */
    val triggers: List<String> = emptyList(),
)

@Serializable
enum class SexStatus { NONE, PROTECTED, UNPROTECTED }

@Serializable
data class IntimacyEntry(
    /** LocalDate.toEpochDay() */
    val day: Long,
    val sex: SexStatus = SexStatus.NONE,
    /** Сколько раз был контакт за день (если был) */
    val sexCount: Int = 1,
    val masturbation: Boolean = false,
    /** Желание / либидо 0..5 */
    val desire: Int = 3,
    /** Был ли алкоголь в этот день перед близостью */
    val alcohol: Boolean = false,
    val note: String = "",
)

@Serializable
data class Goal(
    val id: String,
    val title: String,
    val price: Double,
    val createdMillis: Long,
)

@Serializable
data class Settings(
    val currency: String = "₸",
    val reminderEnabled: Boolean = true,
    val reminderHour: Int = 21,
    val milestoneNotifications: Boolean = true,
    /** Показывать раздел «Близость» */
    val intimacyEnabled: Boolean = true,
    /** Требовать отпечаток / PIN для раздела «Близость» */
    val intimacyLock: Boolean = false,
    /** Запретить скриншоты и превью в списке приложений */
    val secureScreen: Boolean = false,
)

@Serializable
data class AppState(
    val habits: List<Habit> = emptyList(),
    val relapses: List<Relapse> = emptyList(),
    val checkIns: List<CheckIn> = emptyList(),
    val reasons: List<String> = emptyList(),
    val settings: Settings = Settings(),
    /** habitId -> сколько часов последней вехи, о которой уже уведомили */
    val notified: Map<String, Long> = emptyMap(),
    /** Сколько раз удалось перебороть тягу через SOS */
    val cravingsBeaten: Int = 0,
    val lastReminderDay: Long = -1,
    val intimacy: List<IntimacyEntry> = emptyList(),
    val goals: List<Goal> = emptyList(),
    /** Письмо себе — показывается на экране SOS */
    val letter: String = "",
    /** Дата последнего обследования на ИППП (epochDay) */
    val lastStiTestDay: Long = -1,
    /** id полученных общих достижений (для уведомлений) */
    val unlocked: List<String> = emptyList(),
    val unlockedInit: Boolean = false,
) {
    fun intimacyOn(day: Long) = intimacy.firstOrNull { it.day == day }
    fun habit(id: String?) = habits.firstOrNull { it.id == id }
    fun checkIn(day: Long) = checkIns.firstOrNull { it.day == day }
    fun bestStreak(habitId: String, now: Long): Long {
        val current = habit(habitId)?.let { now - it.startMillis } ?: 0L
        val past = relapses.filter { it.habitId == habitId }.maxOfOrNull { it.streakMillis } ?: 0L
        return maxOf(current, past)
    }
}
