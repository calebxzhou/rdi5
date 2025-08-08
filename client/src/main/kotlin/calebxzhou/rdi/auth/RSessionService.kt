package calebxzhou.rdi.auth

import calebxzhou.rdi.model.RAccount
import calebxzhou.rdi.net.body
import calebxzhou.rdi.service.RAccountService
import calebxzhou.rdi.util.objectId
import calebxzhou.rdi.util.serdesJson
import calebxzhou.rdi.util.toObjectId
import com.mojang.authlib.GameProfile
import com.mojang.authlib.SignatureState
import com.mojang.authlib.minecraft.MinecraftProfileTextures
import com.mojang.authlib.minecraft.MinecraftSessionService
import com.mojang.authlib.properties.Property
import com.mojang.authlib.yggdrasil.ProfileResult
import kotlinx.coroutines.runBlocking
import java.net.InetAddress
import java.util.*

class RSessionService : MinecraftSessionService {
    override fun joinServer(profileId: UUID?, authenticationToken: String?, serverId: String?) {}

    override fun hasJoinedServer(
        profileName: String?,
        serverId: String?,
        address: InetAddress?
    ) = null

    override fun getPackedTextures(profile: GameProfile): Property {
        runBlocking {
            RAccountService.queryPlayerInfo(profile.id.objectId)?.body
        } .let { return Property("dto",it?:"{}") }
    }

    override fun unpackTextures(packedTextures: Property): MinecraftProfileTextures {
        val value = packedTextures.value()
        try {
            val dto = serdesJson.decodeFromString<RAccount.Dto>(value)
            return MinecraftProfileTextures(
                dto.cloth.skinTexture,
                dto.cloth.capeTexture,
                null,
                SignatureState.UNSIGNED
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
        val mcp = RAccountService.getMcProfile(rdid)
        return ProfileResult(mcp)
    }

    override fun getSecurePropertyValue(property: Property): String {
        return property.value
    }

}