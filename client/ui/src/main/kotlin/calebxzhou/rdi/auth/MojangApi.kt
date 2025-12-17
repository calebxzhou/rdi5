package calebxzhou.rdi.auth

import calebxzhou.rdi.lgr
import calebxzhou.rdi.model.RAccount
import calebxzhou.rdi.net.httpRequest
import calebxzhou.rdi.util.decodeBase64
import calebxzhou.rdi.util.serdesJson
import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.serialization.Serializable

object MojangApi {
    suspend fun getUuidFromName(name: String): String? {
        try {
            val resp = httpRequest{url( "https://api.mojang.com/users/profiles/minecraft/${name}")}
            data class IdName(val name: String,val id: String)
            val body = resp.body<IdName>()
            return body.id
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    suspend fun getCloth(uuid: String): RAccount.Cloth? {
        return try {
            val resp = httpRequest { url("https://sessionserver.mojang.com/session/minecraft/profile/$uuid") }
            val profile = resp.body<MojangProfileResponse>()
            val texturesValue = profile.properties.firstOrNull { it.name.equals("textures", ignoreCase = true) }?.value
                ?: return null

            val decodedTextures = try {
                texturesValue.decodeBase64
            } catch (ex: IllegalArgumentException) {
                throw IllegalStateException("Failed to decode textures payload", ex)
            }

            val payload = serdesJson.decodeFromString<MojangTexturesPayload>(decodedTextures)
            val textures = payload.textures
            val skin = textures["SKIN"] ?: return null

            val cloth = RAccount.Cloth(
                isSlim = skin.metadata?.model.equals("slim", ignoreCase = true),
                skin = skin.url,
            )
            textures["CAPE"]?.let { cloth.cape = it.url }
            cloth
        } catch (e: Exception) {
            lgr.warn( "Failed to fetch Mojang cloth for uuid=$uuid" )
            e.printStackTrace()
            null
        }
    }

    @Serializable
    private data class MojangProfileResponse(
        val id: String,
        val name: String,
        val properties: List<MojangProperty> = emptyList(),
    )

    @Serializable
    private data class MojangProperty(
        val name: String,
        val value: String,
        val signature: String? = null,
    )

    @Serializable
    private data class MojangTexturesPayload(
        val timestamp: Long? = null,
        val profileId: String? = null,
        val profileName: String? = null,
        val textures: Map<String, MojangTexture> = emptyMap(),
    )

    @Serializable
    private data class MojangTexture(
        val url: String,
        val metadata: MojangTextureMetadata? = null,
    )

    @Serializable
    private data class MojangTextureMetadata(
        val model: String? = null,
    )
}