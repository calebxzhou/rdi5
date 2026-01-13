package calebxzhou.rdi.client.service

import calebxzhou.mykotutils.hwspec.HwSpec
import calebxzhou.mykotutils.std.Ok
import calebxzhou.rdi.client.auth.LocalCredentials
import calebxzhou.rdi.client.auth.LoginInfo
import calebxzhou.rdi.common.exception.RequestError
import calebxzhou.rdi.common.model.RAccount
import calebxzhou.rdi.common.model.Response
import calebxzhou.rdi.common.serdesJson
import calebxzhou.rdi.lgr
import calebxzhou.rdi.client.net.loggedAccount
import calebxzhou.rdi.client.net.server
import calebxzhou.rdi.client.service.PlayerService.getPlayerInfo
import calebxzhou.rdi.client.ui.frag.ProfileFragment
import calebxzhou.rdi.client.ui.component.alertErr
import calebxzhou.rdi.client.ui.component.alertOk
import calebxzhou.rdi.client.ui.go
import com.github.benmanes.caffeine.cache.AsyncLoadingCache
import com.github.benmanes.caffeine.cache.Caffeine
import io.ktor.client.call.*
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
suspend fun playerLogin(usr: String, pwd: String): Result<RAccount> = runCatching {
    val creds = LocalCredentials.read()
    val spec = serdesJson.encodeToString<HwSpec>(HwSpec.get())

    val resp = server.createRequest(
        path = "player/login",
        method = HttpMethod.Post,
        params = mutableMapOf("usr" to usr, "pwd" to pwd, "spec" to spec)
    )
    val account = resp.body<Response<RAccount>>().run {
        data ?: run {
            throw RequestError(msg)
        }
    }
    account.jwt = resp.headers["jwt"]
    val loginInfo = LoginInfo(account.qq, account.pwd)
    creds.loginInfos += account._id to loginInfo
    creds.save()
    loggedAccount = account
    ProfileFragment().go()
    account
}

object PlayerService {


    suspend fun getJwt(usr: String, pwd: String): String {
        return server.makeRequest<String>(
            "player/jwt",
            HttpMethod.Post,
            params = mapOf("usr" to usr, "pwd" to pwd)
        ).data!!
    }

    suspend fun getPlayerInfo(uid: ObjectId): RAccount.Dto {
        return try {
            server.makeRequest<RAccount.Dto>("player/${uid}/info").data ?: RAccount.DEFAULT.dto
        } catch (e: Exception) {
            lgr.warn("获取玩家信息失败", e)
            RAccount.DEFAULT.dto
        }
    }
    suspend fun setCloth(cloth: RAccount.Cloth) : Result<Unit> = runCatching {
        val params = mutableMapOf<String, Any>()
        params["isSlim"] = cloth.isSlim.toString()
        params["skin"] = cloth.skin
        cloth.cape?.let {
            params["cape"] = it
        }

        val resp = server.makeRequest<Unit>("player/skin", HttpMethod.Post,params = params)
        if(resp.ok) Ok()
        else throw RequestError(resp.msg)
    }

}