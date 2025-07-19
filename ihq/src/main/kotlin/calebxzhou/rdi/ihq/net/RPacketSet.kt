package calebxzhou.rdi.ihq.net

import calebxzhou.rdi.ihq.lgr
import calebxzhou.rdi.ihq.net.protocol.CAbortPacket
import calebxzhou.rdi.ihq.net.protocol.CBlockEntityUpdatePacket
import calebxzhou.rdi.ihq.net.protocol.CBlockStateChangePacket
import calebxzhou.rdi.ihq.net.protocol.CChatMessagePacket
import calebxzhou.rdi.ihq.net.protocol.CLoginOkPacket
import calebxzhou.rdi.ihq.net.protocol.CPlayerJoinPacket
import calebxzhou.rdi.ihq.net.protocol.CPlayerLeavePacket
import calebxzhou.rdi.ihq.net.protocol.CPlayerMovePacket
import calebxzhou.rdi.ihq.net.protocol.SFirmEntityAddPacket
import calebxzhou.rdi.ihq.net.protocol.SFirmEntityDelPacket
import calebxzhou.rdi.ihq.net.protocol.SMeLeavePacket
import calebxzhou.rdi.ihq.net.protocol.SMeJoinPacket
import calebxzhou.rdi.ihq.net.protocol.SMeMovePacket
import calebxzhou.rdi.ihq.net.protocol.SMeBlockEntityUpdatePacket
import calebxzhou.rdi.ihq.net.protocol.SMeBlockStateChangePacket
import calebxzhou.rdi.ihq.net.protocol.SMeChangeDimensionPacket
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
        this reg CAbortPacket::class.java
        this reg CBlockEntityUpdatePacket::class.java
        this reg CBlockStateChangePacket::class.java
        this reg CChatMessagePacket::class.java
        this reg CLoginOkPacket::class.java
        this reg CPlayerJoinPacket::class.java
        this reg CPlayerLeavePacket::class.java
        this reg CPlayerMovePacket.Pos::class.java
        this reg CPlayerMovePacket.Rot::class.java
        this reg ::SFirmEntityAddPacket
        this reg ::SFirmEntityDelPacket
        this reg ::SMeChangeDimensionPacket
        this reg ::SMeBlockEntityUpdatePacket
        this reg ::SMeBlockStateChangePacket
        this reg ::SMeJoinPacket
        this reg ::SMeLeavePacket
        this reg { SMeMovePacket.Pos(it) }
        this reg { SMeMovePacket.Rot(it) }
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