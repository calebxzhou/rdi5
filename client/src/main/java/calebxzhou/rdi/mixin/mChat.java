package calebxzhou.rdi.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientCommonPacketListenerImpl;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.CommonListenerCookie;
import net.minecraft.network.Connection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

/**
 * calebxzhou @ 2024-06-14 17:10
 */
@Mixin(ClientPacketListener.class)
public abstract class mChat extends ClientCommonPacketListenerImpl {
    @Shadow public abstract void sendCommand(String command);

    private mChat(Minecraft minecraft, Connection connection, CommonListenerCookie commonListenerCookie) {
        super(minecraft, connection, commonListenerCookie);
    }

    //不显示“无法验证聊天消息“
    @Overwrite
    private boolean enforcesSecureChat() {
        return true;
    }
    @Overwrite
    public void sendChat(String pMessage) {
        sendCommand("speak " + pMessage);
    }
}
