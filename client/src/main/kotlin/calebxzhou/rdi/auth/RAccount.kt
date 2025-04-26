package calebxzhou.rdi.auth

import calebxzhou.rdi.serdes.serdesGson
import calebxzhou.rdi.util.mc
import calebxzhou.rdi.util.toUUID
import com.mojang.authlib.GameProfile
import com.mojang.authlib.minecraft.MinecraftProfileTexture
import com.mojang.authlib.properties.Property
import com.mojang.authlib.yggdrasil.response.MinecraftTexturesPayload
import io.ktor.util.*
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import net.minecraft.client.User
import net.minecraft.client.renderer.entity.ShulkerRenderer.getTextureLocation
import net.minecraft.resources.ResourceLocation
import org.bson.types.ObjectId
import java.util.*

@Serializable
data class RAccount(
    @Contextual
    val _id: ObjectId,
    var name: String,
    var pwd: String,
    var qq: String,
    val score: Int = 0,
    var cloth: Cloth = Cloth(),
) {
    @Serializable
    data class Cloth(
        var isSlim: Boolean = true,
        var skin: String = "https://littleskin.cn/textures/526fe866ed25a7ee1cf894b81a2199aaa03f139803623a25a793f6ae57e22f02",
        var cape: String? = null
    ) {
        val skinTexture
            get() = MinecraftProfileTexture(
                skin,
                mapOf("model" to if (isSlim) "slim" else "normal")
            )
        val capeTexture
            get() = cape?.let { MinecraftProfileTexture(it, mapOf()) }
        val mcTextures: Map<MinecraftProfileTexture.Type, MinecraftProfileTexture>
            get() {
                val textureMap = mutableMapOf(
                    MinecraftProfileTexture.Type.SKIN to skinTexture
                )
                capeTexture?.let { cap ->
                    textureMap += MinecraftProfileTexture.Type.CAPE to cap
                }
                return textureMap
            }
        val skinLocation: ResourceLocation
            get() = getTextureLocation(MinecraftProfileTexture.Type.SKIN, skinTexture.hashUC)
        val capeLocation: ResourceLocation?
            get() = capeTexture?.let { cap -> getTextureLocation(MinecraftProfileTexture.Type.CAPE, cap.hashUC) }
        fun register() {
            mc.skinManager.registerTexture(skinTexture, MinecraftProfileTexture.Type.SKIN)
            capeTexture?.let {
                mc.skinManager.registerTexture(it, MinecraftProfileTexture.Type.CAPE)
            }
        }
    }

    @Serializable
    data class Dto(
        @Contextual
        val id: ObjectId,
        val name: String,
        val cloth: Cloth
    ) {
        @Transient
        val mcProfile = GameProfile(id.toUUID(), name).apply {

            val texturesPayload = MinecraftTexturesPayload(
                this@Dto.id.timestamp.toLong(),
                id,
                name,
                true,
                cloth.mcTextures
            )

            properties.put("textures", Property("textures", serdesGson.toJson(texturesPayload).encodeBase64()))

        }

    }
    companion object {
        val DEFAULT = RAccount(ObjectId(), "未登录", "123456", "12345", 0)

        @JvmStatic
        var now: RAccount? = null

        @JvmStatic
        val mcUserNow
            get() = now?.mcUser ?: User(
                "rdi",
                UUID.randomUUID(),
                "",
                Optional.empty(),
                Optional.empty(),
                User.Type.MOJANG
            )




        /*@JvmStatic
        fun processSkullProfile(
            profileCache: GameProfileCache,
            profile: GameProfile?,
            profileConsumer: Consumer<GameProfile?>
        ) {
            if(profile != null && !profile.name.isNullOrBlank() && (!profile.isComplete || !profile.properties.containsKey("textures")) && profileCache != null){
                profileCache.getAsync(profile.name) {  }
            }
        }*/

    }

    @Contextual
    val uuid = _id.toUUID()
    val dto = Dto(_id, name, cloth)
    val mcUser
        get() = User(name, uuid, "", Optional.empty(), Optional.empty(), User.Type.MOJANG)

    fun logout() {
        now = null
    }

    fun updateCloth(cloth: Cloth) {
        cloth.register()
        this.cloth = cloth
    }

}
