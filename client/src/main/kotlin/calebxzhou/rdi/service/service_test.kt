package calebxzhou.rdi.service

import calebxzhou.rdi.lgr
import calebxzhou.rdi.model.RAccount
import calebxzhou.rdi.model.Room
import calebxzhou.rdi.model.account
import calebxzhou.rdi.model.pack.Modpack
import calebxzhou.rdi.net.server
import calebxzhou.rdi.service.CurseForgeService.toMods
import calebxzhou.rdi.util.json
import calebxzhou.rdi.util.serdesJson
import io.ktor.http.HttpMethod
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
    RAccount.now!!.jwt = PlayerService.getJwt("123123","123123")

    lgr.info(account.json)
    //val mdp = CurseForgeService.loadModpack("C:\\Users\\calebxzhou\\Downloads\\ftb-skies-2-1.9.2.zip")
    //print(mdp.manifest)
    //mdp.manifest.files.toMods().json.let { print(it) }
    test1()
    //ModpackCreate3Fragment.readConfKjs().json.let { println(it) }


    /*ModService().getFingerprintsCurseForge().exactMatches.map { it.id }.let {
        lgr.info("${it.size} found")
        ModService().getInfosCurseForge(it) }.let { File("cf.json").writeText(it.json) }
*/

    /*  val keywords = ModService().idNames.map { it.value }.toList()
    keywords.forEach {
        ModService().getInfoMcmod(it).also { lgr.info(it?.json) }
        delay(300)
    }*/
}
suspend fun test1(){
    val mp = server.makeRequest<Modpack>(
        path = "modpack/",
        method = HttpMethod.Post,
        params = mapOf("name" to "test1")
    ).data!!
    val createVersionResp = server.makeRequest<Unit>(
        path = "modpack/${mp._id}/version/1.8.4",
        method = HttpMethod.Post
    ).msg
    print(createVersionResp)

}