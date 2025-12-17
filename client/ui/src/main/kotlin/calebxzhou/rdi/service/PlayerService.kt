package calebxzhou.rdi.service

import calebxzhou.rdi.auth.LocalCredentials
import calebxzhou.rdi.auth.LoginInfo
import calebxzhou.rdi.lgr
import calebxzhou.rdi.model.HwSpec
import calebxzhou.rdi.model.RAccount
import calebxzhou.rdi.net.server
import calebxzhou.rdi.service.PlayerService.getPlayerInfo
import calebxzhou.rdi.ui2.frag.ProfileFragment
import calebxzhou.rdi.ui2.go
import calebxzhou.rdi.util.ioTask
import calebxzhou.rdi.util.serdesJson
import com.github.benmanes.caffeine.cache.AsyncLoadingCache
import com.github.benmanes.caffeine.cache.Caffeine
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
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
    val params = mutableMapOf("usr" to usr, "pwd" to pwd, "spec" to spec)
    ioTask {
        val account = server.makeRequest<RAccount>(
            path = "player/login",
            method = HttpMethod.Post,
            params = params
        ).data!!
        account.jwt = PlayerService.getJwt(usr,pwd)
        creds.loginInfos += account._id to LoginInfo(account.qq,account.pwd)
        creds.save()
        RAccount.now = account
        ProfileFragment().go()
    }


}
object PlayerService {




    suspend fun getJwt(usr: String,pwd: String): String {
        return server.makeRequest<String>("player/jwt", HttpMethod.Post,  params = mapOf("usr" to usr, "pwd" to pwd)).data!!
    }
    suspend fun getPlayerInfo(uid: ObjectId): RAccount.Dto {
        return try {
            server.makeRequest<RAccount.Dto>( "player/${uid}/info").data?: RAccount.DEFAULT.dto
        } catch (e: Exception) {
            lgr.warn("获取玩家信息失败",e)
            RAccount.DEFAULT.dto
        }
    }


}