package calebxzhou.rdi.net.protocol

import calebxzhou.rdi.lgr
import calebxzhou.rdi.net.CPacket
import calebxzhou.rdi.net.GameNetClient
import calebxzhou.rdi.net.RByteBuf

data class CAbortPacket(
    val reason: String,
): CPacket {
    constructor(buf: RByteBuf):this(
        reason = buf.readUtf()
    )

    override fun handle() {
        lgr.info("断开连接 原因: $reason")
        GameNetClient.disconnect()
    }
}