package calebxzhou.rdi.mixin;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * calebxzhou @ 2024-05-26 12:49
 */
@Mixin(ServerGamePacketListenerImpl.class)
public class mNoQuitNotify {
    @Shadow public ServerPlayer player;

    //退出服务器不显示"XXX退出"
    @Redirect(method = "removePlayerFromWorld",
            at=@At(value = "INVOKE",target = "Lnet/minecraft/server/players/PlayerList;broadcastSystemMessage(Lnet/minecraft/network/chat/Component;Z)V"))
    private void noBroadcastDisconnecting(PlayerList instance, Component component, boolean bl){
        //什么都不做
    }

}
@Mixin(PlayerList.class)
class mNoJoinNotify{
    //进入服务器不显示"XXX进入"
    @Redirect(method = "placeNewPlayer",
            at=@At(value = "INVOKE",target = "Lnet/minecraft/server/players/PlayerList;broadcastSystemMessage(Lnet/minecraft/network/chat/Component;Z)V"))
    private void nojoinsay(PlayerList instance, Component component, boolean bl){
        //什么都不做
    }
}