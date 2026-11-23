package app.ryadom.core.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/** java.time ↔ ISO-строки для хранения и синхронизации. */
abstract class IsoSerializer<T : Any>(name: String, private val parse: (String) -> T) : KSerializer<T> {
    override val descriptor = PrimitiveSerialDescriptor("ryadom.$name", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: T) = encoder.encodeString(value.toString())
    override fun deserialize(decoder: Decoder): T = parse(decoder.decodeString())
}

object LocalDateSerializer : IsoSerializer<LocalDate>("LocalDate", LocalDate::parse)
object LocalTimeSerializer : IsoSerializer<LocalTime>("LocalTime", LocalTime::parse)
object LocalDateTimeSerializer : IsoSerializer<LocalDateTime>("LocalDateTime", LocalDateTime::parse)
object InstantSerializer : IsoSerializer<Instant>("Instant", Instant::parse)
