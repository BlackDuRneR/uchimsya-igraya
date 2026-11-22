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
)

@Serializable
data class Settings(
    val currency: String = "₸",
    val reminderEnabled: Boolean = true,
    val reminderHour: Int = 21,
    val milestoneNotifications: Boolean = true,
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
) {
    fun habit(id: String?) = habits.firstOrNull { it.id == id }
    fun checkIn(day: Long) = checkIns.firstOrNull { it.day == day }
    fun bestStreak(habitId: String, now: Long): Long {
        val current = habit(habitId)?.let { now - it.startMillis } ?: 0L
        val past = relapses.filter { it.habitId == habitId }.maxOfOrNull { it.streakMillis } ?: 0L
        return maxOf(current, past)
    }
}
