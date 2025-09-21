package calebxzhou.rdi.ihq.service

import calebxzhou.rdi.ihq.lgr
import calebxzhou.rdi.ihq.net.e404
import calebxzhou.rdi.ihq.net.initGetParams
import calebxzhou.rdi.ihq.net.response
import calebxzhou.rdi.ihq.util.serdesJson
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respondFile
import kotlinx.serialization.Serializable
import net.peanuuutz.tomlkt.Toml
import java.io.File
import java.security.MessageDigest
import java.util.jar.JarFile

@Serializable
data class ModsToml(
    val mods: List<ModInfo> = emptyList()
)
@Serializable
data class ModInfo(
    val modId: String,
    val version: String
)

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
                        try {
                            val modsToml = Toml.decodeFromString(ModsToml.serializer(), reader.readText())
                            modsToml.mods.firstOrNull()?.let {
                                modIdSha1[it.modId] = calculateSha1(jarFile)
                                modIdFile[it.modId] = jarFile
                            }
                        }catch (e:Exception){
                            lgr.warn(e){"fail to parse ${jarFile.name}"}
                        }
                    }
                }
            }
        }
        lgr.info { "OK ${modIdFile.size}个mod" }
    }


    suspend fun getModList(call: ApplicationCall) {
        call.response(serdesJson.encodeToString(modIdSha1))
    }

    suspend fun getModFile(call: ApplicationCall) {
        call.initGetParams()["modid"]?.let { modid ->
            modIdFile[modid]?.let {
                call.respondFile(it)
            }
        } ?: call.e404()

    }

}