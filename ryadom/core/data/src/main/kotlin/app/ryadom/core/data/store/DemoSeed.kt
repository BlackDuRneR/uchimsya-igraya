package app.ryadom.core.data.store

import app.ryadom.core.data.util.Clock
import app.ryadom.core.model.DaysRule
import app.ryadom.core.model.Dosage
import app.ryadom.core.model.DoseUnit
import app.ryadom.core.model.FamilyGroup
import app.ryadom.core.model.FamilyMessage
import app.ryadom.core.model.FamilyRole
import app.ryadom.core.model.IntakeAction
import app.ryadom.core.model.IntakeEvent
import app.ryadom.core.model.Inventory
import app.ryadom.core.model.Medication
import app.ryadom.core.model.MedicationCategory
import app.ryadom.core.model.MedicationCourse
import app.ryadom.core.model.MedicationSchedule
import app.ryadom.core.model.Person
import app.ryadom.core.model.PersonAccent
import app.ryadom.core.model.PersonKind
import app.ryadom.core.model.QuickMessage
import app.ryadom.core.model.ReminderSettings
import app.ryadom.core.model.ScheduleEngine
import app.ryadom.core.model.User
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime

/**
 * Пример семьи для экрана приветствия «Посмотреть пример».
 * Всё строится от текущего времени, поэтому пример всегда выглядит «живым».
 */
object DemoSeed {
    fun build(clock: Clock): StoreState {
        val now = clock.instant()
        val today = clock.today()
        val nowLocal = clock.now()

        val anna = Person("demo-anna", "Анна", PersonKind.ADULT, PersonAccent.CLAY, FamilyRole.OWNER,
            userId = "demo-user-anna", createdAt = now, updatedAt = now)
        val damir = Person("demo-damir", "Дамир", PersonKind.ADULT, PersonAccent.MOSS, FamilyRole.ADMIN,
            userId = "demo-user-damir", createdAt = now, updatedAt = now)
        val bogdan = Person("demo-bogdan", "Богдан", PersonKind.CHILD, PersonAccent.SAND, FamilyRole.CHILD_PROFILE,
            birthDate = today.minusYears(4).minusMonths(3), createdAt = now, updatedAt = now)
        val parents = setOf(anna.id, damir.id)

        fun med(
            id: String, name: String, cat: MedicationCategory, who: Person, amount: Double, unit: DoseUnit,
            times: List<LocalTime>, days: DaysRule = DaysRule.Daily, start: LocalDate = today.minusDays(21),
            end: LocalDate? = null, remaining: Double? = null, pack: Double? = null, important: Boolean = false,
            nudge: Int? = null, notes: String = "", caretakers: Set<String> = setOf(who.id).intersect(parents),
        ) = Medication(
            id = id, name = name, category = cat, personId = who.id, dosage = Dosage(amount, unit),
            schedule = MedicationSchedule(times, days), course = MedicationCourse(start, end),
            caretakerIds = caretakers, reminder = ReminderSettings(familyNudgeAfterMinutes = nudge),
            inventory = remaining?.let { Inventory(it, pack) }, notes = notes, important = important,
            createdAt = now, updatedAt = now,
        )

        val meds = listOf(
            med("demo-d3", "Витамин D3", MedicationCategory.VITAMIN, bogdan, 2.0, DoseUnit.DROP, listOf(LocalTime.of(9, 0)),
                start = today.minusDays(23), end = today.plusDays(36), remaining = 36.0, pack = 300.0, important = true,
                nudge = 30, notes = "После завтрака, в ложке воды", caretakers = parents),
            med("demo-omega-kids", "Омега детская", MedicationCategory.SUPPLEMENT, bogdan, 1.0, DoseUnit.CAPSULE,
                listOf(LocalTime.of(19, 0)), remaining = 5.0, pack = 60.0, caretakers = parents),
            med("demo-omega", "Омега-3", MedicationCategory.SUPPLEMENT, damir, 1.0, DoseUnit.CAPSULE,
                listOf(LocalTime.of(8, 0)), remaining = 48.0, pack = 90.0),
            med("demo-mg", "Магний B6", MedicationCategory.MINERAL, anna, 1.0, DoseUnit.TABLET,
                listOf(LocalTime.of(8, 0), LocalTime.of(21, 0)), end = today.plusDays(23), remaining = 40.0, pack = 60.0),
            med("demo-fe", "Железо", MedicationCategory.MINERAL, anna, 1.0, DoseUnit.TABLET, listOf(LocalTime.of(14, 0)),
                remaining = 25.0, pack = 30.0),
            med("demo-zn", "Цинк", MedicationCategory.MINERAL, damir, 1.0, DoseUnit.TABLET, listOf(LocalTime.of(13, 0)),
                days = DaysRule.Interval(2, today.minusDays(20)), remaining = 18.0),
            med("demo-c", "Витамин C", MedicationCategory.VITAMIN, anna, 1.0, DoseUnit.TABLET, listOf(LocalTime.of(10, 0)),
                start = today.minusDays(40), end = today.minusDays(11)).let { it.copy(course = it.course.copy(finishedAt = today.minusDays(11))) },
        )

        val family = FamilyGroup("demo-family", "Семья Сейткали", listOf(anna, damir, bogdan), createdAt = now, updatedAt = now)

        // История за две недели: почти всё принято, отметки делали оба родителя, пара пропусков и переносов.
        val events = mutableMapOf<String, IntakeEvent>()
        var seed = 7
        fun rnd(n: Int): Int { seed = (seed * 1103515245 + 12345) and 0x7fffffff; return seed % n }
        for (back in 14 downTo 0) {
            val day = today.minusDays(back.toLong())
            for (m in meds) for (at in ScheduleEngine.occurrences(m, day)) {
                if (back == 0 && !at.isBefore(nowLocal.minusMinutes(20))) continue
                // Утренний витамин Богдана сегодня оставляем неотмеченным: он попадёт в «Сейчас».
                if (back == 0 && m.id == "demo-d3") continue
                val roll = rnd(100)
                val who = if (m.personId == bogdan.id) (if (rnd(2) == 0) anna.id else damir.id) else m.personId
                val action = when {
                    roll < 4 -> IntakeAction.SKIPPED
                    else -> IntakeAction.TAKEN
                }
                val snoozed = if (roll in 4..9) at.plusMinutes(15) else null
                val id = IntakeEvent.idFor(m.id, at)
                val takenAt = (snoozed ?: at).plusMinutes(rnd(12).toLong()).atZone(clock.zone()).toInstant()
                events[id] = IntakeEvent(id, m.id, m.personId, at, action, who, takenAt, snoozed,
                    if (action == IntakeAction.TAKEN) m.dosage.amount else 0.0)
            }
        }

        val messages = listOf(
            FamilyMessage("demo-msg-1", anna.id, damir.id, QuickMessage.DONT_FORGET_EVENING, null, "demo-mg",
                now.minus(Duration.ofHours(20)), setOf(anna.id, damir.id)),
            FamilyMessage("demo-msg-2", damir.id, anna.id, null, "Купил новую упаковку омеги", "demo-omega",
                now.minus(Duration.ofMinutes(35)), setOf(damir.id)),
        )

        return StoreState(
            user = User("demo-user-anna", anna.name, "anna@example.com", now),
            meId = anna.id,
            family = family,
            medications = meds,
            events = events,
            messages = messages,
            isDemo = true,
        )
    }
}
