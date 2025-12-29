import calebxzhou.rdi.common.json
import calebxzhou.rdi.common.model.RAccount
import calebxzhou.rdi.common.serdesJson
import calebxzhou.rdi.master.ygg.YggdrasilService.gameProfile
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class YggTest {
    @Test
    fun profile(){
        val propName = serdesJson.parseToJsonElement(RAccount.DEFAULT.gameProfile.json).jsonObject["properties"]!!.jsonArray.first().jsonObject["name"]?.jsonPrimitive?.content
        assertEquals("textures",propName)
    }
}