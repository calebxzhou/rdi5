package calebxzhou.rdi.auth

import calebxzhou.rdi.lgr
import calebxzhou.rdi.model.RAccount
import calebxzhou.rdi.model.RAccount.Cloth
import calebxzhou.rdi.model.RAccount.Dto
import calebxzhou.rdi.model.RServer
import calebxzhou.rdi.net.body
import calebxzhou.rdi.util.serdesJson
import calebxzhou.rdi.util.toUUID
import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.mojang.authlib.GameProfile
import com.mojang.authlib.minecraft.MinecraftProfileTexture
import kotlinx.coroutines.runBlocking
import net.minecraft.resources.ResourceLocation
import org.bson.types.ObjectId
import java.util.concurrent.TimeUnit

object RAccountService {
    val profileCache = CacheBuilder.newBuilder().expireAfterWrite(30L, TimeUnit.MINUTES).build(object :
        CacheLoader<ObjectId, GameProfile>() {
        override fun load(key: ObjectId): GameProfile {
            return runBlocking {  retrievePlayerInfo(key)}.mcProfile
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


    suspend fun retrievePlayerInfo(uid: ObjectId): RAccount.Dto{
        return RServer.now?.prepareRequest(false, "playerInfo", listOf("uid" to uid))?.let { resp ->
            val json = resp.body
            lgr.info("玩家信息:${json}")
            serdesJson.decodeFromString(json)
        } ?: let {
            lgr.warn("服务器未连接!!")
            Dto(ObjectId(), "未知", Cloth())
        }
    }
}