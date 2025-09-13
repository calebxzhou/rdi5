package calebxzhou.rdi.mixin;

import calebxzhou.rdi.model.RAccount;
import calebxzhou.rdi.model.Room;
import calebxzhou.rdi.net.RawByteHandler;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.buffer.UnpooledDirectByteBuf;
import io.netty.channel.ChannelPipeline;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.network.BandwidthDebugMonitor;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.login.ServerboundHelloPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * calebxzhou @ 2025-09-13 9:27
 */
@Mixin(targets = {"net.minecraft.client.gui.screens.ConnectScreen$1"})
public class mProxyPortChanger {

    @Redirect(method = "run",at= @At(value = "INVOKE", target = "Lnet/minecraft/network/Connection;send(Lnet/minecraft/network/protocol/Packet;)V"))
    private void RDI$BeforeSendHello(Connection connection, Packet<?> packet){
        try {
            var data = Unpooled.buffer();
            data.writeBytes(new byte[]{0x01,0x02,0x03,0x04, (byte) 0xAA, (byte) 0xBB, (byte) 0xCC, (byte) 0xDD});
            data.writeShort(Room.now.getPort());
            connection.channel().writeAndFlush(data);
            connection.send(new ServerboundHelloPacket(RAccount.now.getName(), RAccount.now.getUuid()));
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
@Mixin(Connection.class)
class mProxySerializer{
    @Inject(method = "configureSerialization",at= @At(value = "INVOKE", target = "Lio/netty/channel/ChannelPipeline;addLast(Ljava/lang/String;Lio/netty/channel/ChannelHandler;)Lio/netty/channel/ChannelPipeline;",ordinal = 0))
    private static void RDI$RawByteEncoder(ChannelPipeline pipeline, PacketFlow flow, boolean memoryOnly, BandwidthDebugMonitor bandwithDebugMonitor, CallbackInfo ci){
        pipeline.addFirst("rawByteHandler", new RawByteHandler());
    }

}
