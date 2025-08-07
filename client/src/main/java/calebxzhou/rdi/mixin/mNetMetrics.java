package calebxzhou.rdi.mixin;

import calebxzhou.rdi.service.NetMetrics;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.PacketDecoder;
import net.minecraft.network.PacketEncoder;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.List;

/**
 * calebxzhou @ 2024-06-25 8:17
 */
@Mixin(PacketEncoder.class)
public class mNetMetrics {
    @Inject(method = "encode(Lio/netty/channel/ChannelHandlerContext;Lnet/minecraft/network/protocol/Packet;Lio/netty/buffer/ByteBuf;)V",
    at= @At(value = "INVOKE", target = "Lnet/minecraft/util/profiling/jfr/JvmProfiler;onPacketSent(Lnet/minecraft/network/ConnectionProtocol;Lnet/minecraft/network/protocol/PacketType;Ljava/net/SocketAddress;I)V",shift = At.Shift.AFTER
    ),locals = LocalCapture.CAPTURE_FAILEXCEPTION)
    private void RDI_onPacketEncode(ChannelHandlerContext p_130545_, Packet packet, ByteBuf byteBuf, CallbackInfo ci, PacketType packettype, int i){
        NetMetrics.onPacketSend(packet, byteBuf);
    }
}
@Mixin(PacketDecoder.class)
 class mNetMetrics2 {
    @Inject(method = "decode",
            at= @At(value = "INVOKE", target = "Lnet/minecraft/util/profiling/jfr/JvmProfiler;onPacketReceived(Lnet/minecraft/network/ConnectionProtocol;Lnet/minecraft/network/protocol/PacketType;Ljava/net/SocketAddress;I)V"
            ),locals = LocalCapture.CAPTURE_FAILEXCEPTION)
    private void RDI_onPacketEncode(ChannelHandlerContext context, ByteBuf in, List<Object> out, CallbackInfo ci, int i, Packet packet, PacketType packettype){
        NetMetrics.onPacketRecv(packet, in);
    }
}