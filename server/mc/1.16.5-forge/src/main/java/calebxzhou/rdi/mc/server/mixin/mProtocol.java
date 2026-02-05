package calebxzhou.rdi.mc.server.mixin;

import com.mojang.authlib.GameProfile;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.login.ServerLoginNetHandler;
import net.minecraft.network.login.client.CLoginStartPacket;
import net.minecraft.network.login.server.SLoginSuccessPacket;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.IOException;
import java.util.UUID;

/**
 * calebxzhou @ 2024-05-31 20:09
 */
@Mixin(CLoginStartPacket.class)
class mAllowChineseNameLogin {

    @Shadow
    @Mutable
    private GameProfile gameProfile;

    @Overwrite
    public void read(PacketBuffer buf) throws IOException {
        this.gameProfile = new GameProfile((UUID)buf.readUUID(), buf.readUtf(64));
    }

}
@Mixin(PacketBuffer.class)
class mLargerBuffer{
}
/**
 * calebxzhou @ 2025-02-21 0:32
 */
@Mixin(ServerLoginNetHandler.class)
abstract
class mServerLoginPacketListener {

    @Shadow
    public abstract void disconnect(ITextComponent reason);
    //允许中文用户名
   /* @Overwrite
    public static boolean isValidUsername(String pUsername) {
        return pUsername.chars().filter((p_203791_) -> (p_203791_ <= 32 || p_203791_ >= 127) && (p_203791_ < 0x4E00 || p_203791_ > 0x9FFF)).findAny().isEmpty();
    }*/
    //注入rdid到mc本体的uuid

    @Unique
    CLoginStartPacket $packet;
    @Inject(method = "handleHello",at=@At("HEAD"))
    private void storePacket(CLoginStartPacket pPacket, CallbackInfo ci){
        this.$packet = pPacket;
    }

    @Inject(method = "handleHello",at= @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;usesAuthentication()Z"), cancellable = true)
    private void RDI$CheckInjectUUID(CLoginStartPacket packet, CallbackInfo ci) {

        if (packet.getGameProfile().getId()==null) {
            disconnect(new StringTextComponent("未登录RDI账号！"));
            ci.cancel();
        }

    }
}
