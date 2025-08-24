package calebxzhou.rdi.ui2

import calebxzhou.rdi.ui2.frag.ProfileFragment
import icyllis.modernui.ModernUI
import icyllis.modernui.TestFragment
import icyllis.modernui.audio.AudioManager
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.core.config.Configurator

/**
 * calebxzhou @ 8/23/2025 2:46 PM
 */

//public static SpectrumGraph sSpectrumGraph;
fun main() {
    System.setProperty("java.awt.headless", "true")
    Configurator.setRootLevel(Level.DEBUG)

    ModernUI().use { app ->
        app.run(ProfileFragment())
    }
    AudioManager.getInstance().close()
    System.gc()
}