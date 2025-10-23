package calebxzhou.rdi.mixin;

import net.minecraft.network.protocol.login.ServerboundHelloPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * calebxzhou @ 2025-10-21 10:33
 */
@Mixin(ServerboundHelloPacket.class)
public class mServerboundHelloPacket {
    @ModifyConstant(method = "write",constant = @Constant(intValue = 16))
    private static int RDI$AllowChineseName(int constant){
        return 64;
    }
}
