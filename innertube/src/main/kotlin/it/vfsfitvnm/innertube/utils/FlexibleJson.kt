package it.vfsfitvnm.innertube.utils

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.longOrNull

internal object FlexibleLongSerializer : KSerializer<Long> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("FlexibleLong", PrimitiveKind.LONG)

    override fun deserialize(decoder: Decoder): Long {
        val jsonDecoder = decoder as? JsonDecoder
            ?: return decoder.decodeLong()

        return when (val element = jsonDecoder.decodeJsonElement()) {
            is JsonNull -> error("Expected a long, got null")
            is JsonPrimitive -> element.longOrNull
                ?: element.content.toLongOrNull()
                ?: error("Invalid long: $element")
            else -> error("Invalid long: $element")
        }
    }

    override fun serialize(encoder: Encoder, value: Long) {
        if (encoder is JsonEncoder) {
            encoder.encodeJsonElement(JsonPrimitive(value))
        } else {
            encoder.encodeLong(value)
        }
    }
}

internal object FlexibleIntSerializer : KSerializer<Int> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("FlexibleInt", PrimitiveKind.INT)

    override fun deserialize(decoder: Decoder): Int {
        val jsonDecoder = decoder as? JsonDecoder
            ?: return decoder.decodeInt()

        return when (val element = jsonDecoder.decodeJsonElement()) {
            is JsonNull -> error("Expected an int, got null")
            is JsonPrimitive -> element.intOrNull
                ?: element.content.toIntOrNull()
                ?: error("Invalid int: $element")
            else -> error("Invalid int: $element")
        }
    }

    override fun serialize(encoder: Encoder, value: Int) {
        if (encoder is JsonEncoder) {
            encoder.encodeJsonElement(JsonPrimitive(value))
        } else {
            encoder.encodeInt(value)
        }
    }
}
