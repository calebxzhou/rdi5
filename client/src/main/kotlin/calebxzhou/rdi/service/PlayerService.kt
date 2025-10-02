package calebxzhou.rdi.service

import calebxzhou.rdi.auth.LocalCredentials
import calebxzhou.rdi.auth.LoginInfo
import calebxzhou.rdi.lgr
import calebxzhou.rdi.mixin.AMinecraft
import calebxzhou.rdi.model.HwSpec
import calebxzhou.rdi.model.RAccount
import calebxzhou.rdi.net.RServer
import calebxzhou.rdi.service.PlayerService.getPlayerInfo
import calebxzhou.rdi.ui2.frag.ProfileFragment
import calebxzhou.rdi.ui2.go
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
fun playerLogin(usr: String, pwd: String){
    val creds = LocalCredentials.read()
    val spec = serdesJson.encodeToString<HwSpec>(HwSpec.now)
    RServer.now.hqRequestT<RAccount>(
        path = "login",
        post = true,
        params = listOf("usr" to usr, "pwd" to pwd, "spec" to spec)
    ){
        val account = it.data!!
        creds.loginInfos += account._id to LoginInfo(account.qq,account.pwd)
        creds.save()
        RAccount.now = account
        if (isMcStarted)
            (mc as AMinecraft).setUser(account.mcUser)
        ProfileFragment().go()
    }

}
object PlayerService {


    suspend fun RServer.sendLoginRecord(account: RAccount) {

    }

    @JvmStatic
    fun getPackedTextures(profile: GameProfile): Property? {
        return PlayerInfoCache[profile.id.objectId].mcProfile.properties["textures"].firstOrNull()
            ?.also { profile.properties.put("textures", it) }
    }



    suspend fun getPlayerInfo(uid: ObjectId): RAccount.Dto {
        return try {
            RServer.now.prepareRequest<RAccount.Dto>(false, "player-info", listOf("uid" to uid)).data?: RAccount.DEFAULT.dto
        } catch (e: Exception) {
            lgr.warn("获取玩家信息失败",e)
            RAccount.DEFAULT.dto
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