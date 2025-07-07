package calebxzhou.rdi.ihq.net

import calebxzhou.rdi.ihq.lgr
import calebxzhou.rdi.ihq.net.protocol.SMeLeavePacket
import calebxzhou.rdi.ihq.net.protocol.SMeJoinPacket
import calebxzhou.rdi.ihq.net.protocol.SMeMovePacket
import calebxzhou.rdi.ihq.net.protocol.SMeBlockEntityUpdatePacket
import calebxzhou.rdi.ihq.net.protocol.SMeBlockStateChangePacket
import io.netty.buffer.ByteBuf

typealias PacketReader = (ByteBuf) -> SPacket
typealias PacketWriter = Class<out CPacket>

object RPacketSet {


    private var packCount = 0.toByte()

    //c2s
    private val id2Reader = linkedMapOf<Byte, PacketReader>()

    //s2c
    private val writer2id = linkedMapOf<PacketWriter, Byte>()

    init {
        this reg ::SMeJoinPacket
        this reg ::SMeLeavePacket
        this reg { SMeMovePacket.Pos(it) }
        this reg { SMeMovePacket.Rot(it) }
        this reg ::SMeBlockEntityUpdatePacket
        this reg ::SMeBlockStateChangePacket
    }

    private infix fun reg(reader: PacketReader) {
        id2Reader += packCount to reader
        packCount++
    }

    private infix fun reg(writer: PacketWriter) {
        writer2id += writer to packCount
        packCount++
    }

    fun create(packerId: Byte, data: ByteBuf): SPacket? = id2Reader[packerId]?.let { reader ->
        val packet = reader(data)
        data.readerIndex(data.readerIndex() + data.readableBytes())
        packet
    } ?: let {
        lgr.error("找不到ID${packerId}的包")
        return null
    }


    fun getPacketId(packetClass: PacketWriter): Byte? = writer2id[packetClass]
}