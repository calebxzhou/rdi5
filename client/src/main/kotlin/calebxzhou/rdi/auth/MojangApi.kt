package calebxzhou.rdi.auth

import calebxzhou.rdi.lgr
import calebxzhou.rdi.model.RAccount
import calebxzhou.rdi.net.body
import calebxzhou.rdi.net.httpStringRequest
import calebxzhou.rdi.net.success
import calebxzhou.rdi.util.decodeBase64
import calebxzhou.rdi.util.serdesJson
import com.google.gson.GsonBuilder
import com.mojang.authlib.minecraft.MinecraftProfileTexture
import com.mojang.authlib.yggdrasil.response.MinecraftTexturesPayload
import com.mojang.util.UUIDTypeAdapter

import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.*

object MojangApi {
    suspend fun getUuidFromName(name: String): String? {
        try {
            val resp = httpStringRequest(false, "https://api.mojang.com/users/profiles/minecraft/${name}")
            if (resp.success) {
                val body = resp.body
                lgr.info("查询结果: $body")
                return serdesJson.parseToJsonElement(body).jsonObject["id"]?.jsonPrimitive?.content
            } else {
                return null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    suspend fun getCloth(uuid: String): RAccount.Cloth? {
        try {
            val resp = httpStringRequest(false, "https://sessionserver.mojang.com/session/minecraft/profile/$uuid")
            if (resp.success) {
                val body = resp.body
                lgr.info(body)
                val texture = serdesJson.parseToJsonElement(body)
                    .jsonObject["properties"]
                    ?.jsonArray
                    ?.get(0)
                    ?.jsonObject
                    ?.get("value")
                    ?.jsonPrimitive
                    ?.content?.decodeBase64?:""
                val texturePayload = GsonBuilder().registerTypeAdapter(UUID::class.java,   UUIDTypeAdapter()).create().fromJson(texture, MinecraftTexturesPayload::class.java)
                texturePayload.textures[MinecraftProfileTexture.Type.SKIN]?.let { skin ->
                    val isSlim = skin.getMetadata("model") == "slim"
                    val cloth = RAccount.Cloth(isSlim, skin.url)
                    texturePayload.textures[MinecraftProfileTexture.Type.CAPE]?.let { cloth.cape = it.url }
                    return cloth
                }

            } else {
                return null
            }

        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
        return null
    }
}