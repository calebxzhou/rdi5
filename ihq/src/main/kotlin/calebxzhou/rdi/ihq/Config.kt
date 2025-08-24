package calebxzhou.rdi.ihq

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.Serializable
import net.peanuuutz.tomlkt.Toml
import java.io.File

@Serializable
data class DatabaseConfig(
    val host: String = "127.0.0.1",
    val port: Int = 27017,
    val name: String = "rdi5"
)

@Serializable
data class ServerConfig(
    val port: Int = 28511
)

@Serializable
data class AppConfig(
    val database: DatabaseConfig = DatabaseConfig(),
    val server: ServerConfig = ServerConfig()
) {
    companion object {
        private val configFile = File("config.toml")
        private val lgr = KotlinLogging.logger {  }
        fun load(): AppConfig {
            return if (configFile.exists()) {
                lgr.info { "find config.toml, loading" }
                try {
                    Toml.decodeFromString(serializer(), configFile.readText())
                } catch (e: Exception) {
                    lgr.warn(e) { "read config failed, use default and save" }
                    AppConfig().also { save(it) }
                }
            } else {
                lgr.info { "config.toml not find, create default" }
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
