package calebxzhou.rdi.master.ygg

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.util.*

@Serializable
data class GameProfile(
    @Contextual val id: String,
    val name: String,
    val properties: List<Property>
) {
}
@Serializable
data class Property(
    val name: String,
    val value: String
) {
}
@Serializable
data class MinecraftTexturesPayload(
    val timestamp: Long,

    val profileId: String,
    val profileName: String,
    val isPublic: Boolean,
    val textures: Map<MinecraftProfileTexture.Type, MinecraftProfileTexture>
)
@Serializable
data class MinecraftProfileTexture(
    val url: String,
    val metadata: Map<String, String> = emptyMap()
){
    enum class Type {
        SKIN,
        CAPE,
        ELYTRA
    }
}