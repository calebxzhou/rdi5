package calebxzhou.rdi.service

import calebxzhou.rdi.lgr
import calebxzhou.rdi.model.CurseForgeFileResponse
import calebxzhou.rdi.model.RAccount
import calebxzhou.rdi.model.account
import calebxzhou.rdi.model.pack.Mod
import calebxzhou.rdi.model.pack.Modpack
import calebxzhou.rdi.net.server
import calebxzhou.rdi.service.CurseForgeService.cfreq
import calebxzhou.rdi.service.CurseForgeService.getDownloadUrl
import calebxzhou.rdi.service.CurseForgeService.toMods
import calebxzhou.rdi.util.json
import calebxzhou.rdi.util.serdesJson
import io.ktor.client.call.body
import io.ktor.client.statement.bodyAsText
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
     RAccount.now = RAccount(ObjectId("68b314bbadaf52ddab96b5ed"),"123123","123123","123123")
    RAccount.now!!.jwt = PlayerService.getJwt("123123","123123")
   // print(CurseForgeService.getModsInfo(listOf(233105)))
    /*cfreq(
        "mods/files",
        HttpMethod.Post,
        CurseForgeService.CFFileIdsRequest(listOf(7037478))
    ).bodyAsText().let { print(it) }*/
    cfreq("mods/search?slug=mouse-tweaks").bodyAsText().let { print(it) }
        //Mod("cf","1199550","better-replication-pipes","6188683","1370081689").getDownloadUrl().let { print(it) }
      //val mdp = CurseForgeService.loadModpack("C:\\Users\\calebxzhou\\Downloads\\ftb-skies-2-1.9.2.zip")
    //print(mdp.manifest)
    //mdp.manifest.files.toMods().json.let { print(it) }
   // test1()
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
suspend fun testRebuild(){
    server.makeRequest<Unit>("modpack/69194653663d20f0e88fb268/version/1.9.2/rebuild", method = HttpMethod.Post){}
}
suspend fun testCreateModpackVersion(){
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