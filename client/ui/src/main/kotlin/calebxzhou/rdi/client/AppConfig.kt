package calebxzhou.rdi.client

import calebxzhou.rdi.CONF
import calebxzhou.rdi.common.service.ModService
import calebxzhou.rdi.lgr
import kotlinx.serialization.Serializable
import net.peanuuutz.tomlkt.Toml
import java.io.File
import java.net.Authenticator
import java.net.PasswordAuthentication

/**
 * calebxzhou @ 2025-12-11 10:23
 */
@Serializable
data class AppConfig(
    val useMirror: Boolean = true,
    //不限制
    val maxMemory: Int=0,
    val jre21Path: String?=null,
    val jre8Path: String?=null,
    val carrier: Int = 0,
    val proxyConfig: ProxyConfig?=null,
    val pinyinName: Boolean = false,
){
    companion object {
        private val configFile = File("config.toml")
        fun load(): AppConfig {
            return if (configFile.exists()) {
                try {
                    Toml.decodeFromString(serializer(), configFile.readText())

                } catch (e: Exception) {
                    lgr.warn { "read config failed, use default and save" }
                    e.printStackTrace()
                    AppConfig().also { save(it) }
                }
            } else {
                AppConfig().also { save(it) }
            }.also {
                ModService.useMirror = it.useMirror
                applyProxy(it.proxyConfig)
            }
        }

        fun save(config: AppConfig) {
            try {
                CONF = config
                ModService.useMirror = config.useMirror
                applyProxy(config.proxyConfig)
                configFile.writeText(Toml.encodeToString(serializer(), config))
            } catch (e: Exception) {
                lgr.warn(e) { "save config failed" }
            }
        }

        private fun applyProxy(proxy: ProxyConfig?) {
            val cfg = proxy ?: ProxyConfig()
            if (!cfg.enabled) {
                System.setProperty("java.net.useSystemProxies", "false")
                clearProxyProperties()
                Authenticator.setDefault(null)
                return
            }
            if (cfg.systemProxy) {
                System.setProperty("java.net.useSystemProxies", "true")
                clearProxyProperties()
                Authenticator.setDefault(null)
                return
            }
            System.setProperty("java.net.useSystemProxies", "false")
            if (cfg.host.isNotBlank() && cfg.port > 0) {
                System.setProperty("http.proxyHost", cfg.host)
                System.setProperty("http.proxyPort", cfg.port.toString())
                System.setProperty("https.proxyHost", cfg.host)
                System.setProperty("https.proxyPort", cfg.port.toString())
            } else {
                clearProxyProperties()
            }
            val usr = cfg.usr?.trim().orEmpty()
            val pwd = cfg.pwd?.trim().orEmpty()
            if (usr.isNotEmpty()) {
                Authenticator.setDefault(object : Authenticator() {
                    override fun getPasswordAuthentication(): PasswordAuthentication {
                        return PasswordAuthentication(usr, pwd.toCharArray())
                    }
                })
            } else {
                Authenticator.setDefault(null)
            }
        }

        private fun clearProxyProperties() {
            listOf(
                "http.proxyHost",
                "http.proxyPort",
                "https.proxyHost",
                "https.proxyPort"
            ).forEach { System.clearProperty(it) }
        }

    }
}
@Serializable
data class ProxyConfig(
    val enabled: Boolean = false,
    val systemProxy: Boolean = false,
    val host: String = "127.0.0.1",
    val port: Int = 10808,
    val usr: String ? =null,
    val pwd: String ? =null,
)
