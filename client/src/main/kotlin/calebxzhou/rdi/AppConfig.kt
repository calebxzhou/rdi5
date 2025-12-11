package calebxzhou.rdi

import kotlinx.serialization.Serializable
import net.peanuuutz.tomlkt.Toml
import java.io.File

/**
 * calebxzhou @ 2025-12-11 10:23
 */
@Serializable
data class AppConfig(
    val useMirror: Boolean = true
){
    companion object {
        private val configFile = File("config.toml")
        fun load(): AppConfig {
            return if (configFile.exists()) {
                try {
                    Toml.decodeFromString(serializer(), configFile.readText())
                } catch (e: Exception) {
                    lgr.warn("read config failed, use default and save")
                    e.printStackTrace()
                    AppConfig().also { save(it) }
                }
            } else {
                AppConfig().also { save(it) }
            }
        }

        private fun save(config: AppConfig) {
            try {
                configFile.writeText(Toml.encodeToString(serializer(), config))
            } catch (e: Exception) {
                lgr.warn(e) { "save config failed" }
            }
        }
    }
}