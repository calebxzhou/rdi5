package calebxzhou.rdi.serdes

import com.google.gson.Gson
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
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
val serdesJson = Json { serializersModule = module
    ignoreUnknownKeys = true
    isLenient = true // Allows parsing of malformed JSON
    coerceInputValues = true // Helps with default values and nulls
}
val serdesGson = Gson()
val Any.json
    get() = serdesGson.toJson(this)

inline fun <reified T : Any> KClass<T>.fromJson(json: String, gson: Gson = serdesGson): T {
    return gson.fromJson(json, this.java)
}