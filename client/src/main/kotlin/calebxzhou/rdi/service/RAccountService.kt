package calebxzhou.rdi.service

import calebxzhou.rdi.lgr
import calebxzhou.rdi.model.RAccount
import calebxzhou.rdi.model.RServer
import calebxzhou.rdi.model.Room
import calebxzhou.rdi.net.StringHttpResponse
import calebxzhou.rdi.net.body
import calebxzhou.rdi.util.objectId
import calebxzhou.rdi.util.serdesJson
import calebxzhou.rdi.util.toUUID
import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.mojang.authlib.GameProfile
import com.mojang.authlib.minecraft.MinecraftProfileTexture
import com.mojang.authlib.properties.Property
import kotlinx.coroutines.runBlocking
import net.minecraft.resources.ResourceLocation
import org.bson.types.ObjectId
import java.util.concurrent.TimeUnit

object RAccountService {
    val profileCache = CacheBuilder.newBuilder().expireAfterWrite(30L, TimeUnit.MINUTES).build(object :
        CacheLoader<ObjectId, GameProfile>() {
        override fun load(key: ObjectId): GameProfile {
            return runBlocking { getPlayerInfo(key) }.mcProfile
        }
    })
    fun createMcProfile(uid: ObjectId) = GameProfile(uid.toUUID(), uid.toHexString())
    fun getMcProfile(uid: ObjectId) = profileCache.getUnchecked(uid)
    fun getTextureLocation(type: MinecraftProfileTexture.Type, hashUC: String): ResourceLocation {
        val prefix = when (type) {
            MinecraftProfileTexture.Type.SKIN -> "skins"
            MinecraftProfileTexture.Type.CAPE -> "capes"
            MinecraftProfileTexture.Type.ELYTRA -> "elytra"
            else -> throw IncompatibleClassChangeError()
        }
        return ResourceLocation.parse("$prefix/$hashUC")
    }
    @JvmStatic
    fun getPackedTextures(profile: GameProfile): Property? {
        return getMcProfile(profile.id.objectId).properties.get("textures").firstOrNull()?.also { profile.properties.put("textures",it) }
    }
    suspend fun queryPlayerInfo(uid: ObjectId): StringHttpResponse? {
        return RServer.now?.prepareRequest(false, "player-info", listOf("uid" to uid))
    }
    suspend fun getPlayerInfo(uid: ObjectId): RAccount.Dto{
        return queryPlayerInfo(uid)?.let { resp ->
            val json = resp.body
            lgr.info("玩家信息:${json}")
            serdesJson.decodeFromString(json)
        } ?: let {
            lgr.warn("服务器未连接!!")
            RAccount.Dto(ObjectId(), "未知", RAccount.Cloth())
        }
    }
    suspend fun RAccount.getMyRoom(): Room? {
        RServer.now?.prepareRequest(path = "room/my")?.let { resp ->
            return if (resp.body != "0") {
                serdesJson.decodeFromString<Room>(resp.body)

            } else {
                null
            }
        }

        return null


    }
}