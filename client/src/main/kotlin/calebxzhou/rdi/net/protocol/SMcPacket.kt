package calebxzhou.rdi.net.protocol

import calebxzhou.rdi.net.SPacket
import calebxzhou.rdi.util.mc
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.PacketEncoder
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.GameProtocols
import net.minecraft.network.protocol.game.ServerGamePacketListener

class SMcPacket(
    val roomId: Int,
    val data: Packet<ServerGamePacketListener>
): SPacket {
    override fun write(buf: FriendlyByteBuf) {
        buf.writeByte(roomId)
        val encoder = mc.connection?.connection?.channel()?.pipeline()?.get("encoder") as PacketEncoder<ServerGamePacketListener>
        encoder.protocolInfo.codec().encode(buf,data)
    }
}