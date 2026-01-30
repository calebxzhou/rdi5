package calebxzhou.rdi.mc.client.mixin;

import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundServerDataPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

/**
 * calebxzhou @ 2024-06-14 17:10
 */
@Mixin(ClientboundServerDataPacket.class)
public abstract class mChat {


    //不显示“无法验证聊天消息“
    @Overwrite
    public boolean enforcesSecureChat() {
        return true;
    }

}
