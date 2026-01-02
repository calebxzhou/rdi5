package calebxzhou.rdi.prox

import io.github.oshai.kotlinlogging.KotlinLogging


val lgr = KotlinLogging.logger {  }

/**
 * calebxzhou @ 8/31/2025 5:20 PM
 */
fun main() {
    // Start the basic proxy with binary control packet support
    val proxy = TcpReverseProxy()
    proxy.start("0.0.0.0", Const.SERVER_PORT, "127.0.0.1", 25565)
}