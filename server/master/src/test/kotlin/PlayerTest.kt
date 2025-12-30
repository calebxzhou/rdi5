import org.bson.types.ObjectId
import kotlin.test.Test

class PlayerTest {
    @Test
    fun id(){
        print(ObjectId(ByteArray(12)))
    }
}