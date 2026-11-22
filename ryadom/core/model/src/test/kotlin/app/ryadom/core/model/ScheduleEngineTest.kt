package app.ryadom.core.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class ScheduleEngineTest {
    private val t0 = Instant.parse("2026-11-01T00:00:00Z")
    private val kid = Person("p-b", "Богдан", PersonKind.CHILD, PersonAccent.SAND, FamilyRole.CHILD_PROFILE, createdAt = t0, updatedAt = t0)
    private val mom = Person("p-a", "Анна", PersonKind.ADULT, PersonAccent.CLAY, FamilyRole.OWNER, createdAt = t0, updatedAt = t0)
    private val family = FamilyGroup("f", "Семья", listOf(mom, kid), createdAt = t0, updatedAt = t0)
    private val day = LocalDate.of(2026, 11, 22) // суббота

    private fun med(
        times: List<LocalTime> = listOf(LocalTime.of(9, 0)),
        days: DaysRule = DaysRule.Daily,
        course: MedicationCourse = MedicationCourse(LocalDate.of(2026, 11, 1)),
        inventory: Inventory? = null,
        amount: Double = 2.0,
    ) = Medication(
        id = "m1", name = "Витамин D3", category = MedicationCategory.VITAMIN, personId = kid.id,
        dosage = Dosage(amount, DoseUnit.DROP), schedule = MedicationSchedule(times, days), course = course,
        inventory = inventory, createdAt = t0, updatedAt = t0,
    )

    @Test fun `daily medication has one occurrence per time`() {
        val m = med(times = listOf(LocalTime.of(21, 0), LocalTime.of(9, 0), LocalTime.of(9, 0)))
        assertThat(ScheduleEngine.occurrences(m, day)).containsExactly(day.atTime(9, 0), day.atTime(21, 0)).inOrder()
    }

    @Test fun `weekdays rule respects day of week`() {
        val m = med(days = DaysRule.Weekdays(setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY)))
        assertThat(ScheduleEngine.occurrences(m, day)).isEmpty()
        assertThat(ScheduleEngine.occurrences(m, day.minusDays(4))).hasSize(1) // 18 ноября — среда
    }

    @Test fun `interval rule counts from anchor`() {
        val m = med(days = DaysRule.Interval(3, LocalDate.of(2026, 11, 20)))
        assertThat(ScheduleEngine.isActiveOn(m, LocalDate.of(2026, 11, 20))).isTrue()
        assertThat(ScheduleEngine.isActiveOn(m, LocalDate.of(2026, 11, 22))).isFalse()
        assertThat(ScheduleEngine.isActiveOn(m, LocalDate.of(2026, 11, 23))).isTrue()
        assertThat(ScheduleEngine.isActiveOn(m, LocalDate.of(2026, 11, 17))).isFalse()
    }

    @Test fun `course bounds and archive stop occurrences`() {
        val m = med(course = MedicationCourse(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 21)))
        assertThat(ScheduleEngine.occurrences(m, day)).isEmpty()
        assertThat(ScheduleEngine.occurrences(med().copy(archived = true), day)).isEmpty()
        assertThat(ScheduleEngine.occurrences(med(), LocalDate.of(2026, 10, 31))).isEmpty()
    }

    @Test fun `status follows time when nothing recorded`() {
        val at = day.atTime(9, 0)
        assertThat(ScheduleEngine.status(at, null, at.minusMinutes(1))).isEqualTo(IntakeStatus.UPCOMING)
        assertThat(ScheduleEngine.status(at, null, at)).isEqualTo(IntakeStatus.DUE)
        assertThat(ScheduleEngine.status(at, null, at.plusHours(3))).isEqualTo(IntakeStatus.SKIPPED)
    }

    @Test fun `snooze shifts the due window without touching schedule`() {
        val at = day.atTime(9, 0)
        val ev = IntakeEvent(IntakeEvent.idFor("m1", at), "m1", kid.id, at, IntakeAction.SNOOZED, mom.id, t0, at.plusMinutes(15))
        assertThat(ScheduleEngine.status(at, ev, at.plusMinutes(10))).isEqualTo(IntakeStatus.SNOOZED)
        assertThat(ScheduleEngine.status(at, ev, at.plusMinutes(16))).isEqualTo(IntakeStatus.DUE)
        // плановое время приёма не изменилось
        val intake = ScheduleEngine.intakes(day, listOf(med()), family, mapOf(ev.id to ev), at.plusMinutes(10)).single()
        assertThat(intake.scheduledAt).isEqualTo(at)
        assertThat(intake.effectiveAt).isEqualTo(at.plusMinutes(15))
    }

    @Test fun `recorded actions win over time`() {
        val at = day.atTime(9, 0)
        fun ev(a: IntakeAction) = IntakeEvent(IntakeEvent.idFor("m1", at), "m1", kid.id, at, a, mom.id, t0)
        val late = at.plusHours(10)
        assertThat(ScheduleEngine.status(at, ev(IntakeAction.TAKEN), late)).isEqualTo(IntakeStatus.TAKEN)
        assertThat(ScheduleEngine.status(at, ev(IntakeAction.CANCELLED), late)).isEqualTo(IntakeStatus.CANCELLED)
    }

    @Test fun `event id is deterministic so two parents cannot create duplicates`() {
        val at = LocalDateTime.of(2026, 11, 22, 9, 0)
        assertThat(IntakeEvent.idFor("m1", at)).isEqualTo(IntakeEvent.idFor("m1", at))
    }

    @Test fun `days left uses dosage, times and days rule`() {
        val daily = med(times = listOf(LocalTime.of(9, 0), LocalTime.of(21, 0)), inventory = Inventory(remaining = 20.0), amount = 2.0)
        assertThat(ScheduleEngine.daysLeft(daily)).isEqualTo(5)
        assertThat(ScheduleEngine.isLowStock(daily)).isTrue()
        val every2 = med(days = DaysRule.Interval(2, day), inventory = Inventory(remaining = 20.0, warnDays = 3), amount = 1.0)
        assertThat(ScheduleEngine.daysLeft(every2)).isEqualTo(40)
        assertThat(ScheduleEngine.isLowStock(every2)).isFalse()
        assertThat(ScheduleEngine.daysLeft(med())).isNull()
    }

    @Test fun `course progress counts inclusive days`() {
        val m = med(course = MedicationCourse(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 12, 30)))
        assertThat(ScheduleEngine.courseProgress(m, day)).isEqualTo(22 to 60)
        assertThat(ScheduleEngine.courseEndsOn(m, LocalDate.of(2026, 12, 30))).isTrue()
        assertThat(ScheduleEngine.isCourseFinished(m, LocalDate.of(2026, 12, 31))).isTrue()
    }

    @Test fun `model survives json round trip`() {
        val json = kotlinx.serialization.json.Json
        val m = med(days = DaysRule.Weekdays(setOf(DayOfWeek.MONDAY)), inventory = Inventory(10.0, 30.0))
        val back = json.decodeFromString(Medication.serializer(), json.encodeToString(Medication.serializer(), m))
        assertThat(back).isEqualTo(m)
    }
}
