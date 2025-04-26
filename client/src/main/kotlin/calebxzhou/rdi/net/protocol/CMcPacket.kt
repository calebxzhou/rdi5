package calebxzhou.rdi.net.protocol

import calebxzhou.rdi.mixin.APacketDecoder
import calebxzhou.rdi.net.RByteBuf
import calebxzhou.rdi.util.mc
import net.minecraft.network.PacketDecoder
import net.minecraft.network.PacketEncoder
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ClientGamePacketListener
import net.minecraft.network.protocol.game.ServerGamePacketListener

class CMcPacket(
    val data: Packet<*>
) {
    constructor(buf: RByteBuf): this(
        (mc.connection?.connection?.channel()?.pipeline()?.get("decoder") as APacketDecoder<ClientGamePacketListener>).protocolInfo.codec().decode(buf))
}