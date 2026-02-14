import calebxzhou.rdi.master.service.PlayerService
import kotlinx.coroutines.runBlocking
import org.bson.types.ObjectId
import java.util.UUID
import kotlin.test.Test

class PlayerTest {
    @Test
    fun id()= runBlocking{
        print(PlayerService.getByMsid(UUID.fromString("6400b138-3da9-4780-8540-bb212f487aa2")))
    }
}