import calebxzhou.rdi.client.mc.AddChatMsgCommand
import calebxzhou.rdi.client.mc.ChangeHostCommand
import calebxzhou.rdi.client.mc.GetVersionIdCommand
import calebxzhou.rdi.client.mc.json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class SimpleTest {
    @Test
    fun commandType(){
        assertEquals(AddChatMsgCommand("").type,"add_gui_msg")
        assertNotEquals(ChangeHostCommand(0).type,"changehost")
    }
    @Test
    fun serialize(){
        assertEquals(json.decodeFromString<GetVersionIdCommand>("""{"reqId":100}""").reqId,100)
    }
}