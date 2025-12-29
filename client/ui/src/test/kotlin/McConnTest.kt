import calebxzhou.rdi.client.common.protocol.AddChatMsgCommand
import calebxzhou.rdi.mc.send
import calebxzhou.rdi.mc.startLocalServer
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class McConnTest {
    companion object{
        @BeforeAll
        @JvmStatic
        fun setup(){

        }
    }
    @Test
    fun testPing(){
        startLocalServer(true)
        print("ok")
    }
    @Test
    fun addChat(){
        AddChatMsgCommand("").send()
        print("ok")
    }
}