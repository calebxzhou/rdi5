package calebxzhou.rdi.ui2

import calebxzhou.rdi.model.RAccount
import calebxzhou.rdi.model.Room
import calebxzhou.rdi.ui2.frag.LoginFragment
import calebxzhou.rdi.ui2.frag.ProfileFragment
import calebxzhou.rdi.ui2.frag.RegisterFragment
import calebxzhou.rdi.ui2.frag.TeamFragment
import calebxzhou.rdi.util.serdesJson
import icyllis.modernui.ModernUI
import icyllis.modernui.R
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
    System.setProperty("rdi.debug", "true")
    Configurator.setRootLevel(Level.DEBUG)
    Room.now= serdesJson.decodeFromString<Room>("{\"_id\":\"68babf210ffd4cd84117a8d9\",\"name\":\"123123的房间\",\"containerId\":\"55b0d72dc93a4e4bf604b6abdc0707c910c7552063f5db8a9749fcdf408fa75b\",\"score\":0,\"centerPos\":{\"data\":[0,64,0]},\"members\":[{\"id\":\"68b314bbadaf52ddab96b5ed\",\"isOwner\":true}],\"port\":0}")
    RAccount.now = RAccount(ObjectId("68b314bbadaf52ddab96b5ed"),"123123","123123","123123")

    val frag = TeamFragment( )
        //SelectAccountFragment(RServer.now)
        //ServerFragment()
        //RoomFragment(room)
    val mui = ModernUI().apply {

        setTheme(R.style.Theme_Material3_Dark)
        theme.applyStyle(R.style.ThemeOverlay_Material3_Dark_Rust, true)
    }
    mui.run(frag)
    AudioManager.getInstance().close()
    System.gc()
}