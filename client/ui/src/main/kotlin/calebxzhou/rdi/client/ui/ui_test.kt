package calebxzhou.rdi.client.ui

import calebxzhou.rdi.client.Const
import calebxzhou.rdi.RDI
import calebxzhou.rdi.common.model.RAccount
import calebxzhou.rdi.client.net.loggedAccount
import calebxzhou.rdi.client.service.PlayerService
import calebxzhou.rdi.client.ui.frag.ProfileFragment
import org.bson.types.ObjectId

/**
 * calebxzhou @ 8/23/2025 2:46 PM
 */

//public static SpectrumGraph sSpectrumGraph;
suspend fun main() {
    Const.DEBUG=true
    //System.setProperty("rdi.modDir", "C:\\Users\\calebxzhou\\Documents\\RDI5sea-Ref\\.minecraft\\versions\\rdi55a10\\modss")
    System.setProperty("rdi.coreJar", "C:\\Users\\calebxzhou\\Documents\\RDI5sea-Ref\\.minecraft\\versions\\RDI5.5\\mods\\rdi-5-client.jar")
    System.getProperty("logging.level.root","DEBUG")

    loggedAccount = RAccount(ObjectId("68b314bbadaf52ddab96b5ed"),"123123","123123","123123")
    loggedAccount.jwt = PlayerService.getJwt("123123","123@@@")

    val frag = ProfileFragment()//GameRulesFragment(mutableMapOf())//ModpackInfoFragment (ObjectId("695519fec0e579e94b5c40c5"))
        //HostCreateFragment(ObjectId(),"测试测试测试","1.0",true)
        //SelectAccountFragment(RServer.now)
        //ServerFragment()
        //RoomFragment(room)
    RDI().start(frag)
}