package calebxzhou.rdi.master.service

import calebxzhou.rdi.master.exception.RequestError
import calebxzhou.rdi.master.net.response
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.io.File
import java.util.jar.JarFile

private val coreFile
    get() = File("rdi-5-client.jar")
fun Route.updateRoutes() = route("/update") {
    get("/ver") {
        if (!coreFile.exists()) throw RequestError("客户端rdi核心维护中")
        val version = JarFile(coreFile).use { jar ->
            jar.manifest?.mainAttributes?.getValue("Implementation-Version")
        }?.takeIf { it.isNotBlank() }
            ?: throw RequestError("无法读取客户端版本号")
        response(data = mapOf("version" to version))
    }
    get("/core") {
        if (!coreFile.exists()) throw RequestError("客户端rdi核心维护中")
        call.respondFile(coreFile)
    }
}