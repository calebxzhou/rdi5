package calebxzhou.rdi.ihq

import calebxzhou.rdi.ihq.exception.AuthError
import calebxzhou.rdi.ihq.exception.ParamError
import calebxzhou.rdi.ihq.exception.RequestError
import calebxzhou.rdi.ihq.net.err
import calebxzhou.rdi.ihq.net.response
import calebxzhou.rdi.ihq.service.PlayerService
import calebxzhou.rdi.ihq.service.PlayerService.accountCol
import calebxzhou.rdi.ihq.service.UpdateService
import calebxzhou.rdi.ihq.service.playerRoutes
import calebxzhou.rdi.ihq.service.roomRoutes
import calebxzhou.rdi.ihq.service.teamRoutes
import calebxzhou.rdi.ihq.util.serdesJson
import com.mongodb.MongoClientSettings
import com.mongodb.ServerAddress
import com.mongodb.client.model.IndexOptions
import com.mongodb.client.model.Indexes
import com.mongodb.kotlin.client.coroutine.MongoClient
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.compression.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.respondText
import io.ktor.server.routing.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.bson.UuidRepresentation
import java.io.File

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
fun main(): Unit =runBlocking {
    CRASH_REPORT_DIR.mkdir()
        lgr.info { "init db" }

        accountCol.createIndex(Indexes.ascending("qq"), IndexOptions().unique(true))
        accountCol.createIndex(Indexes.ascending("name"), IndexOptions().unique(true))
    UpdateService.reloadModInfo()
    //5分钟重载mod  没什么用 手动重载了
   /* Timer().scheduleAtFixedRate(object : TimerTask() {
        override fun run() {
            UpdateService.reloadModInfo()
        }
    },0,60000*5)*/
    // Launch both servers concurrently in the coroutine scope
    launch {
        startHttp()
    }


}
fun startHttp(){
    embeddedServer(Netty, host = "::", port = CONF.server.port){
        install(StatusPages) {
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
                call.response<Unit>(false, cause.message ?: "认证错误",null)
            }

            //其他内部错误
            exception<Throwable> { call, cause ->
                call.response<Unit>(false, cause.message ?: "未知错误",null)
            }
        }
        install(ContentNegotiation) {
            json(serdesJson) // Apply the custom Json configuration
        }

        install(Authentication) {
            basic("auth-basic") {
                realm = "Access to the '/' path"
                validate { credentials ->
                    PlayerService.validate(credentials.name, credentials.password)?.let {
                        UserIdPrincipal(it._id.toString())
                    }
                }
            }
        }
        install(Compression) {
            gzip()
            deflate()
        }
        routing {
            playerRoutes()
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
            authenticate("auth-basic") {
                teamRoutes()
                roomRoutes()
            }
        }
    }.start(wait = true)
}

