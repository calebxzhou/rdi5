package calebxzhou.rdi.service

import calebxzhou.rdi.lgr
import calebxzhou.rdi.net.httpStringRequest
import calebxzhou.rdi.util.sha1
import com.electronwill.nightconfig.core.Config
import com.electronwill.nightconfig.toml.TomlFormat
import sun.tools.jar.resources.jar
import java.io.File
import java.util.jar.JarFile
import kotlin.collections.first
import kotlin.collections.plusAssign
import kotlin.io.extension

object ModService {
    val MOD_DIR = System.getProperty("rdi.updateModDir")?.let { File(it) } ?: File("mods")
    fun File.forEachMod(action: (File, JarFile) -> Unit) {
        allMods.forEach { jarFile ->
            JarFile(jarFile).use { jar ->
                action(jarFile, jar)
            }
        }
    }
    private val File.allMods
        get() = listFiles { it.extension == "jar" }?.toList()?:listOf()
    //all mod id to file
    fun gatherModIdFile(modsDir: File): Map<String, File> {
        val idFile = hashMapOf<String, File>()
        modsDir.forEachMod { f,jarFile ->
            try {
                jarFile.getJarEntry("META-INF/neoforge.mods.toml")?.let { modsTomlEntry ->
                        //解析toml
                    jarFile.getInputStream(modsTomlEntry).bufferedReader().use { reader ->
                            val modId = TomlFormat.instance()
                                .createParser()
                                .parse(reader.readText())
                                .get<List<Config>>("mods")
                                .first()
                                .get<String>("modId")
                            idFile += modId to f
                        }
                    }

            } catch (e: Exception) {
                // 如果mod文件损坏，记录日志并跳过
                lgr.warn("mod文件损坏，直接跳过: ${jarFile.name}", e)
            }
        }
        lgr.info("mod安装目录：$modsDir")
        return idFile
    }

    //获取mod id to sha1的映射
    fun gatherModIdSha1(modsDir: File): Map<String, String> {
        val idSha1 = hashMapOf<String, String>()
        modsDir.listFiles { it.extension == "jar" }?.forEach { jarFile ->
            try {
                JarFile(jarFile).use { jar ->
                    jar.getJarEntry("META-INF/neoforge.mods.toml")?.let { modsTomlEntry ->
                        //解析toml
                        jar.getInputStream(modsTomlEntry).bufferedReader().use { reader ->
                            val modId = TomlFormat.instance()
                                .createParser()
                                .parse(reader.readText())
                                .get<List<Config>>("mods")
                                .first()
                                .get<String>("modId")
                            idSha1 += modId to jarFile.sha1
                        }
                    }
                }
            } catch (e: Exception) {
                // 如果mod文件损坏，使用特殊的SHA-1值表示损坏状态
                lgr.warn("mod文件损坏，无法计算SHA-1: ${jarFile.name}", e)
                val tempModId = jarFile.nameWithoutExtension
                // 使用特殊值表示文件损坏，确保与服务端SHA-1不匹配
                idSha1 += tempModId to "corrupted_file_${System.currentTimeMillis()}"
            }
        }
        return idSha1
    }

    suspend fun getInfosModrinth(modsDir: File){
        modsDir.allMods.map { it.sha1 }
        httpStringRequest(true,"https://api.modrinth.com/v2/version_files",listOf(
            "hashes" to modsDir.allMods.map { it.sha1 },
            "algorithm" to "sha1"
        ))
        //todo json requesting

    }
}