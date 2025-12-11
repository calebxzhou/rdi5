import calebxzhou.rdi.model.McVersion
import calebxzhou.rdi.service.GameService
import calebxzhou.rdi.service.GameService.rewriteMirrorUrl
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
class GameDlTest {
    @Test
    fun urlReplace(){
        assertEquals(
            "https://bmclapi2.bangbang93.com/maven/net/neoforged/neoforge/21.1.216/neoforge-21.1.216-installer.jar",
            "https://maven.neoforged.net/releases/net/neoforged/neoforge/21.1.216/neoforge-21.1.216-installer.jar".rewriteMirrorUrl)
    }
    @Test
    fun dl(): Unit = runBlocking{
        GameService.downloadVersion(McVersion.V211){println(it)}
    }
    @Test
    fun installLoader(): Unit = runBlocking {
        GameService.downloadLoader(McVersion.V211,"neoforge"){println(it)}
    }
}