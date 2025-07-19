import calebxzhou.rdi.model.RAccount
import calebxzhou.rdi.net.RServer
import calebxzhou.rdi.net.body
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

/**
 * calebxzhou @ 2025-07-15 17:59
 */
class RoomTest {
    val p1 = RAccount.TESTS[0]
    val p2 = RAccount.TESTS[1]
    @Test
    fun getMyRoom() {
        RAccount.now = p1
        runBlocking {
            val resp = RServer.OFFICIAL_DEBUG.prepareRequest(false,"room/my")
            println( resp.status)
            println( resp.body)
        }
        println("RTest is running")
        // 这里可以添加更多测试逻辑
    }
    @Test
    fun roomInvite(){
        RAccount.now = p1
        runBlocking {
            //首先不能邀请自己
            val resp = RServer.OFFICIAL_DEBUG.prepareRequest(false,"room/invite_qq", listOf("id" to "64b1c8f2d4f1e3a4b5c6d7e8"))

        }
        println("RTest joinRoom is running")

    }
}