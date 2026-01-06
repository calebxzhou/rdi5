package calebxzhou.rdi.mc.client.mixin;

import net.minecraft.client.multiplayer.ClientPacketListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

/**
 * calebxzhou @ 2024-06-14 17:10
 */
@Mixin(ClientPacketListener.class)
public abstract class mChat {


    //不显示“无法验证聊天消息“
    @Overwrite
    private boolean enforcesSecureChat() {
        return true;
    }

}
