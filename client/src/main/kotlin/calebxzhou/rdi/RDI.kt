package calebxzhou.rdi

import com.aayushatharva.brotli4j.Brotli4jLoader
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.fml.common.Mod
import net.neoforged.neoforge.internal.versions.neoforge.NeoForgeVersion.MOD_ID
import org.apache.logging.log4j.LogManager

val lgr = LogManager.getLogger("rdi")
@Mod("rdi")
object RDI {
    init {
        lgr.info("RDI启动中")
        Brotli4jLoader.ensureAvailability()
    }
}