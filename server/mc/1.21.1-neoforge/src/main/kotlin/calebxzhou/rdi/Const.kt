package calebxzhou.rdi

object Const {
    val DEBUG = System.getProperty("rdi.debug").toBoolean()
    @JvmField
    val SERVER_PORT = System.getenv("GAME_PORT")?.toIntOrNull()?:System.getProperty("rdi.port")?.toIntOrNull()?:65232
    val TEST_HOST_ID = "691bf5ef6cf16c0ae4224c31"


}
