package calebxzhou.rdi.ihq.service

import calebxzhou.rdi.ihq.exception.RequestError
import calebxzhou.rdi.ihq.lgr
import calebxzhou.rdi.ihq.net.e404
import calebxzhou.rdi.ihq.net.err
import calebxzhou.rdi.ihq.net.initGetParams
import calebxzhou.rdi.ihq.net.response
import calebxzhou.rdi.ihq.util.serdesJson
import calebxzhou.rdi.ihq.util.sha1
import io.ktor.server.application.ApplicationCall
import io.ktor.http.ContentType
import io.ktor.server.response.respondFile
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import kotlinx.serialization.Serializable
import net.peanuuutz.tomlkt.Toml
import java.io.File
import java.security.MessageDigest
import java.util.jar.JarFile

fun Route.updateRoutes() = route("/update"){
    get("/hash"){
        response(data= UpdateService.coreSha1)
    }
    get("/core"){
        val file = UpdateService.coreFile
        if(!file.exists()) throw RequestError("客户端rdi核心维护中")
        call.respondFile(file)
    }
}
object UpdateService {

    val coreFile
        get() = File("rdi-5-client.jar")
    val coreSha1
        get() = coreFile.sha1



}