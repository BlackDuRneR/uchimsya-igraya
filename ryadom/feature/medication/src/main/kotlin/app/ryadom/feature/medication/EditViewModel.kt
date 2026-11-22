package app.ryadom.feature.medication

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import app.ryadom.core.data.repository.FamilyRepository
import app.ryadom.core.data.repository.MedicationRepository
import app.ryadom.core.data.util.Clock
import app.ryadom.core.data.util.newId
import app.ryadom.core.model.DaysRule
import app.ryadom.core.model.Dosage
import app.ryadom.core.model.DoseUnit
import app.ryadom.core.model.FamilyGroup
import app.ryadom.core.model.Inventory
import app.ryadom.core.model.Medication
import app.ryadom.core.model.MedicationCategory
import app.ryadom.core.model.MedicationCourse
import app.ryadom.core.model.MedicationSchedule
import app.ryadom.core.model.Person
import app.ryadom.core.model.PersonKind
import app.ryadom.core.model.ReminderSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import javax.inject.Inject

@Serializable data class MedicationEditRoute(val id: String? = null, val personId: String? = null)

enum class DaysMode { DAILY, WEEKDAYS, INTERVAL }
enum class EndMode { NONE, DATE, DAYS }
enum class CareMode { ME, PARTNER, BOTH, ANY }

data class Draft(
    val id: String = newId(),
    val isNew: Boolean = true,
    val name: String = "",
    val description: String = "",
    val category: MedicationCategory = MedicationCategory.VITAMIN,
    val personId: String? = null,
    val amount: Double = 1.0,
    val unit: DoseUnit = DoseUnit.TABLET,
    val customUnit: String = "",
    val daysMode: DaysMode = DaysMode.DAILY,
    val weekdays: Set<DayOfWeek> = DayOfWeek.entries.toSet(),
    val intervalDays: Int = 2,
    val times: List<LocalTime> = listOf(LocalTime.of(9, 0)),
    val start: LocalDate = LocalDate.now(),
    val endMode: EndMode = EndMode.NONE,
    val endDate: LocalDate = LocalDate.now().plusDays(29),
    val durationDays: Int = 30,
    val notifyOnEnd: Boolean = true,
    val care: CareMode = CareMode.BOTH,
    val remindersOn: Boolean = true,
    val advanceMinutes: Int = 0,
    val repeatAfter: Int? = 15,
    val nudgeAfter: Int? = null,
    val important: Boolean = false,
    val trackStock: Boolean = false,
    val remaining: String = "",
    val packageSize: String = "",
    val warnDays: Int = 7,
    val notes: String = "",
    val photoUri: String? = null,
    val original: Medication? = null,
)

data class EditState(
    val loading: Boolean = true,
    val draft: Draft = Draft(),
    val family: FamilyGroup? = null,
    val me: Person? = null,
    val nameError: Boolean = false,
    val timesError: Boolean = false,
    val saved: Boolean = false,
    val error: Throwable? = null,
)

@HiltViewModel
class MedicationEditViewModel @Inject constructor(
    handle: SavedStateHandle,
    private val meds: MedicationRepository,
    private val familyRepo: FamilyRepository,
    private val clock: Clock,
) : ViewModel() {
    private val route = handle.toRoute<MedicationEditRoute>()
    private val _state = MutableStateFlow(EditState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val family = familyRepo.family.filterNotNull().first()
            val me = familyRepo.me.first()
            val today = clock.today()
            val existing = route.id?.let { meds.medication(it).first() }
            val draft = existing?.toDraft(family, me) ?: Draft(
                personId = route.personId ?: me?.id,
                start = today, endDate = today.plusDays(29),
                care = if (family.adults.size > 1) CareMode.BOTH else CareMode.ME,
            )
            _state.value = EditState(loading = false, draft = draft, family = family, me = me)
        }
    }

    fun edit(block: (Draft) -> Draft) = _state.update { it.copy(draft = block(it.draft), nameError = false, timesError = false) }

    fun addTime(t: LocalTime) = edit { d -> d.copy(times = (d.times + t).distinct().sorted()) }
    fun removeTime(t: LocalTime) = edit { d -> d.copy(times = d.times - t) }

    fun save() {
        val s = _state.value
        val d = s.draft
        val family = s.family ?: return
        if (d.name.isBlank()) { _state.update { it.copy(nameError = true) }; return }
        if (d.times.isEmpty()) { _state.update { it.copy(timesError = true) }; return }
        val now = clock.instant()
        val partner = family.adults.firstOrNull { it.id != s.me?.id }
        val meId = s.me?.id
        val caretakers = when (d.care) {
            CareMode.ME -> setOfNotNull(meId)
            CareMode.PARTNER -> setOfNotNull(partner?.id)
            CareMode.BOTH -> family.adults.map { it.id }.toSet()
            CareMode.ANY -> emptySet()
        }
        val end = when (d.endMode) {
            EndMode.NONE -> null
            EndMode.DATE -> d.endDate.coerceAtLeast(d.start)
            EndMode.DAYS -> d.start.plusDays(d.durationDays.coerceAtLeast(1).toLong() - 1)
        }
        val base = d.original
        val med = Medication(
            id = d.id,
            name = d.name.trim(),
            category = d.category,
            personId = d.personId ?: meId ?: family.members.first().id,
            dosage = Dosage(d.amount, d.unit, d.customUnit.trim().takeIf { d.unit == DoseUnit.OTHER && it.isNotEmpty() }),
            schedule = MedicationSchedule(
                times = d.times.distinct().sorted(),
                days = when (d.daysMode) {
                    DaysMode.DAILY -> DaysRule.Daily
                    DaysMode.WEEKDAYS -> if (d.weekdays.isEmpty() || d.weekdays.size == 7) DaysRule.Daily else DaysRule.Weekdays(d.weekdays)
                    DaysMode.INTERVAL -> DaysRule.Interval(d.intervalDays.coerceAtLeast(2), d.start)
                },
            ),
            course = MedicationCourse(d.start, end, d.notifyOnEnd, base?.course?.finishedAt?.takeIf { end == base.course.end }),
            caretakerIds = caretakers,
            reminder = ReminderSettings(d.remindersOn, d.advanceMinutes, d.repeatAfter, d.nudgeAfter),
            inventory = if (d.trackStock) Inventory(
                remaining = d.remaining.replace(',', '.').toDoubleOrNull() ?: 0.0,
                packageSize = d.packageSize.replace(',', '.').toDoubleOrNull(),
                warnDays = d.warnDays,
            ) else null,
            description = d.description.trim(),
            notes = d.notes.trim(),
            photoUri = d.photoUri,
            important = d.important,
            archived = base?.archived ?: false,
            createdAt = base?.createdAt ?: now,
            updatedAt = now,
        )
        viewModelScope.launch {
            runCatching { meds.save(med) }
                .onSuccess { _state.update { it.copy(saved = true) } }
                .onFailure { e -> _state.update { it.copy(error = e) } }
        }
    }
}

private fun Medication.toDraft(family: FamilyGroup, me: Person?): Draft {
    val adults = family.adults.map { it.id }.toSet()
    val care = when {
        caretakerIds.isEmpty() -> CareMode.ANY
        caretakerIds == adults && adults.size > 1 -> CareMode.BOTH
        me != null && caretakerIds == setOf(me.id) -> CareMode.ME
        else -> CareMode.PARTNER
    }
    val end = course.end
    return Draft(
        id = id, isNew = false, name = name, description = description, category = category, personId = personId,
        amount = dosage.amount, unit = dosage.unit, customUnit = dosage.customUnit.orEmpty(),
        daysMode = when (schedule.days) { DaysRule.Daily -> DaysMode.DAILY; is DaysRule.Weekdays -> DaysMode.WEEKDAYS; is DaysRule.Interval -> DaysMode.INTERVAL },
        weekdays = (schedule.days as? DaysRule.Weekdays)?.days ?: DayOfWeek.entries.toSet(),
        intervalDays = (schedule.days as? DaysRule.Interval)?.everyDays ?: 2,
        times = schedule.times,
        start = course.start,
        endMode = if (end == null) EndMode.NONE else EndMode.DATE,
        endDate = end ?: course.start.plusDays(29),
        durationDays = end?.let { ChronoUnit.DAYS.between(course.start, it).toInt() + 1 } ?: 30,
        notifyOnEnd = course.notifyOnEnd,
        care = care,
        remindersOn = reminder.enabled, advanceMinutes = reminder.advanceMinutes, repeatAfter = reminder.repeatAfterMinutes,
        nudgeAfter = reminder.familyNudgeAfterMinutes, important = important,
        trackStock = inventory != null,
        remaining = inventory?.remaining?.let { if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() } ?: "",
        packageSize = inventory?.packageSize?.let { if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() } ?: "",
        warnDays = inventory?.warnDays ?: 7,
        notes = notes, photoUri = photoUri, original = this,
    )
}

internal fun Person.isChild() = kind == PersonKind.CHILD
