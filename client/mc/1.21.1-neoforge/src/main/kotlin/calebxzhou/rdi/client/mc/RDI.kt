package calebxzhou.rdi.client.mc

import calebxzhou.mykotutils.log.Loggers
import calebxzhou.rdi.client.mc.connect.WsClient.connectCore
import calebxzhou.rdi.mixin.ASkinManager
import calebxzhou.rdi.mixin.mSkinManager
import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.google.common.cache.LoadingCache
import com.google.gson.Gson
import com.mojang.authlib.SignatureState
import com.mojang.authlib.minecraft.MinecraftProfileTextures
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import net.minecraft.Util
import net.minecraft.client.resources.PlayerSkin
import net.minecraft.client.resources.SkinManager
import net.neoforged.fml.common.Mod
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.function.Function
import java.util.function.Supplier

val scope = CoroutineScope(Dispatchers.IO)
val serdesJson = Json {
    ignoreUnknownKeys = true  // Good for forward compatibility
    classDiscriminator = "type"  // Uses the @SerialName as discriminator
}
inline val <reified T> T.json: String
    get() = serdesJson.encodeToString<T>(this)

@Mod("rdi")
class RDI {
    val lgr by Loggers

    companion object {
        @JvmField
        val IHQ_URL =
            System.getProperty("rdi.ihq.url") ?: throw IllegalArgumentException("启动方式错误：找不到服务器地址1")

        @JvmField
        val GAME_IP =
            System.getProperty("rdi.game.ip") ?: throw IllegalArgumentException("启动方式错误：找不到服务器地址2")

        @JvmField
        val HOST_NAME =
            System.getProperty("rdi.host.name") ?: throw IllegalArgumentException("启动方式错误：找不到主机名")

        @JvmField
        var HOST_PORT = System.getProperty("rdi.host.port")?.toInt()
            ?: throw IllegalArgumentException("启动方式错误：找不到服务器端口")

        @JvmStatic
        fun getSkinCache(skinManager: SkinManager): CacheLoader<SkinManager.CacheKey, CompletableFuture<PlayerSkin>> {
            return object : CacheLoader<SkinManager.CacheKey, CompletableFuture<PlayerSkin>>() {
                override fun load(cacheKey: SkinManager.CacheKey): CompletableFuture<PlayerSkin> {
                    return CompletableFuture.supplyAsync({
                        runCatching {
                            HttpClient().use { httpClient ->
                                val textures = runBlocking {
                                    httpClient.get("${IHQ_URL}/mc-profile/${cacheKey.profileId}/clothes")
                                        .bodyAsText()
                                }.let { Gson().fromJson(it, MinecraftProfileTextures::class.java) }
                                return@supplyAsync textures
                            }
                        }.getOrElse {
                            lgr.warn { "${"Couldn't get textures from RDI IHQ server for profile {}: {}"} ${cacheKey.profileId} ${it.toString()}" }
                            return@supplyAsync MinecraftProfileTextures.EMPTY
                        }
                    }, Util.backgroundExecutor())
                        .thenComposeAsync( { textures ->
                            (skinManager as ASkinManager).invokeRegisterTextures(
                                cacheKey.profileId,
                                textures
                            )
                        }, mc)
                }
            }
        }
    }

    init {
        lgr.info { "RDI启动中" }
        scope.launch {
            runCatching {
                connectCore()
            }.onFailure { error ->
                lgr.error(error) { "WebSocket connection failed" }
            }
        }
    }
}