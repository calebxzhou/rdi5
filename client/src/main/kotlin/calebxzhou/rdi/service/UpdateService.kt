package calebxzhou.rdi.service

import calebxzhou.rdi.Const
import calebxzhou.rdi.lgr
import calebxzhou.rdi.model.RServer
import calebxzhou.rdi.net.body
import calebxzhou.rdi.util.notifyOs
import calebxzhou.rdi.util.serdesJson
import com.electronwill.nightconfig.core.Config
import com.electronwill.nightconfig.toml.TomlFormat
import io.ktor.client.statement.*
import io.ktor.util.cio.*
import io.ktor.utils.io.*
import kotlinx.coroutines.runBlocking
import java.io.File
import java.util.jar.JarFile
import kotlin.system.exitProcess

object UpdateService {
    val disable = System.getProperty("rdi.noUpdate").toBoolean()
    fun gatherModIdFile( modsDir: File) : Map<String,File>{
        val idFile = hashMapOf<String, File>()
        modsDir.listFiles { it.extension == "jar" }.forEach { jarFile ->
            JarFile(jarFile).use { jar ->
                jar.getJarEntry("META-INF/mods.toml")?.let { modsTomlEntry ->
                    jar.getInputStream(modsTomlEntry).bufferedReader().use { reader ->
                        val modId = TomlFormat.instance()
                            .createParser()
                            .parse(reader.readText())
                            .get<List<Config>>("mods")
                            .first()
                            .get<String>("modId")
                        idFile += modId to jarFile
                    }
                }
            }
        }
        println("$modsDir")
        return idFile
    }
    //返回需要更新的modid列表
    suspend fun checkUpdate(modsDir: File): List<String>{
        //测试不更新
        if(disable) {
            lgr.info("已停用更新检查")
            return listOf()
        }

        val server = if (Const.DEBUG) RServer.OFFICIAL_DEBUG else RServer.OFFICIAL_NNG
        //客户端
        val clientIdFile = UpdateService.gatherModIdFile(modsDir)
        val modlist = server.prepareRequest(false, "mod-list").body
        val modsUpdate = hashMapOf<String, File>()
        //服务端
        val serverIdSize: Map<String, Long> = serdesJson.decodeFromString(modlist)
        serverIdSize.forEach { id, size ->
            clientIdFile[id]?.let { file ->
                if (file.length() != size) {
                    modsUpdate += id to file
                }
            } ?: let { modsUpdate += id to File(modsDir, "$id.jar") }
        }
        if (modsUpdate.isEmpty()) {
            return listOf()
        }

        val modsStr = modsUpdate.map { it.key }
        val modsStrDisp = modsStr.joinToString(",")
        notifyOs(
            "以下mod需要更新:${modsStrDisp}.正在更新。"
        )
        modsUpdate.forEach { (id, file) ->
            println("update ${id} -> ${file}")
            runBlocking {
                val resp = server.prepareRequest(false, "mod-file", listOf("modid" to id))
                resp.bodyAsChannel().copyTo(file.writeChannel())
            }
        }
        return modsStr

    }
    fun restart(){
        ProcessBuilder("../../../PCL/LatestLaunch.bat").start()
        exitProcess(0)
    }
}