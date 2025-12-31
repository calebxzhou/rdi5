package calebxzhou.rdi.mixin;

import calebxzhou.rdi.Const;
import calebxzhou.rdi.RDI;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.dedicated.DedicatedServerProperties;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Locale;

/**
 * calebxzhou @ 2024-05-26 10:18
 */
@Mixin(MinecraftServer.class)
public abstract class mServerProps  {
    @Overwrite
    public int getPort() {
        return Const.SERVER_PORT;
    }
    @Overwrite
    public boolean isFlightAllowed() {
        return true;
    }
    /*@Overwrite
    public boolean usesAuthentication() {
        return false;
    }*/

}
@Mixin(DedicatedServerProperties.class)
class mServerProps2  {
    @Mutable @Shadow @Final
    public final int spawnProtection =0;
    @Mutable @Shadow @Final
    public final int serverPort = Const.SERVER_PORT;

    @Redirect(method = "lambda$new$3",at= @At(value = "INVOKE",target = "Ljava/lang/String;toLowerCase(Ljava/util/Locale;)Ljava/lang/String;"
    ))
    private static String RDI$LevelTypeProps(String instance, Locale locale){
        return RDI.Companion.getEnvLevelType();
    }
}

