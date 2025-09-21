package calebxzhou.rdi.ihq

import calebxzhou.rdi.ihq.exception.AuthError
import calebxzhou.rdi.ihq.exception.ParamError
import calebxzhou.rdi.ihq.exception.RequestError
import calebxzhou.rdi.ihq.net.err
import calebxzhou.rdi.ihq.net.response
import calebxzhou.rdi.ihq.service.PlayerService
import calebxzhou.rdi.ihq.service.PlayerService.accountCol
import calebxzhou.rdi.ihq.service.RoomService
import calebxzhou.rdi.ihq.service.UpdateService
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
                call.err("❌参数！${cause.message}")
            }
            //逻辑错误
            exception<RequestError> { call, cause ->
                call.err(""+cause.message)
            }
            //认证错误
            exception<AuthError> { call, cause ->
                call.err("会话无效")
            }

            //其他内部错误
            exception<Throwable> { call, cause ->
                call.err("其他错误："+cause.message)
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
            get("/playerInfo") {
                PlayerService.getInfo(call)
            }
            get("/player-info") {
                PlayerService.getInfo(call)
            }
            get("/player-info-by-names") {
                PlayerService.getInfoByNames(call)
            }
            get("/sponsors") {
                call.response("""
                    2025-04-11,ChenQu,100
                    123
                    243534
                    """.trimIndent())
            }
            get("/name") {
                PlayerService.getNameFromId(call)
            }
            get("/skin") {
                PlayerService.getSkin(call)
            }
            post("/register") {
                PlayerService.register(call)
            }
            post("/login") {
                PlayerService.login(call)
            }
            route("/update"){
                get("/mod-list"){
                    UpdateService.getModList(call)
                }
                get("/mod-file") {
                    UpdateService.getModFile(call)
                }
            }
            post("/crash-report"){
                PlayerService.crashReport(call)
            }
            authenticate("auth-basic") {
                post("/skin") {
                    PlayerService.changeCloth(call)
                }

                post("/profile") {
                    PlayerService.changeProfile(call)
                }
                post("/clearSkin") {
                    PlayerService.clearCloth(call)
                }
                teamRoutes()
                route("/room") {
                    get("/my"){
                        RoomService.my(call)
                    }
                    post("/create") {
                        RoomService.create(call)
                    }
                    post("/delete") {
                        RoomService.delete(call)
                    }
                    post("/home") {
                        RoomService.home(call)
                    }
                    post("/sethome") {
                        RoomService.sethome(call)
                    }
                    post("/quit") {
                        RoomService.quit(call)
                    }
                    /*post("/invite") {
                        RoomService.invite(call)
                    }*/
                    post("/invite_qq") {
                        RoomService.inviteQQ(call)
                    }
                    post("/kick") {
                        RoomService.kick(call)
                    }
                    post("/transfer") {
                        RoomService.transfer(call)
                    }
                    get("/log"){
                        RoomService.getServerLog(call)
                    }

                    get("/list") {
                        RoomService.list(call)
                    }
                    post("/visit") {
                        RoomService.visit(call)
                    }
                    get("/log/stream"){
                        RoomService.streamServerLogSse(call)
                    }
                    /*post("/section/add"){
                        RoomService.addFirmSection(call)
                    }
                    post("/section/remove"){
                        RoomService.removeFirmSection(call)
                    }
                    get("/section"){
                        RoomService.getFirmSectionData(call)
                    }*/
                    route("/server"){
                        get("/status"){
                            RoomService.getServerStatus(call)
                        }
                        post("/start") {
                            RoomService.startServer(call)
                        }
                        post("/stop") {
                            RoomService.stopServer(call)
                        }
                        post("/update"){
                            RoomService.update(call)
                        }
                    }
                }
            }
        }
    }.start(wait = true)
}

