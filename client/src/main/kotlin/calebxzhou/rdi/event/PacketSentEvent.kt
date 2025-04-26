package calebxzhou.rdi.event

import io.netty.channel.Channel
import io.netty.channel.ChannelFuture
import net.minecraft.network.PacketSendListener
import net.minecraft.network.protocol.Packet
import net.neoforged.bus.api.Event

class PacketSentEvent(
    val packet: Packet<*>,
    val sendListener: PacketSendListener?,
    val channel: Channel,
    val channelFuture: ChannelFuture
): Event() {
}