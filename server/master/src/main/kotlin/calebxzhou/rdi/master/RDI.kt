package calebxzhou.rdi.master

import calebxzhou.rdi.common.serdesJson
import calebxzhou.rdi.master.exception.AuthError
import calebxzhou.rdi.master.exception.ParamError
import calebxzhou.rdi.master.exception.RequestError
import calebxzhou.rdi.master.net.response
import calebxzhou.rdi.master.service.*
import calebxzhou.rdi.master.service.PlayerService.accountCol
import calebxzhou.rdi.master.ygg.YggdrasilService.yggdrasilRoutes
import com.mongodb.MongoClientSettings
import com.mongodb.ServerAddress
import com.mongodb.client.model.IndexOptions
import com.mongodb.client.model.Indexes
import com.mongodb.kotlin.client.coroutine.MongoClient
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.compression.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.routing.*
import io.ktor.server.sse.*
import io.ktor.server.websocket.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.bson.UuidRepresentation
import org.bson.codecs.configuration.CodecRegistries.fromProviders
import org.bson.codecs.configuration.CodecRegistries.fromRegistries
import org.bson.codecs.pojo.PojoCodecProvider
import java.io.File
import kotlin.time.Duration.Companion.seconds

val CONF = AppConfig.load()
val lgr = KotlinLogging.logger {  }
val DB = MongoClient.create(
    MongoClientSettings.builder()
        .applyToClusterSettings { builder ->
            builder.hosts(listOf(ServerAddress(CONF.database.host, CONF.database.port)))
        }
        .uuidRepresentation(UuidRepresentation.STANDARD)
        .codecRegistry(
            fromRegistries(
                MongoClientSettings.getDefaultCodecRegistry(),
                fromProviders(
                    PojoCodecProvider.builder().automatic(true).build()
                )
            )
        )
        .build()).getDatabase(CONF.database.name)
private fun storageDir(path: String?, defaultName: String): File {
    val trimmed = path?.trim().orEmpty()
    return if (trimmed.isBlank()) File(defaultName) else File(trimmed)
}

val CRASH_REPORT_DIR = storageDir(CONF.storage.crashReportDir, "crash-report")
val MODPACK_DATA_DIR = storageDir(CONF.storage.modpackDir, "modpack")
val HOSTS_DIR = storageDir(CONF.storage.hostsDir, "hosts")
val GAME_LIBS_DIR = storageDir(CONF.storage.gameLibsDir, "game-libs")
val WORLDS_DIR = storageDir(CONF.storage.worldsDir, "worlds")
class RDI {}
fun main(): Unit =runBlocking {
    CONF.storage.dlModsDir?.let { System.setProperty("rdi.modDir",it) }

    CRASH_REPORT_DIR.mkdirs()
    MODPACK_DATA_DIR.mkdirs()
    HOSTS_DIR.mkdirs()
    GAME_LIBS_DIR.mkdirs()
    WORLDS_DIR.mkdirs()
        lgr.info { "init db" }

        accountCol.createIndex(Indexes.ascending("qq"), IndexOptions().unique(true))
        accountCol.createIndex(Indexes.ascending("name"), IndexOptions().unique(true))

    HostService.startIdleMonitor()
    Runtime.getRuntime().addShutdownHook(Thread {
        HostService.stopIdleMonitor()
    })
    // Launch both servers concurrently in the coroutine scope
    launch {
        startHttp()
    }


}
fun startHttp(){
    embeddedServer(Netty, host = "::", port = CONF.server.port){
        install(StatusPages) {
            status(HttpStatusCode.NotFound) { call, status ->
                call.response<Unit>(-404, "找不到请求的内容",null)
            }
            //参数不全/有问题
            exception<ParamError> { call, cause ->
                call.response<Unit>(false, cause.message ?: "参数错误",null)
            }
            //逻辑错误
            exception<RequestError> { call, cause ->
                call.response<Unit>(false, cause.message ?: "逻辑错误",null)
            }
            //认证错误
            exception<AuthError> { call, cause ->
                call.response<Unit>(-401, cause.message ?: "账密错/未登录",null, HttpStatusCode.Unauthorized)
            }

            //其他内部错误
            exception<Throwable> { call, cause ->
                cause.printStackTrace()
                call.response<Unit>(-500, cause.message ?: "未知错误",null)
            }
        }
        install(ContentNegotiation) {
            json(serdesJson) // Apply the custom Json configuration
        }

        install(Authentication) {
            val jwtConfig = CONF.jwt
            jwt("auth-jwt") {
                realm = jwtConfig.realm
                verifier(JwtService.verifier)
                validate { credential ->
                    val uidClaim = credential.payload.getClaim("uid").asString()
                    if (!uidClaim.isNullOrBlank()) JWTPrincipal(credential.payload) else null
                }
                challenge { _, _ ->
                    call.response<Unit>(-401, "token×",null, HttpStatusCode.Unauthorized)
                }
            }
        }
        install(Compression) {
            gzip {
                matchContentType(ContentType.Text.Any, ContentType.Application.Json)
            }
            deflate {
                matchContentType(ContentType.Text.Any, ContentType.Application.Json)
            }
        }
        install(SSE)
        install(WebSockets){
            pingPeriod = 15.seconds
            timeout = 10.seconds
            maxFrameSize = Long.MAX_VALUE
            masking = false
        }
        routing {
            playerRoutes()
            updateRoutes()
            yggdrasilRoutes()
            /*get("/sponsors") {
                call.respondText("""
                    2025-04-11,ChenQu,100
                    123
                    243534
                    """.trimIndent())
            }
            route("/update"){
                get("/mod-list"){
                    UpdateService.getModList(call)
                }
                get("/mod-file") {
                    UpdateService.getModFile(call)
                }
            }*/
            hostPlayRoutes()
            authenticate( "auth-jwt") {
                hostRoutes()
                worldRoutes()
                chatRoutes()
                modpackRoutes()
                mailRoutes()
            }
        }
    }.start(wait = true)
}

