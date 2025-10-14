package calebxzhou.rdi.service

import calebxzhou.rdi.lgr
import calebxzhou.rdi.model.RAccount
import calebxzhou.rdi.model.Room
import calebxzhou.rdi.util.json
import calebxzhou.rdi.util.serdesJson
import kotlinx.coroutines.delay
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.core.config.Configurator
import org.bson.types.ObjectId

/**
 * calebxzhou @ 8/23/2025 7:30 PM
 */
suspend fun main() {
    System.setProperty("rdi.debug", "true")
    System.setProperty("rdi.modDir", "C:\\Users\\calebxzhou\\Documents\\RDI5sea-Ref\\.minecraft\\versions\\ATM10 To the Sky\\mods")
    Configurator.setRootLevel(Level.DEBUG)
    Room.now= serdesJson.decodeFromString<Room>("{\"_id\":\"68babf210ffd4cd84117a8d9\",\"name\":\"123123的房间\",\"containerId\":\"55b0d72dc93a4e4bf604b6abdc0707c910c7552063f5db8a9749fcdf408fa75b\",\"score\":0,\"centerPos\":{\"data\":[0,64,0]},\"members\":[{\"id\":\"68b314bbadaf52ddab96b5ed\",\"isOwner\":true}],\"port\":0}")
    RAccount.now = RAccount(ObjectId("68b314bbadaf52ddab96b5ed"),"123123","123123","123123")

  /*  val keywords = ModService.idNames.map { it.value }.toList()
    keywords.forEach {
        ModService.getInfoMcmod(it).also { lgr.info(it?.json) }
        delay(300)
    }*/
}