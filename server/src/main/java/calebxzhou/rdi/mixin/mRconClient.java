package calebxzhou.rdi.mixin;

import net.minecraft.server.rcon.thread.RconClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;

/**
 * calebxzhou @ 2025-03-02 23:58
 */
@Mixin(RconClient.class)
public class mRconClient {
    //rcon不需要登录
    @Mutable @Shadow
    private boolean authed=true;
}
