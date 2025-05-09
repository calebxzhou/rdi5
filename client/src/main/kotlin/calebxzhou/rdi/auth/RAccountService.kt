package calebxzhou.rdi.auth

import calebxzhou.rdi.auth.RAccount.Cloth
import calebxzhou.rdi.auth.RAccount.Dto
import calebxzhou.rdi.lgr
import calebxzhou.rdi.net.RServer
import calebxzhou.rdi.net.body
import calebxzhou.rdi.serdes.serdesJson
import calebxzhou.rdi.util.toObjectId
import calebxzhou.rdi.util.toUUID
import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.mojang.authlib.GameProfile
import com.mojang.authlib.minecraft.MinecraftProfileTexture
import net.minecraft.resources.ResourceLocation
import org.bson.types.ObjectId
import java.util.concurrent.TimeUnit

object RAccountService {
    val profileCache = CacheBuilder.newBuilder().expireAfterWrite(30L, TimeUnit.MINUTES).build(object :
        CacheLoader<ObjectId, GameProfile>() {
        override fun load(key: ObjectId): GameProfile {
            return retrievePlayerInfo(key).mcProfile
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


    fun retrievePlayerInfo(uid: ObjectId): RAccount.Dto {
        return RServer.now?.hqSendSync(false, "playerInfo", listOf("uid" to uid))?.let { resp ->
            val json = resp.body
            lgr.info("玩家信息:${json}")
            serdesJson.decodeFromString(json)
        } ?: let {
            lgr.warn("服务器未连接!!")
            Dto(ObjectId(), "未知", Cloth())
        }
    }
}