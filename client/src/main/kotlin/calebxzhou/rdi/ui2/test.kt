package calebxzhou.rdi.ui2

import calebxzhou.rdi.model.RAccount
import calebxzhou.rdi.net.RServer
import calebxzhou.rdi.ui2.frag.ProfileFragment
import calebxzhou.rdi.ui2.frag.ServerFragment
import icyllis.modernui.ModernUI
import icyllis.modernui.TestFragment
import icyllis.modernui.audio.AudioManager
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.core.config.Configurator
import org.bson.types.ObjectId

/**
 * calebxzhou @ 8/23/2025 2:46 PM
 */

//public static SpectrumGraph sSpectrumGraph;
fun main() {
    System.setProperty("java.awt.headless", "true")
    Configurator.setRootLevel(Level.DEBUG)

    RAccount.now = RAccount(ObjectId("68b314bbadaf52ddab96b5ed"),"123123","123123","123123")
    ModernUI().use { app ->
        app.run(

            ServerFragment(RServer.OFFICIAL_DEBUG)
        )
    }
    AudioManager.getInstance().close()
    System.gc()
}