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
data class McsmConfig(
    val host: String = "127.0.0.1",
    val port: Int = 23333,
    val apiKey: String = "c1354eff4a604de39e67b550df6ed607",
    val daemonId: String = "87f1a66a45ec4326a7ad85bebebc77ea",
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
data class AppConfig(
    val database: DatabaseConfig = DatabaseConfig(),
    val server: ServerConfig = ServerConfig(),
    val mcsm: McsmConfig = McsmConfig(),
    val docker: DockerConfig = DockerConfig(),
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
