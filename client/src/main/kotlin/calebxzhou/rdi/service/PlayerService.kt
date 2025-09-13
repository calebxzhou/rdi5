package calebxzhou.rdi.service

import calebxzhou.rdi.auth.LocalCredentials
import calebxzhou.rdi.lgr
import calebxzhou.rdi.mixin.AMinecraft
import calebxzhou.rdi.model.HwSpec
import calebxzhou.rdi.model.RAccount
import calebxzhou.rdi.net.RServer
import calebxzhou.rdi.net.StringHttpResponse
import calebxzhou.rdi.net.body
import calebxzhou.rdi.net.success
import calebxzhou.rdi.service.PlayerService.getPlayerInfo
import calebxzhou.rdi.ui2.frag.RFragment
import calebxzhou.rdi.ui2.component.alertErr
import calebxzhou.rdi.ui2.component.closeLoading
import calebxzhou.rdi.ui2.component.showLoading
import calebxzhou.rdi.util.ioScope
import calebxzhou.rdi.util.isMcStarted
import calebxzhou.rdi.util.mc
import calebxzhou.rdi.util.objectId
import calebxzhou.rdi.util.serdesJson
import com.github.benmanes.caffeine.cache.AsyncLoadingCache
import com.github.benmanes.caffeine.cache.Caffeine
import com.mojang.authlib.GameProfile
import com.mojang.authlib.properties.Property
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket
import org.bson.types.ObjectId
import java.util.concurrent.TimeUnit


object PlayerInfoCache {
    // Async cache: returns futures and loads off-thread
    private val cache: AsyncLoadingCache<ObjectId, RAccount.Dto> = Caffeine.newBuilder()
        .expireAfterWrite(30L, TimeUnit.MINUTES)
        .buildAsync { key: ObjectId ->
            // Caffeine will run this on a separate executor; safe to block here
            runBlocking { getPlayerInfo(key) }
        }

    operator fun get(uid: ObjectId): RAccount.Dto {
        return cache.get(uid).join()
    }
}

object PlayerService {
    suspend fun RServer.playerLogin(frag: RFragment, usr: String, pwd: String): RAccount? = try {
        frag.showLoading()
        val creds = LocalCredentials.read()
        val spec = serdesJson.encodeToString<HwSpec>(HwSpec.now)
        val resp = prepareRequest(
            path = "login",
            post = true,
            params = listOf("usr" to usr, "pwd" to pwd, "spec" to spec)
        )
        if (resp.success) {
            val account = serdesJson.decodeFromString<RAccount>(resp.body)
            creds.idPwds += account._id.toHexString() to account.pwd
            creds.lastLoggedId = account._id.toHexString()
            creds.write()
            RAccount.now = account
            if (isMcStarted)
                (mc as AMinecraft).setUser(account.mcUser)

            account
        } else {
            alertErr(resp.body)
            null
        }

    } catch (e: Exception){
        e.printStackTrace()
        null
    } finally {
       // frag.closeLoading()
    }

    suspend fun RServer.sendLoginRecord(account: RAccount) {

    }

    @JvmStatic
    fun getPackedTextures(profile: GameProfile): Property? {
        return PlayerInfoCache[profile.id.objectId].mcProfile.properties["textures"].firstOrNull()
            ?.also { profile.properties.put("textures", it) }
    }

    suspend fun queryPlayerInfo(uid: ObjectId): StringHttpResponse? {
        return RServer.now?.prepareRequest(false, "player-info", listOf("uid" to uid))
    }

    suspend fun getPlayerInfo(uid: ObjectId): RAccount.Dto {
        return queryPlayerInfo(uid)?.let { resp ->
            val json = resp.body
            lgr.info("玩家信息:${json}")
            serdesJson.decodeFromString(json)
        } ?: let {
            lgr.warn("服务器未连接!!")
            RAccount.Dto(ObjectId(), "未知", RAccount.Cloth())
        }
    }

    @JvmStatic
    fun onPlayerInfoUpdate(packet: ClientboundPlayerInfoUpdatePacket) = ioScope.launch {
        packet.entries().mapNotNull { it.profile?.id?.objectId }.forEach {
            launch {
                val info = PlayerInfoCache[it]
                lgr.info("成功接收玩家信息 $info")
            }
        }
    }

}