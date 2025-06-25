package calebxzhou.rdi.net

import calebxzhou.rdi.lgr
import calebxzhou.rdi.net.protocol.SMeLeavePacket
import calebxzhou.rdi.net.protocol.SMeLoginPacket
import calebxzhou.rdi.net.protocol.SMeMovePacket
import calebxzhou.rdi.net.protocol.SPersistEntityAddPacket
import calebxzhou.rdi.net.protocol.SPlayerBlockEntityUpdatePacket
import calebxzhou.rdi.net.protocol.SPlayerBlockStateChangePacket
import io.netty.buffer.ByteBuf
import kotlin.jvm.java

typealias PacketReader = (ByteBuf) -> CPacket
typealias PacketWriter = Class<out SPacket>

object RPacketSet {

    private var packCount = 0.toByte()

    //s2c
    private val id2Reader = linkedMapOf<Byte,PacketReader>()
    //c2s
    private val writer2id = linkedMapOf<PacketWriter,Byte>()
    init {
        this reg SMeLoginPacket::class.java
        this reg SMeLeavePacket::class.java
        this reg SMeMovePacket.Pos::class.java
        this reg SMeMovePacket.Rot::class.java
        this reg SPlayerBlockEntityUpdatePacket::class.java
        this reg SPlayerBlockStateChangePacket::class.java

    }
    private infix fun reg(reader: PacketReader){
        id2Reader += packCount to reader
        packCount++
    }
    private infix  fun reg(writer: PacketWriter){
        writer2id += writer to packCount
        packCount++
    }

    fun create(packerId: Byte,data: ByteBuf) : CPacket? = id2Reader[packerId]?.let { reader->
        val packet = reader(data)
        data.readerIndex(data.readerIndex()+data.readableBytes())
        packet
    }?:let {
        lgr.error ("找不到ID${packerId}的包")
        return null
    }


    fun getPacketId(packetClass: PacketWriter): Byte? = writer2id[packetClass]
}