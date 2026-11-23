package app.ryadom.core.data

import androidx.test.core.app.ApplicationProvider
import app.ryadom.core.data.repository.LocalFamilyRepository
import app.ryadom.core.data.repository.LocalIntakeRepository
import app.ryadom.core.data.repository.LocalMedicationRepository
import app.ryadom.core.data.repository.LocalPlanRepository
import app.ryadom.core.data.repository.LocalSessionRepository
import app.ryadom.core.data.store.LocalStore
import app.ryadom.core.data.util.Clock
import app.ryadom.core.model.DaysRule
import app.ryadom.core.model.Dosage
import app.ryadom.core.model.DoseUnit
import app.ryadom.core.model.IntakeAction
import app.ryadom.core.model.IntakeStatus
import app.ryadom.core.model.Inventory
import app.ryadom.core.model.Medication
import app.ryadom.core.model.MedicationCategory
import app.ryadom.core.model.MedicationCourse
import app.ryadom.core.model.MedicationSchedule
import app.ryadom.core.model.PersonKind
import app.ryadom.core.model.PersonAccent
import app.ryadom.core.model.RyadomException
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset

/** Фиксированное время: 22 ноября 2026, 10:00 UTC. */
private class FixedClock(var at: LocalDateTime = LocalDateTime.of(2026, 11, 22, 10, 0)) : Clock {
    override fun instant(): Instant = at.toInstant(ZoneOffset.UTC)
    override fun zone(): ZoneId = ZoneOffset.UTC
    override fun minutes() = flowOf(at)
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RepositoriesTest {

    private fun env(scope: kotlinx.coroutines.CoroutineScope) = object {
        val clock = FixedClock()
        val store = LocalStore(ApplicationProvider.getApplicationContext(), scope)
        val session = LocalSessionRepository(store, clock)
        val family = LocalFamilyRepository(store, clock)
        val meds = LocalMedicationRepository(store, clock)
        val intakes = LocalIntakeRepository(store, clock)
        val plan = LocalPlanRepository(store, clock)
    }

    private fun med(personId: String, remaining: Double = 10.0) = Medication(
        id = "m1", name = "Витамин D3", category = MedicationCategory.VITAMIN, personId = personId,
        dosage = Dosage(2.0, DoseUnit.DROP), schedule = MedicationSchedule(listOf(LocalTime.of(9, 0)), DaysRule.Daily),
        course = MedicationCourse(LocalDate.of(2026, 11, 1)), inventory = Inventory(remaining, 60.0),
        createdAt = Instant.EPOCH, updatedAt = Instant.EPOCH,
    )

    @Test fun `marking taken writes off stock once and undo returns it`() = runTest {
        val e = env(backgroundScope)
        e.session.register("Анна", "anna@example.com", "password1")
        e.family.createFamily("Семья")
        val kid = e.family.addMember("Богдан", PersonKind.CHILD, PersonAccent.SAND, null)
        e.meds.save(med(kid.id))

        val intake = e.plan.day().first().intakes.single()
        assertThat(intake.status).isEqualTo(IntakeStatus.DUE)

        e.intakes.markTaken(intake)
        e.intakes.markTaken(intake) // второй родитель нажал одновременно
        assertThat(e.meds.medication("m1").first()!!.inventory!!.remaining).isEqualTo(8.0)
        assertThat(e.store.state.value.events).hasSize(1)

        val taken = e.plan.day().first().intakes.single()
        assertThat(taken.status).isEqualTo(IntakeStatus.TAKEN)
        e.intakes.undo(taken)
        assertThat(e.meds.medication("m1").first()!!.inventory!!.remaining).isEqualTo(10.0)
        assertThat(e.plan.day().first().intakes.single().status).isEqualTo(IntakeStatus.DUE)
    }

    @Test fun `snooze keeps the plan and moves the intake`() = runTest {
        val e = env(backgroundScope)
        e.session.register("Анна", "anna@example.com", "password1")
        e.family.createFamily("Семья")
        val me = e.family.me.first()!!
        e.meds.save(med(me.id))
        val intake = e.plan.day().first().intakes.single()
        e.intakes.snooze(intake, intake.scheduledAt.plusHours(2))
        val after = e.plan.day().first().intakes.single()
        assertThat(after.status).isEqualTo(IntakeStatus.SNOOZED)
        assertThat(after.scheduledAt.toLocalTime()).isEqualTo(LocalTime.of(9, 0))
        assertThat(after.effectiveAt.toLocalTime()).isEqualTo(LocalTime.of(11, 0))
        assertThat(e.store.state.value.events.values.single().action).isEqualTo(IntakeAction.SNOOZED)
    }

    @Test fun `deleting a medication keeps its history`() = runTest {
        val e = env(backgroundScope)
        e.session.register("Анна", "anna@example.com", "password1")
        e.family.createFamily("Семья")
        val me = e.family.me.first()!!
        e.meds.save(med(me.id))
        e.intakes.markTaken(e.plan.day().first().intakes.single())
        e.meds.delete("m1")

        assertThat(e.meds.medications(includeArchived = true).first()).isEmpty()
        assertThat(e.plan.day().first().intakes).isEmpty()
        val history = e.plan.range(LocalDate.of(2026, 11, 22), LocalDate.of(2026, 11, 22)).first().single()
        assertThat(history.taken).isEqualTo(1)
    }

    @Test fun `only managers can add members and owner cannot be removed`() = runTest {
        val e = env(backgroundScope)
        e.session.register("Анна", "anna@example.com", "password1")
        e.family.createFamily("Семья")
        val me = e.family.me.first()!!
        val err = runCatching { e.family.removeMember(me.id) }.exceptionOrNull() as RyadomException
        assertThat(err.reason).isEqualTo(RyadomException.Reason.LAST_OWNER)
        val code = e.family.createInvitation()
        assertThat(code).hasLength(6)
        assertThat(code).doesNotContainMatch("[01OI]")
    }

    @Test fun `registration validates input and never keeps the password`() = runTest {
        val e = env(backgroundScope)
        val bad = runCatching { e.session.register("Анна", "anna", "password1") }.exceptionOrNull() as RyadomException
        assertThat(bad.reason).isEqualTo(RyadomException.Reason.INVALID_EMAIL)
        val weak = runCatching { e.session.register("Анна", "a@b.kz", "123") }.exceptionOrNull() as RyadomException
        assertThat(weak.reason).isEqualTo(RyadomException.Reason.WEAK_PASSWORD)
        e.session.register("Анна", "A@B.kz", "password1")
        assertThat(e.store.state.value.toString()).doesNotContain("password1")
    }
}
