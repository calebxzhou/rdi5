package calebxzhou.rdi.auth

import calebxzhou.rdi.model.RAccount
import calebxzhou.rdi.service.PlayerInfoCache
import calebxzhou.rdi.util.serdesJson
import calebxzhou.rdi.util.toObjectId
import com.mojang.authlib.GameProfile
import com.mojang.authlib.SignatureState
import com.mojang.authlib.minecraft.MinecraftProfileTextures
import com.mojang.authlib.minecraft.MinecraftSessionService
import com.mojang.authlib.properties.Property
import com.mojang.authlib.yggdrasil.ProfileResult
import java.net.InetAddress
import java.util.*

class RSessionService : MinecraftSessionService {
    override fun joinServer(profileId: UUID?, authenticationToken: String?, serverId: String?) {}

    override fun hasJoinedServer(
        profileName: String?,
        serverId: String?,
        address: InetAddress?
    ) = null

    override fun getPackedTextures(profile: GameProfile): Property? {
        return profile.properties.get("textures").firstOrNull()
    }

    override fun unpackTextures(packedTextures: Property): MinecraftProfileTextures {
        val value = packedTextures.value()
        try {
            val dto = serdesJson.decodeFromString<RAccount.Cloth>(value)
            return MinecraftProfileTextures(
                dto.skinTexture,
                dto.capeTexture,
                null,
                SignatureState.SIGNED
            )
        } catch (e: Exception) {
            e.printStackTrace()
            return MinecraftProfileTextures.EMPTY
        }

    }

    override fun fetchProfile(
        profileId: UUID,
        requireSecure: Boolean
    ): ProfileResult {
        val rdid = profileId.toObjectId()
        val mcp = PlayerInfoCache[rdid].mcProfile
        return ProfileResult(mcp)
    }

    override fun getSecurePropertyValue(property: Property): String {
        return property.value
    }

}