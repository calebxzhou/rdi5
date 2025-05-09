package calebxzhou.rdi.net.protocol

import calebxzhou.rdi.net.CPacket

data class CPlayerLeavePacket(
    val tempUid: Byte
): CPacket {
    override fun handle() {
        TODO("Not yet implemented")
    }
}