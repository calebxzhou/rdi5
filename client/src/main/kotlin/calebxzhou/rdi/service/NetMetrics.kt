package calebxzhou.rdi.service

import calebxzhou.rdi.util.*
import io.netty.buffer.ByteBuf
import net.minecraft.ChatFormatting
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.network.protocol.Packet
import java.io.File
import java.util.*


data class PacketRecord(val packetClass: Class<*>, var amount: Long = 0, var size: Long = 0)
data class PacketInfo(val className: String,)
//网络统计
object NetMetrics {
    init {

        Timer("RDI-NetMetrics").schedule(object : TimerTask() {
            var prevTxBytes = 0L
            var prevRxBytes = 0L
            override fun run() {
                prevTxBytes = totalTxBytes
                prevRxBytes = totalRxBytes
                Thread.sleep(1000)
                txKBps = (totalTxBytes - prevTxBytes) / 1024f
                rxKBps = (totalRxBytes - prevRxBytes) / 1024f
                /*if(true){

                val sortedTx = recordsTx.toList().sortedByDescending { (_, value) -> value.size }.toMap()
                val sortedRx = recordsRx.toList().sortedByDescending { (_, value) -> value.size }.toMap()
                val textTx= sortedTx.map { "${it.key} ${it.value.size} ${it.value.amount} ${it.value.packetClass}" }.joinToString("\n")
                File("logs/packet_tx.log").writeText(textTx)
                val textRx= sortedRx.map { "${it.key} ${it.value.size} ${it.value.amount} ${it.value.packetClass}" }.joinToString("\n")
                File("logs/packet_rx.log").writeText(textRx)
                }*/
            }
        }, 0, 2000)
    }

    @JvmStatic
    var totalTxBytes = 0L

    @JvmStatic
    var totalRxBytes = 0L

    var txKBps = 0f
    val txKBpsStr
        get() =
             "↑${txKBps.toFixed(2)}K"
    var rxKBps = 0f
    val rxKBpsStr
        get() = "↓${rxKBps.toFixed(2)}K"

    private val recordsTx = hashMapOf<Int, PacketRecord>()
    private val recordsRx = hashMapOf<Int, PacketRecord>()
    fun export(){
        val txf = File("rdi","net_tx.csv").also { it.createNewFile() }
        val rxf = File("rdi","net_rx.csv").also { it.createNewFile() }
        recordsTx.values.filter { it!=null }.toList().sortedByDescending { it.size }
            .joinToString("\n") { "${it.packetClass},${it.amount},${it.size}" }.let { txf.writeText(it) }
        recordsRx.values.filter { it!=null }.toList().sortedByDescending { it.size }
            .joinToString("\n") { "${it.packetClass},${it.amount},${it.size}" }.let { rxf.writeText(it) }
    }
    @JvmStatic
    fun onPacketSend(packetId: Int, packet: Packet<*>, byteBuf: ByteBuf) {
        totalTxBytes += byteBuf.writerIndex()
        recordsTx[packetId]?.let { record ->
            record.amount++
            record.size += byteBuf.writerIndex()
            recordsTx[packetId] = record
        } ?: let {
            val record = PacketRecord(packet.javaClass)
            recordsTx += packetId to record
        }
    }

    @JvmStatic
    fun onPacketRecv(packetId: Int, packet: Packet<*>, byteBuf: ByteBuf) {
        totalRxBytes += byteBuf.readerIndex()
        recordsRx[packetId]?.let { record ->
            record.amount++
            record.size += byteBuf.readerIndex()
            recordsRx[packetId] = record
        } ?: let {
            val record = PacketRecord(packet.javaClass)
            recordsRx += packetId to record
        }
        /*if(packet is ClientboundCustomPayloadPacket){

        File("logs/packet_rx.log").appendText(packet.identifier.toString()+" "+packet.data.readableBytes())
        }*/
    }

    @JvmStatic
    fun render(gg: GuiGraphics) {
        val tx =
            txKBpsStr
                .mcComp.withStyle(ChatFormatting.GOLD)
        val rx = rxKBpsStr.mcComp
            .withStyle(ChatFormatting.GREEN)
        gg.matrixOp {
            tran0ScaleBack((UiWidth-maxOf(tx.width,rx.width)*0.7).toInt(), UiHeight - 17, 0.7)

            gg.drawText(comp = tx)
            gg.drawText(comp = rx, y = 6)
        }
    }


}