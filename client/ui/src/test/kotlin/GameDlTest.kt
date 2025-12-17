import calebxzhou.rdi.model.McVersion
import calebxzhou.rdi.model.ModLoader
import calebxzhou.rdi.model.pack.Modpack
import calebxzhou.rdi.service.GameService
import calebxzhou.rdi.service.GameService.rewriteMirrorUrl
import calebxzhou.rdi.service.ModpackService
import kotlinx.coroutines.runBlocking
import org.bson.types.ObjectId
import kotlin.test.Test
import kotlin.test.assertEquals

class GameDlTest {
    @Test
    fun urlReplace() {
        assertEquals(
            "https://bmclapi2.bangbang93.com/maven/net/neoforged/neoforge/21.1.216/neoforge-21.1.216-installer.jar",
            "https://maven.neoforged.net/releases/net/neoforged/neoforge/21.1.216/neoforge-21.1.216-installer.jar".rewriteMirrorUrl
        )
    }

    @Test
    fun dl21(): Unit = runBlocking {

        GameService.downloadVersion(McVersion.V211) { println(it) }
    }

    @Test
    fun dl20(): Unit = runBlocking {

        GameService.downloadVersion(McVersion.V201) { println(it) }
    }
    @Test
    fun start21(): Unit = runBlocking {
        GameService.start(McVersion.V211, "neoforge-21.1.216") { println(it) }
    }
    @Test
    fun start20(): Unit = runBlocking {
        GameService.start(McVersion.V201, "1.20.1-forge-47.4.13") { println(it) }
    }
    @Test
    fun startModpack(): Unit = runBlocking {
        GameService.start(McVersion.V211, "693bda0ed294de2450aa7caf_1.9.2") { println(it) }
    }
    @Test
    fun installLoader21(): Unit = runBlocking {
        GameService.downloadLoader(McVersion.V211, ModLoader.NEOFORGE) { println(it) }
    }

    @Test
    fun installLoader20(): Unit = runBlocking {
        GameService.downloadLoader(McVersion.V201, ModLoader.FORGE) { println(it) }
    }

}