import calebxzhou.rdi.net.RServer

suspend fun main(){
    RServer.OFFICIAL_DEBUG.prepareRequest( path = "register",
        post = true,
        params = listOf(
            "name" to "1",
            "pwd" to "123123",
            "qq" to "123123"
        )
    )
    RServer.OFFICIAL_DEBUG.prepareRequest( path = "register",
        post = true,
        params = listOf(
            "name" to "2",
            "pwd" to "456456",
            "qq" to "456456"
        )
    )
}