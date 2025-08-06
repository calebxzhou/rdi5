package calebxzhou.rdi.service

import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.common.ClientboundKeepAlivePacket
import net.minecraft.network.protocol.common.ServerCommonPacketListener
import net.minecraft.network.protocol.game.*
import net.minecraft.server.network.ServerCommonPacketListenerImpl
import net.minecraft.server.network.ServerGamePacketListenerImpl

object NetThrottler {
    //几个tick以后才发一个包
    private val packetTickInterval = hashMapOf<Class<out Packet<*>>,Int>(
        //ClientboundBlockEntityDataPacket::class.java to 4,
    )
    //tick计数
    private val packetTicks = packetTickInterval.mapValues { 0 }.toMutableMap()
    @JvmStatic
    fun allowSendPacket(packetListener: ServerCommonPacketListenerImpl, packet: Packet<*>): Boolean{
        if(PlayerService.isPlayerAfk(packetListener.owner.id)){
            //挂机只发送keep alive数据包
            return packet is ClientboundKeepAlivePacket
                    || packet is ClientboundSystemChatPacket
                    || packet is ClientboundRemoveEntitiesPacket
                    || packet is ClientboundPlayerInfoUpdatePacket
                    || packet is ClientboundAddEntityPacket
                    || packet is ClientboundPlayerInfoRemovePacket
                    || packet is ClientboundPlayerChatPacket
        }

        /*if(packet is ClientboundBlockEntityDataPacket){
            //容器更新仅发送32格内
            if (packetListener.player.blockPosition().distSqr(packet.pos) > 32 * 32) {
                return false
            }
        }*/
        packetTickInterval[packet.javaClass]?.let { interval ->
            packetTicks[packet.javaClass]?.let { ticks ->
                if(ticks >= interval){
                    packetTicks[packet.javaClass] = 0
                    return true
                }else{
                    packetTicks[packet.javaClass] = ticks+1
                    return false
                }
            }
        }

        return true
    }
}