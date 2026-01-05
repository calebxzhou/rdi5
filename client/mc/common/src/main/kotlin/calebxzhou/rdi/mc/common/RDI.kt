package calebxzhou.rdi.mc.common

import java.util.UUID

object RDI {
    init {

    }
    @JvmField
    val IHQ_URL =
        System.getProperty("rdi.ihq.url") ?: throw IllegalArgumentException("启动方式错误：找不到服务器地址1")

    @JvmField
    val GAME_IP =
        System.getProperty("rdi.game.ip") ?: throw IllegalArgumentException("启动方式错误：找不到服务器地址2")

    @JvmField
    val HOST_NAME =
        System.getProperty("rdi.host.name") ?: throw IllegalArgumentException("启动方式错误：找不到主机名")

    @JvmField
    var HOST_PORT = System.getProperty("rdi.host.port")?.toInt()
        ?: throw IllegalArgumentException("启动方式错误：找不到服务器端口")
    @JvmStatic
    fun getProfileTextureQueryUrl(uuid: UUID): String {
        return "${IHQ_URL}/mc-profile/${uuid}/clothes"
    }
}