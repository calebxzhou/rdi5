package calebxzhou.rdi.net

import net.minecraft.network.FriendlyByteBuf


interface SPacket {
    //写数据进FriendlyByteBuf
    fun write(buf: RByteBuf)
}