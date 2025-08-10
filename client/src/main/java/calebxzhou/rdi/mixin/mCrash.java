package calebxzhou.rdi.mixin;

import calebxzhou.rdi.service.CrashUploader;
import fudge.notenoughcrashes.gui.ProblemScreen;
import net.minecraft.CrashReport;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * calebxzhou @ 2025-08-10 18:59
 */
//用了not enough crashes的CrashScreen
@Mixin(ProblemScreen.class)
public class mCrash {
    @Inject(method = "<init>",at=@At("TAIL"))
    private static void RDI$OnCrash(CrashReport report, CallbackInfo ci){
        CrashUploader.start(report);
    }
}
