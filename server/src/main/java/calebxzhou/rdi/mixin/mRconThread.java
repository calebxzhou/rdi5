package calebxzhou.rdi.mixin;

import net.minecraft.server.ServerInterface;
import net.minecraft.server.rcon.thread.RconThread;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * calebxzhou @ 2025-02-25 22:26
 */
@Mixin(RconThread.class)
public class mRconThread {
    //rcon仅绑定本地，仅允许本地访问
    @Redirect(method = "create",at= @At(value = "INVOKE", target = "Lnet/minecraft/server/ServerInterface;getServerIp()Ljava/lang/String;"))
    private static String RDI$RconLocalIpOnly(ServerInterface instance){
        System.out.println("创建RCON线程");
        return "127.0.0.1";
    }

}
