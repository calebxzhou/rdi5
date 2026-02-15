package calebxzhou.rdi

import calebxzhou.rdi.client.AppConfig
import io.github.oshai.kotlinlogging.KotlinLogging

/**
 * Global configuration and logger accessible from commonMain.
 * Desktop: initialized by RDI.kt main().
 * Android: initialized by MainActivity.
 */
var CONF = AppConfig()
val lgr = KotlinLogging.logger("RDI")
