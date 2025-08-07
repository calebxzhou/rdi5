package calebxzhou.rdi.service

import calebxzhou.rdi.util.*
import io.netty.buffer.ByteBuf
import net.minecraft.ChatFormatting
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.PacketType
import java.io.File
import java.util.*


data class PacketRecord(val packetType: PacketType<*>, var amount: Long = 0, var size: Long = 0)
//网络统计
object NetMetrics {
    init {
        Timer("RDI-NetMetrics").schedule(object : TimerTask() {
            var prevBytes = 0L to 0L
            override fun run() {
                prevBytes = totalBytes
                Thread.sleep(1000)
                kbps = (totalBytes.first - prevBytes.first) / 1024f to (totalBytes.second - prevBytes.second) / 1024f
                /*if(true){

                val sortedTx = records.first.toList().sortedByDescending { (_, value) -> value.size }.toMap()
                val sortedRx = records.second.toList().sortedByDescending { (_, value) -> value.size }.toMap()
                val textTx= sortedTx.map { "${it.key} ${it.value.size} ${it.value.amount} ${it.value.packetClass}" }.joinToString("\n")
                File("logs/packet_tx.log").writeText(textTx)
                val textRx= sortedRx.map { "${it.key} ${it.value.size} ${it.value.amount} ${it.value.packetClass}" }.joinToString("\n")
                File("logs/packet_rx.log").writeText(textRx)
                }*/
            }
        }, 0, 2000)
    }

    var totalBytes = 0L to 0L
    var kbps = 0f to 0f
    val kbpsStr: Pair<String, String>
        get() = "↑${kbps.first.toFixed(2)}K" to "↓${kbps.second.toFixed(2)}K"
    val records = hashMapOf<PacketType<*>, PacketRecord>() to hashMapOf<PacketType<*>, PacketRecord>()
    fun export(){
        val txf = File("rdi","net_tx.csv").also { it.createNewFile() }
        val rxf = File("rdi","net_rx.csv").also { it.createNewFile() }
        records.first.values.filter { it!=null }.toList().sortedByDescending { it.size }
            .joinToString("\n") { "${it.packetType.id},${it.amount},${it.size}" }.let { txf.writeText(it) }
        records.second.values.filter { it!=null }.toList().sortedByDescending { it.size }
            .joinToString("\n") { "${it.packetType.id},${it.amount},${it.size}" }.let { rxf.writeText(it) }
    }
    @JvmStatic
    fun onPacketSend(packet: Packet<*>, byteBuf: ByteBuf) {
        val size = byteBuf.writerIndex().toLong()
        totalBytes = (totalBytes.first + size) to totalBytes.second
        val packetType = packet.type()
        records.first[packetType]?.let { record ->
            record.amount++
            record.size += size
        } ?: let {
            val record = PacketRecord(packetType, 1, size)
            records.first[packetType] = record
        }
    }

    @JvmStatic
    fun onPacketRecv(packet: Packet<*>, byteBuf: ByteBuf) {
        val size = byteBuf.readerIndex().toLong()
        totalBytes = totalBytes.first to (totalBytes.second + size)
        val packetType = packet.type()
        records.second[packetType]?.let { record ->
            record.amount++
            record.size += size
        } ?: let {
            val record = PacketRecord(packetType, 1, size)
            records.second[packetType] = record
        }

    }

    @JvmStatic
    fun render(gg: GuiGraphics) {
        val tx = kbpsStr.first
            .mcComp.withStyle(ChatFormatting.GOLD)
        val rx = kbpsStr.second.mcComp
            .withStyle(ChatFormatting.GREEN)
        gg.matrixOp {
            tran0ScaleBack((UiWidth-maxOf(tx.width,rx.width)*0.7).toInt(), UiHeight - 17, 0.7)

            gg.drawText(comp = tx)
            gg.drawText(comp = rx, y = 6)
        }
    }


}