package calebxzhou.rdi.mixin;

import com.mojang.authlib.GameProfile;
import net.minecraft.network.protocol.login.ServerboundHelloPacket;
import net.minecraft.server.network.ServerLoginPacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;

/**
 * calebxzhou @ 2025-02-21 0:32
 */
@Mixin(ServerLoginPacketListenerImpl.class)
public class mServerLoginPacketListener {
    @Shadow @Nullable @Mutable
    private GameProfile authenticatedProfile;

    //允许用户名中文


    //注入rdid到mc本体的uuid
    @Inject(method = "handleHello",at= @At(value = "TAIL"))
    private void injectUUID(ServerboundHelloPacket pPacket, CallbackInfo ci){
        authenticatedProfile = new GameProfile(pPacket.profileId(),pPacket.name());
    }
}
