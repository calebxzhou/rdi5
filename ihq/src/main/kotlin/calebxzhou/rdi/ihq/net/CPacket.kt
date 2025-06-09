package calebxzhou.rdi.ihq.net

interface CPacket {
    fun write(buf: RByteBuf)
}