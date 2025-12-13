import calebxzhou.rdi.lgr
import calebxzhou.rdi.model.RAccount
import calebxzhou.rdi.model.pack.Modpack
import calebxzhou.rdi.net.server
import calebxzhou.rdi.service.CurseForgeService
import calebxzhou.rdi.service.CurseForgeService.mapMods
import calebxzhou.rdi.service.PlayerService
import calebxzhou.rdi.util.json
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import org.bson.types.ObjectId
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import kotlin.test.assertNotNull

class ModpackTest {

	companion object {
		private const val TEST_USER = "123123"
		private const val TEST_PASSWORD = "123123"
		private val TEST_ACCOUNT_ID = ObjectId("68b314bbadaf52ddab96b5ed")

		@JvmStatic
		@BeforeAll
		fun setUpAccount() = runBlocking {
			System.setProperty("rdi.debug", "true")
			System.setProperty("rdi.modDir", "C:\\Users\\calebxzhou\\Documents\\RDI5sea-Ref\\.minecraft\\versions\\ATM10 To the Sky\\mods")
            System.setProperty("logging.level.root","DEBUG")
			val account = RAccount(TEST_ACCOUNT_ID, TEST_USER, TEST_PASSWORD, TEST_USER)
			account.jwt = PlayerService.getJwt(TEST_USER, TEST_PASSWORD)
			RAccount.now = account
		}
	}

	@Test
	@Disabled("Integration test that requires a running backend instance")
	fun rebuildModpackVersion(): Unit = runBlocking {
		server.makeRequest<Unit>(
			path = "modpack/69194653663d20f0e88fb268/version/1.9.2/rebuild",
			method = HttpMethod.Post
		)
	}

	@Test
	@Disabled("Integration test that creates a modpack on the backend")
	fun createModpackVersion(): Unit = runBlocking {
		val modpack = server.makeRequest<Modpack>(
			path = "modpack/",
			method = HttpMethod.Post,
			params = mapOf("name" to "test1")
		).data!!

		val response = server.makeRequest<Unit>(
			path = "modpack/${'$'}{modpack._id}/version/1.8.4",
			method = HttpMethod.Post
		).msg

		assertNotNull(response)
	}

	@Test
	fun loadCurseForgeModpack(): Unit = runBlocking {
		val zipPath = "C:\\Users\\calebxzhou\\Downloads\\ftb-skies-2-1.9.2.zip"
		val modpackData = CurseForgeService.loadModpack(zipPath)
        print(modpackData.manifest.files.mapMods().json)
		assertNotNull(modpackData.manifest)
	}
    @Test
    fun cfModFile(){
        runBlocking {
            CurseForgeService.getModFileInfo(351264,5402061)?.let { print(it.json) }
        }
    }
}