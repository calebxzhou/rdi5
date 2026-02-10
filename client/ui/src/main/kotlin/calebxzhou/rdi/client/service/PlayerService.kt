package calebxzhou.rdi.client.service

import calebxzhou.mykotutils.hwspec.HwSpec
import calebxzhou.rdi.client.auth.LocalCredentials
import calebxzhou.rdi.client.model.LoginInfo
import calebxzhou.rdi.client.net.loggedAccount
import calebxzhou.rdi.client.net.server
import calebxzhou.rdi.client.service.PlayerService.getPlayerInfos
import calebxzhou.rdi.common.exception.RequestError
import calebxzhou.rdi.common.model.RAccount
import calebxzhou.rdi.common.model.Response
import calebxzhou.rdi.common.serdesJson
import calebxzhou.rdi.common.util.ok
import calebxzhou.rdi.lgr
import com.github.benmanes.caffeine.cache.Caffeine
import io.ktor.client.call.*
import io.ktor.http.*
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.raphimc.minecraftauth.MinecraftAuth
import net.raphimc.minecraftauth.java.JavaAuthManager
import net.raphimc.minecraftauth.msa.model.MsaDeviceCode
import net.raphimc.minecraftauth.msa.service.impl.DeviceCodeMsaAuthService
import org.bson.types.ObjectId
import java.util.concurrent.TimeUnit
import java.util.function.Consumer


object PlayerInfoCache {
    private val cache = Caffeine.newBuilder()
        .expireAfterWrite(30L, TimeUnit.MINUTES)
        .build<ObjectId, RAccount.Dto>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val pending = LinkedHashMap<ObjectId, CompletableDeferred<RAccount.Dto>>()
    private val mutex = Mutex()
    private var batchJob: Job? = null
    operator fun minusAssign(uid: ObjectId) {
        cache.invalidate(uid)
    }

    suspend operator fun get(uid: ObjectId): RAccount.Dto {
        return getAsync(uid)
    }

    suspend fun getAsync(uid: ObjectId): RAccount.Dto {
        cache.getIfPresent(uid)?.let { return it }
        val deferred = mutex.withLock {
            cache.getIfPresent(uid)?.let { return it }
            pending[uid] ?: CompletableDeferred<RAccount.Dto>().also { pending[uid] = it }
        }
        scheduleBatch()
        return deferred.await()
    }

    private fun scheduleBatch() {
        if (batchJob?.isActive == true) return
        batchJob = scope.launch {
            while (true) {
                delay(50)
                val batch = mutex.withLock {
                    if (pending.isEmpty()) return@withLock emptyMap()
                    val snapshot = pending.toMap()
                    pending.clear()
                    snapshot
                }
                if (batch.isEmpty()) break
                val ids = batch.keys.toList()
                val infos = runCatching { getPlayerInfos(ids) }.getOrNull()
                val infoMap = infos?.associateBy { it.id }.orEmpty()
                ids.forEach { id ->
                    val info = infoMap[id] ?: RAccount.Dto(id, RAccount.DEFAULT.name, RAccount.Cloth())
                    cache.put(id, info)
                    batch[id]?.complete(info)
                }
                val hasMore = mutex.withLock { pending.isNotEmpty() }
                if (!hasMore) break
            }
        }
    }
}

object PlayerService {

    suspend fun login(usr: String, pwd: String): Result<RAccount> = runCatching {
        val creds = LocalCredentials.read()
        val spec = serdesJson.encodeToString<HwSpec>(HwSpec.NOW)

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
        val loginInfo = LoginInfo(account.qq, account.name, account.pwd)
        creds.loginInfos += account._id to loginInfo
        creds.save()
        loggedAccount = account
        account
    }

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

    suspend fun getPlayerInfos(uids: List<ObjectId>): List<RAccount.Dto> {
        if (uids.isEmpty()) return emptyList()
        return try {
            val idsParam = uids.joinToString("\n") { it.toHexString() }
            server.makeRequest<List<RAccount.Dto>>("player/infos", params = mapOf("ids" to idsParam)).data
                ?: emptyList()
        } catch (e: Exception) {
            lgr.warn("批量获取玩家信息失败", e)
            emptyList()
        }
    }

    suspend fun setCloth(cloth: RAccount.Cloth): Result<Unit> = runCatching {
        val params = mutableMapOf<String, Any>()
        params["isSlim"] = cloth.isSlim.toString()
        params["skin"] = cloth.skin
        cloth.cape?.let {
            params["cape"] = it
        }

        val resp = server.makeRequest<Unit>("player/skin", HttpMethod.Post, params = params)
        if (resp.ok) ok()
        else throw RequestError(resp.msg)
    }

    fun microsoftLogin(onDevice: (MsaDeviceCode) -> Unit): Result<JavaAuthManager> = runCatching {
        JavaAuthManager.create(MinecraftAuth.createHttpClient("rdi-client"))
            .login(::DeviceCodeMsaAuthService, Consumer { deviceCode: MsaDeviceCode ->
                onDevice(deviceCode)
            })
    }

}