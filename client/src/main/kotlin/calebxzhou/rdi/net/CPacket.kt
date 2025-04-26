package calebxzhou.rdi.net

import net.minecraft.network.FriendlyByteBuf

interface CPacket {
    fun handle()
}
