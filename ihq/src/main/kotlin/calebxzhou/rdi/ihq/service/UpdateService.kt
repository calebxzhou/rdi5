package calebxzhou.rdi.ihq.service

import calebxzhou.rdi.ihq.lgr
import calebxzhou.rdi.ihq.net.e404
import calebxzhou.rdi.ihq.net.initGetParams
import calebxzhou.rdi.ihq.net.ok
import calebxzhou.rdi.ihq.util.serdesJson
import com.electronwill.nightconfig.core.Config
import com.electronwill.nightconfig.toml.TomlFormat
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respondFile
import java.io.File
import java.security.MessageDigest
import java.util.jar.JarFile

object UpdateService {
    val MODS_DIR = File("mods")
    val modIdSha1 = hashMapOf<String, String>()
    val modIdFile = hashMapOf<String, File>()

    private fun calculateSha1(file: File): String {
        val digest = MessageDigest.getInstance("SHA-1")
        file.inputStream().use { input ->
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (input.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    fun reloadModInfo() {
        lgr.info { "重载mod列表" }
        modIdSha1.clear()
        modIdFile.clear()
        MODS_DIR.listFiles { it.extension == "jar" }?.forEach { jarFile ->
            JarFile(jarFile).use { jar ->
                jar.getJarEntry("META-INF/neoforge.mods.toml")?.let { modsTomlEntry ->


                    jar.getInputStream(modsTomlEntry).bufferedReader().use { reader ->
                        // Parse the TOML content using NightConfig
                        val config: Config = TomlFormat.instance().createParser().parse(reader.readText())
                        val modsArray = config.get<List<Config>>("mods")
                        val firstMod = modsArray.first()
                        val modId = firstMod.get<String>("modId")
                        val version = firstMod.get<String>("version")
                        modIdSha1 += modId to calculateSha1(jarFile)
                        modIdFile += modId to jarFile
                        //Pair(modId, jarFile.length())
                    }
                }
            }
        }
        lgr.info { "OK ${modIdFile.size}个mod" }
    }


    suspend fun getModList(call: ApplicationCall) {
        call.ok(serdesJson.encodeToString(modIdSha1))
    }

    suspend fun getModFile(call: ApplicationCall) {
        call.initGetParams()["modid"]?.let { modid ->
            modIdFile[modid]?.let {
                call.respondFile(it)
            }
        } ?: call.e404()

    }

}