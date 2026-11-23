package app.ryadom.core.data.util

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/** Источник времени. Подменяется в тестах; часовой пояс читается каждый раз, поэтому смена пояса подхватывается сразу. */
interface Clock {
    fun instant(): Instant
    fun zone(): ZoneId
    fun now(): LocalDateTime = LocalDateTime.ofInstant(instant(), zone())
    fun today(): LocalDate = now().toLocalDate()

    /** Тик раз в минуту, выровненный по началу минуты: статусы «пора» и «пропущено» обновляются сами. */
    fun minutes(): Flow<LocalDateTime> = flow {
        while (true) {
            val now = now()
            emit(now)
            val msToNextMinute = 60_000L - (now.second * 1000L + now.nano / 1_000_000L)
            delay(msToNextMinute.coerceAtLeast(1_000L))
        }
    }
}

@Singleton
class SystemClock @Inject constructor() : Clock {
    override fun instant(): Instant = Instant.now()
    override fun zone(): ZoneId = ZoneId.systemDefault()
}

fun newId(): String = UUID.randomUUID().toString()
