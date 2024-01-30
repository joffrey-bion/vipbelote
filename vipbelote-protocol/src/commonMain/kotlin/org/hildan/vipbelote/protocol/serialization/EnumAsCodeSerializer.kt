package org.hildan.vipbelote.protocol.serialization

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlin.enums.*

internal open class EnumAsCodeSerializer<E : Enum<E>>(
    private val entries: EnumEntries<E>,
    private val getCode: E.() -> Int,
) : KSerializer<E> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("EnumCode", PrimitiveKind.INT)

    override fun serialize(encoder: Encoder, value: E) {
        encoder.encodeInt(value.getCode())
    }

    override fun deserialize(decoder: Decoder): E {
        val code = decoder.decodeInt()
        return entries.find { it.getCode() == code }
            ?: error("Unknown enum code $code - known values: $entries")
    }
}
