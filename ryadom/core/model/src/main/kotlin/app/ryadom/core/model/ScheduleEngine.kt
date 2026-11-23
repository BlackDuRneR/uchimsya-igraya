package app.ryadom.core.model

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import kotlin.math.floor

/**
 * Чистая логика расписания: какие приёмы положены на день и в каком они состоянии.
 * Без Android и без хранилища, поэтому покрыта unit-тестами.
 */
object ScheduleEngine {

    /** Сколько минут после планового времени приём считается «пора», а не «пропущено». */
    const val DUE_WINDOW_MINUTES = 180L

    fun isActiveOn(med: Medication, date: LocalDate): Boolean {
        if (med.archived || med.deletedAt != null) return false
        val c = med.course
        if (date.isBefore(c.start)) return false
        if (c.end != null && date.isAfter(c.end)) return false
        if (c.finishedAt != null && date.isAfter(c.finishedAt)) return false
        return when (val d = med.schedule.days) {
            DaysRule.Daily -> true
            is DaysRule.Weekdays -> date.dayOfWeek in d.days
            is DaysRule.Interval -> {
                val diff = ChronoUnit.DAYS.between(d.anchor, date)
                diff >= 0 && diff % d.everyDays.coerceAtLeast(1) == 0L
            }
        }
    }

    fun occurrences(med: Medication, date: LocalDate): List<LocalDateTime> =
        if (!isActiveOn(med, date)) emptyList()
        else med.schedule.times.distinct().sorted().map { date.atTime(it) }

    fun status(scheduledAt: LocalDateTime, event: IntakeEvent?, now: LocalDateTime): IntakeStatus {
        when (event?.action) {
            IntakeAction.TAKEN -> return IntakeStatus.TAKEN
            IntakeAction.SKIPPED -> return IntakeStatus.SKIPPED
            IntakeAction.CANCELLED -> return IntakeStatus.CANCELLED
            IntakeAction.SNOOZED -> {
                val until = event.snoozedUntil ?: scheduledAt
                return when {
                    now.isBefore(until) -> IntakeStatus.SNOOZED
                    now.isBefore(until.plusMinutes(DUE_WINDOW_MINUTES)) -> IntakeStatus.DUE
                    else -> IntakeStatus.SKIPPED
                }
            }
            null -> Unit
        }
        return when {
            now.isBefore(scheduledAt) -> IntakeStatus.UPCOMING
            now.isBefore(scheduledAt.plusMinutes(DUE_WINDOW_MINUTES)) -> IntakeStatus.DUE
            else -> IntakeStatus.SKIPPED
        }
    }

    /** Все плановые приёмы семьи на день, отсортированные по времени. */
    fun intakes(
        date: LocalDate,
        medications: List<Medication>,
        family: FamilyGroup,
        events: Map<String, IntakeEvent>,
        now: LocalDateTime,
    ): List<Intake> = medications.flatMap { med ->
        val person = family.person(med.personId) ?: return@flatMap emptyList()
        occurrences(med, date).map { at ->
            val ev = events[IntakeEvent.idFor(med.id, at)]
            Intake(med, person, at, status(at, ev, now), ev)
        }
    }.sortedWith(compareBy({ it.scheduledAt }, { it.person.name }, { it.medication.name }))

    /** Сколько единиц уходит в среднем за день (для прогноза остатка). */
    fun dailyConsumption(med: Medication): Double {
        val perDay = med.dosage.amount * med.schedule.times.distinct().size
        val share = when (val d = med.schedule.days) {
            DaysRule.Daily -> 1.0
            is DaysRule.Weekdays -> d.days.size / 7.0
            is DaysRule.Interval -> 1.0 / d.everyDays.coerceAtLeast(1)
        }
        return perDay * share
    }

    /** Примерно на сколько дней хватит остатка; null — остаток не ведётся. */
    fun daysLeft(med: Medication): Int? {
        val inv = med.inventory ?: return null
        val perDay = dailyConsumption(med)
        if (perDay <= 0.0) return null
        return floor(inv.remaining / perDay).toInt().coerceAtLeast(0)
    }

    fun isLowStock(med: Medication): Boolean {
        val left = daysLeft(med) ?: return false
        return left <= (med.inventory?.warnDays ?: 0)
    }

    /** День курса и длина курса; null — курс без даты окончания. */
    fun courseProgress(med: Medication, today: LocalDate): Pair<Int, Int>? {
        val end = med.course.end ?: return null
        val total = ChronoUnit.DAYS.between(med.course.start, end).toInt() + 1
        val day = (ChronoUnit.DAYS.between(med.course.start, today).toInt() + 1).coerceIn(0, total)
        return day to total
    }

    fun isCourseFinished(med: Medication, today: LocalDate): Boolean =
        med.course.finishedAt != null || (med.course.end != null && today.isAfter(med.course.end))

    /** Курс заканчивается сегодня — повод спросить «Завершить курс?». */
    fun courseEndsOn(med: Medication, date: LocalDate): Boolean =
        med.course.finishedAt == null && med.course.end == date
}
