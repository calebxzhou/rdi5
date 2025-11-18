package calebxzhou.rdi.ui2

import calebxzhou.rdi.Const
import calebxzhou.rdi.model.Host
import calebxzhou.rdi.model.RAccount
import calebxzhou.rdi.model.Team
import calebxzhou.rdi.net.server
import calebxzhou.rdi.service.PlayerService
import calebxzhou.rdi.ui2.frag.HostListFragment
import calebxzhou.rdi.ui2.frag.HostModFragment
import calebxzhou.rdi.ui2.frag.ModpackListFragment
import calebxzhou.rdi.ui2.frag.ModpackUploadFragment
import calebxzhou.rdi.ui2.frag.ProfileFragment
import calebxzhou.rdi.ui2.frag.pack.ModpackCreate3Fragment
import calebxzhou.rdi.ui2.frag.pack.ModpackCreateFragment
import calebxzhou.rdi.util.serdesJson
import icyllis.modernui.R
import icyllis.modernui.audio.AudioManager
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.core.config.Configurator
import org.bson.types.ObjectId

/**
 * calebxzhou @ 8/23/2025 2:46 PM
 */

//public static SpectrumGraph sSpectrumGraph;
suspend fun main() {

    System.setProperty("java.awt.headless", "true")
    Const.DEBUG=true
    System.setProperty("rdi.modDir", "C:\\Users\\calebxzhou\\Documents\\RDI5sea-Ref\\.minecraft\\versions\\rdi55a10\\modss")
    Configurator.setRootLevel(Level.DEBUG)

     RAccount.now = RAccount(ObjectId("68b314bbadaf52ddab96b5ed"),"123123","123123","123123")
    RAccount.now!!.jwt = PlayerService.getJwt("123123","123123")
    val team = server.makeRequest<Team>(
        "/team/").data!!
    val firstHost = server.makeRequest<List<Host>>("host/" ).data!!.first()
    val frag = ProfileFragment()
        //SelectAccountFragment(RServer.now)
        //ServerFragment()
        //RoomFragment(room)

    val mui =RodernUI().apply {


        setTheme(R.style.Theme_Material3_Dark)
        theme.applyStyle(R.style.ThemeOverlay_Material3_Dark_Rust, true)
    }

   // mui::class.java.getDeclaredField("mDefaultTypeface").apply { isAccessible=true }.set(mui, Fonts.UI.typeface)
    mui.run(frag)
    AudioManager.getInstance().close()
    System.gc()
}