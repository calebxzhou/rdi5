package calebxzhou.rdi.net

import calebxzhou.rdi.lgr
import io.netty.buffer.ByteBuf

typealias PacketReader = (ByteBuf) -> CPacket
typealias PacketWriter = Class<out SPacket>

object RPacketSet {
    init {

    }
    private var packCount = 0

    //s2c
    private val id2Reader = linkedMapOf<Int,PacketReader>()
    //c2s
    private val writer2id = linkedMapOf<PacketWriter,Int>()

    private fun register(reader: PacketReader){
        id2Reader += packCount to reader
        packCount++
    }
    private fun register(writer: PacketWriter){
        writer2id += writer to packCount
        packCount++
    }
    fun create(packerId: Int,data: ByteBuf) : CPacket? = id2Reader[packerId]?.let { reader->
        val packet = reader(data)
        data.readerIndex(data.readerIndex()+data.readableBytes())
        packet
    }?:let {
        lgr.error ("找不到ID${packerId}的包")
        return null
    }
}