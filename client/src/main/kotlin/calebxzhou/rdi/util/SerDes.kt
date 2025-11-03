package calebxzhou.rdi.util

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import net.minecraft.client.User
import org.bson.types.ObjectId
import java.util.*
import kotlin.reflect.KClass

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
val serdesGson = GsonBuilder()
    .setPrettyPrinting()
    .registerTypeAdapter(User::class.java, mcUserAdapter)
    .create()
val Any.gson
    get() = serdesGson.toJson(this)

inline val <reified T> T.json: String
    get() = serdesJson.encodeToString<T>(this)
inline fun <reified T : Any> KClass<T>.fromJson(json: String, gson: Gson = serdesGson): T {
    return gson.fromJson(json, this.java)
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