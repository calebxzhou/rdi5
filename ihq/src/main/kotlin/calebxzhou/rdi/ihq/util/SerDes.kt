package calebxzhou.rdi.ihq.util

import kotlinx.serialization.KSerializer
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.modules.SerializersModule
import net.benwoodworth.knbt.Nbt
import net.benwoodworth.knbt.NbtByte
import net.benwoodworth.knbt.NbtByteArray
import net.benwoodworth.knbt.NbtCompound
import net.benwoodworth.knbt.NbtDouble
import net.benwoodworth.knbt.NbtFloat
import net.benwoodworth.knbt.NbtInt
import net.benwoodworth.knbt.NbtIntArray
import net.benwoodworth.knbt.NbtList
import net.benwoodworth.knbt.NbtLong
import net.benwoodworth.knbt.NbtLongArray
import net.benwoodworth.knbt.NbtShort
import net.benwoodworth.knbt.NbtString
import net.benwoodworth.knbt.NbtTag
import org.bson.types.ObjectId
import java.util.*
import javax.swing.text.html.HTML.Attribute.N

val module = SerializersModule {
    contextual(ObjectId::class, ObjectIdSerializer)
    contextual(UUID::class, UUIDSerializer)
}

// Use the module with JSON configuration
val serdesJson = Json {
    serializersModule = module
    encodeDefaults = true
    explicitNulls = false
}
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

// Helper function to convert JsonElement to Map<String, Any>
fun JsonElement.toMap(): Map<String, Any> {
    return when (this) {
        is JsonObject -> this.mapValues { it.value.toAny() }
        else -> throw IllegalArgumentException("Expected JsonObject")
    }
}

// Helper function to convert JsonElement to Any
fun JsonElement.toAny(): Any {
    return when (this) {
        is JsonPrimitive -> {
            if (this.isString) this.content
            else if (this.content.toIntOrNull() != null) this.content.toInt()
            else if (this.content.toDoubleOrNull() != null) this.content.toDouble()
            else this.content
        }
        is JsonArray -> this.map { it.toAny() }
        is JsonObject -> this.toMap()
    }
}