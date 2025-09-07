package calebxzhou.rdi.ui2

import calebxzhou.rdi.model.RAccount
import calebxzhou.rdi.model.Room
import calebxzhou.rdi.net.RServer
import calebxzhou.rdi.ui2.frag.ProfileFragment
import calebxzhou.rdi.ui2.frag.RoomFragment
import calebxzhou.rdi.ui2.frag.SelectAccountFragment
import calebxzhou.rdi.ui2.frag.ServerFragment
import calebxzhou.rdi.util.serdesJson
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

            RoomFragment(serdesJson.decodeFromString<Room>("{\"_id\":\"68babf210ffd4cd84117a8d9\",\"name\":\"123123的房间\",\"containerId\":\"55b0d72dc93a4e4bf604b6abdc0707c910c7552063f5db8a9749fcdf408fa75b\",\"score\":0,\"centerPos\":{\"data\":[0,64,0]},\"members\":[{\"id\":\"68b314bbadaf52ddab96b5ed\",\"isOwner\":true}],\"port\":0}"))
        )
    }
    AudioManager.getInstance().close()
    System.gc()
}