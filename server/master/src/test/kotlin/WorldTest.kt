import calebxzhou.rdi.common.anvilrw.format.AnvilReader
import java.io.File
import kotlin.test.Test

class WorldTest {
    @Test
    fun load(){
        AnvilReader(File("C:\\Users\\calebxzhou\\Documents\\coding\\rdi5\\client\\ui\\run\\mc\\versions\\695519fec0e579e94b5c40c5_1.1.5.4\\saves\\新的世界\\region\\r.0.0.mca"))
            .readRegion().chunks.filter { !it.isEmpty }.forEach { it.getNbtData()?.values?.map { println(it.toString()) } }
    }
}