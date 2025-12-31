package calebxzhou.rdi.mixin;

import com.mojang.authlib.GameProfile;
import net.minecraft.network.protocol.login.ServerboundHelloPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerLoginPacketListenerImpl;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;

/**
 * calebxzhou @ 2025-12-31 13:31
 */
@Mixin(ServerLoginPacketListenerImpl.class)
public abstract class mAuth {
    @Shadow
    abstract void startClientVerification(GameProfile authenticatedProfile);

    @Shadow
    @Final
    MinecraftServer server;

    @Shadow
    @Nullable
    private GameProfile authenticatedProfile;

    @Inject(method = "handleHello",at= @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;getSingleplayerProfile()Lcom/mojang/authlib/GameProfile;"), cancellable = true)
    private void RDI$DirectAuthCheck(ServerboundHelloPacket packet, CallbackInfo ci){
        startClientVerification(new GameProfile(packet.profileId(), packet.name()));
        ci.cancel();
    }

    @Inject(method = "startClientVerification",at=@At("TAIL"))
    private void RDI$FillGameProfile(GameProfile authenticatedProfile, CallbackInfo ci){
        var prof = server.getSessionService().fetchProfile(authenticatedProfile.getId(),false);
        if(prof != null){
            this.authenticatedProfile = prof.profile();
        }
    }
}
