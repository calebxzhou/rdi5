package calebxzhou.rdi.model

import calebxzhou.rdi.util.serdesGson
import calebxzhou.rdi.util.toUUID
import com.google.common.hash.Hashing
import com.mojang.authlib.GameProfile
import com.mojang.authlib.minecraft.MinecraftProfileTexture
import com.mojang.authlib.properties.Property
import com.mojang.authlib.yggdrasil.response.MinecraftTexturesPayload
import io.ktor.util.*
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import net.minecraft.client.User
import net.minecraft.client.multiplayer.PlayerInfo
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
        val skinLocation: ResourceLocation
            get() = getTextureLocation(MinecraftProfileTexture.Type.SKIN, skinTexture.hashUC)
        val capeLocation: ResourceLocation?
            get() = capeTexture?.let { cap -> getTextureLocation(MinecraftProfileTexture.Type.CAPE, cap.hashUC) }

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

    }

    @Serializable
    data class Dto(
        @Contextual
        val id: ObjectId,
        val name: String,
        val cloth: Cloth
    ) {
         val mcProfile
            get() = GameProfile(id.toUUID(), name).apply {

            val texturesPayload = MinecraftTexturesPayload(
                this@Dto.id.timestamp.toLong(),
                id,
                name,
                true,
                cloth.mcTextures
            )

            properties.put("textures", Property("textures", serdesGson.toJson(texturesPayload).encodeBase64()))

        }
        val mcPlayerInfo
            get() = PlayerInfo(mcProfile,false)

    }
    companion object {
        val DEFAULT = RAccount(ObjectId(), "未登录", "123456", "12345", 0)
        val TESTS = listOf(
            RAccount(ObjectId("685aa7669acabdc07df8a730"), "测试1", "123123", "123123", 0),
            RAccount(ObjectId("685aa7669acabdc07df8a731"), "测试2", "456456", "456456", 0)
        )

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


        fun getTextureLocation(type: MinecraftProfileTexture.Type, hashUC: String): ResourceLocation {
            val prefix = when (type) {
                MinecraftProfileTexture.Type.SKIN -> "skins"
                MinecraftProfileTexture.Type.CAPE -> "capes"
                MinecraftProfileTexture.Type.ELYTRA -> "elytra"
                else -> throw IncompatibleClassChangeError()
            }
            return ResourceLocation.parse("$prefix/$hashUC")
        }
        val MinecraftProfileTexture.hashUC
            //mc就是这么做的
            get() = Hashing.sha1().hashUnencodedChars(hash).toString()
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
       // cloth.register()
        this.cloth = cloth
    }

}