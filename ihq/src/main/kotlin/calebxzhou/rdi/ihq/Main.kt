package calebxzhou.rdi.ihq

import calebxzhou.rdi.ihq.exception.AuthError
import calebxzhou.rdi.ihq.exception.ParamError
import calebxzhou.rdi.ihq.exception.RequestError
import calebxzhou.rdi.ihq.net.response
import calebxzhou.rdi.ihq.service.PlayerService
import calebxzhou.rdi.ihq.service.PlayerService.accountCol
import calebxzhou.rdi.ihq.service.UpdateService
import calebxzhou.rdi.ihq.service.playerRoutes
import calebxzhou.rdi.ihq.service.HostService
import calebxzhou.rdi.ihq.service.JwtService
import calebxzhou.rdi.ihq.service.hostRoutes
import calebxzhou.rdi.ihq.service.worldRoutes
import calebxzhou.rdi.ihq.service.chatRoutes
import calebxzhou.rdi.ihq.service.hostPlayRoutes
import calebxzhou.rdi.ihq.service.mailRoutes
import calebxzhou.rdi.ihq.service.modpackRoutes
import calebxzhou.rdi.ihq.service.updateRoutes
import calebxzhou.rdi.ihq.util.serdesJson
import com.mongodb.MongoClientSettings
import com.mongodb.ServerAddress
import com.mongodb.client.model.IndexOptions
import com.mongodb.client.model.Indexes
import com.mongodb.kotlin.client.coroutine.MongoClient
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.compression.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.*
import io.ktor.server.sse.*
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.pingPeriod
import io.ktor.server.websocket.timeout
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.bson.UuidRepresentation
import java.io.File
import java.util.Timer
import java.util.TimerTask
import kotlin.time.Duration.Companion.seconds

val CONF = AppConfig.load()
val lgr = KotlinLogging.logger {  }
val DB = MongoClient.create(
    MongoClientSettings.builder()
        .applyToClusterSettings { builder ->
            builder.hosts(listOf(ServerAddress(CONF.database.host, CONF.database.port)))
        }
        .uuidRepresentation(UuidRepresentation.STANDARD)
        .build()).getDatabase(CONF.database.name)
val CRASH_REPORT_DIR = File("crash-report")
val DOWNLOAD_MODS_DIR = File("download-mods")
val MODPACK_DATA_DIR = File("modpack")
val BASE_IMAGE_DIR = File("base-image")
fun main(): Unit =runBlocking {
    CRASH_REPORT_DIR.mkdir()
    DOWNLOAD_MODS_DIR.mkdir()
    MODPACK_DATA_DIR.mkdir()
    BASE_IMAGE_DIR.mkdir()
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
            gzip()
            deflate()
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

