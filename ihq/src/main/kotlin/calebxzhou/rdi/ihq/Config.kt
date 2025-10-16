package calebxzhou.rdi.ihq

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.Serializable
import net.peanuuutz.tomlkt.Toml
import java.io.File

@Serializable
data class DatabaseConfig(
    val host: String = "127.0.0.1",
    val port: Int = 27017,
    val name: String = "rdi5skypro"
)

@Serializable
data class ServerConfig(
    val port: Int = 65231
)

@Serializable
data class ProxyConfig(
    val host: String = "127.0.0.1",
    val port: Int = 23333,
)

@Serializable
data class DockerConfig(
    val host: String = "127.0.0.1",
    val port: Int = 2376,
    val tlsEnabled: Boolean = false,
    val tlsVerify: Boolean = false,
    val certPath: String = "",
    val keyPath: String = "",
    val caPath: String = "",
    val apiVersion: String = "1.41"
)
@Serializable
data class ApiKeyConfig(
    val curseforge: String = "",

)
@Serializable
data class AppConfig(
    val database: DatabaseConfig = DatabaseConfig(),
    val server: ServerConfig = ServerConfig(),
    val proxy: ProxyConfig = ProxyConfig(),
    val docker: DockerConfig = DockerConfig(),
    val apiKey: ApiKeyConfig = ApiKeyConfig(),
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
