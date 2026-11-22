package com.yasnost.app.data

import kotlinx.serialization.Serializable

const val MAIN_PARTNER = "main"
/** Ключ владельца цикла для партнёрши: основная хранит старые данные под "" */
fun ownerKey(partnerId: String) = if (partnerId == MAIN_PARTNER) "" else partnerId

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
    /** Сколько бутылок безалкогольного пива */
    val naBeer: Int = 0,
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
    /** С кем (id партнёров) */
    val partnerIds: List<String> = emptyList(),
    /** Виды: Вагинальный, Оральный, Анальный */
    val kinds: List<String> = emptyList(),
    /** Сколько раз мастурбация за день */
    val mastCount: Int = 1,
) {
    /** Был ли вагинальный контакт (важно для риска беременности). Старые записи без видов считаем вагинальными. */
    val vaginal get() = kinds.isEmpty() || "Вагинальный" in kinds
}

@Serializable
data class Partner(val id: String, val name: String, val createdMillis: Long = 0)

@Serializable
enum class Gender { MALE, FEMALE }

@Serializable
data class Profile(
    val name: String = "",
    val gender: Gender = Gender.MALE,
    val partnerName: String = "",
    val setupDone: Boolean = false,
) {
    val female get() = gender == Gender.FEMALE
}

@Serializable
enum class Flow { SPOTTING, LIGHT, MEDIUM, HEAVY }

@Serializable
data class CycleDay(
    val day: Long,
    /** Чей цикл: "" — свой (для девушки) или основной партнёрши (для парня), иначе id партнёрши */
    val owner: String = "",
    /** null — кровотечения нет */
    val flow: Flow? = null,
    /** Боль 0..10 */
    val pain: Int = 0,
    val symptoms: List<String> = emptyList(),
    /** 0 — не отмечено, 1..5 */
    val mood: Int = 0,
    val meds: List<String> = emptyList(),
    val note: String = "",
    /** Тест на овуляцию: 0 — не делали, 1 — отрицательный, 2 — положительный */
    val ovulationTest: Int = 0,
    val weight: Double? = null,
    /** Базальная температура, °C */
    val temp: Double? = null,
    /** Стаканы воды */
    val water: Int = 0,
)

@Serializable
data class CycleSettings(
    val enabled: Boolean = true,
    val cycleLength: Int = 28,
    val periodLength: Int = 5,
    val remindDaysBefore: Int = 2,
    val fertileReminder: Boolean = false,
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
    /** Для девушек: советы о паре и мягкие напоминания */
    val partnerTips: Boolean = true,
    /** Модули: что человек вообще отслеживает */
    val trackAlcohol: Boolean = true,
    val trackSmoking: Boolean = true,
    val trackOther: Boolean = true,
    /** Выбор модулей уже сделан (для старых пользователей покажем один раз) */
    val modulesChosen: Boolean = false,
    /** Размер плитки ярлыка в dp (0 — авто по ячейке лаунчера) */
    val iconSize: Int = 0,
    /** Сдвиг ярлыка по вертикали в dp (минус — выше) */
    val iconOffset: Int = 0,
) {
    fun tracks(t: HabitType) = when (t) {
        HabitType.ALCOHOL -> trackAlcohol
        HabitType.SMOKING -> trackSmoking
        HabitType.CUSTOM -> trackOther
    }
    val anyHabits get() = trackAlcohol || trackSmoking || trackOther
}

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
    val profile: Profile = Profile(),
    val cycle: List<CycleDay> = emptyList(),
    val cycleSettings: CycleSettings = CycleSettings(),
    /** Ключи уже отправленных уведомлений о цикле */
    val cycleNotified: List<String> = emptyList(),
    /** Начало периода, который закрыт кнопкой «Закончились» */
    val periodClosedStart: Long = -1,
    val partners: List<Partner> = emptyList(),
    /** Для парня: чей цикл сейчас открыт (ключ владельца) */
    val cycleOwner: String = "",
    /** Закрытые кнопкой «Закончились» периоды по владельцам */
    val periodClosed: Map<String, Long> = emptyMap(),
) {
    fun cycleOn(day: Long, owner: String = currentOwner()) = cycle.firstOrNull { it.day == day && it.owner == owner }
    /** Чей цикл показываем сейчас */
    fun currentOwner() = if (profile.female) "" else cycleOwner
    fun closedStart(owner: String) = periodClosed[owner] ?: if (owner == "") periodClosedStart else -1
    fun partner(id: String) = partners.firstOrNull { it.id == id }
    /** Партнёрша, чей цикл открыт (для парня) */
    fun cyclePartner() = partners.firstOrNull { ownerKey(it.id) == cycleOwner }
    fun mainPartnerName() = partners.firstOrNull()?.name ?: profile.partnerName
    /** Старые данные: имя из профиля превращаем в первого партнёра. */
    fun migrated() = if (partners.isEmpty() && profile.partnerName.isNotBlank()) copy(partners = listOf(Partner(MAIN_PARTNER, profile.partnerName))) else this
    fun intimacyOn(day: Long) = intimacy.firstOrNull { it.day == day }
    fun habit(id: String?) = habits.firstOrNull { it.id == id }
    fun checkIn(day: Long) = checkIns.firstOrNull { it.day == day }
    fun bestStreak(habitId: String, now: Long): Long {
        val current = habit(habitId)?.let { now - it.startMillis } ?: 0L
        val past = relapses.filter { it.habitId == habitId }.maxOfOrNull { it.streakMillis } ?: 0L
        return maxOf(current, past)
    }
}
