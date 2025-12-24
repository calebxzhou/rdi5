package calebxzhou.rdi.common

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import org.bson.types.ObjectId
import java.util.UUID

/**
 * calebxzhou @ 2024-06-18 14:25
 */
val module = SerializersModule {
    contextual(ObjectId::class, ObjectIdSerializer)
    contextual(UUID::class, UUIDSerializer)
}

// Use the module with JSON configuration
val serdesJson = Json {
    serializersModule = module
    ignoreUnknownKeys = true
    isLenient = true // Allows parsing of malformed JSON
    coerceInputValues = true // Helps with default values and nulls
}


inline val <reified T> T.json: String
    get() = serdesJson.encodeToString<T>(this)
object ObjectIdSerializer : KSerializer<ObjectId> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("ObjectId", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: ObjectId) {
        encoder.encodeString(value.toHexString())
    }

    override fun deserialize(decoder: Decoder): ObjectId {
        return ObjectId(decoder.decodeString())
    }
}

object UUIDSerializer : KSerializer<UUID> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("UUID", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: UUID) {
        encoder.encodeString(value.toString())
    }

    override fun deserialize(decoder: Decoder): UUID {
        return UUID.fromString(decoder.decodeString())
    }
}