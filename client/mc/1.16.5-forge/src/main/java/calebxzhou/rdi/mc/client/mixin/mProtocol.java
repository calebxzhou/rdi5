package calebxzhou.rdi.mc.client.mixin;

import calebxzhou.rdi.mc.common.RDI;
import com.mojang.authlib.GameProfile;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.ProtocolType;
import net.minecraft.network.handshake.client.CHandshakePacket;
import net.minecraft.network.login.client.CLoginStartPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * calebxzhou @ 2025-10-21 10:33
 */
@Mixin(CLoginStartPacket.class)
class mServerboundHelloPacket {
    @Shadow
    private GameProfile gameProfile;

    @Inject(method = "write",at=@At("TAIL"))
    private void RDI$WriteUUIDPacket(PacketBuffer buffer, CallbackInfo ci){
        buffer.writeUUID(this.gameProfile.getId());
    }
}
@Mixin(CHandshakePacket.class)
class mClientIntentionPacket {
    @Shadow
    @Mutable
    private int port;

    @Inject(method = "<init>(Ljava/lang/String;ILnet/minecraft/network/ProtocolType;)V",at=@At("TAIL"))
    private void RDI$InjectHostPort(String hostName, int port, ProtocolType intention, CallbackInfo ci){
        this.port = calebxzhou.rdi.mc.common.RDI.HOST_PORT;
    }
    @Redirect(method = "write",at= @At(value = "INVOKE", target = "Lnet/minecraft/network/PacketBuffer;writeShort(I)Lio/netty/buffer/ByteBuf;"))
    private ByteBuf RDI$WriteCorrectPort(PacketBuffer buf, int value){
        buf.writeShort(RDI.HOST_PORT);
        return buf;
    }
}