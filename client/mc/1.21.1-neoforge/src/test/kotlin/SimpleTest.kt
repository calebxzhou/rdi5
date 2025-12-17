import calebxzhou.rdi.client.mc.GetVersionIdCommand
import calebxzhou.rdi.client.mc.PingCommand
import calebxzhou.rdi.client.mc.RDI
import calebxzhou.rdi.client.mc.RDI.Companion.send
import calebxzhou.rdi.client.mc.serdesJson
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeAll
import kotlin.test.Test
import kotlin.test.assertEquals

class SimpleTest {
    companion object{
        @JvmStatic
        @BeforeAll
        fun setup() {
            System.setProperty("rdi.ihq.url", "http://127.0.0.1:65231")
            System.setProperty("rdi.game.ip", "127.0.0.1:65232")
            System.setProperty("rdi.host.name", "测试测试12123主机")
            System.setProperty("rdi.host.port", "55000")
        }
    }
    @Test
    fun commandType(){
    }
    @Test
    fun serialize(){
        assertEquals(serdesJson.decodeFromString<GetVersionIdCommand>("""{"reqId":100}""").reqId,100)
    }
    @Test
    fun ws():Unit= runBlocking{
        RDI.connectCore()
        PingCommand().send()
        PingCommand().send()
        PingCommand().send()
        PingCommand().send()
        PingCommand().send()
    }
}