package calebxzhou.rdi.ihq

import calebxzhou.rdi.ihq.exception.AuthError
import calebxzhou.rdi.ihq.exception.ParamError
import calebxzhou.rdi.ihq.exception.RequestError
import calebxzhou.rdi.ihq.net.RFrameDecoder
import calebxzhou.rdi.ihq.net.RLengthFieldPrepender
import calebxzhou.rdi.ihq.net.VarInt.readVarInt
import calebxzhou.rdi.ihq.net.VarInt.writeVarInt
import calebxzhou.rdi.ihq.service.ChatService
import calebxzhou.rdi.ihq.service.PlayerService
import calebxzhou.rdi.ihq.service.PlayerService.accountCol
import calebxzhou.rdi.ihq.service.RoomService
import calebxzhou.rdi.ihq.util.e400
import calebxzhou.rdi.ihq.util.e401
import calebxzhou.rdi.ihq.util.e500
import calebxzhou.rdi.ihq.util.ok
import com.mongodb.MongoClientSettings
import com.mongodb.ServerAddress
import com.mongodb.client.model.IndexOptions
import com.mongodb.client.model.Indexes
import com.mongodb.kotlin.client.coroutine.MongoClient
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.routing.*
import io.ktor.utils.io.*
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.Channel
import io.netty.channel.ChannelHandler
import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelOption
import io.netty.channel.epoll.Epoll
import io.netty.channel.epoll.EpollEventLoopGroup
import io.netty.channel.epoll.EpollServerSocketChannel
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.flow.FlowControlHandler
import io.netty.handler.timeout.ReadTimeoutHandler
import io.netty.util.concurrent.DefaultThreadFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.bson.UuidRepresentation
import java.util.concurrent.ThreadFactory

val DB_HOST = System.getProperty("rdi.dbHost") ?: "127.0.0.1"
val DB_PORT = System.getProperty("rdi.dbPort")?.toIntOrNull() ?: 27017
val lgr = KotlinLogging.logger {  }
val DB = MongoClient.create(
    MongoClientSettings.builder()
        .applyToClusterSettings { builder ->
            builder.hosts(listOf(ServerAddress(DB_HOST, DB_PORT)))
        }
        .uuidRepresentation(UuidRepresentation.STANDARD)
        .build()).getDatabase("rdi5")
val GAME_PORT = System.getProperty("rdi.gamePort")?.toIntOrNull() ?: 28520
val HQ_PORT = System.getProperty("rdi.hqPort")?.toIntOrNull() ?: 28521
fun main(): Unit =runBlocking {

        lgr.info { "init db" }
        RoomService.dbcl.createIndex(Indexes.ascending("members.pid"), IndexOptions().background(true))

        accountCol.createIndex(Indexes.ascending("qq"), IndexOptions().unique(true))
        accountCol.createIndex(Indexes.ascending("name"), IndexOptions().unique(true))

    val selectorManager = SelectorManager(Dispatchers.IO)

    // Start the TCP server in a coroutine
    launch(Dispatchers.IO) {
        startTcp (selectorManager)
    }
    startHttp()
}
fun startTcp(selectorManager: SelectorManager){
    val channel = if(Epoll.isAvailable()) EpollServerSocketChannel::class.java else NioServerSocketChannel::class.java
    val eventGroup = if(Epoll.isAvailable())
        EpollEventLoopGroup(0, DefaultThreadFactory("RDI-Epoll"))
    else
        NioEventLoopGroup(0, DefaultThreadFactory("RDI-Nio"))
    ServerBootstrap()
        .channel(channel)
        .group(eventGroup)
        .localAddress("::",GAME_PORT)
        .handler(object : ChannelInitializer<Channel>() {
            override fun initChannel(channel: Channel) {
                channel.config().setOption(ChannelOption.TCP_NODELAY,true)
                channel.pipeline()
                    .addLast("timeout", ReadTimeoutHandler(15))
                    .addLast("splitter", RFrameDecoder())
                    .addLast(FlowControlHandler())
                    //.addLast("decoder",)
                    //.addLast("encoder",)
                    .addLast("prepender",RLengthFieldPrepender())

            }

        })



}
fun startHttp(){
    /*
    5分钟自动更新mod信息
    Timer().scheduleAtFixedRate(object : TimerTask() {
        override fun run() {
            UpdateService.reloadModInfo()
        }
    },0,60000*5)*/
    embeddedServer(Netty, host = "::", port = HQ_PORT){
        install(StatusPages) {
            //参数不全或者有问题
            exception<ParamError> { call, cause ->
                call.e400(cause.message)
            }
            exception<RequestError> { call, cause ->
                call.e400(cause.message)
            }

            exception<AuthError> { call, cause ->
                call.e401(cause.message)
            }

            //其他内部错误
            exception<Throwable> { call, cause ->
                call.e500(cause.message)
            }
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
        routing {
            get("/playerInfo") {
                PlayerService.getInfo(call)
            }
            get("/sponsors") {
                call.ok("""2025-04-11,ChenQu,100""")
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
            authenticate("auth-basic") {
                post("/skin") {
                    PlayerService.changeCloth(call)
                }

                post("/profile") {
                    PlayerService.changeProfile(call)
                }
                post("/chat"){
                    ChatService.chat(call)
                }
                post("/clearSkin") {
                    PlayerService.clearCloth(call)
                }
                route("/island") {
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
                    post("/invite") {
                        RoomService.invite(call)
                    }
                    post("/invite_qq") {
                        RoomService.inviteQQ(call)
                    }
                    post("/kick") {
                        RoomService.kick(call)
                    }
                    post("/transfer") {
                        RoomService.transfer(call)
                    }
                    get("/list") {
                        RoomService.list(call)
                    }
                    post("/visit") {
                        RoomService.visit(call)
                    }

                }
            }
        }
    }.start(wait = true)
}