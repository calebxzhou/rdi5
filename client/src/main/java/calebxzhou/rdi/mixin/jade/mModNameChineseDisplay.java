package calebxzhou.rdi.mixin.jade;

import calebxzhou.rdi.RDI;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import snownee.jade.util.ClientProxy;

import java.util.Optional;

/**
 * calebxzhou @ 2024-06-29 23:33
 */

@Mixin(ClientProxy.class)
public class mModNameChineseDisplay {
    //高亮显示mod的中文名
    @Inject(method = "getModName",at=@At(value = "RETURN",ordinal = 1), cancellable = true,remap = false)
    private static void getName(String namespace, CallbackInfoReturnable<Optional<String>> cir) {
        if(RDI.modIdChineseName.containsKey(namespace)){
            cir.setReturnValue(Optional.of(RDI.modIdChineseName.get(namespace)));
        }
    }
}
