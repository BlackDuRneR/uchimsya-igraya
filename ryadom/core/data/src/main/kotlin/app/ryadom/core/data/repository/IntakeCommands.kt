package app.ryadom.core.data.repository

import app.ryadom.core.model.Intake
import app.ryadom.core.model.QuickMessage
import java.time.LocalDateTime
import javax.inject.Inject

/** Действия с приёмом, общие для всех экранов (и, на этапе 4, для кнопок в уведомлении). */
class IntakeCommands @Inject constructor(
    private val intakes: IntakeRepository,
    private val messages: MessageRepository,
) {
    suspend fun taken(intake: Intake) = intakes.markTaken(intake)
    suspend fun undo(intake: Intake) = intakes.undo(intake)
    suspend fun snooze(intake: Intake, until: LocalDateTime) = intakes.snooze(intake, until)
    suspend fun skip(intake: Intake) = intakes.skip(intake)
    suspend fun cancel(intake: Intake) = intakes.cancelForToday(intake)
    suspend fun message(intake: Intake, quick: QuickMessage, toId: String?) =
        messages.send(quick, null, toId, intake.medication.id)

    /** Круг в строке: не отмечено → принято, принято → снять отметку. */
    suspend fun toggle(intake: Intake) = if (intake.isDone) undo(intake) else taken(intake)
}
