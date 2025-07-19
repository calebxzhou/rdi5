package calebxzhou.rdi.auth

import calebxzhou.rdi.lgr
import calebxzhou.rdi.util.serdesGson
import calebxzhou.rdi.util.toObjectId
import com.google.gson.JsonParseException
import com.mojang.authlib.GameProfile
import com.mojang.authlib.SignatureState
import com.mojang.authlib.minecraft.MinecraftProfileTexture
import com.mojang.authlib.minecraft.MinecraftProfileTextures
import com.mojang.authlib.minecraft.MinecraftSessionService
import com.mojang.authlib.properties.Property
import com.mojang.authlib.yggdrasil.ProfileResult
import com.mojang.authlib.yggdrasil.response.MinecraftTexturesPayload
import io.ktor.util.decodeBase64String
import java.net.InetAddress
import java.util.*

class RSessionService : MinecraftSessionService {
    override fun joinServer(profileId: UUID?, authenticationToken: String?, serverId: String?) {}

    override fun hasJoinedServer(
        profileName: String?,
        serverId: String?,
        address: InetAddress?
    ) = null

    override fun getPackedTextures(profile: GameProfile) = profile.properties.get("textures").firstOrNull()

    override fun unpackTextures(packedTextures: Property): MinecraftProfileTextures {
        val value = packedTextures.value()

        val result: MinecraftTexturesPayload?
        try {
            val json = value.decodeBase64String()
            result = serdesGson.fromJson<MinecraftTexturesPayload>(json, MinecraftTexturesPayload::class.java)
        } catch (e: JsonParseException) {
            lgr.error("Could not decode textures payload", e)
            return MinecraftProfileTextures.EMPTY
        } catch (e: IllegalArgumentException) {
            lgr.error("Could not decode textures payload", e)
            return MinecraftProfileTextures.EMPTY
        }

        if (result == null || result.textures() == null || result.textures().isEmpty()) {
            return MinecraftProfileTextures.EMPTY
        }

        val textures = result.textures()
        return MinecraftProfileTextures(
            textures[MinecraftProfileTexture.Type.SKIN],
            textures[MinecraftProfileTexture.Type.CAPE],
            textures[MinecraftProfileTexture.Type.ELYTRA],
            SignatureState.UNSIGNED
        )
    }

    override fun fetchProfile(
        profileId: UUID,
        requireSecure: Boolean
    ): ProfileResult {
        val rdid = profileId.toObjectId()
        val mcp = RAccountService.getMcProfile(rdid)
        return ProfileResult(mcp)
    }

    override fun getSecurePropertyValue(property: Property): String {
        return property.value
    }

}