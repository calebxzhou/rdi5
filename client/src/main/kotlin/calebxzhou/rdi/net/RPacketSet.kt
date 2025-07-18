package calebxzhou.rdi.net

import calebxzhou.rdi.lgr
import calebxzhou.rdi.net.protocol.CAbortPacket
import calebxzhou.rdi.net.protocol.CBlockEntityUpdatePacket
import calebxzhou.rdi.net.protocol.CBlockStateChangePacket
import calebxzhou.rdi.net.protocol.CChatMessagePacket
import calebxzhou.rdi.net.protocol.CLoginOkPacket
import calebxzhou.rdi.net.protocol.CPlayerJoinPacket
import calebxzhou.rdi.net.protocol.CPlayerLeavePacket
import calebxzhou.rdi.net.protocol.CPlayerMovePacket
import calebxzhou.rdi.net.protocol.SFirmEntityAddPacket
import calebxzhou.rdi.net.protocol.SFirmEntityDelPacket
import calebxzhou.rdi.net.protocol.SMeLeavePacket
import calebxzhou.rdi.net.protocol.SMeJoinPacket
import calebxzhou.rdi.net.protocol.SMeMovePacket
import calebxzhou.rdi.net.protocol.SMeBlockEntityUpdatePacket
import calebxzhou.rdi.net.protocol.SMeBlockStateChangePacket
import calebxzhou.rdi.net.protocol.SMeChangeDimensionPacket
import io.netty.buffer.ByteBuf
import kotlin.jvm.java

typealias PacketReader = (RByteBuf) -> CPacket
typealias PacketWriter = Class<out SPacket>

object RPacketSet {

    private var packCount = 0.toByte()

    //s2c
    private val id2Reader = linkedMapOf<Byte, PacketReader>()

    //c2s
    private val writer2id = linkedMapOf<PacketWriter, Byte>()

    init {

        this reg ::CAbortPacket
        this reg ::CBlockEntityUpdatePacket
        this reg ::CBlockStateChangePacket
        this reg ::CChatMessagePacket
        this reg ::CLoginOkPacket
        this reg ::CPlayerJoinPacket
        this reg ::CPlayerLeavePacket
        this reg {CPlayerMovePacket.Pos(it)}
        this reg {CPlayerMovePacket.Rot(it)}
        this reg SFirmEntityAddPacket::class.java
        this reg SFirmEntityDelPacket::class.java
        this reg SMeChangeDimensionPacket::class.java
        this reg SMeBlockEntityUpdatePacket::class.java
        this reg SMeBlockStateChangePacket::class.java
        this reg SMeJoinPacket::class.java
        this reg SMeLeavePacket::class.java
        this reg SMeMovePacket.Pos::class.java
        this reg SMeMovePacket.Rot::class.java

    }

    private infix fun reg(reader: PacketReader) {
        id2Reader += packCount to reader
        packCount++
    }

    private infix fun reg(writer: PacketWriter) {
        writer2id += writer to packCount
        packCount++
    }

    fun create(packerId: Byte, data: RByteBuf): CPacket? = id2Reader[packerId]?.let { reader ->
        val packet = reader(data)
        data.readerIndex(data.readerIndex() + data.readableBytes())
        packet
    } ?: let {
        lgr.error("找不到ID${packerId}的包")
        return null
    }


    fun getPacketId(packetClass: PacketWriter): Byte? = writer2id[packetClass]
}